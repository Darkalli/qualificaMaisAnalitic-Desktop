# Qualifica+ Ijuí — Desktop

![Status](https://img.shields.io/badge/Status-Work%20In%20Progress-orange?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-007396?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Cliente desktop do Qualifica+, desenvolvido para apoiar a gestão de inscrições, pessoas, cursos, aulas e usuários.

## Sobre o projeto

A interface utiliza **JavaFX**, com telas em **FXML** e estilos em **CSS**. O menu lateral mantém a navegação entre as páginas na mesma janela.

O projeto está em fase de prototipação: as telas apresentam tabelas, campos de pesquisa e seletores de filtros. A busca e a paginação ainda não consultam dados reais, e os valores exibidos são demonstrativos. A integração com a API Qualifica ainda não está implementada.

## Executar no IntelliJ IDEA

1. Instale o **JDK 25**.
2. Abra o projeto e importe o `pom.xml` como projeto Maven.
3. Configure o SDK do projeto para o JDK 25.
4. Execute a classe `HelloApplication` e clique em **Entrar**.

## Estrutura

```text
src/main/
├── java/com/qualificamaisanaliticinterface/
│   ├── HelloApplication.java   # Inicialização
│   ├── features/              # Controllers das telas
│   └── navigation/            # Troca de telas e conteúdo
└── resources/
    ├── views/                 # Layouts FXML
    ├── css/                   # Estilos por componente
    └── images/                # Imagens e ícones
```
