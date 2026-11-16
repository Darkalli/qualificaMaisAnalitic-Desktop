package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qualificamaisanaliticinterface.features.classes.ClassTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassStatusControllerTest {
    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @Test
    void shouldOfferCancellationOrReactivationForClassStatus() throws Exception {
        assertDialog(StatusClass.ACTIVE, "Cancelar aula?", "Cancelar aula", false);
        assertDialog(StatusClass.CANCELED, "Reativar aula?", "Reativar aula", false);
        assertDialog(StatusClass.POSTPONED, "Reativar aula?", "Reativar aula", false);
        assertDialog(null, "Reativar aula?", "Reativar aula", true);
    }

    @Test
    void shouldCancelActiveClassAndNotifyChange() throws Exception {
        assertSuccessfulStatusChange(StatusClass.ACTIVE, true);
    }

    @Test
    void shouldReactivateCanceledClassAndNotifyChange() throws Exception {
        assertSuccessfulStatusChange(StatusClass.CANCELED, false);
    }

    @Test
    void shouldKeepStatusDialogOpenWhenRequestFails() throws Exception {
        ClassDto courseClass = new ClassDto(7L, "2026-11-20", Period.NIGHT,
                "19:00", "21:00", StatusClass.ACTIVE, course());
        AtomicInteger notifications = new AtomicInteger();
        ClassService service = new ClassService() {
            @Override
            public CompletableFuture<String> cancelCourse(ClassDto selected) {
                assertEquals(courseClass, selected);
                return CompletableFuture.failedFuture(new IllegalStateException("Falha simulada"));
            }
        };
        ClassStatusController controller = new ClassStatusController();
        installService(controller, service);
        controller.setOnChanged(notifications::incrementAndGet);
        Stage stage = openStage("class-status.fxml", controller, () -> controller.setClass(courseClass));
        try {
            fx(() -> {
                controller.confirm.fire();
                return null;
            });
            fx(() -> {
                assertTrue(stage.isShowing());
                assertEquals("Falha simulada", controller.info.getText());
                assertFalse(controller.statusDialogPane.getContent().isDisabled());
                assertFalse(controller.confirm.isDisabled());
                assertEquals(0, notifications.get());
                return null;
            });
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static void assertSuccessfulStatusChange(StatusClass status, boolean cancel) throws Exception {
        ClassDto courseClass = new ClassDto(7L, "2026-11-20", Period.NIGHT,
                "19:00", "21:00", status, course());
        List<String> operations = new ArrayList<>();
        AtomicInteger notifications = new AtomicInteger();
        ClassService service = new ClassService() {
            @Override
            public CompletableFuture<String> cancelCourse(ClassDto selected) {
                assertEquals(courseClass, selected);
                operations.add("cancel");
                return CompletableFuture.completedFuture("ok");
            }

            @Override
            public CompletableFuture<String> reactiveClass(ClassDto selected) {
                assertEquals(courseClass, selected);
                operations.add("reactivate");
                return CompletableFuture.completedFuture("ok");
            }
        };
        ClassStatusController controller = new ClassStatusController();
        installService(controller, service);
        controller.setOnChanged(notifications::incrementAndGet);
        Stage stage = openStage("class-status.fxml", controller, () -> controller.setClass(courseClass));
        try {
            fx(() -> {
                controller.confirm.fire();
                return null;
            });
            fx(() -> {
                assertEquals(List.of(cancel ? "cancel" : "reactivate"), operations);
                assertEquals(1, notifications.get());
                assertFalse(stage.isShowing());
                return null;
            });
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static void installService(ClassStatusController controller, ClassService service) throws Exception {
        Field field = ClassStatusController.class.getDeclaredField("classService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static void assertDialog(StatusClass status, String expectedTitle,
                                     String expectedAction, boolean disabled) throws Exception {
        ClassStatusController controller = new ClassStatusController();
        ClassDto courseClass = new ClassDto(7L, "2026-11-20", Period.NIGHT, "19:00", "21:00",
                status, course());
        Stage stage = openStage("class-status.fxml", controller, () -> controller.setClass(courseClass));
        try {
            fx(() -> {
                assertEquals(expectedTitle, controller.title.getText());
                assertEquals(expectedAction, controller.confirm.getText());
                assertEquals(disabled, controller.confirm.isDisabled());
                assertEquals(course().name(), controller.name.getText());
                assertEquals(courseClass.day(), controller.day.getText());
                if (status == StatusClass.ACTIVE) {
                    assertTrue(controller.note.getText().contains("histórico de presenças será preservado"));
                    assertTrue(controller.statusDialogPane.getStyleClass().contains("delete-dialog"));
                } else {
                    assertTrue(controller.note.getText().contains("A aula voltará a ficar ativa"));
                    assertFalse(controller.statusDialogPane.getStyleClass().contains("delete-dialog"));
                }
                return null;
            });
        } finally {
            fx(() -> {
                stage.close();
                return null;
            });
        }
    }
}
