package D0828.two;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

// Custom class to hold stock data
class StockData {
    String symbol;
    String date; // yyyy/MM/dd
    String time; // HH:mm:ss
    double open;
    double high;
    double low;
    double close;
    long volume;

    public StockData(String symbol, String date, String time, double open, double high, double low, double close, long volume) {
        this.symbol = symbol;
        this.date = date;
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    // For display purposes
    @Override
    public String toString() {
        return symbol + "," + date + "," + time + "," + open + "," + high + "," + low + "," + close + "," + volume;
    }
}

// Main application class
public class StockDataProcessor extends JFrame {
    private HashMap<String, List<StockData>> dataMap = new HashMap<>();
    private JTable displayTable;
    private DefaultTableModel tableModel;
    private JTextField dateField, startDateField, endDateField, lengthField, singleDateTimeField, rangeDateStartTimeField, rangeDateEndTimeField;
    private JTextArea outputArea;
    private List<StockData> currentQueryResults = new ArrayList<>(); // To hold results for export

    public StockDataProcessor() {
        setTitle("臺灣股票分鐘資料處理器");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu for loading file
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("檔案");
        JMenuItem loadItem = new JMenuItem("載入 CSV");
        loadItem.addActionListener(new LoadFileListener());
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Panel for inputs
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Function 3: Single day query
        inputPanel.add(new JLabel("單一日期 (yyyy/MM/dd):"));
        dateField = new JTextField();
        inputPanel.add(dateField);
        JButton singleDayBtn = new JButton("查詢單一日期");
        singleDayBtn.addActionListener(new SingleDayQueryListener());
        inputPanel.add(singleDayBtn);
        inputPanel.add(new JLabel("")); // 空白佔位

        // Function 4: Multi-day query
        inputPanel.add(new JLabel("開始日期 (yyyy/MM/dd):"));
        startDateField = new JTextField();
        inputPanel.add(startDateField);
        inputPanel.add(new JLabel("結束日期 (yyyy/MM/dd):"));
        endDateField = new JTextField();
        inputPanel.add(endDateField);
        inputPanel.add(new JLabel("或使用天數:"));
        lengthField = new JTextField();
        inputPanel.add(lengthField);
        JButton multiDayBtn = new JButton("查詢多日資料");
        multiDayBtn.addActionListener(new MultiDayQueryListener());
        inputPanel.add(multiDayBtn);
        inputPanel.add(new JLabel("")); // 空白佔位

        // Function 5: Single date + time
        inputPanel.add(new JLabel("日期時間 (yyyy/MM/dd HH:mm:ss):"));
        singleDateTimeField = new JTextField();
        inputPanel.add(singleDateTimeField);
        JButton singleTimeBtn = new JButton("查詢單一時間點");
        singleTimeBtn.addActionListener(new SingleTimeQueryListener());
        inputPanel.add(singleTimeBtn);
        inputPanel.add(new JLabel("")); // 空白佔位

        // Function 6: Date + time range
        inputPanel.add(new JLabel("開始日期時間 (yyyy/MM/dd HH:mm:ss):"));
        rangeDateStartTimeField = new JTextField();
        inputPanel.add(rangeDateStartTimeField);
        inputPanel.add(new JLabel("結束日期時間 (yyyy/MM/dd HH:mm:ss):"));
        rangeDateEndTimeField = new JTextField();
        inputPanel.add(rangeDateEndTimeField);
        JButton rangeTimeBtn = new JButton("查詢時間範圍");
        rangeTimeBtn.addActionListener(new TimeRangeQueryListener());
        inputPanel.add(rangeTimeBtn);
        inputPanel.add(new JLabel("")); // 空白佔位

        // Function 7: Export
        JButton exportBtn = new JButton("匯出目前結果至 CSV");
        exportBtn.addActionListener(new ExportListener());
        inputPanel.add(exportBtn);

        add(inputPanel, BorderLayout.NORTH);

        // Table for displaying data
        tableModel = new DefaultTableModel(new String[]{"股票代號", "日期", "時間", "開盤價", "最高價", "最低價", "收盤價", "成交量"}, 0);
        displayTable = new JTable(tableModel);
        displayTable.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        displayTable.getTableHeader().setFont(new Font("微軟正黑體", Font.BOLD, 14));
        add(new JScrollPane(displayTable), BorderLayout.CENTER);

        // Text area for aggregated results (for func 5 & 6)
        outputArea = new JTextArea(5, 20);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        setVisible(true);
    }

