package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.AuthTestSupport.MemoryTokenStore;
import com.qualificamaisanaliticinterface.AuthTestSupport.StubHttpClient;
import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;
import com.qualificamaisanaliticinterface.session.SessionService;
import com.qualificamaisanaliticinterface.session.UserSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class HelloControllerTest {
    private MemoryTokenStore store;
    private FakeAuthService auth;
    private TestController controller;
    private TextField username;
    private PasswordField password;
    private CheckBox persist;
    private Label error;
    private Button login;

    @BeforeAll
    static void startJavaFx() {
        startFx();
    }

    @BeforeEach
    void setUp() throws Exception {
        UserSession.clear();
        fx(() -> {
            store = new MemoryTokenStore();
            auth = new FakeAuthService();
            controller = new TestController(auth, new SessionService(store, new StubHttpClient(), BASE_URL, CLOCK));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            loader.setControllerFactory(type -> controller);
            Parent root = loader.load();
            username = (TextField) loader.getNamespace().get("userField");
            password = (PasswordField) loader.getNamespace().get("passwordField");
            persist = (CheckBox) loader.getNamespace().get("persistLoginCheckBox");
            error = (Label) loader.getNamespace().get("loginErrorLabel");
            login = root.lookupAll(".button").stream().filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(button -> "Login".equals(button.getText())).findFirst().orElseThrow();
            return null;
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        fx(() -> {
            UserSession.clear();
            return null;
        });
    }

    @Test
    void shouldRejectBlankUsernameWithoutCallingAuth() throws Exception {
        fx(() -> {
            username.setText("   ");
            password.setText("password");
            login.fire();
            assertError();
            Assertions.assertTrue(error.getText().contains("usu"));
            Assertions.assertEquals(0, auth.calls);
            Assertions.assertEquals(0, store.saves);
            Assertions.assertEquals(0, store.clears);
            return null;
        });
    }

    @Test
    void shouldRejectBlankPasswordWithoutCallingAuth() throws Exception {
        fx(() -> {
            username.setText("ana");
            password.setText("   ");
            login.fire();
            assertError();
            Assertions.assertTrue(error.getText().contains("senha"));
            Assertions.assertEquals(0, auth.calls);
            return null;
        });
    }

    @Test
    void shouldTrimUsernameAndPreservePasswordExactly() throws Exception {
        submit();
        Assertions.assertEquals("ana", auth.username);
        Assertions.assertEquals(" senha com espaços ", auth.password);
        Assertions.assertEquals(1, auth.calls);
        Assertions.assertNull(UserSession.current());
        Assertions.assertEquals(0, store.saves);
    }

    @Test
    void shouldIgnoreRepeatedClicksWhileLoginIsPending() throws Exception {
        submit();
        fx(() -> {
            login.fire();
            login.fire();
            return null;
        });
        Assertions.assertEquals(1, auth.calls);
        Assertions.assertEquals(0, controller.homeOpens);
        Assertions.assertEquals(0, store.saves);
    }

    @Test
    void shouldPersistSessionAndOpenHomeAfterSuccessfulLogin() throws Exception {
        submit();
        var response = session("login-token");
        auth.result.complete(response);
        fx(() -> {
            Assertions.assertEquals(response, UserSession.current());
            Assertions.assertEquals(response, GSON.fromJson(store.saved, LoginResponse.class));
            Assertions.assertEquals(1, store.saves);
            Assertions.assertEquals(0, store.clears);
            Assertions.assertEquals(1, controller.homeOpens);
            Assertions.assertEquals("", password.getText());
            Assertions.assertEquals("", controller.visiblePasswordField.getText());
            Assertions.assertFalse(error.isVisible());
            Assertions.assertFalse(error.isManaged());
            return null;
        });
    }

    @Test
    void shouldRemovePreviousSavedSessionWhenKeepConnectedIsUnchecked() throws Exception {
        store.saved = GSON.toJson(session("previous-token"));
        fx(() -> {
            persist.setSelected(false);
            return null;
        });
        submit();
        var response = session("new-token");
        auth.result.complete(response);
        fx(() -> {
            Assertions.assertEquals(response, UserSession.current());
            Assertions.assertNull(store.saved);
            Assertions.assertEquals(1, store.clears);
            Assertions.assertEquals(0, store.saves);
            Assertions.assertEquals(1, controller.homeOpens);
            return null;
        });
    }

    @Test
    void shouldShowAuthErrorAndAllowRetry() throws Exception {
        submit();
        auth.result.completeExceptionally(new CompletionException(new CompletionException(
                new AuthService.LoginException("Credenciais recusadas"))));
        fx(() -> {
            assertError();
            Assertions.assertEquals("Credenciais recusadas", error.getText());
            Assertions.assertEquals(0, store.saves);
            Assertions.assertEquals(0, store.clears);
            Assertions.assertFalse(password.getText().isEmpty());
            auth.result = new CompletableFuture<>();
            login.fire();
            Assertions.assertEquals(2, auth.calls);
            Assertions.assertFalse(error.isVisible());
            Assertions.assertFalse(error.isManaged());
            return null;
        });
        auth.result.complete(session("retry-token"));
        fx(() -> {
            Assertions.assertEquals(1, controller.homeOpens);
            return null;
        });
    }

    @Test
    void shouldShowConnectionErrorWithoutLeakingTechnicalDetails() throws Exception {
        submit();
        auth.result.completeExceptionally(new IOException("internal-host:1234 secret diagnostics"));
        fx(() -> {
            assertError();
            Assertions.assertTrue(error.getText().contains("API"));
            Assertions.assertFalse(error.getText().contains("internal-host"));
            Assertions.assertEquals(0, store.saves);
            Assertions.assertEquals(0, store.clears);
            return null;
        });
    }

    @Test
    void shouldStayOnLoginWhenSavingSessionFailsAndAllowRetry() throws Exception {
        store.saveFailure = new IOException("disk unavailable");
        submit();
        auth.result.complete(session("token"));
        fx(() -> {
            assertError();
            Assertions.assertTrue(error.getText().contains("salva"));
            Assertions.assertEquals(1, store.saves);
            Assertions.assertFalse(password.getText().isEmpty());
            store.saveFailure = null;
            auth.result = new CompletableFuture<>();
            login.fire();
            Assertions.assertEquals(2, auth.calls);
            return null;
        });
        auth.result.complete(session("retry-token"));
        fx(() -> {
            Assertions.assertEquals(1, controller.homeOpens);
            return null;
        });
    }

    @Test
    void shouldStayOnLoginWhenClearingSavedSessionFails() throws Exception {
        store.saved = GSON.toJson(session("old-token"));
        store.clearFailure = new IOException("cannot remove file");
        fx(() -> {
            persist.setSelected(false);
            return null;
        });
        submit();
        auth.result.complete(session("new-token"));
        fx(() -> {
            assertError();
            Assertions.assertNotNull(store.saved);
            Assertions.assertEquals(1, store.clears);
            Assertions.assertEquals(0, store.saves);
            return null;
        });
    }

    @Test
    void shouldRejectIncompleteResponseBeforeStartingSession() throws Exception {
        submit();
        auth.result.complete(new LoginResponse("", "Bearer", session("token").expiresAt(), session("token").user()));
        fx(() -> {
            assertError();
            Assertions.assertEquals(0, store.saves);
            return null;
        });
    }

    @Test
    void shouldClearMemorySessionWhenOpeningHomeFails() throws Exception {
        controller.homeFailure = new IOException("missing FXML");
        submit();
        auth.result.complete(session("token"));
        fx(() -> {
            Assertions.assertNull(UserSession.current());
            Assertions.assertEquals(1, controller.homeOpens);
            Assertions.assertTrue(error.isVisible());
            Assertions.assertTrue(error.getText().contains("tela inicial"));
            return null;
        });
    }

    @Test
    void shouldStartWithHiddenPasswordAndRememberLoginSelected() throws Exception {
        fx(() -> {
            Assertions.assertTrue(password.isVisible());
            Assertions.assertFalse(controller.visiblePasswordField.isVisible());
            Assertions.assertFalse(controller.visiblePasswordField.isManaged());
            Assertions.assertTrue(persist.isSelected());
            Assertions.assertFalse(error.isVisible());
            return null;
        });
    }

    @Test
    void shouldPreserveEditedPasswordWhenVisibilityChanges() throws Exception {
        fx(() -> {
            password.setText("initial");
            controller.onTogglePassword(null);
            Assertions.assertFalse(password.isVisible());
            Assertions.assertFalse(password.isManaged());
            Assertions.assertTrue(controller.visiblePasswordField.isVisible());
            Assertions.assertTrue(controller.visiblePasswordField.isManaged());
            Assertions.assertEquals("initial", controller.visiblePasswordField.getText());
            controller.visiblePasswordField.setText("edited");
            controller.onTogglePassword(null);
            Assertions.assertTrue(password.isVisible());
            Assertions.assertTrue(password.isManaged());
            Assertions.assertFalse(controller.visiblePasswordField.isVisible());
            Assertions.assertFalse(controller.visiblePasswordField.isManaged());
            Assertions.assertEquals("edited", password.getText());
            return null;
        });
    }

    private void submit() throws Exception {
        fx(() -> {
            username.setText("  ana  ");
            password.setText(" senha com espaços ");
            login.fire();
            return null;
        });
    }

    private void assertError() {
        Assertions.assertTrue(error.isVisible());
        Assertions.assertTrue(error.isManaged());
        Assertions.assertFalse(error.getText().isBlank());
        Assertions.assertEquals(0, controller.homeOpens);
        Assertions.assertNull(UserSession.current());
    }

    private static class FakeAuthService extends AuthService {
        CompletableFuture<LoginResponse> result = new CompletableFuture<>();
        int calls;
        String username;
        String password;

        FakeAuthService() {
            super(new StubHttpClient(), BASE_URL);
        }

        @Override
        public CompletableFuture<LoginResponse> login(String username, String password) {
            calls++;
            this.username = username;
            this.password = password;
            return result;
        }
    }

    private static class TestController extends HelloController {
        int homeOpens;
        IOException homeFailure;

        TestController(AuthService auth, SessionService sessions) {
            super(auth, sessions);
        }

        @Override
        protected void showHome() throws IOException {
            homeOpens++;
            if (homeFailure != null) {
                throw homeFailure;
            }
        }
    }
}
