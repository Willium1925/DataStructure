package D0715;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 課表編輯器主類別
 * 提供視覺化的課程表編輯功能，支援數字輸入和拖拽操作
 * 繼承JFrame作為主視窗容器
 */
public class ScheduleEditor extends JFrame {

    // === 核心元件宣告 ===

    /**
     * 課表顯示的主要表格元件
     * 用於顯示6x6的課程表格，第一行顯示節數，第一列顯示星期
     */
    private JTable scheduleTable;

    /**
     * 表格的資料模型
     * 管理表格中的所有資料，與scheduleTable緊密結合
     * 當資料變更時，會自動通知表格進行重新渲染
     */
    private DefaultTableModel tableModel;

    /**
     * 下方課程選擇面板
     * 容納所有可選課程的標籤，提供拖拽來源
     */
    private JPanel coursePanel;

    /**
     * 課程編號與課程名稱的對應關係
     * Key: 課程編號("1", "2", "3", "4", "5")
     * Value: 課程名稱("計算機概論", "離散數學"...)
     * 這個Map在多個地方被使用:
     * 1. ScheduleCellRenderer中將編號轉換為課程名稱顯示
     * 2. ScheduleCellEditor中驗證輸入是否有效
     * 3. createCoursePanel中創建課程標籤
     */
    private Map<String, String> courseMap;

    /**
     * 課程對應的顏色陣列
     * 索引0: 空白課程的顏色
     * 索引1-5: 對應課程編號1-5的顏色
     * 這個陣列在ScheduleCellRenderer中被使用來設定背景色
     */
    private Color[] courseColors = {
            Color.LIGHT_GRAY,     // 空白課程 - 淺灰色
            new Color(255, 182, 193),  // 計算機概論 - 淺粉紅
            new Color(173, 216, 230),  // 離散數學 - 淺藍
            new Color(144, 238, 144),  // 資料結構 - 淺綠
            new Color(255, 228, 181),  // 資料庫理論 - 淺橙
            new Color(221, 160, 221),  // 上機實習 - 淺紫
    };

    /**
     * 建構子 - 初始化整個課表編輯器
     * 按照特定順序初始化各個元件，確保相依性正確
     */
    public ScheduleEditor() {
        // 1. 首先初始化課程對應關係，因為後續元件都需要使用這個資料
        initializeCourseMap();

        // 2. 建立使用者介面，包含表格、面板等視覺元件
        initializeUI();

        // 3. 最後設定拖拽功能，因為需要前面的元件都已經建立完成
        setupDragAndDrop();
    }

    /**
     * 初始化課程編號與名稱的對應關係
     * 這個方法必須在其他需要使用courseMap的方法之前呼叫
     * 建立的courseMap會被以下方法使用:
     * - createCoursePanel(): 建立課程選擇標籤
     * - ScheduleCellRenderer: 顯示課程名稱而非編號
     * - ScheduleCellEditor: 驗證輸入的編號是否有效
     */
    private void initializeCourseMap() {
        courseMap = new HashMap<>();
        courseMap.put("1", "計算機概論");
        courseMap.put("2", "離散數學");
        courseMap.put("3", "資料結構");
        courseMap.put("4", "資料庫理論");
        courseMap.put("5", "上機實習");
    }

    /**
     * 初始化使用者介面
     * 建立主視窗的所有元件並進行佈局
     * 使用BorderLayout來安排元件位置
     */
    private void initializeUI() {
        // 設定視窗基本屬性
        setTitle("課表編輯器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 建立課表表格 - 這是主要的編輯區域
        // 必須先建立表格，因為後續的拖拽設定需要參考表格
        createScheduleTable();

        // 建立課程選擇面板 - 提供拖拽來源
        // 依賴courseMap，所以必須在initializeCourseMap()之後執行
        createCoursePanel();

        // 建立保存按鈕 - 提供PNG匯出功能
        JButton saveButton = new JButton("保存PNG");
        // 為按鈕綁定點擊事件，點擊時執行saveScheduleAsPNG()方法
        saveButton.addActionListener(e -> saveScheduleAsPNG());

        // 使用BorderLayout進行佈局:
        // CENTER: 主要的課表區域，會自動調整大小
        add(new JScrollPane(scheduleTable), BorderLayout.CENTER);
        // SOUTH: 底部的課程選擇面板，固定高度
        add(coursePanel, BorderLayout.SOUTH);
        // NORTH: 頂部的保存按鈕，固定高度
        add(saveButton, BorderLayout.NORTH);

        // 設定視窗大小和置中顯示
        setSize(800, 600);
        setLocationRelativeTo(null); // 置中顯示
    }

    /**
     * 建立課程表格
     * 這是整個程式的核心元件，負責顯示和編輯課程資料
     */
    private void createScheduleTable() {
        // 定義表格的欄位標題
        // 第一欄是"時間"，用來顯示節數
        // 後面五欄是"一"到"五"，代表星期一到五
        String[] columns = {"時間", "一", "二", "三", "四", "五"};

        // 建立6x6的資料陣列
        // 第一維度是列(rows)，第二維度是欄(columns)
        Object[][] data = new Object[6][6];

        // 初始化表格資料
        for (int i = 0; i < 6; i++) {
            // 第一欄(索引0)設為節數"1"到"6"
            data[i][0] = String.valueOf(i + 1);
            // 其他欄位初始化為空字串，等待使用者輸入
            for (int j = 1; j < 6; j++) {
                data[i][j] = "";
            }
        }

        // 建立自定義的表格模型
        // 覆寫isCellEditable方法來控制哪些儲存格可以編輯
        tableModel = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 只有第一欄(時間欄)不可編輯，其他欄位都可以編輯
                // 這樣確保節數不會被意外修改
                return column != 0;
            }
        };