    // Normalize date to yyyy/MM/dd
    private String normalizeDate(String date) {
        String[] parts = date.split(Pattern.quote("/"));
        if (parts.length == 3) {
            return String.format("%04d/%02d/%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        return date;
    }

    // Load CSV file
    class LoadFileListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    br.readLine(); // Skip header
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split(",");
                        if (parts.length < 8) continue;
                        String symbol = parts[0].trim();
                        String date = normalizeDate(parts[1].trim());
                        String time = parts[2].trim();
                        double open = Double.parseDouble(parts[3].trim());
                        double high = Double.parseDouble(parts[4].trim());
                        double low = Double.parseDouble(parts[5].trim());
                        double close = Double.parseDouble(parts[6].trim());
                        long volume = Long.parseLong(parts[7].trim());

                        StockData data = new StockData(symbol, date, time, open, high, low, close, volume);
                        dataMap.computeIfAbsent(date, k -> new ArrayList<>()).add(data);
                    }
                    // Sort each day's list by time
                    for (List<StockData> list : dataMap.values()) {
                        list.sort(Comparator.comparing(d -> d.time));
                    }
                    JOptionPane.showMessageDialog(null, "資料載入成功！");
                } catch (IOException | NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "載入檔案時發生錯誤: " + ex.getMessage());
                }
            }
        }
    }

    // Function 3: Single day query
    class SingleDayQueryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String date = normalizeDate(dateField.getText().trim());
            List<StockData> data = dataMap.get(date);
            displayData(data);
        }
    }

    // Function 4: Multi-day query
    class MultiDayQueryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String startDateStr = normalizeDate(startDateField.getText().trim());
            List<StockData> results = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            try {
                Date startDate = sdf.parse(startDateStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);

                if (!endDateField.getText().trim().isEmpty()) {
                    String endDateStr = normalizeDate(endDateField.getText().trim());
                    Date endDate = sdf.parse(endDateStr);
                    while (!cal.getTime().after(endDate)) {
                        String curDate = sdf.format(cal.getTime());
                        if (dataMap.containsKey(curDate)) {
                            results.addAll(dataMap.get(curDate));
                        }
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } else if (!lengthField.getText().trim().isEmpty()) {
                    int length = Integer.parseInt(lengthField.getText().trim());
                    for (int i = 0; i < length; i++) {
                        String curDate = sdf.format(cal.getTime());
                        if (dataMap.containsKey(curDate)) {
                            results.addAll(dataMap.get(curDate));
                        }
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
                displayData(results);
            } catch (ParseException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "無效的日期或天數: " + ex.getMessage());
            }
        }
    }

    // Function 5: Single date + time
    class SingleTimeQueryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String input = singleDateTimeField.getText().trim();
            String[] parts = input.split(" ");
            if (parts.length != 2) return;
            String date = normalizeDate(parts[0]);
            String time = parts[1];
            List<StockData> dayData = dataMap.get(date);
            if (dayData != null) {
                for (StockData data : dayData) {
                    if (data.time.equals(time)) {
                        outputArea.setText("開盤價: " + data.open + "\n最高價: " + data.high + "\n最低價: " + data.low + "\n收盤價: " + data.close + "\n成交量: " + data.volume);
                        currentQueryResults.clear();
                        currentQueryResults.add(data);
                        tableModel.setRowCount(0); // Clear table for aggregated view
                        return;
                    }
                }
            }
            outputArea.setText("找不到 " + input + " 的資料");
        }
    }

    // Function 6: Date + time range (aggregated OHLCV)
    class TimeRangeQueryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String startInput = rangeDateStartTimeField.getText().trim();
            String endInput = rangeDateEndTimeField.getText().trim();
            String[] startParts = startInput.split(" ");
            String[] endParts = endInput.split(" ");
            if (startParts.length != 2 || endParts.length != 2) return;
            String startDate = normalizeDate(startParts[0]);
            String startTime = startParts[1];
            String endDate = normalizeDate(endParts[0]);
            String endTime = endParts[1];

            List<StockData> rangeData = getDataInRange(startDate, startTime, endDate, endTime);
            if (!rangeData.isEmpty()) {
                double open = rangeData.get(0).open;
                double high = rangeData.stream().mapToDouble(d -> d.high).max().getAsDouble();
                double low = rangeData.stream().mapToDouble(d -> d.low).min().getAsDouble();
                double close = rangeData.get(rangeData.size() - 1).close;
                long volume = rangeData.stream().mapToLong(d -> d.volume).sum();

                outputArea.setText("彙總結果:\n開盤價: " + open + "\n最高價: " + high + "\n最低價: " + low + "\n收盤價: " + close + "\n成交量: " + volume);
                currentQueryResults = rangeData; // For export, keep full list
                displayData(rangeData); // Display full list in table
            } else {
                outputArea.setText("範圍內沒有資料");
            }
        }
    }

    // Helper to get data in time range (assuming single day for simplicity, extend if needed)
    private List<StockData> getDataInRange(String startDate, String startTime, String endDate, String endTime) {
        List<StockData> results = new ArrayList<>();
        if (!startDate.equals(endDate)) {
            // For multi-day, but for simplicity, assume same day or handle sequentially
            // Implement multi-day if needed, but query implies same date
            return results; // Placeholder, extend as per need
        }
        List<StockData> dayData = dataMap.get(startDate);
        if (dayData != null) {
            boolean started = false;
            for (StockData data : dayData) {
                if (!started && data.time.compareTo(startTime) >= 0) {
                    started = true;
                }
                if (started) {
                    results.add(data);
                    if (data.time.compareTo(endTime) > 0) {
                        results.remove(results.size() - 1); // Exclude if beyond
                        break;
                    }
                }
            }
        }
        return results;
    }

    // Display list in table
    private void displayData(List<StockData> data) {
        tableModel.setRowCount(0);
        outputArea.setText("");
        if (data != null) {
            for (StockData d : data) {
                tableModel.addRow(new Object[]{d.symbol, d.date, d.time, d.open, d.high, d.low, d.close, d.volume});
            }
            currentQueryResults = data;
        }
    }

    // Function 7: Export to CSV (all fields for now, can add selection later)
    class ExportListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentQueryResults.isEmpty()) {
                JOptionPane.showMessageDialog(null, "沒有資料可以匯出");
                return;
            }
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write("Symbol,Date,Time,Open,High,Low,Close,Volume\n");
                    for (StockData d : currentQueryResults) {
                        bw.write(d.toString() + "\n");
                    }
                    JOptionPane.showMessageDialog(null, "匯出成功！");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "匯出時發生錯誤: " + ex.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StockDataProcessor::new);
    }
}