package gimpanel.tracker.models;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class WebhookPayload
{
    private String type;           // LEVEL, LOOT, KILL_COUNT, QUEST, etc.
    private String playerName;
    private String source;         // "gimpanel-tracker" or "dink" or "group-ironmen-tracker"
    private long timestamp;
    private Map<String, Object> extra;

    // Common fields for compatibility
    private String text;           // Discord message text
    private String content;        // Alternative message content
    private List<WebhookEmbed> embeds;

    public WebhookPayload()
    {
        this.timestamp = System.currentTimeMillis();
        this.source = "gimpanel-tracker";
    }

    public WebhookPayload(String type, String playerName)
    {
        this();
        this.type = type;
        this.playerName = playerName;
    }

    @Data
    public static class WebhookEmbed
    {
        private String title;
        private String description;
        private Integer color;
        private WebhookEmbedThumbnail thumbnail;
        private List<WebhookEmbedField> fields;
        private WebhookEmbedFooter footer;

        @Data
        public static class WebhookEmbedThumbnail
        {
            private String url;
        }

        @Data
        public static class WebhookEmbedField
        {
            private String name;
            private String value;
            private Boolean inline;

            public WebhookEmbedField(String name, String value, Boolean inline)
            {
                this.name = name;
                this.value = value;
                this.inline = inline;
            }
        }

        @Data
        public static class WebhookEmbedFooter
        {
            private String text;
        }
    }
}