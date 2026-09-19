import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;


/*
 * ============================================================
 * V1 - IMPLEMENTAÇÃO SEQUENCIAL
 * ============================================================
 *
 * Código base fornecido pelo professor.
 *
 * Esta implementação funciona como baseline para comparação
 * com as versões paralelas.
 */
private static double calcular(double valor) {

    double resultado = valor;

    for (int i = 0; i < 1000; i++) {

        resultado +=
                Math.sin(valor + i)
                        * Math.cos(valor - i)
                        * Math.sqrt(Math.abs(valor) + 1);
    }

    return resultado;
}


private static double processar(double[][] matriz) {

    double resultado = 0.0;

    for (int i = 0; i < matriz.length; i++) {

        for (int j = 0; j < matriz[i].length; j++) {

            resultado += calcular(matriz[i][j]);
        }
    }

    return resultado;
}


/*
 * Cria a mesma matriz determinística utilizada
 * na implementação original do professor.
 *
 * Assim todas as versões recebem exatamente
 * os mesmos dados.
 */
private static double[][] gerarMatriz(
        int linhas,
        int colunas) {

    double[][] matriz =
            new double[linhas][colunas];

    for (int i = 0; i < linhas; i++) {

        for (int j = 0; j < colunas; j++) {

            int valorBase =
                    ((i + 1) * 31
                            + (j + 1) * 17)
                            % 100;

            matriz[i][j] =
                    (valorBase + 1)
                            / 10000.0;
        }
    }

    return matriz;
}


/*
 * ============================================================
 * PROCESSAMENTO DE UMA PARTE DA MATRIZ
 * ============================================================
 *
 * Utilizado pelas tarefas da versão paralela.
 */
private static double processarFaixa(
        double[][] matriz,
        int linhaInicial,
        int linhaFinal) {

    double resultado = 0.0;

    for (int i = linhaInicial;
         i < linhaFinal;
         i++) {

        for (int j = 0;
             j < matriz[i].length;
             j++) {

            resultado +=
                    calcular(matriz[i][j]);
        }
    }

    return resultado;
}


/*
 * ============================================================
 * V2 - PARALELISMO NÃO ESTRUTURADO
 * ============================================================
 *
 * A matriz é dividida em N tarefas.
 *
 * ExecutorService:
 * gerencia as threads responsáveis pelas tarefas.
 *
 * Future:
 * representa o resultado de cada tarefa.
 *
 * Semaphore:
 * funciona como uma porta de largada.
 * Todas as tarefas são criadas antes da liberação.
 */
private static double processarNaoEstruturado(
        double[][] matriz,
        int quantidadeTarefas)
        throws InterruptedException,
        ExecutionException {

    int quantidadeThreads =
            Math.min(
                    quantidadeTarefas,
                    Runtime.getRuntime()
                            .availableProcessors());

    ExecutorService executor =
            Executors.newFixedThreadPool(
                    quantidadeThreads);

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

                        return processarFaixa(
                                matriz,
                                linhaInicial,
                                linhaFinal);
                    });

            futuros.add(futuro);
        }

        /*
         * Depois que todas as tarefas foram criadas,
         * libera um permit para cada uma.
         */
        inicio.release(
                quantidadeTarefas);

        double resultado = 0.0;

        for (Future<Double> futuro : futuros) {

            resultado += futuro.get();
        }

        return resultado;

    } finally {

        executor.shutdown();
    }
}


/*
 * Os resultados paralelos podem apresentar diferenças
 * muito pequenas devido à ordem das somas com double.
 */
private static boolean resultadosEquivalentes(
        double esperado,
        double obtido) {

    double tolerancia =
            Math.max(
                    1e-8,
                    Math.abs(esperado)
                            * 1e-9);

    return Math.abs(
            esperado - obtido)
            <= tolerancia;
}


/*
 * ============================================================
 * EXECUÇÃO DA PARTE 1
 * ============================================================
 *
 * Compara:
 *
 * V1 - Sequencial
 * V2 - Paralelismo não estruturado
 */
