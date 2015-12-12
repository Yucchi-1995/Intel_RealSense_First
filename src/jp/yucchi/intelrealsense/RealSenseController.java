package jp.yucchi.intelrealsense;

import intel.rssdk.PXCMCapture;
import intel.rssdk.PXCMImage;
import intel.rssdk.PXCMSenseManager;
import intel.rssdk.pxcmStatus;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 *
 * @author Yucchi
 */
public class RealSenseController implements Initializable {

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private ImageView imageView;

    @FXML
    private ToggleGroup toggleGroup;

    @FXML
    private RadioButton colorRadioButton;

    @FXML
    private RadioButton depthRadioButton;

    @FXML
    private RadioButton irRadioButton;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Button exitButton;

    private PXCMSenseManager senseManager;
    private StreamService streamService;
    private pxcmStatus pxcmStatus;
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private Image image;
    private final StringProperty errorContent = new SimpleStringProperty();

    @FXML
    private void handleStartButtonAction(ActionEvent event) {

        streamService = new StreamService();

        streamService.setOnSucceeded(wse -> {
            if (streamService.getValue() != null) {
                imageView.setImage(image);
                streamService.restart();
            }
        });

        streamService.setOnFailed(wse -> {
            if (errorContent.getValue() == null) {
                errorContent.setValue("Error!\n" + "StreamService Failed.");
            }
            errorProcessing();
        });

        // SenseManagerを生成する
        senseManager = PXCMSenseManager.CreateInstance();

        // カラーストリームを有効にする
        senseManager.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_COLOR, WIDTH, HEIGHT);
        // Depth ストリームを有効にする
        senseManager.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_DEPTH, WIDTH, HEIGHT);
        // IR ストリームを有効にする
        senseManager.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_IR, WIDTH, HEIGHT);

        // PXCM_STATUS 初期化
        pxcmStatus = senseManager.Init();

        // ミラーモードにする
        senseManager.QueryCaptureManager().QueryDevice().SetMirrorMode(PXCMCapture.Device.MirrorMode.MIRROR_MODE_HORIZONTAL);

        if (!streamService.isRunning()) {
            streamService.reset();
            streamService.start();
        }

        startButton.disableProperty().bind(streamService.runningProperty());
        stopButton.disableProperty().bind(streamService.runningProperty().not());

    }

    @FXML
    private void handleStoptButtonAction(ActionEvent event) {

        if (streamService.isRunning()) {
            streamService.cancel();
        }

        senseManager.Close();

        imageView.setImage(new Image(this.getClass().getResourceAsStream("resources/duke_cake.jpg")));

    }

    @FXML
    private void handleExittButtonAction(ActionEvent event) {
        exitProcessing();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        imageView.setImage(new Image(this.getClass().getResourceAsStream("resources/duke_cake.jpg")));

        // ストリーミングタイプ選択ラジオスイッチ
        colorRadioButton.setUserData("Color");
        depthRadioButton.setUserData("Depth");
        irRadioButton.setUserData("IR");

    }

    class StreamService extends Service<Image> {

        @Override
        protected Task<Image> createTask() {

            Task<Image> task = new Task<Image>() {

                @Override
                protected Image call() throws Exception {

                    if (pxcmStatus == pxcmStatus.PXCM_STATUS_NO_ERROR) {

                        // フレーム取得
                        if (senseManager.AcquireFrame(true).isSuccessful()) {

                            // フレームデータ取得
                            PXCMCapture.Sample sample = senseManager.QuerySample();

                            // 選択されたストリームによる画像データ処理
                            switch (toggleGroup.getSelectedToggle().getUserData().toString()) {

                                case "Color":
                                    if (sample.color != null) {
                                        // データ取得
                                        PXCMImage.ImageData cData = new PXCMImage.ImageData();
                                        // アクセス権を取得（アクセス権の種類、画像フォーマット、データ）
                                        pxcmStatus = sample.color.AcquireAccess(PXCMImage.Access.ACCESS_READ, PXCMImage.PixelFormat.PIXEL_FORMAT_RGB32, cData);

                                        if (pxcmStatus.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                                            errorContent.setValue("Error!\n" + "Failed to AcquireAccess of ColorImage Data.");
                                            throw new Exception();
                                        }

                                        // BufferedImage に変換 １ピクセルあたり４バイトに注意、PXCMImage.PixelFormat.PIXEL_FORMAT_RGB24 だと３バイト
                                        int cBuff[] = new int[cData.pitches[0] / 4 * HEIGHT];
                                        cData.ToIntArray(0, cBuff);
                                        BufferedImage bImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                                        bImage.setRGB(0, 0, WIDTH, HEIGHT, cBuff, 0, cData.pitches[0] / 4);

                                        // ImageView にセットできるように Image に変換
                                        image = SwingFXUtils.toFXImage(bImage, null);

                                        // データを解放
                                        pxcmStatus = sample.color.ReleaseAccess(cData);

                                        if (pxcmStatus.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) > 0) {
                                            errorContent.setValue("Error!\n" + "Failed to ReleaseAccess of ColorImage Data.");
                                            throw new Exception();
                                        }
                                    }
                                    break;
                                case "Depth":
                                    if (sample.depth != null) {

                                        PXCMImage.ImageData dData = new PXCMImage.ImageData();
                                        sample.depth.AcquireAccess(PXCMImage.Access.ACCESS_READ, PXCMImage.PixelFormat.PIXEL_FORMAT_RGB32, dData);

                                        if (pxcmStatus.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                                            errorContent.setValue("Error!\n" + "Failed to AcquireAccess of DepthImage Data.");
                                            throw new Exception();
                                        }

                                        int dBuff[] = new int[dData.pitches[0] / 4 * HEIGHT];
                                        dData.ToIntArray(0, dBuff);
                                        BufferedImage bImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                                        bImage.setRGB(0, 0, WIDTH, HEIGHT, dBuff, 0, dData.pitches[0] / 4);

                                        image = SwingFXUtils.toFXImage(bImage, null);

                                        pxcmStatus = sample.depth.ReleaseAccess(dData);

                                        if (pxcmStatus.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                                            errorContent.setValue("Error!\n" + "Failed to ReleaseAccess of DepthImage Data.");
                                            throw new Exception();
                                        }
                                    }
                                    break;
                                case "IR":
                                    if (sample.ir != null) {

                                        PXCMImage.ImageData iData = new PXCMImage.ImageData();
                                        sample.ir.AcquireAccess(PXCMImage.Access.ACCESS_READ, PXCMImage.PixelFormat.PIXEL_FORMAT_RGB32, iData);

                                        if (pxcmStatus.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                                            errorContent.setValue("Error!\n" + "Failed to AcquireAccess of IRImage Data.");
                                            throw new Exception();
                                        }

                                        int dBuff[] = new int[iData.pitches[0] / 4 * HEIGHT];
                                        iData.ToIntArray(0, dBuff);
                                        BufferedImage bImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                                        bImage.setRGB(0, 0, WIDTH, HEIGHT, dBuff, 0, iData.pitches[0] / 4);

                                        image = SwingFXUtils.toFXImage(bImage, null);

                                        pxcmStatus = sample.ir.ReleaseAccess(iData);

                                        if (pxcmStatus.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                                            errorContent.setValue("Error!\n" + "Failed to ReleaseAccess of IRImage Data.");
                                            throw new Exception();
                                        }
                                    }

                                    break;
                                default:

                            }

                            // 次のフレームデータを呼び出すためにフレームを解放する
                            senseManager.ReleaseFrame();
                        } else {
                            // 極まれにフレーム取得失敗する
//                            errorContent.setValue("Failed to acquire frame.");
//                            errorProcessing();
                        }

                    } else {
                        errorContent.setValue("Error!\n" + "Failed to Initialize.");
                        errorProcessing();
                    }

                    return image;

                }

            };

            return task;
        }

    }

    private void errorProcessing() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        TextArea textArea = new TextArea(errorContent.get());
        textArea.setEditable(false);
        alert.getDialogPane().setExpandableContent(textArea);
        alert.initStyle(StageStyle.TRANSPARENT);
        alert.setTitle("ERROR");
        alert.setHeaderText("Error!\n"
                + "An unexpected error has occurred.");
        alert.setContentText("Exit the application.");
        alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> exitProcessing());
    }

    private void exitProcessing() {

        // クロージングアニメーション
        DoubleProperty closeOpacityProperty = new SimpleDoubleProperty(1.0);
        anchorPane.getScene().getWindow().opacityProperty().bind(closeOpacityProperty);

        Timeline closeTimeline = new Timeline(
                new KeyFrame(
                        new Duration(100),
                        new KeyValue(closeOpacityProperty, 1.0)
                ), new KeyFrame(
                        new Duration(2_500),
                        new KeyValue(closeOpacityProperty, 0.0)
                ));

        EventHandler<ActionEvent> eh = ae -> {

            if (streamService != null && streamService.isRunning()) {
                streamService.cancel();
            }
            if (pxcmStatus != null) {
                senseManager.Close();
            }
            Platform.exit();
            System.exit(0);
        };

        closeTimeline.setOnFinished(eh);
        closeTimeline.setCycleCount(1);
        closeTimeline.play();
    }

}
