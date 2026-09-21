# Projeto AV1 — Programação Paralela, Concorrente e Distribuída

## Integrantes

- Mateus José Galvão de Melo Guimarães
- Gustavo José Magina Eustachio

**Professor:** Rafael Nunes de Lima  
**Disciplina:** Programação Paralela, Concorrente e Distribuída  
**Curso:** Sistemas de Informação — 5º período

---

## 1. Objetivo

Este projeto tem como objetivo comparar diferentes formas de processamento de uma operação computacionalmente custosa aplicada aos elementos de uma matriz.

A partir da implementação sequencial fornecida pelo professor, foram desenvolvidas quatro versões:

- **V1 — Processamento sequencial**
- **V2 — Paralelismo não estruturado**
- **V3 — Paralelismo estruturado**
- **V4 — Paralelismo estruturado com estado compartilhado**

Os experimentos permitem comparar:

- tempo médio de execução;
- speedup;
- efeito da quantidade de tarefas;
- gerenciamento das tarefas;
- sincronização;
- estado compartilhado;
- correção dos resultados.

---

## 2. Estrutura do projeto

```text
Projeto_AV1/
│
├── README.md
├── USO_IA.md
├── resultados-testes.txt
│
└── src/
    └── Main.java
```

O arquivo `resultados-testes.txt` contém o registro bruto das execuções realizadas durante os experimentos.

---

## 3. Problema computacional

Cada elemento da matriz é submetido a uma operação matematicamente custosa.

O cálculo realiza 1000 iterações utilizando funções trigonométricas e raiz quadrada:

```java
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
```

O elevado custo computacional permite observar com maior clareza as diferenças entre processamento sequencial e paralelo.

---

# 4. Implementações

## V1 — Sequencial

A primeira versão corresponde ao processamento sequencial e funciona como **baseline** para os demais experimentos.

Todos os elementos da matriz são processados em sequência por uma única linha de execução.

```text
Matriz
  ↓
Processamento sequencial
  ↓
Resultado
```

O tempo médio da V1 é utilizado como referência para o cálculo do speedup.

---

## V2 — Paralelismo não estruturado

Na segunda versão, a matriz é dividida em diferentes tarefas.

Foram utilizados:

- `ExecutorService`
- `Future`
- `Semaphore`

O `ExecutorService` gerencia a execução das tarefas.

Cada `Future` representa o resultado parcial produzido por uma tarefa.

O `Semaphore` funciona como uma porta de largada, permitindo que as tarefas sejam criadas antes da liberação do processamento.

```text
                    Matriz
                      ↓
              Divisão em tarefas
                      ↓
              ExecutorService
          ┌───────────┼───────────┐
          ↓           ↓           ↓
       Tarefa 1    Tarefa 2    Tarefa N
          ↓           ↓           ↓
        Future      Future      Future
          └───────────┼───────────┘
                      ↓
                   Resultado
```

---

## V3 — Paralelismo estruturado

A terceira versão utiliza a API:

```java
StructuredTaskScope
```

Cada parte da matriz é criada como uma subtarefa através de:

```java
scope.fork(...)
```

Após a criação das subtarefas, o método:

```java
scope.join();
```

aguarda a conclusão das tarefas pertencentes ao escopo.

```text
              Método principal
                     ↓
             StructuredTaskScope
          ┌──────────┼──────────┐
          ↓          ↓          ↓
      Subtarefa 1 Subtarefa 2 Subtarefa N
          └──────────┼──────────┘
                     ↓
                   join()
                     ↓
                  Resultado
```

A principal diferença em relação à V2 é que as tarefas passam a pertencer explicitamente ao mesmo escopo de execução.

---

## V4 — Estado compartilhado

A quarta versão parte da implementação estruturada e adiciona um estado compartilhado.

Foi utilizada a coleção concorrente:

```java
ConcurrentLinkedQueue<Double>
```

