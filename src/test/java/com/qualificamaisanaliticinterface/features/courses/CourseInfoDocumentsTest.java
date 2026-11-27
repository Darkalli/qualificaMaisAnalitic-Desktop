package com.qualificamaisanaliticinterface.features.courses;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CourseInfoDocumentsTest {
    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException _) {
        }
    }

    @Test
    void courseDocumentButtonsUseTheSelectedCourseAndValidateTheReportPeriod() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            List<String> downloads = new ArrayList<>();
            List<String> filenames = new ArrayList<>();
            CourseInfoController controller = new CourseInfoController() {
                @Override
                void downloadPdf(String endpoint, String filename) {
                    downloads.add(endpoint);
                    filenames.add(filename);
                }
            };
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/pages/courses/course-info.fxml"));
            loader.setControllerFactory(type -> controller);
            DialogPane pane = loader.load();
            controller.setCourse(new CourseDto(37L, "Informática: Módulo 1", null, null, null, null, null, null));

            Button enrollmentList = documentButton(pane, "Relação de inscritos");
            Button schedule = documentButton(pane, "Cronograma do curso");
            Button attendance = documentButton(pane, "Relatório de frequência");
            CheckBox includeContact = (CheckBox) pane.lookup("#includeContact");
            DatePicker start = (DatePicker) pane.lookup("#reportStart");
            DatePicker finish = (DatePicker) pane.lookup("#reportFinish");
            Label periodError = (Label) pane.lookup("#reportPeriodError");

            schedule.fire();
            start.getEditor().setText("31/02/2026");
            attendance.fire();
            assertTrue(periodError.isVisible());
            assertEquals(1, downloads.size());

            start.getEditor().setText("10/11/2026");
            finish.getEditor().setText("09/11/2026");
            schedule.fire();
            assertTrue(periodError.isVisible());
            assertEquals(1, downloads.size());

            assertTrue(includeContact.isSelected());
            enrollmentList.fire();
            includeContact.fire();
            assertFalse(includeContact.isSelected());
            enrollmentList.fire();
            finish.getEditor().setText("20/11/2026");
            attendance.fire();
            assertFalse(periodError.isVisible());
            start.getEditor().clear();
            schedule.fire();
            assertEquals(List.of(
                    "/courses/37/schedule",
                    "/courses/37/enrollments?includeContact=true",
                    "/courses/37/enrollments?includeContact=false",
                    "/attendance?courseId=37&start=2026-11-10&finish=2026-11-20",
                    "/courses/37/schedule?finish=2026-11-20"), downloads);
            assertEquals(List.of(
                    "Cronograma do curso - Informática Módulo 1.pdf",
                    "Relação de inscritos - Informática Módulo 1.pdf",
                    "Relação de inscritos - Informática Módulo 1.pdf",
                    "Relatório de frequência - Informática Módulo 1.pdf",
                    "Cronograma do curso - Informática Módulo 1.pdf"), filenames);
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    private static Button documentButton(DialogPane pane, String text) {
        return pane.lookupAll(".document-button").stream()
                .map(Button.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst().orElseThrow();
    }
}
