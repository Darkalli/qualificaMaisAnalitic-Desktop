package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.people.PersonInfoController;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CourseInfoController {

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

    private double dragOffsetX;
    private double dragOffsetY;

    public void setCourse(CourseDto course) {
        name.setText(course.name());
        description.setText(course.description() == null
                ? "Não informado" : course.description());
        start.setText(course.start() == null ? "Não informada"
                : LocalDate.parse(course.start()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        finish.setText(course.finish() == null ? "Não informada"
                : LocalDate.parse(course.finish()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        status.setText(course.statusCourse() == null ? "Não informada" : course.statusCourse().toString());
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
