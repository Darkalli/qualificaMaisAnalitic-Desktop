package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.people.PersonInfoController;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RegisterInfoController {
    @FXML
    public DialogPane infoDialogPane;
    @FXML
    public Label personName;
    @FXML
    public Label personCpf;
    @FXML
    public Label courseName;
    @FXML
    public Label courseDescription;
    @FXML
    public Label dateField;
    @FXML
    public Label statusField;
    @FXML
    public Label numberField;
    @FXML
    public Button close;
    private RegisterDto register;
    private double dragOffsetX;
    private double dragOffsetY;

    public void setRegister(RegisterDto register) {
        this.register = register;
        personName.setText(register.person().fullName());
        personCpf.setText(register.person().cpf());
        courseName.setText(register.courseOfInterest().name());
        courseDescription.setText(register.courseOfInterest().description() == null
                ? "Não informado" : register.courseOfInterest().description());
        dateField.setText(register.registerDate() == null ? "Não informada"
                : LocalDate.parse(register.registerDate()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        statusField.setText(register.status() == null ? "Não informada" : register.status().toString());
        numberField.setText(register.id() == null ? "Não informado" : register.id().toString());
    }

    @FXML
    private void onPersonInfoClick() throws IOException {
        Navigator.openForm(infoDialogPane.getScene(), "/views/pages/people/person-info.fxml",
                (PersonInfoController controller) -> controller.getPersonInfo(register.person()));
    }

    @FXML
    private void onClickCancel() {
        close.getScene().getWindow().hide();
    }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = infoDialogPane.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = infoDialogPane.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
