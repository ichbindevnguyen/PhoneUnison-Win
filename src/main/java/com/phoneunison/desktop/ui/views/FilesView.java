package com.phoneunison.desktop.ui.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.awt.Desktop;
import java.io.IOException;

public class FilesView extends VBox {
    private final File downloadDir = new File(System.getProperty("user.home"), "Downloads/PhoneUnison");
    private final ObservableList<File> files = FXCollections.observableArrayList();
    private final ListView<File> listView;

    public FilesView() {
        setPadding(new Insets(20));
        setSpacing(15);

        Label title = new Label("Received Files");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshFiles());

        Button openFolderBtn = new Button("Open Folder");
        openFolderBtn.setOnAction(e -> openFolder());

        HBox controls = new HBox(10, refreshBtn, openFolderBtn);

        listView = new ListView<>(files);
        listView.setCellFactory(param -> new FileCell());
        listView.setOnMouseClicked(this::handleFileClick);
        VBox.setVgrow(listView, Priority.ALWAYS);

        getChildren().addAll(title, controls, listView);

        refreshFiles();
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
                    // Fallback: Show Windows "Open With" dialog
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
            } else {
                setText(item.getName() + " (" + (item.length() / 1024) + " KB)");
                setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
            }
        }
    }
}
