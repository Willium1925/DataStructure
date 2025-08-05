package D0805;

import javax.swing.*; // 用於 GUI 組件
import javax.swing.Timer;
import java.awt.*; // 用於繪圖和座標處理
import java.awt.event.*; // 用於事件處理
import java.util.*; // 用於集合類（如 ArrayList、HashSet、LinkedList）

// 主類，負責創建 GUI 窗口和控制面板
public class GraphSimulator extends JFrame {
    private GraphPanel graphPanel; // 繪圖面板，用於顯示圖和搜尋動畫
    private JTextField nodeField, edgeField; // 輸入節點數和邊數的文本框
    private JCheckBox directedCheckBox; // 選擇是否為有向圖的複選框
    private JButton generateButton, dfsButton, bfsButton; // 生成圖、執行 DFS 和 BFS 的按鈕
    private JLabel statusLabel; // 顯示狀態訊息的標籤

    // 建構函數，初始化 GUI
    public GraphSimulator() {
        setTitle("Graph Simulator"); // 設置窗口標題
        setSize(800, 600); // 設置窗口大小
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 點擊關閉按鈕時退出程式
        setLayout(new BorderLayout()); // 使用邊界佈局

        // 創建控制面板
        JPanel controlPanel = new JPanel(); // 控制面板容器
        nodeField = new JTextField("10", 5); // 節點數輸入框，預設值 10
        edgeField = new JTextField("15", 5); // 邊數輸入框，預設值 15
        directedCheckBox = new JCheckBox("Directed Graph"); // 是否為有向圖的複選框
        generateButton = new JButton("Generate Graph"); // 生成圖的按鈕
        dfsButton = new JButton("Run DFS"); // 執行 DFS 的按鈕
        bfsButton = new JButton("Run BFS"); // 執行 BFS 的按鈕
        statusLabel = new JLabel("Ready"); // 狀態標籤，初始顯示 "Ready"

        // 將組件添加到控制面板
        controlPanel.add(new JLabel("Nodes:")); // 添加節點數標籤
        controlPanel.add(nodeField); // 添加節點數輸入框
        controlPanel.add(new JLabel("Edges:")); // 添加邊數標籤
        controlPanel.add(edgeField); // 添加邊數輸入框
        controlPanel.add(directedCheckBox); // 添加有向圖複選框
        controlPanel.add(generateButton); // 添加生成按鈕
        controlPanel.add(dfsButton); // 添加 DFS 按鈕
        controlPanel.add(bfsButton); // 添加 BFS 按鈕
        controlPanel.add(statusLabel); // 添加狀態標籤

        // 初始化繪圖面板
        graphPanel = new GraphPanel(); // 創建用於繪圖的面板
        add(controlPanel, BorderLayout.NORTH); // 將控制面板放在窗口頂部
        add(graphPanel, BorderLayout.CENTER); // 將繪圖面板放在窗口中央

        // 為生成按鈕添加事件監聽器
        generateButton.addActionListener(e -> generateGraph());
        // 為 DFS 按鈕添加事件監聽器
        dfsButton.addActionListener(e -> graphPanel.startDFS());
        // 為 BFS 按鈕添加事件監聽器
        bfsButton.addActionListener(e -> graphPanel.startBFS());
    }

    // 生成圖的方法，根據輸入參數創建圖
    private void generateGraph() {
        try {
            int nodes = Integer.parseInt(nodeField.getText()); // 獲取節點數
            int edges = Integer.parseInt(edgeField.getText()); // 獲取邊數
            boolean isDirected = directedCheckBox.isSelected(); // 獲取是否有向圖選項

            // 驗證輸入
            if (nodes < 1 || edges < 0) { // 檢查節點數是否至少為 1，邊數是否非負
                statusLabel.setText("Invalid input: nodes must be >= 1, edges >= 0");
                return;
            }
            // 檢查邊數是否超過最大可能邊數（有向圖：n*(n-1)，無向圖：n*(n-1)/2）
            int maxEdges = isDirected ? nodes * (nodes - 1) : nodes * (nodes - 1) / 2;
            if (edges > maxEdges) {
                statusLabel.setText("Too many edges for given nodes");
                return;
            }
            // 確保至少有 n-1 條邊以保證連通圖（對於無向圖或弱連通有向圖）
            if (edges < nodes - 1) {
                statusLabel.setText("Too few edges: need at least " + (nodes - 1) + " for connectivity");
                return;
            }

            // 生成圖
            graphPanel.generateGraph(nodes, edges, isDirected);
            statusLabel.setText("Graph generated successfully");
        } catch (NumberFormatException ex) {
            statusLabel.setText("Invalid input: please enter numbers");
        }
    }

