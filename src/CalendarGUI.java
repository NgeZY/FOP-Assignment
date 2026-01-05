import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CalendarGUI extends JFrame {

    private CalendarManager manager;
    private YearMonth currentYearMonth;
    private JPanel calendarPanel;
    private JLabel monthLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarGUI().setVisible(true));
    }

    public CalendarGUI() {
        manager = new CalendarManager();
        currentYearMonth = YearMonth.now();

        setTitle("Calendar App");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        refreshCalendar();

        // Feature: Reminder Notification on Launch
        String reminders = manager.getUpcomingReminders();
        if (!reminders.contains("No events")) {
            JOptionPane.showMessageDialog(this, reminders, "Reminders", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // Navigation
        JPanel navPanel = new JPanel();
        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 18));

        prevBtn.addActionListener(e -> { currentYearMonth = currentYearMonth.minusMonths(1); refreshCalendar(); });
        nextBtn.addActionListener(e -> { currentYearMonth = currentYearMonth.plusMonths(1); refreshCalendar(); });

        navPanel.add(prevBtn); navPanel.add(monthLabel); navPanel.add(nextBtn);

        // Actions
        JPanel actionPanel = new JPanel();
        JButton addBtn = new JButton("Add Event");
        JButton searchBtn = new JButton("Search");
        JButton statsBtn = new JButton("Statistics"); // Feature: Stats
        JButton backupBtn = new JButton("Backup");
        JButton restoreBtn = new JButton("Restore");

        addBtn.addActionListener(e -> showAddEventDialog());
        searchBtn.addActionListener(e -> showSearchDialog());
        statsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, manager.getStatistics()));
        backupBtn.addActionListener(e -> performBackup());
        restoreBtn.addActionListener(e -> performRestore());

        actionPanel.add(addBtn); actionPanel.add(searchBtn); actionPanel.add(statsBtn);
        actionPanel.add(backupBtn); actionPanel.add(restoreBtn);

        topPanel.add(navPanel, BorderLayout.CENTER);
        topPanel.add(actionPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Calendar Grid
        calendarPanel = new JPanel(new GridLayout(0, 7));
        add(calendarPanel, BorderLayout.CENTER);
    }

    private void refreshCalendar() {
        calendarPanel.removeAll();
        monthLabel.setText(currentYearMonth.getMonth() + " " + currentYearMonth.getYear());

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            JLabel header = new JLabel(d, SwingConstants.CENTER);
            header.setBorder(BorderFactory.createEtchedBorder());
            calendarPanel.add(header);
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int emptySlots = firstOfMonth.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < emptySlots; i++) calendarPanel.add(new JLabel(""));

        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
            List<Event> events = manager.getEventsForDate(date);

            JPanel dayPanel = new JPanel(new BorderLayout());
            dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            dayPanel.setBackground(Color.WHITE);

            dayPanel.add(new JLabel(" " + day), BorderLayout.NORTH);

            if (!events.isEmpty()) {
                dayPanel.setBackground(new Color(220, 240, 255));
                JLabel marker = new JLabel(" " + events.size() + " Events");
                marker.setForeground(Color.BLUE);
                dayPanel.add(marker, BorderLayout.CENTER);
            }

            dayPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    showDayDetails(date, events);
                }
            });
            calendarPanel.add(dayPanel);
        }

        int totalSlots = emptySlots + currentYearMonth.lengthOfMonth();
        int remaining = 7 - (totalSlots % 7);
        if (remaining < 7) for (int i=0; i<remaining; i++) calendarPanel.add(new JLabel(""));

        calendarPanel.revalidate(); calendarPanel.repaint();
    }

    private void showDayDetails(LocalDate date, List<Event> events) {
        JDialog d = new JDialog(this, "Events: " + date, true);
        d.setSize(500, 300);
        d.setLocationRelativeTo(this);

        String[] columns = {"ID", "Time", "Title", "Category", "Location"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        for (Event e : events) {
            AdditionalInfo info = manager.getAdditionalInfo(e.getId());
            model.addRow(new Object[]{
                    e.getId(),
                    e.getStartDateTime().toLocalTime(),
                    e.getTitle(),
                    (info != null ? info.getCategory() : "-"),
                    (info != null ? info.getLocation() : "-")
            });
        }

        JTable table = new JTable(model);
        d.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton delBtn = new JButton("Delete Selected");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int id = (int) model.getValueAt(row, 0);
                manager.deleteEvent(id);
                d.dispose();
                refreshCalendar();
            }
        });
        d.add(delBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void showAddEventDialog() {
        JDialog d = new JDialog(this, "Add Event", true);
        d.setSize(400, 500);
        d.setLayout(new GridLayout(10, 2));
        d.setLocationRelativeTo(this);

        JTextField titleF = new JTextField();
        JTextField descF = new JTextField();
        JTextField startF = new JTextField(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        JTextField endF = new JTextField(LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"none", "1d", "1w"});
        JTextField recTimesF = new JTextField("0");
        JTextField recEndF = new JTextField("YYYY-MM-DD");
        // Feature: Additional Fields
        JTextField locF = new JTextField();
        JTextField catF = new JTextField();

        d.add(new JLabel("Title:")); d.add(titleF);
        d.add(new JLabel("Desc:")); d.add(descF);
        d.add(new JLabel("Start (ISO):")); d.add(startF);
        d.add(new JLabel("End (ISO):")); d.add(endF);
        d.add(new JLabel("Recur:")); d.add(recurBox);
        d.add(new JLabel("Recur Times:")); d.add(recTimesF);
        d.add(new JLabel("Recur End Date:")); d.add(recEndF);
        d.add(new JLabel("Location:")); d.add(locF);
        d.add(new JLabel("Category:")); d.add(catF);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            try {
                LocalDateTime s = LocalDateTime.parse(startF.getText());
                LocalDateTime en = LocalDateTime.parse(endF.getText());
                LocalDate rEnd = recEndF.getText().length() > 4 ? LocalDate.parse(recEndF.getText()) : null;

                String res = manager.createEvent(
                        titleF.getText(), descF.getText(), s, en,
                        (String)recurBox.getSelectedItem(),
                        Integer.parseInt(recTimesF.getText()), rEnd,
                        locF.getText(), catF.getText()
                );

                if (res.equals("SUCCESS")) {
                    d.dispose(); refreshCalendar();
                } else {
                    JOptionPane.showMessageDialog(d, res); // Conflict Warning
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(d, "Error: " + ex.getMessage()); }
        });
        d.add(saveBtn);
        d.setVisible(true);
    }

    private void showSearchDialog() {
        String q = JOptionPane.showInputDialog(this, "Search Title or Category:");
        if (q != null) {
            List<String> res = manager.search(q);
            JOptionPane.showMessageDialog(this, res.isEmpty() ? "No results" : String.join("\n", res));
        }
    }

    private void performBackup() {
        String f = JOptionPane.showInputDialog("Backup Filename:");
        if (f != null) { manager.backup(f); JOptionPane.showMessageDialog(this, "Backup Done!"); }
    }

    private void performRestore() {
        String f = JOptionPane.showInputDialog("Restore Filename:");
        if (f != null) { manager.restore(f); refreshCalendar(); JOptionPane.showMessageDialog(this, "Restored!"); }
    }
}