Cada subtarefa calcula seu resultado parcial e o adiciona à coleção:

```java
resultados.add(resultadoParcial);
```

Após a conclusão das subtarefas, os resultados são combinados:

```java
return resultados
        .stream()
        .mapToDouble(Double::doubleValue)
        .sum();
```

A estrutura utilizada é:

```text
                  Matriz
                    ↓
            StructuredTaskScope
         ┌──────────┼──────────┐
         ↓          ↓          ↓
       Tarefa 1   Tarefa 2   Tarefa N
         │          │          │
         └──────────┼──────────┘
                    ↓
       ConcurrentLinkedQueue
                    ↓
                 Resultado
```

A utilização de `ConcurrentLinkedQueue` permite que diferentes tarefas atualizem a coleção concorrente sem a necessidade de uma estrutura de bloqueio manual.

---

# 5. Execução do projeto

O projeto utiliza **Java 25**.

Como `StructuredTaskScope` utiliza recurso preview, a execução deve habilitar essa funcionalidade.

No PowerShell:

```powershell
java --enable-preview --source 25 .\src\Main.java
```

---

# 6. Metodologia dos experimentos

Foram utilizadas quatro dimensões de matriz:

- 500 × 500
- 1000 × 1000
- 1500 × 1500
- 2000 × 2000

Para as versões paralelas foram utilizadas:

- 5 tarefas
- 10 tarefas
- 100 tarefas

Cada configuração foi executada **10 vezes**.

Para cada conjunto de execuções foi calculado o tempo médio.

O speedup foi calculado por:

```text
Speedup = Tempo sequencial / Tempo paralelo
```

A versão sequencial foi executada dez vezes para cada tamanho de matriz e utilizada como baseline.

O tempo total necessário para a bateria completa de experimentos foi:

**44,35 minutos.**

---

# 7. Resultados dos Experimentos

Cada configuração foi executada 10 vezes e o valor apresentado corresponde ao tempo médio das execuções.

## Matriz 500 × 500

| Implementação | Tarefas | Tempo médio (ms) | Speedup | Correto |
|---|---:|---:|---:|:---:|
| V1 - Sequencial | 1 | 3494.270 | - | SIM |
| V2 - Não estruturado | 5 | 903.719 | 3.867 | SIM |
| V3 - Estruturado | 5 | 805.347 | 4.339 | SIM |
| V4 - Estado compartilhado | 5 | 786.851 | 4.441 | SIM |
| V2 - Não estruturado | 10 | 569.907 | 6.131 | SIM |
| V3 - Estruturado | 10 | 539.156 | 6.481 | SIM |
| V4 - Estado compartilhado | 10 | 533.430 | 6.551 | SIM |
| V2 - Não estruturado | 100 | 438.412 | 7.970 | SIM |
| V3 - Estruturado | 100 | 438.626 | 7.966 | SIM |
| V4 - Estado compartilhado | 100 | 438.347 | 7.971 | SIM |

### Melhor configuração

- V2: **100 tarefas — 438.412 ms**
- V3: **100 tarefas — 438.626 ms**
- V4: **100 tarefas — 438.347 ms**

---

## Matriz 1000 × 1000

| Implementação | Tarefas | Tempo médio (ms) | Speedup | Correto |
|---|---:|---:|---:|:---:|
| V1 - Sequencial | 1 | 14051.873 | - | SIM |
| V2 - Não estruturado | 5 | 3754.800 | 3.742 | SIM |
| V3 - Estruturado | 5 | 3201.882 | 4.389 | SIM |
| V4 - Estado compartilhado | 5 | 3125.928 | 4.495 | SIM |
| V2 - Não estruturado | 10 | 2163.266 | 6.496 | SIM |
| V3 - Estruturado | 10 | 2059.277 | 6.824 | SIM |
| V4 - Estado compartilhado | 10 | 2100.176 | 6.691 | SIM |
| V2 - Não estruturado | 100 | 1753.959 | 8.012 | SIM |
| V3 - Estruturado | 100 | 1754.508 | 8.009 | SIM |
| V4 - Estado compartilhado | 100 | 1750.761 | 8.026 | SIM |

