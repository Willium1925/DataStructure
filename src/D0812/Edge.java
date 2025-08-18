package D0812;

// Edge.java
public class Edge {
    private final Node source;
    private final Node destination;
    private final double weight;

    public Edge(Node source, Node destination) {
        this.source = source;
        this.destination = destination;
        this.weight = Math.sqrt(Math.pow(source.getX() - destination.getX(), 2) + Math.pow(source.getY() - destination.getY(), 2));
    }

    public Node getSource() { return source; }
    public Node getDestination() { return destination; }
    public double getWeight() { return weight; }
}