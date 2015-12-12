package jp.yucchi.intelrealsense;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 *
 * @author Yucchi
 */
public class IntelRealSense extends Application {

    private double dragStartX;
    private double dragStartY;

    @Override
    public void start(Stage stage) throws Exception {

        final AudioClip intel = new AudioClip(this.getClass().getResource("resources/intel.wav").toExternalForm());
        intel.play();

        Parent root = FXMLLoader.load(getClass().getResource("RealSense.fxml"));
        Scene scene = new Scene(root);

        scene.setOnMousePressed(e -> {
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
        });

        scene.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragStartX);
            stage.setY(e.getScreenY() - dragStartY);
        });

        scene.getStylesheets().add(this.getClass().getResource("RealSense.css").toExternalForm());
        stage.setScene(scene);
        Image myIcon = new Image(this.getClass().getResource("resources/sakura_icon.png").toExternalForm());
        stage.getIcons().add(myIcon);
        stage.setTitle("Intel RealSence with JavaFX");
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        // オープニングアニメーション
        DoubleProperty openOpacityProperty = new SimpleDoubleProperty(0.0);
        stage.opacityProperty().bind(openOpacityProperty);
        Timeline openTimeline = new Timeline(
                new KeyFrame(
                        new Duration(100),
                        new KeyValue(openOpacityProperty, 0.0)
                ), new KeyFrame(
                        new Duration(2_500),
                        new KeyValue(openOpacityProperty, 1.0)
                ));
        openTimeline.setCycleCount(1);
        openTimeline.play();
        stage.centerOnScreen();
        stage.show();

        stage.setOnCloseRequest(we -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initStyle(StageStyle.TRANSPARENT);
            alert.setTitle("確認");
            alert.setHeaderText("Confirmation.");
            alert.setContentText("Do you really want to exit the application?");
            alert.showAndWait()
                    .filter(response -> response == ButtonType.OK)
                    .ifPresent(response -> {
                        // クロージングアニメーション
                        DoubleProperty closeOpacityProperty = new SimpleDoubleProperty(1.0);
                        stage.opacityProperty().bind(closeOpacityProperty);

                        Timeline closeTimeline = new Timeline(
                                new KeyFrame(
                                        new Duration(100),
                                        new KeyValue(closeOpacityProperty, 1.0)
                                ), new KeyFrame(
                                        new Duration(2_500),
                                        new KeyValue(closeOpacityProperty, 0.0)
                                ));

                        EventHandler<ActionEvent> eh = ae -> {
                            Platform.exit();
                            System.exit(0);
                        };

                        closeTimeline.setOnFinished(eh);
                        closeTimeline.setCycleCount(1);
                        closeTimeline.play();
                    });
            we.consume();
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
