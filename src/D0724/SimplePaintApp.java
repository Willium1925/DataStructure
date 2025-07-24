package D0724;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 簡單的繪圖應用程式
 * 功能包括：選擇顏色、調整畫筆粗細、上一步/下一步操作
 */
public class SimplePaintApp extends JFrame {

    // 繪圖面板
    private DrawingPanel drawingPanel;

    // 工具列組件
    private JButton colorButton;
    private JSlider brushSizeSlider;
    private JButton undoButton;
    private JButton redoButton;
    private JButton clearButton;
    private JLabel brushSizeLabel;

    // 當前繪圖設定
    private Color currentColor = Color.BLACK;
    private int currentBrushSize = 5;

    /**
     * 建構子 - 初始化整個應用程式
     */
    public SimplePaintApp() {
        setTitle("簡單繪圖程式");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // 視窗置中

        initializeComponents();
        setupLayout();
        setupEventListeners();

        setVisible(true);
    }

    /**
     * 初始化所有GUI組件
     */
    private void initializeComponents() {
        // 創建繪圖面板
        drawingPanel = new DrawingPanel();

        // 創建工具列按鈕和控制項
        colorButton = new JButton("選擇顏色");
        colorButton.setBackground(currentColor);
        colorButton.setPreferredSize(new Dimension(100, 30));

        // 畫筆粗細滑桿 (1-20像素)
        brushSizeSlider = new JSlider(1, 20, currentBrushSize);
        brushSizeSlider.setPreferredSize(new Dimension(150, 30));
        brushSizeSlider.setMajorTickSpacing(5);
        brushSizeSlider.setMinorTickSpacing(1);
        brushSizeSlider.setPaintTicks(true);
        brushSizeSlider.setPaintLabels(true);

        brushSizeLabel = new JLabel("畫筆粗細: " + currentBrushSize);

        undoButton = new JButton("上一步");
        redoButton = new JButton("下一步");
        clearButton = new JButton("清除全部");

        // 初始狀態設定
        redoButton.setEnabled(false);
    }

