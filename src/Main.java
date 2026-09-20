import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;


/*
 * Quantidade de execuções utilizada para
 * calcular o tempo médio.
 */
private static final int REPETICOES = 10;


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

            resultado +=
                    calcular(matriz[i][j]);
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

        for (int j = 0;
             j < colunas;
             j++) {

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
 * Utiliza:
 *
 * - ExecutorService
 * - Threads
 * - Future
 * - Semaphore
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

            resultado +=
                    futuro.get();
        }

        return resultado;

    } finally {

        executor.shutdown();
    }
}


/*
 * ============================================================
 * V3 - PARALELISMO ESTRUTURADO
 * ============================================================
 *
 * Utiliza StructuredTaskScope.
 *
 * Cada parte da matriz é processada por uma
 * subtarefa criada com fork().
 *
 * join() aguarda a conclusão das subtarefas.
 */
private static double processarEstruturado(
        double[][] matriz,
        int quantidadeTarefas)
        throws InterruptedException {

    try (var scope =
                 StructuredTaskScope
                         .<Double>open()) {

        List<StructuredTaskScope.Subtask<Double>>
                subtarefas =
                new ArrayList<>();

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

            var subtarefa =
                    scope.fork(() ->
                            processarFaixa(
                                    matriz,
                                    linhaInicial,
                                    linhaFinal));

            subtarefas.add(
                    subtarefa);
        }

        scope.join();

        double resultado = 0.0;

        for (var subtarefa : subtarefas) {

            resultado +=
                    subtarefa.get();
        }

        return resultado;
    }
}


/*
 * ============================================================
 * V4 - ESTADO COMPARTILHADO
 * ============================================================
 *
 * Parte da solução estruturada.
 *
 * As subtarefas compartilham uma
 * ConcurrentLinkedQueue.
 *
 * Cada uma adiciona seu resultado parcial
 * à coleção concorrente.
 */
private static double processarEstadoCompartilhado(
        double[][] matriz,
        int quantidadeTarefas)
        throws InterruptedException {

    ConcurrentLinkedQueue<Double>
            resultados =
            new ConcurrentLinkedQueue<>();

    try (var scope =
                 StructuredTaskScope
                         .<Void>open()) {

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

            scope.fork(() -> {

                double resultadoParcial =
                        processarFaixa(
                                matriz,
                                linhaInicial,
                                linhaFinal);

                resultados.add(
                        resultadoParcial);

                return null;
            });
        }

        scope.join();
    }

    return resultados
            .stream()
            .mapToDouble(
                    Double::doubleValue)
            .sum();
}


/*
 * ============================================================
 * MEDIÇÃO DOS EXPERIMENTOS
 * ============================================================
 *
 * Cada implementação é executada dez vezes.
 *
 * Retorno:
 *
 * [0] = resultado do processamento
 * [1] = tempo médio em milissegundos
 */
private static double[] medir(
        Callable<Double> processamento)
        throws Exception {

    double resultado = 0.0;

    double tempoTotal = 0.0;

    for (int i = 0;
         i < REPETICOES;
         i++) {

        long inicio =
                System.nanoTime();

        resultado =
                processamento.call();

        long fim =
                System.nanoTime();

        tempoTotal +=
                (fim - inicio)
                        / 1_000_000.0;
    }

    double tempoMedio =
            tempoTotal
                    / REPETICOES;

    return new double[]{
            resultado,
            tempoMedio
    };
}


/*
 * Os resultados paralelos podem apresentar diferenças
 * muito pequenas devido à ordem das somas com double.
 *
 * Por isso utilizamos uma pequena tolerância.
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
 * Exibe uma linha da tabela final.
 */
private static void exibirResultado(
        String nome,
        double[] medicao,
        double tempoSequencial,
        double resultadoSequencial) {

    double speedup =
            tempoSequencial
                    / medicao[1];

    String correto =
            resultadosEquivalentes(
                    resultadoSequencial,
                    medicao[0])
                    ? "SIM"
                    : "NÃO";

    System.out.printf(
            "%-28s %15.3f %12.3f %12s%n",
            nome,
            medicao[1],
            speedup,
            correto);
}


/*
 * ============================================================
 * EXECUÇÃO DO EXPERIMENTO
 * ============================================================
 *
 * Compara:
 *
 * V1 - Sequencial
 * V2 - Paralelismo não estruturado
 * V3 - Paralelismo estruturado
 * V4 - Estado compartilhado
 */
private static void executarProcessamento(
        int linhas,
        int colunas,
        int quantidadeTarefas)
        throws Exception {

    System.out.println();
    System.out.println(
            "==============================================");
    System.out.println(
            "              EXPERIMENTO");
    System.out.println(
            "==============================================");

    System.out.println(
            "Matriz: "
                    + linhas
                    + " x "
                    + colunas);

    System.out.println(
            "Tarefas: "
                    + quantidadeTarefas);

    System.out.println(
            "Repetições: "
                    + REPETICOES);

    long quantidadeElementos =
            (long) linhas
                    * colunas;

    System.out.println(
            "Elementos: "
                    + quantidadeElementos);

    System.out.println();
    System.out.println(
            "Gerando matriz...");

    /*
     * A geração da matriz ocorre antes
     * da medição.
     */
    double[][] matriz =
            gerarMatriz(
                    linhas,
                    colunas);

    System.out.println(
            "Executando V1...");

    double[] sequencial =
            medir(() ->
                    processar(matriz));

    System.out.println(
            "Executando V2...");

    double[] naoEstruturado =
            medir(() ->
                    processarNaoEstruturado(
                            matriz,
                            quantidadeTarefas));

    System.out.println(
            "Executando V3...");

    double[] estruturado =
            medir(() ->
                    processarEstruturado(
                            matriz,
                            quantidadeTarefas));

    System.out.println(
            "Executando V4...");

    double[] compartilhado =
            medir(() ->
                    processarEstadoCompartilhado(
                            matriz,
                            quantidadeTarefas));


    System.out.println();
    System.out.println(
            "==============================================================");

    System.out.printf(
            "%-28s %15s %12s %12s%n",
            "Implementação",
            "Tempo médio",
            "Speedup",
            "Correto");

    System.out.println(
            "--------------------------------------------------------------");


    /*
     * Sequencial.
     *
     * Speedup = 1 porque é o baseline.
     */
    System.out.printf(
            "%-28s %15.3f %12s %12s%n",
            "V1 - Sequencial",
            sequencial[1],
            "-",
            "SIM");


    exibirResultado(
            "V2 - Não estruturado",
            naoEstruturado,
            sequencial[1],
            sequencial[0]);


    exibirResultado(
            "V3 - Estruturado",
            estruturado,
            sequencial[1],
            sequencial[0]);


    exibirResultado(
            "V4 - Estado compartilhado",
            compartilhado,
            sequencial[1],
            sequencial[0]);


    System.out.println(
            "==============================================================");

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
