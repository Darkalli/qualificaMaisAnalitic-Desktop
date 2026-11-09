package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;
import java.util.concurrent.CompletionException;


public class UserFormController {

    private final UserService userService = new UserService();
    private boolean passwordVisibility;
    private Runnable onAdded;

    @FXML
    public DialogPane formDialogPane;
    @FXML
    public TextField userField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField visiblePasswordField;
    @FXML
    public PasswordField confirmPasswordField;
    @FXML
    public TextField visibleConfirmPasswordField;
    @FXML
    public ComboBox<UserRole> roleSelection;
    @FXML
    public Label info;

    @FXML
    public Button add;
    @FXML
    public Button close;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize(){
        roleSelection.getItems().setAll(UserRole.values());
    }
    @FXML
    private void onClickAddUser() {
        info.setText("");
        formDialogPane.getContent().setDisable(true);
        String finalPassword = "";

        if (userField.getText() == null || passwordField.getText() == null
                || confirmPasswordField.getText() == null
                || roleSelection.getValue() == null){
            showError(new IllegalArgumentException("Nenhum campo pode estar vazio."));
            formDialogPane.getContent().setDisable(false);
            return;
        }

        if (userField.getText().length() < 3){
            showError(new IllegalArgumentException("O usuário deve ter no mínimo 3 caracteres."));
            formDialogPane.getContent().setDisable(false);
            return;
        }

        onTogglePassword();
        if (Objects.equals(passwordField.getText(), confirmPasswordField.getText())){
            finalPassword = passwordField.getText();
        } else {
            showError(new IllegalArgumentException("As duas senhas devem ser iguais."));
            formDialogPane.getContent().setDisable(false);
            return;
        }
        if (passwordField.getText().length() < 8){
            showError(new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres."));
            formDialogPane.getContent().setDisable(false);
            return;
        }

        userService.addUser(userField.getText(), finalPassword, roleSelection.getValue())
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

    public void onTogglePassword() {
        if(passwordVisibility){
            passwordField.setText(visiblePasswordField.getText());
            confirmPasswordField.setText(visibleConfirmPasswordField.getText());
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            visibleConfirmPasswordField.setVisible(false);
            visibleConfirmPasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            passwordVisibility = false;
        } else {
            visiblePasswordField.setText(passwordField.getText());
            visibleConfirmPasswordField.setText(confirmPasswordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            visibleConfirmPasswordField.setVisible(true);
            visibleConfirmPasswordField.setManaged(true);
            passwordVisibility = true;
        }
    }

    public void setOnAdded(Runnable onAdded) {
        this.onAdded = onAdded;
    }

    private void showError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        info.setText(cause.getMessage() == null ? "Não foi possível criar o usuário." : cause.getMessage());
        if (info.getScene() != null && info.getScene().getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
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
