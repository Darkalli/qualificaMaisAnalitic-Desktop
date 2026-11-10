package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class UserUpdateController {

    @FXML
    public DialogPane updateDialogPane;
    @FXML
    public Button close;
    @FXML
    public TextField userField;
    @FXML
    public ComboBox<UserRole> roleSelection;
    @FXML
    public Label info;
    @FXML
    public Button add;


    private final UserService userService;
    private Long id;
    private Runnable onChanged;
    private double dragOffsetX;
    private double dragOffsetY;

    public UserUpdateController() {
        this(new UserService());
    }

    UserUpdateController(UserService userService) {
        this.userService = userService;
    }

    @FXML
    private void initialize() {
        roleSelection.getItems().setAll(UserRole.values());
    }

    @FXML
    public void setUser(UserDto user){
        id = user.id();
        userField.setText(user.username());
        roleSelection.setValue(user.role());
    }

    @FXML
    private void onClickUpdateUser() {
        info.setText("");
        updateDialogPane.getContent().setDisable(true);
        userService.updateUser(id, userField.getText(), roleSelection.getValue())
                .whenComplete((response, failure) -> Platform.runLater(() -> {
                    updateDialogPane.getContent().setDisable(false);
                    if (failure != null) {
                        showError(failure);
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

    public void setOnUpdated(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private void showError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        info.setText(cause.getMessage() == null ? "Não foi possível atualizar o usuário." : cause.getMessage());
        if (info.getScene() != null && info.getScene().getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = updateDialogPane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = updateDialogPane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
