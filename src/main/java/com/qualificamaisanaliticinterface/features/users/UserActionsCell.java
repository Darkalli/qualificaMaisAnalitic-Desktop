package com.qualificamaisanaliticinterface.features.users;

import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class UserActionsCell extends TableCell<UserDto, Void> {

    private final HBox buttons;
    private final Runnable onUserChanged;

    public UserActionsCell(Runnable onUserChanged) {
        this.onUserChanged = onUserChanged;
        FXMLLoader loader = new FXMLLoader(UserActionsCell.class.getResource("/views/pages/users/user-actions.fxml"));
        try {
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível carregar os botões de ação de usuários", e);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(buttons);
        }
    }

    @FXML
    public void onUpdateUserClick(ActionEvent actionEvent) throws IOException {
        UserDto user = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/users/user-update.fxml",
                (UserUpdateController controller) -> {
                    controller.setUser(user);
                    controller.setOnUpdated(onUserChanged);
                });
    }

    @FXML
    public void onDeleteUserClick(ActionEvent actionEvent) throws IOException {
        UserDto user = getTableRow().getItem();

        Navigator.openForm(getScene(), "/views/pages/users/user-delete.fxml",
                (UserDeleteController controller) -> {
                    controller.getDelectInfo(user);
                    controller.setOnDeleted(onUserChanged);
                });
    }
}
