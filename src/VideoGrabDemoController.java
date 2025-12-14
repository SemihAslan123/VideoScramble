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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

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
    @FXML private Button btnTelecharger;
    @FXML private Button btnBruteForce;
    @FXML private TextField txtR;
    @FXML private TextField txtS;
    @FXML private Label lblStatus;
    @FXML private ImageView imageViewSource;
    @FXML private ImageView imageViewResultat;
    
    private ScheduledExecutorService timer;
    private VideoCapture capture = new VideoCapture();
    private VideoWriter videoWriter = new VideoWriter();
    private String videoPath;
    
    // Pour stocker la dernière frame lue brute (sans conversion UI)
    private Mat currentFrame = null;
    
    private boolean saveRequested = false;
    private String savePath = null;

    private boolean chiffrement = false;
    private boolean dechiffrement = false;
    private int cleR = 0;
    private int cleS = 0;



    @FXML
    public void initialize() {
        if (btnOuvrirFichier != null) {
            btnOuvrirFichier.setOnAction(this::handleOpenVideo);
        }

        btnMelanger.setOnAction(e -> {
            parserCle();
            chiffrement = true;
            dechiffrement = false;
            String msg = "Mode : Mélanger (Chiffrer) avec R=" + cleR + " S=" + cleS;
            System.out.println(msg);
            updateStatus(msg);
            startVideo(videoPath);
        });

        btnDemelanger.setOnAction(e -> {
            parserCle();
            chiffrement = false;
            dechiffrement = true;
            String msg = "Mode : Démélanger (Déchiffrer) avec R=" + cleR + " S=" + cleS;
            System.out.println(msg);
            updateStatus(msg);
            startVideo(videoPath);
        });

        if (btnTelecharger != null) {
            btnTelecharger.setOnAction(this::handleDownloadVideo);
        }
        
        if (btnBruteForce != null) {
            btnBruteForce.setOnAction(this::handleBruteForce);
        }
        
        updateStatus("Prêt. Veuillez ouvrir une vidéo.");
    }


    private void parserCle() {
        try {
            String textR = txtR.getText();
            String textS = txtS.getText();
            cleR = textR.isEmpty() ? 0 : Integer.parseInt(textR);
            cleS = textS.isEmpty() ? 0 : Integer.parseInt(textS);
        } catch (NumberFormatException e) {
            System.err.println("Clés invalides. Veuillez entrer des entiers valides pour R et S.");
            updateStatus("Erreur : Clés R et S invalides (entiers requis).");
        }
    }


    private void handleOpenVideo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une vidéo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers Vidéo", "*.mp4", "*.avi", "*.mkv", "*.m4v", "*.mov"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(btnOuvrirFichier.getScene().getWindow());

        if (selectedFile != null) {
            this.videoPath = selectedFile.getAbsolutePath();
            updateStatus("Vidéo chargée : " + selectedFile.getName());
            startVideo(this.videoPath);
        }
    }

    private void handleDownloadVideo(ActionEvent event) {
        if (this.videoPath == null || this.videoPath.isEmpty()) {
            updateStatus("Impossible d'enregistrer : aucune vidéo chargée.");
            System.err.println("Impossible d'enregistrer : aucune vidéo n'est chargée.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer la vidéo sous...");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichier AVI (MJPG)", "*.avi")
        );
        
        File file = fileChooser.showSaveDialog(btnTelecharger.getScene().getWindow());
        if (file != null) {
            this.savePath = file.getAbsolutePath();
            this.saveRequested = true;
            updateStatus("Enregistrement demandé vers : " + file.getName() + " (Démarrage...)");
            startVideo(this.videoPath);
        }
    }
    
    private void handleBruteForce(ActionEvent event) {
        if (!capture.isOpened()) {
            updateStatus("Erreur : Ouvrez une vidéo chiffrée d'abord.");
            return;
        }

        // On utilise la frame brute stockée en mémoire au lieu de l'image de l'UI
        if (this.currentFrame == null || this.currentFrame.empty()) {
            updateStatus("Erreur : Aucune image en mémoire. Laissez la vidéo tourner un peu.");
            return;
        }
        
        // On clone pour ne pas être affecté par la lecture vidéo qui continue
        Mat scrambledImage = this.currentFrame.clone();
        
        new Thread(() -> {
            int bestR = -1;
            int bestS = -1;
            double bestScore = Double.MAX_VALUE;
            
            Mat tempImage = new Mat(scrambledImage.rows(), scrambledImage.cols(), scrambledImage.type());
            
            for (int r = 0; r <= 255; r++) {
                for (int s = 0; s <= 127; s++) {
                    VideoScramble.processImageByBlocks(scrambledImage, tempImage, r, s, true);
                    double score = VideoScramble.calculateScore(tempImage);
                    
                    if (score < bestScore) {
                        bestScore = score;
                        bestR = r;
                        bestS = s;
                        
                        final int finalR = bestR;
                        final int finalS = bestS;
                        
                        // On clone l'image résultat pour l'affichage (évite les conflits de thread)
                        final Mat bestImageSoFar = tempImage.clone();
                        
                        Platform.runLater(() -> {
                            txtR.setText(String.valueOf(finalR));
                            txtS.setText(String.valueOf(finalS));
                            updateStatus("Recherche... Meilleure clé trouvée : R=" + finalR + ", S=" + finalS);
                            updateImageView(imageViewResultat, mat2Image(bestImageSoFar));
                        });
                    }
                }
            }
            
            final int finalBestR = bestR;
            final int finalBestS = bestS;
            Platform.runLater(() -> {
                updateStatus("Recherche terminée ! Clé trouvée : R=" + finalBestR + ", S=" + finalBestS);
                parserCle();
                dechiffrement = true;
                chiffrement = false;
                startVideo(videoPath);
            });
        }).start();
    }


    public void startVideo(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            return;
        }
        stopAcquisition();
        this.capture.open(videoPath);

        if (this.capture.isOpened()) {
            Runnable frameGrabber = () -> {
                Mat frame = grabFrame();

                if (!frame.empty()) {
                    // On sauvegarde la frame courante pour le brute force
                    this.currentFrame = frame.clone();

                    if (saveRequested && savePath != null) {
                        if (videoWriter.isOpened()) {
                            videoWriter.release();
                        }
                        double fps = capture.get(5);
                        if (fps <= 0) fps = 25.0;
                        
                        Size frameSize = new Size(frame.width(), frame.height());
                        int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
                        
                        videoWriter.open(savePath, fourcc, fps, frameSize, true);
                        
                        if (videoWriter.isOpened()) {
                            String msg = "Enregistrement EN COURS (" + frame.width() + "x" + frame.height() + ")";
                            System.out.println(msg);
                            updateStatus(msg);
                        } else {
                            String err = "Erreur : Impossible de créer le fichier vidéo.";
                            System.err.println(err);
                            updateStatus(err);
                        }
                        saveRequested = false;
                    }

                    Image imageOriginale = mat2Image(frame);
                    updateImageView(imageViewSource, imageOriginale);

                    Mat frameCopie = frame.clone();

                    if (chiffrement) {
                        VideoScramble.processImageByBlocks(frame, frameCopie, cleR, cleS, false);
                    } else if (dechiffrement) {
                        VideoScramble.processImageByBlocks(frame, frameCopie, cleR, cleS, true);
                    }

                    if (videoWriter.isOpened()) {
                        videoWriter.write(frameCopie);
                    }

                    updateImageView(imageViewResultat, mat2Image(frameCopie));
                } else {
                    stopAcquisition();
                    System.out.println("Fin de la vidéo.");
                    updateStatus("Fin de la vidéo (et de l'enregistrement si actif).");
                }
            };

            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
        } else {
            System.err.println("Erreur au niveau de l'ouverture de la vidéo: " + videoPath);
            updateStatus("Erreur ouverture vidéo.");
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
        
        if (videoWriter.isOpened()) {
            videoWriter.release();
            System.out.println("Enregistrement terminé et fichier fermé.");
        }
    }

    private void updateImageView(ImageView view, Image image) {
        onFXThread(view.imageProperty(), image);
    }
    
    private void updateStatus(String text) {
        Platform.runLater(() -> {
            if (lblStatus != null) {
                lblStatus.setText(text);
            }
        });
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
    
    // Cette méthode n'est plus utilisée car on utilise currentFrame directement
    private Mat imageToMat(Image image) {
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        BufferedImage convertedImg = new BufferedImage(bImage.getWidth(), bImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        convertedImg.getGraphics().drawImage(bImage, 0, 0, null);
        Mat mat = new Mat(convertedImg.getHeight(), convertedImg.getWidth(), 16);
        byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
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