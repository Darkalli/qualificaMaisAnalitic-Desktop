package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public class PeopleController {

    private final PersonService personService = new PersonService();

    @FXML
    public TableColumn<PersonDto, String> emailsColumn;
    @FXML
    public TableColumn<PersonDto, String> cellphoneColumn;
    @FXML
    public TableColumn<PersonDto, Void> actionsColumn;

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
    private Label pageInfoLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> courseFilter;

    @FXML
    private ComboBox<Period> periodFilter;

    @FXML
    private void initialize() {
        cpfsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().cpf())
        );

        namesColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().fullName())
        );
        emailsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().email())
        );
        cellphoneColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().personalPhone())
        );
        actionsColumn.setCellFactory(column ->
                new PersonActionsCell(this::loadPeople)
        );

        courseFilter.setDisable(true);

        periodFilter.getItems().setAll(Period.values());
        configureEmptyText(periodFilter, "Todos os períodos");

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
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();
        periodFilter.setValue(null);
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
        pageInfoLabel.setText(message);

        searchField.setDisable(true);
    }

    @FXML
    public void onNewPersonClick(ActionEvent actionEvent) throws IOException {
        Navigator.openForm(
                peopleTable.getScene(),
                "/views/pages/people/person-form.fxml",
                (PersonFormController controller) ->
                        controller.setOnAdded(this::loadPeople)
        );
    }

    private <T> void configureEmptyText(
            ComboBox<T> combo,
            String emptyText
    ) {
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                setText(item == null ? emptyText : item.toString());
                setGraphic(null);
            }
        });
    }
}