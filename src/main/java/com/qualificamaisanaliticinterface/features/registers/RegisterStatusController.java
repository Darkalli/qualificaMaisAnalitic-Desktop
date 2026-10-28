package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class RegisterStatusController {
    @FXML
    public DialogPane statusDialogPane;
    @FXML
    public Label title;
    @FXML
    public Label personName;
    @FXML
    public Label courseName;
    @FXML
    public Label description;
    @FXML
    public Label info;
    @FXML
    public Button confirm;
    @FXML
    public Button close;

    private final RegisterService registerService = new RegisterService();
    private RegisterDto register;
    private Runnable onChanged;
    private double dragOffsetX;
    private double dragOffsetY;

    public void setRegister(RegisterDto register) {
        this.register = register;
        personName.setText(register.person().fullName());
        courseName.setText(register.courseOfInterest().name());
        boolean cancel = register.status() == StatusRegister.ACTIVE;
        title.setText(cancel ? "Cancelar inscrição?" : "Reativar inscrição?");
        confirm.setText(cancel ? "Cancelar inscrição" : "Reativar inscrição");
        description.setText(cancel
                ? "A inscrição ficará cancelada. O cadastro da pessoa e o histórico de presenças serão preservados."
                : "A inscrição voltará a ficar ativa, desde que não exista conflito de horário com outro curso.");
        if (cancel) {
            statusDialogPane.getStyleClass().add("delete-dialog");
            confirm.getStyleClass().add("delection");
        }
        confirm.setDisable(register.status() == null);
    }

    @FXML
    private void onConfirmClick() {
        if (register == null || confirm.isDisabled()) return;
        statusDialogPane.getContent().setDisable(true);
        info.setText("");
        CompletableFuture<String> operation = register.status() == StatusRegister.ACTIVE
                ? registerService.cancelRegister(register) : registerService.reactiveRegister(register);
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