private static void executarProcessamento(
        int linhas,
        int colunas,
        int quantidadeTarefas)
        throws Exception {

    System.out.println();
    System.out.println(
            "==========================================");
    System.out.println(
            "       PROCESSAMENTO DA MATRIZ");
    System.out.println(
            "==========================================");

    System.out.println(
            "Matriz: "
                    + linhas
                    + " x "
                    + colunas);

    System.out.println(
            "Quantidade de tarefas: "
                    + quantidadeTarefas);

    long quantidadeElementos =
            (long) linhas * colunas;

    System.out.println(
            "Elementos: "
                    + quantidadeElementos);

    System.out.println(
            "Gerando matriz...");

    double[][] matriz =
            gerarMatriz(
                    linhas,
                    colunas);

    System.out.println(
            "Matriz criada.");

    /*
     * ========================================================
     * V1 - SEQUENCIAL
     * ========================================================
     */

    long inicioSequencial =
            System.nanoTime();

    double resultadoSequencial =
            processar(matriz);

    long fimSequencial =
            System.nanoTime();

    double tempoSequencial =
            (fimSequencial
                    - inicioSequencial)
                    / 1_000_000.0;


    /*
     * ========================================================
     * V2 - NÃO ESTRUTURADO
     * ========================================================
     */

    long inicioParalelo =
            System.nanoTime();

    double resultadoParalelo =
            processarNaoEstruturado(
                    matriz,
                    quantidadeTarefas);

    long fimParalelo =
            System.nanoTime();

    double tempoParalelo =
            (fimParalelo
                    - inicioParalelo)
                    / 1_000_000.0;


    double speedup =
            tempoSequencial
                    / tempoParalelo;


    System.out.println();
    System.out.println(
            "------------------------------------------");

    System.out.printf(
            "V1 - Sequencial: %.3f ms%n",
            tempoSequencial);

    System.out.printf(
            "V2 - Não estruturado: %.3f ms%n",
            tempoParalelo);

    System.out.printf(
            "Speedup: %.3f%n",
            speedup);

    System.out.println(
            "Resultado correto: "
                    + (resultadosEquivalentes(
                    resultadoSequencial,
                    resultadoParalelo)
                    ? "SIM"
                    : "NÃO"));

    System.out.println(
            "------------------------------------------");

    System.out.printf(
            "Resultado sequencial: %.6f%n",
            resultadoSequencial);

    System.out.printf(
            "Resultado paralelo:   %.6f%n",
            resultadoParalelo);

    System.out.println();
}


/*
 * Permite somente as três quantidades sugeridas
 * no projeto da disciplina.
 */
private static int lerQuantidadeTarefas(
        Scanner scanner) {

    while (true) {

        System.out.println();
        System.out.print(
                "Quantidade de tarefas "
                        + "(5, 10 ou 100): ");

        int quantidade =
                scanner.nextInt();

        if (quantidade == 5
                || quantidade == 10
                || quantidade == 100) {

            return quantidade;
        }

        System.out.println(
                "Valor inválido. "
                        + "Escolha 5, 10 ou 100.");
    }
}


/*
 * ============================================================
 * MENU ORIGINAL DO PROJETO
 * ============================================================
 */
private static void exibirMenu() {

    System.out.println();
    System.out.println(
            "==========================================");
    System.out.println(
            "      PROJETO DE COMPUTAÇÃO PARALELA");
    System.out.println(
            "==========================================");

    System.out.println(
            "1 - Matriz 500 x 500");

    System.out.println(
            "2 - Matriz 1000 x 1000");

    System.out.println(
            "3 - Matriz 1500 x 1500");

    System.out.println(
            "4 - Matriz 2000 x 2000");

    System.out.println(
            "0 - Sair");

    System.out.println(
            "==========================================");

    System.out.print(
            "Escolha uma opção: ");
}


/*
 * ============================================================
 * MAIN
 * ============================================================
 */
public static void main(String[] args)
        throws Exception {

    Scanner scanner =
            new Scanner(System.in);

    int opcao;

    do {

        exibirMenu();

        opcao =
                scanner.nextInt();

        switch (opcao) {

            case 1 -> {

                int tarefas =
                        lerQuantidadeTarefas(
                                scanner);

                executarProcessamento(
                        500,
                        500,
                        tarefas);
            }

            case 2 -> {

                int tarefas =
                        lerQuantidadeTarefas(
                                scanner);

                executarProcessamento(
                        1000,
                        1000,
                        tarefas);
            }

            case 3 -> {

                int tarefas =
                        lerQuantidadeTarefas(
                                scanner);

                executarProcessamento(
                        1500,
                        1500,
                        tarefas);
            }

            case 4 -> {

                int tarefas =
                        lerQuantidadeTarefas(
                                scanner);

                executarProcessamento(
                        2000,
                        2000,
                        tarefas);
            }

            case 0 -> {

                System.out.println();
                System.out.println(
                        "Encerrando o programa...");
            }

            default -> {

                System.out.println();
                System.out.println(
                        "Opção inválida!");
            }
        }

    } while (opcao != 0);

    scanner.close();

    System.out.println(
            "Programa encerrado.");
}
