package br.cesar.av1.core;

public final class CalculadoraPesada {

    private CalculadoraPesada() {
    }

    public static double calcular(double valor) {
        double resultado = valor;

        for (int i = 0; i < 1000; i++) {
            resultado +=
                    Math.sin(valor + i)
                    * Math.cos(valor - i)
                    * Math.sqrt(Math.abs(valor) + 1);
        }

        return resultado;
    }

    public static double somarFaixa(
            double[][] matriz,
            int linhaInicial,
            int linhaFinal) {

        double resultado = 0.0;

        for (int i = linhaInicial; i < linhaFinal; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                resultado += calcular(matriz[i][j]);
            }
        }

        return resultado;
    }
}