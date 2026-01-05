import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startDateTime = start;
        this.endDateTime = end;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }

    public String toCSV() {
        return id + "," + title + "," + description + "," +
                startDateTime.format(DATE_FMT) + "," + endDateTime.format(DATE_FMT);
    }

    public static Event fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length < 5) return null;
        try {
            return new Event(
                    Integer.parseInt(parts[0].trim()),
                    parts[1].trim(),
                    parts[2].trim(),
                    LocalDateTime.parse(parts[3].trim(), DATE_FMT),
                    LocalDateTime.parse(parts[4].trim(), DATE_FMT)
            );
        } catch (Exception e) { return null; }
    }

    @Override
    public String toString() {
        return String.format("%s (%s - %s)", title, startDateTime.toLocalTime(), endDateTime.toLocalTime());
    }
}