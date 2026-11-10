package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.concurrent.CompletionException;

public class UserDeleteController {

    @FXML
    public Label deleteName;
    @FXML
    public Label advise;
    @FXML
    public Button cancel;
    @FXML
    public Button delete;

    private final UserService userService;
    public Long id;
    private Runnable onDeleted;

    public UserDeleteController() {
        this(new UserService());
    }

    UserDeleteController(UserService userService) {
        this.userService = userService;
    }

    public void getDelectInfo(UserDto dto){
        id = dto.id();
        deleteName.setText(dto.username());
    }
    public void onDeleteUserClick(ActionEvent actionEvent) {
        delete.setDisable(true);
        userService.deleteUser(id).whenComplete((response, failure) -> {
            if(failure != null){
                Platform.runLater(() -> {
                    Throwable cause = failure;
                    while (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    String message = cause.getMessage();

                    delete.setDisable(false);
                    advise.setText(message);
                    if (advise.getScene().getWindow() instanceof Stage stage) {
                        stage.sizeToScene();
                    }
                });
            }else{
                Platform.runLater(() -> {
                    cancel.getScene().getWindow().hide();
                    onDeleted.run();
                });
            }
        });
    }

    @FXML
    private void onClickCancel() {
        cancel.getScene().getWindow().hide();
    }

    public void setOnDeleted(Runnable onDeleted) {
        this.onDeleted = onDeleted;
    }
}
