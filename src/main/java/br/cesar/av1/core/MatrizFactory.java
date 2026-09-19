package br.cesar.av1.core;

import java.util.SplittableRandom;

public final class MatrizFactory {

    private static final long SEED = 42L;

    private MatrizFactory() {
    }

    public static double[][] gerar(int tamanho) {

        double[][] matriz = new double[tamanho][tamanho];
        SplittableRandom random = new SplittableRandom(SEED);

        for (int i = 0; i < tamanho; i++) {
            for (int j = 0; j < tamanho; j++) {
                matriz[i][j] = random.nextDouble(1.0, 100.0);
            }
        }

        return matriz;
    }
}