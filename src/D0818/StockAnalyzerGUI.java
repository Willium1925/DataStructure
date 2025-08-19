package D0818;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * 股票分析GUI程式
 * 提供股票成交量和成交金額的排行榜功能
 */
public class StockAnalyzerGUI extends JFrame {

    // GUI元件
    private JTextField stockCodeField;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField topNField;
    private JRadioButton volumeRadio;
    private JRadioButton amountRadio;
    private JRadioButton singleDateRadio;
    private JRadioButton dateRangeRadio;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton analyzeButton;

    // 資料格式
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 儲存股票資料的資料結構
    private List<StockRecord> stockData;

    /**
     * 股票記錄類別
     */
    static class StockRecord {
        String stockCode;
        LocalDate date;
        long volume;
        long amount;

        public StockRecord(String stockCode, LocalDate date, long volume, long amount) {
            this.stockCode = stockCode;
            this.date = date;
            this.volume = volume;
            this.amount = amount;
        }
    }

    /**
     * 建構函數 - 初始化GUI介面
     */
    public StockAnalyzerGUI() {
        setTitle("股票交易分析系統");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 載入股票資料
        loadStockData();

        // 建立GUI元件
        createComponents();

        // 設定視窗屬性
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    /**
     * 建立GUI元件
     */
    private void createComponents() {
        // 建立輸入面板
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.NORTH);

        // 建立結果表格
        createResultTable();
        JScrollPane scrollPane = new JScrollPane(resultTable);
        add(scrollPane, BorderLayout.CENTER);

        // 建立按鈕面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 建立輸入面板
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createTitledBorder("查詢條件"));

