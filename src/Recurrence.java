import java.time.LocalDate;

public class Recurrence {
    private int eventId;
    private String interval; // "1d", "1w"
    private int times;       // 0 if using endDate
    private LocalDate endDate; // null if using times

    public Recurrence(int eventId, String interval, int times, LocalDate endDate) {
        this.eventId = eventId;
        this.interval = interval;
        this.times = times;
        this.endDate = endDate;
    }

    public int getEventId() { return eventId; }
    public String getInterval() { return interval; }
    public int getTimes() { return times; }
    public LocalDate getEndDate() { return endDate; }

    public String toCSV() {
        String dateStr = (endDate == null) ? "0" : endDate.toString();
        return eventId + "," + interval + "," + times + "," + dateStr;
    }

    public static Recurrence fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length < 4) return null;
        try {
            LocalDate date = parts[3].trim().equals("0") ? null : LocalDate.parse(parts[3].trim());
            return new Recurrence(
                    Integer.parseInt(parts[0].trim()),
                    parts[1].trim(),
                    Integer.parseInt(parts[2].trim()),
                    date
            );
        } catch (Exception e) { return null; }
    }
}