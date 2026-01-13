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
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        initStyle(StageStyle.DECORATED);
        setTitle("Pair Device");
        setResizable(false);
        initializeUI();
        generatePairingCode();
        setupConnectionListener();
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

        Label titleLabel = new Label("Pair Your Phone");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.getStyleClass().add("dialog-title");

        Label instructionsLabel = new Label(
                "1. Install PhoneUnison on your Android phone\n" +
                        "2. Open the app and tap 'Connect to PC'\n" +
                        "3. Scan the QR code or enter the code manually");
        instructionsLabel.setTextAlignment(TextAlignment.CENTER);
        instructionsLabel.getStyleClass().add("instructions");

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
        ipBox.getChildren().addAll(ipLabel, ipAddressLabel);

        VBox codeBox = new VBox(2);
        codeBox.setAlignment(Pos.CENTER);
        Label codeLabel = new Label("Code");
        codeLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px;");
        pairingCodeLabel = new Label("------");
        pairingCodeLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
        pairingCodeLabel.getStyleClass().add("pairing-code");
        codeBox.getChildren().addAll(codeLabel, pairingCodeLabel);

        codeRow.getChildren().addAll(ipBox, codeBox);
        codeSection.getChildren().addAll(orLabel, codeRow);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);

        statusLabel = new Label("Waiting for connection...");
        statusLabel.getStyleClass().add("status-label");

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().addAll(progressIndicator, statusLabel);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button refreshButton = new Button("Refresh Code");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> generatePairingCode());

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> close());

        buttonBox.getChildren().addAll(refreshButton, cancelButton);

        root.getChildren().addAll(titleLabel, instructionsLabel, qrContainer, codeSection, statusBox, buttonBox);

        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);
        setScene(scene);
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
