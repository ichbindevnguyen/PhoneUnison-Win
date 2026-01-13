package com.phoneunison.desktop.ui;

import javafx.scene.Scene;

public class ThemeManager {
    private static ThemeManager instance;
    private Scene scene;

    private ThemeManager() {
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void registerScene(Scene scene) {
        this.scene = scene;
        applyTheme();
    }

    public void setTheme(String themeName) {
        com.phoneunison.desktop.config.AppConfig config = com.phoneunison.desktop.PhoneUnisonApp.getInstance()
                .getConfig();
        if (config != null) {
            config.setTheme(themeName);
            config.save();
        }
        applyTheme();
    }

    private void applyTheme() {
        if (scene != null) {
            scene.getStylesheets().clear();
            com.phoneunison.desktop.config.AppConfig config = com.phoneunison.desktop.PhoneUnisonApp.getInstance()
                    .getConfig();
            String theme = (config != null) ? config.getTheme() : "light";

            // Map theme names to css files if needed, or use direct naming
            String cssFile = "/css/light.css";
            if ("dark".equalsIgnoreCase(theme)) {
                cssFile = "/css/dark.css";
            } else if ("catppuccin".equalsIgnoreCase(theme)) {
                cssFile = "/css/catppuccin.css";
            }

            scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
        }
    }
}
