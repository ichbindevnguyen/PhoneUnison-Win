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
import com.phoneunison.desktop.utils.QRCodeGenerator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class PairingDialog extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(PairingDialog.class);
    private final ConnectionService connectionService;
    private ImageView qrCodeView;
    private Label pairingCodeLabel;
    private Label ipAddressLabel;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private javafx.beans.value.ChangeListener<Boolean> connectionListener;

    public PairingDialog(Stage owner, ConnectionService connectionService) {
        this.connectionService = connectionService;

        try {
            initModality(Modality.APPLICATION_MODAL);
        } catch (Exception e) {
            logger.warn("Could not set modality: {}", e.getMessage());
        }

        try {
            if (owner != null) {
                initOwner(owner);
            }
        } catch (Exception e) {
            logger.warn("Could not set owner: {}", e.getMessage());
        }

        try {
            initStyle(StageStyle.DECORATED);
        } catch (Exception e) {
            logger.warn("Could not set style: {}", e.getMessage());
        }

        setTitle("Pair Device");
        setResizable(false);

        logger.info("Initializing PairingDialog UI...");
        initializeUI();
        logger.info("PairingDialog UI initialized");

        logger.info("Generating initial pairing code...");
        generatePairingCode();
        logger.info("Initial pairing code generated");

        setupConnectionListener();
        logger.info("PairingDialog fully initialized");
    }

    private void setupConnectionListener() {
        connectionListener = (obs, wasConnected, isConnected) -> {
            if (isConnected) {
                Platform.runLater(() -> {
                    statusLabel.setText("Connected successfully!");
                    statusLabel.setStyle("-fx-text-fill: #27AE60;");
                    progressIndicator.setVisible(false);

                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(this::close);
                        } catch (InterruptedException ignored) {
                        }
                    }).start();
                });
            }
        };
        connectionService.connectedProperty().addListener(connectionListener);
    }

    @Override
    public void close() {
        if (connectionListener != null) {
            connectionService.connectedProperty().removeListener(connectionListener);
        }
        super.close();
    }

    private void initializeUI() {
        VBox root = new VBox(15);
        root.getStyleClass().add("pairing-dialog");
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #2d2d2d;");

        Label titleLabel = new Label("Pair Your Phone");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setStyle("-fx-text-fill: white;");

        Label instructionsLabel = new Label(
                "1. Install PhoneUnison on your Android phone\n" +
                        "2. Open the app and tap 'Connect to PC'\n" +
                        "3. Scan the QR code or enter the code manually");
        instructionsLabel.setTextAlignment(TextAlignment.CENTER);
        instructionsLabel.getStyleClass().add("instructions");
        instructionsLabel.setStyle("-fx-text-fill: #cccccc;");

        StackPane qrContainer = new StackPane();
        qrContainer.getStyleClass().add("qr-container");
        qrContainer.setPadding(new Insets(15));
        qrContainer.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");

        qrCodeView = new ImageView();
        qrCodeView.setFitWidth(180);
        qrCodeView.setFitHeight(180);
        qrCodeView.setPreserveRatio(true);
        qrContainer.getChildren().add(qrCodeView);

        VBox codeSection = new VBox(8);
        codeSection.setAlignment(Pos.CENTER);

        Label orLabel = new Label("Or enter manually:");
        orLabel.getStyleClass().add("or-label");
        orLabel.setStyle("-fx-text-fill: #7F8C8D;");

        HBox codeRow = new HBox(20);
        codeRow.setAlignment(Pos.CENTER);

        VBox ipBox = new VBox(2);
        ipBox.setAlignment(Pos.CENTER);
        Label ipLabel = new Label("IP Address");
        ipLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px;");
        ipAddressLabel = new Label("Loading...");
        ipAddressLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        ipAddressLabel.getStyleClass().add("ip-address");
        ipAddressLabel.setStyle("-fx-text-fill: #3498db;");
        ipBox.getChildren().addAll(ipLabel, ipAddressLabel);

        VBox codeBox = new VBox(2);
        codeBox.setAlignment(Pos.CENTER);
        Label codeLabel = new Label("Code");
        codeLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px;");
        pairingCodeLabel = new Label("------");
        pairingCodeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
        pairingCodeLabel.getStyleClass().add("pairing-code");
        pairingCodeLabel.setStyle("-fx-text-fill: #27ae60;");
        codeBox.getChildren().addAll(codeLabel, pairingCodeLabel);

        codeRow.getChildren().addAll(ipBox, codeBox);
        codeSection.getChildren().addAll(orLabel, codeRow);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);

        statusLabel = new Label("Waiting for connection...");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setStyle("-fx-text-fill: #95a5a6;");

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().addAll(progressIndicator, statusLabel);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button refreshButton = new Button("Refresh Code");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 5;");
        refreshButton.setOnAction(e -> generatePairingCode());

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setStyle(
                "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 5;");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(refreshButton, cancelButton);

        root.getChildren().addAll(titleLabel, instructionsLabel, qrContainer, codeSection, statusBox, buttonBox);

        Scene scene = new Scene(root, 450, 550);

        try {
            ThemeManager.getInstance().registerScene(scene);
        } catch (Exception e) {
            logger.warn("Could not apply theme to pairing dialog: {}", e.getMessage());
        }

        setScene(scene);
        setMinWidth(400);
        setMinHeight(500);
        centerOnScreen();
    }

    private void generatePairingCode() {
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            ipAddressLabel.setText(localIp);

            String pairingCode = connectionService.generatePairingCode();
            String qrContent = connectionService.getPairingQRContent();
            pairingCodeLabel.setText(formatPairingCode(pairingCode));

            javafx.scene.image.Image qrImage = QRCodeGenerator.generateQRCode(qrContent, 180, 180);
            qrCodeView.setImage(qrImage);

            statusLabel.setText("Waiting for connection...");
            statusLabel.setStyle("");
            progressIndicator.setVisible(true);

            logger.info("Generated pairing code: {} for IP: {}", pairingCode, localIp);
        } catch (Exception e) {
            logger.error("Failed to generate pairing code", e);
            statusLabel.setText("Error generating code. Try again.");
            ipAddressLabel.setText("Error");
        }
    }

    private String formatPairingCode(String code) {
        return code.length() == 6 ? code.substring(0, 3) + " " + code.substring(3) : code;
    }
}
