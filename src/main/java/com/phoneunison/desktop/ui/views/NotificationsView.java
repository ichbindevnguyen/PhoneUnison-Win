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

/**
 * View for displaying phone notifications.
 */
public class NotificationsView extends VBox {

    private ObservableList<NotificationItem> notifications;
    private ListView<NotificationItem> notificationList;

    public NotificationsView() {
        initializeUI();
    }

    private void initializeUI() {
        getStyleClass().add("view-container");
        setSpacing(15);
        setPadding(new Insets(0));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label titleLabel = new Label("Notifications");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("view-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearAllBtn = new Button("Clear All");
        clearAllBtn.getStyleClass().add("secondary-button");
        clearAllBtn.setOnAction(e -> clearAllNotifications());

        header.getChildren().addAll(titleLabel, spacer, clearAllBtn);

        // Notification list
        notifications = FXCollections.observableArrayList();
        notificationList = new ListView<>(notifications);
        notificationList.getStyleClass().add("notification-list");
        notificationList.setCellFactory(param -> new NotificationCell());
        notificationList.setPlaceholder(createEmptyPlaceholder());
        VBox.setVgrow(notificationList, Priority.ALWAYS);

        getChildren().addAll(header, notificationList);
    }

    private VBox createEmptyPlaceholder() {
        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("🔔");
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label textLabel = new Label("No notifications");
        textLabel.getStyleClass().add("placeholder-text");

        Label subLabel = new Label("Notifications from your phone will appear here");
        subLabel.getStyleClass().add("placeholder-subtext");

        placeholder.getChildren().addAll(iconLabel, textLabel, subLabel);
        return placeholder;
    }

    public void addNotification(NotificationItem notification) {
        notifications.add(0, notification);
    }

    public void clearAllNotifications() {
        notifications.clear();
    }

    private void addSampleNotifications() {
        notifications.add(new NotificationItem(
                "WhatsApp", "John Doe", "Hey, are you free for lunch today?",
                "com.whatsapp", System.currentTimeMillis() - 120000));
        notifications.add(new NotificationItem(
                "Gmail", "New email from Amazon", "Your order has been shipped...",
                "com.google.android.gm", System.currentTimeMillis() - 300000));
        notifications.add(new NotificationItem(
                "Calendar", "Meeting in 30 minutes", "Team standup - Conference Room A",
                "com.google.android.calendar", System.currentTimeMillis() - 600000));
    }

    /**
     * Notification item data class.
     */
    public static class NotificationItem {
        private final String appName;
        private final String title;
        private final String content;
        private final String packageName;
        private final long timestamp;

        public NotificationItem(String appName, String title, String content,
                String packageName, long timestamp) {
            this.appName = appName;
            this.title = title;
            this.content = content;
            this.packageName = packageName;
            this.timestamp = timestamp;
        }

        public String getAppName() {
            return appName;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public String getPackageName() {
            return packageName;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Custom cell renderer for notifications.
     */
    private class NotificationCell extends ListCell<NotificationItem> {
        @Override
        protected void updateItem(NotificationItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox cell = new HBox(12);
                cell.getStyleClass().add("notification-cell");
                cell.setPadding(new Insets(12));
                cell.setAlignment(Pos.CENTER_LEFT);

                // App icon placeholder
                StackPane iconPane = new StackPane();
                iconPane.getStyleClass().add("app-icon");
                Circle iconBg = new Circle(20);
                iconBg.setFill(Color.web("#3daee9"));
                Label iconLabel = new Label(getAppEmoji(item.getPackageName()));
                iconLabel.setStyle("-fx-font-size: 18px;");
                iconPane.getChildren().addAll(iconBg, iconLabel);

                // Content
                VBox content = new VBox(4);
                HBox.setHgrow(content, Priority.ALWAYS);

                HBox titleRow = new HBox(8);
                Label appLabel = new Label(item.getAppName());
                appLabel.getStyleClass().add("notification-app");
                Label timeLabel = new Label(formatTime(item.getTimestamp()));
                timeLabel.getStyleClass().add("notification-time");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                titleRow.getChildren().addAll(appLabel, spacer, timeLabel);

                Label titleLabel = new Label(item.getTitle());
                titleLabel.getStyleClass().add("notification-title");
                titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));

                Label contentLabel = new Label(item.getContent());
                contentLabel.getStyleClass().add("notification-content");
                contentLabel.setWrapText(true);

                content.getChildren().addAll(titleRow, titleLabel, contentLabel);

                // Dismiss button
                Button dismissBtn = new Button("✕");
                dismissBtn.getStyleClass().add("dismiss-button");
                dismissBtn.setOnAction(e -> notifications.remove(item));

                cell.getChildren().addAll(iconPane, content, dismissBtn);
                setGraphic(cell);
            }
        }

        private String getAppEmoji(String packageName) {
            if (packageName.contains("whatsapp"))
                return "💬";
            if (packageName.contains("gmail") || packageName.contains("gm"))
                return "📧";
            if (packageName.contains("calendar"))
                return "📅";
            if (packageName.contains("phone"))
                return "📞";
            return "📱";
        }

        private String formatTime(long timestamp) {
            long diff = System.currentTimeMillis() - timestamp;
            if (diff < 60000)
                return "Just now";
            if (diff < 3600000)
                return (diff / 60000) + " min ago";
            if (diff < 86400000)
                return (diff / 3600000) + " hr ago";
            return (diff / 86400000) + " days ago";
        }
    }
}