    /**
     * 設定GUI佈局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 工具列面板
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolPanel.add(colorButton);
        toolPanel.add(new JLabel("   ")); // 間隔
        toolPanel.add(brushSizeLabel);
        toolPanel.add(brushSizeSlider);
        toolPanel.add(new JLabel("   ")); // 間隔
        toolPanel.add(undoButton);
        toolPanel.add(redoButton);
        toolPanel.add(clearButton);

        // 將組件加入主視窗
        add(toolPanel, BorderLayout.NORTH);
        add(drawingPanel, BorderLayout.CENTER);
    }

    /**
     * 設定事件監聽器
     */
    private void setupEventListeners() {
        // 顏色選擇按鈕事件
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "選擇畫筆顏色", currentColor);
            if (newColor != null) {
                currentColor = newColor;
                colorButton.setBackground(currentColor);
                drawingPanel.setCurrentColor(currentColor);
            }
        });

        // 畫筆粗細滑桿事件
        brushSizeSlider.addChangeListener(e -> {
            currentBrushSize = brushSizeSlider.getValue();
            brushSizeLabel.setText("畫筆粗細: " + currentBrushSize);
            drawingPanel.setCurrentBrushSize(currentBrushSize);
        });

        // 上一步按鈕事件
        undoButton.addActionListener(e -> {
            drawingPanel.undo();
            updateButtonStates();
        });

        // 下一步按鈕事件
        redoButton.addActionListener(e -> {
            drawingPanel.redo();
            updateButtonStates();
        });

        // 清除按鈕事件
        clearButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "確定要清除所有內容嗎？",
                    "確認清除",
                    JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
                drawingPanel.clear();
                updateButtonStates();
            }
        });
    }

    /**
     * 更新按鈕的啟用/停用狀態
     */
    private void updateButtonStates() {
        undoButton.setEnabled(drawingPanel.canUndo());
        redoButton.setEnabled(drawingPanel.canRedo());
    }

    /**
     * 繪圖面板類別 - 負責實際的繪圖功能
     */
    private class DrawingPanel extends JPanel {

        // 儲存所有繪圖動作的列表
        private List<DrawingAction> drawingHistory;
        private int currentHistoryIndex;

        // 目前繪圖設定
        private Color currentColor = Color.BLACK;
        private int currentBrushSize = 5;

        // 滑鼠狀態
        private boolean isDrawing = false;
        private Point lastPoint;

        /**
         * 繪圖面板建構子
         */
        public DrawingPanel() {
            setBackground(Color.WHITE);
            drawingHistory = new ArrayList<>();
            currentHistoryIndex = -1;

            setupMouseListeners();
        }

        /**
         * 設定滑鼠事件監聽器
         */
        private void setupMouseListeners() {
            // 滑鼠按下事件
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isDrawing = true;
                    lastPoint = e.getPoint();

                    // 開始新的繪圖動作
                    startNewDrawingAction();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isDrawing = false;

                    // 完成當前繪圖動作
                    finishCurrentDrawingAction();
                    updateButtonStates();
                }
            });

            // 滑鼠拖拽事件
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDrawing && lastPoint != null) {
                        Point currentPoint = e.getPoint();

                        // 在當前繪圖動作中加入線段
                        addLineToCurrentAction(lastPoint, currentPoint, currentColor, currentBrushSize);

                        lastPoint = currentPoint;
                        repaint(); // 重新繪製
                    }
                }
            });
        }

        /**
         * 開始新的繪圖動作
         */
        private void startNewDrawingAction() {
            // 如果目前不在歷史記錄的最新位置，清除後面的記錄
            if (currentHistoryIndex < drawingHistory.size() - 1) {
                drawingHistory = drawingHistory.subList(0, currentHistoryIndex + 1);
            }

            // 加入新的繪圖動作
            drawingHistory.add(new DrawingAction());
            currentHistoryIndex++;
        }

        /**
         * 在當前繪圖動作中加入線段
         */
        private void addLineToCurrentAction(Point from, Point to, Color color, int brushSize) {
            if (currentHistoryIndex >= 0 && currentHistoryIndex < drawingHistory.size()) {
                DrawingAction currentAction = drawingHistory.get(currentHistoryIndex);
                currentAction.addLine(new DrawingLine(from, to, color, brushSize));
            }
        }

        /**
         * 完成當前繪圖動作
         */
        private void finishCurrentDrawingAction() {
            // 這裡可以做一些收尾工作，目前暫時不需要
        }

        /**
         * 繪製面板內容
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;

            // 設定抗鋸齒，讓線條更平滑
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 繪製所有歷史記錄中的動作（到當前位置為止）
            for (int i = 0; i <= currentHistoryIndex && i < drawingHistory.size(); i++) {
                DrawingAction action = drawingHistory.get(i);
                action.draw(g2d);
            }
        }

        /**
         * 設定當前顏色
         */
        public void setCurrentColor(Color color) {
            this.currentColor = color;
        }

        /**
         * 設定當前畫筆粗細
         */
        public void setCurrentBrushSize(int size) {
            this.currentBrushSize = size;
        }

        /**
         * 復原操作
         */
        public void undo() {
            if (canUndo()) {
                currentHistoryIndex--;
                repaint();
            }
        }

        /**
         * 重做操作
         */
        public void redo() {
            if (canRedo()) {
                currentHistoryIndex++;
                repaint();
            }
        }

        /**
         * 檢查是否可以復原
         */
        public boolean canUndo() {
            return currentHistoryIndex >= 0;
        }

        /**
         * 檢查是否可以重做
         */
        public boolean canRedo() {
            return currentHistoryIndex < drawingHistory.size() - 1;
        }

        /**
         * 清除所有內容
         */
        public void clear() {
            drawingHistory.clear();
            currentHistoryIndex = -1;
            repaint();
        }
    }

    /**
     * 繪圖動作類別 - 代表一次完整的繪圖操作（例如畫一條連續的線）
     */
    private static class DrawingAction {
        private List<DrawingLine> lines;

        public DrawingAction() {
            lines = new ArrayList<>();
        }

        public void addLine(DrawingLine line) {
            lines.add(line);
        }

        public void draw(Graphics2D g2d) {
            for (DrawingLine line : lines) {
                line.draw(g2d);
            }
        }
    }

    /**
     * 繪圖線段類別 - 代表兩點之間的一條線
     */
    private static class DrawingLine {
        private Point from;
        private Point to;
        private Color color;
        private int brushSize;

        public DrawingLine(Point from, Point to, Color color, int brushSize) {
            this.from = new Point(from); // 複製點以避免參考問題
            this.to = new Point(to);
            this.color = color;
            this.brushSize = brushSize;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(from.x, from.y, to.x, to.y);
        }
    }

    /**
     * 主程式進入點
     */
    public static void main(String[] args) {
        // 設定系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 在事件分派執行緒中啟動GUI
        SwingUtilities.invokeLater(() -> {
            new SimplePaintApp();
        });
    }
}