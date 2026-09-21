# Projeto AV1 - Programação Paralela, Concorrente e Distribuída

Projeto desenvolvido para a avaliação da disciplina de
**Programação Paralela, Concorrente e Distribuída**.

## Integrantes

- Mateus José Galvão de Melo Guimarães
- Gustavo José Magina Eustachio

## Objetivo

Comparar diferentes estratégias de processamento de uma matriz
computacionalmente custosa, evoluindo de uma solução sequencial
para soluções concorrentes e paralelas.

O projeto parte da implementação sequencial fornecida pelo professor e demonstra a evolução do processamento de uma matriz utilizando diferentes estratégias de concorrência e paralelismo.

O mesmo problema é resolvido através de quatro versões:

1. V1 - Processamento Sequencial
2. V2 - Paralelismo Não Estruturado
3. V3 - Paralelismo Estruturado
4. V4 - Estado Compartilhado

---

## V1 - Processamento Sequencial

A implementação sequencial fornecida pelo professor é utilizada como baseline.

A matriz é percorrida elemento por elemento e cada valor é submetido à função `calcular()`.

---

## V2 - Paralelismo Não Estruturado

A matriz é dividida em N partes.

Cada tarefa fica responsável por um intervalo de linhas.

São utilizados:

- `ExecutorService`
- pool de threads
- `Future`
- `Semaphore`

O `Semaphore` funciona como uma porta de largada: as tarefas são criadas e posteriormente liberadas para execução.

---

## V3 - Paralelismo Estruturado

A mesma divisão da matriz é realizada utilizando:

- `StructuredTaskScope`
- `fork()`
- `join()`
- `Subtask`

As subtarefas pertencem ao mesmo escopo e a thread principal aguarda sua conclusão utilizando `join()`.

---

## V4 - Estado Compartilhado

A quarta versão parte da implementação estruturada e adiciona um estado compartilhado.

É utilizada:

```java
ConcurrentLinkedQueue<Double>
```

Cada subtarefa adiciona seu resultado parcial à mesma coleção concorrente.

Após a conclusão das subtarefas, os resultados são somados pela aplicação principal.

---

# Arquitetura

```mermaid
flowchart TD

    A[Matriz] --> B[V1 - Sequencial]

    A --> C[V2 - Não Estruturado]
    C --> D[ExecutorService]
    D --> E[Tarefas]
    E --> F[Future]
    F --> R[Resultado]

    A --> G[V3 - Estruturado]
    G --> H[StructuredTaskScope]
    H --> I[fork]
    I --> J[join]
    J --> R

    A --> K[V4 - Estado Compartilhado]
    K --> L[StructuredTaskScope]
    L --> M[ConcurrentLinkedQueue]
    M --> R

    B --> R
```

---

# Experimentos

O projeto utiliza os seguintes tamanhos de matriz:

- 500 x 500
- 1000 x 1000
- 1500 x 1500
- 2000 x 2000

As versões paralelas podem ser executadas com:

- 5 tarefas
- 10 tarefas
- 100 tarefas

Cada implementação é executada **10 vezes**.

O programa apresenta o tempo médio das dez execuções.

---

# Speedup

O speedup é calculado através da fórmula:

```text
Speedup = Tempo médio sequencial / Tempo médio paralelo
```

Valores maiores que `1` indicam que a versão paralela foi mais rápida que o baseline sequencial.

---

# Validação do resultado

Todas as versões utilizam exatamente a mesma matriz.

O resultado das versões paralelas é comparado ao resultado da versão sequencial.

Como a ordem das operações de soma com `double` pode variar em processamento paralelo, a comparação utiliza uma pequena tolerância numérica.

---

# Deadlock

A implementação não utiliza múltiplos locks adquiridos em diferentes ordens.

Na V2, cada tarefa realiza somente um `acquire()` no `Semaphore`.

A thread principal libera um permit para cada tarefa antes de aguardar seus resultados.

Dessa maneira, não existe dependência circular entre recursos.

Na V3 e na V4, as subtarefas estão delimitadas pelo mesmo `StructuredTaskScope`.

---

# Livelock

Não existe mecanismo em que duas tarefas fiquem continuamente modificando seu comportamento em resposta uma à outra.

Também não são utilizados ciclos de tentativa e liberação de locks.

---

# Starvation

Na V2 o `Semaphore` é criado utilizando política justa:

```java
new Semaphore(0, true)
```

Além disso, existe um permit para cada tarefa.

Todas as tarefas executam uma quantidade finita de processamento.

Na V4 é utilizada uma `ConcurrentLinkedQueue`, evitando a necessidade de gerenciamento manual de um lock para atualização da coleção compartilhada.

---

# Como executar

## Requisitos

- Java JDK 25
- IntelliJ IDEA ou terminal
- Preview Features habilitadas para `StructuredTaskScope`

Pelo terminal:

```bash
java --enable-preview --source 25 src/Main.java
```

