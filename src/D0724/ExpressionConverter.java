package D0724;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;

/**
 * 中序運算式轉換器 - 將中序運算式轉換為前序和後序運算式
 * 支援：加減乘除、次方、括號、負號
 * 會將不必要的 0 清除
 */
public class ExpressionConverter extends JFrame {

    // GUI 元件
    private JTextField inputField;      // 輸入欄位
    private JTextArea prefixResult;     // 前序結果顯示
    private JTextArea postfixResult;    // 後序結果顯示
    private JButton convertButton;      // 轉換按鈕
    private JButton clearButton;        // 清除按鈕

    public ExpressionConverter() {
        initializeGUI();
    }

    /**
     * 初始化GUI介面
     */
    private void initializeGUI() {
        setTitle("中序運算式轉換器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 建立主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 輸入標籤和欄位
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 5, 5);
        mainPanel.add(new JLabel("中序運算式:"), gbc);

        inputField = new JTextField(30);
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.insets = new Insets(10, 5, 5, 10);
        mainPanel.add(inputField, gbc);

        // 前序結果標籤和顯示區域
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(5, 10, 5, 5);
        mainPanel.add(new JLabel("前序運算式:"), gbc);

        prefixResult = new JTextArea(3, 30);
        prefixResult.setEditable(false);
        prefixResult.setFont(new Font("Monospaced", Font.PLAIN, 14));
        prefixResult.setBackground(new Color(240, 240, 240));
        JScrollPane prefixScroll = new JScrollPane(prefixResult);
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 10);
        mainPanel.add(prefixScroll, gbc);

        // 後序結果標籤和顯示區域
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.insets = new Insets(5, 10, 5, 5);
        mainPanel.add(new JLabel("後序運算式:"), gbc);

        postfixResult = new JTextArea(3, 30);
        postfixResult.setEditable(false);
        postfixResult.setFont(new Font("Monospaced", Font.PLAIN, 14));
        postfixResult.setBackground(new Color(240, 240, 240));
        JScrollPane postfixScroll = new JScrollPane(postfixResult);
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.insets = new Insets(5, 5, 5, 10);
        mainPanel.add(postfixScroll, gbc);

        // 按鈕面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        convertButton = new JButton("轉換");
        clearButton = new JButton("清除");

        // 設定按鈕樣式
        convertButton.setPreferredSize(new Dimension(80, 30));
        clearButton.setPreferredSize(new Dimension(80, 30));

