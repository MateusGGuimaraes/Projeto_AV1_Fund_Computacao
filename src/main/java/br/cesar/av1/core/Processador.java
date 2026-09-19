package br.cesar.av1.core;

public interface Processador {

    String nome();

    ResultadoProcessamento processar(
            double[][] matriz,
            int quantidadeTarefas) throws Exception;
}