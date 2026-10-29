package com.qualificamaisanaliticinterface.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;

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
}
