package D0819;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * 股票資料檢視器，提供 GUI 介面讓使用者查詢並排序股票交易資料。
 * 支援單日或日期區間查詢，按成交量或成交金額排序，並顯示前 N 筆結果。
 * 使用外部合併排序演算法處理大數據量，並測量排序時間。
 */
public class StockDataViewer extends JFrame {
    private JTextField dateField, startDateField, endDateField, topNField; // 輸入欄位
    private JComboBox<String> sortFieldCombo, queryTypeCombo; // 下拉選單
    private JTextArea resultArea; // 結果顯示區
    private static final String DATA_FILE = "stock_data.csv"; // 資料檔案
    private static final int CHUNK_SIZE = 100000; // 每個分塊的記錄數
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 初始化 GUI 介面，設置輸入欄位、按鈕和結果顯示區。
     */
    public StockDataViewer() {
        setTitle("股票資料檢視器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 輸入面板
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("查詢類型："));
        queryTypeCombo = new JComboBox<>(new String[]{"單日", "日期區間"});
        queryTypeCombo.addActionListener(e -> updateDateFields());
        inputPanel.add(queryTypeCombo);

        inputPanel.add(new JLabel("日期 (yyyy-MM-dd)："));
        dateField = new JTextField(10);
        inputPanel.add(dateField);

        inputPanel.add(new JLabel("開始日期 (yyyy-MM-dd)："));
        startDateField = new JTextField(10);
        inputPanel.add(startDateField);

        inputPanel.add(new JLabel("結束日期 (yyyy-MM-dd)："));
        endDateField = new JTextField(10);
        inputPanel.add(endDateField);

        inputPanel.add(new JLabel("排序欄位："));
        sortFieldCombo = new JComboBox<>(new String[]{"成交量", "成交金額"});
        inputPanel.add(sortFieldCombo);

        inputPanel.add(new JLabel("顯示前 N 筆："));
        topNField = new JTextField("10", 5);
        inputPanel.add(topNField);

        JButton queryButton = new JButton("查詢");
        queryButton.addActionListener(e -> performQuery());
        inputPanel.add(queryButton);

        // 結果顯示區
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        updateDateFields(); // 初始化輸入欄位狀態
    }

    /**
     * 根據查詢類型更新日期輸入欄位的啟用狀態。
     * 單日查詢啟用單一日期欄位，區間查詢啟用開始和結束日期欄位。
     */
    private void updateDateFields() {
        boolean isSingleDay = queryTypeCombo.getSelectedItem().equals("單日");
        dateField.setEnabled(isSingleDay);
        startDateField.setEnabled(!isSingleDay);
        endDateField.setEnabled(!isSingleDay);
    }

    /**
     * 執行查詢，根據使用者輸入過濾資料並排序，顯示結果和排序時間。
     */
    private void performQuery() {
        try {
            boolean sortByVolume = sortFieldCombo.getSelectedItem().equals("成交量");
            int topN = Integer.parseInt(topNField.getText());
            if (topN <= 0) throw new NumberFormatException("前 N 筆必須為正數");

            // 記錄排序開始時間
            long startTime = System.nanoTime();
            List<String[]> sortedData;
            if (queryTypeCombo.getSelectedItem().equals("單日")) {
                LocalDate date = LocalDate.parse(dateField.getText(), DATE_FORMATTER);
                sortedData = externalMergeSort(singleDayFilter(date), sortByVolume);
            } else {
                LocalDate startDate = LocalDate.parse(startDateField.getText(), DATE_FORMATTER);
                LocalDate endDate = LocalDate.parse(endDateField.getText(), DATE_FORMATTER);
                sortedData = externalMergeSort(dateRangeFilter(startDate, endDate), sortByVolume);
            }
            // 計算排序時間（奈秒轉毫秒）
            long endTime = System.nanoTime();
            double sortTimeMs = (endTime - startTime) / 1_000_000.0;

            // 顯示結果
            StringBuilder result = new StringBuilder();
            result.append(String.format("排序耗時 %.2f 毫秒\n", sortTimeMs));
            result.append("股票代碼\t股票名稱\t成交量\t成交金額\t交易日期\n");
            for (int i = 0; i < Math.min(topN, sortedData.size()); i++) {
                String[] record = sortedData.get(i);
                result.append(String.format("%s\t%s\t%s\t%s\t%s\n",
                        record[0], record[1], record[3], record[4], record[2]));
            }
            resultArea.setText(result.toString());
        } catch (Exception e) {
            resultArea.setText("錯誤：" + e.getMessage());
        }
    }

