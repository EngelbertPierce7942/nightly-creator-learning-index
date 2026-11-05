package education.creatorindex.reindex;

import java.util.Locale;

public record LearningResource(String sourceUrl, Kind kind, String content, boolean publishable) {
    public enum Kind {
        DIGITAL_ASSET_DELIVERY,
        SUBSCRIBER_UPDATE,
        CONTENT_PROCESSING;

        static Kind fromUrl(String url) {
            String value = url.toLowerCase(Locale.ROOT);
            if (value.contains("asset")) return DIGITAL_ASSET_DELIVERY;
            if (value.contains("update")) return SUBSCRIBER_UPDATE;
            return CONTENT_PROCESSING;
        }
    }

    public boolean shouldIndex() {
        return publishable && content != null && !content.isBlank();
    }
}
