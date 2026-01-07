import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarManager {
    private List<Event> events;
    private List<Recurrence> recurrences;
    private List<AdditionalInfo> additionalInfos;
    private FileHandler fileHandler;

    public CalendarManager() {
        this.fileHandler = new FileHandler();
        refresh();
    }

    public void refresh() {
        this.events = fileHandler.loadEvents();
        this.recurrences = fileHandler.loadRecurrences();
        this.additionalInfos = fileHandler.loadAdditional();
    }

    // --- Core Logic: Add Event with Conflict Detection ---
    public String createEvent(String title, String desc, LocalDateTime start, LocalDateTime end,
                              String recInt, int recTimes, LocalDate recEnd,
                              String loc, String cat) {

        // 1. Conflict Detection (Feature: Conflict Detection)
        if (hasConflict(start, end)) {
            return "CONFLICT: Event overlaps with an existing event!";
        }

        int newId = events.stream().mapToInt(Event::getId).max().orElse(0) + 1;

        Event newEvent = new Event(newId, title, desc, start, end);
        events.add(newEvent);

        if (recInt != null && !recInt.equals("none")) {
            recurrences.add(new Recurrence(newId, recInt, recTimes, recEnd));
        }

        if (loc != null || cat != null) {
            additionalInfos.add(new AdditionalInfo(newId, loc == null ? "" : loc, cat == null ? "" : cat));
        }

        saveAll();
        return "SUCCESS";
    }

    private boolean hasConflict(LocalDateTime start, LocalDateTime end) {
        for (Event e : events) {
            // Check overlap: (StartA < EndB) and (EndA > StartB)
            if (start.isBefore(e.getEndDateTime()) && end.isAfter(e.getStartDateTime())) {
                return true;
            }
            // Note: Advanced conflict detection for recurring instances is complex; 
            // checking root events is sufficient for this scope.
        }
        return false;
    }

    public void deleteEvent(int id) {
        events.removeIf(e -> e.getId() == id);
        recurrences.removeIf(r -> r.getEventId() == id);
        additionalInfos.removeIf(a -> a.getEventId() == id);
        saveAll();
    }

    public void saveAll() {
        fileHandler.saveEvents(events);
        fileHandler.saveRecurrences(recurrences);
        fileHandler.saveAdditional(additionalInfos);
    }

    public void backup(String path) { fileHandler.backupData(path, events, recurrences, additionalInfos); }
    public void restore(String path) { fileHandler.restoreData(path); refresh(); }

    // --- Helper: Expand Recurring Events for a Date ---
    public List<Event> getEventsForDate(LocalDate date) {
        List<Event> dailyEvents = new ArrayList<>();
        for (Event e : events) {
            if (isEventOnDate(e, date)) {
                dailyEvents.add(e);
            }
        }
        return dailyEvents;
    }

    // In CalendarManager.java

    private boolean isEventOnDate(Event e, LocalDate target) {
        LocalDate start = e.getStartDateTime().toLocalDate();
        if (start.equals(target)) return true; // Exact match

        Recurrence r = recurrences.stream().filter(rec -> rec.getEventId() == e.getId()).findFirst().orElse(null);
        if (r == null || target.isBefore(start)) return false;

        long diff = ChronoUnit.DAYS.between(start, target);
        boolean intervalMatch = false;

        if (r.getInterval().equals("Daily")) intervalMatch = true;
        else if (r.getInterval().equals("Weekly")) intervalMatch = (diff % 7 == 0);

        if (intervalMatch) {
            // Check stop limits
            if (r.getEndDate() != null) return !target.isAfter(r.getEndDate());

            // BUG FIX HERE:
            if (r.getTimes() > 0) {
                long count = r.getInterval().equals("Weekly") ? diff / 7 : diff;
                // Change '<' to '<=' so that 'times=1' allows exactly 1 repetition.
                return count <= r.getTimes();
            }
            return true; // No limit (Infinite if times is 0 and date is null)
        }
        return false;
    }

    // --- Feature: Reminders (Upcoming events) ---
    public String getUpcomingReminders() {
        StringBuilder sb = new StringBuilder();
        LocalDate today = LocalDate.now();
        List<Event> todaysEvents = getEventsForDate(today);

        if (todaysEvents.isEmpty()) return "No events for today.";

        sb.append("You have ").append(todaysEvents.size()).append(" event(s) today:\n");
        for (Event e : todaysEvents) {
            sb.append("- ").append(e.getTitle()).append(" at ").append(e.getStartDateTime().toLocalTime()).append("\n");
        }
        return sb.toString();
    }

    // --- Feature: Statistics ---
    public String getStatistics() {
        // Find busiest day of the week based on ALL stored events (simplified)
        Map<DayOfWeek, Integer> freq = new HashMap<>();
        for (Event e : events) {
            DayOfWeek dow = e.getStartDateTime().getDayOfWeek();
            freq.put(dow, freq.getOrDefault(dow, 0) + 1);
        }

        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> "Busiest Day: " + entry.getKey() + " (" + entry.getValue() + " events)")
                .orElse("Not enough data for statistics.");
    }

    // --- Feature: Advanced Search ---
    public List<Event> search(String query) {
        String q = query.toLowerCase();
        List<Event> results = new ArrayList<>();

        for (Event e : events) {
            // Find associated additional info (Location/Category)
            AdditionalInfo info = additionalInfos.stream()
                    .filter(a -> a.getEventId() == e.getId()).findFirst().orElse(null);

            String cat = (info != null) ? info.getCategory().toLowerCase() : "";
            String loc = (info != null) ? info.getLocation().toLowerCase() : "";

            // Search in Title, Category, OR Location
            if (e.getTitle().toLowerCase().contains(q) || cat.contains(q) || loc.contains(q)) {
                results.add(e);
            }
        }
        return results;
    }

    public AdditionalInfo getAdditionalInfo(int eventId) {
        return additionalInfos.stream().filter(a -> a.getEventId() == eventId).findFirst().orElse(null);
    }

    // Add this inside CalendarManager.java

    public void updateEvent(int id, String title, String desc, LocalDateTime start, LocalDateTime end,
                            String recInt, int recTimes, LocalDate recEnd,
                            String loc, String cat) {

        // 1. Replace the Core Event object
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId() == id) {
                events.set(i, new Event(id, title, desc, start, end));
                break;
            }
        }

        // 2. Update Recurrence (Remove old, add new if exists)
        recurrences.removeIf(r -> r.getEventId() == id);
        if (recInt != null && !recInt.equals("none")) {
            recurrences.add(new Recurrence(id, recInt, recTimes, recEnd));
        }

        // 3. Update Additional Info (Remove old, add new)
        additionalInfos.removeIf(a -> a.getEventId() == id);
        if (loc != null || cat != null) {
            additionalInfos.add(new AdditionalInfo(id, loc == null ? "" : loc, cat == null ? "" : cat));
        }

        saveAll();
    }

    // Helper to get a single event by ID (needed for the GUI)
    public Event getEventById(int id) {
        return events.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    // Helper to get Recurrence by ID
    public Recurrence getRecurrence(int eventId) {
        return recurrences.stream().filter(r -> r.getEventId() == eventId).findFirst().orElse(null);
    }
}