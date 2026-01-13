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
package com.phoneunison.desktop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_DIR = "PhoneUnison";
    private static final String CONFIG_FILE = "config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private int serverPort = 8765;
    private boolean autoStart = true;
    private boolean minimizeToTray = true;
    private boolean startMinimized = false;

    private boolean notificationsEnabled = true;
    private boolean smsEnabled = true;
    private boolean callsEnabled = true;
    private boolean clipboardEnabled = true;
    private boolean fileTransferEnabled = true;

    private boolean wifiEnabled = true;
    private boolean bluetoothEnabled = true;
    private int connectionTimeout = 30000;

    private String theme = "kde-breeze-dark";
    private double windowWidth = 900;
    private double windowHeight = 600;
    private double windowX = -1;
    private double windowY = -1;

    private List<PairedDevice> pairedDevices = new ArrayList<>();
    private String encryptedMasterKey;
    private String downloadDir = System.getProperty("user.home") + "/Downloads/PhoneUnison";

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public static AppConfig load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                AppConfig config = gson.fromJson(json, AppConfig.class);
                logger.info("Configuration loaded from {}", configPath);
                return config;
            } catch (IOException e) {
                logger.error("Failed to load configuration, using defaults", e);
            }
        }
        logger.info("Using default configuration");
        return new AppConfig();
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            String json = gson.toJson(this);
            Files.writeString(configPath, json);
            logger.info("Configuration saved to {}", configPath);
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }

    private static Path getConfigPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, CONFIG_DIR, CONFIG_FILE);
    }

    public void addPairedDevice(PairedDevice device) {
        pairedDevices.removeIf(d -> d.getDeviceId().equals(device.getDeviceId()));
        pairedDevices.add(device);
        save();
    }

    public void removePairedDevice(String deviceId) {
        pairedDevices.removeIf(d -> d.getDeviceId().equals(deviceId));
        save();
    }

    public PairedDevice getPairedDevice(String deviceId) {
        return pairedDevices.stream()
                .filter(d -> d.getDeviceId().equals(deviceId))
                .findFirst()
                .orElse(null);
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }

    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }

    public boolean isStartMinimized() {
        return startMinimized;
    }

    public void setStartMinimized(boolean startMinimized) {
        this.startMinimized = startMinimized;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public boolean isCallsEnabled() {
        return callsEnabled;
    }

    public void setCallsEnabled(boolean callsEnabled) {
        this.callsEnabled = callsEnabled;
    }

    public boolean isClipboardEnabled() {
        return clipboardEnabled;
    }

    public void setClipboardEnabled(boolean clipboardEnabled) {
        this.clipboardEnabled = clipboardEnabled;
    }

    public boolean isFileTransferEnabled() {
        return fileTransferEnabled;
    }

    public void setFileTransferEnabled(boolean fileTransferEnabled) {
        this.fileTransferEnabled = fileTransferEnabled;
    }

    public boolean isWifiEnabled() {
        return wifiEnabled;
    }

    public void setWifiEnabled(boolean wifiEnabled) {
        this.wifiEnabled = wifiEnabled;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothEnabled;
    }

    public void setBluetoothEnabled(boolean bluetoothEnabled) {
        this.bluetoothEnabled = bluetoothEnabled;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(double windowWidth) {
        this.windowWidth = windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(double windowHeight) {
        this.windowHeight = windowHeight;
    }

    public double getWindowX() {
        return windowX;
    }

    public void setWindowX(double windowX) {
        this.windowX = windowX;
    }

    public double getWindowY() {
        return windowY;
    }

    public void setWindowY(double windowY) {
        this.windowY = windowY;
    }

    public List<PairedDevice> getPairedDevices() {
        return pairedDevices;
    }

    public String getEncryptedMasterKey() {
        return encryptedMasterKey;
    }

    public void setEncryptedMasterKey(String encryptedMasterKey) {
        this.encryptedMasterKey = encryptedMasterKey;
    }
}
