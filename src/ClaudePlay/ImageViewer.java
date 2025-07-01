package ClaudePlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageViewer extends JFrame {
    private JLabel imageLabel;
    private JButton prevButton;
    private JButton selectDirButton;
    private JButton nextButton;

    private List<File> imageFiles;
    private int currentIndex = -1;
    private File currentDirectory;

    // 支援的圖片格式
    private static final String[] SUPPORTED_FORMATS = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};

    public ImageViewer() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("圖片瀏覽器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 創建圖片顯示區域
        imageLabel = new JLabel("請選擇圖片目錄", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(800, 600));
        imageLabel.setBorder(BorderFactory.createLoweredBevelBorder());

        // 將圖片標籤放在滾動面板中
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        add(scrollPane, BorderLayout.CENTER);

        // 創建按鈕面板
        JPanel buttonPanel = new JPanel(new FlowLayout());

        prevButton = new JButton("上一張");
        selectDirButton = new JButton("選擇目錄");
        nextButton = new JButton("下一張");

        // 初始狀態下禁用上一張和下一張按鈕
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);

        // 添加按鈕事件監聽器
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousImage();
            }
        });

        selectDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectDirectory();
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextImage();
            }
        });

        buttonPanel.add(prevButton);
        buttonPanel.add(selectDirButton);
        buttonPanel.add(nextButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void selectDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("選擇圖片目錄");

        if (currentDirectory != null) {
            fileChooser.setCurrentDirectory(currentDirectory);
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentDirectory = fileChooser.getSelectedFile();
            loadImagesFromDirectory(currentDirectory);
        }
    }

    private void loadImagesFromDirectory(File directory) {
        imageFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (isImageFile(file)) {
                    imageFiles.add(file);
                }
            }
        }

        if (!imageFiles.isEmpty()) {
            // 按檔名排序
            imageFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            currentIndex = 0;
            displayImage(imageFiles.get(currentIndex));
            updateButtonStates();
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText("目錄中沒有找到圖片檔案");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            currentIndex = -1;
        }
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;

        String fileName = file.getName().toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (fileName.endsWith(format)) {
                return true;
            }
        }
        return false;
    }

    private void displayImage(File imageFile) {
        try {
            ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
            Image image = icon.getImage();

            // 縮放到原始大小的二分之一
            int originalWidth = icon.getIconWidth();
            int originalHeight = icon.getIconHeight();
            int scaledWidth = originalWidth / 2;
            int scaledHeight = originalHeight / 2;

            if (scaledWidth > 0 && scaledHeight > 0) {
                Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                imageLabel.setIcon(scaledIcon);
                imageLabel.setText("");
            } else {
                imageLabel.setIcon(null);
                imageLabel.setText("無法載入圖片: " + imageFile.getName());
            }

            // 更新標題顯示當前圖片資訊
            setTitle("圖片瀏覽器 - " + imageFile.getName() +
                    " (" + (currentIndex + 1) + "/" + imageFiles.size() + ")");

        } catch (Exception e) {
            imageLabel.setIcon(null);
            imageLabel.setText("載入圖片時發生錯誤: " + imageFile.getName());
            e.printStackTrace();
        }
    }

    private void showPreviousImage() {
        if (imageFiles != null && !imageFiles.isEmpty() && currentIndex > 0) {
            currentIndex--;
            displayImage(imageFiles.get(currentIndex));
            updateButtonStates();
        }
    }

    private void showNextImage() {
        if (imageFiles != null && !imageFiles.isEmpty() && currentIndex < imageFiles.size() - 1) {
            currentIndex++;
            displayImage(imageFiles.get(currentIndex));
            updateButtonStates();
        }
    }

    private void updateButtonStates() {
        if (imageFiles == null || imageFiles.isEmpty()) {
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else {
            prevButton.setEnabled(currentIndex > 0);
            nextButton.setEnabled(currentIndex < imageFiles.size() - 1);
        }
    }

    public static void main(String[] args) {
        // 設定系統外觀
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ImageViewer().setVisible(true);
            }
        });
    }
}