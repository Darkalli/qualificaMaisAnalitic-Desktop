package com.qualificamaisanaliticinterface.navigation;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

public class WindowBarController {

    @FXML
    public HBox windowBarBox;
    @FXML
    public FontIcon maximise;
    @FXML
    public Button maximiseButton;
    @FXML
    public Button minimiseButton;
    private boolean maximized;
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    public void onMinimizeClick(ActionEvent actionEvent) {
        Node node = (Node) actionEvent.getSource();
        Stage stage = (Stage) node.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    public void onMaximiseClick(ActionEvent actionEvent) {
        Node node = (Node) actionEvent.getSource();
        Stage stage = (Stage) node.getScene().getWindow();
        if(!maximized) {
            maximise.setIconLiteral("far-clone");
            stage.setMaximized(true);
            maximized = true;
        } else {
            maximise.setIconLiteral("far-square");
            stage.setMaximized(false);
            maximized = false;
        }
    }

    public void setMaximise(){
        maximiseButton.setVisible(false);
        maximiseButton.setManaged(false);
    }

    public void setMinimise(){
        minimiseButton.setVisible(false);
        minimiseButton.setManaged(false);
    }

    @FXML
    public void onCloseClick(ActionEvent actionEvent) {
        Node node = (Node) actionEvent.getSource();
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onDragPressed(MouseEvent event) {
        Window window = windowBarBox.getScene().getWindow();
        dragOffsetX = event.getScreenX() - window.getX();
        dragOffsetY = event.getScreenY() - window.getY();
    }

    @FXML
    private void onDragMoved(MouseEvent event) {
        Window window = windowBarBox.getScene().getWindow();
        window.setX(event.getScreenX() - dragOffsetX);
        window.setY(event.getScreenY() - dragOffsetY);
    }
}
