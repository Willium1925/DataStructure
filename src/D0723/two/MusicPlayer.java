package D0723.two;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 音樂播放器主類別
 * 使用 Java Swing GUI 和內建 Sound API
 * 注意：此版本主要支援 WAV 格式，MP3 需要額外的解碼器
 *
 * 功能包含：
 * 1. 播放清單管理
 * 2. 音樂檔案播放控制（主要支援 WAV）
 * 3. 拖曳排序
 * 4. 隨機播放和循環播放
 */
public class MusicPlayer extends JFrame {

    // GUI 元件
    private DefaultListModel<MusicFile> playlistModel;  // 播放清單資料模型
    private JList<MusicFile> playlistJList;             // 播放清單視圖
    private JLabel nowPlayingLabel;                     // 正在播放顯示標籤
    private JLabel timeLabel;                           // 時間顯示標籤
    private JProgressBar progressBar;                   // 播放進度條

    // 控制按鈕
    private JButton playPauseButton;
    private JButton stopButton;
    private JButton previousButton;
    private JButton nextButton;
    private JButton shuffleButton;
    private JButton repeatButton;

    // 音樂播放相關
    private Clip audioClip;                             // 音訊剪輯
    private AudioInputStream audioInputStream;          // 音訊輸入流
    private List<MusicFile> originalPlaylist;           // 原始播放清單
    private int currentIndex = -1;                      // 目前播放的音樂索引
    private boolean isPlaying = false;                  // 播放狀態
    private boolean isPaused = false;                   // 暫停狀態
    private boolean isShuffleMode = false;              // 隨機播放模式
    private boolean isRepeatMode = false;               // 循環播放模式

    // 音樂播放監控
    private Timer progressTimer;                        // 進度更新計時器
    private long clipTimePosition = 0;                  // 播放位置

    // 支援的音樂格式（主要是 WAV，其他格式需要額外解碼器）
    private static final String[] SUPPORTED_FORMATS = {".wav", ".au", ".aiff"};

    /**
     * 音樂檔案封裝類別
     * 用於在播放清單中顯示檔案名稱
     */
    private static class MusicFile {
        private File file;
        private String displayName;

        public MusicFile(File file) {
            this.file = file;
            // 移除副檔名作為顯示名稱
            String name = file.getName();
            int lastDot = name.lastIndexOf('.');
            this.displayName = lastDot > 0 ? name.substring(0, lastDot) : name;
        }

        public File getFile() { return file; }

        @Override
        public String toString() { return displayName; }
    }

    /**
     * 建構子 - 初始化音樂播放器
     */
    public MusicPlayer() {
        initializePlayer();
        initializeGUI();
        setupEventHandlers();
    }

