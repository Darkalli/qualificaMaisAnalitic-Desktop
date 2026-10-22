package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public class PeopleController {

    private final PersonService personService = new PersonService();

    private List<PersonDto> allPeople = List.of();

    @FXML
    private TableView<PersonDto> peopleTable;

    @FXML
    private TableColumn<PersonDto, String> cpfsColumn;

    @FXML
    private TableColumn<PersonDto, String> namesColumn;

    @FXML
    private Label totalPeopleLabel;

    @FXML
    private Label peopleCountLabel;

    @FXML
    private Label pageInfoLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> courseFilter;

    @FXML
    private ComboBox<String> periodFilter;

    @FXML
    private Pagination tablePagination;

    @FXML
    private void initialize() {
        cpfsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().cpf())
        );

        namesColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().fullName())
        );

        // Esses filtros ainda não estão implementados.
        courseFilter.setDisable(true);
        periodFilter.setDisable(true);

        // Nesta etapa, mostramos todos os resultados.
        tablePagination.setVisible(false);
        tablePagination.setManaged(false);

        loadPeople();
    }

    private void loadPeople() {
        peopleTable.setPlaceholder(new Label("Carregando pessoas..."));

        personService.findAll()
                .whenComplete((people, failure) ->
                        Platform.runLater(() -> {
                            if (failure != null) {
                                showLoadError(failure);
                                return;
                            }

                            allPeople = people;

                            totalPeopleLabel.setText(
                                    String.valueOf(allPeople.size())
                            );

                            onSearchClick();
                        })
                );
    }

    @FXML
    private void onSearchClick() {
        String search = searchField.getText()
                .strip()
                .toLowerCase(Locale.ROOT);

        List<PersonDto> filtered = allPeople.stream()
                .filter(person ->
                        person.fullName()
                                .toLowerCase(Locale.ROOT)
                                .contains(search)
                                || person.cpf().contains(search)
                )
                .toList();

        peopleTable.getItems().setAll(filtered);

        peopleTable.setPlaceholder(new Label(
                allPeople.isEmpty()
                        ? "Nenhuma pessoa cadastrada."
                        : "Nenhuma pessoa encontrada para essa busca."
        ));

        peopleCountLabel.setText(filtered.size() + " pessoas");

        pageInfoLabel.setText(
                "Exibindo " + filtered.size()
                        + " de " + allPeople.size() + " pessoas"
        );
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();
        onSearchClick();
    }

    private void showLoadError(Throwable failure) {
        Throwable cause = failure;

        while (cause instanceof CompletionException
                && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = "Não foi possível carregar as pessoas.";

        if (cause instanceof ApiException apiException
                && apiException.getStatusCode() == 401) {
            message = "Sessão inválida ou expirada. Entre novamente.";
        }

        peopleTable.setPlaceholder(new Label(message));
        totalPeopleLabel.setText("—");
        peopleCountLabel.setText("Consulta indisponível");
        pageInfoLabel.setText(message);

        searchField.setDisable(true);
    }

    @FXML
    public void onNewPersonClick(ActionEvent actionEvent) throws IOException {
        Navigator.openForm(peopleTable.getScene(), "/views/pages/forms/person-form.fxml");
    }
}