package education.creatorindex.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import education.creatorindex.infrai.InfraiRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientConfiguration {
    @Bean
    OpenAIClient openAIClient(InfraiProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .baseUrl(properties.baseUrl() + "/v1")
                .build();
    }

    @Bean
    InfraiRestClient infraiRestClient(InfraiProperties properties, ObjectMapper mapper) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        return new InfraiRestClient(http, mapper, properties.baseUrl(), properties.apiKey());
    }
}
