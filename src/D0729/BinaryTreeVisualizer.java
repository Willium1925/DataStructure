package D0729;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * 二元樹演示器主類 - 使用陣列實作
 * 陣列[0]存放資料量，節點從[1]開始存放
 */
public class BinaryTreeVisualizer extends JFrame {
    // 陣列結構的二元樹
    private Integer[] tree;           // 樹陣列，[0]存放節點數量
    private boolean[] deleted;        // 標記已刪除的節點
    private int maxSize;             // 陣列最大容量
    private int selectedIndex = -1;   // 當前選中的節點索引

    // GUI 元件
    private TreePanel treePanel;              // 樹形圖顯示面板
    private JTextArea traversalResult;        // 遍歷結果顯示區
    private JTextField inputField;            // 使用者輸入欄
    private JTextField randomCountField;      // 隨機數量輸入欄

    // 節點操作相關元件
    private JLabel selectedNodeLabel;
    private JTextField editField;
    private JButton editButton;
    private JButton deleteButton;

    // 顯示相關常數
    private static final int NODE_RADIUS = 18;     // 節點半徑（調小一些）
    private static final int LEVEL_HEIGHT = 60;    // 層級間距（調小一些）
    private static final int MIN_NODE_SPACING = 45; // 最小節點間距

    /**
     * 可拖曳和縮放的樹顯示面板
     */
    class TreePanel extends JPanel {
        private double scale = 1.0;          // 縮放比例
        private int offsetX = 0, offsetY = 0; // 偏移量
        private Point lastMousePoint;         // 上一次滑鼠位置
        private boolean isDragging = false;   // 是否正在拖曳

        public TreePanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("二元樹結構 (可拖曳/滾輪縮放)"));

            // 滑鼠事件處理
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMousePoint = e.getPoint();

