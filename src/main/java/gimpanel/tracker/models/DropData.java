package gimpanel.tracker.models;

import lombok.Data;

@Data
public class DropData
{
    private String playerName;
    private String itemName;
    private int itemId;
    private int quantity;
    private String source;
    private String rarity;
    private long value;
    private String location;
    private long timestamp;

    public enum Rarity
    {
        COMMON("common"),
        UNCOMMON("uncommon"),
        RARE("rare"),
        VERY_RARE("very_rare"),
        ULTRA_RARE("ultra_rare");

        private final String value;

        Rarity(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

        public static Rarity fromString(String rarity)
        {
            for (Rarity r : Rarity.values())
            {
                if (r.value.equalsIgnoreCase(rarity))
                {
                    return r;
                }
            }
            return COMMON;
        }
    }

    public DropData()
    {
        this.timestamp = System.currentTimeMillis();
    }

    public DropData(String playerName, String itemName, int itemId, int quantity, String source)
    {
        this.playerName = playerName;
        this.itemName = itemName;
        this.itemId = itemId;
        this.quantity = quantity;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }
}