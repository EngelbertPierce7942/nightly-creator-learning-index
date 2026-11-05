package education.creatorindex.reindex;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearningResourceDecisionTest {
    @Test
    void indexesOnlyPublishableResourcesWithContent() {
        List<LearningResource> resources = List.of(
                new LearningResource("https://school.test/assets/lesson-kit", LearningResource.Kind.DIGITAL_ASSET_DELIVERY,
                        "Download the worksheet after lesson three.", true),
                new LearningResource("https://school.test/updates/draft", LearningResource.Kind.SUBSCRIBER_UPDATE,
                        "Draft note for next cohort.", false),
                new LearningResource("https://school.test/processing/empty", LearningResource.Kind.CONTENT_PROCESSING,
                        "  ", true));

        assertThat(CreatorReindexService.selectForIndex(resources))
                .extracting(LearningResource::sourceUrl)
                .containsExactly("https://school.test/assets/lesson-kit");
    }
}
