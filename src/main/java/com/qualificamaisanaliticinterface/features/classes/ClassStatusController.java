package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
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

public class ClassStatusController {

    private final ClassService classService = new ClassService();

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
    public Label day;
    @FXML
    public Label note;
    @FXML
    public Label info;
    private ClassDto classDto;
    private Runnable onChanged;
    private double dragOffsetX;
    private double dragOffsetY;

    public void setClass(ClassDto classDto) {
        this.classDto = classDto;
        name.setText(classDto.course().name());
        day.setText(classDto.day());
        boolean cancel = classDto.statusClass() == StatusClass.ACTIVE;
        title.setText(cancel ? "Cancelar aula?" : "Reativar aula?");
        confirm.setText(cancel ? "Cancelar aula" : "Reativar aula");
        note.setText(cancel
                ? "A aula ficará cancelada. O histórico de presenças será preservado."
                : "A aula voltará a ficar ativa, desde que não exista conflito de horário com outra aula.");
        if (cancel) {
            statusDialogPane.getStyleClass().add("delete-dialog");
            confirm.getStyleClass().add("delection");
        }
        confirm.setDisable(classDto.statusClass() == null);
    }

    @FXML
    private void onConfirmClick() {
        if (classDto == null || confirm.isDisabled()) return;
        statusDialogPane.getContent().setDisable(true);
        info.setText("");
        CompletableFuture<String> operation = classDto.statusClass() == StatusClass.ACTIVE
                ? classService.cancelCourse(classDto) : classService.reactiveClass(classDto);
        operation.whenComplete((response, failure) -> Platform.runLater(() -> {
            statusDialogPane.getContent().setDisable(false);
            if (failure != null) {
                Throwable cause = failure;
                while (cause instanceof CompletionException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                info.setText(cause.getMessage() == null ? "Não foi possível alterar a aula." : cause.getMessage());
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
