import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
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
        JDialog d = new JDialog(this, "Add New Event", true);
        d.setSize(500, 550);
        d.setLayout(new GridBagLayout());
        d.setLocationRelativeTo(this);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleF = new JTextField(15);
        JTextField descF = new JTextField(15);

        // 1. DATE: Standard Date Spinners
        JSpinner startDateSpinner = createDateSpinner();
        JSpinner endDateSpinner = createDateSpinner();

        // 2. TIME: Time Spinners (Precision: Minutes)
        JSpinner startTimeSpinner = createTimeSpinner();
        JSpinner endTimeSpinner = createTimeSpinner();

        // Default End Time = Start Time + 1 hour
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0); // Optional: round to neat hour
        endTimeSpinner.setValue(cal.getTime());

        // Recurrence & Extra Fields
        JComboBox<String> recurBox = new JComboBox<>(new String[]{"none", "1d", "1w"});
        JTextField recTimesF = new JTextField("0");
        JTextField recEndF = new JTextField("YYYY-MM-DD");
        JTextField locF = new JTextField();
        JTextField catF = new JTextField();

        // --- Layout ---
        gbc.gridx = 0; gbc.gridy = 0; d.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; d.add(titleF, gbc);

        gbc.gridx = 0; gbc.gridy = 1; d.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; d.add(descF, gbc);

        // Start Row
        gbc.gridx = 0; gbc.gridy = 2; d.add(new JLabel("Start:"), gbc);
        gbc.gridx = 1;
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        startPanel.add(startDateSpinner);
        startPanel.add(Box.createHorizontalStrut(10));
        startPanel.add(startTimeSpinner);
        d.add(startPanel, gbc);

        // End Row
        gbc.gridx = 0; gbc.gridy = 3; d.add(new JLabel("End:"), gbc);
        gbc.gridx = 1;
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        endPanel.add(endDateSpinner);
        endPanel.add(Box.createHorizontalStrut(10));
        endPanel.add(endTimeSpinner);
        d.add(endPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; d.add(new JLabel("Recurrence:"), gbc);
        gbc.gridx = 1; d.add(recurBox, gbc);

        gbc.gridx = 0; gbc.gridy = 5; d.add(new JLabel("Repeat Times (0 if date):"), gbc);
        gbc.gridx = 1; d.add(recTimesF, gbc);

        gbc.gridx = 0; gbc.gridy = 6; d.add(new JLabel("Stop Repeating Date:"), gbc);
        gbc.gridx = 1; d.add(recEndF, gbc);

        gbc.gridx = 0; gbc.gridy = 7; d.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; d.add(locF, gbc);

        gbc.gridx = 0; gbc.gridy = 8; d.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; d.add(catF, gbc);

        JButton saveBtn = new JButton("Save Event");
        saveBtn.setBackground(new Color(100, 200, 100));
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        d.add(saveBtn, gbc);

        // --- Save Action ---
        saveBtn.addActionListener(e -> {
            try {
                // Merge Date Spinner + Time Spinner
                LocalDateTime start = getLocalDateTime(startDateSpinner, startTimeSpinner);
                LocalDateTime end = getLocalDateTime(endDateSpinner, endTimeSpinner);

                LocalDate rEnd = null;
                if (!recEndF.getText().trim().equals("YYYY-MM-DD") && !recEndF.getText().trim().isEmpty()) {
                    rEnd = LocalDate.parse(recEndF.getText().trim());
                }

                String res = manager.createEvent(
                        titleF.getText(), descF.getText(), start, end,
                        (String)recurBox.getSelectedItem(),
                        Integer.parseInt(recTimesF.getText()), rEnd,
                        locF.getText(), catF.getText()
                );

                if (res.equals("SUCCESS")) {
                    d.dispose();
                    refreshCalendar();
                    JOptionPane.showMessageDialog(this, "Event Created!");
                } else {
                    JOptionPane.showMessageDialog(d, res, "Conflict", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Error: " + ex.getMessage());
            }
        });

        d.setVisible(true);
    }

    private void showSearchDialog() {
        String q = JOptionPane.showInputDialog(this, "Search Title, Category, or Location:");
        if (q != null && !q.trim().isEmpty()) {
            // Call the new Manager method returning Objects
            List<Event> results = manager.search(q);

            // Call the new GUI method to show the table
            showSearchResultsDialog(results);
        }
    }

    private void showSearchResultsDialog(List<Event> results) {
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events found matching your search.");
            return;
        }

        JDialog d = new JDialog(this, "Search Results", true);
        d.setSize(700, 450); // Slightly larger to fit columns nicely
        d.setLocationRelativeTo(this);

        // 1. Define Table Columns
        // We include "ID" as the first column so we can identify the event programmatically
        String[] columns = {"ID", "Date", "Time", "Title", "Category", "Location"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            // Prevent user from editing cells directly
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 2. Formatters
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        // 3. Populate Rows
        for (Event e : results) {
            AdditionalInfo info = manager.getAdditionalInfo(e.getId());
            model.addRow(new Object[]{
                    e.getId(),                                  // Column 0: ID
                    e.getStartDateTime().format(dateFmt),       // Column 1: Date
                    e.getStartDateTime().format(timeFmt),       // Column 2: Time
                    e.getTitle(),                               // Column 3: Title
                    (info != null ? info.getCategory() : "-"),  // Column 4: Category
                    (info != null ? info.getLocation() : "-")   // Column 5: Location
            });
        }

        // 4. Create Table
        JTable table = new JTable(model);
        table.setRowHeight(25);

        // Hide the "ID" column (Column 0) from view, but keep data for logic
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Set Column Widths for better readability
        table.getColumnModel().getColumn(1).setPreferredWidth(90);  // Date
        table.getColumnModel().getColumn(2).setPreferredWidth(60);  // Time
        table.getColumnModel().getColumn(3).setPreferredWidth(200); // Title

        d.add(new JScrollPane(table), BorderLayout.CENTER);

        // 5. Button Panel
        JPanel btnPanel = new JPanel();
        JButton goToBtn = new JButton("Go to Date");
        JButton closeBtn = new JButton("Close");

        // --- LOGIC: Jump Calendar to Selected Event ---
        goToBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                // Get the date string from Column 1
                String dateStr = (String) model.getValueAt(row, 1);
                LocalDate date = LocalDate.parse(dateStr);

                // Update the main calendar view to that month
                currentYearMonth = YearMonth.from(date);
                refreshCalendar();

                d.dispose(); // Close the search window
            } else {
                JOptionPane.showMessageDialog(d, "Please select an event row first.");
            }
        });

        closeBtn.addActionListener(e -> d.dispose());

        btnPanel.add(goToBtn);
        btnPanel.add(closeBtn);

        d.add(btnPanel, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void performBackup() {
        String f = JOptionPane.showInputDialog("Backup Filename:");
        if (f != null) { manager.backup(f); JOptionPane.showMessageDialog(this, "Backup Done!"); }
    }

    private void performRestore() {
        String f = JOptionPane.showInputDialog("Restore Filename:");
        if (f != null) { manager.restore(f); refreshCalendar(); JOptionPane.showMessageDialog(this, "Restored!"); }
    }

    // Helper: Create a Spinner for Dates (YYYY-MM-DD)
    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        return spinner;
    }

    // Helper: Create a Spinner for Time (HH:mm) with 1-minute precision
    private JSpinner createTimeSpinner() {
        // 1. Current time rounded to start of minute
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // 2. Create Model with MINUTE step size
        // properties: value, start, end, stepSize
        SpinnerDateModel model = new SpinnerDateModel(cal.getTime(), null, null, Calendar.MINUTE);

        JSpinner spinner = new JSpinner(model);

        // 3. Format as HH:mm
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm");
        spinner.setEditor(editor);

        return spinner;
    }

    // Helper: Create a Dropdown that lists 15-min intervals,
