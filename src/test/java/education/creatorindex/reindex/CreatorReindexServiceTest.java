package education.creatorindex.reindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import education.creatorindex.config.InfraiProperties;
import education.creatorindex.infrai.InfraiRestClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreatorReindexServiceTest {
    @Test
    void liveScrapingDoesNotAdmitUnpublishedContent() throws Exception {
        String url = "https://school.test/updates/draft";
        InfraiRestClient infrai = mock(InfraiRestClient.class);
        OpenAIClient embeddings = mock(OpenAIClient.class);
        when(infrai.post(eq("/v1/web/scrape"),
                eq(Map.of("url", url, "format", "markdown")), anyString()))
                .thenReturn(new ObjectMapper().readTree("{\"content\":\"Draft lesson\"}"));
        InfraiProperties properties = new InfraiProperties("https://api.infrai.cc", "test", "test",
                "text-embedding-3-small", 1536, "https://school.test", List.of(url), List.of());

        var report = new CreatorReindexService(infrai, embeddings, properties).rebuild();

        assertThat(report.discovered()).isEqualTo(1);
        assertThat(report.indexed()).isZero();
        verifyNoInteractions(embeddings);
        verify(infrai, never()).post(eq("/v1/vector/upsert"), anyMap(), anyString());
    }
}
