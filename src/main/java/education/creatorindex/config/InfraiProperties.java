package education.creatorindex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("infrai")
public record InfraiProperties(
        String baseUrl,
        String apiKey,
        String collection,
        String embeddingModel,
        int embeddingDimension,
        String publicBaseUrl,
        List<String> sourceUrls,
        List<String> publishedSourceUrls) {
}
