/*
 * Copyright 2026 PhoneUnison Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phoneunison.desktop.ui;

import com.phoneunison.desktop.services.ConnectionService;
import com.phoneunison.desktop.ui.views.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainWindow {

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    private final Stage stage;
    private final ConnectionService connectionService;
    private BorderPane root;
    private VBox sidebar;
    private StackPane contentArea;
    private Label connectionStatusLabel;
    private Label deviceNameLabel;
    private Label batteryLabel;
    private Circle connectionIndicator;
    private NotificationsView notificationsView;
    private MessagesView messagesView;
    private CallsView callsView;
    private FilesView filesView;
    private SettingsView settingsView;

    public MainWindow(Stage stage, ConnectionService connectionService) {
        this.stage = stage;
        this.connectionService = connectionService;
        initializeUI();
        setupEventHandlers();
    }

    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-window");
        sidebar = createSidebar();
        root.setLeft(sidebar);
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        contentArea.setPadding(new Insets(20));
        root.setCenter(contentArea);
        notificationsView = new NotificationsView();
        messagesView = new MessagesView(connectionService);
        callsView = new CallsView(connectionService);
        filesView = new FilesView(connectionService);
        settingsView = new SettingsView();
        showView(notificationsView);
        Scene scene = new Scene(root, 1000, 650);
        ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.registerScene(scene);
        stage.setScene(scene);
        stage.setTitle("PhoneUnison");
        stage.setMinWidth(600);
        stage.setMinHeight(450);

        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustLayoutForWidth(newVal.doubleValue());
        });

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
        } catch (Exception e) {
            logger.warn("Could not load app icon");
        }
    }

    private void adjustLayoutForWidth(double width) {
        if (width < 800) {
            sidebar.setPrefWidth(70);
            sidebar.getChildren().forEach(node -> {
                if (node instanceof VBox vbox) {
                    vbox.getChildren().forEach(child -> {
                        if (child instanceof ToggleButton btn) {
                            HBox content = (HBox) btn.getGraphic();
                            if (content != null && content.getChildren().size() > 1) {
                                content.getChildren().get(1).setVisible(false);
                                content.getChildren().get(1).setManaged(false);
                            }
                        }
                    });
                }
            });
        } else {
            sidebar.setPrefWidth(250);
            sidebar.getChildren().forEach(node -> {
                if (node instanceof VBox vbox) {
                    vbox.getChildren().forEach(child -> {
                        if (child instanceof ToggleButton btn) {
                            HBox content = (HBox) btn.getGraphic();
                            if (content != null && content.getChildren().size() > 1) {
                                content.getChildren().get(1).setVisible(true);
                                content.getChildren().get(1).setManaged(true);
                            }
                        }
                    });
                }
            });
        }
        contentArea.setPadding(new Insets(width < 700 ? 10 : 20));
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        sidebar.setSpacing(0);
        VBox deviceHeader = createDeviceHeader();
        VBox navItems = new VBox(5);
        navItems.setPadding(new Insets(20, 15, 20, 15));
        ToggleGroup navGroup = new ToggleGroup();
        ToggleButton notifBtn = createNavButton("🔔", "Notifications", navGroup);
        ToggleButton msgBtn = createNavButton("💬", "Messages", navGroup);
        ToggleButton callBtn = createNavButton("📞", "Calls", navGroup);
        ToggleButton fileBtn = createNavButton("📁", "Files", navGroup);
        notifBtn.setOnAction(e -> showView(notificationsView));
        msgBtn.setOnAction(e -> showView(messagesView));
        callBtn.setOnAction(e -> showView(callsView));
        fileBtn.setOnAction(e -> showView(filesView));
        notifBtn.setSelected(true);
        navItems.getChildren().addAll(notifBtn, msgBtn, callBtn, fileBtn);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        ToggleButton settingsBtn = createNavButton("⚙️", "Settings", navGroup);
        settingsBtn.setOnAction(e -> showView(settingsView));
        VBox bottomNav = new VBox();
        bottomNav.setPadding(new Insets(0, 15, 20, 15));
        bottomNav.getChildren().add(settingsBtn);
        sidebar.getChildren().addAll(deviceHeader, navItems, spacer, bottomNav);
        return sidebar;
    }

    private VBox createDeviceHeader() {
        VBox header = new VBox(10);
        header.getStyleClass().add("device-header");
        header.setPadding(new Insets(20));
        header.setAlignment(Pos.CENTER);
        StackPane phoneIcon = new StackPane();
        phoneIcon.getStyleClass().add("phone-icon");
        phoneIcon.setPrefSize(60, 60);
        Label phoneEmoji = new Label("📱");
        phoneEmoji.setStyle("-fx-font-size: 32px;");
        phoneIcon.getChildren().add(phoneEmoji);
        deviceNameLabel = new Label("No Device Connected");
        deviceNameLabel.getStyleClass().add("device-name");
        deviceNameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        batteryLabel = new Label("");
        batteryLabel.getStyleClass().add("device-battery");
        batteryLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");

        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);
        connectionIndicator = new Circle(5);
        connectionIndicator.setFill(Color.web("#da4453"));
        connectionStatusLabel = new Label("Disconnected");
        connectionStatusLabel.getStyleClass().add("connection-status");
        statusBox.getChildren().addAll(connectionIndicator, connectionStatusLabel);

        Button pairButton = new Button("Pair Device");
        pairButton.getStyleClass().add("pair-button");
        pairButton.setOnAction(e -> showPairingDialog());
        pairButton.visibleProperty().bind(connectionService.connectedProperty().not());
        pairButton.managedProperty().bind(pairButton.visibleProperty());

        header.getChildren().addAll(phoneIcon, deviceNameLabel, batteryLabel, statusBox, pairButton);
        return header;
    }

    private ToggleButton createNavButton(String icon, String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton();
        button.setToggleGroup(group);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");
        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("nav-text");
        content.getChildren().addAll(iconLabel, textLabel);
        button.setGraphic(content);
        return button;
    }

    private void showView(Region view) {
        if (!contentArea.getChildren().isEmpty()) {
            Region currentView = (Region) contentArea.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentView);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                contentArea.getChildren().clear();
                view.setOpacity(0);
                contentArea.getChildren().add(view);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), view);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } else {
            view.setOpacity(0);
            contentArea.getChildren().add(view);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), view);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }
    }

    private void showPairingDialog() {
        PairingDialog dialog = new PairingDialog(stage, connectionService);
        dialog.showAndWait();
    }

    private void setupEventHandlers() {
        connectionService.connectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                connectionIndicator.setFill(Color.web("#27ae60"));
                connectionStatusLabel.setText("Connected");
            } else {
                connectionIndicator.setFill(Color.web("#da4453"));
                connectionStatusLabel.setText("Disconnected");
                deviceNameLabel.setText("No Device Connected");
                batteryLabel.setText("");
            }
        });

        connectionService.deviceNameProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                deviceNameLabel.setText(newVal);
            }
        });

        connectionService.batteryLevelProperty().addListener((obs, oldVal, newVal) -> {
            if (connectionService.connectedProperty().get()) {
                batteryLabel.setText(newVal + "% Battery");
            }
        });

        connectionService.getMessageHandler().setNotificationCallback(
                (id, packageName, appName, title, content, icon) -> {
                    javafx.application.Platform.runLater(() -> {
                        notificationsView.addNotification(
                                new com.phoneunison.desktop.ui.views.NotificationsView.NotificationItem(
                                        appName, title, content, packageName, System.currentTimeMillis()));
                    });

                    // Show Windows system tray popup notification
                    com.phoneunison.desktop.ui.TrayManager trayMgr = com.phoneunison.desktop.PhoneUnisonApp
                            .getInstance().getTrayManager();
                    if (trayMgr != null) {
                        trayMgr.showNotification(appName + ": " + title, content);
                    }
                });
    }

    public void show() {
        stage.show();
    }
}
