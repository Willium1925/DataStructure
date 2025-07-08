package D0702;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MatrixSearchProgramFinal extends JFrame {
    private int[][] matrix;
    private int size;
    private HashMap<Integer, Position> hashMap;
    private List<NumberWithPosition> sortedList;

    // GUI 元件
    private JTable originalMatrixTable;
    private JTable sortedMatrixTable;
    private JTextArea resultArea;
    private JTextField searchField;
    private JTextField sizeField;
    private JTextField testCountField;
    private JButton generateButton;
    private JButton searchButton;
    private JButton randomSearchButton;
    private JButton averageTestButton;

    // 位置類別
    static class Position {
        int row, col;

        Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public String toString() {
            return "(" + row + ", " + col + ")";
        }
    }

    // 數字與位置的組合
    static class NumberWithPosition {
        int value;
        Position position;

        NumberWithPosition(int value, Position position) {
            this.value = value;
            this.position = position;
        }
    }

    // 搜尋結果類別
    static class SearchResult {
        boolean found;
        Position position;
        long timeNanos;

        SearchResult(boolean found, Position position, long timeNanos) {
            this.found = found;
            this.position = position;
            this.timeNanos = timeNanos;
        }

        public double getTimeMicros() {
            return timeNanos / 1000.0;
        }
    }

    // 平均搜尋結果類別
    static class AverageSearchResult {
        boolean allFound;
        double avgTimeMicros;
        int foundCount;
        int totalCount;

        AverageSearchResult(boolean allFound, double avgTimeMicros, int foundCount, int totalCount) {
            this.allFound = allFound;
            this.avgTimeMicros = avgTimeMicros;
            this.foundCount = foundCount;
            this.totalCount = totalCount;
        }

        public double getSuccessRate() {
            return totalCount > 0 ? (double) foundCount / totalCount * 100 : 0;
        }
    }

    public MatrixSearchProgramFinal() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("李振維 D1397026 N×N 矩陣搜尋程式");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 創建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 頂部控制面板
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 中間內容面板
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 左側面板（矩陣顯示）
        JPanel leftPanel = createLeftPanel();
        contentPanel.add(leftPanel, BorderLayout.WEST);

        // 右側面板（結果顯示）
        JPanel rightPanel = createRightPanel();
        contentPanel.add(rightPanel, BorderLayout.EAST);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        // 設定視窗大小和位置
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 第一行：矩陣設定
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstRow.add(new JLabel("矩陣大小 N:"));
        sizeField = new JTextField("5", 5);
        firstRow.add(sizeField);

        generateButton = new JButton("生成矩陣");
        generateButton.addActionListener(e -> generateMatrix());
        firstRow.add(generateButton);

        // 第二行：搜尋功能
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        secondRow.add(new JLabel("搜尋數字:"));
        searchField = new JTextField(10);
        secondRow.add(searchField);

        searchButton = new JButton("開始搜尋");
        searchButton.addActionListener(e -> performSearch());
        searchButton.setEnabled(false);
        secondRow.add(searchButton);

        randomSearchButton = new JButton("隨機產生並搜尋");
        randomSearchButton.addActionListener(e -> performRandomSearch());
        randomSearchButton.setEnabled(false);
        secondRow.add(randomSearchButton);

        // 第三行：批次測試
        JPanel thirdRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        thirdRow.add(new JLabel("測試次數:"));
        testCountField = new JTextField("100", 5);
        thirdRow.add(testCountField);

        averageTestButton = new JButton("批次測試平均時間");
        averageTestButton.addActionListener(e -> performAverageTest());
        averageTestButton.setEnabled(false);
        thirdRow.add(averageTestButton);

        panel.add(firstRow);
        panel.add(secondRow);
        panel.add(thirdRow);

        return panel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(600, 700));

        // 上方：原始矩陣
        JPanel originalPanel = new JPanel(new BorderLayout());
        originalPanel.setBorder(BorderFactory.createTitledBorder("原始矩陣"));

        originalMatrixTable = new JTable();
        originalMatrixTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        originalMatrixTable.setRowHeight(30);
        originalMatrixTable.setEnabled(false);

        // 設定表格置中顯示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        JScrollPane originalScrollPane = new JScrollPane(originalMatrixTable);
        originalScrollPane.setPreferredSize(new Dimension(580, 300));
        originalPanel.add(originalScrollPane, BorderLayout.CENTER);

        // 下方：排序後矩陣
        JPanel sortedPanel = new JPanel(new BorderLayout());
        sortedPanel.setBorder(BorderFactory.createTitledBorder("二元搜尋排序後矩陣"));

        sortedMatrixTable = new JTable();
        sortedMatrixTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        sortedMatrixTable.setRowHeight(30);
        sortedMatrixTable.setEnabled(false);

        JScrollPane sortedScrollPane = new JScrollPane(sortedMatrixTable);
        sortedScrollPane.setPreferredSize(new Dimension(580, 300));
        sortedPanel.add(sortedScrollPane, BorderLayout.CENTER);

        leftPanel.add(originalPanel, BorderLayout.NORTH);
        leftPanel.add(sortedPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(500, 700));
        rightPanel.setBorder(BorderFactory.createTitledBorder("搜尋結果"));

        resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(248, 248, 248));

        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        rightPanel.add(resultScrollPane, BorderLayout.CENTER);

        return rightPanel;
    }

    private void generateMatrix() {
        try {
            size = Integer.parseInt(sizeField.getText());
            if (size <= 0 || size > 1000) {
                JOptionPane.showMessageDialog(this, "請輸入 1-1000 之間的數字", "錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }

            matrix = new int[size][size];
            hashMap = new HashMap<>();

            // 生成不重複數字
            List<Integer> numbers = new ArrayList<>();
            for (int i = 1; i <= size * size; i++) {
                numbers.add(i);
            }
            Collections.shuffle(numbers);

            // 填入矩陣
            int index = 0;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    matrix[i][j] = numbers.get(index++);
                }
            }

            // 建立雜湊表
            buildHashMap();

            // 準備二元搜尋資料
            prepareBinarySearch();

            // 更新 GUI
            updateOriginalMatrixTable();
            updateSortedMatrixTable();

            // 啟用按鈕
            searchButton.setEnabled(true);
            randomSearchButton.setEnabled(true);
            averageTestButton.setEnabled(true);

            resultArea.setText("矩陣生成完成！\n請輸入要搜尋的數字，或使用隨機搜尋功能。\n數字範圍：1 到 " + (size * size) + "\n\n");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildHashMap() {
        hashMap.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                hashMap.put(matrix[i][j], new Position(i, j));
            }
        }
    }

    private void prepareBinarySearch() {
        sortedList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                sortedList.add(new NumberWithPosition(matrix[i][j], new Position(i, j)));
            }
        }

        sortedList.sort((a, b) -> Integer.compare(a.value, b.value));
    }

    private void updateOriginalMatrixTable() {
        String[] columnNames = new String[size];
        for (int i = 0; i < size; i++) {
            columnNames[i] = String.valueOf(i);
        }

        Object[][] data = new Object[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                data[i][j] = matrix[i][j];
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        originalMatrixTable.setModel(model);

        // 設定固定欄位寬度，避免字體壓縮
        for (int i = 0; i < size; i++) {
            originalMatrixTable.getColumnModel().getColumn(i).setPreferredWidth(60);
            originalMatrixTable.getColumnModel().getColumn(i).setMinWidth(60);
        }

        // 設定置中顯示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < size; i++) {
            originalMatrixTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 設定表格自動調整模式
        originalMatrixTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    private void updateSortedMatrixTable() {
        String[] columnNames = new String[size];
        for (int i = 0; i < size; i++) {
            columnNames[i] = String.valueOf(i);
        }

        Object[][] data = new Object[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int index = i * size + j;
                if (index < sortedList.size()) {
                    data[i][j] = sortedList.get(index).value;
                }
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        sortedMatrixTable.setModel(model);

        // 設定固定欄位寬度，避免字體壓縮
        for (int i = 0; i < size; i++) {
            sortedMatrixTable.getColumnModel().getColumn(i).setPreferredWidth(60);
            sortedMatrixTable.getColumnModel().getColumn(i).setMinWidth(60);
        }

        // 設定置中顯示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < size; i++) {
            sortedMatrixTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 設定表格自動調整模式
        sortedMatrixTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    private void performSearch() {
        try {
            int target = Integer.parseInt(searchField.getText());

            // 檢查數字是否在有效範圍內
            if (target < 1 || target > size * size) {
                JOptionPane.showMessageDialog(this,
                        "請輸入範圍內的數字 (1 到 " + (size * size) + ")",
                        "錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }

            displaySingleSearchResult(target);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performRandomSearch() {
        // 隨機產生範圍內的數字
        Random random = new Random();
        int target = random.nextInt(size * size) + 1;

        // 更新搜尋欄位
        searchField.setText(String.valueOf(target));

        displaySingleSearchResult(target);
    }

    private void displaySingleSearchResult(int target) {
        StringBuilder result = new StringBuilder();
        result.append("=== 搜尋結果 ===\n");
        result.append("搜尋目標: ").append(target).append("\n");
        result.append("矩陣大小: ").append(size).append("×").append(size).append("\n");
        result.append("數字範圍: 1 到 ").append(size * size).append("\n\n");

        // 執行三種搜尋
        SearchResult seqResult = sequentialSearch(target);
        SearchResult binResult = binarySearch(target);
        SearchResult hashResult = hashSearch(target);

        // 顯示結果
        result.append("1. 循序搜尋:\n");
        if (seqResult.found) {
            result.append("   ✓ 找到於位置: ").append(seqResult.position).append("\n");
            result.append("   執行時間: ").append(String.format("%.2f", seqResult.getTimeMicros())).append(" 微秒\n\n");
        } else {
            result.append("   ✗ 找不到\n");
            result.append("   執行時間: ").append(String.format("%.2f", seqResult.getTimeMicros())).append(" 微秒\n\n");
        }

        result.append("2. 二元搜尋:\n");
        if (binResult.found) {
            result.append("   ✓ 找到於位置: ").append(binResult.position).append("\n");
            result.append("   執行時間: ").append(String.format("%.2f", binResult.getTimeMicros())).append(" 微秒\n\n");
        } else {
            result.append("   ✗ 找不到\n");
            result.append("   執行時間: ").append(String.format("%.2f", binResult.getTimeMicros())).append(" 微秒\n\n");
        }

        result.append("3. 雜湊搜尋:\n");
        if (hashResult.found) {
            result.append("   ✓ 找到於位置: ").append(hashResult.position).append("\n");
            result.append("   執行時間: ").append(String.format("%.2f", hashResult.getTimeMicros())).append(" 微秒\n\n");
        } else {
            result.append("   ✗ 找不到\n");
            result.append("   執行時間: ").append(String.format("%.2f", hashResult.getTimeMicros())).append(" 微秒\n\n");
        }

        // 搜尋時間分析
        result.append("=== 搜尋時間分析 ===\n");
        result.append("循序搜尋: ").append(String.format("%.2f", seqResult.getTimeMicros())).append(" 微秒\n");
        result.append("二元搜尋: ").append(String.format("%.2f", binResult.getTimeMicros())).append(" 微秒\n");
        result.append("雜湊搜尋: ").append(String.format("%.2f", hashResult.getTimeMicros())).append(" 微秒\n");

        resultArea.setText(result.toString());
    }

    private void performAverageTest() {
        try {
            int testCount = Integer.parseInt(testCountField.getText());
            if (testCount <= 0 || testCount > 10000) {
                JOptionPane.showMessageDialog(this, "請輸入 1-10000 之間的測試次數", "錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultArea.setText("正在執行批次測試，請稍候...\n");

            // 使用 SwingWorker 來避免界面凍結
            SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    performBatchTest(testCount);
                    return null;
                }
            };

            worker.execute();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的測試次數", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performBatchTest(int testCount) {
        Random random = new Random();

        // 收集所有測試結果
        List<SearchResult> seqResults = new ArrayList<>();
        List<SearchResult> binResults = new ArrayList<>();
        List<SearchResult> hashResults = new ArrayList<>();

        for (int i = 0; i < testCount; i++) {
            // 隨機選擇搜尋目標
            int target = random.nextInt(size * size) + 1;

            // 執行三種搜尋
            seqResults.add(sequentialSearch(target));
            binResults.add(binarySearch(target));
            hashResults.add(hashSearch(target));
        }

        // 計算平均結果
        AverageSearchResult seqAvg = calculateAverageResult(seqResults);
        AverageSearchResult binAvg = calculateAverageResult(binResults);
        AverageSearchResult hashAvg = calculateAverageResult(hashResults);

        // 顯示結果
        displayBatchTestResults(testCount, seqAvg, binAvg, hashAvg);
    }

    private AverageSearchResult calculateAverageResult(List<SearchResult> results) {
        long totalTime = 0;
        int foundCount = 0;

        for (SearchResult result : results) {
            totalTime += result.timeNanos;
            if (result.found) {
                foundCount++;
            }
        }

        double avgTimeMicros = (double) totalTime / results.size() / 1000.0;
        boolean allFound = foundCount == results.size();

        return new AverageSearchResult(allFound, avgTimeMicros, foundCount, results.size());
    }

    private void displayBatchTestResults(int testCount, AverageSearchResult seqAvg,
                                         AverageSearchResult binAvg, AverageSearchResult hashAvg) {
        StringBuilder result = new StringBuilder();
        result.append("=== 批次測試結果 ===\n");
        result.append("測試次數: ").append(testCount).append("\n");
        result.append("矩陣大小: ").append(size).append("×").append(size).append("\n");
        result.append("數字範圍: 1 到 ").append(size * size).append("\n\n");

        result.append("1. 循序搜尋平均結果:\n");
        result.append("   平均執行時間: ").append(String.format("%.2f", seqAvg.avgTimeMicros)).append(" 微秒\n");
        result.append("   成功找到次數: ").append(seqAvg.foundCount).append("/").append(seqAvg.totalCount).append("\n");
        result.append("   成功率: ").append(String.format("%.2f", seqAvg.getSuccessRate())).append("%\n\n");

        result.append("2. 二元搜尋平均結果:\n");
        result.append("   平均執行時間: ").append(String.format("%.2f", binAvg.avgTimeMicros)).append(" 微秒\n");
        result.append("   成功找到次數: ").append(binAvg.foundCount).append("/").append(binAvg.totalCount).append("\n");
        result.append("   成功率: ").append(String.format("%.2f", binAvg.getSuccessRate())).append("%\n\n");

        result.append("3. 雜湊搜尋平均結果:\n");
        result.append("   平均執行時間: ").append(String.format("%.2f", hashAvg.avgTimeMicros)).append(" 微秒\n");
        result.append("   成功找到次數: ").append(hashAvg.foundCount).append("/").append(hashAvg.totalCount).append("\n");
        result.append("   成功率: ").append(String.format("%.2f", hashAvg.getSuccessRate())).append("%\n\n");

        result.append("=== 平均搜尋時間比較 ===\n");
        result.append("循序搜尋: ").append(String.format("%.2f", seqAvg.avgTimeMicros)).append(" 微秒\n");
        result.append("二元搜尋: ").append(String.format("%.2f", binAvg.avgTimeMicros)).append(" 微秒\n");
        result.append("雜湊搜尋: ").append(String.format("%.2f", hashAvg.avgTimeMicros)).append(" 微秒\n\n");

        // 效能分析
        result.append("=== 效能分析 ===\n");
        double seqTime = seqAvg.avgTimeMicros;
        double binTime = binAvg.avgTimeMicros;
        double hashTime = hashAvg.avgTimeMicros;

        if (hashTime > 0) {
            result.append("雜湊比循序快了: ").append(String.format("%.2f", seqTime / hashTime)).append(" 倍\n");
            result.append("雜湊比二元快了: ").append(String.format("%.2f", binTime / hashTime)).append(" 倍\n");
        }

        if (binTime > 0) {
            result.append("二元比循序快了: ").append(String.format("%.2f", seqTime / binTime)).append(" 倍\n");
        }

        SwingUtilities.invokeLater(() -> resultArea.setText(result.toString()));
    }

    // 1. 循序搜尋
    private SearchResult sequentialSearch(int target) {
        long startTime = System.nanoTime();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (matrix[i][j] == target) {
                    long endTime = System.nanoTime();
                    return new SearchResult(true, new Position(i, j), endTime - startTime);
                }
            }
        }

        long endTime = System.nanoTime();
        return new SearchResult(false, null, endTime - startTime);
    }

    // 2. 二元搜尋
    private SearchResult binarySearch(int target) {
        long startTime = System.nanoTime();

        int left = 0, right = sortedList.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            int midValue = sortedList.get(mid).value;

            if (midValue == target) {
                long endTime = System.nanoTime();
                return new SearchResult(true, sortedList.get(mid).position, endTime - startTime);
            } else if (midValue < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        long endTime = System.nanoTime();
        return new SearchResult(false, null, endTime - startTime);
    }

    // 3. 雜湊搜尋
    private SearchResult hashSearch(int target) {
        long startTime = System.nanoTime();

        Position position = hashMap.get(target);

        long endTime = System.nanoTime();

        if (position != null) {
            return new SearchResult(true, position, endTime - startTime);
        } else {
            return new SearchResult(false, null, endTime - startTime);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new MatrixSearchProgramFinal().setVisible(true);
        });
    }
}