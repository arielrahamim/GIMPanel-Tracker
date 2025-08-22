package gimpanel.tracker.models;

import lombok.Data;

@Data
public class QuestData
{
    private String playerName;
    private String questName;
    private String status;
    private int questPoints;
    private long completedAt;
    private boolean isCompleted;

    public enum QuestStatus
    {
        NOT_STARTED("NOT_STARTED"),
        IN_PROGRESS("IN_PROGRESS"),
        COMPLETED("COMPLETED");

        private final String value;

        QuestStatus(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

        public static QuestStatus fromString(String status)
        {
            for (QuestStatus s : QuestStatus.values())
            {
                if (s.value.equalsIgnoreCase(status))
                {
                    return s;
                }
            }
            return NOT_STARTED;
        }
    }

    public QuestData()
    {
    }

    public QuestData(String playerName, String questName, QuestStatus status)
    {
        this.playerName = playerName;
        this.questName = questName;
        this.status = status.getValue();
        this.isCompleted = status == QuestStatus.COMPLETED;
        if (this.isCompleted)
        {
            this.completedAt = System.currentTimeMillis();
        }
    }
}