public class AdditionalInfo {
    private int eventId;
    private String location;
    private String category;

    public AdditionalInfo(int eventId, String location, String category) {
        this.eventId = eventId;
        this.location = location;
        this.category = category;
    }

    public int getEventId() { return eventId; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }

    public String toCSV() {
        String safeLoc = location.replace(",", " ");
        String safeCat = category.replace(",", " ");
        return eventId + "," + safeLoc + "," + safeCat;
    }

    public static AdditionalInfo fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length < 3) return null;
        return new AdditionalInfo(
                Integer.parseInt(parts[0].trim()),
                parts[1].trim(),
                parts[2].trim()
        );
    }
}