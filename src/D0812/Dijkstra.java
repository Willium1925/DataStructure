package D0812;// Dijkstra.java
import java.util.*;

public class Dijkstra {

    public static class Result {
        public final Map<Node, Double> distances;
        public final Map<Node, Node> predecessors;

        public Result(Map<Node, Double> distances, Map<Node, Node> predecessors) {
            this.distances = distances;
            this.predecessors = predecessors;
        }
    }

    public static Result findShortestPaths(Graph graph, Node startNode) {
        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        Set<Node> visited = new HashSet<>();

        for (Node node : graph.getNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
            predecessors.put(node, null);
        }
        distances.put(startNode, 0.0);
        pq.add(startNode);

        while (!pq.isEmpty()) {
            Node currentNode = pq.poll();

            if (visited.contains(currentNode)) continue;
            visited.add(currentNode);

            for (Edge edge : graph.getEdges()) {
                Node neighbor = null;
                if (edge.getSource().equals(currentNode)) {
                    neighbor = edge.getDestination();
                } else if (edge.getDestination().equals(currentNode)) {
                    neighbor = edge.getSource();
                }

                if (neighbor != null && !visited.contains(neighbor)) {
                    double newDist = distances.get(currentNode) + edge.getWeight();
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        predecessors.put(neighbor, currentNode);
                        pq.remove(neighbor);
                        pq.add(neighbor);
                    }
                }
            }
        }
        return new Result(distances, predecessors);
    }

    public static List<Node> getPath(Result result, Node targetNode) {
        LinkedList<Node> path = new LinkedList<>();
        Node step = targetNode;
        // 檢查路徑是否存在
        if (result.predecessors.get(step) == null && !step.equals(result.predecessors.keySet().iterator().next())) {
            return null;
        }

        while (step != null) {
            path.addFirst(step);
            step = result.predecessors.get(step);
        }
        return path;
    }
}