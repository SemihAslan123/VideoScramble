import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.WindowEvent;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Core;

/**
 * Ce travail a été réalisé par Semih Aslan et Nourath Affo.
 * S5-B2
 *
 * @author <a href="mailto:semihaslan2210@gmail.com">Semih Aslan</a>
 * @author <a href="mailto:nouraffo08@gmail.com">Nourath Affo</a>
 *
 */


public class VideoGrabDemo extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("VideoGrabDemo.fxml"));
            BorderPane rootElement = (BorderPane) loader.load();

            Scene scene = new Scene(rootElement, 1000, 600);

            primaryStage.setTitle("Video Scramble");
            primaryStage.setScene(scene);
            primaryStage.show();


            VideoGrabDemoController controller = loader.getController();
            primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
                public void handle(WindowEvent we) {
                    controller.setClosed();
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        launch(args);
    }

}

