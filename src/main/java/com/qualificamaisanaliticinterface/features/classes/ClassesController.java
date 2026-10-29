package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;

import static com.qualificamaisanaliticinterface.utils.TextUtils.configureEmptyText;
import static com.qualificamaisanaliticinterface.utils.TextUtils.formatDate;

public class ClassesController {

    private final ClassService classService = new ClassService();
    private List<ClassDto> allClasses = List.of();
    private boolean loaded;

    @FXML
    public Label presence;
    @FXML
    public TableView<ClassDto> classesTable;
    @FXML
    public TableColumn<ClassDto, String> courseColumn;
    @FXML
    public TableColumn<ClassDto, String> dateColumn;
    @FXML
    public TableColumn<ClassDto, String> startColumn;
    @FXML
    public TableColumn<ClassDto, String> finishColumn;
    @FXML
    public TableColumn<ClassDto, String> statusColumn;
    @FXML
    public TableColumn<ClassDto, Void> actionsColumn;
    @FXML
    public TableColumn<ClassDto,String> periodColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Period> periodFilter;
    @FXML
    public DatePicker dateFilter;


    @FXML
    private void initialize() {

        courseColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().course() == null ? "Não informado" : cell.getValue().course().name()));
        dateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatDate(cell.getValue().day())));
        periodColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().session() == null ? "Não informado" : cell.getValue().session().toString()));
        startColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatDate(cell.getValue().start())));
        finishColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatDate(cell.getValue().finish())));
        statusColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().statusClass() == null ? "Não informado" : cell.getValue().statusClass().toString()));
        actionsColumn.setCellFactory(column ->
                new ClassesActionsCell(this::loadClasses));
        periodFilter.getItems().setAll(Period.values());
        configureEmptyText(periodFilter, "Todos os períodos");

        loadClasses();
    }

    private void loadClasses() {
        classesTable.setPlaceholder(new Label("Carregando Aulas..."));
        classService.findAll().whenComplete((classes, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                showLoadError(failure);
                return;
            }
            allClasses = classes;
            loaded = true;
            presence.setText(String.valueOf(allClasses.size()));
            if (!loaded) {
                return;
            }
            classesTable.getItems().setAll(allClasses);
            classesTable.setPlaceholder(new Label("Nenhuma aula encontrada."));
            onSearchClick();
        }));
    }

    @FXML
    public void onNewClassClick() throws IOException {
        Navigator.openForm(classesTable.getScene(), "/views/pages/classes/class-form.fxml",
                (ClassFormController controller) -> controller.setOnAdded(this::loadClasses));
    }

    @FXML
    private void onSearchClick() {
    }

    private void showLoadError(Throwable failure) {
        loaded = false;
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = "Não foi possível carregar as aulas.";
        if (cause instanceof ApiException exception && exception.getStatusCode() == 401) {
            message = "Sessão inválida ou expirada. Entre novamente.";
        }
        classesTable.getItems().clear();
        allClasses = List.of();
        presence.setText("—");
        classesTable.setPlaceholder(new Label(message));
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();
        dateFilter.setValue(null);
        periodFilter.setValue(null);
    }
}