        buttonPanel.add(convertButton);
        buttonPanel.add(clearButton);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 10, 10);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // 新增事件監聽器
        convertButton.addActionListener(new ConvertAction());
        clearButton.addActionListener(new ClearAction());

        // 讓輸入欄位按Enter也能觸發轉換
        inputField.addActionListener(new ConvertAction());

        // 設定視窗大小和位置
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * 轉換按鈕事件處理
     */
    private class ConvertAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String infix = inputField.getText().trim();
            if (infix.isEmpty()) {
                JOptionPane.showMessageDialog(ExpressionConverter.this,
                        "請輸入中序運算式", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // 轉換為前序和後序
                String prefix = infixToPrefix(infix);
                String postfix = infixToPostfix(infix);

                prefixResult.setText(prefix);
                postfixResult.setText(postfix);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ExpressionConverter.this,
                        "轉換錯誤: " + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 清除按鈕事件處理
     */
    private class ClearAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            inputField.setText("");
            prefixResult.setText("");
            postfixResult.setText("");
            inputField.requestFocus();
        }
    }

    /**
     * 取得運算子的優先級
     * @param operator 運算子
     * @return 優先級數值（數值越大優先級越高）
     */
    private int getPrecedence(char operator) {
        switch (operator) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            case '^':  // 次方運算子
                return 3;
            case '~':  // 負號運算子（一元運算子，最高優先級）
                return 4;
            default:
                return 0;
        }
    }

    /**
     * 判斷字元是否為運算子
     * @param ch 字元
     * @return 是否為運算子
     */
    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^' || ch == '~';
    }

    /**
     * 判斷字元是否為數字或字母（運算元）
     * @param ch 字元
     * @return 是否為運算元
     */
    private boolean isOperand(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '.';
    }

    /**
     * 將中序運算式轉換為後序運算式
     * @param infix 中序運算式
     * @return 後序運算式
     */
    private String infixToPostfix(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();

        // 預處理：處理負號
        infix = preprocessNegativeSign(infix);

        boolean lastWasOperand = false; // 追蹤上一個是否為運算元

        for (int i = 0; i < infix.length(); i++) {
            char ch = infix.charAt(i);

            // 跳過空格
            if (ch == ' ') {
                continue;
            }

            // 如果是運算元（數字、字母、小數點）
            if (isOperand(ch)) {
                if (lastWasOperand) {
                    // 如果上一個也是運算元，不需要空格（連續數字）
                }
                result.append(ch);
                lastWasOperand = true;
            }
            // 如果是左括號
            else if (ch == '(') {
                stack.push(ch);
                lastWasOperand = false;
            }
            // 如果是右括號
            else if (ch == ')') {
                // 如果剛結束運算元，加空格
                if (lastWasOperand) {
                    result.append(' ');
                }
                // 彈出所有運算子直到遇到左括號
                while (!stack.isEmpty() && stack.peek() != '(') {
                    char op = stack.pop();
                    if (op == '~') {
                        result.append('-'); // 將 ~ 轉回 -
                    } else {
                        result.append(op);
                    }
                    result.append(' ');
                }
                if (!stack.isEmpty()) {
                    stack.pop(); // 移除左括號
                }
                lastWasOperand = false;
            }
            // 如果是運算子
            else if (isOperator(ch)) {
                // 如果剛結束運算元，加空格
                if (lastWasOperand) {
                    result.append(' ');
                }
                // 對於一元負號，直接處理（右結合）
                if (ch == '~') {
                    // 一元負號為右結合，優先級最高
                    while (!stack.isEmpty() && stack.peek() != '(' &&
                            getPrecedence(stack.peek()) > getPrecedence(ch)) {
                        char op = stack.pop();
                        if (op == '~') {
                            result.append('-');
                        } else {
                            result.append(op);
                        }
                        result.append(' ');
                    }
                } else {
                    // 彈出優先級較高或相等的運算子（次方為右結合，特殊處理）
                    while (!stack.isEmpty() && stack.peek() != '(' &&
                            ((ch != '^' && getPrecedence(stack.peek()) >= getPrecedence(ch)) ||
                                    (ch == '^' && getPrecedence(stack.peek()) > getPrecedence(ch)))) {
                        char op = stack.pop();
                        if (op == '~') {
                            result.append('-');
                        } else {
                            result.append(op);
                        }
                        result.append(' ');
                    }
                }
                stack.push(ch);
                lastWasOperand = false;
            }
        }

        // 如果最後是運算元，加空格
        if (lastWasOperand && !stack.isEmpty()) {
            result.append(' ');
        }

        // 彈出剩餘的運算子
        while (!stack.isEmpty()) {
            if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                result.append(' ');
            }
            char op = stack.pop();
            if (op == '~') {
                result.append('-'); // 將 ~ 轉回 -
            } else {
                result.append(op);
            }
            if (!stack.isEmpty()) {
                result.append(' ');
            }
        }

        return result.toString().trim();
    }

    /**
     * 將中序運算式轉換為前序運算式
     * @param infix 中序運算式
     * @return 前序運算式
     */
    private String infixToPrefix(String infix) {
        // 步驟1：反轉運算式
        StringBuilder reversed = new StringBuilder(infix).reverse();

        // 步驟2：交換括號
        for (int i = 0; i < reversed.length(); i++) {
            if (reversed.charAt(i) == '(') {
                reversed.setCharAt(i, ')');
            } else if (reversed.charAt(i) == ')') {
                reversed.setCharAt(i, '(');
            }
        }

        // 步驟3：獲得修改後的後序運算式
        String modifiedPostfix = infixToPostfixForPrefix(reversed.toString());

        // 步驟4：反轉結果得到前序運算式
        return new StringBuilder(modifiedPostfix).reverse().toString();
    }

    /**
     * 為前序轉換特製的後序轉換方法
     * @param infix 修改過的中序運算式
     * @return 後序運算式
     */
    private String infixToPostfixForPrefix(String infix) {
        StringBuilder result = new StringBuilder();
        Stack<Character> stack = new Stack<>();

        // 預處理：處理負號
        infix = preprocessNegativeSign(infix);

        boolean lastWasOperand = false; // 追蹤上一個是否為運算元

        for (int i = 0; i < infix.length(); i++) {
            char ch = infix.charAt(i);

            if (ch == ' ') continue;

            // 如果是運算元
            if (isOperand(ch)) {
                if (lastWasOperand && result.length() > 0) {
                    result.append(' ');
                }
                result.append(ch);
                lastWasOperand = true;
            }
            // 如果是左括號
            else if (ch == '(') {
                stack.push(ch);
                lastWasOperand = false;
            }
            // 如果是右括號
            else if (ch == ')') {
                if (lastWasOperand) {
                    result.append(' ');
                }
                while (!stack.isEmpty() && stack.peek() != '(') {
                    char op = stack.pop();
                    result.append(op == '~' ? '-' : op).append(' ');
                }
                if (!stack.isEmpty()) {
                    stack.pop();
                }
                lastWasOperand = false;
            }
            // 如果是運算子
            else if (isOperator(ch)) {
                if (lastWasOperand) {
                    result.append(' ');
                }

                while (!stack.isEmpty() && stack.peek() != '(' &&
                      getPrecedence(stack.peek()) >= getPrecedence(ch)) {
                    char op = stack.pop();
                    result.append(op == '~' ? '-' : op).append(' ');
                }
                stack.push(ch);
                lastWasOperand = false;
            }
        }

        if (lastWasOperand) {
            result.append(' ');
        }

        // 彈出剩餘的運算子
        while (!stack.isEmpty()) {
            char op = stack.pop();
            if (op != '(') {
                result.append(op == '~' ? '-' : op);
                if (!stack.isEmpty()) {
                    result.append(' ');
                }
            }
        }

        return result.toString().trim();
    }

    /**
     * 預處理負號：使用特殊符號代替負號
     * @param expression 原始運算式
     * @return 處理後的運算式
     */
    private String preprocessNegativeSign(String expression) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);

            // 跳過空格但保留在結果中
            if (ch == ' ') {
                result.append(ch);
                continue;
            }

            if (ch == '-') {
                // 判斷是否為負號而非減號
                boolean isNegative = false;

                if (i == 0) {
                    isNegative = true;
                } else {
                    // 往前找非空格字元
                    int prevIndex = i - 1;
                    while (prevIndex >= 0 && expression.charAt(prevIndex) == ' ') {
                        prevIndex--;
                    }
                    if (prevIndex >= 0) {
                        char prevChar = expression.charAt(prevIndex);
                        isNegative = (prevChar == '(' || isOperator(prevChar));
                    } else {
                        isNegative = true;
                    }
                }

                if (isNegative) {
                    // 使用特殊字元 ~ 代表負號運算子
                    result.append('~');
                } else {
                    // 普通的減號
                    result.append(ch);
                }
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    /**
     * 主方法 - 程式入口點
     */
    public static void main(String[] args) {
        // 設定系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 如果設定失敗，使用預設外觀
        }

        // 在事件派發執行緒中啟動GUI
        SwingUtilities.invokeLater(() -> {
            new ExpressionConverter().setVisible(true);
        });
    }
}

