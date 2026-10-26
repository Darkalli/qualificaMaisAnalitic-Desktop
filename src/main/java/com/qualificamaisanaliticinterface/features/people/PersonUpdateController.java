package com.qualificamaisanaliticinterface.features.people;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.people.dto.UpdatePersonRequest;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SearchableComboBox;

public class PersonUpdateController {
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
    public TextField familyNumberField;
    @FXML
    public TextField numberField;
    @FXML
    public TextField streetField;
    @FXML
    public TextField neighborhoodField;
    @FXML
    public CheckBox hasWhatsAppCheckBox;
    @FXML
    public ComboBox<Gender> genderField;
    @FXML
    public SearchableComboBox<Education> educationField;
    @FXML
    public SearchableComboBox<WorkState> workstateField;
    @FXML
    public CheckComboBox<Disabilities> disabilitiesField;
    @FXML
    public DialogPane formDialogPane;
    @FXML
    public Button close;
    @FXML
    public Button save;
    @FXML
    public Label info;
    private double dragOffsetX;
    private double dragOffsetY;
    private String originalCpf;
    private Runnable onUpdated;
    private final ApiClient apiClient = new ApiClient();
    private final Gson gson = new Gson();

    @FXML
    private void initialize() {
        genderField.getItems().setAll(Gender.values());
        educationField.getItems().setAll(Education.values());
        workstateField.getItems().setAll(WorkState.values());
        disabilitiesField.getItems().setAll(Disabilities.values());
        educationField.setOnShown(event -> {
            if (educationField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar escolaridade...");
            }
        });
        workstateField.setOnShown(event -> {
            if (workstateField.lookup(".combo-box-search") instanceof TextField search) {
                search.setPromptText("Pesquisar estado de trabalho...");
            }
        });
        disabilitiesField.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null && disabilitiesField.lookup("#disabilitiesField > .combo-box") instanceof ComboBox<?> internal) {
                internal.setVisibleRowCount(4);
            }
        });
    }

    public void setPerson(PersonDto person) {
        Objects.requireNonNull(person, "Informe a pessoa a editar.");
        originalCpf = person.cpf();
        nameField.setText(text(person.fullName()));
        socialNameField.setText(text(person.socialName()));
        cpfField.setText(text(person.cpf()));
        emailField.setText(text(person.email()));
        personalNumberField.setText(text(person.personalPhone()));
        familyNumberField.setText(text(person.familyPhone()));
        hasWhatsAppCheckBox.setSelected(Boolean.TRUE.equals(person.personalPhoneHasWhatsapp()));
        numberField.setText(person.address() == null || person.address().number() == null ? "" : person.address().number().toString());
        streetField.setText(person.address() == null ? "" : text(person.address().street()));
        neighborhoodField.setText(person.address() == null ? "" : text(person.address().neighborhood()));
        genderField.setValue(person.gender());
        educationField.setValue(person.education());
        workstateField.setValue(person.workState());
        disabilitiesField.getCheckModel().clearChecks();
        if (person.disabilities() != null) {
            person.disabilities().forEach(value -> disabilitiesField.getCheckModel().check(value));
        }
        info.setText("");
    }

    private static String text(String value) { return value == null ? "" : value; }

    @FXML
    private void onClickSave() {
        if (save.isDisabled()) {
            return;
        }
        try {
            if (originalCpf == null || originalCpf.isBlank()) {
                throw new IllegalArgumentException("Selecione uma pessoa antes de salvar.");
            }
            int number;
            try {
                number = Integer.parseInt(numberField.getText().strip());
            } catch (NumberFormatException invalidNumber) {
                throw new IllegalArgumentException("Informe um número de endereço inteiro válido.");
            }
            var request = new UpdatePersonRequest(
                    originalCpf, socialNameField.getText(), emailField.getText(),
                    personalNumberField.getText(), familyNumberField.getText(),
                    new PersonDto.AddressDto(
                            number, streetField.getText(), neighborhoodField.getText()),
                    genderField.getValue(), educationField.getValue(), workstateField.getValue(),
                    Set.copyOf(disabilitiesField.getCheckModel().getCheckedItems()));
            info.setText("");
            formDialogPane.getContent().setDisable(true);
            apiClient.patch("/api/person", gson.toJson(request)).whenComplete((response, failure) ->
                    Platform.runLater(() -> {
                        formDialogPane.getContent().setDisable(false);
                        if (failure != null) {
                            showError(failure);
                            return;
                        }
                        close.getScene().getWindow().hide();
                        if (onUpdated != null) {
                            onUpdated.run();
                        }
                    }));
        } catch (IllegalArgumentException invalidInput) {
            formDialogPane.getContent().setDisable(false);
            showError(invalidInput);
        }
    }

    public void setOnUpdated(Runnable onUpdated) {
        this.onUpdated = onUpdated;
    }

    private void showError(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        info.setText(cause.getMessage() == null ? "Não foi possível atualizar a pessoa." : cause.getMessage());
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
