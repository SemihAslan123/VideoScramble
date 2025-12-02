import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

/**
 * Ce travail a été réalisé par Semih Aslan et Nourath Affo.
 * S5-B2
 *
 * @author <a href="mailto:semihaslan2210@gmail.com">Semih Aslan</a>
 * @author <a href="mailto:nouraffo08@gmail.com">Nourath Affo</a>
 *
 */
public class VideoGrabDemoController {
    @FXML
    private Button btnOpen;
    @FXML
    private ImageView imageViewSource;
    private ScheduledExecutorService timer;
    private VideoCapture capture = new VideoCapture();
    private boolean cameraActive = false;

    @FXML
    public void initialize() {
        // action du bouton "Ouvrir vidéo"
        if (btnOpen != null) {
            btnOpen.setOnAction(this::handleOpenVideo);
        }
    }

    private void handleOpenVideo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une vidéo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers Vidéo", "*.mp4", "*.avi", "*.mkv", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(btnOpen.getScene().getWindow());

        if (selectedFile != null) {
            startVideo(selectedFile.getAbsolutePath());
        }
    }

    public void startVideo(String videoPath) {
        stopAcquisition();
        this.capture.open(videoPath);

        if (this.capture.isOpened()) {
            // lecture de la vidéo sélectionne
            Runnable frameGrabber = new Runnable() {
                @Override
                public void run() {
                    // on récupère l'image
                    Mat frame = grabFrame();

                    // y'a t-il une prochaine image ?
                    if (!frame.empty()) {
                        // Conversion et affichage
                        Image imageToShow = mat2Image(frame);
                        updateImageView(imageViewSource, imageToShow);
                    }
                    else {
                        // Fin de la vidéo : on arrête le moteur
                        stopAcquisition();
                        System.out.println("Fin de la vidéo.");
                    }
                }
            };

            // démarrage du chrono, toute les 33 secondes.
            // on a environ un flux vidéo d'environ 30 images par seconde.
            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
        } else {
            System.err.println("Impossible d'ouvrir le fichier vidéo : " + videoPath);
        }
    }

    private Mat grabFrame() {
        Mat frame = new Mat();

        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
                if (!frame.empty()) {
                    // basic single frame processing can be performed here
                }

            }
            catch (Exception e) {
                System.err.println("Exception lors de la lecture de l'image :" + e);
            }
        }
        return frame;
    }


    private void stopAcquisition() {
        if (this.timer!=null && !this.timer.isShutdown()) {
            try {
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                System.err.println("Erreur lors de l'arrêt du timer :" + e);
            }
        }

        if (this.capture.isOpened()) {
            this.capture.release();
        }
    }


    private void updateImageView(ImageView view, Image image) {
        onFXThread(view.imageProperty(), image);
    }

    protected void setClosed() {
        this.stopAcquisition();
    }

    private Image matToJavaFXImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new java.io.ByteArrayInputStream(buffer.toArray()));
    }


    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        }
        catch (Exception e) {
            System.err.println("Cannot convert the Mat obejct: " + e);
            return null;
        }
    }

    private static BufferedImage matToBufferedImage(Mat original) {
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    public static <T> void onFXThread(final ObjectProperty<T> property, final T value) {
        Platform.runLater(() -> {
            property.set(value);
        });
    }

}