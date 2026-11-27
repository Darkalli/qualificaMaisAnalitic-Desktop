package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.utils.TextUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

import java.util.Objects;

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
    public Label disabilitiesInfoField;
    @FXML
    public Label numberField;
    @FXML
    public Label streetField;
    @FXML
    public Label neighborhoodField;
    @FXML
    public Label infoName;
    @FXML
    private Hyperlink documents;
    @FXML
    private VBox documentOptions;

    private PopOver documentsPopover;
    private PersonDto person;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        documentsPopover = new PopOver(documentOptions);
        documentsPopover.setDetachable(false);
        documentsPopover.setConsumeAutoHidingEvents(false);
        documentsPopover.setArrowSize(10);
        documentsPopover.setCornerRadius(12);
        documentsPopover.getStyleClass().add("person-documents-popover");
        documentsPopover.getRoot().getStylesheets().add(
                PersonInfoController.class.getResource("/css/application.css").toExternalForm());
    }

    public void getPersonInfo(PersonDto dto) {
        person = dto;
        documents.setDisable(dto.id() == null || dto.id() <= 0);
        infoName.setText(dto.fullName());
        nameField.setText(dto.fullName());
        socialNameField.setText(dto.socialName());
        cpfField.setText(dto.cpf());
        emailField.setText(dto.email());
        personalNumberField.setText(dto.personalPhone());
        hasWhatsAppCheckBox.setSelected(Boolean.TRUE.equals(dto.personalPhoneHasWhatsapp()));
        familyNumberField.setText(dto.familyPhone());
        genderField.setText(Objects.toString(dto.gender(), "Não informado"));
        educationField.setText(Objects.toString(dto.education(), "Não informada"));
        workstateField.setText(Objects.toString(dto.workState(), "Não informada"));
        disabilitiesInfoField.setText(Objects.toString(dto.disabilities(), "Não informado"));
        PersonDto.AddressDto address = dto.address();
        numberField.setText(address == null ? "" : Objects.toString(address.number(), ""));
        streetField.setText(address == null ? "" : address.street());
        neighborhoodField.setText(address == null ? "" : address.neighborhood());
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

    @FXML
    private void onDocumentsClick() {
        if (documentsPopover.isShowing()) {
            documentsPopover.hide(Duration.ZERO);
            return;
        }

        Bounds anchor = documents.localToScreen(documents.getBoundsInLocal());
        Rectangle2D screen = Screen.getScreensForRectangle(anchor.getCenterX(), anchor.getCenterY(), 1, 1)
                .stream().findFirst().orElseGet(Screen::getPrimary).getVisualBounds();
        double rightSpace = screen.getMaxX() - anchor.getMaxX();
        double leftSpace = anchor.getMinX() - screen.getMinX();
        double popupWidth = documentOptions.prefWidth(-1) + 2 * documentsPopover.getArrowSize();
        documentsPopover.setArrowLocation(rightSpace < popupWidth && leftSpace > rightSpace
                ? PopOver.ArrowLocation.RIGHT_TOP : PopOver.ArrowLocation.LEFT_TOP);
        documentsPopover.show(documents, 0);
    }

    @FXML
    private void onParticipantRecordClick() {
        downloadDocument("record", "Ficha do participante");
    }

    @FXML
    private void onParticipationHistoryClick() {
        downloadDocument("history", "Histórico de participação");
    }

    private void downloadDocument(String report, String title) {
        documentsPopover.hide(Duration.ZERO);
        new RegisterService().downloadPdf("/api/reports/participants/" + person.id() + "/" + report,
                TextUtils.pdfFilename(title, person.fullName()), infoDialogPane.getScene().getWindow());
    }
}
