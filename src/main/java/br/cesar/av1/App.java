package br.cesar.av1;

import br.cesar.av1.core.MatrizFactory;
import br.cesar.av1.core.Processador;
import br.cesar.av1.core.ResultadoProcessamento;
import br.cesar.av1.processadores.ProcessadorNaoEstruturado;
import br.cesar.av1.processadores.ProcessadorSequencial;

public class App {

    public static void main(String[] args) throws Exception {

        String modo =
                args.length > 0
                        ? args[0]
                        : "seq";

        int tamanho =
                args.length > 1
                        ? Integer.parseInt(args[1])
                        : 500;

        int tarefas =
                args.length > 2
                        ? Integer.parseInt(args[2])
                        : 5;

        double[][] matriz =
                MatrizFactory.gerar(tamanho);

        Processador processador =
                switch (modo.toLowerCase()) {

                    case "seq" ->
                            new ProcessadorSequencial();

                    case "nao-estruturado" ->
                            new ProcessadorNaoEstruturado();

                    default ->
                            throw new IllegalArgumentException(
                                    "Modo invalido: " + modo);
                };

        long inicio = System.nanoTime();

        ResultadoProcessamento resultado =
                processador.processar(
                        matriz,
                        tarefas);

        long fim = System.nanoTime();

        double tempoMs =
                (fim - inicio) / 1_000_000.0;

        System.out.println(
                "Implementacao: "
                + processador.nome());

        System.out.println(
                "Resultado: "
                + resultado.resultado());

        System.out.println(
                "Elementos processados: "
                + resultado.elementosProcessados());

        System.out.printf(
                "Tempo: %.3f ms%n",
                tempoMs);
    }
}