package D0723.three;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * JavaFX音樂播放器主類
 * 功能包含：播放清單、媒體控制、拖曳排序、隨機播放等
 */
public class MusicPlayer extends Application {

    // 媒體播放相關變數
    private MediaPlayer mediaPlayer;
    private ObservableList<File> playlist = FXCollections.observableArrayList();
    private int currentIndex = 0;

    // 播放模式：0=順序播放, 1=隨機播放, 2=循環播放
    private int playMode = 0;
    private Random random = new Random();

    // UI元件
    private ListView<File> playlistView;
    private Label nowPlayingLabel;
    private Label timeLabel;
    private Slider progressSlider;
    private Button playPauseButton;
    private Button stopButton;
    private Button prevButton;
    private Button nextButton;
    private Button modeButton;
    private Slider volumeSlider;

    // 自定義資料格式，用於拖曳功能
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX 音樂播放器");

        // 建立主要版面
        BorderPane root = new BorderPane();

        // 左側：播放清單區域
        VBox leftPanel = createPlaylistPanel();

        // 右側：正在播放資訊區域
        VBox rightPanel = createNowPlayingPanel();

        // 底部：控制按鈕區域
        HBox bottomPanel = createControlPanel();

        // 組合版面
        root.setLeft(leftPanel);
        root.setCenter(rightPanel);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // 設定關閉時清理資源
        primaryStage.setOnCloseRequest(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            Platform.exit();
        });
    }

    /**
     * 建立播放清單面板
     */
    private VBox createPlaylistPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(300);

        // 資料夾選擇按鈕
        Button selectFolderButton = new Button("選擇音樂資料夾");
        selectFolderButton.setOnAction(e -> selectMusicFolder());

        // 播放清單標題
        Label playlistTitle = new Label("播放清單");
        playlistTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // 建立可拖曳的播放清單
        playlistView = new ListView<>(playlist);
        playlistView.setPrefHeight(400);

        // 設定清單項目的顯示格式（只顯示檔案名稱，不含副檔名）
        playlistView.setCellFactory(lv -> {
            ListCell<File> cell = new ListCell<File>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        String name = item.getName();
                        // 移除副檔名顯示
                        int lastDot = name.lastIndexOf('.');
                        if (lastDot > 0) {
                            name = name.substring(0, lastDot);
                        }
                        setText(name);
                    }
                }
            };

            // 實作拖曳功能
            setupDragAndDrop(cell);

            return cell;
        });

        // 雙擊播放功能
        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = playlistView.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0) {
                    currentIndex = selectedIndex;
                    playSelectedSong();
                }
            }
        });

        panel.getChildren().addAll(selectFolderButton, playlistTitle, playlistView);
        return panel;
    }

    /**
     * 設定清單項目的拖曳功能
     */
    private void setupDragAndDrop(ListCell<File> cell) {
        // 開始拖曳
        cell.setOnDragDetected(event -> {
            if (cell.getItem() == null) return;

            Dragboard dragboard = cell.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(SERIALIZED_MIME_TYPE, cell.getIndex());
            dragboard.setContent(content);
            event.consume();
        });

        // 拖曳經過時的視覺效果
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell &&
                    event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // 拖曳進入時的視覺效果
        cell.setOnDragEntered(event -> {
            if (event.getGestureSource() != cell &&
                    event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                cell.setOpacity(0.3);
            }
        });

        // 拖曳離開時恢復原狀
        cell.setOnDragExited(event -> {
            if (event.getGestureSource() != cell &&
                    event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                cell.setOpacity(1);
            }
        });

        // 放下時重新排序
        cell.setOnDragDropped(event -> {
            if (cell.getItem() == null) return;

            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                int draggedIdx = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                int thisIdx = cell.getIndex();

                // 重新排序播放清單
                File draggedFile = playlist.remove(draggedIdx);
                playlist.add(thisIdx, draggedFile);

                // 更新當前播放索引
                if (currentIndex == draggedIdx) {
                    currentIndex = thisIdx;
                } else if (draggedIdx < currentIndex && thisIdx >= currentIndex) {
                    currentIndex--;
                } else if (draggedIdx > currentIndex && thisIdx <= currentIndex) {
                    currentIndex++;
                }

                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });

        cell.setOnDragDone(event -> {
            if (event.getTransferMode() == TransferMode.MOVE) {
                cell.updateListView(playlistView);
            }
            event.consume();
        });
    }

    /**
     * 建立正在播放資訊面板
     */
    private VBox createNowPlayingPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.CENTER);

        // 正在播放標題
        Label title = new Label("正在播放");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 歌曲名稱顯示
        nowPlayingLabel = new Label("尚未選擇歌曲");
        nowPlayingLabel.setStyle("-fx-font-size: 16px;");
        nowPlayingLabel.setWrapText(true);

        // 播放進度條
        progressSlider = new Slider();
        progressSlider.setMajorTickUnit(30);
        progressSlider.setShowTickLabels(true);
        progressSlider.setPrefWidth(300);

        // 進度條拖曳事件
        progressSlider.setOnMousePressed(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        // 時間顯示
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-font-size: 12px;");

        // 音量控制
        Label volumeLabel = new Label("音量");
        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(200);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
            }
        });

        panel.getChildren().addAll(title, nowPlayingLabel, progressSlider, timeLabel, volumeLabel, volumeSlider);
        return panel;
    }

    /**
     * 建立控制按鈕面板
     */
    private HBox createControlPanel() {
        HBox panel = new HBox(15);
        panel.setPadding(new Insets(15));
        panel.setAlignment(Pos.CENTER);

        // 上一首按鈕
        prevButton = new Button("⏮ 上一首");
        prevButton.setOnAction(e -> playPrevious());

        // 播放/暫停按鈕
        playPauseButton = new Button("▶ 播放");
        playPauseButton.setOnAction(e -> togglePlayPause());

        // 停止按鈕
        stopButton = new Button("⏹ 停止");
        stopButton.setOnAction(e -> stopMusic());

        // 下一首按鈕
        nextButton = new Button("⏭ 下一首");
        nextButton.setOnAction(e -> playNext());

        // 播放模式按鈕
        modeButton = new Button("🔁 順序播放");
        modeButton.setOnAction(e -> switchPlayMode());

        panel.getChildren().addAll(prevButton, playPauseButton, stopButton, nextButton, modeButton);
        return panel;
    }

    /**
     * 選擇音樂資料夾
     */
    private void selectMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("選擇音樂資料夾");

        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            loadMusicFiles(selectedDirectory);
        }
    }

    /**
     * 載入音樂檔案
     */
    private void loadMusicFiles(File directory) {
        playlist.clear();

        try {
            // 支援的音樂格式
            String[] supportedFormats = {".mp3", ".wav", ".m4a", ".flac", ".aac"};

            Files.walk(directory.toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.toString().toLowerCase();
                        for (String format : supportedFormats) {
                            if (fileName.endsWith(format)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .forEach(path -> playlist.add(path.toFile()));

            System.out.println("載入了 " + playlist.size() + " 個音樂檔案");

        } catch (Exception e) {
            showAlert("錯誤", "載入音樂檔案時發生錯誤：" + e.getMessage());
        }
    }

    /**
     * 播放選中的歌曲
     */
    private void playSelectedSong() {
        if (playlist.isEmpty() || currentIndex < 0 || currentIndex >= playlist.size()) {
            return;
        }

        // 停止當前播放
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        try {
            File currentFile = playlist.get(currentIndex);
            Media media = new Media(currentFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // 設定音量
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

            // 更新正在播放顯示
            String fileName = currentFile.getName();
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                fileName = fileName.substring(0, lastDot);
            }
            nowPlayingLabel.setText(fileName);

            // 設定媒體播放器事件監聽
            mediaPlayer.setOnReady(() -> {
                Duration totalDuration = mediaPlayer.getTotalDuration();
                progressSlider.setMax(totalDuration.toSeconds());
                updateTimeLabel();
            });

            // 播放進度更新
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue(newTime.toSeconds());
                }
                updateTimeLabel();
            });

            // 歌曲結束時自動播放下一首
            mediaPlayer.setOnEndOfMedia(() -> {
                playNext();
            });

            // 錯誤處理
            mediaPlayer.setOnError(() -> {
                showAlert("播放錯誤", "無法播放檔案：" + currentFile.getName());
                playNext();
            });

            // 開始播放
            mediaPlayer.play();
            playPauseButton.setText("⏸ 暫停");

            // 高亮當前播放項目
            playlistView.getSelectionModel().select(currentIndex);

        } catch (Exception e) {
            showAlert("錯誤", "播放音樂時發生錯誤：" + e.getMessage());
        }
    }

    /**
     * 播放/暫停切換
     */
    private void togglePlayPause() {
        if (mediaPlayer == null) {
            if (!playlist.isEmpty()) {
                currentIndex = 0;
                playSelectedSong();
            }
            return;
        }

        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playPauseButton.setText("▶ 播放");
        } else if (status == MediaPlayer.Status.PAUSED) {
            mediaPlayer.play();
            playPauseButton.setText("⏸ 暫停");
        }
    }

    /**
     * 停止播放
     */
    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playPauseButton.setText("▶ 播放");
            progressSlider.setValue(0);
            timeLabel.setText("00:00 / 00:00");
        }
    }

    /**
     * 播放上一首
     */
    private void playPrevious() {
        if (playlist.isEmpty()) return;

        if (playMode == 1) { // 隨機播放
            currentIndex = random.nextInt(playlist.size());
        } else {
            currentIndex--;
            if (currentIndex < 0) {
                currentIndex = playlist.size() - 1;
            }
        }

        playSelectedSong();
    }

    /**
     * 播放下一首
     */
    private void playNext() {
        if (playlist.isEmpty()) return;

        if (playMode == 1) { // 隨機播放
            currentIndex = random.nextInt(playlist.size());
        } else if (playMode == 2) { // 單曲循環
            // currentIndex 保持不變
        } else { // 順序播放
            currentIndex++;
            if (currentIndex >= playlist.size()) {
                currentIndex = 0;
            }
        }

        playSelectedSong();
    }

    /**
     * 切換播放模式
     */
    private void switchPlayMode() {
        playMode = (playMode + 1) % 3;

        switch (playMode) {
            case 0:
                modeButton.setText("🔁 順序播放");
                break;
            case 1:
                modeButton.setText("🔀 隨機播放");
                break;
            case 2:
                modeButton.setText("🔂 單曲循環");
                break;
        }
    }

    /**
     * 更新時間顯示
     */
    private void updateTimeLabel() {
        if (mediaPlayer != null) {
            Duration currentTime = mediaPlayer.getCurrentTime();
            Duration totalTime = mediaPlayer.getTotalDuration();

            String current = formatTime(currentTime);
            String total = formatTime(totalTime);

            timeLabel.setText(current + " / " + total);
        }
    }

    /**
     * 格式化時間顯示
     */
    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }

        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 顯示錯誤提示框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 主程式入口
     */
    public static void main(String[] args) {
        launch(args);
    }
}