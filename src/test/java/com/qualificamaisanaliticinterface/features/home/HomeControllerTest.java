package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.AuthTestSupport.MemoryTokenStore;
import com.qualificamaisanaliticinterface.AuthTestSupport.StubHttpClient;
import com.qualificamaisanaliticinterface.HelloController;
import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.session.SessionService;
import com.qualificamaisanaliticinterface.session.UserSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class HomeControllerTest {
    private MemoryTokenStore store;
    private FakeAuthService auth;
    private TestController controller;
    private Parent home;
    private Scene scene;
    private Stage stage;
    private Button logout;
    private FakeSummaryService summary;

    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @BeforeEach
    void setUp() throws Exception {
        fx(() -> {
            UserSession.start(session("logout-token"));
            store = new MemoryTokenStore();
            store.saved = GSON.toJson(UserSession.current());
            auth = new FakeAuthService();
            SessionService sessions = new SessionService(store, new StubHttpClient(), BASE_URL, CLOCK);
            summary = new FakeSummaryService();
            controller = new TestController(auth, sessions, summary);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            loader.setControllerFactory(type -> controller);
            home = loader.load();
            scene = new Scene(home, 800, 600);
            stage = new Stage();
            stage.setScene(scene);
            stage.show();
            logout = home.lookupAll(".button").stream().filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(button -> "Sair".equals(button.getText())).findFirst().orElseThrow();
            return null;
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        fx(() -> {
            if (stage != null) {
                stage.close();
            }
            UserSession.clear();
            return null;
        });
    }

    @Test
    void shouldDisplayLoggedInUserDateAndSummaryWithoutWaitingForAllRequests() throws Exception {
        summary.courses.complete(4L);
        summary.registers.complete(18L);
        summary.people.complete(27L);
        fx(() -> {
            Assertions.assertEquals("Olá, ana!", label("greeting").getText());
            Assertions.assertEquals("segunda-feira, 16 de novembro de 2026", label("currentDate").getText());
            Assertions.assertEquals(LocalDate.of(2026, 11, 16), summary.requestedDate);
            Assertions.assertEquals("4", label("coursesCount").getText());
            Assertions.assertEquals("18", label("registersCount").getText());
            Assertions.assertEquals("27", label("peopleCount").getText());
            Assertions.assertEquals("Carregando...", label("classesState").getText());
            return null;
        });
        summary.classes.complete(0L);
        fx(() -> {
            Assertions.assertEquals("0", label("classesCount").getText());
            return null;
        });
    }

    @Test
    void shouldKeepAvailableCountsWhenOneRequestFailsAndAllowRefresh() throws Exception {
        summary.courses.completeExceptionally(new IOException("offline"));
        summary.people.complete(12L);
        fx(() -> {
            Assertions.assertEquals("—", label("coursesCount").getText());
            Assertions.assertEquals("Não foi possível carregar.", label("coursesState").getText());
            Assertions.assertEquals("12", label("peopleCount").getText());
            summary.courses = CompletableFuture.completedFuture(3L);
            home.lookupAll(".button").stream().filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(button -> "Atualizar".equals(button.getText()))
                    .findFirst().orElseThrow().fire();
            return null;
        });
        fx(() -> {
            Assertions.assertEquals("3", label("coursesCount").getText());
            Assertions.assertEquals("Em andamento", label("coursesState").getText());
            return null;
        });
    }

    @Test
    void shouldIgnoreOldSummaryAfterRefresh() throws Exception {
        CompletableFuture<Long> previous = summary.courses;
        fx(() -> {
            summary.courses = CompletableFuture.completedFuture(8L);
            home.lookupAll(".button").stream().filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(button -> "Atualizar".equals(button.getText()))
                    .findFirst().orElseThrow().fire();
            return null;
        });
        previous.complete(2L);
        fx(() -> {
            Assertions.assertEquals("8", label("coursesCount").getText());
            return null;
        });
    }

    @Test
    void shouldClearSavedSessionAndRevokeCurrentTokenBeforeReturningToLogin() throws Exception {
        clickLogout();
        fx(() -> {
            Assertions.assertNull(store.saved);
            Assertions.assertEquals(1, store.clears);
            Assertions.assertEquals(1, auth.calls);
            Assertions.assertEquals("logout-token", auth.token);
            Assertions.assertSame(home, scene.getRoot());
            Assertions.assertTrue(home.isDisable());
            Assertions.assertNotNull(UserSession.current());
            return null;
        });
        auth.result.complete(null);
        fx(() -> {
            assertLoggedOut();
            Assertions.assertTrue(controller.warnings.isEmpty());
            return null;
        });
    }

    @Test
    void shouldFinishLocalLogoutEvenWhenServerRevocationFails() throws Exception {
        clickLogout();
        auth.result.completeExceptionally(new IOException("offline"));
        fx(() -> {
            assertLoggedOut();
            Assertions.assertEquals(1, controller.warnings.size());
            Assertions.assertTrue(controller.warnings.getFirst().contains("servidor"));
            return null;
        });
    }

    @Test
    void shouldIgnoreRepeatedLogoutEventsWhileRevocationIsPending() throws Exception {
        clickLogout();
        fx(() -> {

            var handler = HomeController.class.getDeclaredMethod("onLogoutClick", javafx.event.ActionEvent.class);
            handler.setAccessible(true);
            handler.invoke(controller, new javafx.event.ActionEvent(logout, logout));
            Assertions.assertEquals(1, auth.calls);
            Assertions.assertEquals(1, store.clears);
            Assertions.assertEquals(1, controller.loginLoads);
            return null;
        });
        auth.result.complete(null);
        fx(() -> {
            assertLoggedOut();
            return null;
        });
    }

    @Test
    void shouldKeepCurrentSessionWhenLoginScreenCannotBeLoaded() throws Exception {
        controller.loadFailure = new IOException("missing FXML");
        clickLogout();
        fx(() -> {
            Assertions.assertSame(home, scene.getRoot());
            Assertions.assertFalse(home.isDisable());
            Assertions.assertNotNull(UserSession.current());
            Assertions.assertNotNull(store.saved);
            Assertions.assertEquals(0, store.clears);
            Assertions.assertEquals(0, auth.calls);
            Assertions.assertEquals(1, controller.warnings.size());
            return null;
        });
    }

    @Test
    void shouldKeepCurrentSessionWhenSavedSessionCannotBeClearedAndAllowRetry() throws Exception {
        store.clearFailure = new IOException("file locked");
        clickLogout();
        fx(() -> {
            Assertions.assertSame(home, scene.getRoot());
            Assertions.assertFalse(home.isDisable());
            Assertions.assertNotNull(UserSession.current());
            Assertions.assertNotNull(store.saved);
            Assertions.assertEquals(0, auth.calls);
            Assertions.assertEquals(1, controller.warnings.size());
            store.clearFailure = null;
            logout.fire();
            Assertions.assertEquals(1, auth.calls);
            Assertions.assertEquals(2, store.clears);
            return null;
        });
        auth.result.complete(null);
        fx(() -> {
            assertLoggedOut();
            return null;
        });
    }

    @Test
    void shouldAllowRetryAfterLoginScreenLoadFailure() throws Exception {
        controller.loadFailure = new IOException("missing FXML");
        clickLogout();
        controller.loadFailure = null;
        clickLogout();
        Assertions.assertEquals(1, auth.calls);
        auth.result.complete(null);
        fx(() -> {
            assertLoggedOut();
            return null;
        });
    }

    @Test
    void shouldReturnToLoginWhenMemorySessionIsAlreadyMissing() throws Exception {
        UserSession.clear();
        clickLogout();
        Assertions.assertNull(auth.token);
        auth.result.complete(null);
        fx(() -> {
            assertLoggedOut();
            return null;
        });
    }

    private Label label(String id) {
        return (Label) home.lookup("#" + id);
    }

    private void clickLogout() throws Exception {
        fx(() -> {
            logout.fire();
            return null;
        });
    }

    private void assertLoggedOut() {
        Assertions.assertNull(UserSession.current());
        Assertions.assertNull(store.saved);
        Assertions.assertSame(controller.loginRoot, scene.getRoot());
        Assertions.assertFalse(scene.getRoot().isDisable());
        Assertions.assertFalse(stage.isMaximized());
        Assertions.assertEquals(380, stage.getWidth(), 1);
        Assertions.assertEquals(420, stage.getHeight(), 1);
    }

    private static class FakeAuthService extends AuthService {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        int calls;
        String token;

        FakeAuthService() {
            super(new StubHttpClient(), BASE_URL);
        }

        @Override
        public CompletableFuture<Void> logout(String token) {
            calls++;
            this.token = token;
            return result;
        }
    }

    private static class TestController extends HomeController {
        final SessionService sessions;
        final AuthService auth;
        final List<String> warnings = new ArrayList<>();
        Parent loginRoot;
        int loginLoads;
        IOException loadFailure;

        TestController(AuthService auth, SessionService sessions, HomeSummaryService summary) {
            super(auth, sessions, summary, CLOCK);
            this.auth = auth;
            this.sessions = sessions;
        }

        @Override
        protected Parent loadLoginScreen() throws IOException {
            loginLoads++;
            if (loadFailure != null) {
                throw loadFailure;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            loader.setControllerFactory(type -> new HelloController(auth, sessions));
            loginRoot = loader.load();
            return loginRoot;
        }

        @Override
        protected void showLogoutWarning(String message) {
            warnings.add(message);
        }
    }

    private static class FakeSummaryService extends HomeSummaryService {
        CompletableFuture<Long> courses = new CompletableFuture<>();
        final CompletableFuture<Long> registers = new CompletableFuture<>();
        final CompletableFuture<Long> people = new CompletableFuture<>();
        final CompletableFuture<Long> classes = new CompletableFuture<>();
        LocalDate requestedDate;

        FakeSummaryService() {
            super(null, null, null, null);
        }

        @Override
        public CompletableFuture<Long> activeCourses() { return courses; }

        @Override
        public CompletableFuture<Long> activeRegisters() { return registers; }

        @Override
        public CompletableFuture<Long> registeredPeople() { return people; }

        @Override
        public CompletableFuture<Long> todaysClasses(LocalDate today) {
            requestedDate = today;
            return classes;
        }
    }
}
