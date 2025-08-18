package D0812;// FloydWarshall.java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 實現 Floyd-Warshall 演算法來找到圖中所有節點對之間的最短路徑。
 */
public class FloydWarshall {

    private final double[][] dist; // 距離矩陣，dist[i][j] 儲存從 i 到 j 的最短距離
    private final int[][] next;   // 路徑重建矩陣，next[i][j] 儲存從 i 到 j 路徑上的下一個節點
    private final int numNodes;

    /**
     * 構造函數，初始化並執行 Floyd-Warshall 演算法。
     * @param graph 要計算的圖。
     */
    public FloydWarshall(Graph graph) {
        this.numNodes = graph.getNodes().size();
        this.dist = new double[numNodes][numNodes];
        this.next = new int[numNodes][numNodes];

        // 1. 初始化距離 (dist) 和路徑 (next) 矩陣
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                    next[i][j] = j;
                } else {
                    dist[i][j] = Double.POSITIVE_INFINITY;
                    next[i][j] = -1; // -1 表示沒有直接路徑
                }
            }
        }

        // 根據圖的邊來填寫初始權重
        for (Edge edge : graph.getEdges()) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            double weight = edge.getWeight();
            dist[u][v] = weight;
            dist[v][u] = weight; // 無向圖
            next[u][v] = v;
            next[v][u] = u;
        }

        // 2. 演算法核心：三層迴圈
        // k 是中介點
        for (int k = 0; k < numNodes; k++) {
            // i 是起點
            for (int i = 0; i < numNodes; i++) {
                // j 是終點
                for (int j = 0; j < numNodes; j++) {
                    // 如果從 i 到 k 再到 j 的路徑比現有從 i 到 j 的路徑更短
                    if (dist[i][k] != Double.POSITIVE_INFINITY &&
                            dist[k][j] != Double.POSITIVE_INFINITY &&
                            dist[i][k] + dist[k][j] < dist[i][j]) {

                        // 更新最短距離
                        dist[i][j] = dist[i][k] + dist[k][j];
                        // 更新路徑，從 i 到 j 的下一步應該和從 i 到 k 的下一步一樣
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
    }

    /**
     * 獲取指定起點和終點的最短距離。
     * @param u 起點 ID
     * @param v 終點 ID
     * @return 最短距離，如果不可達則為無窮大
     */
    public double getShortestDistance(int u, int v) {
        return dist[u][v];
    }

    /**
     * 重建從起點 u 到終點 v 的最短路徑。
     * @param u 起點 ID
     * @param v 終點 ID
     * @param graph 圖物件，用於將節點 ID 轉換為 Node 物件
     * @return 由 Node 物件組成的路徑列表，如果不可達則為 null
     */
    public List<Node> getPath(int u, int v, Graph graph) {
        if (next[u][v] == -1) {
            return null; // 不可達
        }

        List<Node> path = new ArrayList<>();
        int current = u;
        while (current != v) {
            path.add(graph.getNodeById(current));
            current = next[current][v];
        }
        path.add(graph.getNodeById(v));
        return path;
    }
}