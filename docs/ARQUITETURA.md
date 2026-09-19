# Arquitetura do Projeto

O projeto processa uma matriz de números utilizando diferentes
estratégias de execução.

## Evolução

```mermaid
flowchart TD
    A[Matriz] --> B{Implementação}

    B --> C[V1 - Sequencial]
    B --> D[V2 - Não Estruturado]

    C --> E[Processamento elemento a elemento]

    D --> F[ExecutorService]
    F --> G[Tarefas]
    G --> H[Future]
    H --> I[Resultado Final]