                    // 檢查是否點擊在節點上
                    int clickedIndex = findNodeAt(e.getX(), e.getY());
                    if (clickedIndex != -1) {
                        selectedIndex = clickedIndex;
                        updateSelectedNodeInfo();
                        repaint();
                    } else {
                        isDragging = true;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isDragging = false;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDragging && lastMousePoint != null) {
                        int dx = e.getX() - lastMousePoint.x;
                        int dy = e.getY() - lastMousePoint.y;
                        offsetX += dx;
                        offsetY += dy;
                        lastMousePoint = e.getPoint();
                        repaint();
                    }
                }
            });

            // 滑鼠滾輪縮放
            addMouseWheelListener(e -> {
                double factor = 1.1;
                if (e.getWheelRotation() < 0) {
                    scale *= factor;
                } else {
                    scale /= factor;
                }
                scale = Math.max(0.1, Math.min(scale, 3.0)); // 限制縮放範圍
                repaint();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 應用變換
            g2d.translate(offsetX, offsetY);
            g2d.scale(scale, scale);

            drawTree(g2d);
        }

        /**
         * 重置視圖位置和縮放
         */
        public void resetView() {
            scale = 1.0;
            offsetX = 0;
            offsetY = 0;
            repaint();
        }
    }

    public BinaryTreeVisualizer() {
        initializeTree();
        initializeGUI();
    }

    /**
     * 初始化陣列樹結構
     */
    private void initializeTree() {
        maxSize = 127; // 可容納7層完全二元樹 (2^7 - 1)
        tree = new Integer[maxSize + 1]; // +1 因為從索引1開始使用
        deleted = new boolean[maxSize + 1];
        tree[0] = 0; // 初始節點數量為0
    }

    /**
     * 初始化圖形使用者介面
     */
    private void initializeGUI() {
        setTitle("二元樹演示器 - 陣列實作版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 建立左側面板（包含樹形圖和遍歷結果）
        createLeftPanel();

        // 建立右側控制面板
        createControlPanel();

        // 設定視窗大小和居中顯示
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // 初始化示例樹
        initializeSampleTree();
    }

    /**
     * 建立左側面板（包含樹形圖和遍歷結果）
     */
    private void createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 上半部：樹形圖面板
        createTreePanel(leftPanel);

        // 下半部：遍歷結果面板
        createTraversalPanel(leftPanel);

        add(leftPanel, BorderLayout.CENTER);
    }

    /**
     * 建立樹形圖顯示面板
     */
    private void createTreePanel(JPanel parentPanel) {
        JPanel treePanelContainer = new JPanel(new BorderLayout());

        treePanel = new TreePanel();
        treePanelContainer.add(treePanel, BorderLayout.CENTER);

        // 添加控制按鈕
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton resetViewBtn = new JButton("重置視圖");
        JButton centerTreeBtn = new JButton("置中顯示");

        resetViewBtn.addActionListener(e -> treePanel.resetView());
        centerTreeBtn.addActionListener(e -> centerTree());

        controlPanel.add(resetViewBtn);
        controlPanel.add(centerTreeBtn);
        treePanelContainer.add(controlPanel, BorderLayout.SOUTH);

        // 設定樹形圖面板佔上半部（70%高度）
        treePanelContainer.setPreferredSize(new Dimension(0, 600));
        parentPanel.add(treePanelContainer, BorderLayout.CENTER);
    }

    /**
     * 建立遍歷結果顯示面板
     */
    private void createTraversalPanel(JPanel parentPanel) {
        JPanel traversalContainer = new JPanel(new BorderLayout());
        traversalContainer.setBorder(BorderFactory.createTitledBorder("遍歷結果"));
        traversalContainer.setPreferredSize(new Dimension(0, 250)); // 下半部高度

        traversalResult = new JTextArea();
        traversalResult.setEditable(false);
        traversalResult.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        traversalResult.setBackground(Color.LIGHT_GRAY);

        JScrollPane scrollPane = new JScrollPane(traversalResult);
        traversalContainer.add(scrollPane, BorderLayout.CENTER);

        // 遍歷按鈕面板
        JPanel traversalButtons = new JPanel(new GridLayout(1, 4, 5, 5));

        JButton preOrderBtn = new JButton("前序遍歷 (VLR)");
        JButton inOrderBtn = new JButton("中序遍歷 (LVR)");
        JButton postOrderBtn = new JButton("後序遍歷 (LRV)");
        JButton levelOrderBtn = new JButton("層序遍歷");

        // 設定按鈕事件
        preOrderBtn.addActionListener(e -> performTraversal("前序"));
        inOrderBtn.addActionListener(e -> performTraversal("中序"));
        postOrderBtn.addActionListener(e -> performTraversal("後序"));
        levelOrderBtn.addActionListener(e -> performTraversal("層序"));

        traversalButtons.add(preOrderBtn);
        traversalButtons.add(inOrderBtn);
        traversalButtons.add(postOrderBtn);
        traversalButtons.add(levelOrderBtn);

        traversalContainer.add(traversalButtons, BorderLayout.SOUTH);
        parentPanel.add(traversalContainer, BorderLayout.SOUTH);
    }

    /**
     * 建立右側控制面板
     */
    private void createControlPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder("控制面板"));
        rightPanel.setPreferredSize(new Dimension(320, 0)); // 稍微加寬

        // 陣列狀態顯示
        rightPanel.add(createArrayStatusSection());
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 使用者輸入區
        rightPanel.add(createInputSection());
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 隨機生成區
        rightPanel.add(createRandomSection());
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 節點操作區
        rightPanel.add(createNodeOperationSection());
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 樹操作區
        rightPanel.add(createTreeOperationSection());

        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * 建立陣列狀態顯示區域
     */
    private JPanel createArrayStatusSection() {
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createTitledBorder("陣列狀態"));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        JLabel sizeLabel = new JLabel("節點數量：0");
        JLabel capacityLabel = new JLabel("最大容量：" + maxSize);

        sizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        capacityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusPanel.add(sizeLabel);
        statusPanel.add(capacityLabel);

        // 儲存引用以便更新
        this.sizeLabel = sizeLabel;

        return statusPanel;
    }

    private JLabel sizeLabel; // 節點數量標籤引用

    /**
     * 建立使用者輸入區域
     */
    private JPanel createInputSection() {
        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(BorderFactory.createTitledBorder("手動輸入"));
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        inputField = new JTextField();
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JTextField positionField = new JTextField();
        positionField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JButton addButton = new JButton("新增到指定位置");
        JButton addNextButton = new JButton("新增到下一個空位");

        addButton.addActionListener(e -> addNodeAtPosition(inputField.getText(), positionField.getText()));
        addNextButton.addActionListener(e -> addNodeAtNextEmpty(inputField.getText()));

        // Enter鍵快速新增
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addNodeAtNextEmpty(inputField.getText());
                }
            }
        });

        inputPanel.add(new JLabel("輸入數值："));
        inputPanel.add(inputField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        inputPanel.add(new JLabel("指定位置（可選）："));
        inputPanel.add(positionField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(addNextButton);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        inputPanel.add(addButton);

        return inputPanel;
    }

    /**
     * 建立隨機生成區域
     */
    private JPanel createRandomSection() {
        JPanel randomPanel = new JPanel();
        randomPanel.setBorder(BorderFactory.createTitledBorder("隨機生成"));
        randomPanel.setLayout(new BoxLayout(randomPanel, BoxLayout.Y_AXIS));

        randomCountField = new JTextField("15");
        randomCountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JButton randomButton = new JButton("生成隨機樹");
        randomButton.addActionListener(e -> generateRandomTree());

        randomPanel.add(new JLabel("節點數量："));
        randomPanel.add(randomCountField);
        randomPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        randomPanel.add(randomButton);

        return randomPanel;
    }

    /**
     * 建立節點操作區域
     */
    private JPanel createNodeOperationSection() {
        JPanel nodePanel = new JPanel();
        nodePanel.setBorder(BorderFactory.createTitledBorder("節點操作"));
        nodePanel.setLayout(new BoxLayout(nodePanel, BoxLayout.Y_AXIS));

        selectedNodeLabel = new JLabel("選中節點：無");
        selectedNodeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        editField = new JTextField();
        editField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        editField.setEnabled(false);

        editButton = new JButton("修改節點值");
        deleteButton = new JButton("刪除節點及子節點");

        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // 設定按鈕事件
        editButton.addActionListener(e -> editSelectedNode(editField.getText()));
        deleteButton.addActionListener(e -> deleteSelectedNode());

        // 設定刪除按鈕顏色提醒
        deleteButton.setBackground(new Color(255, 200, 200));
        deleteButton.setOpaque(true);

        nodePanel.add(selectedNodeLabel);
        nodePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        nodePanel.add(new JLabel("新數值："));
        nodePanel.add(editField);
        nodePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        nodePanel.add(editButton);
        nodePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        nodePanel.add(deleteButton);

        return nodePanel;
    }

    /**
     * 建立樹操作區域
     */
    private JPanel createTreeOperationSection() {
        JPanel treeOperationPanel = new JPanel();
        treeOperationPanel.setBorder(BorderFactory.createTitledBorder("樹操作"));
        treeOperationPanel.setLayout(new BoxLayout(treeOperationPanel, BoxLayout.Y_AXIS));

        JButton clearButton = new JButton("清空樹");
        JButton showArrayButton = new JButton("顯示陣列內容");
        JButton compactButton = new JButton("壓縮陣列");

        clearButton.addActionListener(e -> clearTree());
        showArrayButton.addActionListener(e -> showArrayContent());
        compactButton.addActionListener(e -> createArrayStatusSection());

        // 設定清空按鈕顏色
        clearButton.setBackground(new Color(255, 200, 200));
        clearButton.setOpaque(true);

        // 設定壓縮按鈕顏色
        compactButton.setBackground(new Color(200, 255, 200));
        compactButton.setOpaque(true);

        treeOperationPanel.add(clearButton);
        treeOperationPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        treeOperationPanel.add(showArrayButton);
        treeOperationPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        treeOperationPanel.add(compactButton);

        return treeOperationPanel;
    }

    /**
     * 初始化示例樹
     */
    private void initializeSampleTree() {
        // 建立一個示例樹
        tree[1] = 50;  // 根節點
        tree[2] = 30;  // 左子節點
        tree[3] = 70;  // 右子節點
        tree[4] = 20;  // 左子節點的左子節點
        tree[5] = 40;  // 左子節點的右子節點
        tree[6] = 60;  // 右子節點的左子節點
        tree[7] = 80;  // 右子節點的右子節點

        tree[0] = 7; // 設定節點數量

        updateDisplay();
    }

    /**
     * 新增節點到指定位置
     */
    private void addNodeAtPosition(String valueStr, String positionStr) {
        try {
            if (valueStr.trim().isEmpty()) return;

            int value = Integer.parseInt(valueStr.trim());
            int position = positionStr.trim().isEmpty() ? getNextEmptyPosition() : Integer.parseInt(positionStr.trim());

            if (position < 1 || position > maxSize) {
                JOptionPane.showMessageDialog(this, "位置必須在 1 到 " + maxSize + " 之間", "位置錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }

            tree[position] = value;
            deleted[position] = false;
            tree[0] = Math.max(tree[0], position); // 更新節點數量

            inputField.setText("");
            updateDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的整數", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 新增節點到下一個空位置
     */
    private void addNodeAtNextEmpty(String valueStr) {
        try {
            if (valueStr.trim().isEmpty()) return;

            int value = Integer.parseInt(valueStr.trim());
            int position = getNextEmptyPosition();

            if (position > maxSize) {
                JOptionPane.showMessageDialog(this, "樹已滿，無法新增更多節點", "樹已滿", JOptionPane.WARNING_MESSAGE);
                return;
            }

            tree[position] = value;
            deleted[position] = false;
            tree[0] = Math.max(tree[0], position);

            inputField.setText("");
            updateDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的整數", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 找到下一個空的位置
     */
    private int getNextEmptyPosition() {
        for (int i = 1; i <= maxSize; i++) {
            if (tree[i] == null || deleted[i]) {
                return i;
            }
        }
        return maxSize + 1; // 表示已滿
    }

    /**
     * 生成隨機樹
     */
    private void generateRandomTree() {
        try {
            int count = Integer.parseInt(randomCountField.getText().trim());
            if (count <= 0 || count > maxSize) {
                JOptionPane.showMessageDialog(this, "請輸入1到" + maxSize + "之間的數字", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 清空現有樹
            clearTree();

            // 生成不重複的隨機數字
            Set<Integer> numbers = new HashSet<>();
            Random random = new Random();

            while (numbers.size() < count) {
                numbers.add(random.nextInt(200) + 1); // 1-200的隨機數
            }

            // 將數字依序放入陣列
            int index = 1;
            for (int num : numbers) {
                tree[index] = num;
                deleted[index] = false;
                index++;
                if (index > maxSize) break;
            }

            tree[0] = Math.min(count, maxSize); // 設定節點數量
            updateDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的數字", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 找到指定座標的節點索引
     */
    private int findNodeAt(int x, int y) {
        // 需要考慮縮放和偏移
        x = (int) ((x - treePanel.offsetX) / treePanel.scale);
        y = (int) ((y - treePanel.offsetY) / treePanel.scale);

        for (int i = 1; i <= tree[0]; i++) {
            if (tree[i] != null && !deleted[i]) {
                Point pos = getNodePosition(i);
                double distance = Math.sqrt(Math.pow(x - pos.x, 2) + Math.pow(y - pos.y, 2));
                if (distance <= NODE_RADIUS) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 計算節點在畫布上的位置
     */
    private Point getNodePosition(int index) {
        if (index < 1) return new Point(0, 0);

        // 計算節點所在的層級 (從0開始)
        int level = (int) (Math.log(index) / Math.log(2));

        // 計算該層級的起始索引
        int levelStart = (int) Math.pow(2, level);

        // 計算節點在該層級中的位置 (從0開始)
        int positionInLevel = index - levelStart;

        // 計算該層級的總節點數
        int nodesInLevel = (int) Math.pow(2, level);

        // 計算畫布中心
        int centerX = 400; // 畫布中心X座標
        int startY = 50;   // 第一層的Y座標

        // 計算X座標：使用動態間距避免重疊
        int totalWidth = Math.max(600, nodesInLevel * MIN_NODE_SPACING);
        int spacing = totalWidth / Math.max(1, nodesInLevel);
        int x = centerX - totalWidth / 2 + positionInLevel * spacing + spacing / 2;

        // 計算Y座標
        int y = startY + level * LEVEL_HEIGHT;

        return new Point(x, y);
    }

    /**
     * 更新選中節點資訊顯示
     */
    private void updateSelectedNodeInfo() {
        if (selectedIndex == -1 || tree[selectedIndex] == null) {
            selectedNodeLabel.setText("選中節點：無");
            editField.setText("");
            editField.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
            String nodeValue = deleted[selectedIndex] ? "已刪除" : tree[selectedIndex].toString();
            selectedNodeLabel.setText("選中節點：位置[" + selectedIndex + "] = " + nodeValue);
            editField.setText(deleted[selectedIndex] ? "" : tree[selectedIndex].toString());
            editField.setEnabled(!deleted[selectedIndex]);
            editButton.setEnabled(!deleted[selectedIndex]);
            deleteButton.setEnabled(!deleted[selectedIndex]);
        }
    }

    /**
     * 修改選中節點的值
     */
    private void editSelectedNode(String newValueStr) {
        if (selectedIndex == -1 || deleted[selectedIndex]) return;

        try {
            int newValue = Integer.parseInt(newValueStr.trim());
            tree[selectedIndex] = newValue;
            updateDisplay();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "請輸入有效的整數", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 刪除選中節點（包含所有子節點）
     */
    private void deleteSelectedNode() {
        if (selectedIndex == -1 || deleted[selectedIndex]) return;

        // 確認刪除操作
        int result = JOptionPane.showConfirmDialog(
                this,
                "確定要刪除節點 [" + selectedIndex + ":" + tree[selectedIndex] + "] 及其所有子節點嗎？",
                "確認刪除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            // 遞迴刪除節點及其所有子節點
            deleteNodeAndChildren(selectedIndex);
            updateSelectedNodeInfo();
            updateDisplay();
        }
    }

    /**
     * 遞迴刪除節點及其所有子節點
     */
    private void deleteNodeAndChildren(int index) {
        if (index > maxSize || tree[index] == null) return;

        // 先遞迴刪除左右子節點
        int leftChild = index * 2;
        int rightChild = index * 2 + 1;

        if (leftChild <= maxSize) {
            deleteNodeAndChildren(leftChild);
        }

        if (rightChild <= maxSize) {
            deleteNodeAndChildren(rightChild);
        }

        // 最後刪除當前節點
        deleted[index] = true;
    }

    /**
     * 清空樹
     */
    private void clearTree() {
        for (int i = 0; i <= maxSize; i++) {
            tree[i] = null;
            if (i < deleted.length) {
                deleted[i] = false;
            }
        }
        tree[0] = 0;
        selectedIndex = -1;
        updateSelectedNodeInfo();
        updateDisplay();
    }

    /**
     * 顯示陣列內容
     */
    private void showArrayContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("陣列內容：\n");
        sb.append("[0] = ").append(tree[0]).append(" (節點數量)\n");
        sb.append("========================================\n");

        int activeNodes = 0;
        int deletedNodes = 0;

        for (int i = 1; i <= Math.min(tree[0], 50); i++) { // 最多顯示50個節點
            sb.append(String.format("[%2d] = ", i));
            if (tree[i] == null) {
                sb.append("null");
            } else if (deleted[i]) {
                sb.append(String.format("%3d (已刪除)", tree[i]));
                deletedNodes++;
            } else {
                sb.append(String.format("%3d", tree[i]));
                activeNodes++;
            }

            // 顯示父子關係
            if (i > 1) {
                int parent = i / 2;
                sb.append("  [父:").append(parent).append("]");
            }
            if (i * 2 <= tree[0]) {
                sb.append("  [左:").append(i * 2).append("]");
            }
            if (i * 2 + 1 <= tree[0]) {
                sb.append("  [右:").append(i * 2 + 1).append("]");
            }

            sb.append("\n");
        }

        if (tree[0] > 50) {
            sb.append("... (還有更多節點)\n");
        }

        sb.append("========================================\n");
        sb.append("統計：活躍節點 ").append(activeNodes).append(" 個，已刪除節點 ").append(deletedNodes).append(" 個\n");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 500));

        JOptionPane.showMessageDialog(this, scrollPane, "陣列內容詳情", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 置中顯示樹
     */
    private void centerTree() {
        if (tree[0] == 0) return;

        // 計算樹的邊界
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (int i = 1; i <= tree[0]; i++) {
            if (tree[i] != null && !deleted[i]) {
                Point pos = getNodePosition(i);
                minX = Math.min(minX, pos.x - NODE_RADIUS);
                maxX = Math.max(maxX, pos.x + NODE_RADIUS);
                minY = Math.min(minY, pos.y - NODE_RADIUS);
                maxY = Math.max(maxY, pos.y + NODE_RADIUS);
            }
        }

        // 計算置中所需的偏移
        int treeWidth = maxX - minX;
        int treeHeight = maxY - minY;
        int panelWidth = treePanel.getWidth();
        int panelHeight = treePanel.getHeight();

        treePanel.offsetX = (panelWidth - treeWidth) / 2 - minX;
        treePanel.offsetY = (panelHeight - treeHeight) / 2 - minY;

        treePanel.repaint();
    }

    /**
     * 執行指定的遍歷方式
     */
    private void performTraversal(String type) {
        if (tree[0] == 0) {
            traversalResult.setText("樹為空");
            return;
        }

        List<String> result = new ArrayList<>();

        switch (type) {
            case "前序":
                preOrderTraversal(1, result);
                traversalResult.setText("前序遍歷 (VLR):\n" + String.join(" -> ", result));
                break;
            case "中序":
                inOrderTraversal(1, result);
                traversalResult.setText("中序遍歷 (LVR):\n" + String.join(" -> ", result));
                break;
            case "後序":
                postOrderTraversal(1, result);
                traversalResult.setText("後序遍歷 (LRV):\n" + String.join(" -> ", result));
                break;
            case "層序":
                levelOrderTraversal(result);
                traversalResult.setText("層序遍歷:\n" + String.join(" -> ", result));
                break;
        }
    }

    /**
     * 前序遍歷 (根->左->右)
     */
    private void preOrderTraversal(int index, List<String> result) {
        if (index > maxSize || tree[index] == null) return;

        // 訪問根節點
        result.add(getNodeDisplayText(index));
        // 遍歷左子樹
        preOrderTraversal(index * 2, result);
        // 遍歷右子樹
        preOrderTraversal(index * 2 + 1, result);
    }

    /**
     * 中序遍歷 (左->根->右)
     */
    private void inOrderTraversal(int index, List<String> result) {
        if (index > maxSize || tree[index] == null) return;

        // 遍歷左子樹
        inOrderTraversal(index * 2, result);
        // 訪問根節點
        result.add(getNodeDisplayText(index));
        // 遍歷右子樹
        inOrderTraversal(index * 2 + 1, result);
    }

    /**
     * 後序遍歷 (左->右->根)
     */
    private void postOrderTraversal(int index, List<String> result) {
        if (index > maxSize || tree[index] == null) return;

        // 遍歷左子樹
        postOrderTraversal(index * 2, result);
        // 遍歷右子樹
        postOrderTraversal(index * 2 + 1, result);
        // 訪問根節點
        result.add(getNodeDisplayText(index));
    }

    /**
     * 層序遍歷 (由上到下，由左到右)
     */
    private void levelOrderTraversal(List<String> result) {
        for (int i = 1; i <= tree[0]; i++) {
            if (tree[i] != null) {
                result.add(getNodeDisplayText(i));
            }
        }
    }

    /**
     * 取得節點的顯示文字
     */
    private String getNodeDisplayText(int index) {
        if (deleted[index]) {
            return "[" + index + ":已刪除]";
        } else {
            return "[" + index + ":" + tree[index] + "]";
        }
    }

    /**
     * 更新所有顯示
     */
    private void updateDisplay() {
        sizeLabel.setText("節點數量：" + tree[0]);
        updateSelectedNodeInfo();
        treePanel.repaint();

        // 自動執行層序遍歷顯示
        performTraversal("層序");
    }

    /**
     * 繪製樹形圖
     */
    private void drawTree(Graphics2D g2d) {
        if (tree[0] == 0) {
            // 顯示空樹提示
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 24));
            String message = "樹為空 - 請新增節點";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (800 - fm.stringWidth(message)) / 2;
            int y = 300;
            g2d.drawString(message, x, y);
            return;
        }

        // 先繪製所有連線
        drawConnections(g2d);

        // 再繪製所有節點
        for (int i = 1; i <= tree[0]; i++) {
            if (tree[i] != null) {
                drawNode(g2d, i);
            }
        }
    }

    /**
     * 繪製節點間的連線
     */
    private void drawConnections(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));

        for (int i = 1; i <= tree[0]; i++) {
            if (tree[i] != null && !deleted[i]) {
                Point parentPos = getNodePosition(i);

                // 左子節點連線
                int leftChild = i * 2;
                if (leftChild <= tree[0] && tree[leftChild] != null && !deleted[leftChild]) {
                    Point leftPos = getNodePosition(leftChild);
                    g2d.drawLine(parentPos.x, parentPos.y, leftPos.x, leftPos.y);
                }

                // 右子節點連線
                int rightChild = i * 2 + 1;
                if (rightChild <= tree[0] && tree[rightChild] != null && !deleted[rightChild]) {
                    Point rightPos = getNodePosition(rightChild);
                    g2d.drawLine(parentPos.x, parentPos.y, rightPos.x, rightPos.y);
                }
            }
        }
    }

    /**
     * 繪製單個節點
     */
    private void drawNode(Graphics2D g2d, int index) {
        Point pos = getNodePosition(index);

        // 選擇顏色
        if (deleted[index]) {
            g2d.setColor(Color.LIGHT_GRAY); // 已刪除節點用灰色
        } else if (index == selectedIndex) {
            g2d.setColor(Color.YELLOW);     // 選中節點用黃色
        } else {
            g2d.setColor(Color.BLACK);      // 正常節點用黑色
        }

        // 繪製節點圓圈
        g2d.fillOval(pos.x - NODE_RADIUS, pos.y - NODE_RADIUS,
                NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 繪製節點邊框
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(pos.x - NODE_RADIUS, pos.y - NODE_RADIUS,
                NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 繪製節點文字
        if (!deleted[index] && tree[index] != null) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            String text = tree[index].toString();
            FontMetrics fm = g2d.getFontMetrics();
            int textX = pos.x - fm.stringWidth(text) / 2;
            int textY = pos.y + fm.getAscent() / 2 - 2;

            g2d.drawString(text, textX, textY);
        }

        // 繪製索引標籤（小字顯示在節點下方）
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        String indexText = String.valueOf(index);
        FontMetrics fm = g2d.getFontMetrics();
        int indexX = pos.x - fm.stringWidth(indexText) / 2;
        int indexY = pos.y + NODE_RADIUS + 15;
        g2d.drawString(indexText, indexX, indexY);
    }

    /**
     * 主程式入口
     */
    public static void main(String[] args) {
        // 設定系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 在事件調度執行緒中啟動GUI
        SwingUtilities.invokeLater(() -> {
            new BinaryTreeVisualizer().setVisible(true);
        });
    }
}