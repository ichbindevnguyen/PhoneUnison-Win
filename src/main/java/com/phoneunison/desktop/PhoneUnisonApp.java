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
package com.phoneunison.desktop;

import com.phoneunison.desktop.config.AppConfig;
import com.phoneunison.desktop.services.ConnectionService;
import com.phoneunison.desktop.ui.MainWindow;
import com.phoneunison.desktop.ui.TrayManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneUnisonApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(PhoneUnisonApp.class);

    private static PhoneUnisonApp instance;

    private Stage primaryStage;
    private MainWindow mainWindow;
    private TrayManager trayManager;
    private ConnectionService connectionService;
    private AppConfig config;

    @Override
    public void init() throws Exception {
        instance = this;
        logger.info("Initializing PhoneUnison Desktop...");
        config = AppConfig.load();
        connectionService = new ConnectionService(config);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        logger.info("Starting PhoneUnison Desktop...");
        Platform.setImplicitExit(false);
        mainWindow = new MainWindow(primaryStage, connectionService);
        mainWindow.show();
        trayManager = new TrayManager(this);
        trayManager.addToSystemTray();
        connectionService.start();
        logger.info("PhoneUnison Desktop started successfully");
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down PhoneUnison Desktop...");
        if (connectionService != null) {
            connectionService.stop();
        }
        if (trayManager != null) {
            trayManager.removeFromSystemTray();
        }
        if (config != null) {
            config.save();
        }
        logger.info("PhoneUnison Desktop shut down complete");
    }

    public void showMainWindow() {
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.show();
                primaryStage.toFront();
            }
        });
    }

    public void hideToTray() {
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.hide();
            }
        });
    }

    public void exitApplication() {
        Platform.exit();
        System.exit(0);
    }

    public static PhoneUnisonApp getInstance() {
        return instance;
    }

    public ConnectionService getConnectionService() {
        return connectionService;
    }

    public TrayManager getTrayManager() {
        return trayManager;
    }

    public AppConfig getConfig() {
        return config;
    }
}
