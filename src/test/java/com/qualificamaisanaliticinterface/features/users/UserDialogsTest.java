package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import static com.qualificamaisanaliticinterface.features.users.UserTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class UserDialogsTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void validFormSubmitsUserAndNotifiesList() throws Exception {
        RecordingUserService service = new RecordingUserService();
        AtomicInteger changes = new AtomicInteger();
        Dialog<UserFormController> dialog = fx(() -> {
            UserFormController controller = new UserFormController(service);
            Stage stage = show("user-form.fxml", controller);
            controller.setOnAdded(changes::incrementAndGet);
            controller.userField.setText("ana");
            controller.passwordField.setText("senha1234");
            controller.confirmPasswordField.setText("senha1234");
            controller.roleSelection.setValue(UserRole.ADMIN);
            controller.add.fire();
            return new Dialog<>(controller, stage);
        });

        fx(() -> {
            try {
                assertEquals("ana", service.username);
                assertEquals("senha1234", service.password);
                assertEquals(UserRole.ADMIN, service.role);
                assertEquals(1, changes.get());
                assertFalse(dialog.controller.formDialogPane.getContent().isDisable());
                assertFalse(dialog.stage.isShowing());
            } finally { dialog.stage.close(); }
            return null;
        });
    }

    @Test
    void updateDialogPopulatesUserAndRoleChoices() throws Exception {
        fx(() -> {
            UserUpdateController controller = new UserUpdateController();
            UserTestSupport.load("user-update.fxml", controller);
            controller.setUser(user());
            assertEquals("ana", controller.userField.getText());
            assertEquals(UserRole.ADMIN, controller.roleSelection.getValue());
            assertEquals(UserRole.values().length, controller.roleSelection.getItems().size());
            return null;
        });
    }

    @Test
    void updateDialogSubmitsChangesAndNotifiesList() throws Exception {
        RecordingUserService service = new RecordingUserService();
        AtomicInteger changes = new AtomicInteger();
        Dialog<UserUpdateController> dialog = fx(() -> {
            UserUpdateController controller = new UserUpdateController(service);
            Stage stage = show("user-update.fxml", controller);
            controller.setUser(user());
            controller.setOnUpdated(changes::incrementAndGet);
            controller.userField.setText("ana2");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            return new Dialog<>(controller, stage);
        });

        fx(() -> {
            try {
                assertEquals(7L, service.id);
                assertEquals("ana2", service.username);
                assertEquals(UserRole.AGENT, service.role);
                assertEquals(1, changes.get());
                assertFalse(dialog.controller.updateDialogPane.getContent().isDisable());
                assertFalse(dialog.stage.isShowing());
            } finally { dialog.stage.close(); }
            return null;
        });
    }

    @Test
    void updateFailureShowsMessageAndKeepsDialogOpen() throws Exception {
        RecordingUserService service = new RecordingUserService();
        service.updateResult = CompletableFuture.failedFuture(new IllegalArgumentException("Nome indisponível"));
        Dialog<UserUpdateController> dialog = fx(() -> {
            UserUpdateController controller = new UserUpdateController(service);
            Stage stage = show("user-update.fxml", controller);
            controller.setUser(user());
            controller.add.fire();
            return new Dialog<>(controller, stage);
        });

        fx(() -> {
            try {
                assertEquals("Nome indisponível", dialog.controller.info.getText());
                assertFalse(dialog.controller.updateDialogPane.getContent().isDisable());
                assertTrue(dialog.stage.isShowing());
            } finally { dialog.stage.close(); }
            return null;
        });
    }

    @Test
    void deleteDialogShowsSelectedUser() throws Exception {
        fx(() -> {
            UserDeleteController controller = new UserDeleteController();
            UserTestSupport.load("user-delete.fxml", controller);
            controller.getDelectInfo(user());
            assertEquals(7L, controller.id);
            assertEquals("ana", controller.deleteName.getText());
            return null;
        });
    }

    @Test
    void deleteDialogSubmitsIdAndNotifiesList() throws Exception {
        RecordingUserService service = new RecordingUserService();
        AtomicInteger changes = new AtomicInteger();
        Dialog<UserDeleteController> dialog = fx(() -> {
            UserDeleteController controller = new UserDeleteController(service);
            Stage stage = show("user-delete.fxml", controller);
            controller.getDelectInfo(user());
            controller.setOnDeleted(changes::incrementAndGet);
            controller.delete.fire();
            return new Dialog<>(controller, stage);
        });

        fx(() -> {
            try {
                assertEquals(7L, service.id);
                assertEquals(1, changes.get());
                assertFalse(dialog.stage.isShowing());
            } finally { dialog.stage.close(); }
            return null;
        });
    }

    @Test
    void deleteFailureRestoresButtonAndShowsMessage() throws Exception {
        RecordingUserService service = new RecordingUserService();
        service.deleteResult = CompletableFuture.failedFuture(new IllegalArgumentException("Exclusão bloqueada"));
        Dialog<UserDeleteController> dialog = fx(() -> {
            UserDeleteController controller = new UserDeleteController(service);
            Stage stage = show("user-delete.fxml", controller);
            controller.getDelectInfo(user());
            controller.delete.fire();
            return new Dialog<>(controller, stage);
        });

        fx(() -> {
            try {
                assertEquals("Exclusão bloqueada", dialog.controller.advise.getText());
                assertFalse(dialog.controller.delete.isDisabled());
                assertTrue(dialog.stage.isShowing());
            } finally { dialog.stage.close(); }
            return null;
        });
    }

    private static Stage show(String fxml, Object controller) throws Exception {
        Stage stage = new Stage();
        stage.setScene(new Scene(UserTestSupport.load(fxml, controller)));
        stage.show();
        return stage;
    }

    private record Dialog<T>(T controller, Stage stage) {}

    private static class RecordingUserService extends UserService {
        Long id;
        String username;
        String password;
        UserRole role;
        CompletableFuture<String> updateResult = CompletableFuture.completedFuture("ok");
        CompletableFuture<String> deleteResult = CompletableFuture.completedFuture("ok");

        @Override public CompletionStage<String> addUser(String username, String password, UserRole role) {
            this.username = username; this.password = password; this.role = role;
            return CompletableFuture.completedFuture("ok");
        }
        @Override public CompletableFuture<String> updateUser(Long id, String username, UserRole role) {
            this.id = id; this.username = username; this.role = role;
            return updateResult;
        }
        @Override public CompletableFuture<String> deleteUser(Long id) {
            this.id = id;
            return deleteResult;
        }
    }
}
