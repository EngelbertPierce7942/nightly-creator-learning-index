package education.creatorindex.reindex;

import com.fasterxml.jackson.databind.JsonNode;
import education.creatorindex.config.InfraiProperties;
import education.creatorindex.infrai.InfraiException;
import education.creatorindex.infrai.InfraiRestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ReindexController {
    private final CreatorReindexService reindex;
    private final InfraiRestClient infrai;
    private final InfraiProperties properties;

    public ReindexController(CreatorReindexService reindex, InfraiRestClient infrai, InfraiProperties properties) {
        this.reindex = reindex;
        this.infrai = infrai;
        this.properties = properties;
    }

    @RequestMapping(value = "/reindex/nightly", method = {RequestMethod.GET, RequestMethod.POST})
    CreatorReindexService.ReindexReport nightlyReindex() {
        return reindex.rebuild();
    }

    @PostMapping("/schedule")
    Map<String, String> schedule() {
        infrai.post("/v1/vector/collection/create", Map.of(
                "collection", properties.collection(),
                "dimension", properties.embeddingDimension(),
                "metric", "cosine",
                "metadata", Map.of("purpose", "creator-learning-search")),
                "collection-" + properties.collection());
        JsonNode data = infrai.post("/v1/cron/create", Map.of(
                "cron_expr", "0 2 * * *",
                "task", properties.publicBaseUrl() + "/reindex/nightly"),
                "creator-nightly-reindex");
        return Map.of("job_id", data.path("job_id").asText(), "task", properties.publicBaseUrl() + "/reindex/nightly");
    }

    @ExceptionHandler(InfraiException.class)
    ResponseEntity<Map<String, Object>> handleInfrai(InfraiException error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(Map.of("code", error.code(), "message", error.getMessage()));
    }
}
