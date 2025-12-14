import org.opencv.core.Mat;


/**
 * Ce travail a été réalisé par Semih Aslan et Nourath Affo.
 * S5-B2
 *
 * @author <a href="mailto:semihaslan2210@gmail.com">Semih Aslan</a>
 * @author <a href="mailto:nouraffo08@gmail.com">Nourath Affo</a>
 *
 */
public class VideoScramble {


    public static void processImageByBlocks(Mat input, Mat output, int r, int s, boolean reverse) {
        int height = input.rows();
        int currentY = 0; // ligne de départ
        int remainingHeight = height;

        // jusqua quil ny a plus de lignes à traiter
        while (remainingHeight > 0) {
            // plus grande puissance de 2 <= remainingHeight
            int blockSize = Integer.highestOneBit(remainingHeight);

            //permut sur ce bloc spécifique
            applyPermutationOnBlock(input, output, currentY, blockSize, r, s, reverse);

            // bloc suivant
            currentY += blockSize;
            remainingHeight -= blockSize;
        }
    }


    private static void applyPermutationOnBlock(Mat input, Mat output, int startY, int size, int r, int s, boolean reverse) {
        // On pré-calcule les indices cibles
        int[] permutation = new int[size];
        long step = (2L * s + 1); // long pour éviter le débordement temporaire

        for (int i = 0; i < size; i++) {
            // j'utilise la formule du sujet pour calculer l'indice cible
            int targetIndex = (int)((r + step * i) % size);
            permutation[i] = targetIndex;
        }

        if (!reverse) {
            // chiffrement
            for (int i = 0; i < size; i++) {
                int srcRowIdx = startY + i;
                int destRowIdx = startY + permutation[i];

                // Copie de la ligne i vers sa destination mélangée
                Mat srcRow = input.row(srcRowIdx);
                Mat destRow = output.row(destRowIdx);
                srcRow.copyTo(destRow);
            }
        } else {
            // déchiffrement
            for (int i = 0; i < size; i++) {
                int srcRowIdx = startY + permutation[i];
                int destRowIdx = startY + i;

                Mat srcRow = input.row(srcRowIdx);
                Mat destRow = output.row(destRowIdx);
                srcRow.copyTo(destRow);
            }
        }
    }

    /**
     * Calcule un score de "désordre" pour l'image.
     * Utilise la distance euclidienne STANDARD (telle que définie dans la formule).
     * d(x,y) = (somme((xi - yi)^2))^(1/2)
     */
    public static double calculateScore(Mat image) {
        int rows = image.rows();
        int cols = image.cols();
        int channels = image.channels();
        
        int totalBytes = (int) (image.total() * image.elemSize());
        byte[] buffer = new byte[totalBytes];
        image.get(0, 0, buffer);

        double totalScore = 0;
        int stride = cols * channels;

        // Calcul de la distance euclidienne entre lignes consécutives
        for (int i = 0; i < rows - 1; i++) {
            double lineDistanceSq = 0;
            int currentRowStart = i * stride;
            int nextRowStart = (i + 1) * stride;
            
            for (int j = 0; j < stride; j++) {
                int val1 = buffer[currentRowStart + j] & 0xFF;
                int val2 = buffer[nextRowStart + j] & 0xFF;
                
                double diff = val1 - val2;
                lineDistanceSq += diff * diff;
            }
            // Racine carrée (puissance 1/2)
            totalScore += Math.sqrt(lineDistanceSq);
        }

        return totalScore;
    }
}
