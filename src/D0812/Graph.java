package D0812;// Graph.java
import java.util.*;

public class Graph {
    private final List<Node> nodes;
    private final List<Edge> edges;

    public Graph() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public void addNode(Node node) { nodes.add(node); }

    public void addEdge(Node source, Node dest) {
        if (source.equals(dest)) return;
        for (Edge edge : edges) {
            if ((edge.getSource().equals(source) && edge.getDestination().equals(dest)) ||
                    (edge.getSource().equals(dest) && edge.getDestination().equals(source))) {
                return;
            }
        }
        edges.add(new Edge(source, dest));
    }

    public List<Node> getNodes() { return Collections.unmodifiableList(nodes); }
    public List<Edge> getEdges() { return Collections.unmodifiableList(edges); }
    public Node getNodeById(int id) {
        return nodes.stream().filter(node -> node.getId() == id).findFirst().orElse(null);
    }

    public void generateRandomGraph(int numNodes, int numEdges, int width, int height) {
        nodes.clear();
        edges.clear();
        Random rand = new Random();

        for (int i = 0; i < numNodes; i++) {
            int x = rand.nextInt(width - 60) + 30;
            int y = rand.nextInt(height - 60) + 30;
            addNode(new Node(i, x, y));
        }

        long maxEdges = (long) numNodes * (numNodes - 1) / 2;
        if (numEdges > maxEdges) {
            numEdges = (int) maxEdges;
        }

        while (edges.size() < numEdges && numNodes > 1) {
            Node source = nodes.get(rand.nextInt(numNodes));
            Node dest = nodes.get(rand.nextInt(numNodes));
            addEdge(source, dest);
        }
    }

    public String getAdjacencyMatrixString() {
        if (nodes.isEmpty()) return "圖是空的";

        double[][] matrix = new double[nodes.size()][nodes.size()];
        for (double[] row : matrix) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        for (int i = 0; i < nodes.size(); i++) matrix[i][i] = 0;

        for (Edge edge : edges) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            matrix[u][v] = edge.getWeight();
            matrix[v][u] = edge.getWeight();
        }

        StringBuilder sb = new StringBuilder("鄰接矩陣 (∞ 表示不相連):\n");
        sb.append(String.format("%-6s", ""));
        for (int i = 0; i < nodes.size(); i++) sb.append(String.format("%-8s", "V" + i));
        sb.append("\n");

        for (int i = 0; i < nodes.size(); i++) {
            sb.append(String.format("%-6s", "V" + i));
            for (int j = 0; j < nodes.size(); j++) {
                if (matrix[i][j] == Double.POSITIVE_INFINITY) {
                    sb.append(String.format("%-8s", "∞"));
                } else {
                    sb.append(String.format("%-8.2f", matrix[i][j]));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}