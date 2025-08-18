package D0812;// GraphGUI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

public class GraphGUI extends JFrame {

    private GraphPanel graphPanel;
    private JTextArea matrixTextArea;
    private JTextArea resultTextArea;
    private JTextField nodesField;
    private JTextField edgesField;
    private JComboBox<String> startNodeBox;
    private JComboBox<String> endNodeBox;

    private Graph graph;
    // **新增**: 用於儲存 Floyd-Warshall 演算法的結果
    private FloydWarshall floydWarshallResult;

    public GraphGUI() {
        setTitle("圖資料結構與最短路徑視覺化 (Dijkstra vs Floyd-Warshall)");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        graph = new Graph();

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.7);

        graphPanel = new GraphPanel();
        mainSplitPane.setLeftComponent(new JScrollPane(graphPanel));

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nodesField = new JTextField("12", 3);
        edgesField = new JTextField("20", 3);
        JButton generateButton = new JButton("產生圖形 & 計算路徑");

        controlPanel.add(new JLabel("節點數:"));
        controlPanel.add(nodesField);
        controlPanel.add(new JLabel("邊數:"));
        controlPanel.add(edgesField);
        controlPanel.add(generateButton);

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startNodeBox = new JComboBox<>();
        endNodeBox = new JComboBox<>();
        JButton findPathButton = new JButton("比較單點到全部");
        JButton findTwoPointPathButton = new JButton("比較兩點間距離");

        pathPanel.add(new JLabel("起點:"));
        pathPanel.add(startNodeBox);
        pathPanel.add(findPathButton);
        pathPanel.add(new JLabel("終點:"));
        pathPanel.add(endNodeBox);
        pathPanel.add(findTwoPointPathButton);

        matrixTextArea = new JTextArea("鄰接矩陣將顯示於此...");
        resultTextArea = new JTextArea("演算法比較結果將顯示於此...");
        matrixTextArea.setEditable(false);
        resultTextArea.setEditable(false);
        matrixTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // 加大字體方便閱讀

        JSplitPane textSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(matrixTextArea), new JScrollPane(resultTextArea));
        textSplitPane.setResizeWeight(0.4);

        JPanel topControlPanel = new JPanel();
        topControlPanel.setLayout(new BoxLayout(topControlPanel, BoxLayout.Y_AXIS));
        topControlPanel.add(controlPanel);
        topControlPanel.add(pathPanel);

        rightPanel.add(topControlPanel, BorderLayout.NORTH);
        rightPanel.add(textSplitPane, BorderLayout.CENTER);

        mainSplitPane.setRightComponent(rightPanel);
        add(mainSplitPane);

        generateButton.addActionListener(this::generateGraphAndAlgorithms);
        findPathButton.addActionListener(this::compareAllPathsFromSource);
        findTwoPointPathButton.addActionListener(this::comparePathBetweenTwoNodes);

