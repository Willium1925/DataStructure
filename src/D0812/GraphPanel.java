package D0812;// GraphPanel.java
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class GraphPanel extends JPanel {
    private Graph graph;
    private List<Node> shortestPath = Collections.emptyList();
    private static final int NODE_DIAMETER = 20;

    public GraphPanel() {
        this.graph = new Graph();
        setBackground(Color.WHITE);
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.shortestPath = Collections.emptyList();
        repaint();
    }

    public void setShortestPath(List<Node> shortestPath) {
        this.shortestPath = (shortestPath == null) ? Collections.emptyList() : shortestPath;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (graph == null) return;

        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.GRAY);
        for (Edge edge : graph.getEdges()) {
            g2d.drawLine(edge.getSource().getX(), edge.getSource().getY(),
                    edge.getDestination().getX(), edge.getDestination().getY());
        }

        if (!shortestPath.isEmpty()) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(4));
            for (int i = 0; i < shortestPath.size() - 1; i++) {
                Node u = shortestPath.get(i);
                Node v = shortestPath.get(i + 1);
                g2d.drawLine(u.getX(), u.getY(), v.getX(), v.getY());
            }
        }

        for (Node node : graph.getNodes()) {
            g2d.setColor(Color.BLUE);
            if (shortestPath.contains(node)) {
                g2d.setColor(Color.RED);
            }
            g2d.fillOval(node.getX() - NODE_DIAMETER / 2, node.getY() - NODE_DIAMETER / 2, NODE_DIAMETER, NODE_DIAMETER);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int stringWidth = fm.stringWidth(node.getLabel());
            g2d.drawString(node.getLabel(), node.getX() - stringWidth / 2, node.getY() + fm.getAscent() / 2 - 2);
        }
    }
}