    /**
     * 初始化音樂播放器
     */
    private void initializePlayer() {
        try {
            originalPlaylist = new ArrayList<>();

            // 初始化進度更新計時器
            progressTimer = new Timer(100, e -> updateProgress());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "無法初始化音樂播放器: " + e.getMessage(),
                    "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 初始化圖形使用者介面
     */
    private void initializeGUI() {
        setTitle("音樂播放器 (支援 WAV/AU/AIFF 格式)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 建立主要面板
        createPlaylistPanel();    // 左側播放清單
        createNowPlayingPanel();  // 右側正在播放
        createControlPanel();     // 底部控制面板

        // 設定視窗大小和位置
        setSize(800, 600);
        setLocationRelativeTo(null);

        // 初始化按鈕狀態
        updateButtonStates();
    }

    /**
     * 建立左側播放清單面板
     */
    private void createPlaylistPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 5));
        leftPanel.setPreferredSize(new Dimension(350, 0));

        // 播放清單標題和按鈕
        JPanel playlistHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        playlistHeader.add(new JLabel("播放清單"));

        JButton addFolderButton = new JButton("選擇資料夾");
        JButton clearButton = new JButton("清空清單");
        playlistHeader.add(addFolderButton);
        playlistHeader.add(clearButton);

        // 播放清單
        playlistModel = new DefaultListModel<>();
        playlistJList = new JList<>(playlistModel);
        playlistJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistJList.setDragEnabled(true);  // 啟用拖曳功能
        playlistJList.setDropMode(DropMode.INSERT);

        // 設定拖曳處理器
        playlistJList.setTransferHandler(new PlaylistTransferHandler());

        JScrollPane scrollPane = new JScrollPane(playlistJList);
        scrollPane.setPreferredSize(new Dimension(330, 400));

        leftPanel.add(playlistHeader, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // 事件處理
        addFolderButton.addActionListener(e -> selectMusicFolder());
        clearButton.addActionListener(e -> clearPlaylist());

        // 雙擊播放
        playlistJList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = playlistJList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        playMusic(index);
                    }
                }
            }
        });

        add(leftPanel, BorderLayout.WEST);
    }

    /**
     * 建立右側正在播放面板
     */
    private void createNowPlayingPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(10, 5, 10, 10));

        // 正在播放資訊
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("正在播放"));

        nowPlayingLabel = new JLabel("請選擇音樂檔案", SwingConstants.CENTER);
        nowPlayingLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));

        timeLabel = new JLabel("00:00 / 00:00", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");

        infoPanel.add(nowPlayingLabel, BorderLayout.NORTH);
        infoPanel.add(progressBar, BorderLayout.CENTER);
        infoPanel.add(timeLabel, BorderLayout.SOUTH);

        // 格式說明
        JLabel formatLabel = new JLabel("<html><center>支援格式: WAV, AU, AIFF<br>如需 MP3 支援，請使用 BasicPlayer 版本</center></html>", SwingConstants.CENTER);
        formatLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 10));
        formatLabel.setForeground(Color.GRAY);
        infoPanel.add(formatLabel, BorderLayout.SOUTH);

        rightPanel.add(infoPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * 建立底部控制面板
     */
    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        // 建立控制按鈕
        previousButton = new JButton("上一首");
        playPauseButton = new JButton("播放");
        stopButton = new JButton("停止");
        nextButton = new JButton("下一首");
        shuffleButton = new JButton("隨機播放");
        repeatButton = new JButton("循環播放");

        // 音量控制
        JLabel volumeLabel = new JLabel("音量:");
        JSlider volumeSlider = new JSlider(0, 100, 70);
        volumeSlider.setPreferredSize(new Dimension(100, 25));

        // 音量控制事件
        volumeSlider.addChangeListener(e -> {
            if (audioClip != null && audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                float gain = (float) (Math.log(volumeSlider.getValue() / 100.0) / Math.log(10.0) * 20.0);
                gainControl.setValue(gain);
            }
        });

        // 將按鈕加入面板
        controlPanel.add(previousButton);
        controlPanel.add(playPauseButton);
        controlPanel.add(stopButton);
        controlPanel.add(nextButton);
        controlPanel.add(new JLabel("  "));  // 間隔
        controlPanel.add(shuffleButton);
        controlPanel.add(repeatButton);
        controlPanel.add(new JLabel("  "));  // 間隔
        controlPanel.add(volumeLabel);
        controlPanel.add(volumeSlider);

        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * 設定事件處理器
     */
    private void setupEventHandlers() {
        previousButton.addActionListener(e -> playPrevious());
        playPauseButton.addActionListener(e -> togglePlayPause());
        stopButton.addActionListener(e -> stopMusic());
        nextButton.addActionListener(e -> playNext());
        shuffleButton.addActionListener(e -> toggleShuffle());
        repeatButton.addActionListener(e -> toggleRepeat());
    }

    /**
     * 選擇音樂資料夾
     */
    private void selectMusicFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("選擇音樂資料夾");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            loadMusicFromFolder(selectedFolder);
        }
    }

    /**
     * 從資料夾載入音樂檔案
     */
    private void loadMusicFromFolder(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(this, "無效的資料夾路徑", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 清空現有播放清單
        playlistModel.clear();
        originalPlaylist.clear();

        // 遞迴搜尋音樂檔案
        List<File> musicFiles = new ArrayList<>();
        findMusicFiles(folder, musicFiles);

        // 排序檔案名稱
        musicFiles.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        // 加入播放清單
        for (File file : musicFiles) {
            MusicFile musicFile = new MusicFile(file);
            playlistModel.addElement(musicFile);
            originalPlaylist.add(musicFile);
        }

        // 重設播放狀態
        currentIndex = -1;
        updateButtonStates();

        if (musicFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "在選擇的資料夾中沒有找到支援的音樂檔案 (WAV, AU, AIFF)",
                    "未找到檔案", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "已載入 " + musicFiles.size() + " 首音樂",
                    "載入完成", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 遞迴搜尋音樂檔案
     */
    private void findMusicFiles(File directory, List<File> musicFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 遞迴搜尋子資料夾
                    findMusicFiles(file, musicFiles);
                } else if (isSupportedFormat(file)) {
                    musicFiles.add(file);
                }
            }
        }
    }

    /**
     * 檢查檔案是否為支援的音樂格式
     */
    private boolean isSupportedFormat(File file) {
        String fileName = file.getName().toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (fileName.endsWith(format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 播放指定索引的音樂
     */
    private void playMusic(int index) {
        if (index < 0 || index >= playlistModel.getSize()) {
            return;
        }

        try {
            // 停止目前播放
            if (audioClip != null && audioClip.isRunning()) {
                audioClip.stop();
                audioClip.close();
            }
            if (audioInputStream != null) {
                audioInputStream.close();
            }

            // 載入新音樂檔案
            MusicFile musicFile = playlistModel.getElementAt(index);
            File file = musicFile.getFile();

            audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);

            // 設定播放結束監聽器
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    SwingUtilities.invokeLater(() -> {
                        if (audioClip != null && audioClip.getFramePosition() >= audioClip.getFrameLength()) {
                            // 播放結束，自動播放下一首
                            onSongFinished();
                        }
                    });
                }
            });

            // 開始播放
            audioClip.start();

            currentIndex = index;
            isPlaying = true;
            isPaused = false;

            // 更新顯示
            nowPlayingLabel.setText(musicFile.toString());
            playlistJList.setSelectedIndex(index);
            updateButtonStates();

            // 啟動進度更新計時器
            progressTimer.start();

        } catch (UnsupportedAudioFileException e) {
            JOptionPane.showMessageDialog(this,
                    "不支援的音訊格式: " + e.getMessage() + "\n請使用 WAV, AU 或 AIFF 格式",
                    "格式錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "無法播放音樂: " + e.getMessage(),
                    "播放錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 切換播放/暫停
     */
    private void togglePlayPause() {
        if (audioClip == null) {
            if (currentIndex >= 0) {
                playMusic(currentIndex);
            }
            return;
        }

        if (isPlaying && !isPaused) {
            // 暫停
            clipTimePosition = audioClip.getFramePosition();
            audioClip.stop();
            isPaused = true;
            progressTimer.stop();
        } else if (isPaused) {
            // 恢復播放
            audioClip.setFramePosition((int) clipTimePosition);
            audioClip.start();
            isPaused = false;
            progressTimer.start();
        } else if (!isPlaying && currentIndex >= 0) {
            // 重新開始播放
            playMusic(currentIndex);
        }

        updateButtonStates();
    }

    /**
     * 停止播放
     */
    private void stopMusic() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.setFramePosition(0);
            clipTimePosition = 0;
        }

        isPlaying = false;
        isPaused = false;
        progressTimer.stop();

        progressBar.setValue(0);
        progressBar.setString("0%");
        timeLabel.setText("00:00 / 00:00");
        updateButtonStates();
    }

    /**
     * 播放上一首
     */
    private void playPrevious() {
        if (playlistModel.getSize() == 0) return;

        int newIndex;
        if (isShuffleMode) {
            // 隨機模式：隨機選擇
            newIndex = (int) (Math.random() * playlistModel.getSize());
        } else {
            // 一般模式：上一首
            newIndex = currentIndex - 1;
            if (newIndex < 0) {
                newIndex = isRepeatMode ? playlistModel.getSize() - 1 : 0;
            }
        }
        playMusic(newIndex);
    }

    /**
     * 播放下一首
     */
    private void playNext() {
        if (playlistModel.getSize() == 0) return;

        int newIndex;
        if (isShuffleMode) {
            // 隨機模式：隨機選擇
            newIndex = (int) (Math.random() * playlistModel.getSize());
        } else {
            // 一般模式：下一首
            newIndex = currentIndex + 1;
            if (newIndex >= playlistModel.getSize()) {
                newIndex = isRepeatMode ? 0 : playlistModel.getSize() - 1;
            }
        }
        playMusic(newIndex);
    }

    /**
     * 歌曲播放結束處理
     */
    private void onSongFinished() {
        isPlaying = false;
        if (isRepeatMode || currentIndex < playlistModel.getSize() - 1) {
            playNext();
        } else {
            stopMusic();
        }
    }

    /**
     * 切換隨機播放模式
     */
    private void toggleShuffle() {
        isShuffleMode = !isShuffleMode;
        shuffleButton.setText(isShuffleMode ? "隨機播放 (開)" : "隨機播放");
        shuffleButton.setBackground(isShuffleMode ? Color.LIGHT_GRAY : null);
    }

    /**
     * 切換循環播放模式
     */
    private void toggleRepeat() {
        isRepeatMode = !isRepeatMode;
        repeatButton.setText(isRepeatMode ? "循環播放 (開)" : "循環播放");
        repeatButton.setBackground(isRepeatMode ? Color.LIGHT_GRAY : null);
    }

    /**
     * 清空播放清單
     */
    private void clearPlaylist() {
        stopMusic();
        playlistModel.clear();
        originalPlaylist.clear();
        currentIndex = -1;
        nowPlayingLabel.setText("請選擇音樂檔案");
        updateButtonStates();
    }

    /**
     * 更新播放進度
     */
    private void updateProgress() {
        if (audioClip != null && audioClip.isRunning()) {
            long currentFrame = audioClip.getFramePosition();
            long totalFrames = audioClip.getFrameLength();

            if (totalFrames > 0) {
                int progress = (int) ((currentFrame * 100) / totalFrames);
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");

                // 計算時間（假設取樣率為 44.1kHz）
                AudioFormat format = audioClip.getFormat();
                float frameRate = format.getFrameRate();

                long currentSeconds = (long) (currentFrame / frameRate);
                long totalSeconds = (long) (totalFrames / frameRate);

                String currentTime = formatTime(currentSeconds);
                String totalTime = formatTime(totalSeconds);
                timeLabel.setText(currentTime + " / " + totalTime);
            }
        }
    }

    /**
     * 更新按鈕狀態
     */
    private void updateButtonStates() {
        boolean hasPlaylist = playlistModel.getSize() > 0;
        boolean canPlay = hasPlaylist && (!isPlaying || isPaused);
        boolean canPause = isPlaying && !isPaused;

        playPauseButton.setText(canPause ? "暫停" : "播放");
        playPauseButton.setEnabled(hasPlaylist);
        stopButton.setEnabled(isPlaying || isPaused);
        previousButton.setEnabled(hasPlaylist);
        nextButton.setEnabled(hasPlaylist);
    }

    /**
     * 格式化時間顯示（秒轉換為 mm:ss 格式）
     */
    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 拖曳處理器類別
     * 處理播放清單項目的拖曳排序
     */
    private class PlaylistTransferHandler extends TransferHandler {
        private int[] indices = null;
        private int addIndex = -1;

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList list = (JList) c;
            indices = list.getSelectedIndices();
            Object[] values = list.getSelectedValues();

            StringBuilder buff = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                Object val = values[i];
                buff.append(val == null ? "" : val.toString());
                if (i != values.length - 1) {
                    buff.append("\n");
                }
            }
            return new StringSelection(buff.toString());
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
            int index = dl.getIndex();
            boolean insert = dl.isInsert();

            if (!insert) {
                return false;
            }

            addIndex = index;
            return true;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            if (action == TransferHandler.MOVE && indices != null) {
                // 更新播放清單順序
                List<MusicFile> itemsToMove = new ArrayList<>();
                for (int index : indices) {
                    itemsToMove.add(playlistModel.getElementAt(index));
                }

                // 移除原項目
                for (int i = indices.length - 1; i >= 0; i--) {
                    playlistModel.removeElementAt(indices[i]);
                }

                // 在新位置插入
                int insertIndex = addIndex;
                for (int i = 0; i < indices.length; i++) {
                    if (indices[i] < addIndex) {
                        insertIndex--;
                    }
                }

                for (int i = 0; i < itemsToMove.size(); i++) {
                    playlistModel.insertElementAt(itemsToMove.get(i), insertIndex + i);
                }

                // 更新目前播放索引
                if (currentIndex >= 0 && currentIndex < originalPlaylist.size()) {
                    MusicFile currentMusic = originalPlaylist.get(currentIndex);
                    for (int i = 0; i < playlistModel.getSize(); i++) {
                        if (playlistModel.getElementAt(i).getFile().equals(currentMusic.getFile())) {
                            currentIndex = i;
                            break;
                        }
                    }
                }
            }
            indices = null;
            addIndex = -1;
        }
    }

    /**
     * 主程式進入點
     */
    public static void main(String[] args) {
        // 設定 Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 啟動音樂播放器
        SwingUtilities.invokeLater(() -> {
            new MusicPlayer().setVisible(true);
        });
    }
}