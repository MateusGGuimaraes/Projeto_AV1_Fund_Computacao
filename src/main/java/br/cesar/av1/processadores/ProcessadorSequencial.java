package br.cesar.av1.processadores;

import br.cesar.av1.core.CalculadoraPesada;
import br.cesar.av1.core.Processador;
import br.cesar.av1.core.ResultadoProcessamento;

public class ProcessadorSequencial implements Processador {

    @Override
    public String nome() {
        return "V1 - Sequencial";
    }

    @Override
    public ResultadoProcessamento processar(
            double[][] matriz,
            int quantidadeTarefas) {

        double resultado =
                CalculadoraPesada.somarFaixa(
                        matriz,
                        0,
                        matriz.length);

        int elementos = matriz.length * matriz[0].length;

        return new ResultadoProcessamento(
                resultado,
                elementos);
    }
}