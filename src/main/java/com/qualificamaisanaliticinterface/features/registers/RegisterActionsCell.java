package com.qualificamaisanaliticinterface.features.registers;

import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class RegisterActionsCell extends TableCell<RegisterDto, Void> {
    private final HBox buttons;
    private final Runnable onRegistersChanged;

    public RegisterActionsCell(Runnable onRegistersChanged) {
        this.onRegistersChanged = onRegistersChanged;
        FXMLLoader loader = new FXMLLoader(RegisterActionsCell.class.getResource("/views/pages/registers/register-actions.fxml"));
        try {
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível carregar os botões da inscrição.", exception);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : buttons);
    }

    @FXML
    public void onRegisterInfoClick() throws IOException {
        RegisterDto register = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/registers/register-info.fxml",
                (RegisterInfoController controller) -> controller.setRegister(register));
    }

    @FXML
    public void onRegisterStatusClick() throws IOException {
        RegisterDto register = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/registers/register-status.fxml",
                (RegisterStatusController controller) -> {
                    controller.setRegister(register);
                    controller.setOnChanged(onRegistersChanged);
                });
    }
}