//        generateGraphAndAlgorithms(null);
    }

    /**
     * **修改**: 產生新圖形後，立即執行 Floyd-Warshall 演算法。
     * Dijkstra 則是在使用者查詢時才即時執行。
     */
    private void generateGraphAndAlgorithms(ActionEvent e) {
        try {
            int numNodes = Integer.parseInt(nodesField.getText());
            int numEdges = Integer.parseInt(edgesField.getText());

            if (numNodes <= 0 || numEdges < 0) {
                JOptionPane.showMessageDialog(this, "節點和邊數必須為正整數。", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }
            long maxEdges = (long) numNodes * (numNodes - 1) / 2;
            if (numEdges > maxEdges) {
                numEdges = (int) maxEdges;
                edgesField.setText(String.valueOf(maxEdges));
            }

            graph = new Graph();
            graph.generateRandomGraph(numNodes, numEdges, graphPanel.getWidth(), graphPanel.getHeight());

            // **新增**: 在產生圖形後，立即計算所有配對的最短路徑
            floydWarshallResult = new FloydWarshall(graph);

            graphPanel.setGraph(graph);
            matrixTextArea.setText(graph.getAdjacencyMatrixString());
            resultTextArea.setText("新圖形已產生，所有點對路徑已由 Floyd-Warshall 預先計算完畢。\n請選擇起點和終點進行比較。");
            updateComboBoxes();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的整數。", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * **新增**: 比較 Dijkstra 和 Floyd-Warshall 從單一起點到所有其他點的結果。
     */
    private void compareAllPathsFromSource(ActionEvent e) {
        if (startNodeBox.getSelectedItem() == null || graph.getNodes().isEmpty()) return;

        int startId = startNodeBox.getSelectedIndex();
        Node startNode = graph.getNodeById(startId);
        if (startNode == null) return;

        // 1. 執行 Dijkstra
        Dijkstra.Result dijkstraResult = Dijkstra.findShortestPaths(graph, startNode);

        graphPanel.setShortestPath(null); // 清除高亮
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.##");

        sb.append(String.format("從 V%d 出發到各點的最短路徑比較:\n", startId));
        sb.append("=========================================================\n");

        List<Node> nodes = graph.getNodes();
        nodes.sort(Comparator.comparingInt(Node::getId));

        for (Node targetNode : nodes) {
            int targetId = targetNode.getId();
            sb.append(String.format("--- 到 V%d ---\n", targetId));

            // Dijkstra 結果
            double dijkstraDist = dijkstraResult.distances.get(targetNode);
            sb.append(String.format("Dijkstra    : 距離 %-8s | 路徑: ",
                    dijkstraDist == Double.POSITIVE_INFINITY ? "∞" : df.format(dijkstraDist)));
            List<Node> dijkstraPath = Dijkstra.getPath(dijkstraResult, targetNode);
            sb.append(formatPath(dijkstraPath));
            sb.append("\n");

            // Floyd-Warshall 結果
            double fwDist = floydWarshallResult.getShortestDistance(startId, targetId);
            sb.append(String.format("Floyd-Warsh.: 距離 %-8s | 路徑: ",
                    fwDist == Double.POSITIVE_INFINITY ? "∞" : df.format(fwDist)));
            List<Node> fwPath = floydWarshallResult.getPath(startId, targetId, graph);
            sb.append(formatPath(fwPath));
            sb.append("\n");
        }
        resultTextArea.setText(sb.toString());
    }

    /**
     * **修改**: 比較 Dijkstra 和 Floyd-Warshall 尋找兩點間最短路徑的結果。
     */
    private void comparePathBetweenTwoNodes(ActionEvent e) {
        if (startNodeBox.getSelectedItem() == null || endNodeBox.getSelectedItem() == null || graph.getNodes().isEmpty()) return;

        int startId = startNodeBox.getSelectedIndex();
        int endId = endNodeBox.getSelectedIndex();

        Node startNode = graph.getNodeById(startId);
        Node endNode = graph.getNodeById(endId);

        if (startNode == null || endNode == null) return;

        // --- 執行 Dijkstra ---
        Dijkstra.Result dijkstraResult = Dijkstra.findShortestPaths(graph, startNode);
        double dijkstraDist = dijkstraResult.distances.get(endNode);
        List<Node> dijkstraPath = Dijkstra.getPath(dijkstraResult, endNode);

        // --- 提取預先計算好的 Floyd-Warshall 結果 ---
        double fwDist = floydWarshallResult.getShortestDistance(startId, endId);
        List<Node> fwPath = floydWarshallResult.getPath(startId, endId, graph);

        // --- 格式化輸出 ---
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.##");

        sb.append(String.format("比較 V%d 到 V%d 的最短路徑:\n", startId, endId));
        sb.append("===================================\n");

        // Dijkstra 輸出
        sb.append("[Dijkstra 演算法]\n");
        if (dijkstraPath == null || dijkstraDist == Double.POSITIVE_INFINITY) {
            sb.append("路徑不存在。\n");
        } else {
            sb.append("距離: ").append(df.format(dijkstraDist)).append("\n");
            sb.append("路徑: ").append(formatPath(dijkstraPath)).append("\n");
        }

        sb.append("\n");

        // Floyd-Warshall 輸出
        sb.append("[Floyd-Warshall 演算法]\n");
        if (fwPath == null || fwDist == Double.POSITIVE_INFINITY) {
            sb.append("路徑不存在。\n");
        } else {
            sb.append("距離: ").append(df.format(fwDist)).append("\n");
            sb.append("路徑: ").append(formatPath(fwPath)).append("\n");
        }

        resultTextArea.setText(sb.toString());

        // 在 GUI 上高亮顯示路徑 (這裡我們選擇顯示 Dijkstra 的結果)
        graphPanel.setShortestPath(dijkstraPath);
    }

    private void updateComboBoxes() {
        startNodeBox.removeAllItems();
        endNodeBox.removeAllItems();
        if (graph != null) {
            for (Node node : graph.getNodes()) {
                String item = "V" + node.getId();
                startNodeBox.addItem(item);
                endNodeBox.addItem(item);
            }
        }
    }

    /**
     * 輔助方法，將 Node 列表格式化為 V0 -> V1 -> V2 的字串
     */
    private String formatPath(List<Node> path) {
        if (path == null || path.isEmpty()) {
            return "N/A";
        }
        StringBuilder pathStr = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            pathStr.append("V").append(path.get(i).getId());
            if (i < path.size() - 1) pathStr.append(" -> ");
        }
        return pathStr.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GraphGUI gui = new GraphGUI();
            gui.setVisible(true);
            // 在視窗可見後再呼叫，此時 getWidth() 和 getHeight() 才有有效值
            gui.generateGraphAndAlgorithms(null); // <--- 新增這一行
        });
    }
}