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
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
            controller = new TestController(auth, sessions);
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

        TestController(AuthService auth, SessionService sessions) {
            super(auth, sessions);
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
}
