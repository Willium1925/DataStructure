package D0828;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.*;

public class SearchComparisonGUI extends JFrame {

    private final JButton startButton;
    private final JTable resultTable;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private final JTextArea sampleDataTextArea;

    public SearchComparisonGUI() {
        super("搜尋演算法效能比較");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // --- UI Components ---
        startButton = new JButton("開始比較");
        statusLabel = new JLabel("準備就緒", SwingConstants.CENTER);

        String[] columnNames = {"搜尋方法", "平均時間 (找到)", "平均時間 (未找到)", "快多少倍 (找到)", "快多少倍 (未找到)"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        JScrollPane resultScrollPane = new JScrollPane(resultTable);

        sampleDataTextArea = new JTextArea(12, 50);
        sampleDataTextArea.setEditable(false);
        sampleDataTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sampleDataScrollPane = new JScrollPane(sampleDataTextArea);
        sampleDataScrollPane.setBorder(new TitledBorder("前 10 筆隨機資料範例"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultScrollPane, sampleDataScrollPane);
        splitPane.setResizeWeight(0.7);

        // --- Layout ---
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel();
        topPanel.add(startButton);

        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(splitPane, BorderLayout.CENTER);
        contentPane.add(statusLabel, BorderLayout.SOUTH);

        // --- Event Listener ---
        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            statusLabel.setText("處理中，請稍候...");
            tableModel.setRowCount(0);
            sampleDataTextArea.setText("");
            new SearchTask().execute();
        });
    }

    private static class TaskResult {
        final List<Object[]> performanceResults;
        final String sampleData;
        TaskResult(List<Object[]> performanceResults, String sampleData) {
            this.performanceResults = performanceResults;
            this.sampleData = sampleData;
        }
    }

    private class SearchTask extends SwingWorker<TaskResult, String> {

        private static final int DATA_SIZE = 1_000_000;
        private static final int SEARCH_KEYS_COUNT = 10;
        private static final int WARMUP_COUNT = 20_000;

        @Override
        protected TaskResult doInBackground() throws Exception {
            publish("步驟 1/6: 產生 " + DATA_SIZE + " 筆隨機資料...");

            Set<TransactionData> dataSet = new HashSet<>(DATA_SIZE);
            List<String> itemNames = List.of("筆記型電腦", "滑鼠", "鍵盤", "螢幕", "網路攝影機");
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Set<Long> existingIds = new HashSet<>();

            while(dataSet.size() < DATA_SIZE) {
                long customerId = random.nextLong(DATA_SIZE * 2);
                if (existingIds.add(customerId)) {
                    LocalDate date = LocalDate.now().minusDays(random.nextInt(365));
                    String itemName = itemNames.get(random.nextInt(itemNames.size()));
                    double price = Math.round(random.nextDouble(100, 5000) * 100.0) / 100.0;
                    dataSet.add(new TransactionData(date, customerId, itemName, price));
                }
            }
            List<TransactionData> dataList = new ArrayList<>(dataSet);

            publish("步驟 2/6: 準備範例資料...");
            StringBuilder sb = new StringBuilder();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            sb.append(String.format("%-12s | %-15s | %-8s | %s\n", "客戶代碼", "物品名稱", "價格", "交易日期"));
            sb.append("-".repeat(60)).append("\n");
            for (int i = 0; i < 10 && i < dataList.size(); i++) {
                TransactionData td = dataList.get(i);
                sb.append(String.format("%-12d | %-15s | %-8.2f | %s\n",
                        td.getCustomerId(), td.getItemName(), td.getPrice(), td.getTransactionDate().format(formatter)));
            }
            String sampleDataString = sb.toString();

            publish("步驟 3/6: 準備資料結構 (排序、建立HashMap)...");
            TransactionData[] sortedArray = dataList.toArray(new TransactionData[0]);
            Arrays.sort(sortedArray);
            Map<Long, TransactionData> dataMap = dataList.stream()
                    .collect(Collectors.toMap(TransactionData::getCustomerId, td -> td));

            List<Long> existingKeys = new ArrayList<>(existingIds).subList(0, SEARCH_KEYS_COUNT);
            List<Long> nonExistingKeys = LongStream.range(DATA_SIZE * 2 + 1, DATA_SIZE * 2 + 1 + SEARCH_KEYS_COUNT)
                    .boxed().collect(Collectors.toList());

            publish("步驟 4/6: JIT 暖機...");
            for (int i = 0; i < WARMUP_COUNT; i++) {
                long key = existingKeys.get(i % SEARCH_KEYS_COUNT);
                linearSearch(dataList, key);
                binarySearch(sortedArray, key);
                interpolationSearch(sortedArray, key);
                hashSearch(dataMap, key);
            }

            publish("步驟 5/6: 執行搜尋效能測試...");
            // *** 修改: 變數類型從 long 改為 double，儲存精確的平均時間 ***
            double linearFoundTime = measureSearch(this::linearSearch, dataList, existingKeys);
            double binaryFoundTime = measureSearch(this::binarySearch, sortedArray, existingKeys);
            double interpolationFoundTime = measureSearch(this::interpolationSearch, sortedArray, existingKeys);
            double hashFoundTime = measureSearch(this::hashSearch, dataMap, existingKeys);

            double linearNotFoundTime = measureSearch(this::linearSearch, dataList, nonExistingKeys);
            double binaryNotFoundTime = measureSearch(this::binarySearch, sortedArray, nonExistingKeys);
            double interpolationNotFoundTime = measureSearch(this::interpolationSearch, sortedArray, nonExistingKeys);
            double hashNotFoundTime = measureSearch(this::hashSearch, dataMap, nonExistingKeys);

            publish("步驟 6/6: 計算結果...");
            List<Object[]> results = new ArrayList<>();
            results.add(formatResult("線性搜尋", linearFoundTime, linearNotFoundTime, linearFoundTime, linearNotFoundTime));
            results.add(formatResult("二分搜尋", binaryFoundTime, binaryNotFoundTime, linearFoundTime, linearNotFoundTime));
            results.add(formatResult("插補搜尋", interpolationFoundTime, interpolationNotFoundTime, linearFoundTime, linearNotFoundTime));
            results.add(formatResult("雜湊搜尋 (HashMap)", hashFoundTime, hashNotFoundTime, linearFoundTime, linearNotFoundTime));

            return new TaskResult(results, sampleDataString);
        }

