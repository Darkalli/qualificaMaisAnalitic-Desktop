package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.concurrent.CompletionException;

public class PersonDeleteController {
    @FXML
    public Label deleteName;
    public Long personId;
    public Button delete;
    public Button cancel;
    private final ApiClient apiClient = new ApiClient();
    public Label advise;
    private Runnable onDeleted;

    public void getDelectInfo(PersonDto dto){
        personId = dto.id();
        deleteName.setText(dto.fullName());
    }
    public void onDeletePersonClick(ActionEvent actionEvent) {
        delete.setDisable(true);
        apiClient.delete("/api/person/person/" + personId).whenComplete((response, failure) -> {
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
