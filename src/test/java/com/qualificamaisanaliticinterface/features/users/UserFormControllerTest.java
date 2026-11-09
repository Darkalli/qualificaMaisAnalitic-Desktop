package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UserFormControllerTest {

    @BeforeAll
    static void startJavaFx(){
        Platform.startup(() -> Platform.setImplicitExit(false));
    }

    @Test
    void shouldStartWithPasswordHidden() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            Assertions.assertTrue(controller.passwordField.isVisible());
            Assertions.assertFalse(controller.visiblePasswordField.isVisible());
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldShowPasswordWhenToggled() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.passwordField.setText("teste");
            controller.onTogglePassword();
            Assertions.assertFalse(controller.passwordField.isVisible());
            Assertions.assertTrue(controller.visiblePasswordField.isVisible());
            Assertions.assertEquals("teste", controller.visiblePasswordField.getText());
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldPreserveEditedPasswordWhenHiddenAgain() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.passwordField.setText("testeInicial");
            controller.onTogglePassword();
            controller.visiblePasswordField.setText("testeEditado");
            controller.onTogglePassword();
            Assertions.assertTrue(controller.passwordField.isVisible());
            Assertions.assertFalse(controller.visiblePasswordField.isVisible());
            Assertions.assertEquals("testeEditado", controller.passwordField.getText());
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectShortUsernameAndKeepFormEnabled(){
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.userField.setText("ab");
            controller.passwordField.setText("senhaTeste");
            controller.confirmPasswordField.setText("senhaTeste");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("O usuário deve ter no mínimo 3 caracteres.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        try {
            task.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldRejectShortPasswordAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.userField.setText("teste");
            controller.passwordField.setText("1234567");
            controller.confirmPasswordField.setText("1234567");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("A senha deve ter no mínimo 8 caracteres.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectMismatchedPasswordAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.userField.setText("teste");
            controller.passwordField.setText("senhaTeste1");
            controller.confirmPasswordField.setText("senhaTeste2");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("As duas senhas devem ser iguais.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectEmptyFieldsAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.add.fire();
            Assertions.assertEquals("Nenhum campo pode estar vazio.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectEmptyUserFieldAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();

            controller.passwordField.setText("senhaTeste1");
            controller.confirmPasswordField.setText("senhaTeste2");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("O usuário deve ter no mínimo 3 caracteres.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectEmptyPasswordFieldAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.userField.setText("teste");
            controller.confirmPasswordField.setText("senhaTeste2");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("As duas senhas devem ser iguais.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldStartWithConfirmPasswordHidden() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            Assertions.assertTrue(controller.confirmPasswordField.isVisible());
            Assertions.assertFalse(controller.visibleConfirmPasswordField.isVisible());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

        @Test
    void shouldRejectEmptyPasswordConfirmationFieldAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.userField.setText("teste");
            controller.passwordField.setText("senhaTeste1");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("As duas senhas devem ser iguais.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldShowConfirmPasswordWhenToggled() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.confirmPasswordField.setText("teste");
            controller.onTogglePassword();
            Assertions.assertFalse(controller.confirmPasswordField.isVisible());
            Assertions.assertTrue(controller.visibleConfirmPasswordField.isVisible());
            Assertions.assertEquals("teste", controller.visibleConfirmPasswordField.getText());
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldPreserveEditedConfirmPasswordWhenHiddenAgain() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.confirmPasswordField.setText("testeInicial");
            controller.onTogglePassword();
            controller.visibleConfirmPasswordField.setText("testeEditado");
            controller.onTogglePassword();
            Assertions.assertTrue(controller.confirmPasswordField.isVisible());
            Assertions.assertFalse(controller.visibleConfirmPasswordField.isVisible());
            Assertions.assertEquals("testeEditado", controller.confirmPasswordField.getText());
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldFieldBeManagedAndNotManaged() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            Assertions.assertTrue(controller.passwordField.isManaged());
            Assertions.assertFalse(controller.visiblePasswordField.isManaged());
            Assertions.assertTrue(controller.confirmPasswordField.isManaged());
            Assertions.assertFalse(controller.visibleConfirmPasswordField.isManaged());
            controller.onTogglePassword();
            Assertions.assertFalse(controller.passwordField.isManaged());
            Assertions.assertTrue(controller.visiblePasswordField.isManaged());
            Assertions.assertFalse(controller.confirmPasswordField.isManaged());
            Assertions.assertTrue(controller.visibleConfirmPasswordField.isManaged());
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldHaveAllUserRoleInSelector() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            for(UserRole role : UserRole.values()){
                Assertions.assertTrue (controller.roleSelection.getItems().contains(role));
            }
            return null;
        });

        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldChangeTheErrorMessage() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();

            controller.passwordField.setText("senhaTeste1");
            controller.confirmPasswordField.setText("senhaTeste2");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("O usuário deve ter no mínimo 3 caracteres.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            controller.userField.setText("teste");
            controller.passwordField.setText("senha");
            controller.confirmPasswordField.setText("senha");
            controller.roleSelection.setValue(UserRole.AGENT);
            controller.add.fire();
            Assertions.assertEquals("A senha deve ter no mínimo 8 caracteres.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldRejectEmptyRoleFieldAndKeepFormEnabled() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();
            controller.userField.setText("teste");
            controller.passwordField.setText("12345678");
            controller.confirmPasswordField.setText("12345678");
            controller.add.fire();
            Assertions.assertEquals("Nenhum campo pode estar vazio.",controller.info.getText());
            Assertions.assertFalse(controller.formDialogPane.getContent().isDisable());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldAllFieldsBeEmpty() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() ->{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/pages/users/user-form.fxml")
            );
            loader.load();
            UserFormController controller = loader.getController();

            Assertions.assertEquals("",controller.userField.getText());
            Assertions.assertEquals("",controller.passwordField.getText());
            Assertions.assertEquals("",controller.visiblePasswordField.getText());
            Assertions.assertEquals("",controller.confirmPasswordField.getText());
            Assertions.assertEquals("",controller.visibleConfirmPasswordField.getText());
            Assertions.assertNull(controller.roleSelection.getValue());
            Assertions.assertEquals("",controller.info.getText());
            return null;
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }
}