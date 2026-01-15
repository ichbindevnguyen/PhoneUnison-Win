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
package com.phoneunison.desktop.protocol;

import com.phoneunison.desktop.services.ConnectionService;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final ConnectionService connectionService;
    private NotificationCallback notificationCallback;
    private SMSCallback smsCallback;
    private CallCallback callCallback;
    private ClipboardCallback clipboardCallback;
    private FileCallback fileCallback;
    private SimListCallback simListCallback;

    public MessageHandler(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public void handleMessage(Channel channel, Message message) {
        if (message == null || message.getType() == null) {
            logger.warn("Received invalid message");
            return;
        }
        logger.debug("Handling message type: {}", message.getType());
        switch (message.getType()) {
            case Message.HEARTBEAT -> handleHeartbeat(channel, message);
            case Message.PAIRING_REQUEST -> handlePairingRequest(channel, message);
            case Message.NOTIFICATION -> handleNotification(message);
            case Message.SMS_LIST, Message.SMS_MESSAGES, Message.SMS_RECEIVED -> handleSMS(message);
            case Message.CALL_STATE -> handleCallState(message);
            case Message.SIM_LIST -> handleSimList(message);
            case Message.CLIPBOARD -> handleClipboard(message);
            case Message.FILE_OFFER, Message.FILE_CHUNK, Message.FILE_COMPLETE -> handleFile(message);
            case Message.ERROR -> handleError(message);
            default -> logger.warn("Unknown message type: {}", message.getType());
        }
    }

    private void handleHeartbeat(Channel channel, Message message) {
        // Extract battery level if present
        try {
            if (message.getData() != null && message.getData().containsKey("battery")) {
                Object batteryObj = message.getData().get("battery");
                if (batteryObj instanceof Number) {
                    int level = ((Number) batteryObj).intValue();
                    connectionService.updateBatteryLevel(level);
                } else if (batteryObj instanceof String) {
                    try {
                        int level = Integer.parseInt((String) batteryObj);
                        connectionService.updateBatteryLevel(level);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse battery info", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        Message response = new Message(Message.HEARTBEAT, data);
        sendMessage(channel, response);
    }

    private void handlePairingRequest(Channel channel, Message message) {
        String code = message.getDataField("code");
        String deviceId = message.getDataField("deviceId");
        String deviceName = message.getDataField("deviceName");
        String publicKey = message.getDataField("publicKey");

        logger.info("Pairing request from: {} with code: {}", deviceName, code);

        boolean valid = connectionService.validatePairing(code, deviceId, publicKey);

        Map<String, Object> data = new HashMap<>();
        data.put("success", valid);

        if (valid) {
            data.put("deviceId", getLocalDeviceId());
            data.put("deviceName", getLocalDeviceName());
            connectionService.confirmConnection(deviceId, deviceName, channel);
            logger.info("Pairing successful with: {}", deviceName);
        } else {
            logger.warn("Pairing failed - invalid code");
        }

        Message response = new Message(Message.PAIRING_RESPONSE, data);
        sendMessage(channel, response);
    }

    private void sendMessage(Channel channel, Message message) {
        if (channel != null && channel.isActive()) {
            String json = new com.google.gson.Gson().toJson(message);
            channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(json));
        }
    }

    private void handleNotification(Message message) {
        if (notificationCallback != null) {
            String id = message.getDataField("id");
            String packageName = message.getDataField("packageName");
            String appName = message.getDataField("appName");
            String title = message.getDataField("title");
            String content = message.getDataField("text");
            String icon = message.getDataField("icon");
            notificationCallback.onNotification(id, packageName, appName, title, content, icon);
        }
    }

    private void handleSMS(Message message) {
        if (smsCallback != null)
            smsCallback.onSMSMessage(message);
    }

    private void handleCallState(Message message) {
        if (callCallback != null) {
            String state = message.getDataField("state");
            String number = message.getDataField("number");
            String contactName = message.getDataField("contactName");
            callCallback.onCallState(state, number, contactName);
        }
    }

    private void handleClipboard(Message message) {
        if (clipboardCallback != null) {
            String content = message.getDataField("content");
            String contentType = message.getDataField("contentType");
            clipboardCallback.onClipboardContent(content, contentType);
        }
    }

    private void handleFile(Message message) {
        if (fileCallback != null)
            fileCallback.onFileMessage(message);
    }

    private void handleError(Message message) {
        String code = message.getDataField("code");
        String errorMessage = message.getDataField("message");
        logger.error("Device error: {} - {}", code, errorMessage);
    }

    private String getLocalDeviceId() {
        return "pc-" + System.getProperty("user.name");
    }

    private String getLocalDeviceName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Windows PC";
        }
    }

    public interface NotificationCallback {
        void onNotification(String id, String packageName, String appName, String title, String content, String icon);
    }

    public interface SMSCallback {
        void onSMSMessage(Message message);
    }

    public interface CallCallback {
        void onCallState(String state, String number, String contactName);
    }

    public interface ClipboardCallback {
        void onClipboardContent(String content, String contentType);
    }

    public interface FileCallback {
        void onFileMessage(Message message);
    }

    public interface SimListCallback {
        void onSimList(Message message);
    }

    public void setNotificationCallback(NotificationCallback callback) {
        this.notificationCallback = callback;
    }

    public void setSmsCallback(SMSCallback callback) {
        this.smsCallback = callback;
    }

    public void setCallCallback(CallCallback callback) {
        this.callCallback = callback;
    }

    public void setClipboardCallback(ClipboardCallback callback) {
        this.clipboardCallback = callback;
    }

    public void setFileCallback(FileCallback callback) {
        this.fileCallback = callback;
    }

    public void setSimListCallback(SimListCallback callback) {
        this.simListCallback = callback;
    }

    private void handleSimList(Message message) {
        if (simListCallback != null) {
            simListCallback.onSimList(message);
        }
    }
}
