package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.qualificamaisanaliticinterface.features.people.PersonTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class PeopleControllerTest {
    @BeforeAll static void startJavaFx() { startFx(); }

    @Test
    void loadsPeopleAndDisplaysTableValues() throws Exception {
        View view = fx(() -> load(CompletableFuture.completedFuture(people())));

        fx(() -> {
            assertEquals("2", ((Label) view.root.lookup("#totalPeopleLabel")).getText());
            assertEquals(2, view.table().getItems().size());
            assertEquals("Ana Silva", view.table().getColumns().get(1).getCellData(0));
            assertEquals("ana@example.com", view.controller.emailsColumn.getCellData(0));
            assertEquals("11999999999", view.controller.cellphoneColumn.getCellData(0));
            assertTrue(view.table().getSelectionModel().isCellSelectionEnabled());
            assertTrue(((ComboBox<?>) view.root.lookup("#courseFilter")).isDisabled());
            return null;
        });
    }

    @Test
    void searchesByNameAndCpfAndClearsSearch() throws Exception {
        View view = fx(() -> load(CompletableFuture.completedFuture(people())));

        fx(() -> {
            TextField search = (TextField) view.root.lookup("#searchField");
            search.setText("  ANA  ");
            search.fireEvent(new javafx.event.ActionEvent());
            assertEquals(List.of(people().getFirst()), List.copyOf(view.table().getItems()));

            search.setText("98765432100");
            search.fireEvent(new javafx.event.ActionEvent());
            assertEquals(List.of(people().get(1)), List.copyOf(view.table().getItems()));

            ((ComboBox<Period>) view.root.lookup("#periodFilter")).setValue(Period.AFTERNOON);
            ((Hyperlink) view.root.lookup(".clear-filters")).fire();
            assertEquals(2, view.table().getItems().size());
            assertTrue(search.getText().isEmpty());
            assertNull(((ComboBox<?>) view.root.lookup("#periodFilter")).getValue());
            return null;
        });
    }

    @Test
    void unmatchedSearchShowsPlaceholder() throws Exception {
        View view = fx(() -> load(CompletableFuture.completedFuture(people())));

        fx(() -> {
            TextField search = (TextField) view.root.lookup("#searchField");
            search.setText("inexistente");
            search.fireEvent(new javafx.event.ActionEvent());
            assertTrue(view.table().getItems().isEmpty());
            assertEquals("Nenhuma pessoa encontrada para essa busca.", ((Label) view.table().getPlaceholder()).getText());
            return null;
        });
    }

    @Test
    void expiredSessionDisablesSearchAndShowsError() throws Exception {
        View view = fx(() -> load(CompletableFuture.failedFuture(new ApiException(401, "Expirada"))));

        fx(() -> {
            String message = "Sessão inválida ou expirada. Entre novamente.";
            assertEquals("—", ((Label) view.root.lookup("#totalPeopleLabel")).getText());
            assertEquals(message, ((Label) view.table().getPlaceholder()).getText());
            assertTrue(((TextField) view.root.lookup("#searchField")).isDisabled());
            return null;
        });
    }

    private static View load(CompletableFuture<List<PersonDto>> result) throws Exception {
        PersonService service = new PersonService() {
            @Override public CompletableFuture<List<PersonDto>> findAll() { return result; }
        };
        FXMLLoader loader = new FXMLLoader(PeopleControllerTest.class.getResource("/views/pages/people/people.fxml"));
        loader.setControllerFactory(type -> new PeopleController(service));
        Parent root = loader.load();
        return new View(loader.getController(), root);
    }

    private static List<PersonDto> people() {
        PersonDto ana = person();
        PersonDto bruno = new PersonDto(8L, "Bruno Lima", null, "98765432100", null,
                null, false, null, null, null, null, null, null);
        return List.of(ana, bruno);
    }

    private record View(PeopleController controller, Parent root) {
        @SuppressWarnings("unchecked")
        TableView<PersonDto> table() { return (TableView<PersonDto>) root.lookup("#peopleTable"); }
    }
}
