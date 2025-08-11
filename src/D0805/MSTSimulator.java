package D0805;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class MSTSimulator extends JFrame {
    private JPanel controlPanel;
    private JTextField nodesField, edgesField, speedField;
    private JComboBox<String> algoComboBox;
    private JTextArea outputArea;
    private GraphCanvas canvas;
    private JButton startButton;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<Edge> mstEdges;
    private Random rand = new Random();

    public MSTSimulator() {
        setTitle("最小生成樹模擬器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 控制面板
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.add(new JLabel("節點數:"));
        nodesField = new JTextField("5", 5);
        controlPanel.add(nodesField);
        controlPanel.add(new JLabel("邊數:"));
        edgesField = new JTextField("7", 5);
        controlPanel.add(edgesField);
        controlPanel.add(new JLabel("動畫延遲(ms):"));
        speedField = new JTextField("1000", 5);
        controlPanel.add(speedField);
        algoComboBox = new JComboBox<>(new String[]{"Kruskal", "Prim"});
        controlPanel.add(algoComboBox);
        startButton = new JButton("開始模擬");
        controlPanel.add(startButton);
        add(controlPanel, BorderLayout.NORTH);

        // 輸出區域
        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        // 圖形畫布
        canvas = new GraphCanvas();
        add(canvas, BorderLayout.CENTER);

        // 按鈕事件
        startButton.addActionListener(e -> startSimulation());
    }

    private void startSimulation() {
        outputArea.setText("");
        canvas.clear();
        if (!generateGraph()) return;
        canvas.repaint();
        String algorithm = (String) algoComboBox.getSelectedItem();
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> startButton.setEnabled(false));
            if (algorithm.equals("Kruskal")) {
                runKruskal();
            } else {
                runPrim();
            }
            SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
        }).start();
    }

    private boolean generateGraph() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        mstEdges = new ArrayList<>();
        int numNodes, numEdges, delay;

        try {
            numNodes = Integer.parseInt(nodesField.getText());
            numEdges = Integer.parseInt(edgesField.getText());
            delay = Integer.parseInt(speedField.getText());
            if (numNodes < 2) {
                outputArea.append("錯誤：節點數必須≥2\n");
                return false;
            }
            if (numEdges < numNodes - 1 || numEdges > numNodes * (numNodes - 1) / 2) {
                outputArea.append("錯誤：邊數必須在 " + (numNodes - 1) + " 至 " + (numNodes * (numNodes - 1) / 2) + " 之間\n");
                return false;
            }
            if (delay < 100 || delay > 5000) {
                outputArea.append("錯誤：動畫延遲必須在100至5000毫秒之間\n");
                return false;
            }
        } catch (NumberFormatException e) {
            outputArea.append("錯誤：請輸入有效的數字\n");
            return false;
        }

        // 生成隨機節點
        for (int i = 0; i < numNodes; i++) {
            int x = rand.nextInt(600) + 100;
            int y = rand.nextInt(300) + 100;
            nodes.add(new Node(i, x, y));
        }

        // 生成隨機邊
        Set<String> edgeSet = new HashSet<>();
        while (edges.size() < numEdges) {
            int u = rand.nextInt(numNodes);
            int v = rand.nextInt(numNodes);
            if (u != v && !edgeSet.contains(u + "-" + v) && !edgeSet.contains(v + "-" + u)) {
                int weight = rand.nextInt(90) + 10;
                edges.add(new Edge(u, v, weight));
                edgeSet.add(u + "-" + v);
            }
        }
        outputArea.append("已生成圖：節點數=" + numNodes + "，邊數=" + numEdges + "\n");
        return true;
    }

    private void runKruskal() {
        int delay = Integer.parseInt(speedField.getText());
        outputArea.append("執行Kruskal演算法...\n");
        Collections.sort(edges);
        DisjointSet ds = new DisjointSet(nodes.size());
        mstEdges.clear();
        int edgeCount = 0;

        for (Edge edge : edges) {
            int u = ds.find(edge.u);
            int v = ds.find(edge.v);
            if (u != v) {
                mstEdges.add(edge);
                ds.union(u, v);
                edgeCount++;
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("加入邊 (" + edge.u + ", " + edge.v + ")，權重=" + edge.weight + "\n");
                    canvas.setCurrentEdge(edge);
                    canvas.repaint();
                });
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (edgeCount == nodes.size() - 1) break;
        }
        SwingUtilities.invokeLater(() -> {
            outputArea.append("Kruskal演算法完成。總權重：" +
                    mstEdges.stream().mapToInt(e -> e.weight).sum() + "\n");
            canvas.setCurrentEdge(null);
            canvas.repaint();
        });
    }

    private void runPrim() {
        int delay = Integer.parseInt(speedField.getText());
        outputArea.append("執行Prim演算法...\n");
        PriorityQueue<Edge> pq = new PriorityQueue<>();
        boolean[] inMST = new boolean[nodes.size()];
        mstEdges.clear();
        int startVertex = 0;
        inMST[startVertex] = true;

        for (Edge edge : edges) {
            if (edge.u == startVertex) {
                pq.add(edge);
            } else if (edge.v == startVertex) {
                pq.add(new Edge(edge.v, edge.u, edge.weight));
            }
        }

        while (!pq.isEmpty() && mstEdges.size() < nodes.size() - 1) {
            Edge edge = pq.poll();
            int v = inMST[edge.u] ? edge.v : edge.u;
            if (inMST[v]) continue;
            inMST[v] = true;
            mstEdges.add(edge);
            SwingUtilities.invokeLater(() -> {
                outputArea.append("加入邊 (" + edge.u + ", " + edge.v + ")，權重=" + edge.weight + "\n");
                canvas.setCurrentEdge(edge);
                canvas.repaint();
            });
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Edge e : edges) {
                if (e.u == v && !inMST[e.v]) {
                    pq.add(new Edge(v, e.v, e.weight));
                } else if (e.v == v && !inMST[e.u]) {
                    pq.add(new Edge(v, e.u, e.weight));
                }
            }
        }
        SwingUtilities.invokeLater(() -> {
            outputArea.append("Prim演算法完成。總權重：" +
                    mstEdges.stream().mapToInt(e -> e.weight).sum() + "\n");
            canvas.setCurrentEdge(null);
            canvas.repaint();
        });
    }

    private class Node {
        int id, x, y;
        Node(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private class Edge implements Comparable<Edge> {
        int u, v, weight;
        Edge(int u, int v, int weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
        @Override
        public int compareTo(Edge other) {
            return Integer.compare(this.weight, other.weight);
        }
    }

    private class DisjointSet {
        int[] parent;
        DisjointSet(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }
        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);
            return parent[x];
        }
        void union(int x, int y) {
            parent[find(x)] = find(y);
        }
    }

    private class GraphCanvas extends JPanel {
        private Edge currentEdge;

        void setCurrentEdge(Edge edge) {
            this.currentEdge = edge;
        }

        void clear() {
            nodes = null;
            edges = null;
            mstEdges = null;
            currentEdge = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (nodes == null || edges == null) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 繪製邊
            g2.setColor(Color.GRAY);
            for (Edge edge : edges) {
                Node u = nodes.get(edge.u);
                Node v = nodes.get(edge.v);
                g2.drawLine(u.x, u.y, v.x, v.y);
                int midX = (u.x + v.x) / 2;
                int midY = (u.y + v.y) / 2;
                g2.drawString(String.valueOf(edge.weight), midX, midY);
            }

            // 繪製MST邊
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            for (Edge edge : mstEdges) {
                Node u = nodes.get(edge.u);
                Node v = nodes.get(edge.v);
                g2.drawLine(u.x, u.y, v.x, v.y);
            }

            // 繪製當前處理的邊
            if (currentEdge != null) {
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(3));
                Node u = nodes.get(currentEdge.u);
                Node v = nodes.get(currentEdge.v);
                g2.drawLine(u.x, u.y, v.x, v.y);
            }

            // 繪製節點
            g2.setColor(Color.BLACK);
            for (Node node : nodes) {
                g2.fillOval(node.x - 10, node.y - 10, 20, 20);
                g2.drawString(String.valueOf(node.id), node.x - 5, node.y - 15);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MSTSimulator().setVisible(true);
        });
    }
}