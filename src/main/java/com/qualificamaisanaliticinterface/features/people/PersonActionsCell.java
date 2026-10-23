package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

public class PersonActionsCell extends TableCell<PersonDto, Void>{

    private final HBox buttons;

    public PersonActionsCell() {
        FXMLLoader loader = new FXMLLoader(PersonActionsCell.class.getResource("/views/pages/forms/person-actions.fxml"));
        try{
            loader.setController(this);
            buttons = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível carregar os botões de ação de pessoas", e);
        }
    }

    @Override
    protected void updateItem(Void item, boolean empty){
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(buttons);
        }
    }

    @FXML
    public void onPersonInfoClick(ActionEvent actionEvent) throws IOException {
        PersonDto person = getTableRow().getItem();
        Navigator.openForm(getScene(), "/views/pages/forms/person-info.fxml", (PersonInfoController controller) -> controller.getPersonInfo(person));
    }
}
