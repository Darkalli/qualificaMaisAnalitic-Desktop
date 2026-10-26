package com.qualificamaisanaliticinterface.features.people;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SearchableComboBox;

import java.util.Set;
import java.util.concurrent.CompletionException;

public class PersonFormController {

    @FXML
    public TextField nameField;
    @FXML
    public TextField socialNameField;
    @FXML
    public TextField cpfField;
    @FXML
    public TextField emailField;
    @FXML
    public TextField personalNumberField;
    @FXML
    public CheckBox hasWhatsAppCheckBox;
    @FXML
    public TextField familyNumberField;
    @FXML
    public ComboBox<Gender> genderField;
    @FXML
    public SearchableComboBox<Education> educationField;
    @FXML
    public SearchableComboBox<WorkState> workstateField;
    @FXML
    public CheckComboBox<Disabilities> disabilitiesField;
    @FXML
    public TextField numberField;
    @FXML
    public TextField streetField;
    @FXML
    public TextField neighborhoodField;
    @FXML
    public Label info;
    
    @FXML
    public DialogPane formDialogPane;
    public Button close;
    public Button add;
    private double dragOffsetX;
    private double dragOffsetY;

    private final ApiClient apiClient = new ApiClient();
    private final Gson gson = new Gson();
    private Runnable onAdded;


    @FXML
    private void initialize(){
        educationField.setOnShown(event -> {
            TextField search = (TextField) educationField.lookup(".combo-box-search");
            search.setPromptText("Pesquisar escolaridade...");
        });
        workstateField.setOnShown(event -> {
            TextField search = (TextField) workstateField.lookup(".combo-box-search");
            search.setPromptText("Pesquisar estado de trabalho...");
        });
        genderField.getItems().setAll(Gender.values());
        educationField.getItems().setAll(Education.values());
        workstateField.getItems().setAll(WorkState.values());
        disabilitiesField.getItems().setAll(Disabilities.values());

        disabilitiesField.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if(newSkin != null){
                ComboBox<?> internal = (ComboBox<?>) disabilitiesField.lookup("#disabilitiesField > .combo-box");
                internal.setVisibleRowCount(4);
            }
        });
    }

    @FXML
    private void onClickAddPerson(){
        addPerson(new PersonDto(nameField.getText(), socialNameField.getText(), cpfField.getText(),
                emailField.getText(), personalNumberField.getText(), hasWhatsAppCheckBox.isSelected(),
                familyNumberField.getText(), new PersonDto.AddressDto(Integer.parseInt(numberField.getText()),
                streetField.getText(), neighborhoodField.getText()), genderField.getValue(), educationField.getValue(),
                workstateField.getValue(), Set.copyOf(disabilitiesField.getCheckModel().getCheckedItems())));
    }

    private void addPerson(PersonDto dto){
        add.setDisable(true);
        apiClient.post("/api/person", gson.toJson(dto)).whenComplete((response, failure) -> {
            if(failure != null){
                Platform.runLater(() -> {
                    Throwable cause = failure;
                    while (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    String message = cause.getMessage();

                    add.setDisable(false);
                    info.setText(message);
                    if (info.getScene().getWindow() instanceof Stage stage) {
                        stage.sizeToScene();
                    }
                });
            }else{
                Platform.runLater(() -> {
                    close.getScene().getWindow().hide();
                    onAdded.run();
                });
            }
        });
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    public void onDragPressed(MouseEvent mouseEvent) {
        Window window = formDialogPane.getScene().getWindow();
        dragOffsetX = mouseEvent.getScreenX() - window.getX();
        dragOffsetY = mouseEvent.getScreenY() - window.getY();
    }

    public void onDragMoved(MouseEvent mouseEvent) {
        Window window = formDialogPane.getScene().getWindow();
        window.setX(mouseEvent.getScreenX() - dragOffsetX);
        window.setY(mouseEvent.getScreenY() - dragOffsetY);
    }

    public void setOnAdded(Runnable onAdded){
        this.onAdded = onAdded;
    }
}
