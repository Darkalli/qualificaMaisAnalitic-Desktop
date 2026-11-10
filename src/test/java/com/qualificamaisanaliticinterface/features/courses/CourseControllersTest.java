package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CourseControllersTest {
    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException _) {
        }
    }

    @Test
    void newCourseStartsWithDatesAndAvailablePeriods() throws Exception {
        onFxThread(() -> {
            CoursesFormController controller = load("course-form.fxml");
            assertEquals(LocalDate.now(), controller.startField.getValue());
            assertEquals(LocalDate.now().plusDays(1), controller.finishField.getValue());
            assertTrue(controller.periodField.getItems().containsAll(Arrays.asList(Period.values())));
            assertNull(controller.periodField.getValue());
        });
    }

    @Test
    void newCourseRejectsInvalidStartDateBeforeCallingApi() throws Exception {
        onFxThread(() -> {
            CoursesFormController controller = load("course-form.fxml");
            controller.startField.getEditor().setText("data inválida");
            controller.add.fire();
            assertEquals("Informe uma data válida.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
        });
    }

    @Test
    void newCourseRejectsInvalidFinishDateBeforeCallingApi() throws Exception {
        onFxThread(() -> {
            CoursesFormController controller = load("course-form.fxml");
            controller.finishField.getEditor().setText("data inválida");
            controller.add.fire();
            assertEquals("Informe uma data válida.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
        });
    }

    @Test
    void updateCoursePopulatesFields() throws Exception {
        onFxThread(() -> {
            CourseUpdateController controller = load("course-update.fxml");
            CourseDto course = course(StatusCourse.ACTIVE);
            controller.setCourse(course);
            assertEquals(course.name(), controller.nameField.getText());
            assertEquals(course.description(), controller.descriptionField.getText());
            assertEquals(course.period(), controller.periodField.getValue());
            assertEquals(LocalDate.parse(course.start()), controller.startField.getValue());
            assertEquals(LocalDate.parse(course.finish()), controller.finishField.getValue());
        });
    }

    @Test
    void updateCourseRejectsInvalidDateBeforeCallingApi() throws Exception {
        onFxThread(() -> {
            CourseUpdateController controller = load("course-update.fxml");
            controller.setCourse(course(StatusCourse.ACTIVE));
            controller.finishField.getEditor().setText("data inválida");
            controller.add.fire();
            assertEquals("Informe uma data válida.", controller.info.getText());
            assertFalse(controller.formDialogPane.getContent().isDisable());
        });
    }

    @Test
    void courseInfoShowsFormattedFields() throws Exception {
        onFxThread(() -> {
            CourseInfoController controller = load("course-info.fxml");
            controller.setCourse(course(StatusCourse.ACTIVE));
            assertEquals("Curso de teste", controller.name.getText());
            assertEquals("Descrição de teste", controller.description.getText());
            assertEquals("10/11/2026", controller.start.getText());
            assertEquals("20/11/2026", controller.finish.getText());
            assertEquals("Ativo", controller.status.getText());
        });
    }

    @Test
    void courseInfoShowsFallbacksForMissingFields() throws Exception {
        onFxThread(() -> {
            CourseInfoController controller = load("course-info.fxml");
            controller.setCourse(new CourseDto(1L, "Curso", null, null, null, null, null));
            assertEquals("Não informado", controller.description.getText());
            assertEquals("Não informada", controller.start.getText());
            assertEquals("Não informada", controller.finish.getText());
            assertEquals("Não informada", controller.status.getText());
        });
    }

    @Test
    void activeCourseOffersCancellation() throws Exception {
        onFxThread(() -> {
            CourseStatusController controller = load("course-status.fxml");
            controller.setCourse(course(StatusCourse.ACTIVE));
            assertEquals("Cancelar curso?", controller.title.getText());
            assertEquals("Cancelar curso", controller.confirm.getText());
            assertFalse(controller.confirm.isDisabled());
            assertTrue(controller.statusDialogPane.getStyleClass().contains("delete-dialog"));
        });
    }

    @Test
    void canceledCourseOffersReactivation() throws Exception {
        onFxThread(() -> {
            CourseStatusController controller = load("course-status.fxml");
            controller.setCourse(course(StatusCourse.CANCELED));
            assertEquals("Reativar curso?", controller.title.getText());
            assertEquals("Reativar curso", controller.confirm.getText());
            assertFalse(controller.confirm.isDisabled());
        });
    }

    @Test
    void courseWithoutStatusCannotBeChanged() throws Exception {
        onFxThread(() -> {
            CourseStatusController controller = load("course-status.fxml");
            controller.setCourse(course(null));
            assertTrue(controller.confirm.isDisabled());
        });
    }

    private static CourseDto course(StatusCourse status) {
        return new CourseDto(1L, "Curso de teste", "Descrição de teste", "2026-11-10", "2026-11-20", status, Period.AFTERNOON);
    }

    private static <T> T load(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(CourseControllersTest.class.getResource("/views/pages/courses/" + fxml));
        loader.load();
        return loader.getController();
    }

    private static void onFxThread(FxAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface FxAction {
        void run() throws Exception;
    }
}
