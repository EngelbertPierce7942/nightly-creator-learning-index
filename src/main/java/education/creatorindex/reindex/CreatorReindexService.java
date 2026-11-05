package education.creatorindex.reindex;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import education.creatorindex.config.InfraiProperties;
import education.creatorindex.infrai.InfraiRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class CreatorReindexService {
    private final InfraiRestClient infrai;
    private final OpenAIClient openAI;
    private final InfraiProperties properties;
    private final Clock clock;

    @Autowired
    public CreatorReindexService(InfraiRestClient infrai, OpenAIClient openAI, InfraiProperties properties) {
        this(infrai, openAI, properties, Clock.systemUTC());
    }

    CreatorReindexService(InfraiRestClient infrai, OpenAIClient openAI, InfraiProperties properties, Clock clock) {
        this.infrai = infrai;
        this.openAI = openAI;
        this.properties = properties;
        this.clock = clock;
    }

    public ReindexReport rebuild() {
        String run = LocalDate.now(clock).toString();
        List<LearningResource> resources = properties.sourceUrls().stream().map(url -> scrape(url, run)).toList();
        List<LearningResource> selected = selectForIndex(resources);
        List<Map<String, ?>> vectors = new ArrayList<>();
        for (LearningResource resource : selected) {
            List<Float> embedding = openAI.embeddings().create(EmbeddingCreateParams.builder()
                    .model(properties.embeddingModel())
                    .input(resource.content())
                    .build()).data().getFirst().embedding();
            vectors.add(Map.of(
                    "id", stableId(resource.sourceUrl()),
                    "embedding", embedding,
                    "metadata", Map.of("source_url", resource.sourceUrl(), "kind", resource.kind().name())));
        }
        if (!vectors.isEmpty()) {
            infrai.post("/v1/vector/upsert", Map.of("collection", properties.collection(), "vectors", vectors),
                    "reindex-" + properties.collection() + "-" + run);
        }
        return new ReindexReport(run, resources.size(), vectors.size(), properties.collection());
    }

    static List<LearningResource> selectForIndex(List<LearningResource> resources) {
        return resources.stream().filter(LearningResource::shouldIndex).toList();
    }

    private LearningResource scrape(String url, String run) {
        JsonNode data = infrai.post("/v1/web/scrape", Map.of("url", url, "format", "markdown"),
                "scrape-" + stableId(url) + "-" + run);
        String content = data.path("content").asText();
        return new LearningResource(url, LearningResource.Kind.fromUrl(url), content,
                properties.publishedSourceUrls() != null && properties.publishedSourceUrls().contains(url));
    }

    private static String stableId(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record ReindexReport(String runDate, int discovered, int indexed, String collection) {
    }
}
