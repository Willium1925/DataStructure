package D0728.grok;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RestaurantSimulation extends JFrame {
    private static final int NUM_TABLES = 6;
    private static final int TABLE_SIZE = 100;
    private static final int PANEL_WIDTH = 1000;
    private static final int PANEL_HEIGHT = 700;
    private static final int STAFF_SIZE = 30;
    private final ConcurrentLinkedQueue<Order> orderQueue = new ConcurrentLinkedQueue<>();
    private final ArrayList<Table> tables = new ArrayList<>();
    private final JTextArea logArea;
    private volatile boolean running = true;
    private final List<Staff> deliveryStaff = new ArrayList<>();
    private final Object deliveryLock = new Object(); // 添加同步锁

    public RestaurantSimulation() {
        setTitle("餐廳模擬系統");
        setSize(PANEL_WIDTH, PANEL_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Restaurant panel for tables, orders, and staff
        JPanel restaurantPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));

                // Draw tables
                for (Table table : tables) {
                    // Draw table (dark if occupied, light if empty)
                    g2d.setColor(table.isOccupied() ? new Color(139, 69, 19) : new Color(210, 180, 140));
                    g2d.fillOval(table.x, table.y, TABLE_SIZE, TABLE_SIZE);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString("桌號 " + table.id, table.x + 20, table.y + TABLE_SIZE / 2);

                    // Draw order next to table
                    if (table.isOccupied() && table.order != null) {
                        int yOffset = table.y;
                        g2d.setColor(Color.BLACK);
                        g2d.drawString("訂單:", table.x + TABLE_SIZE + 20, yOffset);
                        yOffset += 25;
                        for (String item : table.order.items) {
                            g2d.setColor(table.order.deliveredItems.contains(item) ? Color.GREEN : Color.BLACK);
                            g2d.drawString("- " + item, table.x + TABLE_SIZE + 30, yOffset);
                            yOffset += 20;
                        }
                    }
                }

                // Draw staff
                for (Staff staff : deliveryStaff) {
                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(staff.x, staff.y, STAFF_SIZE, STAFF_SIZE);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("送餐員 " + staff.id, staff.x + 5, staff.y + STAFF_SIZE / 2);
                }
            }
        };
        restaurantPanel.setPreferredSize(new Dimension(PANEL_WIDTH - 300, PANEL_HEIGHT));
        add(restaurantPanel, BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(20, 25);
        logArea.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(300, PANEL_HEIGHT));
        add(logScrollPane, BorderLayout.EAST);

        // End button
        JButton endButton = new JButton("結束模擬");
        endButton.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        endButton.addActionListener(e -> {
            running = false;
            logArea.append("模擬已由使用者終止。\n");
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(endButton);
        add(buttonPanel, BorderLayout.NORTH);

        // Initialize tables
        for (int i = 0; i < NUM_TABLES; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = 50 + col * (TABLE_SIZE + 150);
            int y = 50 + row * (TABLE_SIZE + 150);
            tables.add(new Table(i + 1, x, y));
        }

        // Initialize 3 delivery staff at kitchen
        for (int i = 0; i < 3; i++) {
            deliveryStaff.add(new Staff(i + 1, 10, 10 + i * 40));
        }

        // Start simulation threads
        new Thread(this::generateOrders).start();
        new Thread(this::processOrders).start();
        for (int i = 0; i < 3; i++) {
            int staffId = i;
            new Thread(() -> deliverOrders(staffId)).start();
        }
        new Thread(this::repaintLoop).start();
    }

    /**
     * 訂單生成執行緒的主要邏輯
     * 隨機生成新的訂單並分配給空閒的桌子
     */
    private void generateOrders() {
        Random rand = new Random();
        String[] menu = {"漢堡", "披薩", "沙拉", "義大利麵", "牛排"};
        while (running) {
            try {
                // 隨機延遲2-5秒生成新訂單
                Thread.sleep(rand.nextInt(3000) + 2000);
                
                // 隨機選擇一張桌子
                Table table = tables.get(rand.nextInt(NUM_TABLES));
                if (!table.isOccupied()) {
                    // 如果桌子空閒，創建新訂單
                    table.setOccupied(true);
                    Order order = new Order(table.id);
                    
                    // 隨機生成1-3個餐點
                    int numItems = rand.nextInt(3) + 1;
                    for (int j = 0; j < numItems; j++) {
                        order.addItem(menu[rand.nextInt(menu.length)]);
                    }
                    
                    // 將訂單分配給桌子並加入處理佇列
                    table.order = order;
                    orderQueue.offer(order);
                    logArea.append("桌號 " + table.id + " 提交訂單: " + order.items + "\n");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 訂單處理執行緒的主要邏輯
     * 負責處理佇列中的訂單，逐一製作每個餐點
     */
    private void processOrders() {
        Random rand = new Random();
        while (running) {
            try {
                // 查看佇列中的第一個訂單（不移除）
                Order order = orderQueue.peek();
                if (order != null) {
                    // 處理訂單中每個尚未完成的餐點
                    for (String item : order.items) {
                        if (!order.preparedItems.get(item)) {
                            logArea.append("正在處理桌號 " + order.tableId + " 的單點: " + item + "\n");
                            // 模擬餐點製作時間（2-5秒）
                            Thread.sleep(rand.nextInt(3000) + 2000);
                            // 標記餐點為已完成
                            order.preparedItems.put(item, true);
                            logArea.append("桌號 " + order.tableId + " 的單點已準備完成: " + item + "\n");
                        }
                    }

                    // 當訂單中所有餐點都製作完成時，將訂單從佇列中移除
                    if (order.preparedItems.values().stream().allMatch(prepared -> prepared)) {
                        orderQueue.poll();
                    }
                }
                // 每秒檢查一次新訂單
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 送餐員執行緒的主要邏輯
     * 每個送餐員都是一個獨立的執行緒，負責尋找並送達準備好的餐點
     * 
     * @param staffId 送餐員編號
     */
    private void deliverOrders(int staffId) {
        Random rand = new Random();
        Staff staff = deliveryStaff.get(staffId);

        while (running) {
            try {
                // 檢查送餐員是否可用
                if (staff.isAvailable) {
                    Table targetTable = null;    // 目標桌號
                    String itemToDeliver = null; // 待送餐點

                    // 使用同步鎖來安全地查找和認領待送的餐點
                    // 確保同一時間只有一個送餐員可以認領餐點，避免重複送餐
                    synchronized (deliveryLock) {
                        // 遍歷所有桌子尋找需要送餐的訂單
                        for (Table table : tables) {
                            // 檢查桌子是否有未完成的訂單
                            if (table.order != null && !table.order.isDelivered) {
                                Order order = table.order;

                                // 在這個訂單中尋找第一個已準備好但未送達的餐點
                                for (String item : order.items) {
                                    // 檢查餐點是否已準備好且未送達
                                    if (order.preparedItems.get(item) && !order.deliveredItems.contains(item)) {
                                        targetTable = table;
                                        itemToDeliver = item;
                                        // 立即標記為已送達，防止其他送餐員重複送餐
                                        order.deliveredItems.add(item);
                                        break;
                                    }
                                }
                                // 如果找到了要送的餐點就停止搜尋
                                if (targetTable != null) break;
                            }
                        }
                    }

                    // 如果找到了需要送的餐點，開始送餐流程
                    if (targetTable != null && itemToDeliver != null) {
                        staff.isAvailable = false; // 標記送餐員為忙碌狀態
                        Table table = targetTable;

                        // 移動送餐員到目標桌子
                        moveStaffTo(staff, table.x + TABLE_SIZE / 2, table.y + TABLE_SIZE / 2);
                        logArea.append("送餐員 " + staff.id + " 前往桌號 " + table.id + " 送餐\n");

                        // 送餐過程
                        Thread.sleep(500); // 模擬送餐時間
                        logArea.append("送餐員 " + staff.id + " 送達桌號 " + table.id + " 的單點: " + itemToDeliver + "\n");

                        // 再次使用同步鎖檢查訂單狀態
                        synchronized (deliveryLock) {
                            // 檢查該桌的所有餐點是否都已送達
                            if (table.order.deliveredItems.size() == table.order.items.size()) {
                                table.order.isDelivered = true;
                                // 啟動新執行緒在一段時間後清空桌子
                                new Thread(() -> {
                                    try {
                                        // 隨機等待 5-10 秒後清空桌子
                                        Thread.sleep(rand.nextInt(5000) + 5000);
                                        table.clearTable();
                                        logArea.append("桌號 " + table.id + " 已清空。\n");
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }).start();
                            }
                        }

                        // 送餐員返回廚房
                        moveStaffTo(staff, 10, 10 + staffId * 40);
                        logArea.append("送餐員 " + staff.id + " 返回廚房\n");
                        staff.isAvailable = true; // 標記送餐員為可用狀態
                    }
                }
                // 短暫休息後繼續檢查新的可送餐點
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 送餐員移動動畫的實現方法
     * 將送餐員從當前位置平滑移動到目標位置
     * 
     * @param staff 要移動的送餐員
     * @param targetX 目標X座標
     * @param targetY 目標Y座標
     */
    private void moveStaffTo(Staff staff, int targetX, int targetY) {
        int steps = 20; // 移動的步數，越多移動越平滑
        int startX = staff.x;
        int startY = staff.y;
        // 計算每步移動的距離
        int dx = (targetX - startX) / steps;
        int dy = (targetY - startY) / steps;
        
        // 分步移動送餐員
        for (int i = 0; i <= steps; i++) {
            staff.x = startX + dx * i;
            staff.y = startY + dy * i;
            try {
                Thread.sleep(50); // 每步暫停50毫秒實現平滑移動效果
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // 確保最後位置準確
        staff.x = targetX;
        staff.y = targetY;
    }

    private void repaintLoop() {
        while (running) {
            try {
                SwingUtilities.invokeLater(() -> getContentPane().repaint());
                Thread.sleep(100); // Repaint every 100ms for smooth animation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 代表餐廳中的一張桌子
     * 包含桌號、位置信息以及當前的訂單狀態
     */
    private class Table {
        int id;          // 桌號
        int x, y;        // 桌子在畫面上的位置
        private boolean occupied = false;  // 桌子是否有客人
        private Order order;              // 當前桌子的訂單

        Table(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        boolean isOccupied() {
            return occupied;
        }

        void setOccupied(boolean occupied) {
            this.occupied = occupied;
        }

        void clearTable() {
            order = null;
            occupied = false;
        }
    }

    /**
     * 代表一份訂單
     * 包含訂單的所有餐點及其狀態信息
     */
    private class Order {
        int tableId;                                    // 訂單所屬的桌號
        ArrayList<String> items = new ArrayList<>();     // 訂單中的所有餐點
        ArrayList<String> deliveredItems = new ArrayList<>();  // 已送達的餐點
        Map<String, Boolean> preparedItems = new HashMap<>();  // 餐點準備狀態
        boolean isDelivered = false;                    // 整份訂單是否已送完

        Order(int tableId) {
            this.tableId = tableId;
        }

        /**
         * 添加一個餐點到訂單中
         * @param item 要添加的餐點名稱
         */
        void addItem(String item) {
            items.add(item);
            preparedItems.put(item, false);  // 初始狀態為未準備
        }

        /**
         * 檢查訂單中是否有已準備好但未送達的餐點
         * @return 如果有未送達的已準備餐點則返回true
         */
        boolean hasUndeliveredPreparedItems() {
            return items.stream()
                .anyMatch(item -> preparedItems.get(item) && !deliveredItems.contains(item));
        }
    }

    /**
     * 代表一位送餐員
     * 包含送餐員的狀態和位置信息
     */
    private class Staff {
        int id;                     // 送餐員編號
        int x, y;                   // 送餐員當前位置
        boolean isAvailable = true; // 送餐員是否可接受新的送餐任務

        Staff(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 主程式入口
     * 使用 SwingUtilities.invokeLater 確保在 EDT 中創建和顯示 GUI
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RestaurantSimulation sim = new RestaurantSimulation();
            sim.setVisible(true);
        });
    }
}
