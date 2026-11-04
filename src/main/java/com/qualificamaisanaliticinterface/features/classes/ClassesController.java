package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.classes.dto.PresenceDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.utils.TextUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.controlsfx.control.SearchableComboBox;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.qualificamaisanaliticinterface.utils.TextUtils.configureEmptyText;
import static com.qualificamaisanaliticinterface.utils.TextUtils.formatDate;

public class ClassesController {

    private final ClassService classService = new ClassService();
    private final RegisterService registerService = new RegisterService();

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
    public SearchableComboBox<CourseDto> courseField;
    @FXML
    private ComboBox<Period> periodFilter;
    @FXML
    public DatePicker dateFilter;


    @FXML
    private void initialize() {
        TextUtils.configureCopy(classesTable);

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

        courseField.setOnShown(event -> {
            if (courseField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar curso...");
            }
        });

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
            if (!loaded) {
                return;
            }
            classesTable.getItems().setAll(allClasses);
            classesTable.setPlaceholder(new Label("Nenhuma aula encontrada."));
            onSearchClick();
        }));

        CourseDto selectedCourse = courseField.getValue();
        registerService.findCourses().thenApply( courses -> {
            Platform.runLater(() -> {
                courseField.getItems().setAll(courses);
                courseField.setValue(selectedCourse == null ? null : courses.stream()
                        .filter(course -> course.id().equals(selectedCourse.id())).findFirst().orElse(null));
            });
            return null;
        }).exceptionally(failure -> {
            Platform.runLater(() -> showLoadError(failure));
            return null;
        });
    }

    @FXML
    public void onNewClassClick() throws IOException {
        Navigator.openForm(classesTable.getScene(), "/views/pages/classes/class-batch-form.fxml",
                (ClassBatchFormController controller) -> controller.setOnAdded(this::loadClasses));
    }

    @FXML
    private void onSearchClick() {
        CourseDto course = courseField.getValue();
        Period selectedPeriod = periodFilter.getValue();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate selectedDate = dateFilter.getValue();

        if(selectedDate != null){
            selectedDate.format(formatter);
        }

        List<ClassDto> filtered = allClasses.stream()
                .filter(courseClass -> selectedPeriod == null
                        || courseClass.session() == selectedPeriod)
                .filter(courseClass -> course == null
                        || Objects.equals(courseClass.course().name(), course.name()))
                .filter(courseClass -> selectedDate == null
                        || selectedDate.toString().equals(courseClass.day()))
                .toList();

        classesTable.getItems().setAll(filtered);
        updatePresenceAverage(filtered);
        classesTable.setPlaceholder(new Label(
                allClasses.isEmpty()
                        ? "Nenhuma aula cadastrada."
                        : "Nenhuma aula encontrada para essa busca."
        ));
    }

    private void updatePresenceAverage(List<ClassDto> classes){
        List<CompletableFuture<List<PresenceDto>>> requests = new ArrayList<>();

        for(ClassDto courseClass : classes) {
            requests.add(
                    classService.findAllPresences(courseClass.course().id(), courseClass.id())
            );
        }
        CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0]))
                .whenComplete( (ignored, failure) -> Platform.runLater(() -> {
                    if (failure != null){
                        Platform.runLater(() -> presence.setText("-"));
                        return;
                    }

                    List<PresenceDto> collected = new ArrayList<>();

                    for (CompletableFuture<List<PresenceDto>> request : requests) {
                        List<PresenceDto> classPresences = request.join();
                        collected.addAll(classPresences);
                    }

                    int presenceCount = 0;
                    for (PresenceDto presence : collected){
                        if (presence.status() != null && presence.status() == PresenceStatus.PRESENT){
                            presenceCount++;
                        }
                    }
                    try {
                        double result = (double) (presenceCount * 100) / collected.size();
                        DecimalFormat df = new DecimalFormat("0.00");
                        String formatedResult = df.format(result);
                        presence.setText(formatedResult + "%");
                    } catch (ArithmeticException a) {
                        presence.setText("-");
                    }
                }));
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
        courseField.setValue(null);
        dateFilter.setValue(null);
        periodFilter.setValue(null);
        onSearchClick();
    }
}
