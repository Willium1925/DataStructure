package D0728.gpt;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

public class RestaurantSimulatorWithAnimation extends JFrame {
    private static final int TABLE_COUNT = 4;

    private List<TablePanel> tables = new ArrayList<>();
    private JTextArea logArea = new JTextArea();
    private JButton stopButton = new JButton("結束");
    private volatile boolean running = true;

    private ExecutorService executor = Executors.newCachedThreadPool();
    private BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();

    private JPanel animationLayer = new JPanel(null);

    public RestaurantSimulatorWithAnimation() {
        setTitle("餐廳模擬器（含動畫）");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel tableArea = new JPanel(new GridLayout(2, 2, 20, 20));
        for (int i = 0; i < TABLE_COUNT; i++) {
            TablePanel table = new TablePanel(i);
            tables.add(table);
            tableArea.add(table);
        }

        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(300, 0));

        JPanel controlPanel = new JPanel();
        controlPanel.add(stopButton);

        animationLayer.setOpaque(false);
        animationLayer.setPreferredSize(new Dimension(1200, 700));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(tableArea, BorderLayout.CENTER);
        leftPanel.add(animationLayer, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        executor.submit(this::simulateCustomerOrders);
        executor.submit(this::simulateKitchen);

        stopButton.addActionListener(e -> {
            running = false;
            executor.shutdownNow();
            log("系統已結束。\n");
        });
    }

    private void simulateCustomerOrders() {
        Random rand = new Random();
        while (running) {
            try {
                int tableIndex = rand.nextInt(TABLE_COUNT);
                TablePanel table = tables.get(tableIndex);
                Order order = Order.generateRandom(tableIndex);
                table.addOrder(order);
                orderQueue.put(order);
                log("桌號 " + tableIndex + " 下訂單：" + order);
                Thread.sleep(3000 + rand.nextInt(3000));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void simulateKitchen() {
        while (running) {
            try {
                Order order = orderQueue.take();
                for (String item : order.getItems()) {
                    Thread.sleep(2000); // 模擬製作時間
                    SwingUtilities.invokeLater(() -> {
                        animateDelivery(order.getTableNumber(), item);
                        tables.get(order.getTableNumber()).markItemAsDelivered(order, item);
                        log("送餐至桌號 " + order.getTableNumber() + ": " + item);
                    });
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void animateDelivery(int tableNumber, String item) {
        JLabel foodLabel = new JLabel(item);
        foodLabel.setOpaque(true);
        foodLabel.setBackground(Color.YELLOW);
        foodLabel.setBounds(900, 600, 80, 30);
        getLayeredPane().add(foodLabel, JLayeredPane.POPUP_LAYER);

        new Timer(20, new ActionListener() {
            int x = 900;
            int y = 600;
            int targetX = 100 + (tableNumber % 2) * 300;
            int targetY = 100 + (tableNumber / 2) * 300;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (Math.abs(x - targetX) < 5 && Math.abs(y - targetY) < 5) {
                    ((Timer) e.getSource()).stop();
                    getLayeredPane().remove(foodLabel);
                    repaint();
                    return;
                }
                x += (targetX - x) / 10;
                y += (targetY - y) / 10;
                foodLabel.setLocation(x, y);
            }
        }).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RestaurantSimulatorWithAnimation sim = new RestaurantSimulatorWithAnimation();
            sim.setVisible(true);
        });
    }
}

class TablePanel extends JPanel {
    private int tableNumber;
    private List<Order> orders = new ArrayList<>();
    private DefaultListModel<String> orderModel = new DefaultListModel<>();
    private Map<String, Boolean> itemStatus = new LinkedHashMap<>();
    private JLabel label = new JLabel("空桌", SwingConstants.CENTER);
    private JList<String> orderList = new JList<>(orderModel);

    public TablePanel(int tableNumber) {
        this.tableNumber = tableNumber;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("桌號 " + tableNumber));
        add(label, BorderLayout.NORTH);
        add(new JScrollPane(orderList), BorderLayout.CENTER);
        updateColor();
    }

    public boolean isAvailable() {
        return orders.isEmpty();
    }

    public void addOrder(Order order) {
        orders.add(order);
        label.setText("用餐中");
        for (String item : order.getItems()) {
            orderModel.addElement(item);
            itemStatus.put(item, false);
        }
        updateColor();
    }

    public void markItemAsDelivered(Order order, String item) {
        itemStatus.put(item, true);
        updateListColors();
        boolean allDelivered = orders.stream().flatMap(o -> o.getItems().stream())
                .allMatch(i -> itemStatus.getOrDefault(i, false));
        if (allDelivered) {
            label.setText("空桌");
            orders.clear();
            itemStatus.clear();
            orderModel.clear();
        }
        updateColor();
    }

    private void updateColor() {
        setBackground(isAvailable() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
    }

    private void updateListColors() {
        orderList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value, int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String item = (String) value;
                if (itemStatus.getOrDefault(item, false)) {
                    label.setForeground(Color.GREEN);
                } else {
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        });
        orderList.repaint();
    }
}

class Order {
    private static int nextId = 1;
    private int id;
    private int tableNumber;
    private List<String> items;

    public Order(int tableNumber, List<String> items) {
        this.id = nextId++;
        this.tableNumber = tableNumber;
        this.items = items;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public List<String> getItems() {
        return items;
    }

    public static Order generateRandom(int tableNumber) {
        List<String> allItems = Arrays.asList("炒飯", "牛肉麵", "湯包", "水餃", "炒青菜");
        Random rand = new Random();
        int count = 1 + rand.nextInt(3);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(allItems.get(rand.nextInt(allItems.size())));
        }
        return new Order(tableNumber, items);
    }

    @Override
    public String toString() {
        return "訂單#" + id + ": " + String.join(", ", items);
    }
}
