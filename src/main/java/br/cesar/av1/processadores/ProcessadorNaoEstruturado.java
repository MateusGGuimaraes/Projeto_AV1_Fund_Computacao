package br.cesar.av1.processadores;

import br.cesar.av1.core.CalculadoraPesada;
import br.cesar.av1.core.Processador;
import br.cesar.av1.core.ResultadoProcessamento;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ProcessadorNaoEstruturado implements Processador {

    @Override
    public String nome() {
        return "V2 - Paralelismo nao estruturado";
    }

    @Override
    public ResultadoProcessamento processar(
            double[][] matriz,
            int quantidadeTarefas) throws Exception {

        if (quantidadeTarefas <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de tarefas deve ser maior que zero.");
        }

        int processadores =
                Runtime.getRuntime().availableProcessors();

        int tamanhoPool =
                Math.max(
                        1,
                        Math.min(
                                quantidadeTarefas,
                                processadores));

        ExecutorService executor =
                Executors.newFixedThreadPool(tamanhoPool);

        /*
         * Semaphore usado como portao de inicio.
         * Todas as tarefas sao criadas antes de serem liberadas.
         *
         * O parametro true habilita politica justa (fair).
         */
        Semaphore inicio =
                new Semaphore(0, true);

        List<Future<Double>> futuros =
                new ArrayList<>();

        try {

            for (int tarefa = 0;
                 tarefa < quantidadeTarefas;
                 tarefa++) {

                int linhaInicial =
                        tarefa
                        * matriz.length
                        / quantidadeTarefas;

                int linhaFinal =
                        (tarefa + 1)
                        * matriz.length
                        / quantidadeTarefas;

                Future<Double> futuro =
                        executor.submit(() -> {

                            inicio.acquire();

                            return CalculadoraPesada.somarFaixa(
                                    matriz,
                                    linhaInicial,
                                    linhaFinal);
                        });

                futuros.add(futuro);
            }

            /*
             * Libera todas as tarefas somente depois
             * que elas tiverem sido submetidas.
             */
            inicio.release(quantidadeTarefas);

            double resultadoFinal = 0.0;

            for (Future<Double> futuro : futuros) {
                resultadoFinal += futuro.get();
            }

            int elementos =
                    matriz.length * matriz[0].length;

            return new ResultadoProcessamento(
                    resultadoFinal,
                    elementos);

        } finally {

            executor.shutdown();

            try {

                if (!executor.awaitTermination(
                        10,
                        TimeUnit.SECONDS)) {

                    executor.shutdownNow();
                }

            } catch (InterruptedException e) {

                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}