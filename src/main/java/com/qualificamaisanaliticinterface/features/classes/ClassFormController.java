package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.SearchableComboBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.concurrent.CompletionException;

public class ClassFormController {
    
    private final RegisterService registerService = new RegisterService();
    private final ClassService classService = new ClassService();
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


    private Runnable onAdded;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        dateField.setValue(LocalDate.now());

        startField.setTextFormatter(new TextFormatter<String>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[0-9]{0,2}(:[0-9]{0,2})?")) {
                return null;
            }
            if (change.isAdded() && text.matches("[0-9]{2}")) {
                change.setRange(0, change.getControlText().length());
                change.setText(text + ":");
                change.selectRange(3, 3);
            }
            return change;
        }));
        finishField.setTextFormatter(new TextFormatter<String>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[0-9]{0,2}(:[0-9]{0,2})?")) {
                return null;
            }
            if (change.isAdded() && text.matches("[0-9]{2}")) {
                change.setRange(0, change.getControlText().length());
                change.setText(text + ":");
                change.selectRange(3, 3);
            }
            return change;
        }));

        courseField.setOnShown(event -> {
            if (courseField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar curso...");
            }
        });
        loadOptions();
    }

    private void loadOptions() {
        add.setDisable(true);
        info.setText("Carregando cursos...");
        CourseDto selectedCourse = courseField.getValue();
        registerService.findCourses().thenApply( courses -> {
            Platform.runLater(() -> {
                courseField.getItems().setAll(courses);
                courseField.setValue(selectedCourse == null ? null : courses.stream()
                        .filter(course -> course.id().equals(selectedCourse.id())).findFirst().orElse(null));
                add.setDisable(courses.isEmpty());
                info.setText(courses.isEmpty() ? "Cadastre um curso antes de criar uma inscrição." : "");
            });
            return null;
        }).exceptionally(failure -> {
            Platform.runLater(() -> showError(failure));
            return null;
        });
    }

    @FXML
    private void onClickAddClass() {
        LocalDate date;
        CourseDto course = courseField.getValue();
        try {
            date = dateField.getConverter().fromString(dateField.getEditor().getText());
            if (date == null) {
                throw new IllegalArgumentException("Informe o dia.");
            }
        } catch (RuntimeException invalidDate) {
            showError(new IllegalArgumentException("Informe uma data válida."));
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("HH:mm")
                .withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalTime start = LocalTime.parse(startField.getText(), formatter);
            LocalTime finish = LocalTime.parse(finishField.getText(), formatter);
            Period period;
            LocalTime morning = LocalTime.of(12, 0),
                    afternoon = LocalTime.of(18, 0);
            if(start.isBefore(morning)){
                period = Period.MORNING;
            }else if(start.isBefore(afternoon)){
                period = Period.AFTERNOON;
            }else {
                period = Period.NIGHT;
            }

            info.setText("");
            formDialogPane.getContent().setDisable(true);
            classService.addClass(date.toString(), period, start.toString(), finish.toString(), StatusClass.ACTIVE,course.id())
                    .whenComplete((response, failure) -> Platform.runLater(() -> {
                        formDialogPane.getContent().setDisable(false);
                        if (failure != null) {
                            showError(failure);
                            return;
                        }
                        close.getScene().getWindow().hide();
                        if (onAdded != null) {
                            onAdded.run();
                        }
                    }));
        } catch (DateTimeParseException e) {
            showError(new IllegalArgumentException("Informe uma hora válida."));
        }
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    private void showError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        info.setText(cause.getMessage() == null ? "Não foi possível criar a inscrição." : cause.getMessage());
        if (info.getScene() != null && info.getScene().getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    public void setOnAdded(Runnable onAdded) {
        this.onAdded = onAdded;
    }

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
