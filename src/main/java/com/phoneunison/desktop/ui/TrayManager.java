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
package com.phoneunison.desktop.ui;

import com.phoneunison.desktop.PhoneUnisonApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class TrayManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TrayManager.class);
    private final PhoneUnisonApp app;
    private TrayIcon trayIcon;
    
    public TrayManager(PhoneUnisonApp app) { this.app = app; }
    
    public void addToSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("System tray is not supported on this platform");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = loadTrayIcon();
            PopupMenu popup = createPopupMenu();
            trayIcon = new TrayIcon(image, "PhoneUnison", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) app.showMainWindow();
                }
            });
            tray.add(trayIcon);
            logger.info("Added to system tray");
        } catch (Exception e) {
            logger.error("Failed to add to system tray", e);
        }
    }
    
    public void removeFromSystemTray() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            logger.info("Removed from system tray");
        }
    }
    
    public void showNotification(String title, String message) {
        if (trayIcon != null) trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
    }
    
    private Image loadTrayIcon() {
        try {
            URL iconUrl = getClass().getResource("/icons/tray-icon.png");
            if (iconUrl != null) return Toolkit.getDefaultToolkit().getImage(iconUrl);
        } catch (Exception e) { logger.warn("Could not load tray icon, using default"); }
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(61, 174, 233));
        g.fillOval(2, 2, 12, 12);
        g.dispose();
        return image;
    }
    
    private PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("Open PhoneUnison");
        openItem.addActionListener(e -> app.showMainWindow());
        MenuItem statusItem = new MenuItem("Status: Disconnected");
        statusItem.setEnabled(false);
        popup.addSeparator();
        MenuItem notifItem = new MenuItem("Notifications");
        notifItem.addActionListener(e -> app.showMainWindow());
        MenuItem msgItem = new MenuItem("Messages");
        msgItem.addActionListener(e -> app.showMainWindow());
        popup.addSeparator();
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> app.exitApplication());
        popup.add(openItem);
        popup.add(statusItem);
        popup.addSeparator();
        popup.add(notifItem);
        popup.add(msgItem);
        popup.addSeparator();
        popup.add(exitItem);
        return popup;
    }
    
    public void updateConnectionStatus(boolean connected, String deviceName) {
        if (trayIcon != null) {
            trayIcon.setToolTip(connected ? "PhoneUnison - Connected to " + deviceName : "PhoneUnison - Disconnected");
        }
    }
}