// BUT defaults to the exact 'targetTime' (e.g. 4:05)
    // Helper: Create a Dropdown that lists 15-min intervals,
// BUT defaults to the exact 'targetTime' (e.g. 4:05)
    private JComboBox<String> createTimeComboBox(LocalTime targetTime) {
        JComboBox<String> combo = new JComboBox<>();
        LocalTime t = LocalTime.of(0, 0);

        // 1. Generate the standard dropdown list (00:00, 00:15, 00:30...)
        // This gives the user quick options if they want them.
        while (true) {
            combo.addItem(t.format(DateTimeFormatter.ofPattern("HH:mm")));
            t = t.plusMinutes(15);
            if (t.equals(LocalTime.of(0, 0))) break;
        }

        // 2. Allow the box to accept ANY text (like "16:07")
        combo.setEditable(true);

        // 3. Set the displayed value to the EXACT target time
        // Since it's editable, this works even if "16:05" isn't in the list above.
        String exactTime = targetTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        combo.setSelectedItem(exactTime);

        return combo;
    }

    // Helper: Merge Date Spinner + Time Spinner -> LocalDateTime
    private LocalDateTime getLocalDateTime(JSpinner dateSpinner, JSpinner timeSpinner) {
        // Get values as legacy java.util.Date objects
        java.util.Date datePart = (java.util.Date) dateSpinner.getValue();
        java.util.Date timePart = (java.util.Date) timeSpinner.getValue();

        // Convert to Java 8 LocalTime/LocalDate
        LocalDate localDate = datePart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalTime localTime = timePart.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

        // Combine them, stripping seconds to be clean
        return LocalDateTime.of(localDate, localTime.truncatedTo(ChronoUnit.MINUTES));
    }
}