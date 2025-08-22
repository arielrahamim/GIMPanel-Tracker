package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class QuestNotification extends WebhookPayload
{
    public QuestNotification(QuestData quest)
    {
        super("QUEST", quest.getPlayerName());
        
        // Dink-compatible extra data
        Map<String, Object> extra = new HashMap<>();
        extra.put("questName", quest.getQuestName());
        extra.put("name", quest.getQuestName());
        extra.put("status", quest.getStatus());
        extra.put("questPoints", quest.getQuestPoints());
        setExtra(extra);

        // Only send notification for completed quests
        if (!"COMPLETED".equals(quest.getStatus()))
        {
            return;
        }

        // Discord embed for rich display
        WebhookEmbed embed = new WebhookEmbed();
        embed.setTitle("📜 Quest Completed!");
        embed.setDescription(String.format("%s completed **%s**!", 
            quest.getPlayerName(), quest.getQuestName()));
        embed.setColor(0xFFD700); // Gold for quest completion

        List<WebhookEmbed.WebhookEmbedField> fields = new ArrayList<>();
        fields.add(new WebhookEmbed.WebhookEmbedField("Quest", quest.getQuestName(), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Status", "Completed", true));
        
        if (quest.getQuestPoints() > 0)
        {
            fields.add(new WebhookEmbed.WebhookEmbedField("Quest Points", String.valueOf(quest.getQuestPoints()), true));
        }

        embed.setFields(fields);
        
        WebhookEmbed.WebhookEmbedFooter footer = new WebhookEmbed.WebhookEmbedFooter();
        footer.setText("GIMPanel Tracker");
        embed.setFooter(footer);

        List<WebhookEmbed> embeds = new ArrayList<>();
        embeds.add(embed);
        setEmbeds(embeds);

        // Simple text format for compatibility
        setText(String.format("%s completed quest: %s", quest.getPlayerName(), quest.getQuestName()));
    }
}