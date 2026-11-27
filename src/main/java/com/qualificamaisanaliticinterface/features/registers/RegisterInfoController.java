package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.people.PersonInfoController;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.utils.TextUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDate;

public class RegisterInfoController {

    private final RegisterService registerService = new RegisterService();

    @FXML
    public DatePicker startField;
    @FXML
    public DatePicker finishField;
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
                : TextUtils.formatDateOrDateTime(register.registerDate()));
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

    public void onRegisterProofClick(ActionEvent actionEvent) {
        Window owner = ((Node) actionEvent.getSource())
                .getScene()
                .getWindow();

        registerService.downloadPdf(
                "/api/reports/registrations/" + register.id() + "/proof",
                "Comprovante-de-inscrição-" + register.person().fullName() + ".pdf",
                owner
        );
    }

    public void onParticipationClick(ActionEvent actionEvent) {
        Window owner = ((Node) actionEvent.getSource())
                .getScene()
                .getWindow();

        LocalDate start = startField.getValue();
        LocalDate finish = finishField.getValue();

        String endpoint = "/api/reports/registrations/" + register.id() + "/declaration";
        String filename = "Declaração-de-participação-" + register.person().fullName();
        if (start != null && finish != null) {
            endpoint += "?start=" + start + "&finish=" + finish;
            filename += "-" + start + "-" + finish;
        }
        registerService.downloadPdf(endpoint, filename + ".pdf", owner);
    }

    public void onConclusionClick(ActionEvent actionEvent) {
        Window owner = ((Node) actionEvent.getSource())
                .getScene()
                .getWindow();

        registerService.downloadPdf(
                "/api/reports/registrations/" + register.id() + "/certificate",
                "Certificado-de-conclusão-" + register.person().fullName() + ".pdf",
                owner
        );
    }
}
