module com.qualificamaisanaliticinterface {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.google.gson;
    requires com.sun.jna.platform;
    requires com.google.errorprone.annotations;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.qualificamaisanaliticinterface
            to javafx.fxml;
    exports com.qualificamaisanaliticinterface;
    exports com.qualificamaisanaliticinterface.session;
    exports com.qualificamaisanaliticinterface.features.login.dto;
    exports com.qualificamaisanaliticinterface.features.enuns;

    opens com.qualificamaisanaliticinterface.features.home
            to javafx.fxml;
    opens com.qualificamaisanaliticinterface.features.login.dto
            to com.google.gson;
    opens com.qualificamaisanaliticinterface.features.people.dto
            to com.google.gson;
    opens com.qualificamaisanaliticinterface.features.people to javafx.fxml;
    opens com.qualificamaisanaliticinterface.features.registers to javafx.fxml;
    opens com.qualificamaisanaliticinterface.features.registers.dto to com.google.gson;
    opens com.qualificamaisanaliticinterface.features.courses to javafx.fxml;
    opens com.qualificamaisanaliticinterface.features.courses.dto to com.google.gson;

}