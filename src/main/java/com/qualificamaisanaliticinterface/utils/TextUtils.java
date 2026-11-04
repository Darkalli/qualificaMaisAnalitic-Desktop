package com.qualificamaisanaliticinterface.utils;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TextUtils {

    public static <T> void configureEmptyText(
            ComboBox<T> combo,
            String emptyText
    ) {
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                setText(item == null ? emptyText : item.toString());
                setGraphic(null);
            }
        });
    }
    public static String formatDate(String date) {
        if (date == null) return "";
        try {
            return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException invalidDate) {
            return date;
        }
    }

    public static <T> void configureCopy(TableView<T> table){
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isShortcutDown() || event.getCode() != KeyCode.C){
                return;
            }

            var selectedCells = table.getSelectionModel().getSelectedCells();

            if (selectedCells.isEmpty()){
                return;
            }
            TablePosition position = selectedCells.getFirst();
            TableColumn column = position.getTableColumn();

            if(column == null){
                return;
            }

            Object value = column.getCellData(position.getRow());

            if (value == null){
                return;
            }

            ClipboardContent content = new ClipboardContent();
            content.putString(value.toString());
            Clipboard.getSystemClipboard().setContent(content);

            event.consume();
        });
    }
}
