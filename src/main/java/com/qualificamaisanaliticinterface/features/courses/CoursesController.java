package com.qualificamaisanaliticinterface.courses;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;

public class CoursesController {

    @FXML
    private Pagination tablePagination;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> startFilter;
    @FXML
    private ComboBox<String> finishFilter;

    @FXML
    private ComboBox<String> periodFilter;



    @FXML
    private void initialize() {
        tablePagination.currentPageIndexProperty().addListener(
                (observable, oldIndex, newIndex) -> {
                    int pageIndex = newIndex.intValue();
                    System.out.println(
                            "Índice da página selecionada: " + pageIndex
                    );
                }
        );
        // Opções temporárias, apenas para testar a interface.
        periodFilter.getItems().setAll("Período A", "Período B");
        configureEmptyText(periodFilter, "Todos os períodos");

        startFilter.getItems().setAll("Começo A", "Começo B");
        configureEmptyText(startFilter, "Todos os começos");
        finishFilter.getItems().setAll("Fim A", "Fim B");
        configureEmptyText(finishFilter, "Todos os fins");

    }

    private void updatePagination(int totalPages) {
        tablePagination.setPageCount(Math.max(1, totalPages));

        tablePagination.setVisible(totalPages > 1);
        tablePagination.setManaged(totalPages > 1);
    }

    @FXML
    private void onSearchClick() {
        String search = searchField.getText().trim();

        System.out.println("Texto da pesquisa: " + search);
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();

        periodFilter.setValue(null);
        startFilter.setValue(null);
        finishFilter.setValue(null);
    }

    private void configureEmptyText(
            ComboBox<String> combo,
            String emptyText
    ) {
        combo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(item == null ? emptyText : item);
                setGraphic(null);
            }
        });
    }
}
