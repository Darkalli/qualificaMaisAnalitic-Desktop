package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.features.users.UserTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class UsersControllerTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void loadsUsersAndRoles() throws Exception {
        View view = fx(() -> load(CompletableFuture.completedFuture(users())));

        fx(() -> {
            assertEquals("2", view.controller.totalUsersLabel.getText());
            assertEquals(2, view.controller.usersTable.getItems().size());
            assertEquals("ana", view.controller.nameColumn.getCellData(0));
            assertEquals("Admin", view.controller.roleColumn.getCellData(0));
            return null;
        });
    }

    @Test
    void searchesIgnoringCaseAndClearsSearch() throws Exception {
        View view = fx(() -> load(CompletableFuture.completedFuture(users())));

        fx(() -> {
            TextField search = (TextField) view.root.lookup("#searchField");
            search.setText("  BRUNO  ");
            search.fireEvent(new javafx.event.ActionEvent());
            assertEquals(List.of(users().get(1)), List.copyOf(view.controller.usersTable.getItems()));

            ((Hyperlink) view.root.lookup(".clear-filters")).fire();
            assertEquals(2, view.controller.usersTable.getItems().size());
            assertTrue(search.getText().isEmpty());
            return null;
        });
    }

    @Test
    void unmatchedSearchShowsPlaceholder() throws Exception {
        View view = fx(() -> load(CompletableFuture.completedFuture(users())));

        fx(() -> {
            TextField search = (TextField) view.root.lookup("#searchField");
            search.setText("inexistente");
            search.fireEvent(new javafx.event.ActionEvent());
            assertTrue(view.controller.usersTable.getItems().isEmpty());
            assertEquals("Nenhum usuário encontrado para esses filtros.", ((Label) view.controller.usersTable.getPlaceholder()).getText());
            return null;
        });
    }

    @Test
    void expiredSessionShowsLoadError() throws Exception {
        View view = fx(() -> load(CompletableFuture.failedFuture(new ApiException(401, "Expirada"))));

        fx(() -> {
            assertEquals("—", view.controller.totalUsersLabel.getText());
            assertTrue(view.controller.usersTable.getItems().isEmpty());
            assertEquals("Sessão inválida ou expirada. Entre novamente.", ((Label) view.controller.usersTable.getPlaceholder()).getText());
            return null;
        });
    }

    private static View load(CompletableFuture<List<UserDto>> result) throws Exception {
        UserService service = new UserService() {
            @Override public CompletableFuture<List<UserDto>> findAll() { return result; }
        };
        UsersController controller = new UsersController(service);
        Parent root = UserTestSupport.load("users.fxml", controller);
        return new View(controller, root);
    }

    private static List<UserDto> users() {
        return List.of(user(), new UserDto(8L, "bruno", UserRole.AGENT));
    }

    private record View(UsersController controller, Parent root) {}
}
