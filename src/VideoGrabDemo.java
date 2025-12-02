import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.Scene;


import javafx.stage.WindowEvent;
import org.opencv.core.Core;


public class VideoGrabDemo extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) {

        //VideoCapture capture = new VideoCapture(0); // 0 indique la première webcam
        //Mat frame = new Mat();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("VideoGrabDemo.fxml"));
            BorderPane rootElement = (BorderPane) loader.load();

            Scene scene = new Scene(rootElement, 1000, 600);

            primaryStage.setTitle("Capture vidéo de la webcam avec OpenCV");
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

