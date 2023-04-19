package com.plf.player;

import com.leewyatt.rxcontrols.controls.RXAudioSpectrum;
import com.leewyatt.rxcontrols.controls.RXLrcView;
import com.leewyatt.rxcontrols.controls.RXMediaProgressBar;
import com.leewyatt.rxcontrols.controls.RXToggleButton;
import com.leewyatt.rxcontrols.pojo.LrcDoc;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class PlayerController {



    @FXML
    private AnchorPane drawerPane;

    @FXML
    private Label soundNum;

    @FXML
    private Slider soundSlider;

    @FXML
    private ListView<File> listView;


    @FXML
    private BorderPane sliderPane;

    @FXML
    private StackPane soundBtn;

    @FXML
    private Label timeLabel;

    private ContextMenu soundPopup;

    @FXML
    private RXAudioSpectrum audioSpectrum;



    @FXML
    private ToggleButton playBtn;

    @FXML
    private RXMediaProgressBar progressBar;




    @FXML
    void onAddMusicAction(MouseEvent event) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mp3", "*.mp3"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(findStage());
        ObservableList<File> items = listView.getItems();
        if (fileList != null) {
            fileList.forEach(file -> {
                if (!items.contains(file)) {
                    items.add(file);
                }
            });
        }
    }
    @FXML
    void onCloseAction(MouseEvent event) {
        Platform.exit();
    }

    @FXML
    void onFullAction(MouseEvent event) {
        Stage stage = (Stage) drawerPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    void onHideSliderPaneAction(MouseEvent event) {

        showAnim.stop();
        hideAnim.play();
    }

    @FXML
    void onPlayAction(ActionEvent event) {
        if (player != null) {
            if (playBtn.isSelected()) {
                player.play();
            } else {
                player.pause();
            }
        } else {
            playBtn.setSelected(false);
        }
    }

    @FXML
    void onPlayNextAction(MouseEvent event) {

    }




    @FXML
    void onShowSliderPaneAction(MouseEvent event) {

        drawerPane.setVisible(true);
        hideAnim.stop();
        showAnim.play();
    }

    private Stage findStage(){
       return  (Stage) drawerPane.getScene().getWindow();
    }
    @FXML
    void onSoundPopupAction(MouseEvent event) {
        Stage stage = findStage();
        Bounds bounds = soundBtn.localToScreen(soundBtn.getBoundsInLocal());
        soundPopup.show(stage,bounds.getMinX() - 20, bounds.getMinY() - 165);
    }

    private Timeline showAnim;
    private Timeline hideAnim;
    private void initAnim() {
        showAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 0)));
        hideAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 300)));
        hideAnim.setOnFinished(event -> drawerPane.setVisible(false));
    }

    private void initSoundPopup(){
        soundPopup = new ContextMenu(new SeparatorMenuItem());
        Parent soundRoot = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/sound.fxml"));
            soundRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            soundSlider = (Slider) namespace.get("soundSlider");
            Label soundNumLabel = (Label) namespace.get("soundNum");
            soundNumLabel.textProperty().bind(soundSlider.valueProperty().asString("%.0f%%"));
            //声音滑块改变时,改变player的音量
            soundSlider.valueProperty().addListener((ob, ov, nv) -> {
                if (player != null) {
                    player.setVolume(nv.doubleValue() / 100);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        soundPopup.getScene().setRoot(soundRoot);
    }
    @FXML
    void initialize() {
        initAnim();
        initSoundPopup();
        initListView();

    }

    @FXML
    void onPlayNextMusic(MouseEvent event) {
        playNextMusic();
    }

    private void playNextMusic() {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        index = (index == size - 1) ? 0 : index + 1;
        listView.getSelectionModel().select(index);
    }


    @FXML
    void onPlayPreActon(MouseEvent event) {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        index = (index == 0) ? size - 1 : index - 1;
        listView.getSelectionModel().select(index);
    }


    //    =================================================================
    private MediaPlayer player;
    private void initListView() {
        listView.setCellFactory(fileListView -> new MusicCell());

        listView.getSelectionModel().selectedItemProperty().addListener((ob, oldFile, newFile) -> {
            if (player != null) {
                disposeMediaPlayer();
            }
            if (newFile != null) {
                player = new MediaPlayer(new Media(newFile.toURI().toString()));
                player.setVolume(soundSlider.getValue() / 100);

                //设置频谱可视化
                player.setAudioSpectrumListener((v, v1, magnitudes, floats1) -> audioSpectrum.setMagnitudes(magnitudes));
                //设置进度条的总时长
                progressBar.durationProperty().bind(player.getMedia().durationProperty());
                //进度条修改播放进度修改
                progressBar.setOnMouseClicked(mouseEvent -> {
                    MouseEventEventHandler();
                });
                progressBar.setOnMouseDragged(mouseEvent -> {
                    MouseEventEventHandler();
                });
                //播放器的进度修改监听器
                player.currentTimeProperty().addListener(durationChangeListener);
                //如果播放完当前歌曲, 自动播放下一首
                player.setOnEndOfMedia(this::playNextMusic);
                playBtn.setSelected(true);

                player.play();
            }
        });
    }

    private void MouseEventEventHandler() {
        if (player!=null){
            player.seek(progressBar.getCurrentTime());
        }
    }

    private float[] emptyAry = new float[128];
    private ChangeListener<Duration> durationChangeListener = (ob1, ov1, nv1) -> {
        progressBar.setCurrentTime(nv1);
        changeTimeLabel(nv1);
    };
    private void changeTimeLabel(Duration nv1) {
        String currentTime = sdf.format(nv1.toMillis());
        String bufferedTimer = sdf.format(player.getBufferProgressTime().toMillis());
        timeLabel.setText(currentTime+ " / "+bufferedTimer);
    }
    private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    private void disposeMediaPlayer() {
        player.stop();

        player.setAudioSpectrumListener(null);
        progressBar.durationProperty().unbind();
        progressBar.setCurrentTime(Duration.ZERO);
        player.currentTimeProperty().removeListener(durationChangeListener);
        audioSpectrum.setMagnitudes(emptyAry);
        timeLabel.setText("00:00 / 00:00");
        playBtn.setSelected(false);
        player.setOnEndOfMedia(null);
        player.dispose();
        player = null;
    }

}
