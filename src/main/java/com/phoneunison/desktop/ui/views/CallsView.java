package com.phoneunison.desktop.ui.views;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CallsView extends VBox {
    public CallsView() {
        setAlignment(Pos.CENTER);
        Label label = new Label("Phone Calls - Coming Soon");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: #888;");
        getChildren().add(label);
    }
}
