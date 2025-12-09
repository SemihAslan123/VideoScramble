import org.opencv.core.Mat;

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

            System.out.println("Traitement d'un bloc de taille " + blockSize + " à partir de la ligne " + currentY);
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

                System.out.println("Ligne " + srcRowIdx + " déplacée vers " + destRowIdx);
            }
        } else {
            // déchiffrement
            for (int i = 0; i < size; i++) {
                int srcRowIdx = startY + permutation[i];
                int destRowIdx = startY + i;

                Mat srcRow = input.row(srcRowIdx);
                Mat destRow = output.row(destRowIdx);
                srcRow.copyTo(destRow);

                System.out.println("Ligne " + srcRowIdx + " déplacée vers " + destRowIdx);
            }
        }
    }
}