    // 主方法，啟動程式
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GraphSimulator().setVisible(true); // 在事件分派線程中創建並顯示窗口
        });
    }
}

// 繪圖面板類，負責圖的可視化和搜尋動畫
class GraphPanel extends JPanel {
    private ArrayList<Point> nodes = new ArrayList<>(); // 儲存節點座標
    private ArrayList<int[]> edges = new ArrayList<>(); // 儲存邊（每條邊為 [from, to]）
    private boolean isDirected; // 是否為有向圖
    private ArrayList<Integer> visited = new ArrayList<>(); // 已訪問節點列表
    private LinkedList<Integer> queue = new LinkedList<>(); // 用於 BFS 的佇列
    private Stack<Integer> stack = new Stack<>(); // 用於 DFS 的堆疊
    private Timer animationTimer; // 控制動畫的計時器
    private boolean isDFS = false; // 當前是否執行 DFS 動畫

    // 建構函數，初始化繪圖面板
    public GraphPanel() {
        setBackground(Color.WHITE); // 設置背景為白色
        // 初始化動畫計時器，每 500ms 更新一次
        animationTimer = new Timer(500, e -> {
            if (isDFS) {
                processDFSStep(); // 執行 DFS 動畫步驟
            } else {
                processBFSStep(); // 執行 BFS 動畫步驟
            }
            repaint(); // 重繪面板
        });
    }

    // 生成圖的方法
    public void generateGraph(int nodeCount, int edgeCount, boolean directed) {
        nodes.clear(); // 清空節點列表
        edges.clear(); // 清空邊列表
        visited.clear(); // 清空已訪問節點
        queue.clear(); // 清空 BFS 佇列
        stack.clear(); // 清空 DFS 堆疊
        isDirected = directed; // 設置是否有向圖
        animationTimer.stop(); // 停止當前動畫

        // 生成隨機節點座標
        Random rand = new Random();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(new Point(
                    50 + rand.nextInt(getWidth() - 100), // 隨機 x 座標，留邊距
                    50 + rand.nextInt(getHeight() - 100) // 隨機 y 座標，留邊距
            ));
        }

        // 生成連通圖：先創建一棵生成樹，確保圖連通
        Set<Integer> connected = new HashSet<>(); // 已連通的節點集合
        connected.add(0); // 從節點 0 開始
        Set<String> edgeSet = new HashSet<>(); // 用於檢查邊是否重複
        while (connected.size() < nodeCount) {
            int from = new ArrayList<>(connected).get(rand.nextInt(connected.size())); // 從已連通節點中隨機選一個
            int to = rand.nextInt(nodeCount); // 隨機選一個目標節點
            if (!connected.contains(to)) { // 目標節點尚未連通
                String edgeKey = directed ? from + "-" + to : Math.min(from, to) + "-" + Math.max(from, to);
                if (!edgeSet.contains(edgeKey)) {
                    edges.add(new int[]{from, to}); // 添加邊
                    edgeSet.add(edgeKey);
                    connected.add(to); // 將目標節點加入連通集合
                }
            }
        }

