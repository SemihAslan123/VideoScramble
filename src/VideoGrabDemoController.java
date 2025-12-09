import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
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
    @FXML private Button btnOuvrirFichier;
    @FXML private Button btnMelanger;
    @FXML private Button btnDemelanger;
    @FXML private TextField txtR;
    @FXML private TextField txtS;
    @FXML private ImageView imageViewSource;
    @FXML private ImageView imageViewResultat;
    private ScheduledExecutorService timer;
    private VideoCapture capture = new VideoCapture();
    // si c'est vrai alors on chiffrera/déchiffrera la vidéo
    private boolean chiffrement = false;
    private boolean dechiffrement = false;
    private int cleR = 0;
    private int cleS = 0;



    /**
     * Permet d'initialiser les actions des boutons et champs de texte.
     * */
    @FXML
    public void initialize() {
        if (btnOuvrirFichier != null) {
            btnOuvrirFichier.setOnAction(this::handleOpenVideo);
        }

        btnMelanger.setOnAction(e -> {
            parserCle();
            chiffrement = true;
            dechiffrement = false;
            System.out.println("Mode : Mélanger (Chiffrer) avec R=" + cleR + " S=" + cleS);
        });

        btnDemelanger.setOnAction(e -> {
            parserCle();
            chiffrement = false;
            dechiffrement = true;
            System.out.println("Mode : Démélanger (Déchiffrer) avec R=" + cleR + " S=" + cleS);
        });
    }


    /**
     * Permet de récupérer les clés R et S depuis les champs de texte dans la vue.
     * On récupère les clés R et S, on met 0 par défaut si les champs sont vides
     * puis on renvoie un message dans le terminal en cas d'erreur de format.
     */
    private void parserCle() {
        try {
            String textR = txtR.getText();
            String textS = txtS.getText();
            cleR = textR.isEmpty() ? 0 : Integer.parseInt(textR);
            cleS = textS.isEmpty() ? 0 : Integer.parseInt(textS);
        } catch (NumberFormatException e) {
            System.err.println("Clés invalides. Veuillez entrer des entiers valides pour R et S.");
        }
    }


    /**
     * @argument event: ActionEvent
     * ---
     * Permet de sélectionner une vidéo depuis les fichiers de l'appareil.
     * Méthode utilisé par le bouton "Ouvrir Vidéo".
     * */
    private void handleOpenVideo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une vidéo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers Vidéo", "*.mp4", "*.avi", "*.mkv", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(btnOuvrirFichier.getScene().getWindow());

        if (selectedFile != null) {
            startVideo(selectedFile.getAbsolutePath());
        }
    }


    /**
     * @argument videoPath: String
     * ---
     * Ouvre la lecture de la vidéo et démarre le traitement image par image.
     * Mélange ou Démélange qui se passe sur une copie de l'image originale.
     * Si aucun bouton n'est pressé, frameProcessed reste une copie de l'originale.
     * */
    public void startVideo(String videoPath) {
        stopAcquisition();
        this.capture.open(videoPath);

        if (this.capture.isOpened()) {
            Runnable frameGrabber = () -> {
                Mat frame = grabFrame();

                if (!frame.empty()) {
                    Image imageOriginale = mat2Image(frame);
                    updateImageView(imageViewSource, imageOriginale);

                    Mat frameCopie = frame.clone();

                    if (chiffrement) {
                        VideoScramble.processImageByBlocks(frame, frameCopie, cleR, cleS, false);
                    } else if (dechiffrement) {
                        VideoScramble.processImageByBlocks(frame, frameCopie, cleR, cleS, true);
                    }

                    updateImageView(imageViewResultat, mat2Image(frameCopie));
                } else {
                    stopAcquisition();
                    System.out.println("C'est la fin de la vidéo !!!");
                }
            };

            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
        } else {
            System.err.println("Erreur au niveau de l'ouverture de la vidéo: " + videoPath);
        }
    }




    private Mat grabFrame() {
        Mat frame = new Mat();

        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
            } catch (Exception e) {
                System.err.println("Erreur au niveau de la lecture de l'image :" + e);
            }
        }
        return frame;
    }

    private void stopAcquisition() {
        if (this.timer!=null && !this.timer.isShutdown()) {
            try {
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.err.println("Erreur au niveau de l'arrêt du timer :" + e);
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

    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
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