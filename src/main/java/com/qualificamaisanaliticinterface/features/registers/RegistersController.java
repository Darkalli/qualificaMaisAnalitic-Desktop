package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.RegisterActionsCell;
import com.qualificamaisanaliticinterface.features.registers.RegisterFormController;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.features.registers.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public class RegistersController {
    private final RegisterService registerService = new RegisterService();
    private List<RegisterDto> allRegisters = List.of();
    private boolean loaded;

    @FXML
    public TableView<RegisterDto> registersTable;
    @FXML
    public TableColumn<RegisterDto, String> cpfsColumn;
    @FXML
    public TableColumn<RegisterDto, String> namesColumn;
    @FXML
    public TableColumn<RegisterDto, String> courseColumn;
    @FXML
    public TableColumn<RegisterDto, String> dateColumn;
    @FXML
    public TableColumn<RegisterDto, String> statusColumn;
    @FXML
    public TableColumn<RegisterDto, Void> actionsColumn;
    @FXML
    public Label totalRegistersLabel;
    @FXML
    public Label registersCountLabel;
    @FXML
    public Label loadMessage;
    @FXML
    public TextField searchField;
    @FXML
    public ComboBox<CourseDto> courseFilter;
    @FXML
    public ComboBox<StatusRegister> statusFilter;
    @FXML
    public Button refresh;

    @FXML
    private void initialize() {
        cpfsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().person().cpf()));
        namesColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().person().fullName()));
        courseColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().courseOfInterest().name()));
        dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().registerDate())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().status() == null ? "Não informado" : cell.getValue().status().toString()));
        actionsColumn.setCellFactory(column -> new RegisterActionsCell(this::loadRegisters));
        statusFilter.getItems().setAll(StatusRegister.values());
        configureEmptyText(courseFilter, "Todos os cursos");
        configureEmptyText(statusFilter, "Todas as situações");
        loadRegisters();
    }

    private void loadRegisters() {
        registersTable.setPlaceholder(new Label("Carregando inscrições..."));
        registerService.findAll().whenComplete((registers, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                showLoadError(failure);
                return;
            }
            allRegisters = registers;
            loaded = true;
            CourseDto selectedCourse = courseFilter.getValue();
            courseFilter.getItems().setAll(allRegisters.stream()
                    .map(RegisterDto::courseOfInterest).distinct().toList());
            courseFilter.setValue(selectedCourse == null ? null : courseFilter.getItems().stream()
                    .filter(course -> course.id().equals(selectedCourse.id())).findFirst().orElse(null));
            totalRegistersLabel.setText(String.valueOf(allRegisters.size()));
            onSearchClick();
        }));
    }

    @FXML
    public void onNewRegisterClick() throws IOException {
        Navigator.openForm(registersTable.getScene(), "/views/pages/registers/register-form.fxml",
                (RegisterFormController controller) -> controller.setOnAdded(this::loadRegisters));
    }

    @FXML
    public void onRefreshClick() {
        loadRegisters();
    }

    @FXML
    public void onSearchClick() {
        if (!loaded) return;
        String search = searchField.getText().strip().toLowerCase(Locale.ROOT);
        String cpfSearch = search.replaceAll("\\D", "");
        CourseDto selectedCourse = courseFilter.getValue();
        StatusRegister selectedStatus = statusFilter.getValue();
        List<RegisterDto> filtered = allRegisters.stream()
                .filter(register -> search.isBlank()
                        || register.person().fullName().toLowerCase(Locale.ROOT).contains(search)
                        || (!cpfSearch.isBlank() && register.person().cpf().contains(cpfSearch)))
                .filter(register -> selectedCourse == null
                        || register.courseOfInterest().id().equals(selectedCourse.id()))
                .filter(register -> selectedStatus == null || register.status() == selectedStatus)
                .toList();
        registersTable.getItems().setAll(filtered);
        registersTable.setPlaceholder(new Label(allRegisters.isEmpty()
                ? "Nenhuma inscrição cadastrada." : "Nenhuma inscrição encontrada para esses filtros."));
    }

    @FXML
    public void onClearFiltersClick() {
        searchField.clear();
        courseFilter.setValue(null);
        statusFilter.setValue(null);
        onSearchClick();
    }

    private void showLoadError(Throwable failure) {
        loaded = false;
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = "Não foi possível carregar as inscrições.";
        if (cause instanceof ApiException exception && exception.getStatusCode() == 401) {
            message = "Sessão inválida ou expirada. Entre novamente.";
        }
        registersTable.getItems().clear();
        allRegisters = List.of();
        totalRegistersLabel.setText("—");
        registersCountLabel.setText("Consulta indisponível");
        loadMessage.setText(message);
        registersTable.setPlaceholder(new Label(message));
    }

    private String formatDate(String date) {
        if (date == null) return "";
        try {
            return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException invalidDate) {
            return date;
        }
    }

    private <T> void configureEmptyText(ComboBox<T> combo, String emptyText) {
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
