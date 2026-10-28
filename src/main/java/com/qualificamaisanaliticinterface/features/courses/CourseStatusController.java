package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class CourseStatusController {


    private final CourseService courseService = new CourseService();
    @FXML
    public DialogPane statusDialogPane;
    @FXML
    public Label title;
    @FXML
    public Label name;
    @FXML
    public Button close;
    @FXML
    public Button confirm;
    @FXML
    public Label description;
    @FXML
    public Label note;
    @FXML
    public Label info;
    private CourseDto course;
    private Runnable onChanged;
    private double dragOffsetX;
    private double dragOffsetY;

    public void setCourse(CourseDto course) {
        this.course = course;
        name.setText(course.name());
        description.setText(course.description());
        boolean cancel = course.statusCourse() == StatusCourse.ACTIVE;
        title.setText(cancel ? "Cancelar curso?" : "Reativar curso?");
        confirm.setText(cancel ? "Cancelar curso" : "Reativar curso");
        description.setText(cancel
                ? "O curso ficará cancelado. Os cadastros das pessoas e o histórico de presenças serão preservados."
                : "O curso voltará a ficar ativo, desde que não exista conflito de horário com outro curso.");
        if (cancel) {
            statusDialogPane.getStyleClass().add("delete-dialog");
            confirm.getStyleClass().add("delection");
        }
        confirm.setDisable(course.statusCourse() == null);
    }

    @FXML
    private void onConfirmClick() {
        if (course == null || confirm.isDisabled()) return;
        statusDialogPane.getContent().setDisable(true);
        info.setText("");
        CompletableFuture<String> operation = course.statusCourse() == StatusCourse.ACTIVE
                ? courseService.cancelCourse(course) : courseService.reactiveCourse(course);
        operation.whenComplete((response, failure) -> Platform.runLater(() -> {
            statusDialogPane.getContent().setDisable(false);
            if (failure != null) {
                Throwable cause = failure;
                while (cause instanceof CompletionException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                info.setText(cause.getMessage() == null ? "Não foi possível alterar a inscrição." : cause.getMessage());
                if (info.getScene().getWindow() instanceof Stage stage) {
                    stage.sizeToScene();
                }
                return;
            }
            close.getScene().getWindow().hide();
            if (onChanged != null) {
                onChanged.run();
            }
        }));
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = statusDialogPane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = statusDialogPane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