        // 使用自定義模型建立表格
        scheduleTable = new JTable(tableModel);

        // 設定表格視覺屬性
        scheduleTable.setRowHeight(60);  // 設定列高，讓課程名稱有足夠空間顯示
        scheduleTable.getColumnModel().getColumn(0).setMaxWidth(60);  // 限制時間欄寬度

        // 為可編輯的欄位(第2到第6欄)設定自定義的渲染器和編輯器
        for (int i = 1; i < scheduleTable.getColumnCount(); i++) {
            // 設定自定義渲染器 - 負責顯示課程名稱和背景色
            scheduleTable.getColumnModel().getColumn(i).setCellRenderer(new ScheduleCellRenderer());
            // 設定自定義編輯器 - 負責處理使用者輸入和驗證
            scheduleTable.getColumnModel().getColumn(i).setCellEditor(new ScheduleCellEditor());
        }

        // 為第一欄(時間欄)設定專用的渲染器
        // 這個渲染器讓時間欄看起來像表頭，與其他欄位有視覺區別
        scheduleTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);  // 文字置中
                setBackground(Color.LIGHT_GRAY);  // 灰色背景，類似表頭
                setFont(getFont().deriveFont(Font.BOLD, 14f));  // 粗體字
                return this;
            }
        });

        // 設定表格外觀
        scheduleTable.setGridColor(Color.BLACK);  // 網格線顏色
        scheduleTable.setShowGrid(true);  // 顯示網格線
    }

    /**
     * 建立課程選擇面板
     * 這個面板顯示所有可選課程，使用者可以從這裡拖拽課程到課表上
     * 依賴courseMap和courseColors，所以必須在這兩個資料結構初始化後執行
     */
    private void createCoursePanel() {
        // 使用FlowLayout，讓課程標籤從左到右排列
        coursePanel = new JPanel(new FlowLayout());
        coursePanel.setBorder(BorderFactory.createTitledBorder("可選課程"));

        // 遍歷所有課程，為每個課程建立一個可拖拽的標籤
        for (Map.Entry<String, String> entry : courseMap.entrySet()) {
            String courseCode = entry.getKey();    // 課程編號 "1", "2", "3"...
            String courseName = entry.getValue();  // 課程名稱 "計算機概論", "離散數學"...

            // 建立課程標籤，顯示格式為 "1. 計算機概論"
            JLabel courseLabel = new JLabel(courseCode + ". " + courseName);
            courseLabel.setOpaque(true);  // 允許設定背景色

            // 設定背景色，使用courseColors陣列中對應的顏色
            // 將字串轉換為整數作為陣列索引
            courseLabel.setBackground(courseColors[Integer.parseInt(courseCode)]);

            // 設定視覺效果
            courseLabel.setBorder(BorderFactory.createRaisedBevelBorder());  // 立體邊框
            courseLabel.setPreferredSize(new Dimension(120, 40));  // 固定大小
            courseLabel.setHorizontalAlignment(SwingConstants.CENTER);  // 文字置中

            // === 設定拖拽功能 ===
            // 為每個課程標籤設定拖拽處理器，傳入課程編號
            courseLabel.setTransferHandler(new CourseTransferHandler(courseCode));

            // 添加滑鼠事件監聽器來啟動拖拽
            courseLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    // 當滑鼠按下時，啟動拖拽操作
                    JComponent comp = (JComponent) e.getSource();
                    TransferHandler handler = comp.getTransferHandler();
                    // 以COPY模式開始拖拽，傳遞課程編號資料
                    handler.exportAsDrag(comp, e, TransferHandler.COPY);
                }
            });

            // 將課程標籤添加到面板中
            coursePanel.add(courseLabel);
        }
    }

    /**
     * 設定拖拽功能
     * 為scheduleTable設定拖拽接收器，使其能夠接收從coursePanel拖拽過來的課程
     * 這個方法必須在scheduleTable建立完成後呼叫
     */
    private void setupDragAndDrop() {
        // 設定表格的拖拽處理器，用於接收拖拽的課程資料
        scheduleTable.setTransferHandler(new ScheduleTransferHandler());

        // 設定拖拽模式為ON，表示可以在表格上的任意位置放置
        scheduleTable.setDropMode(DropMode.ON);
    }

    /**
     * 將課表儲存為PNG檔案
     * 這個方法會將整個表格渲染為圖片並儲存
     */
    private void saveScheduleAsPNG() {
        try {
            // 建立與表格相同大小的BufferedImage
            // TYPE_INT_RGB表示使用RGB顏色模式
            BufferedImage image = new BufferedImage(
                    scheduleTable.getWidth(),
                    scheduleTable.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            // 取得圖片的Graphics2D物件用於繪製
            Graphics2D g2d = image.createGraphics();

            // 先填充白色背景
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

            // 將表格繪製到圖片上
            // 這會觸發所有的渲染器，包括ScheduleCellRenderer
            scheduleTable.paint(g2d);

            // 釋放Graphics2D資源
            g2d.dispose();

            // 顯示檔案儲存對話框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("schedule.png"));  // 預設檔名

            // 如果使用者選擇了儲存位置
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // 將圖片寫入檔案
                ImageIO.write(image, "png", file);
                // 顯示成功訊息
                JOptionPane.showMessageDialog(this, "課表已保存為: " + file.getName());
            }
        } catch (IOException e) {
            // 如果儲存失敗，顯示錯誤訊息
            JOptionPane.showMessageDialog(this, "保存失敗: " + e.getMessage());
        }
    }

    /**
     * 自定義單元格渲染器
     * 負責決定每個課程儲存格的顯示方式
     * 這個類別與courseMap和courseColors密切相關
     */
    class ScheduleCellRenderer extends DefaultTableCellRenderer {

        /**
         * 渲染單元格的方法
         * 每當表格需要重新繪製時，就會呼叫這個方法
         *
         * @param table 表格元件 (就是我們的scheduleTable)
         * @param value 儲存格的值 (可能是課程編號或空字串)
         * @param isSelected 是否被選中
         * @param hasFocus 是否有焦點
         * @param row 列索引
         * @param column 欄索引
         * @return 渲染後的元件
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            // 呼叫父類別的方法進行基本設定
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // 取得儲存格的值，如果為null則設為空字串
            String cellValue = value != null ? value.toString() : "";
            Color bgColor = Color.WHITE;  // 預設背景色為白色

            // 根據儲存格的值決定顯示內容和背景色
            if (!cellValue.isEmpty()) {
                // 如果儲存格有值，檢查是否為有效的課程編號
                if (courseMap.containsKey(cellValue)) {
                    // 如果是有效編號，顯示課程名稱而非編號
                    setText(courseMap.get(cellValue));
                    // 設定對應的背景色
                    bgColor = courseColors[Integer.parseInt(cellValue)];
                } else {
                    // 如果不是有效編號，直接顯示原始值
                    setText(cellValue);
                }
            } else {
                // 如果儲存格為空，顯示空字串
                setText("");
            }

            // 應用背景色和其他視覺設定
            setBackground(bgColor);
            setHorizontalAlignment(CENTER);  // 文字置中
            setBorder(BorderFactory.createLineBorder(Color.BLACK));  // 黑色邊框

            return this;
        }
    }

    /**
     * 自定義單元格編輯器
     * 負責處理使用者在課表格中的輸入
     * 繼承DefaultCellEditor並使用JTextField作為編輯元件
     */
    class ScheduleCellEditor extends DefaultCellEditor {
        private JTextField textField;  // 編輯時使用的文字輸入框

        /**
         * 建構子
         * 建立編輯器並設定輸入限制
         */
        public ScheduleCellEditor() {
            // 使用JTextField建立編輯器
            super(new JTextField());
            textField = (JTextField) getComponent();
            textField.setHorizontalAlignment(JTextField.CENTER);  // 文字置中

            // 添加鍵盤事件監聽器，限制輸入內容
            textField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();

                    // 只允許數字和退格鍵
                    if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
                        e.consume();  // 忽略非數字字元
                    }

                    // 限制只能輸入一個字元
                    if (Character.isDigit(c)) {
                        String current = textField.getText();
                        if (current.length() >= 1) {
                            e.consume();  // 如果已經有一個字元，忽略新的輸入
                        }
                    }
                }
            });
        }

        /**
         * 停止編輯時的驗證
         * 當使用者按下Enter或點擊其他地方時會呼叫這個方法
         *
         * @return true表示允許停止編輯，false表示編輯繼續
         */
        @Override
        public boolean stopCellEditing() {
            String value = textField.getText();

            // 如果輸入不為空，但不是有效的課程編號，則拒絕停止編輯
            if (!value.isEmpty() && !courseMap.containsKey(value)) {
                JOptionPane.showMessageDialog(scheduleTable, "請輸入1-5之間的數字");
                return false;  // 繼續編輯
            }

            // 如果輸入有效，呼叫父類別方法完成編輯
            // 這會觸發表格模型的更新，進而觸發渲染器重新繪製
            return super.stopCellEditing();
        }
    }

    /**
     * 課程拖拽處理器
     * 負責處理從課程面板拖拽課程的功能
     * 這個類別會被coursePanel中的每個課程標籤使用
     */
    class CourseTransferHandler extends TransferHandler {
        private String courseCode;  // 此處理器對應的課程編號

        /**
         * 建構子
         * @param courseCode 課程編號，當拖拽開始時會傳遞這個資料
         */
        public CourseTransferHandler(String courseCode) {
            this.courseCode = courseCode;
        }

        /**
         * 建立可傳輸的資料
         * 當拖拽開始時會呼叫這個方法
         *
         * @param c 拖拽的來源元件 (課程標籤)
         * @return 要傳輸的資料 (課程編號)
         */
        @Override
        protected Transferable createTransferable(JComponent c) {
            // 建立包含課程編號的字串傳輸資料
            // 這個資料會被ScheduleTransferHandler接收
            return new StringSelection(courseCode);
        }

        /**
         * 定義支援的拖拽動作
         * @param c 拖拽的來源元件
         * @return 支援的動作類型
         */
        @Override
        public int getSourceActions(JComponent c) {
            // 返回COPY，表示這是複製操作，不會移除原始的課程標籤
            return COPY;
        }
    }

    /**
     * 課表拖拽處理器
     * 負責處理將課程拖拽到課表上的功能
     * 這個類別會被scheduleTable使用
     */
    class ScheduleTransferHandler extends TransferHandler {

        /**
         * 檢查是否可以接受拖拽
         *
         * @param support 拖拽支援物件，包含拖拽的相關資訊
         * @return true表示可以接受，false表示拒絕
         */
        @Override
        public boolean canImport(TransferSupport support) {
            // 必須同時滿足兩個條件：
            // 1. 是拖拽操作（而不是剪貼簿操作）
            // 2. 資料類型是字串（課程編號）
            return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        /**
         * 執行拖拽匯入
         * 當使用者將課程拖拽到表格上並釋放滑鼠時會呼叫這個方法
         *
         * @param support 拖拽支援物件
         * @return true表示匯入成功，false表示失敗
         */
        @Override
        public boolean importData(TransferSupport support) {
            // 先檢查是否可以匯入
            if (!canImport(support)) {
                return false;
            }

            try {
                // 從傳輸資料中取得課程編號
                // 這個資料來自CourseTransferHandler.createTransferable()
                String courseCode = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // 取得拖拽的目標位置
                JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();

                int row = dl.getRow();     // 目標列
                int col = dl.getColumn();  // 目標欄

                // 不允許拖拽到第一欄（時間欄）
                if (col == 0) return false;

                // 將課程編號設定到目標儲存格
                // 這會觸發表格模型的更新，進而觸發ScheduleCellRenderer重新渲染
                tableModel.setValueAt(courseCode, row, col);

                return true;  // 匯入成功
            } catch (Exception e) {
                // 如果發生任何異常，返回false表示匯入失敗
                return false;
            }
        }
    }

    /**
     * 程式進入點
     * 使用SwingUtilities.invokeLater確保在事件分派執行緒中建立GUI
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 設定系統外觀，讓程式看起來更像原生應用程式
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 建立並顯示課表編輯器
            new ScheduleEditor().setVisible(true);
        });
    }
}