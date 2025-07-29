package D0729;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;

/**
 * 工廠機台流程軟體 - 生產者消費者模式
 * 使用 PriorityBlockingQueue 來快速獲取最小編號的物品
 */
public class FactoryMachineSystem extends JFrame {

    // 物品類別 - 包含生產時間和編號
    static class Item implements Comparable<Item> {
        private final LocalDateTime productionTime;  // 生產時間
        private final int itemNumber;                // 物品編號

        public Item(LocalDateTime productionTime, int itemNumber) {
            this.productionTime = productionTime;
            this.itemNumber = itemNumber;
        }

        public LocalDateTime getProductionTime() {
            return productionTime;
        }

        public int getItemNumber() {
            return itemNumber;
        }

        // 實現 Comparable 接口，用於優先隊列排序（按編號從小到大）
        @Override
        public int compareTo(Item other) {
            return Integer.compare(this.itemNumber, other.itemNumber);
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            return String.format("編號: %d (生產時間: %s)",
                    itemNumber, productionTime.format(formatter));
        }
    }

    // 緩衝區 - 使用 PriorityBlockingQueue 自動維護最小堆
    // 這樣可以在 O(log n) 時間內插入，O(log n) 時間內取出最小值
    private PriorityBlockingQueue<Item> buffer;

    // 控制變數
    private AtomicBoolean isRunning = new AtomicBoolean(false);  // 系統運行狀態
    private int maxBufferSize = 10;  // 緩衝區最大大小
    private Random random = new Random();  // 隨機數生成器

    // GUI 組件
    private JButton startButton;
    private JButton stopButton;
    private JSpinner bufferSizeSpinner;
    private JTextArea bufferDisplayArea;
    private JTextArea processLogArea;
    private JLabel statusLabel;
    private JLabel producedCountLabel;
    private JLabel consumedCountLabel;

    // 統計數據
    private volatile int producedCount = 0;
    private volatile int consumedCount = 0;

    public FactoryMachineSystem() {
        initializeGUI();
        buffer = new PriorityBlockingQueue<>(maxBufferSize);
    }

    /**
     * 初始化GUI介面
     */
    private void initializeGUI() {
        setTitle("工廠機台流程軟體系統");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 控制面板
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // 顯示面板 (分割為緩衝區和日誌)
        JSplitPane displayPanel = createDisplayPanel();
        add(displayPanel, BorderLayout.CENTER);

        // 狀態面板
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);

