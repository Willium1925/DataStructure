package ClaudePlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 公因數計算器 - 具有GUI介面的Java程式
 * 可輸入兩個數字並找到其全部公因數
 *
 * 相容版本：Java 8+ (建議使用Java 21 LTS)
 * GUI框架：Java Swing (內建於JDK)
 */
public class CommonFactorCalculator extends JFrame {

    // GUI 元件
    private JTextField number1Field;
    private JTextField number2Field;
    private JTextArea resultArea;
    private JButton calculateButton;
    private JButton clearButton;
    private JLabel titleLabel;
    private JLabel number1Label;
    private JLabel number2Label;
    private JLabel resultLabel;

    public CommonFactorCalculator() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupWindow();
    }

    /**
     * 初始化所有GUI元件
     */
    private void initializeComponents() {
        // 標題
        titleLabel = new JLabel("公因數計算器", JLabel.CENTER);
        titleLabel.setFont(new Font("微軟正黑體", Font.BOLD, 24));
        titleLabel.setForeground(new Color(51, 102, 153));

        // 輸入標籤
        number1Label = new JLabel("第一個數字：");
        number1Label.setFont(new Font("微軟正黑體", Font.PLAIN, 14));

        number2Label = new JLabel("第二個數字：");
        number2Label.setFont(new Font("微軟正黑體", Font.PLAIN, 14));

        // 輸入欄位
        number1Field = new JTextField(15);
        number1Field.setFont(new Font("Arial", Font.PLAIN, 14));
        number1Field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        number2Field = new JTextField(15);
        number2Field.setFont(new Font("Arial", Font.PLAIN, 14));
        number2Field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // 按鈕
        calculateButton = new JButton("計算公因數");
        calculateButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        calculateButton.setBackground(new Color(51, 153, 102));
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setFocusPainted(false);
        calculateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        clearButton = new JButton("清除");
        clearButton.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        clearButton.setBackground(new Color(153, 102, 51));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 結果標籤
        resultLabel = new JLabel("計算結果：");
        resultLabel.setFont(new Font("微軟正黑體", Font.BOLD, 14));

        // 結果顯示區域
        resultArea = new JTextArea(10, 30);
        resultArea.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(248, 248, 248));
        resultArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
    }

    /**
     * 設置視窗佈局
     */
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        // 頂部面板 - 標題
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.WHITE);
        topPanel.add(titleLabel);

        // 中間面板 - 輸入區域
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // 第一個數字輸入
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(number1Label, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(number1Field, gbc);

        // 第二個數字輸入
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(number2Label, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(number2Field, gbc);

        // 按鈕面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(calculateButton);
        buttonPanel.add(clearButton);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(buttonPanel, gbc);

        // 底部面板 - 結果顯示
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        bottomPanel.add(resultLabel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        // 將所有面板加入主視窗
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 設置事件處理器
     */
    private void setupEventHandlers() {
        // 計算按鈕事件
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateCommonFactors();
            }
        });

        // 清除按鈕事件
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });

        // Enter鍵快捷鍵
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        getRootPane().getInputMap().put(enterKey, "calculate");
        getRootPane().getActionMap().put("calculate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateCommonFactors();
            }
        });
    }

    /**
     * 設置視窗屬性
     */
    private void setupWindow() {
        setTitle("公因數計算器 v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(500, 600));
        pack();
        setLocationRelativeTo(null); // 視窗置中

        // 設置圖示（如果有的話）
        try {
            // 可以在這裡設置自定義圖示
            // setIconImage(ImageIO.read(new File("icon.png")));
        } catch (Exception e) {
            // 忽略圖示載入錯誤
        }
    }

    /**
     * 計算兩個數字的所有公因數
     */
    private void calculateCommonFactors() {
        try {
            // 獲取輸入值
            String input1 = number1Field.getText().trim();
            String input2 = number2Field.getText().trim();

            // 驗證輸入
            if (input1.isEmpty() || input2.isEmpty()) {
                showError("請輸入兩個數字！");
                return;
            }

            int num1 = Integer.parseInt(input1);
            int num2 = Integer.parseInt(input2);

            // 檢查是否為正整數
            if (num1 <= 0 || num2 <= 0) {
                showError("請輸入大於0的正整數！");
                return;
            }

            // 計算公因數
            ArrayList<Integer> commonFactors = findAllCommonFactors(num1, num2);

            // 顯示結果
            displayResults(num1, num2, commonFactors);

        } catch (NumberFormatException e) {
            showError("請輸入有效的整數！");
        } catch (Exception e) {
            showError("計算過程中發生錯誤：" + e.getMessage());
        }
    }

    /**
     * 找到兩個數字的所有公因數
     * @param num1 第一個數字
     * @param num2 第二個數字
     * @return 所有公因數的列表（從小到大排序）
     */
    private ArrayList<Integer> findAllCommonFactors(int num1, int num2) {
        ArrayList<Integer> factors1 = findFactors(num1);
        ArrayList<Integer> factors2 = findFactors(num2);
        ArrayList<Integer> commonFactors = new ArrayList<>();

        // 找出共同的因數
        for (int factor : factors1) {
            if (factors2.contains(factor)) {
                commonFactors.add(factor);
            }
        }

        Collections.sort(commonFactors);
        return commonFactors;
    }

    /**
     * 找到一個數字的所有因數
     * @param num 要找因數的數字
     * @return 該數字的所有因數
     */
    private ArrayList<Integer> findFactors(int num) {
        ArrayList<Integer> factors = new ArrayList<>();

        // 只需檢查到平方根，因為因數是成對出現的
        for (int i = 1; i <= Math.sqrt(num); i++) {
            if (num % i == 0) {
                factors.add(i);
                // 如果i不是平方根，則同時加入對應的另一個因數
                if (i != num / i) {
                    factors.add(num / i);
                }
            }
        }

        Collections.sort(factors);
        return factors;
    }

    /**
     * 顯示計算結果
     */
    private void displayResults(int num1, int num2, ArrayList<Integer> commonFactors) {
        StringBuilder result = new StringBuilder();

        result.append("═══════════════════════════════════════\n");
        result.append("           計算結果\n");
        result.append("═══════════════════════════════════════\n\n");

        result.append("輸入的數字：\n");
        result.append("• 第一個數字：").append(num1).append("\n");
        result.append("• 第二個數字：").append(num2).append("\n\n");

        // 顯示各自的因數
        ArrayList<Integer> factors1 = findFactors(num1);
        ArrayList<Integer> factors2 = findFactors(num2);

        result.append("各數字的因數：\n");
        result.append("• ").append(num1).append(" 的因數：").append(factors1.toString()).append("\n");
        result.append("• ").append(num2).append(" 的因數：").append(factors2.toString()).append("\n\n");

        // 顯示公因數
        result.append("公因數：\n");
        if (commonFactors.isEmpty()) {
            result.append("• 沒有公因數（除了1以外）\n");
        } else {
            result.append("• 所有公因數：").append(commonFactors.toString()).append("\n");
            result.append("• 公因數個數：").append(commonFactors.size()).append(" 個\n");
            result.append("• 最大公因數 (GCD)：").append(Collections.max(commonFactors)).append("\n");
        }

        result.append("\n═══════════════════════════════════════\n");

        // 顯示計算方法說明
        result.append("\n計算方法說明：\n");
        result.append("1. 分別找出兩個數字的所有因數\n");
        result.append("2. 比較兩個因數列表，找出相同的數字\n");
        result.append("3. 將公因數從小到大排序顯示\n");

        resultArea.setText(result.toString());
        resultArea.setCaretPosition(0); // 滾動到頂部
    }

    /**
     * 顯示錯誤訊息
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        resultArea.setText("請重新輸入正確的數字。");
    }

    /**
     * 清除所有欄位
     */
    private void clearFields() {
        number1Field.setText("");
        number2Field.setText("");
        resultArea.setText("請輸入兩個正整數，然後點擊「計算公因數」按鈕。\n\n" +
                "提示：\n" +
                "• 可以使用 Enter 鍵快速計算\n" +
                "• 支援任何大於0的正整數\n" +
                "• 程式會顯示所有公因數及詳細說明");
        number1Field.requestFocus();
    }

    /**
     * 主程式入口點
     */
    public static void main(String[] args) {
        // 設置系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 如果無法設置系統外觀，使用預設外觀
        }

        // 在事件派發執行緒中建立和顯示GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CommonFactorCalculator().setVisible(true);
            }
        });
    }
}