### Melhor configuração

- V2: **100 tarefas — 1753.959 ms**
- V3: **100 tarefas — 1754.508 ms**
- V4: **100 tarefas — 1750.761 ms**

---

## Matriz 1500 × 1500

| Implementação | Tarefas | Tempo médio (ms) | Speedup | Correto |
|---|---:|---:|---:|:---:|
| V1 - Sequencial | 1 | 31493.399 | - | SIM |
| V2 - Não estruturado | 5 | 8151.439 | 3.864 | SIM |
| V3 - Estruturado | 5 | 7162.746 | 4.397 | SIM |
| V4 - Estado compartilhado | 5 | 7075.497 | 4.451 | SIM |
| V2 - Não estruturado | 10 | 4782.449 | 6.585 | SIM |
| V3 - Estruturado | 10 | 4612.125 | 6.828 | SIM |
| V4 - Estado compartilhado | 10 | 4510.464 | 6.982 | SIM |
| V2 - Não estruturado | 100 | 3953.970 | 7.965 | SIM |
| V3 - Estruturado | 100 | 3952.566 | 7.968 | SIM |
| V4 - Estado compartilhado | 100 | 3958.554 | 7.956 | SIM |

### Melhor configuração

- V2: **100 tarefas — 3953.970 ms**
- V3: **100 tarefas — 3952.566 ms**
- V4: **100 tarefas — 3958.554 ms**

---

## Matriz 2000 × 2000

| Implementação | Tarefas | Tempo médio (ms) | Speedup | Correto |
|---|---:|---:|---:|:---:|
| V1 - Sequencial | 1 | 56030.365 | - | SIM |
| V2 - Não estruturado | 5 | 14264.509 | 3.928 | SIM |
| V3 - Estruturado | 5 | 13096.662 | 4.278 | SIM |
| V4 - Estado compartilhado | 5 | 12917.985 | 4.337 | SIM |
| V2 - Não estruturado | 10 | 8304.731 | 6.747 | SIM |
| V3 - Estruturado | 10 | 8043.425 | 6.966 | SIM |
| V4 - Estado compartilhado | 10 | 7941.499 | 7.055 | SIM |
| V2 - Não estruturado | 100 | 7087.692 | 7.905 | SIM |
| V3 - Estruturado | 100 | 7041.512 | 7.957 | SIM |
| V4 - Estado compartilhado | 100 | 7023.127 | 7.978 | SIM |

### Melhor configuração

- V2: **100 tarefas — 7087.692 ms**
- V3: **100 tarefas — 7041.512 ms**
- V4: **100 tarefas — 7023.127 ms**

---

# 8. Análise dos resultados

Os experimentos demonstraram uma diferença significativa entre o processamento sequencial e as versões paralelas.

A versão sequencial apresentou crescimento aproximadamente proporcional à quantidade de elementos processados.

Na matriz 500 × 500, o tempo médio sequencial foi:

```text
3494.270 ms
```

Na matriz 2000 × 2000, esse tempo aumentou para:

```text
56030.365 ms
```

As versões paralelas reduziram consideravelmente o tempo de processamento.

---

## Efeito da quantidade de tarefas

A utilização de **5 tarefas** produziu speedups aproximadamente entre 3,7 e 4,5.

Com **10 tarefas**, os speedups aumentaram para aproximadamente 6,1 a 7,1.

Com **100 tarefas**, os resultados ficaram próximos de um speedup de 8.

Nos experimentos realizados, **100 tarefas apresentou o menor tempo médio em V2, V3 e V4 para todos os tamanhos de matriz avaliados**.

---

## Comparação entre V2, V3 e V4

Com apenas 5 tarefas, as versões estruturadas V3 e V4 apresentaram vantagem em relação à V2 na maior parte dos testes.

