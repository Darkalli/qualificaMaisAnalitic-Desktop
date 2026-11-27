# Qualifica+ Ijuí — Desktop

![Status](https://img.shields.io/badge/Status-Em%20desenvolvimento-orange?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-007396?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Cliente desktop do **Qualifica+ Ijuí** para gestão de pessoas, cursos, inscrições, aulas e presenças. O aplicativo usa a API Qualifica para autenticação, cadastros e emissão de PDFs, e abre os painéis do Superset na área de Estatísticas.

[Interface](#interface) · [Funcionalidades](#funcionalidades) · [Como executar](#como-executar) · [Tecnologias](#tecnologias)

## Interface

A tela inicial reúne atalhos de cadastro, indicadores, aulas do dia, inscrições recentes e pendências de presença. As capturas usam dados fictícios.

![Tela inicial do Qualifica+ com atalhos, resumo de atividades e aulas do dia](docs/images/inicio.png)

<details>
<summary>Pessoas</summary>

![Cadastro de pessoas com pesquisa por nome ou CPF e ações por registro](docs/images/pessoas.png)

</details>

<details>
<summary>Cursos, inscritos e lista de espera</summary>

![Listagem de cursos com filtros por período, situação e datas](docs/images/cursos.png)

![Detalhes do curso com acesso à lista de inscritos, à lista de espera e aos PDFs](docs/images/curso-detalhes.png)

![Janela de participantes com inscritos e lista de espera lado a lado](docs/images/participantes-curso.png)

</details>

<details>
<summary>Inscrições e documentos</summary>

![Listagem de inscrições com filtros por curso e situação, incluindo a situação Reserva](docs/images/inscricoes.png)

![Detalhes da inscrição com comprovante, certificado e período da declaração](docs/images/inscricao-detalhes.png)

</details>

<details>
<summary>Aulas e presenças</summary>

![Listagem de aulas com filtros, média de presença e acesso à chamada](docs/images/aulas.png)

![Chamada da aula com as opções Presente, Ausente e Justificada](docs/images/presencas.png)

</details>

<details>
<summary>Configurações de conexão</summary>

![Configurações com os endereços atuais da API e do Superset e botão Salvar](docs/images/configuracoes.png)

</details>

## Funcionalidades

| Área | O que você pode fazer |
| --- | --- |
| **Pessoas** | Buscar pelo nome ou CPF, consultar fichas, cadastrar, editar e excluir pessoas. Baixar a ficha do participante e o histórico de participação. |
| **Cursos** | Buscar e filtrar cursos, editar dados e definir a quantidade máxima de participantes. Consultar a lista de inscritos, a lista de espera e a ficha de cada pessoa. |
| **Inscrições** | Inscrever pessoas, filtrar por curso e situação, cancelar e reativar inscrições. Emitir comprovante, declaração de participação e certificado de conclusão. |
| **Aulas** | Cadastrar aulas individuais ou em lote, consultar e editar a agenda e abrir a chamada. |
| **Presenças** | Registrar presente, ausente ou falta justificada. Consultar a média de presença segundo os filtros da listagem de aulas. |
| **Documentos do curso** | Baixar a relação de inscritos com contato opcional, o cronograma e o relatório de frequência. |
| **Estatísticas** | Acessar os painéis do Superset dentro do aplicativo. |
| **Configurações** | Editar e testar os endereços da API e do Superset antes de salvar. |

Nas tabelas compatíveis, selecione uma célula e pressione **Ctrl+C** para copiar seu conteúdo.

### Presenças e aulas em lote

A **Média de presença** divide o número de marcações como presente pelo total de lançamentos das aulas que atendem aos filtros. Ausências e faltas justificadas entram no total. Pessoas sem lançamento ficam fora do cálculo.

O cadastro em lote cria aulas nos dias da semana selecionados dentro do intervalo informado, incluindo a data final. A API recebe cada aula em uma requisição individual. Se houver uma falha, as aulas que já foram aceitas continuam cadastradas.

## Como executar

**Requisitos:** Windows, JDK 25, Maven, acesso à API Qualifica e uma conta cadastrada. O cliente usa a proteção de dados do Windows para armazenar a sessão.

No IntelliJ IDEA, importe o `pom.xml`, selecione o **JDK 25** e execute `com.qualificamaisanaliticinterface.HelloApplication`. Pelo terminal, na raiz do projeto:

```powershell
mvn javafx:run
```

Na tela de login, abra **Configurações de conexão**. Informe o endereço-base da API sem o sufixo `/api` e, se usar os painéis, informe o endereço do Superset. O botão **Salvar** testa os endereços preenchidos. Reabra o aplicativo para aplicar as alterações.

## Tecnologias

- **Java 25**, **JavaFX 21.0.6**, FXML e CSS na interface desktop.
- **Maven** para dependências e execução.
- **ControlsFX** para componentes de interface e **Ikonli / Font Awesome 5** para ícones.
- **Java HttpClient**, **CompletableFuture** e **Gson** na integração com a API.
- **JNA** para a proteção local da sessão no Windows.

## Estrutura do projeto

| Caminho | Conteúdo |
| --- | --- |
| `src/main/java/com/qualificamaisanaliticinterface` | Inicialização, controllers, serviços, sessão e navegação. |
| `src/main/resources/views` | Telas e diálogos em FXML. |
| `src/main/resources/css` | Estilos da interface. |
| `src/test/java` | Testes. |
| `docs/images` | Capturas da interface usadas no README. |
