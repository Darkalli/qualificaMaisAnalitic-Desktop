package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterService;
import com.qualificamaisanaliticinterface.utils.TextUtils;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.LocalDateStringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.StringJoiner;

public class CourseInfoController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    @FXML
    public DialogPane infoDialogPane;
    @FXML
    public Label name;
    @FXML
    public Label description;
    @FXML
    public Label start;
    @FXML
    public Label finish;
    @FXML
    public Label status;
    @FXML
    public Button close;
    @FXML
    private VBox documentsBox;
    @FXML
    private CheckBox includeContact;
    @FXML
    private DatePicker reportStart;
    @FXML
    private DatePicker reportFinish;
    @FXML
    private Label reportPeriodError;

    private CourseDto course;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        reportStart.setConverter(new LocalDateStringConverter(DATE_FORMAT, DATE_FORMAT));
        reportFinish.setConverter(new LocalDateStringConverter(DATE_FORMAT, DATE_FORMAT));
    }

    public void setCourse(CourseDto course) {
        this.course = course;
        documentsBox.setDisable(course.id() == null || course.id() <= 0);
        name.setText(course.name());
        description.setText(course.description() == null
                ? "Não informado" : course.description());
        start.setText(course.start() == null ? "Não informada"
                : LocalDate.parse(course.start()).format(DATE_FORMAT));
        finish.setText(course.finish() == null ? "Não informada"
                : LocalDate.parse(course.finish()).format(DATE_FORMAT));
        status.setText(course.statusCourse() == null ? "Não informada" : course.statusCourse().toString());
    }

    @FXML
    private void onEnrollmentListClick() {
        downloadPdf("/courses/" + course.id() + "/enrollments?includeContact=" + includeContact.isSelected(),
                TextUtils.pdfFilename("Relação de inscritos", course.name()));
    }

    @FXML
    private void onScheduleClick() {
        downloadPeriodPdf("/courses/" + course.id() + "/schedule", "Cronograma do curso");
    }

    @FXML
    private void onAttendanceClick() {
        downloadPeriodPdf("/attendance?courseId=" + course.id(), "Relatório de frequência");
    }

    private void downloadPeriodPdf(String endpoint, String title) {
        LocalDate from;
        LocalDate to;
        try {
            from = reportStart.getConverter().fromString(reportStart.getEditor().getText());
            to = reportFinish.getConverter().fromString(reportFinish.getEditor().getText());
        } catch (DateTimeParseException exception) {
            setReportPeriodError("Informe datas válidas no formato dd/mm/aaaa.");
            return;
        }

        if (from != null && to != null && from.isAfter(to)) {
            setReportPeriodError("A data final deve ser igual ou posterior à inicial.");
            return;
        }

        StringJoiner parameters = new StringJoiner("&", endpoint.contains("?") ? "&" : "?", "")
                .setEmptyValue("");
        if (from != null) {
            parameters.add("start=" + from);
        }
        if (to != null) {
            parameters.add("finish=" + to);
        }
        setReportPeriodError("");
        downloadPdf(endpoint + parameters, TextUtils.pdfFilename(title, course.name()));
    }

    private void setReportPeriodError(String message) {
        boolean visible = !message.isEmpty();
        boolean visibilityChanged = reportPeriodError.isVisible() != visible;
        reportPeriodError.setText(message);
        reportPeriodError.setVisible(visible);
        reportPeriodError.setManaged(visible);
        Scene scene = infoDialogPane.getScene();
        if (visibilityChanged && scene != null && scene.getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    void downloadPdf(String endpoint, String filename) {
        new RegisterService().downloadPdf("/api/reports" + endpoint,
                filename, infoDialogPane.getScene().getWindow());
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