Com o aumento da quantidade de tarefas, as diferenças entre as três implementações diminuíram.

Com 100 tarefas, V2, V3 e V4 apresentaram tempos bastante próximos.

Isso mostra que, nesse ambiente de execução, o ganho de desempenho passa a se estabilizar quando o número de tarefas é aumentado.

---

## Maior speedup observado

O maior speedup obtido durante os experimentos foi:

```text
8.026
```

Esse resultado ocorreu com:

```text
Matriz: 1000 × 1000
Implementação: V4 - Estado compartilhado
Tarefas: 100
Tempo médio: 1750.761 ms
```

---

## Correção dos resultados

Todas as configurações apresentaram:

```text
Correto = SIM
```

Isso significa que as versões paralelas produziram resultados equivalentes ao baseline sequencial, considerando a tolerância utilizada para operações com valores do tipo `double`.

---

# 9. Deadlock

Não foi identificado cenário de deadlock nas implementações.

Na V2 existe apenas um `Semaphore` utilizado como mecanismo de liberação inicial das tarefas.

As tarefas:

1. aguardam uma permissão;
2. recebem a permissão;
3. executam seu processamento;
4. terminam.

Não existem múltiplos recursos sendo adquiridos em ordens diferentes e não existe espera circular entre tarefas.

Nas versões V3 e V4, o ciclo de vida das subtarefas é controlado pelo `StructuredTaskScope`.

---

# 10. Livelock

Não existem mecanismos de repetição nos quais duas ou mais tarefas alterem continuamente seu comportamento em resposta umas às outras.

As tarefas recebem uma faixa definida da matriz, executam o cálculo e encerram.

Dessa forma, não existe situação de livelock no fluxo implementado.

---

# 11. Starvation

Na V2 foi utilizado:

```java
new Semaphore(0, true);
```

O parâmetro `true` habilita uma política justa de concessão das permissões entre as threads que aguardam no semáforo.

Além disso:

- todas as tarefas recebem quantidade finita de trabalho;
- todas as permissões necessárias são liberadas;
- não existem tarefas de prioridade diferente;
- cada tarefa termina após processar sua parte da matriz.

Nas versões estruturadas, todas as subtarefas pertencem ao mesmo `StructuredTaskScope` e são aguardadas através do `join()`.

Assim, o fluxo implementado não apresenta uma condição estrutural que mantenha permanentemente uma tarefa sem possibilidade de progresso.

---

# 12. Conclusão

Os resultados demonstraram claramente o benefício do paralelismo para o problema proposto.

A implementação sequencial apresentou os maiores tempos de execução em todos os tamanhos avaliados.

A divisão do processamento em tarefas permitiu reduzir significativamente o tempo necessário para processar as matrizes.

Os experimentos também mostraram que aumentar a quantidade de tarefas de 5 para 10 e posteriormente para 100 aumentou o desempenho no ambiente utilizado.

A configuração com 100 tarefas apresentou os melhores tempos médios para todas as versões paralelas.

Entretanto, V2, V3 e V4 apresentam diferenças conceituais importantes.

A V2 exige gerenciamento explícito do executor, dos resultados e da sincronização.

A V3 organiza as subtarefas dentro de um escopo estruturado, tornando a relação entre criação, execução e término das tarefas mais explícita.

A V4 acrescenta estado compartilhado utilizando uma coleção concorrente, demonstrando como múltiplas tarefas podem registrar seus resultados de maneira segura.

Assim, o projeto permitiu observar não apenas ganhos de desempenho, mas também diferentes estratégias de organização e sincronização do processamento concorrente.

---

# 13. Registro completo dos experimentos

As dez execuções realizadas para cada configuração estão disponíveis no arquivo:

```text
resultados-testes.txt
```

O arquivo foi mantido no repositório para permitir a consulta aos tempos individuais utilizados no cálculo das médias.

---
