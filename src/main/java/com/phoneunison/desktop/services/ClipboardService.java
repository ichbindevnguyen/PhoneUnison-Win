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
package com.phoneunison.desktop.services;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class ClipboardService implements ClipboardOwner {
    
    private static final Logger logger = LoggerFactory.getLogger(ClipboardService.class);
    private final ConnectionService connectionService;
    private final Clipboard clipboard;
    private String lastContent = "";
    private boolean enabled = true;
    private volatile boolean running = false;
    private Thread monitorThread;
    
    public ClipboardService(ConnectionService connectionService) {
        this.connectionService = connectionService;
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }
    
    public void start() {
        if (running) return;
        running = true;
        monitorThread = new Thread(this::monitorClipboard, "ClipboardMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
        logger.info("Clipboard service started");
    }
    
    public void stop() {
        running = false;
        if (monitorThread != null) monitorThread.interrupt();
        logger.info("Clipboard service stopped");
    }
    
    public void setContent(String content) {
        if (!enabled || content == null || content.equals(lastContent)) return;
        try {
            lastContent = content;
            StringSelection selection = new StringSelection(content);
            clipboard.setContents(selection, this);
            logger.debug("Clipboard set from device: {} chars", content.length());
        } catch (Exception e) {
            logger.error("Failed to set clipboard", e);
        }
    }
    
    public String getTextContent() {
        try {
            Transferable contents = clipboard.getContents(this);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            logger.debug("Could not get clipboard text: {}", e.getMessage());
        }
        return null;
    }
    
    private void monitorClipboard() {
        while (running) {
            try {
                Thread.sleep(500);
                if (!enabled || !connectionService.isConnected()) continue;
                String currentContent = getTextContent();
                if (currentContent != null && !currentContent.equals(lastContent)) {
                    lastContent = currentContent;
                    sendClipboardToDevice(currentContent);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.debug("Clipboard monitor error: {}", e.getMessage());
            }
        }
    }
    
    private void sendClipboardToDevice(String content) {
        logger.debug("Sending clipboard to device: {} chars", content.length());
    }
    
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {}
    
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
}
