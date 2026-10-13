module com.qualificamaisanaliticinterface {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens com.qualificamaisanaliticinterface to javafx.fxml;
    exports com.qualificamaisanaliticinterface;

    opens com.qualificamaisanaliticinterface.features.home to javafx.fxml;

}