O programa solicitará primeiro o tamanho da matriz:

```text
1 - Matriz 500 x 500
2 - Matriz 1000 x 1000
3 - Matriz 1500 x 1500
4 - Matriz 2000 x 2000
0 - Sair
```

Em seguida:

```text
Quantidade de tarefas (5, 10 ou 100):
```

O programa executará as quatro versões 10 vezes e apresentará:

- tempo médio;
- speedup;
- validação do resultado.

---

## Resultados dos Experimentos

Cada configuração foi executada 10 vezes e o valor apresentado
corresponde ao tempo médio das execuções.

| Matriz | Implementação | Tarefas | Tempo médio (ms) | Speedup | Correto |
|---|---|---:|---:|---:|---|
| 500x500 | V1 - Sequencial | 1 | 3494.270 | - | SIM |
| 500x500 | V2 - Não estruturado | 5 | 903.719 | 3.867 | SIM |
| 500x500 | V3 - Estruturado | 5 | 805.347 | 4.339 | SIM |
| 500x500 | V4 - Estado compartilhado | 5 | 786.851 | 4.441 | SIM |
| 500x500 | V2 - Não estruturado | 10 | 569.907 | 6.131 | SIM |
| 500x500 | V3 - Estruturado | 10 | 539.156 | 6.481 | SIM |
| 500x500 | V4 - Estado compartilhado | 10 | 533.430 | 6.551 | SIM |
| 500x500 | V2 - Não estruturado | 100 | 438.412 | 7.970 | SIM |
| 500x500 | V3 - Estruturado | 100 | 438.626 | 7.966 | SIM |
| 500x500 | V4 - Estado compartilhado | 100 | 438.347 | 7.971 | SIM |

| 1000x1000 | V1 - Sequencial | 1 | 14051.873 | - | SIM |
| 1000x1000 | V2 - Não estruturado | 5 | 3754.800 | 3.742 | SIM |
| 1000x1000 | V3 - Estruturado | 5 | 3201.882 | 4.389 | SIM |
| 1000x1000 | V4 - Estado compartilhado | 5 | 3125.928 | 4.495 | SIM |
| 1000x1000 | V2 - Não estruturado | 10 | 2163.266 | 6.496 | SIM |
| 1000x1000 | V3 - Estruturado | 10 | 2059.277 | 6.824 | SIM |
| 1000x1000 | V4 - Estado compartilhado | 10 | 2100.176 | 6.691 | SIM |
| 1000x1000 | V2 - Não estruturado | 100 | 1753.959 | 8.012 | SIM |
| 1000x1000 | V3 - Estruturado | 100 | 1754.508 | 8.009 | SIM |
| 1000x1000 | V4 - Estado compartilhado | 100 | 1750.761 | 8.026 | SIM |

| 1500x1500 | V1 - Sequencial | 1 | 31493.399 | - | SIM |
| 1500x1500 | V2 - Não estruturado | 5 | 8151.439 | 3.864 | SIM |
| 1500x1500 | V3 - Estruturado | 5 | 7162.746 | 4.397 | SIM |
| 1500x1500 | V4 - Estado compartilhado | 5 | 7075.497 | 4.451 | SIM |
| 1500x1500 | V2 - Não estruturado | 10 | 4782.449 | 6.585 | SIM |
| 1500x1500 | V3 - Estruturado | 10 | 4612.125 | 6.828 | SIM |
| 1500x1500 | V4 - Estado compartilhado | 10 | 4510.464 | 6.982 | SIM |
| 1500x1500 | V2 - Não estruturado | 100 | 3953.970 | 7.965 | SIM |
| 1500x1500 | V3 - Estruturado | 100 | 3952.566 | 7.968 | SIM |
| 1500x1500 | V4 - Estado compartilhado | 100 | 3958.554 | 7.956 | SIM |

| 2000x2000 | V1 - Sequencial | 1 | 56030.365 | - | SIM |
| 2000x2000 | V2 - Não estruturado | 5 | 14264.509 | 3.928 | SIM |
| 2000x2000 | V3 - Estruturado | 5 | 13096.662 | 4.278 | SIM |
| 2000x2000 | V4 - Estado compartilhado | 5 | 12917.985 | 4.337 | SIM |
| 2000x2000 | V2 - Não estruturado | 10 | 8304.731 | 6.747 | SIM |
| 2000x2000 | V3 - Estruturado | 10 | 8043.425 | 6.966 | SIM |
| 2000x2000 | V4 - Estado compartilhado | 10 | 7941.499 | 7.055 | SIM |
| 2000x2000 | V2 - Não estruturado | 100 | 7087.692 | 7.905 | SIM |
| 2000x2000 | V3 - Estruturado | 100 | 7041.512 | 7.957 | SIM |
| 2000x2000 | V4 - Estado compartilhado | 100 | 7023.127 | 7.978 | SIM |
