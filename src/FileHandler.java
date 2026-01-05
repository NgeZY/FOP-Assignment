import java.io.*;
import java.util.*;

public class FileHandler {
    private static final String EVENT_FILE = "event.csv";
    private static final String RECUR_FILE = "recurrent.csv";
    private static final String ADD_FILE = "additional.csv";

    // --- Generic Loaders ---
    public List<Event> loadEvents() {
        return loadList(EVENT_FILE, Event::fromCSV);
    }
    public List<Recurrence> loadRecurrences() {
        return loadList(RECUR_FILE, Recurrence::fromCSV);
    }
    public List<AdditionalInfo> loadAdditional() {
        return loadList(ADD_FILE, AdditionalInfo::fromCSV);
    }

    private <T> List<T> loadList(String filename, java.util.function.Function<String, T> mapper) {
        List<T> list = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                T obj = mapper.apply(line);
                if (obj != null) list.add(obj);
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }

    // --- Generic Savers ---
    public void saveEvents(List<Event> list) {
        saveList(EVENT_FILE, "eventId, title, description, startDateTime, endDateTime", list, Event::toCSV);
    }
    public void saveRecurrences(List<Recurrence> list) {
        saveList(RECUR_FILE, "eventId, recurrentInterval, recurrentTimes, recurrentEndDate", list, Recurrence::toCSV);
    }
    public void saveAdditional(List<AdditionalInfo> list) {
        saveList(ADD_FILE, "eventId, location, category", list, AdditionalInfo::toCSV);
    }

    private <T> void saveList(String filename, String header, List<T> list, java.util.function.Function<T, String> mapper) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println(header);
            for (T item : list) pw.println(mapper.apply(item));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Backup & Restore (Unified File) ---
    public void backupData(String path, List<Event> ev, List<Recurrence> rec, List<AdditionalInfo> add) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("###EVENTS###");
            for (Event e : ev) pw.println(e.toCSV());

            pw.println("###RECURRENCE###");
            for (Recurrence r : rec) pw.println(r.toCSV());

            pw.println("###ADDITIONAL###");
            for (AdditionalInfo a : add) pw.println(a.toCSV());

        } catch (IOException e) { System.out.println("Backup Error: " + e.getMessage()); }
    }

    public void restoreData(String path) {
        List<Event> ev = new ArrayList<>();
        List<Recurrence> rec = new ArrayList<>();
        List<AdditionalInfo> add = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            String mode = "";
            while ((line = br.readLine()) != null) {
                if (line.startsWith("###")) {
                    mode = line; continue;
                }
                if (mode.equals("###EVENTS###")) {
                    Event e = Event.fromCSV(line);
                    if(e!=null) ev.add(e);
                } else if (mode.equals("###RECURRENCE###")) {
                    Recurrence r = Recurrence.fromCSV(line);
                    if(r!=null) rec.add(r);
                } else if (mode.equals("###ADDITIONAL###")) {
                    AdditionalInfo a = AdditionalInfo.fromCSV(line);
                    if(a!=null) add.add(a);
                }
            }
            saveEvents(ev);
            saveRecurrences(rec);
            saveAdditional(add);
        } catch (IOException e) { System.out.println("Restore Error: " + e.getMessage()); }
    }
}