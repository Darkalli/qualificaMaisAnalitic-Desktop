package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.registers.RegisterActionsCell;
import com.qualificamaisanaliticinterface.features.registers.RegisterInfoController;
import com.qualificamaisanaliticinterface.features.registers.RegisterStatusController;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class CoursesActionsCell  extends TableCell<CourseDto, Void> {
    private final HBox buttons;
    private final Runnable onCoursesChanged;

    public CoursesActionsCell(Runnable onCoursesChanged) {
        this.onCoursesChanged = onCoursesChanged;
        FXMLLoader loader = new FXMLLoader(RegisterActionsCell.class.getResource("/views/pages/courses/course-actions.fxml"));
        try {
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível carregar os botões do curso.", exception);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : buttons);
    }

    @FXML
    public void onCourseInfoClick() throws IOException {
        CourseDto course = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/courses/course-info.fxml",
                (CourseInfoController controller) -> controller.setCourse(course));
    }

    @FXML
    public void onCourseStatusClick() throws IOException {
        CourseDto course = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/courses/course-status.fxml",
                (CourseStatusController controller) -> {
                    controller.setCourse(course);
                    controller.setOnChanged(onCoursesChanged);
                });
    }

    @FXML
    public void onUpdateCourseClick() throws IOException{
        CourseDto course = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/courses/course-update.fxml",
                (CourseUpdateController controller) -> {
                    controller.setCourse(course);
                    controller.setOnChanged(onCoursesChanged);
                });
    }
}
