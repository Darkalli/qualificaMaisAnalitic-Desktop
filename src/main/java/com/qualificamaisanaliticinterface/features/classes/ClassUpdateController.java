package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.CourseService;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.LocalDateStringConverter;
import org.controlsfx.control.SearchableComboBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletionException;

public class ClassUpdateController {

    @FXML
    public DialogPane formDialogPane;
    @FXML
    public SearchableComboBox<CourseDto> courseField;
    @FXML
    public DatePicker dateField;
    @FXML
    public TextField startField;
    @FXML
    public TextField finishField;
    @FXML
    public Label info;
    @FXML
    public Button close;
    @FXML
    public Button add;
    private ClassDto courseClass;
    private Runnable onChanged;
    private double dragOffsetX;
    private double dragOffsetY;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private final CourseService courseService = new CourseService();
    private final ClassService classService = new ClassService();
    private boolean loading = true;
    private boolean saving;

    @FXML
    private void initialize() {
        add.setDisable(true);
        String datePattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                FormatStyle.SHORT, null, dateField.getChronology(), Locale.getDefault());
        // Resolve the actual calendar date strictly instead of normalizing e.g. 31/02.
        DateTimeFormatter dateParser = DateTimeFormatter.ofPattern(datePattern.replace("y", "u")
                        .replaceAll("u{2,}", "uuuu"))
                .withChronology(dateField.getChronology()).withResolverStyle(ResolverStyle.STRICT);
        dateField.setConverter(new LocalDateStringConverter(null, dateParser));
        configureTime(startField);
        configureTime(finishField);
        courseField.setOnShown(event -> {
            if (courseField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar curso...");
            }
        });
        info.setText("Carregando cursos...");
        courseService.findAll().whenComplete((courses, failure) -> Platform.runLater(() -> {
            loading = false;
            if (failure != null) {
                showError(failure);
            } else {
                CourseDto selected = courseField.getValue();
                courseField.getItems().setAll(courses);
                if (courseClass != null) retainCourse(courseClass.course());
                retainCourse(selected);
                courseField.setValue(selected);
                info.setText("");
            }
            updateSaveState();
        }));
    }

    private void configureTime(TextField field) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[0-9]{0,2}(:[0-9]{0,2})?")) return null;
            if (change.isAdded() && text.matches("[0-9]{2}")) {
                change.setRange(0, change.getControlText().length());
                change.setText(text + ":");
                change.selectRange(3, 3);
            }
            return change;
        }));
    }

    private void retainCourse(CourseDto course) {
        if (course != null && courseField.getItems().stream()
                .noneMatch(item -> Objects.equals(item.id(), course.id()))) {
            courseField.getItems().add(course);
        }
    }

    private void updateSaveState() {
        add.setDisable(courseClass == null || courseClass.id() == null || loading || saving);
    }

    private void showError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        info.setText(cause.getMessage() == null ? "Não foi possível atualizar a aula." : cause.getMessage());
        if (info.getScene() != null && info.getScene().getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    public void setClass(ClassDto courseClass) {
        this.courseClass = courseClass;
        retainCourse(courseClass.course());
        courseField.setValue(courseClass.course());
        dateField.setValue(LocalDate.parse(courseClass.day()));
        startField.setText(LocalTime.parse(courseClass.start()).format(TIME));
        finishField.setText(LocalTime.parse(courseClass.finish()).format(TIME));
        updateSaveState();
    }
    public void setOnChanged(Runnable onChanged) { this.onChanged = onChanged; }

    @FXML
    private void onClickChangeClass() {
        if (courseClass == null || courseClass.id() == null || loading || saving) return;
        CourseDto course = courseField.getValue();
        if (course == null || course.id() == null) {
            showError(new IllegalArgumentException("Selecione um curso."));
            return;
        }
        LocalDate day;
        try {
            day = dateField.getConverter().fromString(dateField.getEditor().getText());
            if (day == null) throw new IllegalArgumentException();
        } catch (RuntimeException invalidDate) {
            showError(new IllegalArgumentException("Informe uma data válida."));
            return;
        }
        LocalTime start;
        LocalTime finish;
        try {
            start = readTime(startField, courseClass.start());
            finish = readTime(finishField, courseClass.finish());
        } catch (DateTimeParseException invalidTime) {
            showError(new IllegalArgumentException("Informe horários válidos no formato HH:mm."));
            return;
        }
        if (!start.isBefore(finish)) {
            showError(new IllegalArgumentException("O início da aula deve ser anterior ao final."));
            return;
        }
        Period session =
                start.isBefore(LocalTime.NOON) ? Period.MORNING
                : start.isBefore(LocalTime.of(18, 0)) ? Period.AFTERNOON
                : Period.NIGHT;
        saving = true;
        updateSaveState();
        info.setText("");
        formDialogPane.getContent().setDisable(true);
        // Status is intentionally omitted: editing a schedule must not reactivate the class.
        classService.updateClass(courseClass.id(), day.toString(), session,
                start.toString(), finish.toString(), null, course.id())
                .whenComplete((response, failure) -> Platform.runLater(() -> {
                    saving = false;
                    formDialogPane.getContent().setDisable(false);
                    updateSaveState();
                    if (failure != null) {
                        showError(failure);
                        return;
                    }
                    close.getScene().getWindow().hide();
                    if (onChanged != null) onChanged.run();
                }));
    }

    private LocalTime readTime(TextField field, String original) {
        LocalTime parsed = LocalTime.parse(field.getText(), TIME);
        LocalTime previous = LocalTime.parse(original);
        // The editor shows minutes; untouched values retain their original precision.
        return field.getText().equals(previous.format(TIME)) ? previous : parsed;
    }

    @FXML
    private void onClickCancel() { close.getScene().getWindow().hide(); }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = formDialogPane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }
    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = formDialogPane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}

