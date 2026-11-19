package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class HomeTodayClassesTest {
    private Parent home;
    private TestController controller;
    private FakeSummaryService summary;
    private VBox list;
    private Label message;
    private final CourseDto course = new CourseDto(10L, "Informática básica", null, null, null, null, null);

    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @BeforeEach
    void setUp() throws Exception {
        fx(() -> {
            summary = new FakeSummaryService();
            controller = new TestController(summary);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            loader.setControllerFactory(type -> controller);
            home = loader.load();
            Scene scene = new Scene(home, 1380, 810);
            for (String css : List.of("application", "navigation", "home")) {
                scene.getStylesheets().add(getClass().getResource("/css/" + css + ".css").toExternalForm());
            }
            list = (VBox) loader.getNamespace().get("todayClassesList");
            message = (Label) loader.getNamespace().get("todayClassesMessage");
            home.applyCss();
            home.layout();
            return null;
        });
    }

    @Test
    void shouldShowCourseTimeAndStatusAndOpenPresencesForSelectedClass() throws Exception {
        ClassDto active = lesson(1L, StatusClass.ACTIVE);
        ClassDto canceled = lesson(2L, StatusClass.CANCELED);
        ClassDto postponed = lesson(3L, StatusClass.POSTPONED);
        summary.classes.complete(List.of(active, canceled, postponed));

        fx(() -> {
            home.applyCss();
            home.layout();
            Assertions.assertEquals(LocalDate.of(2026, 11, 16), summary.requestedDate);
            Assertions.assertEquals(1, summary.calls);
            Assertions.assertEquals(3, list.getChildren().size());
            Assertions.assertEquals("1", ((Label) home.lookup("#classesCount")).getText());
            Assertions.assertFalse(message.isManaged());
            Assertions.assertEquals(course.name(), ((Label) list.getChildren().getFirst().lookup(".home-agenda-course")).getText());
            Assertions.assertEquals("08:00 às 10:30", ((Label) list.getChildren().getFirst().lookup(".home-agenda-time")).getText());
            Assertions.assertEquals("Ativa", ((Label) list.getChildren().get(0).lookup(".home-agenda-status")).getText());
            Assertions.assertEquals("Cancelada", ((Label) list.getChildren().get(1).lookup(".home-agenda-status")).getText());
            Assertions.assertEquals("Adiada", ((Label) list.getChildren().get(2).lookup(".home-agenda-status")).getText());
            ((Button) list.getChildren().get(1).lookup(".home-agenda-button")).fire();
            Assertions.assertSame(canceled, controller.openedClass);
            return null;
        });
    }

    @Test
    void shouldShowEmptyMessageWhenNoClassesAreScheduled() throws Exception {
        summary.classes.complete(List.of());
        fx(() -> {
            Assertions.assertTrue(list.getChildren().isEmpty());
            Assertions.assertTrue(message.isVisible());
            Assertions.assertEquals("Nenhuma aula agendada para hoje.", message.getText());
            Assertions.assertEquals("0", ((Label) home.lookup("#classesCount")).getText());
            return null;
        });
    }

    @Test
    void shouldShowLoadFailureAndRecoverWhenRefreshed() throws Exception {
        summary.classes.completeExceptionally(new IOException("offline"));
        fx(() -> {
            Assertions.assertTrue(message.getText().contains("Não foi possível carregar"));
            Assertions.assertTrue(message.isManaged());
            Assertions.assertEquals("-", ((Label) home.lookup("#classesCount")).getText());
            Assertions.assertEquals("12", ((Label) home.lookup("#peopleCount")).getText());
            summary.classes = CompletableFuture.completedFuture(List.of(lesson(1L, StatusClass.ACTIVE)));
            refresh();
            return null;
        });
        fx(() -> {
            Assertions.assertEquals(1, list.getChildren().size());
            Assertions.assertFalse(message.isVisible());
            Assertions.assertEquals("1", ((Label) home.lookup("#classesCount")).getText());
            return null;
        });
    }

    @Test
    void shouldIgnorePreviousClassesAfterRefresh() throws Exception {
        var previous = summary.classes;
        fx(() -> {
            summary.classes = CompletableFuture.completedFuture(List.of(lesson(2L, StatusClass.POSTPONED)));
            refresh();
            return null;
        });
        previous.complete(List.of(lesson(1L, StatusClass.ACTIVE)));
        fx(() -> {
            Assertions.assertEquals(1, list.getChildren().size());
            Assertions.assertEquals("0", ((Label) home.lookup("#classesCount")).getText());
            ((Button) list.getChildren().getFirst().lookup(".home-agenda-button")).fire();
            Assertions.assertEquals(2L, controller.openedClass.id());
            return null;
        });
    }

    @Test
    void shouldShowRecentPersonsCoursesDatesAndRegistrationStatus() throws Exception {
        summary.recent.complete(List.of(register(1L, "Ana", StatusRegister.ACTIVE), register(2L, "Bruno", StatusRegister.CANCELED)));
        fx(() -> {
            VBox recent = (VBox) home.lookup("#recentRegistersList");
            Assertions.assertEquals(2, recent.getChildren().size());
            Assertions.assertEquals("Ana", ((Label) recent.getChildren().getFirst().lookup(".home-agenda-course")).getText());
            Assertions.assertEquals(course.name(), ((Label) recent.getChildren().getFirst().lookup(".home-agenda-time")).getText());
            Assertions.assertTrue(recent.getChildren().getFirst().lookupAll(".label").stream()
                    .map(Label.class::cast).anyMatch(label -> "16/11/2026".equals(label.getText())));
            Assertions.assertEquals("Cancelada", ((Label) recent.getChildren().get(1).lookup(".home-agenda-status")).getText());
            Assertions.assertFalse(home.lookup("#recentRegistersMessage").isManaged());
            return null;
        });
    }

    @Test
    void shouldShowEmptyMessagesForRecentRegistrationsAndPendingCalls() throws Exception {
        summary.recent.complete(List.of());
        summary.pending.complete(List.of());
        fx(() -> {
            Assertions.assertEquals("Nenhuma inscrição cadastrada.", ((Label) home.lookup("#recentRegistersMessage")).getText());
            Assertions.assertEquals("Nenhuma chamada pendente.", ((Label) home.lookup("#pendingPresencesMessage")).getText());
            Assertions.assertTrue(home.lookup("#recentRegistersMessage").isVisible());
            Assertions.assertTrue(home.lookup("#pendingPresencesMessage").isVisible());
            return null;
        });
    }

    @Test
    void shouldOpenPendingCallWithDateAndRefreshPendingListAfterClosing() throws Exception {
        ClassDto pending = new ClassDto(5L, "2026-11-15", null, "08:00", "10:00", StatusClass.ACTIVE, course);
        summary.pending.complete(List.of(pending));
        fx(() -> {
            VBox pendingList = (VBox) home.lookup("#pendingPresencesList");
            var row = pendingList.getChildren().getFirst();
            Assertions.assertEquals(LocalDateTime.of(2026, 11, 16, 12, 0), summary.requestedTime);
            Assertions.assertEquals("Sem chamada", ((Label) row.lookup(".home-agenda-status")).getText());
            Assertions.assertEquals("15/11/2026 • 08:00 às 10:00", ((Label) row.lookup(".home-agenda-time")).getText());
            summary.pending = CompletableFuture.completedFuture(List.of());
            ((Button) row.lookup(".home-agenda-button")).fire();
            Assertions.assertSame(pending, controller.openedClass);
            return null;
        });
        fx(() -> {
            Assertions.assertTrue(((VBox) home.lookup("#pendingPresencesList")).getChildren().isEmpty());
            Assertions.assertEquals("Nenhuma chamada pendente.", ((Label) home.lookup("#pendingPresencesMessage")).getText());
            return null;
        });
    }

    @Test
    void shouldKeepOtherPanelsAvailableWhenRecentRegistrationsFailAndAllowRetry() throws Exception {
        summary.recent.completeExceptionally(new IOException("offline"));
        summary.pending.complete(List.of(lesson(1L, StatusClass.ACTIVE)));
        fx(() -> {
            Assertions.assertTrue(((Label) home.lookup("#recentRegistersMessage")).getText().contains("Não foi possível carregar"));
            Assertions.assertEquals(1, ((VBox) home.lookup("#pendingPresencesList")).getChildren().size());
            Assertions.assertEquals("12", ((Label) home.lookup("#peopleCount")).getText());
            summary.recent = CompletableFuture.completedFuture(List.of(register(1L, "Ana", StatusRegister.ACTIVE)));
            refresh();
            return null;
        });
        fx(() -> {
            Assertions.assertEquals(1, ((VBox) home.lookup("#recentRegistersList")).getChildren().size());
            Assertions.assertFalse(home.lookup("#recentRegistersMessage").isManaged());
            return null;
        });
    }

    @Test
    void shouldShowVerificationFailureInsteadOfClaimingThereAreNoPendingCalls() throws Exception {
        summary.pending.completeExceptionally(new IOException("offline"));
        summary.recent.complete(List.of(register(1L, "Ana", StatusRegister.ACTIVE)));
        fx(() -> {
            Assertions.assertTrue(((Label) home.lookup("#pendingPresencesMessage")).getText().contains("Não foi possível verificar"));
            Assertions.assertEquals(1, ((VBox) home.lookup("#recentRegistersList")).getChildren().size());
            return null;
        });
    }

    @Test
    void shouldIgnoreOldActivityResultsAfterRefresh() throws Exception {
        var oldRecent = summary.recent;
        var oldPending = summary.pending;
        fx(() -> {
            summary.recent = CompletableFuture.completedFuture(List.of(register(2L, "Bruno", StatusRegister.ACTIVE)));
            summary.pending = CompletableFuture.completedFuture(List.of());
            refresh();
            return null;
        });
        oldRecent.complete(List.of(register(1L, "Ana", StatusRegister.ACTIVE)));
        oldPending.complete(List.of(lesson(1L, StatusClass.ACTIVE)));
        fx(() -> {
            VBox recent = (VBox) home.lookup("#recentRegistersList");
            Assertions.assertEquals("Bruno", ((Label) recent.getChildren().getFirst().lookup(".home-agenda-course")).getText());
            Assertions.assertTrue(((VBox) home.lookup("#pendingPresencesList")).getChildren().isEmpty());
            return null;
        });
    }

    private RegisterDto register(Long id, String name, StatusRegister status) {
        PersonDto person = new PersonDto(id, name, null, null, null, null, null, null, null, null, null, null, null);
        return new RegisterDto(id, person, course, "2026-11-16", status);
    }

    @Test
    void shouldDisplayFailureWhenPresenceScreenCannotOpen() throws Exception {
        summary.classes.complete(List.of(lesson(1L, StatusClass.ACTIVE)));
        fx(() -> {
            controller.openFailure = new IOException("missing FXML");
            ((Button) list.getChildren().getFirst().lookup(".home-agenda-button")).fire();
            Assertions.assertTrue(message.isVisible());
            Assertions.assertTrue(message.getText().contains("Não foi possível abrir as presenças"));
            Assertions.assertEquals(1, list.getChildren().size());
            return null;
        });
    }

    private ClassDto lesson(Long id, StatusClass status) {
        return new ClassDto(id, "2026-11-16", null, "08:00:00", "10:30:00", status, course);
    }

    private void refresh() {
        ((Button) home.lookup(".home-refresh")).fire();
    }

    private static class TestController extends HomeController {
        ClassDto openedClass;
        IOException openFailure;

        TestController(HomeSummaryService summary) {
            super(null, null, summary, CLOCK);
        }

        @Override
        protected void openClassPresences(ClassDto courseClass) throws IOException {
            if (openFailure != null) {
                throw openFailure;
            }
            openedClass = courseClass;
        }
    }

    private static class FakeSummaryService extends HomeSummaryService {
        CompletableFuture<List<ClassDto>> classes = new CompletableFuture<>();
        CompletableFuture<List<RegisterDto>> recent = new CompletableFuture<>();
        CompletableFuture<List<ClassDto>> pending = new CompletableFuture<>();
        LocalDate requestedDate;
        LocalDateTime requestedTime;
        int calls;

        FakeSummaryService() {
            super(null, null, null, null);
        }

        @Override
        public CompletableFuture<Long> activeCourses() { return CompletableFuture.completedFuture(2L); }

        @Override
        public CompletableFuture<Long> activeRegisters() { return CompletableFuture.completedFuture(8L); }

        @Override
        public CompletableFuture<Long> registeredPeople() { return CompletableFuture.completedFuture(12L); }

        @Override
        public CompletableFuture<List<ClassDto>> findTodaysClasses(LocalDate today) {
            calls++;
            requestedDate = today;
            return classes;
        }

        @Override
        public CompletableFuture<List<RegisterDto>> recentRegisters() {
            return recent;
        }

        @Override
        public CompletableFuture<List<ClassDto>> pendingPresences(LocalDateTime now) {
            requestedTime = now;
            return pending;
        }
    }
}
