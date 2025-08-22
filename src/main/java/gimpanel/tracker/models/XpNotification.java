package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class XpNotification extends WebhookPayload
{
    public XpNotification(SkillData skill)
    {
        super("XP_GAIN", skill.getPlayerName());
        
        // Dink-compatible extra data
        Map<String, Object> extra = new HashMap<>();
        extra.put("skill", skill.getSkillName());
        extra.put("level", skill.getLevel());
        extra.put("xp", skill.getXp());
        extra.put("xpGained", skill.getXpGained());
        setExtra(extra);

        // Discord embed for rich display
        WebhookEmbed embed = new WebhookEmbed();
        embed.setTitle("📈 Experience Gained!");
        embed.setDescription(String.format("%s gained **%,d** XP in %s!", 
            skill.getPlayerName(), skill.getXpGained(), skill.getSkillName()));
        
        // Color based on XP gained amount
        if (skill.getXpGained() >= 10000)
        {
            embed.setColor(0xFFD700); // Gold for high XP gains
        }
        else if (skill.getXpGained() >= 1000)
        {
            embed.setColor(0x9932CC); // Purple for medium XP gains
        }
        else
        {
            embed.setColor(0x32CD32); // Green for regular XP gains
        }

        List<WebhookEmbed.WebhookEmbedField> fields = new ArrayList<>();
        fields.add(new WebhookEmbed.WebhookEmbedField("Skill", skill.getSkillName(), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("XP Gained", String.format("%,d", skill.getXpGained()), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Total XP", String.format("%,d", skill.getXp()), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Current Level", String.valueOf(skill.getLevel()), true));

        embed.setFields(fields);
        
        WebhookEmbed.WebhookEmbedFooter footer = new WebhookEmbed.WebhookEmbedFooter();
        footer.setText("GIMPanel Tracker");
        embed.setFooter(footer);

        List<WebhookEmbed> embeds = new ArrayList<>();
        embeds.add(embed);
        setEmbeds(embeds);

        // Simple text format for compatibility
        setText(String.format("%s gained %,d XP in %s!", 
            skill.getPlayerName(), skill.getXpGained(), skill.getSkillName()));
    }
}
