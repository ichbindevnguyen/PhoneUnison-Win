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

            String theme = "kde-breeze";
            try {
                com.phoneunison.desktop.config.AppConfig config = com.phoneunison.desktop.PhoneUnisonApp.getInstance()
                        .getConfig();
                if (config != null && config.getTheme() != null) {
                    theme = config.getTheme();
                }
            } catch (Exception e) {
                logger.debug("Could not get theme from config, using default: {}", e.getMessage());
            }

            String cssFile = mapThemeToCssFile(theme);
            logger.info("Loading theme: {} -> {}", theme, cssFile);

            URL cssResource = getClass().getResource(cssFile);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
                logger.info("Applied theme CSS: {}", cssFile);
            } else {
                logger.warn("Could not find CSS resource: {}, trying fallback", cssFile);
                URL fallback = getClass().getResource("/styles/kde-breeze.css");
                if (fallback != null) {
                    scene.getStylesheets().add(fallback.toExternalForm());
                    logger.info("Applied fallback theme CSS");
                } else {
                    logger.error("No CSS files found - UI may look incorrect");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to apply theme to scene: {}", e.getMessage(), e);
        }
    }

    private String mapThemeToCssFile(String theme) {
        if (theme == null) {
            return "/styles/kde-breeze.css";
        }

        String lowerTheme = theme.toLowerCase();

        if (lowerTheme.contains("kde-breeze-light") || lowerTheme.equals("light")) {
            return "/styles/kde-breeze-light.css";
        } else if (lowerTheme.contains("kde-breeze") || lowerTheme.equals("dark")) {
            return "/styles/kde-breeze.css";
        } else if (lowerTheme.contains("catppuccin-mocha")) {
            return "/styles/catppuccin-mocha.css";
        } else if (lowerTheme.contains("catppuccin-macchiato")) {
            return "/styles/catppuccin-macchiato.css";
        } else if (lowerTheme.contains("catppuccin-frappe")) {
            return "/styles/catppuccin-frappe.css";
        } else if (lowerTheme.contains("catppuccin-latte")) {
            return "/styles/catppuccin-latte.css";
        } else if (lowerTheme.contains("catppuccin")) {
            return "/styles/catppuccin-mocha.css";
        }

        return "/styles/kde-breeze.css";
    }
}
