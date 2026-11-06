package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public class UsersController {

    private final UserService userService = new UserService();
    private List<UserDto> allUsers = List.of();
    private boolean loaded;

    @FXML
    public Label totalUsersLabel;
    @FXML
    public TableColumn<UserDto, String> nameColumn;
    @FXML
    public TableColumn<UserDto, String> roleColumn;
    @FXML
    public TableColumn<UserDto, Void> actionsColumn;
    @FXML
    public TableView<UserDto> usersTable;
    @FXML
    private TextField searchField;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().username()));
        roleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().role().getDescription()));
        actionsColumn.setCellFactory(column -> new UserActionsCell(this::loadUsers));
        loadUsers();
    }

    private void loadUsers() {
        usersTable.setPlaceholder(new Label("Carregando usuários..."));
        userService.findAll().whenComplete((users, failure) -> {
            Platform.runLater(() -> {
                if (failure != null) {
                    showLoadError(failure);
                    return;
                }
                allUsers = users;
                loaded = true;

                totalUsersLabel.setText(String.valueOf(allUsers.size()));
                onSearchClick();
            });
        });
    }

    @FXML
    public void onNewUserClick() throws IOException {
        Navigator.openForm(usersTable.getScene(), "/views/pages/users/user-form.fxml",
                (UserFormController controller) -> controller.setOnAdded(this::loadUsers));
    }

    @FXML
    private void onSearchClick() {
        if (!loaded) return;
        String search = searchField.getText().strip().toLowerCase(Locale.ROOT);
        List<UserDto> filtered = allUsers.stream()
                .filter(user -> search.isBlank()
                        || user.username().toLowerCase(Locale.ROOT).contains(search))
                .toList();
        usersTable.getItems().setAll(filtered);
        usersTable.setPlaceholder(new Label(allUsers.isEmpty()
                ? "Nenhum usuário cadastrado." : "Nenhum usuário encontrado para esses filtros."));
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();
        onSearchClick();
    }

    private void showLoadError(Throwable failure) {
        loaded = false;
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = "Não foi possível carregar os usuários.";
        if (cause instanceof ApiException exception && exception.getStatusCode() == 401) {
            message = "Sessão inválida ou expirada. Entre novamente.";
        }
        usersTable.getItems().clear();
        allUsers = List.of();
        totalUsersLabel.setText("—");
        usersTable.setPlaceholder(new Label(message));
    }

    private void configureEmptyText(
            ComboBox<String> combo,
            String emptyText
    ) {
        combo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(item == null ? emptyText : item);
                setGraphic(null);
            }
        });
    }
}