        // 添加剩餘的隨機邊
        while (edges.size() < edgeCount) {
            int from = rand.nextInt(nodeCount);
            int to = rand.nextInt(nodeCount);
            if (from != to) { // 避免自環
                String edgeKey = directed ? from + "-" + to : Math.min(from, to) + "-" + Math.max(from, to);
                if (!edgeSet.contains(edgeKey)) {
                    edges.add(new int[]{from, to});
                    edgeSet.add(edgeKey);
                }
            }
        }
        repaint(); // 重繪面板
    }

    // 啟動 DFS 動畫
    public void startDFS() {
        visited.clear(); // 清空已訪問節點
        queue.clear(); // 清空佇列
        stack.clear(); // 清空堆疊
        animationTimer.stop(); // 停止當前動畫
        isDFS = true; // 設置為 DFS 模式
        if (!nodes.isEmpty()) {
            stack.push(0); // 從節點 0 開始
            visited.add(0); // 標記起始節點為已訪問
            animationTimer.start(); // 開始動畫
        }
    }

    // 啟動 BFS 動畫
    public void startBFS() {
        visited.clear(); // 清空已訪問節點
        queue.clear(); // 清空佇列
        stack.clear(); // 清空堆疊
        animationTimer.stop(); // 停止當前動畫
        isDFS = false; // 設置為 BFS 模式
        if (!nodes.isEmpty()) {
            queue.offer(0); // 將起始節點加入佇列
            visited.add(0); // 標記起始節點為已訪問
            animationTimer.start(); // 開始動畫
        }
    }

    // 繪製面板內容
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 調用父類的繪製方法
        Graphics2D g2d = (Graphics2D) g; // 轉換為 Graphics2D 以支持更高級繪圖
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 啟用抗鋸齒

        // 繪製邊
        g2d.setColor(Color.BLACK); // 設置邊的顏色
        for (int[] edge : edges) {
            Point from = nodes.get(edge[0]); // 起點座標
            Point to = nodes.get(edge[1]); // 終點座標
            if (isDirected) {
                drawArrow(g2d, from.x, from.y, to.x, to.y); // 繪製有向邊（帶箭頭）
            } else {
                g2d.drawLine(from.x, from.y, to.x, to.y); // 繪製無向邊
            }
        }

        // 繪製節點
        for (int i = 0; i < nodes.size(); i++) {
            Point p = nodes.get(i); // 獲取節點座標
            if (visited.contains(i)) {
                g2d.setColor(Color.RED); // 已訪問節點顯示為紅色
            } else {
                g2d.setColor(Color.BLUE); // 未訪問節點顯示為藍色
            }
            g2d.fillOval(p.x - 15, p.y - 15, 30, 30); // 繪製節點（圓形）
            g2d.setColor(Color.WHITE); // 節點編號的顏色
            g2d.drawString(String.valueOf(i), p.x - 5, p.y + 5); // 繪製節點編號
        }

        // 檢查動畫是否應停止
        if (animationTimer.isRunning() && visited.size() >= nodes.size()) {
            animationTimer.stop(); // 所有節點都訪問過，停止動畫
        }
    }

    // 處理 DFS 動畫的一個步驟
    private void processDFSStep() {
        if (!stack.isEmpty()) {
            int current = stack.peek(); // 獲取堆疊頂部節點
            boolean found = false; // 是否找到未訪問的鄰居
            for (int[] edge : edges) {
                int next = -1;
                if (edge[0] == current && !visited.contains(edge[1])) {
                    next = edge[1]; // 找到有向邊的鄰居
                } else if (!isDirected && edge[1] == current && !visited.contains(edge[0])) {
                    next = edge[0]; // 無向圖中檢查反向邊
                }
                if (next != -1) {
                    stack.push(next); // 將新節點壓入堆疊
                    visited.add(next); // 標記為已訪問
                    found = true; // 找到鄰居
                    break; // 每次只處理一個鄰居
                }
            }
            if (!found) {
                stack.pop(); // 無未訪問鄰居，彈出當前節點
            }
        }
    }

    // 處理 BFS 動畫的一個步驟
    private void processBFSStep() {
        if (!queue.isEmpty()) {
            int current = queue.poll(); // 從佇列中取出當前節點
            for (int[] edge : edges) {
                int next = -1;
                if (edge[0] == current && !visited.contains(edge[1])) {
                    next = edge[1]; // 找到有向邊的鄰居
                } else if (!isDirected && edge[1] == current && !visited.contains(edge[0])) {
                    next = edge[0]; // 無向圖中檢查反向邊
                }
                if (next != -1) {
                    queue.offer(next); // 將新節點加入佇列
                    visited.add(next); // 標記為已訪問
                }
            }
        }
    }

    // 繪製有向邊的箭頭
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1, y1, x2, y2); // 繪製邊的主線
        double angle = Math.atan2(y2 - y1, x2 - x1); // 計算箭頭角度
        int arrowSize = 10; // 箭頭大小
        // 繪製箭頭的第一條線
        g2d.drawLine(x2, y2,
                (int)(x2 - arrowSize * Math.cos(angle - Math.PI/6)),
                (int)(y2 - arrowSize * Math.sin(angle - Math.PI/6)));
        // 繪製箭頭的第二條線
        g2d.drawLine(x2, y2,
                (int)(x2 - arrowSize * Math.cos(angle + Math.PI/6)),
                (int)(y2 - arrowSize * Math.sin(angle + Math.PI/6)));
    }
}