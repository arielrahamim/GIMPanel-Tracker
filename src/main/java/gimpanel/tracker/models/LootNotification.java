package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class LootNotification extends WebhookPayload
{
    public LootNotification(DropData drop)
    {
        super("LOOT", drop.getPlayerName());
        
        // Dink-compatible extra data
        Map<String, Object> extra = new HashMap<>();
        extra.put("source", drop.getSource());
        extra.put("category", "LOOT");
        
        // Items array (Dink format)
        Map<String, Object> item = new HashMap<>();
        item.put("id", drop.getItemId());
        item.put("name", drop.getItemName());
        item.put("quantity", drop.getQuantity());
        item.put("priceEach", drop.getValue());
        item.put("rarity", drop.getRarity());
        
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item);
        extra.put("items", items);
        
        setExtra(extra);

        // Discord embed for rich display
        WebhookEmbed embed = new WebhookEmbed();
        embed.setTitle("💎 Loot Received!");
        embed.setDescription(String.format("%s received **%s** x%d from %s", 
            drop.getPlayerName(), drop.getItemName(), drop.getQuantity(), drop.getSource()));

        // Color based on rarity
        switch (drop.getRarity().toLowerCase())
        {
            case "legendary":
            case "ultra_rare":
                embed.setColor(0xFF4500); // Orange-red for legendary
                embed.setTitle("🌟 LEGENDARY DROP!");
                break;
            case "epic":
            case "very_rare":
                embed.setColor(0x9932CC); // Purple for epic
                embed.setTitle("⭐ Epic Drop!");
                break;
            case "rare":
                embed.setColor(0x0080FF); // Blue for rare
                embed.setTitle("💎 Rare Drop!");
                break;
            case "uncommon":
                embed.setColor(0x32CD32); // Green for uncommon
                break;
            default:
                embed.setColor(0x808080); // Gray for common
                break;
        }

        List<WebhookEmbed.WebhookEmbedField> fields = new ArrayList<>();
        fields.add(new WebhookEmbed.WebhookEmbedField("Item", drop.getItemName(), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Quantity", String.valueOf(drop.getQuantity()), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Source", drop.getSource(), true));
        fields.add(new WebhookEmbed.WebhookEmbedField("Rarity", capitalizeFirst(drop.getRarity()), true));
        
        if (drop.getValue() > 0)
        {
            fields.add(new WebhookEmbed.WebhookEmbedField("Value", String.format("%,d gp", drop.getValue()), true));
        }
        
        if (drop.getLocation() != null && !drop.getLocation().equals("Unknown"))
        {
            fields.add(new WebhookEmbed.WebhookEmbedField("Location", drop.getLocation(), true));
        }

        embed.setFields(fields);
        
        WebhookEmbed.WebhookEmbedFooter footer = new WebhookEmbed.WebhookEmbedFooter();
        footer.setText("GIMPanel Tracker");
        embed.setFooter(footer);

        List<WebhookEmbed> embeds = new ArrayList<>();
        embeds.add(embed);
        setEmbeds(embeds);

        // Simple text format for compatibility
        setText(String.format("%s received %s x%d from %s (Value: %,d gp)", 
            drop.getPlayerName(), drop.getItemName(), drop.getQuantity(), drop.getSource(), drop.getValue()));
    }

    private String capitalizeFirst(String str)
    {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replace("_", " ");
    }
}