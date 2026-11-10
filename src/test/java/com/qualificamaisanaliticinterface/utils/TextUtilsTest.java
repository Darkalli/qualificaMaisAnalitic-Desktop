package com.qualificamaisanaliticinterface.utils;

import com.qualificamaisanaliticinterface.features.enuns.Period;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.qualificamaisanaliticinterface.utils.TextUtils.*;

public class TextUtilsTest {

    @BeforeAll
    static void startJavaFx(){
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            // JavaFX was started by another test class.
        }
    }

    @Test
    void shouldFormatDate(){
        String date = formatDate(null);
        Assertions.assertEquals("", date);
        date = formatDate("2026-11-10");
        Assertions.assertEquals("10/11/2026", date);
        date = formatDate("2026-02-30");
        Assertions.assertEquals("2026-02-30", date);
    }

    @Test
    void shouldShowTextInComboBox() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
               ComboBox<Period> filter = new ComboBox<>();
               filter.getItems().setAll(Period.values());
               configureEmptyText(filter, "Todos os períodos");
               StackPane stackPane = new StackPane(filter);
               Scene scene = new Scene(stackPane);
               stage.setScene(scene);
               stage.show();
               stackPane.applyCss();
               stackPane.layout();
               filter.setValue(Period.AFTERNOON);
               Assertions.assertEquals(Period.AFTERNOON.toString(), filter.getButtonCell().getText());
               filter.setValue(null);
               Assertions.assertEquals("Todos os períodos", filter.getButtonCell().getText());
               return null;
           }finally {
               stage.close();
           }
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldSelectAndCopy() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                TableView<String> table = new TableView<>();
                TableColumn<String, String> column = new TableColumn<>("Valor");
                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
                table.getColumns().add(column);
                table.getItems().add("Teste");
                StackPane stackPane = new StackPane(table);
                Scene scene = new Scene(stackPane);
                stage.setScene(scene);
                stage.show();
                stackPane.applyCss();
                stackPane.layout();
                configureCopy(table);
                Assertions.assertTrue(table.getSelectionModel().isCellSelectionEnabled());
                table.getSelectionModel().select(0, column);
                AtomicReference<KeyEvent> dispatched = new AtomicReference<>();
                table.addEventFilter(KeyEvent.KEY_PRESSED, dispatched::set);
                KeyEvent copy = keyPress(KeyCode.C, true);
                Event.fireEvent(table, copy);
                Assertions.assertEquals("Teste", Clipboard.getSystemClipboard().getString());
                Assertions.assertTrue(dispatched.get().isConsumed());

                putOnClipboard("sentinela");
                KeyEvent otherKey = keyPress(KeyCode.V, true);
                Event.fireEvent(table, otherKey);
                Assertions.assertEquals("sentinela", Clipboard.getSystemClipboard().getString());
                Assertions.assertFalse(dispatched.get().isConsumed());

                KeyEvent noShortcut = keyPress(KeyCode.C, false);
                Event.fireEvent(table, noShortcut);
                Assertions.assertEquals("sentinela", Clipboard.getSystemClipboard().getString());
                Assertions.assertFalse(dispatched.get().isConsumed());

                table.getSelectionModel().clearSelection();
                KeyEvent noSelection = keyPress(KeyCode.C, true);
                Event.fireEvent(table, noSelection);
                Assertions.assertEquals("sentinela", Clipboard.getSystemClipboard().getString());
                Assertions.assertFalse(dispatched.get().isConsumed());

                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(null));
                table.getSelectionModel().select(0, column);
                KeyEvent nullValue = keyPress(KeyCode.C, true);
                Event.fireEvent(table, nullValue);
                Assertions.assertEquals("sentinela", Clipboard.getSystemClipboard().getString());
                Assertions.assertFalse(dispatched.get().isConsumed());
                return null;
            }finally {
                stage.close();
            }
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldCopyFirstSelectedCell() throws ExecutionException, InterruptedException, TimeoutException {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Stage stage = new Stage();
            try {
                TableView<String> table = new TableView<>();
                TableColumn<String, String> column = new TableColumn<>("Valor");
                column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
                table.getColumns().add(column);
                table.getItems().addAll("Primeiro", "Segundo");
                configureCopy(table);
                table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                StackPane stackPane = new StackPane(table);
                stage.setScene(new Scene(stackPane));
                stage.show();
                stackPane.applyCss();
                stackPane.layout();

                table.getSelectionModel().select(0, column);
                table.getSelectionModel().select(1, column);
                String expected = column.getCellData(table.getSelectionModel().getSelectedCells().getFirst().getRow());
                Event.fireEvent(table, keyPress(KeyCode.C, true));
                Assertions.assertEquals(expected, Clipboard.getSystemClipboard().getString());
                return null;
            } finally {
                stage.close();
            }
        });
        Platform.runLater(task);
        task.get(5, TimeUnit.SECONDS);
    }

    private static KeyEvent keyPress(KeyCode code, boolean shortcut) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, shortcut, false, false);
    }

    private static void putOnClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
