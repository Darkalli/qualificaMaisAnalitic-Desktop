package com.qualificamaisanaliticinterface.features.classes;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ClassesActionsCell extends TableCell<ClassDto, Void> {
    private final HBox buttons;
    private final Runnable onClassesChanged;

    public ClassesActionsCell(Runnable onClassesChanged) {
        this.onClassesChanged = onClassesChanged;
        FXMLLoader loader = new FXMLLoader(ClassesActionsCell.class.getResource("/views/pages/classes/class-actions.fxml"));
        try {
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível carregar os botões da aula.", exception);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : buttons);
    }

    @FXML
    public void onClassInfoClick() throws IOException {
        ClassDto courseClass = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/classes/class-info.fxml",
                (ClassInfoController controller) -> controller.setClass(courseClass));
    }

    @FXML
    public void onClassDeleteClick() throws IOException {
        ClassDto courseClass = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/classes/class-status.fxml",
                (ClassStatusController controller) -> {
                    controller.setClass(courseClass);
                    controller.setOnChanged(onClassesChanged);
                });
    }

    @FXML
    public void onUpdateClassClick() throws IOException{
        ClassDto courseClass = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/classes/class-update.fxml",
                (ClassUpdateController controller) -> {
                    controller.setClass(courseClass);
                    controller.setOnChanged(onClassesChanged);
                });
    }
}
