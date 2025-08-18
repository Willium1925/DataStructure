package D0812;// Node.java
import java.awt.Point;

public class Node {
    private final int id;
    private final String label;
    private int x;
    private int y;

    public Node(int id, int x, int y) {
        this.id = id;
        this.label = String.valueOf(id);
        this.x = x;
        this.y = y;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Point getPoint() { return new Point(x, y); }
}