        setSize(900, 600);  // 增加視窗寬度來容納日誌面板
        setLocationRelativeTo(null);
    }

    /**
     * 創建控制面板
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        // 緩衝區大小設定
        panel.add(new JLabel("緩衝區大小:"));
        bufferSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        bufferSizeSpinner.setPreferredSize(new Dimension(60, 25));
        panel.add(bufferSizeSpinner);

        // 開始按鈕
        startButton = new JButton("開始生產");
        startButton.addActionListener(new StartButtonListener());
        panel.add(startButton);

        // 停止按鈕
        stopButton = new JButton("停止生產");
        stopButton.addActionListener(new StopButtonListener());
        stopButton.setEnabled(false);
        panel.add(stopButton);

        return panel;
    }

    /**
     * 創建顯示面板 (分割為緩衝區顯示和處理日誌)
     */
    private JSplitPane createDisplayPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5); // 兩邊各佔50%

        // 緩衝區顯示面板
        JPanel bufferPanel = new JPanel(new BorderLayout());
        bufferPanel.setBorder(BorderFactory.createTitledBorder("緩衝區內容"));

        bufferDisplayArea = new JTextArea(20, 25);
        bufferDisplayArea.setEditable(false);
        bufferDisplayArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane bufferScrollPane = new JScrollPane(bufferDisplayArea);
        bufferScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        bufferPanel.add(bufferScrollPane, BorderLayout.CENTER);

        // 處理日誌面板
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("處理日誌"));

        processLogArea = new JTextArea(20, 35);
        processLogArea.setEditable(false);
        processLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        processLogArea.setBackground(new Color(248, 248, 248));

        JScrollPane logScrollPane = new JScrollPane(processLogArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 清除日誌按鈕
        JButton clearLogButton = new JButton("清除日誌");
        clearLogButton.addActionListener(e -> {
            processLogArea.setText("");
            appendToLog("═══════════════════════════════════════");
            appendToLog("日誌已清除 - " + getCurrentTimeString());
            appendToLog("═══════════════════════════════════════");
        });

        JPanel logControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logControlPanel.add(clearLogButton);

        logPanel.add(logScrollPane, BorderLayout.CENTER);
        logPanel.add(logControlPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(bufferPanel);
        splitPane.setRightComponent(logPanel);

        return splitPane;
    }

    /**
     * 創建狀態面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        statusLabel = new JLabel("狀態: 停止");
        producedCountLabel = new JLabel("已生產: 0");
        consumedCountLabel = new JLabel("已處理: 0");

        panel.add(statusLabel);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(producedCountLabel);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(consumedCountLabel);

        return panel;
    }

    /**
     * 獲取當前時間字符串
     */
    private String getCurrentTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        return LocalDateTime.now().format(formatter);
    }

    /**
     * 添加日誌到處理日誌區域
     */
    private void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            processLogArea.append(message + "\n");
            // 自動滾動到底部
            processLogArea.setCaretPosition(processLogArea.getDocument().getLength());
        });
    }

    /**
     * 添加帶時間戳的日誌
     */
    private void appendTimestampedLog(String category, String message) {
        String timestampedMessage = String.format("[%s] %s: %s",
                getCurrentTimeString(), category, message);
        appendToLog(timestampedMessage);
    }
    private class Producer implements Runnable {
        @Override
        public void run() {
            while (isRunning.get()) {
                try {
                    // 檢查緩衝區是否已滿
                    if (buffer.size() < maxBufferSize) {
                        // 生成新物品
                        int itemNumber = random.nextInt(900) + 100;  // 100-999之間的隨機數
                        LocalDateTime productionTime = LocalDateTime.now();
                        Item newItem = new Item(productionTime, itemNumber);

                        // 將物品放入緩衝區
                        buffer.offer(newItem);
                        producedCount++;

                        // 更新GUI顯示
                        SwingUtilities.invokeLater(() -> {
                            updateDisplay();
                            producedCountLabel.setText("已生產: " + producedCount);
                        });

                        System.out.println("生產者: 生產了物品 " + newItem);
                    }

                    // 生產週期約0.2秒
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 消費者執行緒 - 處理時間為生產時間的1.2倍（約0.24秒）
     */
    private class Consumer implements Runnable {
        @Override
        public void run() {
            appendTimestampedLog("系統", "消費者執行緒啟動");

            while (isRunning.get()) {
                try {
                    // 從緩衝區取出最小編號的物品
                    // PriorityBlockingQueue.poll() 會自動返回最小的元素
                    Item item = buffer.poll();

                    if (item != null) {
                        // 記錄開始處理日誌
                        appendTimestampedLog("消費者",
                                String.format("開始處理物品 編號:%d (緩衝區剩餘: %d)",
                                        item.getItemNumber(), buffer.size()));

                        // 更新GUI顯示
                        SwingUtilities.invokeLater(() -> {
                            updateDisplay();
                        });

                        // 處理時間約0.24秒（生產時間的1.2倍）
                        long startTime = System.currentTimeMillis();
                        Thread.sleep(240);
                        long endTime = System.currentTimeMillis();

                        consumedCount++;

                        // 記錄完成處理日誌
                        appendTimestampedLog("消費者",
                                String.format("完成處理物品 編號:%d (耗時: %dms)",
                                        item.getItemNumber(), (endTime - startTime)));

                        // 更新處理統計
                        SwingUtilities.invokeLater(() -> {
                            consumedCountLabel.setText("已處理: " + consumedCount);
                        });

                    } else {
                        // 如果緩衝區為空，記錄等待日誌（每5秒記錄一次避免日誌過多）
                        if (System.currentTimeMillis() % 5000 < 100) {
                            appendTimestampedLog("消費者", "緩衝區為空，等待生產者...");
                        }
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    appendTimestampedLog("系統", "消費者執行緒被中斷");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            appendTimestampedLog("系統", "消費者執行緒結束");
        }
    }

    /**
     * 更新GUI顯示緩衝區內容
     */
    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("緩衝區內容 (").append(buffer.size()).append("/").append(maxBufferSize).append("):\n");
        sb.append("═══════════════════════════════════\n");

        if (buffer.isEmpty()) {
            sb.append("緩衝區為空\n");
        } else {
            // 創建一個臨時陣列來顯示排序後的內容
            Object[] items = buffer.toArray();
            java.util.Arrays.sort(items);

            for (int i = 0; i < items.length; i++) {
                Item item = (Item) items[i];
                sb.append(String.format("%2d. %s\n", i + 1, item));
            }
        }

        bufferDisplayArea.setText(sb.toString());

        // 自動滾動到底部
        bufferDisplayArea.setCaretPosition(bufferDisplayArea.getDocument().getLength());
    }

    /**
     * 開始按鈕監聽器
     */
    private class StartButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            maxBufferSize = (Integer) bufferSizeSpinner.getValue();
            buffer.clear();  // 清空緩衝區

            // 重置統計數據
            producedCount = 0;
            consumedCount = 0;

            // 清除並初始化日誌
            processLogArea.setText("");
            appendToLog("═══════════════════════════════════════");
            appendToLog("工廠機台系統啟動 - " + getCurrentTimeString());
            appendToLog("緩衝區大小設定為: " + maxBufferSize);
            appendToLog("═══════════════════════════════════════");

            isRunning.set(true);

            // 啟動生產者和消費者執行緒
            new Thread(new Producer(), "Producer-Thread").start();
            new Thread(new Consumer(), "Consumer-Thread").start();

            // 更新GUI狀態
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            bufferSizeSpinner.setEnabled(false);
            statusLabel.setText("狀態: 運行中");

            updateDisplay();
        }
    }

    /**
     * 停止按鈕監聽器
     */
    private class StopButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            appendTimestampedLog("系統", "收到停止指令，正在關閉系統...");

            isRunning.set(false);

            // 等待一小段時間讓執行緒正常結束
            Timer timer = new Timer(500, (ActionEvent ae) -> {
                appendToLog("═══════════════════════════════════════");
                appendTimestampedLog("系統", "工廠機台系統已停止");
                appendToLog("最終統計 - 生產數量: " + producedCount + ", 處理數量: " + consumedCount);
                appendToLog("═══════════════════════════════════════");
            });
            timer.setRepeats(false);
            timer.start();

            // 更新GUI狀態
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            bufferSizeSpinner.setEnabled(true);
            statusLabel.setText("狀態: 停止");
        }
    }

    /**
     * 主程式入口
     */
    public static void main(String[] args) {
        // 設定系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 在EDT中創建和顯示GUI
        SwingUtilities.invokeLater(() -> {
            new FactoryMachineSystem().setVisible(true);
        });
    }
}