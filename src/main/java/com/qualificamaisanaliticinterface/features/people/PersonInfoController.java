package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SearchableComboBox;

public class PersonInfoController {

    @FXML
    public DialogPane infoDialogPane;
    public Button close;

    @FXML
    public Label nameField;
    @FXML
    public Label socialNameField;
    @FXML
    public Label cpfField;
    @FXML
    public Label emailField;
    @FXML
    public Label personalNumberField;
    @FXML
    public CheckBox hasWhatsAppCheckBox;
    @FXML
    public Label familyNumberField;
    @FXML
    public Label genderField;
    @FXML
    public Label educationField;
    @FXML
    public Label workstateField;
    @FXML
    public Label disabilitiesField;
    @FXML
    public Label numberField;
    @FXML
    public Label streetField;
    @FXML
    public Label neighborhoodField;
    @FXML
    public Label infoName;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize(){
    }
    public void getPersonInfo(PersonDto dto) {
        infoName.setText(dto.fullName());
        nameField.setText(dto.fullName());
        socialNameField.setText(dto.socialName());
        cpfField.setText(dto.cpf());
        emailField.setText(dto.email());
        personalNumberField.setText(dto.personalPhone());
        hasWhatsAppCheckBox.setSelected(dto.personalPhoneHasWhatsapp());
        familyNumberField.setText(dto.familyPhone());
        genderField.setText(dto.gender().toString());
        educationField.setText(dto.education().toString());
        workstateField.setText(dto.workState().toString());
        disabilitiesField.setText(dto.disabilities().toString());
        numberField.setText(String.valueOf(dto.address().number()));
        streetField.setText(dto.address().street());
        neighborhoodField.setText(dto.address().neighborhood());
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    public void onDragPressed(MouseEvent mouseEvent) {
        Window window = infoDialogPane.getScene().getWindow();
        dragOffsetX = mouseEvent.getScreenX() - window.getX();
        dragOffsetY = mouseEvent.getScreenY() - window.getY();
    }

    public void onDragMoved(MouseEvent mouseEvent) {
        Window window = infoDialogPane.getScene().getWindow();
        window.setX(mouseEvent.getScreenX() - dragOffsetX);
        window.setY(mouseEvent.getScreenY() - dragOffsetY);
    }
}
