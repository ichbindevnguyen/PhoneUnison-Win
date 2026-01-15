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
package com.phoneunison.desktop.ui.views;

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
import com.phoneunison.desktop.services.ConnectionService;
import com.phoneunison.desktop.protocol.Message;
import com.phoneunison.desktop.protocol.MessageHandler;
import javafx.application.Platform;
import java.util.Map;
import java.util.List;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * View for SMS messaging.
 */
public class MessagesView extends HBox implements MessageHandler.SMSCallback {

    private final ConnectionService connectionService;
    private ObservableList<Conversation> conversations;
    private ListView<Conversation> conversationList;
    private VBox messagePane;
    private VBox messageContainer;
    private TextField messageInput;
    private Conversation selectedConversation;

    public MessagesView(ConnectionService connectionService) {
        this.connectionService = connectionService;
        initializeUI();
        setupService();
    }

    private void setupService() {
        connectionService.getMessageHandler().setSmsCallback(this);
        // Request conversations if connected
        if (connectionService.connectedProperty().get()) {
            refreshConversations();
        }

        connectionService.connectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                refreshConversations();
            }
        });
    }

    private void refreshConversations() {
        connectionService.sendMessage(null, new Message(Message.SMS_LIST, null));
    }

    private void initializeUI() {
        getStyleClass().add("messages-view");
        setSpacing(0);

        // Left panel - conversation list
        VBox leftPanel = createConversationListPanel();
        leftPanel.setPrefWidth(280);
        leftPanel.setMinWidth(250);

        // Right panel - message view
        messagePane = createMessagePane();
        HBox.setHgrow(messagePane, Priority.ALWAYS);

        getChildren().addAll(leftPanel, messagePane);
    }

    private VBox createConversationListPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("conversation-list-panel");
        panel.setPadding(new Insets(15));

        // Header with new message button
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Messages");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newMsgBtn = new Button("+");
        newMsgBtn.getStyleClass().add("icon-button");
        newMsgBtn.setOnAction(e -> showNewMessageDialog());

        header.getChildren().addAll(titleLabel, spacer, newMsgBtn);

        // Search box
        TextField searchField = new TextField();
        searchField.setPromptText("Search conversations...");
        searchField.getStyleClass().add("search-field");

        // Conversation list
        conversations = FXCollections.observableArrayList();
        conversationList = new ListView<>(conversations);
        conversationList.getStyleClass().add("conversation-list");
        conversationList.setCellFactory(param -> new ConversationCell());
        conversationList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> selectConversation(newVal));
        VBox.setVgrow(conversationList, Priority.ALWAYS);

        panel.getChildren().addAll(header, searchField, conversationList);
        return panel;
    }

    private VBox createMessagePane() {
        VBox pane = new VBox();
        pane.getStyleClass().add("message-pane");

        // Empty state initially
        showEmptyState(pane);

        return pane;
    }

    private void showEmptyState(VBox pane) {
        pane.getChildren().clear();
        pane.setAlignment(Pos.CENTER);

        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("💬");
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label textLabel = new Label("Select a conversation");
        textLabel.getStyleClass().add("placeholder-text");

        Label subLabel = new Label("Choose from your existing conversations or start a new one");
        subLabel.getStyleClass().add("placeholder-subtext");

        placeholder.getChildren().addAll(iconLabel, textLabel, subLabel);
        pane.getChildren().add(placeholder);
    }

    private void selectConversation(Conversation conversation) {
        if (conversation == null) {
            showEmptyState(messagePane);
            return;
        }

        // Request messages for this conversation
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("threadId", conversation.getThreadId());
        connectionService.sendMessage(null, new Message(Message.SMS_MESSAGES, data));

        selectedConversation = conversation;
        messagePane.getChildren().clear();
        messagePane.setAlignment(Pos.TOP_LEFT);

        // Header
        HBox header = new HBox(10);
        header.getStyleClass().add("message-header");
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(20);
        avatar.setFill(Color.web("#3daee9"));

        VBox contactInfo = new VBox(2);
        Label nameLabel = new Label(conversation.getContactName());
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label numberLabel = new Label(conversation.getPhoneNumber());
        numberLabel.getStyleClass().add("phone-number");
        contactInfo.getChildren().addAll(nameLabel, numberLabel);

        header.getChildren().addAll(avatar, contactInfo);

        // Messages container
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("message-scroll");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        messageContainer = new VBox(8);
        messageContainer.getStyleClass().add("message-container");
        messageContainer.setPadding(new Insets(15));
        scrollPane.setContent(messageContainer);

        // Display messages
        for (UiMessage msg : conversation.getMessages()) {
            messageContainer.getChildren().add(createMessageBubble(msg));
        }

        // Input area
        HBox inputArea = new HBox(10);
        inputArea.getStyleClass().add("message-input-area");
        inputArea.setPadding(new Insets(15));
        inputArea.setAlignment(Pos.CENTER);

        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.getStyleClass().add("message-input");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        messageInput.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("send-button");
        sendBtn.setOnAction(e -> sendMessage());

        inputArea.getChildren().addAll(messageInput, sendBtn);

        messagePane.getChildren().addAll(header, scrollPane, inputArea);
    }

    private HBox createMessageBubble(UiMessage message) {
        HBox row = new HBox();
        row.getStyleClass().add("message-row");

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add(message.isIncoming() ? "message-bubble-incoming" : "message-bubble-outgoing");
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setMaxWidth(400);

        Label textLabel = new Label(message.getText());
        textLabel.setWrapText(true);

        Label timeLabel = new Label(formatMessageTime(message.getTimestamp()));
        timeLabel.getStyleClass().add("message-time");

        bubble.getChildren().addAll(textLabel, timeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (message.isIncoming()) {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(bubble, spacer);
        } else {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(spacer, bubble);
        }

        return row;
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || selectedConversation == null)
            return;

        UiMessage message = new UiMessage(text, System.currentTimeMillis(), false);
        selectedConversation.getMessages().add(message);
        messageContainer.getChildren().add(createMessageBubble(message));
        messageInput.clear();

        // Send via ConnectionService
        if (selectedConversation != null) {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("address", selectedConversation.getPhoneNumber()); // Assuming phone number is address
            data.put("body", text);
            connectionService.sendMessage(null, new Message(Message.SMS_SEND, data));
        }
    }

    private void showNewMessageDialog() {
        // TODO: Implement new message dialog
    }

    private String formatMessageTime(long timestamp) {
        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Inner classes for data

    public static class Conversation {
        private final String threadId;
        private final String contactName;
        private final String phoneNumber;
        private final List<UiMessage> messages = new ArrayList<>();

        public Conversation(String threadId, String contactName, String phoneNumber) {
            this.threadId = threadId;
            this.contactName = contactName;
            this.phoneNumber = phoneNumber;
        }

        public String getThreadId() {
            return threadId;
        }

        public String getContactName() {
            return contactName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public List<UiMessage> getMessages() {
            return messages;
        }

        public void addMessage(UiMessage message) {
            messages.add(message);
        }

        public UiMessage getLastMessage() {
            return messages.isEmpty() ? null : messages.get(messages.size() - 1);
        }
    }

    public static class UiMessage {
        private final String text;
        private final long timestamp;
        private final boolean incoming;

        public UiMessage(String text, long timestamp, boolean incoming) {
            this.text = text;
            this.timestamp = timestamp;
            this.incoming = incoming;
        }

        public String getText() {
            return text;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isIncoming() {
            return incoming;
        }
    }

    private class ConversationCell extends ListCell<Conversation> {
        @Override
        protected void updateItem(Conversation item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
            } else {
                HBox cell = new HBox(10);
                cell.getStyleClass().add("conversation-cell");
                cell.setPadding(new Insets(10));
                cell.setAlignment(Pos.CENTER_LEFT);

                Circle avatar = new Circle(22);
                avatar.setFill(Color.web("#3daee9"));

                VBox content = new VBox(4);
                HBox.setHgrow(content, Priority.ALWAYS);

                Label nameLabel = new Label(item.getContactName());
                nameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));

                UiMessage lastMsg = item.getLastMessage();
                Label previewLabel = new Label(lastMsg != null ? lastMsg.getText() : "");
                previewLabel.getStyleClass().add("message-preview");
                previewLabel.setMaxWidth(180);

                content.getChildren().addAll(nameLabel, previewLabel);
                cell.getChildren().addAll(avatar, content);
                setGraphic(cell);
            }
        }
    }

    @Override
    public void onSMSMessage(Message message) {
        Platform.runLater(() -> {
            if (Message.SMS_LIST.equals(message.getType())) {
                updateConversationList(message);
            } else if (Message.SMS_MESSAGES.equals(message.getType())) {
                updateMessages(message);
            }
        });
    }

    private void updateConversationList(Message message) {
        List<Map<String, Object>> convs = (List<Map<String, Object>>) message.getData().get("conversations");
        if (convs == null)
            return;

        conversations.clear();
        for (Map<String, Object> c : convs) {
            String threadId = String.valueOf(c.get("threadId")); // careful with types
            String address = (String) c.get("address");
            String contactName = (String) c.get("contactName");
            // snippet, etc...
            conversations.add(new Conversation(threadId, contactName, address));
        }
    }

    private void updateMessages(Message message) {
        String threadId = (String) message.getData().get("threadId");
        if (selectedConversation == null || !selectedConversation.getThreadId().equals(threadId))
            return;

        List<Map<String, Object>> msgs = (List<Map<String, Object>>) message.getData().get("messages");
        if (msgs == null)
            return;

        selectedConversation.getMessages().clear();
        messageContainer.getChildren().clear();

        for (Map<String, Object> m : msgs) {
            String body = (String) m.get("body");
            Double ts = (Double) m.get("timestamp"); // Gson/Json might parse as double
            long timestamp = ts.longValue();

            // Type 1 = Inbox (Incoming), 2 = Sent (Outgoing)
            // But types might be Double too
            Object typeObj = m.get("type");
            int type = 1;
            if (typeObj instanceof Number)
                type = ((Number) typeObj).intValue();

            boolean incoming = (type == 1);

            UiMessage msg = new UiMessage(body, timestamp, incoming);
            selectedConversation.getMessages().add(msg);
            messageContainer.getChildren().add(createMessageBubble(msg));
        }
    }
}
