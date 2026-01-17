package com.phoneunison.desktop.ui;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static ThemeManager instance;
    private final List<Scene> registeredScenes = new ArrayList<>();

    private ThemeManager() {
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void registerScene(Scene scene) {
        if (scene != null && !registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyThemeToScene(scene);
        }
    }

    public void setTheme(String themeName) {
        try {
            com.phoneunison.desktop.config.AppConfig config = com.phoneunison.desktop.PhoneUnisonApp.getInstance()
                    .getConfig();
            if (config != null) {
                config.setTheme(themeName);
                config.save();
            }
        } catch (Exception e) {
            logger.warn("Could not save theme preference: {}", e.getMessage());
        }
        applyTheme();
    }

    private void applyTheme() {
        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene);
        }
    }

    private void applyThemeToScene(Scene scene) {
        if (scene == null) {
            return;
        }

        try {
            scene.getStylesheets().clear();

            String theme = "light";
            try {
                com.phoneunison.desktop.config.AppConfig config = com.phoneunison.desktop.PhoneUnisonApp.getInstance()
                        .getConfig();
                if (config != null && config.getTheme() != null) {
                    theme = config.getTheme();
                }
            } catch (Exception e) {
                logger.debug("Could not get theme from config, using default: {}", e.getMessage());
            }

            String cssFile = "/css/light.css";
            if ("dark".equalsIgnoreCase(theme)) {
                cssFile = "/css/dark.css";
            } else if ("catppuccin".equalsIgnoreCase(theme)) {
                cssFile = "/css/catppuccin.css";
            }

            URL cssResource = getClass().getResource(cssFile);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
                logger.debug("Applied theme CSS: {}", cssFile);
            } else {
                logger.warn("Could not find CSS resource: {}", cssFile);
            }
        } catch (Exception e) {
            logger.warn("Failed to apply theme to scene: {}", e.getMessage());
        }
    }
}
