package D0715.SparseMatrix;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class SparseMatrixTransposer extends JFrame {
    private JSpinner rowSpinner, colSpinner;
    private JSlider densitySlider;
    private JTextArea originalMatrixArea, method1TripleArea, method1ResultArea,
            method2NumArea, method2CpotArea, method2ResultArea;
    private JLabel method1TimeLabel, method2TimeLabel;
    private JButton generateButton, transposeButton;

    // 稀疏矩陣存儲結構
    private static class Triple {
        int row, col, value;

        Triple(int row, int col, int value) {
            this.row = row;
            this.col = col;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("(%d,%d,%d)", row, col, value);
        }
    }

    private int[][] originalMatrix;
    private List<Triple> sparseMatrix;
    private List<Triple> method1Result;
    private TransposeResult method2Result;
    private int rows, cols;

    public SparseMatrixTransposer() {
        initializeGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("稀疏矩陣轉置器");
        setSize(1000, 800);
        setLocationRelativeTo(null);
    }

    private void initializeGUI() {
        setLayout(new BorderLayout());

        // 控制面板
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // 主顯示區域
        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 原始矩陣顯示區
        originalMatrixArea = new JTextArea(8, 50);
        originalMatrixArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        originalMatrixArea.setEditable(false);
        JScrollPane originalScroll = new JScrollPane(originalMatrixArea);
        originalScroll.setBorder(new TitledBorder("原始稀疏矩陣"));
        mainPanel.add(originalScroll);

        // 方法1顯示區 (左右分割)
        JPanel method1Panel = new JPanel(new BorderLayout());
        JPanel method1ContentPanel = new JPanel(new GridLayout(1, 2, 5, 0));

        // 左側：三元組
        method1TripleArea = new JTextArea(8, 25);
        method1TripleArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        method1TripleArea.setEditable(false);
        JScrollPane method1TripleScroll = new JScrollPane(method1TripleArea);
        method1TripleScroll.setBorder(new TitledBorder("三元組"));
        method1ContentPanel.add(method1TripleScroll);

        // 右側：轉置後矩陣
        method1ResultArea = new JTextArea(8, 25);
        method1ResultArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        method1ResultArea.setEditable(false);
        JScrollPane method1ResultScroll = new JScrollPane(method1ResultArea);
        method1ResultScroll.setBorder(new TitledBorder("轉置後稀疏矩陣"));
        method1ContentPanel.add(method1ResultScroll);

        method1TimeLabel = new JLabel("執行時間: 未計算");
        method1TimeLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        method1Panel.add(method1ContentPanel, BorderLayout.CENTER);
        method1Panel.add(method1TimeLabel, BorderLayout.SOUTH);
        method1Panel.setBorder(new TitledBorder("方法1: 普通轉置法"));
        mainPanel.add(method1Panel);

        // 方法2顯示區 (左中右分割)
        JPanel method2Panel = new JPanel(new BorderLayout());
        JPanel method2ContentPanel = new JPanel(new GridLayout(1, 3, 5, 0));

        // 左側：num陣列
        method2NumArea = new JTextArea(8, 15);
        method2NumArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        method2NumArea.setEditable(false);
        JScrollPane method2NumScroll = new JScrollPane(method2NumArea);
        method2NumScroll.setBorder(new TitledBorder("num陣列"));
        method2ContentPanel.add(method2NumScroll);

        // 中間：cpot陣列
        method2CpotArea = new JTextArea(8, 15);
        method2CpotArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        method2CpotArea.setEditable(false);
        JScrollPane method2CpotScroll = new JScrollPane(method2CpotArea);
        method2CpotScroll.setBorder(new TitledBorder("cpot陣列"));
        method2ContentPanel.add(method2CpotScroll);

        // 右側：轉置後矩陣
        method2ResultArea = new JTextArea(8, 20);
        method2ResultArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        method2ResultArea.setEditable(false);
        JScrollPane method2ResultScroll = new JScrollPane(method2ResultArea);
        method2ResultScroll.setBorder(new TitledBorder("轉置後稀疏矩陣"));
        method2ContentPanel.add(method2ResultScroll);

        method2TimeLabel = new JLabel("執行時間: 未計算");
        method2TimeLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        method2Panel.add(method2ContentPanel, BorderLayout.CENTER);
        method2Panel.add(method2TimeLabel, BorderLayout.SOUTH);
        method2Panel.setBorder(new TitledBorder("方法2: 快速轉置法"));
        mainPanel.add(method2Panel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        // 行列數設定
        panel.add(new JLabel("列數:"));
        rowSpinner = new JSpinner(new SpinnerNumberModel(5, 3, 20, 1));
        panel.add(rowSpinner);

        panel.add(new JLabel("行數:"));
        colSpinner = new JSpinner(new SpinnerNumberModel(5, 3, 20, 1));
        panel.add(colSpinner);

        // 密度設定
        panel.add(new JLabel("疏密度:"));
        densitySlider = new JSlider(10, 50, 30);
        densitySlider.setMajorTickSpacing(10);
        densitySlider.setPaintTicks(true);
        densitySlider.setPaintLabels(true);
        panel.add(densitySlider);
        panel.add(new JLabel("%"));

        // 按鈕
        generateButton = new JButton("生成矩陣");
        generateButton.addActionListener(e -> generateMatrix());
        panel.add(generateButton);

        transposeButton = new JButton("執行轉置");
        transposeButton.addActionListener(e -> executeTranspose());
        transposeButton.setEnabled(false);
        panel.add(transposeButton);

        return panel;
    }

    private void generateMatrix() {
        rows = (Integer) rowSpinner.getValue();
        cols = (Integer) colSpinner.getValue();
        int density = densitySlider.getValue();

        // 生成稀疏矩陣
        originalMatrix = new int[rows][cols];
        sparseMatrix = new ArrayList<>();
        Random random = new Random();

        int totalElements = rows * cols;
        int nonZeroCount = (int) (totalElements * density / 100.0);

        // 隨機填充非零元素
        Set<String> positions = new HashSet<>();
        while (positions.size() < nonZeroCount) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            String pos = r + "," + c;
            if (!positions.contains(pos)) {
                positions.add(pos);
                int value = random.nextInt(9) + 1; // 1-9的隨機值
                originalMatrix[r][c] = value;
                sparseMatrix.add(new Triple(r, c, value));
            }
        }

        // 顯示原始矩陣
        displayOriginalMatrix();
        transposeButton.setEnabled(true);
        method1TripleArea.setText("");
        method1ResultArea.setText("");
        method2NumArea.setText("");
        method2CpotArea.setText("");
        method2ResultArea.setText("");
        method1TimeLabel.setText("執行時間: 未計算");
        method2TimeLabel.setText("執行時間: 未計算");
    }

    private void displayOriginalMatrix() {
        StringBuilder sb = new StringBuilder();
        sb.append("原始矩陣 (").append(rows).append("×").append(cols).append("):\n");

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sb.append(String.format("%3d ", originalMatrix[i][j]));
            }
            sb.append("\n");
        }

        originalMatrixArea.setText(sb.toString());
    }

    private void executeTranspose() {
        // 方法1: 普通轉置法 - 只計算核心轉置時間
        long time1 = normalTransposeWithTiming();

        // 方法2: 快速轉置法 - 只計算核心轉置時間
        long time2 = fastTransposeWithTiming();

        // 顯示結果
        displayMethod1Result(method1Result, time1);
        displayMethod2Result(method2Result, time2);
    }

    private List<Triple> normalTranspose() {
        List<Triple> result = new ArrayList<>();

        // 對每一列進行處理
        for (int j = 0; j < cols; j++) {
            for (Triple t : sparseMatrix) {
                if (t.col == j) {
                    result.add(new Triple(t.col, t.row, t.value));
                }
            }
        }

        return result;
    }

    private long normalTransposeWithTiming() {
        List<Triple> result = new ArrayList<>();

        // 只計算核心轉置部分的時間
        long startTime = System.nanoTime();

        // 對每一列進行處理
        for (int j = 0; j < cols; j++) {
            for (Triple t : sparseMatrix) {
                if (t.col == j) {
                    result.add(new Triple(t.col, t.row, t.value));
                }
            }
        }

        long endTime = System.nanoTime();

        // 將結果暫存以供顯示使用
        this.method1Result = result;

        return endTime - startTime;
    }

    private static class TransposeResult {
        List<Triple> result;
        int[] num;
        int[] cpot;

        TransposeResult(List<Triple> result, int[] num, int[] cpot) {
            this.result = result;
            this.num = num;
            this.cpot = cpot;
        }
    }

    private TransposeResult fastTranspose() {
        int[] num = new int[cols];  // 統計每一列的非零元素個數
        int[] cpot = new int[cols]; // 每一列第一個非零元素在轉置後的位置

        // 統計每一列的非零元素個數
        for (Triple t : sparseMatrix) {
            num[t.col]++;
        }

        // 計算每一列第一個非零元素在轉置後的位置
        cpot[0] = 0;
        for (int i = 1; i < cols; i++) {
            cpot[i] = cpot[i - 1] + num[i - 1];
        }

        // 執行快速轉置
        List<Triple> result = new ArrayList<>(Collections.nCopies(sparseMatrix.size(), null));
        for (Triple t : sparseMatrix) {
            int pos = cpot[t.col];
            result.set(pos, new Triple(t.col, t.row, t.value));
            cpot[t.col]++;
        }

        return new TransposeResult(result, num, cpot);
    }

    private long fastTransposeWithTiming() {
        int[] num = new int[cols];  // 統計每一列的非零元素個數
        int[] cpot = new int[cols]; // 每一列第一個非零元素在轉置後的位置

        // 統計每一列的非零元素個數
        for (Triple t : sparseMatrix) {
            num[t.col]++;
        }

        // 計算每一列第一個非零元素在轉置後的位置
        cpot[0] = 0;
        for (int i = 1; i < cols; i++) {
            cpot[i] = cpot[i - 1] + num[i - 1];
        }

        // 只計算核心轉置部分的時間
        long startTime = System.nanoTime();

        // 執行快速轉置
        List<Triple> result = new ArrayList<>(Collections.nCopies(sparseMatrix.size(), null));
        for (Triple t : sparseMatrix) {
            int pos = cpot[t.col];
            result.set(pos, new Triple(t.col, t.row, t.value));
            cpot[t.col]++;
        }

        long endTime = System.nanoTime();

        // 將結果暫存以供顯示使用
        this.method2Result = new TransposeResult(result, num, cpot);

        return endTime - startTime;
    }

    private void displayMethod1Result(List<Triple> result, long time) {
        // 左側：三元組
        StringBuilder triplesSb = new StringBuilder();
        triplesSb.append("原始三元組:\n");
        triplesSb.append("(列, 行, 值)\n");
        for (Triple t : sparseMatrix) {
            triplesSb.append(t.toString()).append("\n");
        }

        triplesSb.append("\n轉置後三元組:\n");
        triplesSb.append("(列, 行, 值)\n");
        for (Triple t : result) {
            triplesSb.append(t.toString()).append("\n");
        }

        method1TripleArea.setText(triplesSb.toString());

        // 右側：轉置後矩陣
        StringBuilder resultSb = new StringBuilder();
        resultSb.append("轉置後矩陣 (").append(cols).append("×").append(rows).append("):\n");
        int[][] transposedMatrix = new int[cols][rows];
        for (Triple t : result) {
            transposedMatrix[t.row][t.col] = t.value;
        }

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                resultSb.append(String.format("%3d ", transposedMatrix[i][j]));
            }
            resultSb.append("\n");
        }

        method1ResultArea.setText(resultSb.toString());
        method1TimeLabel.setText(String.format("執行時間: %.2f 微秒", time / 1000.0));
    }

    private void displayMethod2Result(TransposeResult result, long time) {
        // 左側：num陣列
        StringBuilder numSb = new StringBuilder();
        numSb.append("各列非零元素個數:\n");
        for (int i = 0; i < cols; i++) {
            numSb.append(String.format("列%d: %d個\n", i, result.num[i]));
        }
        method2NumArea.setText(numSb.toString());

        // 中間：cpot陣列
        StringBuilder cpotSb = new StringBuilder();
        cpotSb.append("各列在轉置後的起始位置:\n");
        // 重新計算cpot用於顯示
        int[] displayCpot = new int[cols];
        displayCpot[0] = 0;
        for (int i = 1; i < cols; i++) {
            displayCpot[i] = displayCpot[i - 1] + result.num[i - 1];
        }
        for (int i = 0; i < cols; i++) {
            cpotSb.append(String.format("列%d: 位置%d\n", i, displayCpot[i]));
        }
        method2CpotArea.setText(cpotSb.toString());

        // 右側：轉置後矩陣
        StringBuilder resultSb = new StringBuilder();
        resultSb.append("轉置後矩陣 (").append(cols).append("×").append(rows).append("):\n");
        int[][] transposedMatrix = new int[cols][rows];
        for (Triple t : result.result) {
            transposedMatrix[t.row][t.col] = t.value;
        }

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                resultSb.append(String.format("%3d ", transposedMatrix[i][j]));
            }
            resultSb.append("\n");
        }

        method2ResultArea.setText(resultSb.toString());
        method2TimeLabel.setText(String.format("執行時間: %.2f 微秒", time / 1000.0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SparseMatrixTransposer().setVisible(true);
        });
    }
}