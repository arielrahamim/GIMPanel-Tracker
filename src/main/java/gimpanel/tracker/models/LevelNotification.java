package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class LevelNotification extends WebhookPayload
{
    public LevelNotification(SkillData skill)
    {
        super("LEVEL", skill.getPlayerName());
        
        // Dink-compatible extra data
        Map<String, Object> extra = new HashMap<>();
        extra.put("skill", skill.getSkillName());
        extra.put("level", skill.getLevel());
        extra.put("xp", skill.getXp());
        extra.put("levelsGained", skill.getLevelsGained());
        extra.put("xpGained", skill.getXpGained());
        setExtra(extra);

        // Discord embed for rich display
        WebhookEmbed embed = new WebhookEmbed();
        embed.setTitle("🎉 Level Up!");
        embed.setDescription(String.format("%s reached level **%d** in %s!", 
            skill.getPlayerName(), skill.getLevel(), skill.getSkillName()));
        
        if (skill.getLevel() == 99)
        {
            embed.setColor(0xFFD700); // Gold for 99s
            embed.setTitle("🏆 99 Achievement!");
        }
        else if (skill.getLevel() >= 90)
        {
            embed.setColor(0x9932CC); // Purple for high levels
        }
        else if (skill.getLevel() >= 70)
        {
            embed.setColor(0x00CED1); // Cyan for medium levels
        }
        else
        {
            embed.setColor(0x32CD32); // Green for regular levels
        }

        List<WebhookEmbed.WebhookEmbedField> fields = new ArrayList<>();
        fields.add(new WebhookEmbed.WebhookEmbedField("Skill", skill.getSkillName(), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Level", String.valueOf(skill.getLevel()), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("XP", String.format("%,d", skill.getXp()), true));
        
        if (skill.getXpGained() > 0)
        {
            fields.add(new WebhookEmbed.WebhookEmbedField("XP Gained", String.format("%,d", skill.getXpGained()), true));
        }

        embed.setFields(fields);
        
        WebhookEmbed.WebhookEmbedFooter footer = new WebhookEmbed.WebhookEmbedFooter();
        footer.setText("GIMPanel Tracker");
        embed.setFooter(footer);

        List<WebhookEmbed> embeds = new ArrayList<>();
        embeds.add(embed);
        setEmbeds(embeds);

        // Simple text format for compatibility
        setText(String.format("%s reached level %d in %s!", 
            skill.getPlayerName(), skill.getLevel(), skill.getSkillName()));
    }
}