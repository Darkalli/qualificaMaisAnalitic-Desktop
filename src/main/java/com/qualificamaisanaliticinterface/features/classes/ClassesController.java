package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

import static com.qualificamaisanaliticinterface.utils.TextUtils.configureEmptyText;

public class ClassesController {

    @FXML
    public Label presence;
    @FXML
    public TableView<ClassDto> classesTable;
    @FXML
    public TableColumn<ClassDto, String> courseColumn;
    @FXML
    public TableColumn<ClassDto, String> dateColumn;
    @FXML
    public TableColumn<ClassDto, String> startColumn;
    @FXML
    public TableColumn<ClassDto, String> finishColumn;
    @FXML
    public TableColumn<ClassDto, String> statusColumn;
    @FXML
    public TableColumn<ClassDto, Void> ActionsColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Period> periodFilter;
    @FXML
    public DatePicker dateFilter;


    @FXML
    private void initialize() {

        periodFilter.getItems().setAll(Period.values());
        configureEmptyText(periodFilter, "Todos os períodos");
    }

    @FXML
    private void onSearchClick() {
        String search = searchField.getText().trim();

        System.out.println("Texto da pesquisa: " + search);
    }

    @FXML
    private void onClearFiltersClick() {
        searchField.clear();
        dateFilter.setValue(null);
        periodFilter.setValue(null);
    }
}
