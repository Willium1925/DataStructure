package D0811;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ArticulationPointComparison extends JFrame {
    private static final int MIN_VERTICES = 1;
    private static final int MAX_VERTICES = 20;
    private static final int VERTEX_STEP = 1;
    private static final int EDGE_STEP_FACTOR = 1;
    private static final int RUNS_PER_TEST = 5; // 平均多次運行以減少隨機誤差

    public ArticulationPointComparison() {
        setTitle("Articulation Point Algorithm Comparison");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);

        StringBuilder crossoverInfo = new StringBuilder("<html><h2>Crossover Points</h2><ul>");

        // 對不同頂點數進行測試
        for (int vertices = MIN_VERTICES; vertices <= MAX_VERTICES; vertices += VERTEX_STEP) {
            XYSeries simpleSeries = new XYSeries("Simple Method");
            XYSeries tarjanSeries = new XYSeries("Tarjan Method");
            List<Double> crossoverEdges = new ArrayList<>();

            // 邊數從 V 到 V*(V-1)/2 的 10% 逐步增加
            int maxEdges = (vertices * (vertices - 1)) / 2;
            for (int edges = vertices; edges <= maxEdges; edges += vertices * EDGE_STEP_FACTOR) {
                long simpleTime = 0;
                long tarjanTime = 0;

                // 多次運行取平均值
                for (int run = 0; run < RUNS_PER_TEST; run++) {
                    List<List<Integer>> graph = generateRandomConnectedGraph(vertices, edges);
                    simpleTime += measureSimpleMethod(graph);
                    tarjanTime += measureTarjanMethod(graph);
                }

                double simpleAvgTime = simpleTime / (double) RUNS_PER_TEST / 1_000_000.0; // 轉為毫秒
                double tarjanAvgTime = tarjanTime / (double) RUNS_PER_TEST / 1_000_000.0;

                simpleSeries.add(edges, simpleAvgTime);
                tarjanSeries.add(edges, tarjanAvgTime);

                // 檢查交叉點
                if (simpleAvgTime > tarjanAvgTime && simpleSeries.getItemCount() > 1 && tarjanSeries.getItemCount() > 1) {
                    double prevSimpleTime = simpleSeries.getY(simpleSeries.getItemCount() - 2).doubleValue();
                    double prevTarjanTime = tarjanSeries.getY(tarjanSeries.getItemCount() - 2).doubleValue();
                    if (prevSimpleTime < prevTarjanTime) {
                        crossoverEdges.add((double) edges);
                    }
                }
            }

            // 添加圖表
            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(simpleSeries);
            dataset.addSeries(tarjanSeries);

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Execution Time (Vertices = " + vertices + ")",
                    "Number of Edges",
                    "Time (ms)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 400));
            mainPanel.add(chartPanel);

            // 記錄交叉點
            if (!crossoverEdges.isEmpty()) {
                crossoverInfo.append("<li>Vertices = ").append(vertices)
                        .append(": Crossover at edges ≈ ").append(crossoverEdges).append("</li>");
            }
        }

        crossoverInfo.append("</ul></html>");
        JLabel crossoverLabel = new JLabel(crossoverInfo.toString());
        mainPanel.add(crossoverLabel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // 生成隨機連通圖
    private List<List<Integer>> generateRandomConnectedGraph(int vertices, int edges) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            graph.add(new ArrayList<>());
        }

        // 生成最小生成樹確保連通
        Random rand = new Random();
        List<Integer> verticesList = new ArrayList<>();
        for (int i = 0; i < vertices; i++) verticesList.add(i);
        Collections.shuffle(verticesList);

        for (int i = 1; i < vertices; i++) {
            int u = verticesList.get(i);
            int v = verticesList.get(rand.nextInt(i));
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // 添加額外的邊
        int currentEdges = vertices - 1;
        while (currentEdges < edges) {
            int u = rand.nextInt(vertices);
            int v = rand.nextInt(vertices);
            if (u != v && !graph.get(u).contains(v)) {
                graph.get(u).add(v);
                graph.get(v).add(u);
                currentEdges++;
            }
        }

        return graph;
    }

    // 普通方法：移除每個頂點並檢查連通性
    private long measureSimpleMethod(List<List<Integer>> graph) {
        long startTime = System.nanoTime();
        int vertices = graph.size();
        for (int v = 0; v < vertices; v++) {
            // 複製圖並移除頂點 v
            List<List<Integer>> tempGraph = new ArrayList<>();
            for (int i = 0; i < vertices; i++) {
                if (i != v) {
                    List<Integer> neighbors = new ArrayList<>(graph.get(i));
                    int finalV = v;
                    neighbors.removeIf(n -> n == finalV);
                    tempGraph.add(neighbors);
                } else {
                    tempGraph.add(new ArrayList<>());
                }
            }

            // 檢查連通性
            boolean[] visited = new boolean[vertices];
            dfs(tempGraph, 0, v, visited);
            boolean isConnected = true;
            for (int i = 0; i < vertices; i++) {
                if (i != v && !visited[i]) {
                    isConnected = false;
                    break;
                }
            }
        }
        return System.nanoTime() - startTime;
    }

    private void dfs(List<List<Integer>> graph, int u, int skipVertex, boolean[] visited) {
        if (u == skipVertex) return;
        visited[u] = true;
        for (int v : graph.get(u)) {
            if (!visited[v] && v != skipVertex) {
                dfs(graph, v, skipVertex, visited);
            }
        }
    }

    // 快速方法：Tarjan 演算法
    private long measureTarjanMethod(List<List<Integer>> graph) {
        long startTime = System.nanoTime();
        int vertices = graph.size();
        int[] disc = new int[vertices];
        int[] low = new int[vertices];
        int[] parent = new int[vertices];
        boolean[] ap = new boolean[vertices];
        Arrays.fill(disc, -1);
        Arrays.fill(parent, -1);
        int[] time = {0};

        for (int u = 0; u < vertices; u++) {
            if (disc[u] == -1) {
                tarjanDFS(u, graph, disc, low, parent, ap, time);
            }
        }
        return System.nanoTime() - startTime;
    }

    private void tarjanDFS(int u, List<List<Integer>> graph, int[] disc, int[] low, int[] parent, boolean[] ap, int[] time) {
        int children = 0;
        disc[u] = low[u] = ++time[0];
        for (int v : graph.get(u)) {
            if (disc[v] == -1) {
                children++;
                parent[v] = u;
                tarjanDFS(v, graph, disc, low, parent, ap, time);
                low[u] = Math.min(low[u], low[v]);

                if (parent[u] == -1 && children > 1) {
                    ap[u] = true;
                }
                if (parent[u] != -1 && low[v] >= disc[u]) {
                    ap[u] = true;
                }
            } else if (v != parent[u]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ArticulationPointComparison::new);
    }
}