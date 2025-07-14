package D0714;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FactorialPerformanceGuiInt extends JFrame {
    private JTextField inputField;
    private JTextArea resultArea;
    private JButton compareButton;
    private JButton clearButton;

    // int類型階乘的安全上限（13! = 6,227,020,800 仍在int範圍內）
    private static final int MAX_FACTORIAL_INT = 12;

    public FactorialPerformanceGuiInt() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("階乘效能比較器（int版本）");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // 創建面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 頂部輸入面板
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("請輸入數字（0-" + MAX_FACTORIAL_INT + "）："));
        inputField = new JTextField(10);
        inputPanel.add(inputField);

        compareButton = new JButton("開始比較");
        clearButton = new JButton("清除結果");
        inputPanel.add(compareButton);
        inputPanel.add(clearButton);

        // 結果顯示區域
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(580, 400));

        // 添加事件監聽器
        compareButton.addActionListener(new CompareButtonListener());
        clearButton.addActionListener(e -> resultArea.setText(""));

        // 組裝界面
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // 顯示初始說明
        showInitialInfo();
    }

    private void showInitialInfo() {
        resultArea.append("=".repeat(50) + "\n");
        resultArea.append("階乘效能比較器（int版本）\n");
        resultArea.append("=".repeat(50) + "\n");
        resultArea.append("說明：\n");
        resultArea.append("• 使用int類型進行階乘計算\n");
        resultArea.append("• 輸入範圍：0 到 " + MAX_FACTORIAL_INT + "\n");
        resultArea.append("• 超過 " + MAX_FACTORIAL_INT + " 會導致整數溢位\n");
        resultArea.append("• 程式將比較遞迴和迴圈兩種方法的效能\n");
        resultArea.append("\n參考值：\n");
        for (int i = 0; i <= MAX_FACTORIAL_INT; i++) {
            resultArea.append(String.format("%2d! = %,d\n", i, factorialIterative(i)));
        }
        resultArea.append("\n請在上方輸入數字並點擊「開始比較」\n");
        resultArea.append("=".repeat(50) + "\n\n");
    }

    private class CompareButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int n = Integer.parseInt(inputField.getText().trim());

                if (n < 0) {
                    JOptionPane.showMessageDialog(FactorialPerformanceGuiInt.this,
                            "請輸入非負整數！", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (n > MAX_FACTORIAL_INT) {
                    JOptionPane.showMessageDialog(FactorialPerformanceGuiInt.this,
                            String.format("輸入數字過大！\n" +
                                            "int類型最大安全計算範圍：0 到 %d\n" +
                                            "%d! = %,d（接近int上限）\n" +
                                            "請輸入 %d 以下的數字",
                                    MAX_FACTORIAL_INT, MAX_FACTORIAL_INT,
                                    factorialIterative(MAX_FACTORIAL_INT), MAX_FACTORIAL_INT),
                            "數值超出範圍", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                performanceTest(n);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(FactorialPerformanceGuiInt.this,
                        "請輸入有效的整數！", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performanceTest(int n) {
        resultArea.append("=".repeat(50) + "\n");
        resultArea.append("階乘效能測試 - 數字: " + n + "\n");
        resultArea.append("=".repeat(50) + "\n");

        // 執行多次測試取平均值
        int testRounds = 1000000; // 因為計算簡單，增加測試次數提高精度
        long recursiveTotal = 0;
        long iterativeTotal = 0;

        int recursiveResult = 0;
        int iterativeResult = 0;

        // 暖身執行（避免JIT編譯器影響）
        for (int i = 0; i < 10000; i++) {
            factorialRecursive(n);
            factorialIterative(n);
        }

        // 測試遞迴方法
        resultArea.append("測試遞迴方法...\n");
        for (int i = 0; i < testRounds; i++) {
            long start = System.nanoTime();
            recursiveResult = factorialRecursive(n);
            long end = System.nanoTime();
            recursiveTotal += (end - start);
        }

        // 測試迴圈方法
        resultArea.append("測試迴圈方法...\n");
        for (int i = 0; i < testRounds; i++) {
            long start = System.nanoTime();
            iterativeResult = factorialIterative(n);
            long end = System.nanoTime();
            iterativeTotal += (end - start);
        }

        // 計算平均執行時間
        double recursiveAvg = recursiveTotal / (double) testRounds;
        double iterativeAvg = iterativeTotal / (double) testRounds;

        // 顯示結果
        resultArea.append("\n【測試結果】\n");
        resultArea.append(String.format("測試輪數: %,d 次\n", testRounds));
        resultArea.append(String.format("遞迴方法平均執行時間: %.3f 納秒\n", recursiveAvg));
        resultArea.append(String.format("迴圈方法平均執行時間: %.3f 納秒\n", iterativeAvg));

        // 效能比較
        if (recursiveAvg < iterativeAvg) {
            double improvement = ((iterativeAvg - recursiveAvg) / iterativeAvg) * 100;
            resultArea.append(String.format("遞迴方法較快，快了 %.2f%%\n", improvement));
        } else {
            double improvement = ((recursiveAvg - iterativeAvg) / recursiveAvg) * 100;
            resultArea.append(String.format("迴圈方法較快，快了 %.2f%%\n", improvement));
        }

        // 記憶體使用分析
        resultArea.append("\n【記憶體使用分析】\n");
        resultArea.append(String.format("遞迴方法: 使用堆疊空間 O(n) = %d 層堆疊調用\n", n));
        resultArea.append("迴圈方法: 使用常數空間 O(1)，記憶體使用效率較高\n");

        // 堆疊深度警告
        if (n > 100) {
            resultArea.append("⚠️  注意：遞迴深度較深，可能影響效能\n");
        }

        // 顯示計算結果
        resultArea.append("\n【計算結果】\n");
        resultArea.append(String.format("%d! = %,d\n", n, recursiveResult));

        // 驗證結果一致性
        if (recursiveResult == iterativeResult) {
            resultArea.append("✓ 兩種方法計算結果一致\n");
        } else {
            resultArea.append("✗ 警告：兩種方法計算結果不一致！\n");
        }

        // 顯示int類型限制資訊
        resultArea.append("\n【int類型限制】\n");
        resultArea.append("int類型範圍: -2,147,483,648 到 2,147,483,647\n");
        resultArea.append(String.format("當前結果 %,d 佔用範圍: %.2f%%\n",
                recursiveResult, (recursiveResult / 2147483647.0) * 100));

        if (n == MAX_FACTORIAL_INT) {
            resultArea.append("⚠️  這是int類型能安全計算的最大階乘值\n");
        }

        resultArea.append("\n");

        // 自動滾動到底部
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
    }

    // 遞迴方法計算階乘
    private int factorialRecursive(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * factorialRecursive(n - 1);
    }

    // 迴圈方法計算階乘
    private int factorialIterative(int n) {
        int result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public static void main(String[] args) {
        // 設置Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new FactorialPerformanceGuiInt().setVisible(true);
        });
    }
}