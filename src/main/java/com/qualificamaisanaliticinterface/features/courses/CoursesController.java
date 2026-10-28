package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

import static com.qualificamaisanaliticinterface.utils.TextUtils.configureEmptyText;

public class CoursesController {

    private final CourseService courseService = new CourseService();
    private List<CourseDto> allCourses = List.of();
    private boolean loaded;

    @FXML
    public TableView<CourseDto> coursesTable;
    @FXML
    public TableColumn<CourseDto, String> namesColumn;
    @FXML
    public TableColumn<CourseDto, String> descriptionColumn;
    @FXML
    public TableColumn<CourseDto, String> periodColumn;
    @FXML
    public TableColumn<CourseDto, String> startColumn;
    @FXML
    public TableColumn<CourseDto, String> finishColumn;
    @FXML
    public TableColumn<CourseDto, String> statusColumn;
    @FXML
    public TableColumn<CourseDto, Void> actionsColumn;
    @FXML
    public ComboBox<StatusCourse> statusFilter;
    @FXML
    public Label totalCoursesLabel;
    @FXML
    private TextField searchField;
    @FXML
    private DatePicker startFilter;
    @FXML
    private DatePicker finishFilter;

    @FXML
    private ComboBox<Period> periodFilter;

    @FXML
    private void initialize() {
        namesColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        descriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().description()));
        periodColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().period() == null ? "Não informado" : cell.getValue().period().toString()));
        startColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().start())));
        finishColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().finish())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().statusCourse() == null ? "Não informado" : cell.getValue().statusCourse().toString()));
        actionsColumn.setCellFactory(column -> new CoursesActionsCell(this::loadCourses));

        periodFilter.getItems().setAll(Period.values());
        configureEmptyText(periodFilter, "Todos os períodos");
        statusFilter.getItems().setAll(StatusCourse.values());
        configureEmptyText(statusFilter, "Todos os Estados");

        loadCourses();
    }

    private void loadCourses() {
        coursesTable.setPlaceholder(new Label("Carregando Cursos..."));
        courseService.findAll().whenComplete((courses, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                showLoadError(failure);
                return;
            }
            allCourses = courses;
            loaded = true;
            totalCoursesLabel.setText(String.valueOf(allCourses.size()));
            onSearchClick();
        }));
    }

    @FXML
    public void onNewCourseClick() throws IOException {
        Navigator.openForm(coursesTable.getScene(), "/views/pages/courses/course-form.fxml",
                (CoursesFormController controller) -> controller.setOnAdded(this::loadCourses));
    }

    @FXML
    public void onSearchClick() {
        if (!loaded) return;
        String search = searchField.getText().strip().toLowerCase(Locale.ROOT);
        Period selectedPeriod = periodFilter.getValue();
        StatusCourse selectedStatus = statusFilter.getValue();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate selectedStart = startFilter.getValue();
        LocalDate selectedFinish = finishFilter.getValue();
        if(selectedStart != null){
            selectedStart.format(formatter);
        }
        if(selectedFinish != null) {
            selectedFinish.format(formatter);
        }

        List<CourseDto> filtered = allCourses.stream()
                .filter(course -> search.isBlank()
                        || course.name().toLowerCase(Locale.ROOT).contains(search))
                .filter(course -> selectedPeriod == null
                        || course.period() == selectedPeriod)
                .filter(course -> selectedStatus == null
                        || course.statusCourse() == selectedStatus)
                .filter(course -> selectedStart == null
                        || selectedStart.toString().equals(course.start()))
                .filter(course -> selectedFinish == null
                        || selectedFinish.toString().equals(course.finish()))
                .toList();
        coursesTable.getItems().setAll(filtered);
        coursesTable.setPlaceholder(new Label(allCourses.isEmpty()
                ? "Nenhuma inscrição cadastrada." : "Nenhuma inscrição encontrada para esses filtros."));
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();

        periodFilter.setValue(null);
        statusFilter.setValue(null);
        startFilter.setValue(null);
        finishFilter.setValue(null);
    }

    private void showLoadError(Throwable failure) {
        loaded = false;
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = "Não foi possível carregar os cursos.";
        if (cause instanceof ApiException exception && exception.getStatusCode() == 401) {
            message = "Sessão inválida ou expirada. Entre novamente.";
        }
        coursesTable.getItems().clear();
        allCourses = List.of();
        totalCoursesLabel.setText("—");
        coursesTable.setPlaceholder(new Label(message));
    }

    private String formatDate(String date) {
        if (date == null) return "";
        try {
            return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException invalidDate) {
            return date;
        }
    }
}