        // 股票代碼輸入
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("股票代碼:"), gbc);
        gbc.gridx = 1;
        stockCodeField = new JTextField(10);
        stockCodeField.setToolTipText("輸入股票代碼，留空表示查詢所有股票");
        panel.add(stockCodeField, gbc);

        // 日期查詢類型選擇
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("日期查詢:"), gbc);
        gbc.gridx = 1;
        JPanel dateTypePanel = new JPanel();
        singleDateRadio = new JRadioButton("單一日期", true);
        dateRangeRadio = new JRadioButton("日期區間");
        ButtonGroup dateGroup = new ButtonGroup();
        dateGroup.add(singleDateRadio);
        dateGroup.add(dateRangeRadio);
        dateTypePanel.add(singleDateRadio);
        dateTypePanel.add(dateRangeRadio);
        panel.add(dateTypePanel, gbc);

        // 開始日期
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("開始日期:"), gbc);
        gbc.gridx = 1;
        startDateField = new JTextField(10);
        startDateField.setToolTipText("格式: yyyy-MM-dd");
        panel.add(startDateField, gbc);

        // 結束日期
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("結束日期:"), gbc);
        gbc.gridx = 1;
        endDateField = new JTextField(10);
        endDateField.setToolTipText("格式: yyyy-MM-dd，單一日期查詢時可留空");
        panel.add(endDateField, gbc);

        // 排序方式選擇
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("排序依據:"), gbc);
        gbc.gridx = 1;
        JPanel sortPanel = new JPanel();
        volumeRadio = new JRadioButton("成交量", true);
        amountRadio = new JRadioButton("成交金額");
        ButtonGroup sortGroup = new ButtonGroup();
        sortGroup.add(volumeRadio);
        sortGroup.add(amountRadio);
        sortPanel.add(volumeRadio);
        sortPanel.add(amountRadio);
        panel.add(sortPanel, gbc);

        // 顯示前N名
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("顯示前N名:"), gbc);
        gbc.gridx = 1;
        topNField = new JTextField("10", 10);
        panel.add(topNField, gbc);

        return panel;
    }

    /**
     * 建立結果表格
     */
    private void createResultTable() {
        String[] columns = {"排名", "股票代碼", "成交量", "成交金額", "日期"};
        tableModel = new DefaultTableModel(columns, 0);
        resultTable = new JTable(tableModel);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * 建立按鈕面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        analyzeButton = new JButton("開始分析");
        analyzeButton.addActionListener(new AnalyzeButtonListener());
        panel.add(analyzeButton);

        JButton clearButton = new JButton("清除結果");
        clearButton.addActionListener(e -> tableModel.setRowCount(0));
        panel.add(clearButton);

        return panel;
    }

    /**
     * 載入股票資料從CSV檔案
     */
    private void loadStockData() {
        stockData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("stock_data.csv"))) {
            String line = reader.readLine(); // 跳過標題行

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String stockCode = parts[0].trim();
                    LocalDate date = LocalDate.parse(parts[1].trim(), DATE_FORMAT);
                    long volume = Long.parseLong(parts[2].trim());
                    long amount = Long.parseLong(parts[3].trim());

                    stockData.add(new StockRecord(stockCode, date, volume, amount));
                }
            }

            System.out.println("載入完成，共 " + stockData.size() + " 筆資料");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "無法載入資料檔案: " + e.getMessage(),
                    "錯誤",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 分析按鈕事件監聽器
     */
    private class AnalyzeButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            performAnalysis();
        }
    }

    /**
     * 執行股票分析
     */
    private void performAnalysis() {
        try {
            // 清除之前的結果
            tableModel.setRowCount(0);

            // 取得查詢參數
            String targetStockCode = stockCodeField.getText().trim();
            String startDateText = startDateField.getText().trim();
            String endDateText = endDateField.getText().trim();
            int topN = Integer.parseInt(topNField.getText().trim());
            boolean sortByVolume = volumeRadio.isSelected();
            boolean isSingleDate = singleDateRadio.isSelected();

            // 驗證輸入
            if (startDateText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "請輸入開始日期", "輸入錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate startDate = LocalDate.parse(startDateText, DATE_FORMAT);
            LocalDate endDate = isSingleDate ? startDate : LocalDate.parse(endDateText, DATE_FORMAT);

            // 過濾資料
            List<StockRecord> filteredData = filterStockData(targetStockCode, startDate, endDate);

            if (filteredData.isEmpty()) {
                JOptionPane.showMessageDialog(this, "沒有找到符合條件的資料", "查詢結果", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 依據股票代碼分組並計算總計
            Map<String, StockSummary> stockSummaryMap = groupAndSummarizeData(filteredData);

            // 排序資料
            List<StockSummary> sortedResults = sortStockSummaries(stockSummaryMap, sortByVolume, topN);

            // 顯示結果
            displayResults(sortedResults);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "分析過程發生錯誤: " + ex.getMessage(),
                    "錯誤",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 過濾股票資料
     * 根據股票代碼和日期範圍過濾資料
     */
    private List<StockRecord> filterStockData(String targetStockCode, LocalDate startDate, LocalDate endDate) {
        List<StockRecord> filtered = new ArrayList<>();

        // 逐一檢查每筆股票記錄
        for (StockRecord record : stockData) {
            // 檢查股票代碼條件（如果有指定的話）
            boolean stockCodeMatch = targetStockCode.isEmpty() || record.stockCode.equals(targetStockCode);

            // 檢查日期範圍條件
            // 使用compareTo方法比較日期：
            // - 如果record.date等於startDate，compareTo回傳0
            // - 如果record.date在startDate之後，compareTo回傳正數
            // - 如果record.date在startDate之前，compareTo回傳負數
            boolean dateInRange = (record.date.compareTo(startDate) >= 0) &&
                    (record.date.compareTo(endDate) <= 0);

            // 如果兩個條件都符合，則加入過濾後的清單
            if (stockCodeMatch && dateInRange) {
                filtered.add(record);
            }
        }

        return filtered;
    }

    /**
     * 股票摘要類別，用於儲存每支股票的統計資料
     */
    static class StockSummary {
        String stockCode;
        long totalVolume;    // 總成交量
        long totalAmount;    // 總成交金額
        String dateRange;    // 日期範圍字串（用於顯示）

        public StockSummary(String stockCode) {
            this.stockCode = stockCode;
            this.totalVolume = 0;
            this.totalAmount = 0;
        }
    }

    /**
     * 將股票資料依代碼分組並計算總計
     * 這個方法實現了資料的聚合邏輯
     */
    private Map<String, StockSummary> groupAndSummarizeData(List<StockRecord> filteredData) {
        // 使用HashMap來儲存每支股票的摘要資料
        // Key是股票代碼，Value是該股票的統計摘要
        Map<String, StockSummary> summaryMap = new HashMap<>();

        // 遍歷所有過濾後的股票記錄
        for (StockRecord record : filteredData) {
            // 檢查是否已經有這支股票的摘要資料
            StockSummary summary = summaryMap.get(record.stockCode);

            // 如果還沒有，建立新的摘要物件
            if (summary == null) {
                summary = new StockSummary(record.stockCode);
                summaryMap.put(record.stockCode, summary);
            }

            // 累加這筆記錄的成交量和成交金額到總計中
            summary.totalVolume += record.volume;
            summary.totalAmount += record.amount;
        }

        return summaryMap;
    }

    /**
     * 排序股票摘要資料
     * 這是整個程式的核心排序邏輯
     */
    private List<StockSummary> sortStockSummaries(Map<String, StockSummary> summaryMap,
                                                  boolean sortByVolume,
                                                  int topN) {

        // 將HashMap的值轉換為List，方便進行排序
        List<StockSummary> summaryList = new ArrayList<>(summaryMap.values());

        // 使用Collections.sort()方法進行排序
        // 第二個參數是Comparator，定義排序規則
        Collections.sort(summaryList, new Comparator<StockSummary>() {
            @Override
            public int compare(StockSummary s1, StockSummary s2) {
                // 根據使用者選擇的排序條件進行比較
                if (sortByVolume) {
                    // 依照成交量排序（由大到小）
                    // Long.compare(a, b) 會回傳：
                    // - 負數：如果a < b
                    // - 0：如果a == b  
                    // - 正數：如果a > b
                    // 我們要由大到小排序，所以要反轉比較結果
                    return Long.compare(s2.totalVolume, s1.totalVolume);
                } else {
                    // 依照成交金額排序（由大到小）
                    return Long.compare(s2.totalAmount, s1.totalAmount);
                }
            }
        });

        // 只取前N名的結果
        // Math.min確保不會超出清單的實際大小
        int endIndex = Math.min(topN, summaryList.size());

        // subList(0, endIndex) 取得從索引0到endIndex-1的子清單
        // 再用ArrayList包裝，建立新的清單物件
        return new ArrayList<>(summaryList.subList(0, endIndex));
    }

    /**
     * 在表格中顯示排序結果
     */
    private void displayResults(List<StockSummary> results) {
        // 清除表格現有資料
        tableModel.setRowCount(0);

        // 逐一將結果加入表格
        for (int i = 0; i < results.size(); i++) {
            StockSummary summary = results.get(i);

            // 建立表格行資料
            Object[] rowData = {
                    i + 1,                                    // 排名（從1開始）
                    summary.stockCode,                        // 股票代碼
                    String.format("%,d", summary.totalVolume), // 成交量（加入千分位逗號）
                    String.format("%,d", summary.totalAmount), // 成交金額（加入千分位逗號）
                    startDateField.getText() + (singleDateRadio.isSelected() ?
                            "" : " ~ " + endDateField.getText())  // 日期範圍
            };

            // 將這一行資料加入表格模型
            tableModel.addRow(rowData);
        }

        // 顯示分析完成的訊息
        String sortType = volumeRadio.isSelected() ? "成交量" : "成交金額";
        JOptionPane.showMessageDialog(this,
                String.format("分析完成！共找到 %d 支股票，已依%s排序",
                        results.size(), sortType),
                "分析結果",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 主程式進入點
     */
    public static void main(String[] args) {
        // 設定系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 如果設定失敗，使用預設外觀
        }

        // 在事件分派執行緒中建立並顯示GUI
        SwingUtilities.invokeLater(() -> {
            new StockAnalyzerGUI().setVisible(true);
        });
    }
}