        @Override
        protected void done() {
            try {
                TaskResult taskResult = get();
                for (Object[] rowData : taskResult.performanceResults) {
                    tableModel.addRow(rowData);
                }
                sampleDataTextArea.setText(taskResult.sampleData);
                sampleDataTextArea.setCaretPosition(0);
                statusLabel.setText("比較完成！");
            } catch (InterruptedException | ExecutionException e) {
                statusLabel.setText("發生錯誤: " + e.getMessage());
                e.printStackTrace();
            } finally {
                startButton.setEnabled(true);
            }
        }

        @Override
        protected void process(List<String> chunks) {
            statusLabel.setText(chunks.get(chunks.size() - 1));
        }

        // *** 修改: 參數類型從 long 改為 double ***
        private Object[] formatResult(String name, double foundTime, double notFoundTime, double baseFound, double baseNotFound) {
            String foundRatioStr, notFoundRatioStr;
            if (name.equals("線性搜尋")) {
                foundRatioStr = "1.00 倍 (基準)";
                notFoundRatioStr = "1.00 倍 (基準)";
            } else {
                double foundRatio = (foundTime > 0) ? baseFound / foundTime : Double.POSITIVE_INFINITY;
                double notFoundRatio = (notFoundTime > 0) ? baseNotFound / notFoundTime : Double.POSITIVE_INFINITY;
                foundRatioStr = String.format("快 %.2f 倍", foundRatio);
                notFoundRatioStr = String.format("快 %.2f 倍", notFoundRatio);
            }
            return new Object[]{
                    name,
                    String.format("%,.3f ns", foundTime), // *** 修改: 直接使用平均時間，不再除以 SEARCH_KEYS_COUNT ***
                    String.format("%,.3f ns", notFoundTime),
                    foundRatioStr,
                    notFoundRatioStr
            };
        }

        @FunctionalInterface
        interface SearchFunction<T, U, R> { R apply(T t, U u); }

        // *** 核心修改: 全新、更精準的計時方法 ***
        private <T> double measureSearch(SearchFunction<T, Long, Optional<TransactionData>> searchMethod, T dataStructure, List<Long> keys) {
            // 為了讓測量更穩定，對每個 key 的搜尋都重複多次
            final int iterationsPerKey = 10000;

            long totalDuration = 0;

            // 對每一個要搜尋的 key 獨立計時，再取總和
            for (Long key : keys) {
                long startTime = System.nanoTime();
                // 在計時區間內，重複執行同一個搜尋操作
                for (int i = 0; i < iterationsPerKey; i++) {
                    searchMethod.apply(dataStructure, key);
                }
                long endTime = System.nanoTime();
                totalDuration += (endTime - startTime);
            }

            // 計算單次操作的平均時間
            // 總時間 / (key的數量 * 每個key的重複次數)
            return (double) totalDuration / (keys.size() * iterationsPerKey);
        }

        // --- 搜尋演算法實作 (無變動) ---
        public Optional<TransactionData> linearSearch(List<TransactionData> list, long key) {
            for (TransactionData data : list) { if (data.getCustomerId() == key) return Optional.of(data); }
            return Optional.empty();
        }
        public Optional<TransactionData> binarySearch(TransactionData[] sortedArray, long key) {
            int low = 0; int high = sortedArray.length - 1;
            while (low <= high) {
                int mid = low + (high - low) / 2; long midVal = sortedArray[mid].getCustomerId();
                if (midVal < key) low = mid + 1; else if (midVal > key) high = mid - 1; else return Optional.of(sortedArray[mid]);
            }
            return Optional.empty();
        }
        public Optional<TransactionData> interpolationSearch(TransactionData[] sortedArray, long key) {
            int low = 0; int high = sortedArray.length - 1;
            while (low <= high && key >= sortedArray[low].getCustomerId() && key <= sortedArray[high].getCustomerId()) {
                if (low == high) { if (sortedArray[low].getCustomerId() == key) return Optional.of(sortedArray[low]); return Optional.empty(); }
                long lowVal = sortedArray[low].getCustomerId(); long highVal = sortedArray[high].getCustomerId();
                int pos = low + (int) (((double) (high - low) / (highVal - lowVal)) * (key - lowVal));
                if (pos >= sortedArray.length || pos < 0) return Optional.empty();
                long posVal = sortedArray[pos].getCustomerId();
                if (posVal == key) return Optional.of(sortedArray[pos]);
                if (posVal < key) low = pos + 1; else high = pos - 1;
            }
            return Optional.empty();
        }
        public Optional<TransactionData> hashSearch(Map<Long, TransactionData> map, long key) {
            return Optional.ofNullable(map.get(key));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SearchComparisonGUI frame = new SearchComparisonGUI();
            frame.setVisible(true);
        });
    }
}
