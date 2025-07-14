package D0714;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;

public class FactorialPerformanceGUI extends JFrame {
    private JTextField inputField;
    private JTextArea resultArea;
    private JButton compareButton;
    private JButton clearButton;

    public FactorialPerformanceGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("階乘效能比較器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // 創建面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 頂部輸入面板
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("請輸入數字："));
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
    }

    private class CompareButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int n = Integer.parseInt(inputField.getText().trim());

                if (n < 0) {
                    JOptionPane.showMessageDialog(FactorialPerformanceGUI.this,
                            "請輸入非負整數！", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (n > 10000) {
                    int choice = JOptionPane.showConfirmDialog(FactorialPerformanceGUI.this,
                            "輸入數字較大，可能需要較長時間計算，是否繼續？",
                            "確認", JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                performanceTest(n);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(FactorialPerformanceGUI.this,
                        "請輸入有效的整數！", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performanceTest(int n) {
        resultArea.append("=".repeat(50) + "\n");
        resultArea.append("階乘效能測試 - 數字: " + n + "\n");
        resultArea.append("=".repeat(50) + "\n");

        // 執行多次測試取平均值
        int testRounds = 100;
        long recursiveTotal = 0;
        long iterativeTotal = 0;

        BigInteger recursiveResult = null;
        BigInteger iterativeResult = null;

        // 暖身執行（避免JIT編譯器影響）
        for (int i = 0; i < 10; i++) {
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
        resultArea.append(String.format("遞迴方法平均執行時間: %.2f 納秒\n", recursiveAvg));
        resultArea.append(String.format("迴圈方法平均執行時間: %.2f 納秒\n", iterativeAvg));

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
        if (n < 1000) {
            resultArea.append("遞迴方法: 使用堆疊空間 O(n)，可能導致堆疊溢位\n");
        } else {
            resultArea.append("遞迴方法: 對於大數值，極可能導致 StackOverflowError\n");
        }
        resultArea.append("迴圈方法: 使用常數空間 O(1)，記憶體使用效率較高\n");

        // 顯示結果值（如果不太大）
        if (n <= 20) {
            resultArea.append("\n【計算結果】\n");
            resultArea.append(n + "! = " + recursiveResult.toString() + "\n");
        } else {
            resultArea.append("\n【計算結果】\n");
            String result = recursiveResult.toString();
            if (result.length() > 100) {
                resultArea.append(n + "! = " + result.substring(0, 50) + "...(共" + result.length() + "位數)\n");
            } else {
                resultArea.append(n + "! = " + result + "\n");
            }
        }

        // 驗證結果一致性
        if (recursiveResult.equals(iterativeResult)) {
            resultArea.append("✓ 兩種方法計算結果一致\n");
        } else {
            resultArea.append("✗ 警告：兩種方法計算結果不一致！\n");
        }

        resultArea.append("\n");

        // 自動滾動到底部
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
    }

    // 遞迴方法計算階乘
    private BigInteger factorialRecursive(int n) {
        if (n <= 1) {
            return BigInteger.ONE;
        }
        return BigInteger.valueOf(n).multiply(factorialRecursive(n - 1));
    }

    // 迴圈方法計算階乘
    private BigInteger factorialIterative(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
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