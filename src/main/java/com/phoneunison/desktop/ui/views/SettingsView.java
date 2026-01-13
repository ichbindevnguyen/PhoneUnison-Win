package com.phoneunison.desktop.ui.views;

import com.phoneunison.desktop.PhoneUnisonApp;
import com.phoneunison.desktop.config.AppConfig;
import com.phoneunison.desktop.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class SettingsView extends VBox {

        private final AppConfig config;

        public SettingsView() {
                this.config = PhoneUnisonApp.getInstance().getConfig();

                setPadding(new Insets(20));
                setSpacing(20);
                setAlignment(Pos.TOP_LEFT);

                Label title = new Label("Settings");
                title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

                GridPane grid = new GridPane();
                grid.setHgap(15);
                grid.setVgap(15);

                // Theme Selection
                Label themeLabel = new Label("Theme:");
                ComboBox<String> themeCombo = new ComboBox<>();
                themeCombo.getItems().addAll("Light", "Dark", "Catppuccin");
                themeCombo.setValue(capitalize(config.getTheme()));
                themeCombo.setOnAction(e -> {
                        String selected = themeCombo.getValue().toLowerCase();
                        if (config != null) {
                                config.setTheme(selected);
                                ThemeManager.getInstance().setTheme(selected);
                        }
                });

                // Download Directory
                Label downloadLabel = new Label("Downloads:");
                TextField downloadField = new TextField(config.getDownloadDir());
                downloadField.setPrefWidth(300);
                downloadField.setEditable(false);

                Button browseBtn = new Button("Browse...");
                browseBtn.setOnAction(e -> {
                        DirectoryChooser chooser = new DirectoryChooser();
                        chooser.setTitle("Select Download Directory");
                        File initialDir = new File(config.getDownloadDir());
                        if (initialDir.exists())
                                chooser.setInitialDirectory(initialDir);

                        File selected = chooser.showDialog(getScene().getWindow());
                        if (selected != null) {
                                String path = selected.getAbsolutePath();
                                downloadField.setText(path);
                                config.setDownloadDir(path);
                                config.save();
                        }
                });

                grid.add(themeLabel, 0, 0);
                grid.add(themeCombo, 1, 0);

                grid.add(downloadLabel, 0, 1);
                grid.add(downloadField, 1, 1);
                grid.add(browseBtn, 2, 1);

                // About Section
                VBox aboutBox = new VBox(10);
                aboutBox.setPadding(new Insets(20, 0, 0, 0));

                Label aboutTitle = new Label("About");
                aboutTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

                Label versionLabel = new Label("PhoneUnison v1.0.0");
                versionLabel.setStyle("-fx-font-size: 14px;");

                Label descLabel = new Label("Open-source phone-PC connectivity app inspired by KDE design");
                descLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
                descLabel.setWrapText(true);

                Label authorLabel = new Label("Author: iBDN");
                authorLabel.setStyle("-fx-font-size: 13px;");

                Hyperlink githubLink = new Hyperlink("GitHub: github.com/ichbindevnguyen");
                githubLink.setStyle("-fx-font-size: 12px;");
                githubLink.setOnAction(e -> {
                        try {
                                java.awt.Desktop.getDesktop()
                                                .browse(new java.net.URI("https://github.com/ichbindevnguyen"));
                        } catch (Exception ex) {
                                ex.printStackTrace();
                        }
                });

                Label copyrightLabel = new Label("© 2026 iBDN. Apache License 2.0");
                copyrightLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

                aboutBox.getChildren().addAll(aboutTitle, versionLabel, descLabel, authorLabel, githubLink,
                                copyrightLabel);

                getChildren().addAll(title, grid, aboutBox);
        }

        private String capitalize(String str) {
                if (str == null || str.isEmpty())
                        return "Light";
                if (str.equalsIgnoreCase("kde-breeze-dark"))
                        return "Dark"; // Migration compatibility
                return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
}
