package com.qualificamaisanaliticinterface.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;

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
}
