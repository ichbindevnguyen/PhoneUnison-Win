package com.phoneunison.desktop.ui.views;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import com.phoneunison.desktop.services.ConnectionService;
import com.phoneunison.desktop.protocol.Message;
import com.phoneunison.desktop.protocol.MessageHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallsView extends VBox implements MessageHandler.CallCallback {

    private final ConnectionService connectionService;
    private TextField phoneNumberField;
    private ComboBox<SimCard> simSelector;
    private ObservableList<SimCard> simCards;
    private VBox incomingCallOverlay;
    private Label incomingCallerLabel;
    private Label incomingNumberLabel;
    private Label callStatusLabel;
    private boolean isInCall = false;

    public CallsView(ConnectionService connectionService) {
        this.connectionService = connectionService;
        initializeUI();
        setupCallbacks();
    }

    public CallsView() {
        this.connectionService = null;
        initializeUI();
    }

    private void setupCallbacks() {
        if (connectionService != null) {
            connectionService.getMessageHandler().setCallCallback(this);
            connectionService.connectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    requestSimList();
                }
            });

            connectionService.getMessageHandler().setSimListCallback(message -> {
                Platform.runLater(() -> updateSimList(message));
            });

            if (connectionService.isConnected()) {
                requestSimList();
            }
        }
    }

    private void requestSimList() {
        if (connectionService != null && connectionService.isConnected()) {
            connectionService.sendMessage(null, new Message(Message.SIM_LIST_REQUEST, null));
        }
    }

    private void initializeUI() {
        getStyleClass().add("calls-view");
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Phone Calls");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.getStyleClass().add("view-title");

        VBox dialerContainer = createDialer();
        VBox callStatusContainer = createCallStatusArea();

        incomingCallOverlay = createIncomingCallOverlay();
        incomingCallOverlay.setVisible(false);
        incomingCallOverlay.setManaged(false);

        StackPane mainContent = new StackPane();
        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(title, dialerContainer, callStatusContainer);

        mainContent.getChildren().addAll(content, incomingCallOverlay);
        getChildren().add(mainContent);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
    }

    private VBox createDialer() {
        VBox dialer = new VBox(15);
        dialer.setAlignment(Pos.CENTER);
        dialer.setMaxWidth(350);
        dialer.getStyleClass().add("dialer-container");
        dialer.setPadding(new Insets(20));

        phoneNumberField = new TextField();
        phoneNumberField.setPromptText("Enter phone number");
        phoneNumberField.getStyleClass().add("phone-input");
        phoneNumberField.setStyle("-fx-font-size: 24px; -fx-alignment: center;");
        phoneNumberField.setMaxWidth(300);

        simCards = FXCollections.observableArrayList();
        simCards.add(new SimCard(-1, "Default SIM", ""));

        simSelector = new ComboBox<>(simCards);
        simSelector.setPromptText("Select SIM");
        simSelector.getStyleClass().add("sim-selector");
        simSelector.setMaxWidth(300);
        simSelector.getSelectionModel().selectFirst();

        GridPane dialPad = createDialPad();

        HBox callButtons = new HBox(20);
        callButtons.setAlignment(Pos.CENTER);

        Button callButton = new Button("📞 Call");
        callButton.getStyleClass().addAll("call-button", "primary-button");
        callButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 12 40;");
        callButton.setOnAction(e -> makeCall());

        Button clearButton = new Button("⌫");
        clearButton.getStyleClass().add("clear-button");
        clearButton.setStyle("-fx-font-size: 18px; -fx-padding: 12 20;");
        clearButton.setOnAction(e -> {
            String text = phoneNumberField.getText();
            if (!text.isEmpty()) {
                phoneNumberField.setText(text.substring(0, text.length() - 1));
            }
        });

        callButtons.getChildren().addAll(callButton, clearButton);

        dialer.getChildren().addAll(phoneNumberField, simSelector, dialPad, callButtons);
        return dialer;
    }

    private GridPane createDialPad() {
        GridPane pad = new GridPane();
        pad.setAlignment(Pos.CENTER);
        pad.setHgap(15);
        pad.setVgap(15);

        String[][] buttons = {
                { "1", "2", "3" },
                { "4", "5", "6" },
                { "7", "8", "9" },
                { "*", "0", "#" }
        };

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                String digit = buttons[row][col];
                Button btn = createDialButton(digit);
                pad.add(btn, col, row);
            }
        }

        return pad;
    }

    private Button createDialButton(String digit) {
        Button btn = new Button(digit);
        btn.getStyleClass().add("dial-button");
        btn.setStyle("-fx-font-size: 24px; -fx-min-width: 70px; -fx-min-height: 70px; " +
                "-fx-background-radius: 35; -fx-background-color: #3d4f5f; -fx-text-fill: white;");
        btn.setOnAction(e -> {
            phoneNumberField.setText(phoneNumberField.getText() + digit);
            animateButton(btn);
        });

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 24px; -fx-min-width: 70px; -fx-min-height: 70px; " +
                "-fx-background-radius: 35; -fx-background-color: #4d6070; -fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 24px; -fx-min-width: 70px; -fx-min-height: 70px; " +
                "-fx-background-radius: 35; -fx-background-color: #3d4f5f; -fx-text-fill: white;"));

        return btn;
    }

    private void animateButton(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
        st.setToX(0.9);
        st.setToY(0.9);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private VBox createCallStatusArea() {
        VBox statusArea = new VBox(10);
        statusArea.setAlignment(Pos.CENTER);
        statusArea.setPadding(new Insets(20));

        callStatusLabel = new Label("");
        callStatusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        callStatusLabel.getStyleClass().add("call-status");

        statusArea.getChildren().add(callStatusLabel);
        return statusArea;
    }

    private VBox createIncomingCallOverlay() {
        VBox overlay = new VBox(20);
        overlay.setAlignment(Pos.CENTER);
        overlay.getStyleClass().add("incoming-call-overlay");
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-padding: 40;");

        Circle avatar = new Circle(50);
        avatar.setFill(Color.web("#3daee9"));

        incomingCallerLabel = new Label("Unknown");
        incomingCallerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        incomingCallerLabel.setStyle("-fx-text-fill: white;");

        incomingNumberLabel = new Label("");
        incomingNumberLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        incomingNumberLabel.setStyle("-fx-text-fill: #aaa;");

        Label incomingLabel = new Label("Incoming Call");
        incomingLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        incomingLabel.setStyle("-fx-text-fill: #27ae60;");

        HBox actionButtons = new HBox(40);
        actionButtons.setAlignment(Pos.CENTER);

        Button answerBtn = new Button("✓ Answer");
        answerBtn.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 15 30; -fx-background-radius: 25;");
        answerBtn.setOnAction(e -> answerCall());

        Button rejectBtn = new Button("✕ Reject");
        rejectBtn.setStyle(
                "-fx-background-color: #da4453; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 15 30; -fx-background-radius: 25;");
        rejectBtn.setOnAction(e -> rejectCall());

        actionButtons.getChildren().addAll(answerBtn, rejectBtn);

        overlay.getChildren().addAll(avatar, incomingCallerLabel, incomingNumberLabel, incomingLabel, actionButtons);
        return overlay;
    }

    private void makeCall() {
        String number = phoneNumberField.getText().trim();
        if (number.isEmpty()) {
            callStatusLabel.setText("Please enter a phone number");
            return;
        }

        if (connectionService == null || !connectionService.isConnected()) {
            callStatusLabel.setText("Not connected to device");
            return;
        }

        SimCard selectedSim = simSelector.getValue();
        int subscriptionId = selectedSim != null ? selectedSim.subscriptionId : -1;

        Map<String, Object> data = new HashMap<>();
        data.put("phoneNumber", number);
        data.put("subscriptionId", subscriptionId);

        connectionService.sendMessage(null, new Message(Message.CALL_DIAL, data));
        callStatusLabel.setText("Dialing " + number + "...");
    }

    private void answerCall() {
        if (connectionService != null && connectionService.isConnected()) {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "answer");
            connectionService.sendMessage(null, new Message(Message.CALL_ACTION, data));
        }
        hideIncomingCallOverlay();
    }

    private void rejectCall() {
        if (connectionService != null && connectionService.isConnected()) {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "reject");
            connectionService.sendMessage(null, new Message(Message.CALL_ACTION, data));
        }
        hideIncomingCallOverlay();
    }

    private void showIncomingCallOverlay(String name, String number) {
        incomingCallerLabel.setText(name != null && !name.isEmpty() ? name : "Unknown");
        incomingNumberLabel.setText(number != null ? number : "");

        incomingCallOverlay.setVisible(true);
        incomingCallOverlay.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(200), incomingCallOverlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideIncomingCallOverlay() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), incomingCallOverlay);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            incomingCallOverlay.setVisible(false);
            incomingCallOverlay.setManaged(false);
        });
        ft.play();
    }

    @Override
    public void onCallState(String state, String number, String contactName) {
        Platform.runLater(() -> {
            switch (state) {
                case "ringing" -> {
                    isInCall = true;
                    showIncomingCallOverlay(contactName, number);
                    callStatusLabel.setText("Incoming call from " + (contactName != null ? contactName : number));
                }
                case "offhook" -> {
                    isInCall = true;
                    hideIncomingCallOverlay();
                    callStatusLabel.setText("In call with " + (contactName != null ? contactName : number));
                }
                case "idle" -> {
                    isInCall = false;
                    hideIncomingCallOverlay();
                    callStatusLabel.setText("Call ended");
                }
                default -> callStatusLabel.setText("Call state: " + state);
            }
        });
    }

    private void updateSimList(Message message) {
        if (message.getData() == null)
            return;

        Object simsObj = message.getData().get("sims");
        if (!(simsObj instanceof List))
            return;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sims = (List<Map<String, Object>>) simsObj;

        simCards.clear();
        for (Map<String, Object> sim : sims) {
            int subscriptionId = sim.get("subscriptionId") instanceof Number
                    ? ((Number) sim.get("subscriptionId")).intValue()
                    : -1;
            String displayName = sim.get("displayName") instanceof String
                    ? (String) sim.get("displayName")
                    : "SIM";
            String carrierName = sim.get("carrierName") instanceof String
                    ? (String) sim.get("carrierName")
                    : "";

            simCards.add(new SimCard(subscriptionId, displayName, carrierName));
        }

        if (!simCards.isEmpty()) {
            simSelector.getSelectionModel().selectFirst();
        }
    }

    public static class SimCard {
        final int subscriptionId;
        final String displayName;
        final String carrierName;

        public SimCard(int subscriptionId, String displayName, String carrierName) {
            this.subscriptionId = subscriptionId;
            this.displayName = displayName;
            this.carrierName = carrierName;
        }

        @Override
        public String toString() {
            if (carrierName != null && !carrierName.isEmpty()) {
                return displayName + " (" + carrierName + ")";
            }
            return displayName;
        }
    }
}