    /**
     * 過濾單日資料，僅保留指定日期的記錄。
     * @param date 查詢日期
     * @return 過濾後的資料列表
     */
    private List<String[]> singleDayFilter(LocalDate date) throws IOException {
        List<String[]> filtered = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(DATA_FILE), StandardCharsets.UTF_8))) {
            reader.readLine(); // 跳過標頭
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[2].equals(date.format(DATE_FORMATTER))) {
                    filtered.add(parts);
                }
            }
        }
        return filtered;
    }

    /**
     * 過濾日期區間資料，保留指定範圍內的記錄。
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 過濾後的資料列表
     */
    private List<String[]> dateRangeFilter(LocalDate startDate, LocalDate endDate) throws IOException {
        List<String[]> filtered = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(DATA_FILE), StandardCharsets.UTF_8))) {
            reader.readLine(); // 跳過標頭
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                LocalDate date = LocalDate.parse(parts[2], DATE_FORMATTER);
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    filtered.add(parts);
                }
            }
        }
        return filtered;
    }

    /**
     * 外部合併排序演算法，適用於大數據量。
     * 邏輯：
     * 1. 將資料分割成小塊（每塊最多 CHUNK_SIZE 筆），各塊在記憶體中排序。
     * 2. 將排序後的塊寫入臨時檔案。
     * 3. 使用優先級隊列合併所有臨時檔案，生成最終排序結果。
     * 理由：
     * - 當資料量過大（例如 36,000,000 筆）無法全部載入記憶體時，外部排序是必要選擇。
     * - 每塊大小 (CHUNK_SIZE=100,000) 是基於記憶體限制的折衷，確保單塊可載入記憶體。
     * - 使用優先級隊列進行合併，時間複雜度為 O(N log k)，其中 N 為總記錄數，k 為塊數。
     * @param data 待排序資料
     * @param sortByVolume 是否按成交量排序（否則按成交金額）
     * @return 排序後的資料列表
     */
    private List<String[]> externalMergeSort(List<String[]> data, boolean sortByVolume) throws IOException {
        // 如果資料量小於等於 CHUNK_SIZE，直接使用記憶體內合併排序
        if (data.size() <= CHUNK_SIZE) {
            return mergeSort(data, sortByVolume);
        }

        // 分割資料成多個塊並排序
        List<File> tempFiles = new ArrayList<>();
        for (int i = 0; i < data.size(); i += CHUNK_SIZE) {
            // 提取子塊，範圍為 i 到 i+CHUNK_SIZE
            List<String[]> chunk = data.subList(i, Math.min(i + CHUNK_SIZE, data.size()));
            // 對子塊進行記憶體內合併排序
            List<String[]> sortedChunk = mergeSort(new ArrayList<>(chunk), sortByVolume);
            // 創建臨時檔案儲存排序後的塊
            File tempFile = File.createTempFile("stock_chunk_", ".csv");
            tempFile.deleteOnExit(); // 程式結束時自動刪除
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
                for (String[] record : sortedChunk) {
                    writer.write(String.join(",", record) + "\n");
                }
            }
            tempFiles.add(tempFile);
        }

        // 使用優先級隊列合併排序後的塊
        // 邏輯：
        // 1. 為每個臨時檔案開啟一個 BufferedReader，讀取第一筆記錄。
        // 2. 將每筆記錄放入優先級隊列，按指定欄位（成交量或金額）排序。
        // 3. 每次從隊列取出最大值，加入結果，並從對應檔案讀取下一筆。
        // 4. 重複直到所有記錄處理完畢。
        // 理由：
        // - 優先級隊列確保每次取出最小（或最大，視排序方向）記錄，模擬 K 路合併。
        // - 僅保持 k 筆記錄在記憶體中（k 為塊數），大幅降低記憶體使用量。
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>((a, b) -> {
            long valA = Long.parseLong(a.record[sortByVolume ? 3 : 4]);
            long valB = Long.parseLong(b.record[sortByVolume ? 3 : 4]);
            return Long.compare(valB, valA); // 降序排序
        });

        List<BufferedReader> readers = new ArrayList<>();
        for (File tempFile : tempFiles) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(tempFile), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line != null) {
                queue.offer(new QueueEntry(line.split(","), reader));
            }
            readers.add(reader);
        }

        List<String[]> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            QueueEntry entry = queue.poll();
            result.add(entry.record);
            BufferedReader reader = entry.reader;
            String line = reader.readLine();
            if (line != null) {
                queue.offer(new QueueEntry(line.split(","), reader));
            }
        }

        // 清理資源
        for (BufferedReader reader : readers) {
            reader.close();
        }
        for (File tempFile : tempFiles) {
            tempFile.delete();
        }

        return result;
    }

    /**
     * 記憶體內合併排序，用於排序單個小塊資料。
     * 邏輯：
     * 1. 使用分治法將資料分成兩半，遞迴排序。
     * 2. 合併兩個已排序的子列表，生成完整排序結果。
     * 理由：
     * - 合併排序時間複雜度為 O(n log n)，穩定且適合鏈結結構。
     * - 在外部排序中，每塊資料量小，適合記憶體內快速排序。
     * @param data 待排序資料
     * @param sortByVolume 是否按成交量排序
     * @return 排序後的資料列表
     */
    private List<String[]> mergeSort(List<String[]> data, boolean sortByVolume) {
        if (data.size() <= 1) return data;

        int mid = data.size() / 2;
        List<String[]> left = mergeSort(data.subList(0, mid), sortByVolume);
        List<String[]> right = mergeSort(data.subList(mid, data.size()), sortByVolume);

        return merge(left, right, sortByVolume);
    }

    /**
     * 合併兩個已排序的子列表。
     * 邏輯：
     * 1. 比較兩個列表的元素，按指定欄位（成交量或金額）合併。
     * 2. 確保降序排列（較大值優先）。
     * 理由：
     * - 合併是合併排序的核心，確保穩定性和正確性。
     * - 使用索引遍歷避免額外記憶體分配，提高效率。
     * @param left 左子列表
     * @param right 右子列表
     * @param sortByVolume 是否按成交量排序
     * @return 合併後的排序列表
     */
    private List<String[]> merge(List<String[]> left, List<String[]> right, boolean sortByVolume) {
        List<String[]> result = new ArrayList<>();
        int i = 0, j = 0;
        int fieldIndex = sortByVolume ? 3 : 4; // 成交量或金額的欄位索引

        while (i < left.size() && j < right.size()) {
            long valueLeft = Long.parseLong(left.get(i)[fieldIndex]);
            long valueRight = Long.parseLong(right.get(j)[fieldIndex]);
            if (valueLeft >= valueRight) { // 降序排序
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }

        result.addAll(left.subList(i, left.size()));
        result.addAll(right.subList(j, right.size()));
        return result;
    }

    /**
     * 輔助類，用於優先級隊列儲存臨時檔案的記錄和對應的讀取器。
     */
    private static class QueueEntry {
        String[] record; // 當前記錄
        BufferedReader reader; // 檔案讀取器

        QueueEntry(String[] record, BufferedReader reader) {
            this.record = record;
            this.reader = reader;
        }
    }

    /**
     * 主程式，啟動 GUI。
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StockDataViewer viewer = new StockDataViewer();
            viewer.setVisible(true);
        });
    }
}
