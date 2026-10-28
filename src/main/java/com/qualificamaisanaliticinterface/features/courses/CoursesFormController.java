package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.concurrent.CompletionException;

public class CoursesFormController {

    private final CourseService courseService = new CourseService();

    @FXML
    public DialogPane formDialogPane;
    @FXML
    public Button close;
    @FXML
    public Button add;
    @FXML
    public Label info;
    @FXML
    public TextField nameField;
    @FXML
    public TextField descriptionField;
    @FXML
    public ComboBox<Period> periodField;
    @FXML
    public DatePicker startField;
    @FXML
    public DatePicker finishField;

    private Runnable onAdded;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        startField.setValue(LocalDate.now());
        finishField.setValue(LocalDate.now().plusDays(1));
        periodField.getItems().setAll(Period.values());
    }
    @FXML
    private void onClickAddCourse() {
        LocalDate startDate;
        LocalDate finishDate;
        try {
            startDate = startField.getConverter().fromString(startField.getEditor().getText());
            if (startDate == null) {
                throw new IllegalArgumentException("Informe a data de inicio.");
            }
            finishDate = finishField.getConverter().fromString(finishField.getEditor().getText());
            if (finishDate == null) {
                throw new IllegalArgumentException("Informe a data de finalização.");
            }
        } catch (RuntimeException invalidDate) {
            showError(new IllegalArgumentException("Informe uma data válida."));
            return;
        }
        info.setText("");
        formDialogPane.getContent().setDisable(true);
        courseService.addCourse(nameField.getText(), descriptionField.getText(), startDate.toString(),finishDate.toString(), StatusCourse.ACTIVE, periodField.getValue())
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
