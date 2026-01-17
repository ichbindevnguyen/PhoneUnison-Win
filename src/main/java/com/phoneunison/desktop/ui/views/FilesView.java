package com.phoneunison.desktop.ui.views;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import com.phoneunison.desktop.services.ConnectionService;
import com.phoneunison.desktop.protocol.Message;

import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FilesView extends VBox {
    private final File downloadDir = new File(System.getProperty("user.home"), "Downloads/PhoneUnison");
    private final ObservableList<File> files = FXCollections.observableArrayList();
    private final ListView<File> listView;
    private ConnectionService connectionService;
    private Label statusLabel;
    private ProgressBar progressBar;

    public FilesView() {
        this(null);
    }

    public FilesView(ConnectionService connectionService) {
        this.connectionService = connectionService;
        setPadding(new Insets(20));
        setSpacing(15);

        Label title = new Label("Files");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.getStyleClass().add("view-title");

        HBox controls = createControls();

        VBox sendSection = createSendSection();

        Label receivedLabel = new Label("Received Files");
        receivedLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        receivedLabel.setStyle("-fx-text-fill: #bdc3c7;");

        listView = new ListView<>(files);
        listView.setCellFactory(param -> new FileCell());
        listView.setOnMouseClicked(this::handleFileClick);
        listView.getStyleClass().add("files-list");
        VBox.setVgrow(listView, Priority.ALWAYS);

        getChildren().addAll(title, sendSection, new Separator(), receivedLabel, controls, listView);

        refreshFiles();
    }

    private HBox createControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> refreshFiles());

        Button openFolderBtn = new Button("📂 Open Folder");
        openFolderBtn.getStyleClass().add("secondary-button");
        openFolderBtn.setOnAction(e -> openFolder());

        controls.getChildren().addAll(refreshBtn, openFolderBtn);
        return controls;
    }

    private VBox createSendSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: rgba(61, 174, 233, 0.1); -fx-background-radius: 10;");

        Label sendLabel = new Label("Send File to Phone");
        sendLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));

        Label descLabel = new Label("Select a file to send to your connected Android device");
        descLabel.setStyle("-fx-text-fill: #888;");

        Button sendButton = new Button("📤 Send File to Phone");
        sendButton.getStyleClass().add("primary-button");
        sendButton.setStyle(
                "-fx-background-color: #3daee9; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        sendButton.setOnAction(e -> selectAndSendFile());

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #888;");

        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        section.getChildren().addAll(sendLabel, descLabel, sendButton, statusLabel, progressBar);
        return section;
    }

    private void selectAndSendFile() {
        if (connectionService == null || !connectionService.isConnected()) {
            updateStatus("❌ Not connected to device", true);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            sendFile(selectedFile);
        }
    }

    private void sendFile(File file) {
        updateStatus("📤 Sending: " + file.getName() + "...", false);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", file.getName());
        data.put("fileSize", file.length());
        data.put("filePath", file.getAbsolutePath());

        connectionService.sendMessage(null, new Message(Message.FILE_OFFER, data));

        new Thread(() -> {
            try {
                uploadFile(file);
                javafx.application.Platform.runLater(() -> {
                    updateStatus("✅ Sent: " + file.getName(), false);
                    progressBar.setProgress(1);
                    hideProgressAfterDelay();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    updateStatus("❌ Failed to send: " + e.getMessage(), true);
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    private void uploadFile(File file) throws Exception {
        if (connectionService == null)
            throw new Exception("Not connected");

        String host = connectionService.getConnectedDeviceIP();
        int port = 8766;

        if (host == null || host.isEmpty()) {
            throw new Exception("Device IP not available");
        }

        java.net.URL url = new java.net.URL("http://" + host + ":" + port + "/upload?filename=" +
                java.net.URLEncoder.encode(file.getName(), "UTF-8"));

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Content-Length", String.valueOf(file.length()));

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                java.io.OutputStream os = conn.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;
            long fileSize = file.length();

            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                final double progress = (double) totalSent / fileSize;
                javafx.application.Platform.runLater(() -> progressBar.setProgress(progress));
            }
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Server returned: " + responseCode);
        }
    }

    private void updateStatus(String text, boolean isError) {
        statusLabel.setText(text);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");

        FadeTransition ft = new FadeTransition(Duration.millis(200), statusLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideProgressAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setProgress(0);
                });
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    public void refreshFiles() {
        files.clear();
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] fileList = downloadDir.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (f.isFile()) {
                        files.add(f);
                    }
                }
            }
        }
    }

    private void openFolder() {
        try {
            if (!downloadDir.exists())
                downloadDir.mkdirs();
            Desktop.getDesktop().open(downloadDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFileClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            File selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    Desktop.getDesktop().open(selected);
                } catch (IOException e) {
                    try {
                        Runtime.getRuntime().exec(new String[] {
                                "rundll32",
                                "shell32.dll,OpenAs_RunDLL",
                                selected.getAbsolutePath()
                        });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private static class FileCell extends ListCell<File> {
        @Override
        protected void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox cell = new HBox(10);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(8));

                Label icon = new Label(getFileIcon(item.getName()));
                icon.setStyle("-fx-font-size: 20px;");

                VBox info = new VBox(2);
                Label name = new Label(item.getName());
                name.setStyle("-fx-font-weight: bold;");
                Label size = new Label(formatFileSize(item.length()));
                size.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
                info.getChildren().addAll(name, size);

                cell.getChildren().addAll(icon, info);
                setGraphic(cell);
                setText(null);
            }
        }

        private String getFileIcon(String name) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".gif"))
                return "🖼️";
            if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi"))
                return "🎬";
            if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac"))
                return "🎵";
            if (lower.endsWith(".pdf"))
                return "📄";
            if (lower.endsWith(".doc") || lower.endsWith(".docx"))
                return "📝";
            if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z"))
                return "📦";
            return "📁";
        }

        private String formatFileSize(long bytes) {
            if (bytes < 1024)
                return bytes + " B";
            if (bytes < 1024 * 1024)
                return (bytes / 1024) + " KB";
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
