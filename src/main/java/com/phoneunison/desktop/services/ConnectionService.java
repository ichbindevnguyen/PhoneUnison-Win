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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.phoneunison.desktop.config.AppConfig;
import com.phoneunison.desktop.config.PairedDevice;
import com.phoneunison.desktop.protocol.Message;
import com.phoneunison.desktop.protocol.MessageHandler;
import com.phoneunison.desktop.utils.CryptoUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import com.phoneunison.desktop.network.UDPDiscoveryService;

public class ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);
    private static final Gson gson = new GsonBuilder().create();

    private final AppConfig config;
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final ConcurrentHashMap<String, Channel> connectedDevices = new ConcurrentHashMap<>();
    private final MessageHandler messageHandler;
    private UDPDiscoveryService udpDiscovery;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private String currentPairingCode;
    private String currentPublicKey;
    private long pairingExpiry;
    private final IntegerProperty batteryLevel = new SimpleIntegerProperty(0);
    private final StringProperty deviceNameProperty = new SimpleStringProperty("No Device Connected");
    private String connectedDeviceName;

    public ConnectionService(AppConfig config) {
        this.config = config;
        this.messageHandler = new MessageHandler(this);

        // Auto-accept file offers
        this.messageHandler.setFileCallback(message -> {
            if (Message.FILE_OFFER.equals(message.getType())) {
                String fileName = message.getDataField("fileName");
                String uri = message.getDataField("uri");

                if (fileName == null || fileName.isEmpty()) {
                    logger.warn("Received file offer with null/empty fileName");
                    return;
                }

                logger.info("Received file offer: {}", fileName);

                // Send accept
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("fileName", fileName);
                if (uri != null) {
                    data.put("uri", uri);
                }

                sendMessage(null, new Message(Message.FILE_ACCEPT, data));
            }
        });
    }

    public void start() {
        if (serverChannel != null && serverChannel.isActive()) {
            logger.warn("Server already running");
            return;
        }
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new com.phoneunison.desktop.network.BasicFileUploadHandler(
                                    config.getDownloadDir()));
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketServerProtocolHandler("/phoneunison"));
                            pipeline.addLast(new WebSocketFrameHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind(config.getServerPort()).sync();
            serverChannel = future.channel();
            logger.info("WebSocket server started on port {}", config.getServerPort());

            startUDPDiscovery();

        } catch (Exception e) {
            logger.error("Failed to start WebSocket server", e);
            stop();
        }
    }

    private void startUDPDiscovery() {
        try {
            String deviceId = "pc-" + System.getProperty("user.name") + "-" + System.currentTimeMillis();
            String deviceName = InetAddress.getLocalHost().getHostName();

            udpDiscovery = new UDPDiscoveryService(config.getServerPort(), deviceId, deviceName);
            udpDiscovery.setListener((alias, deviceModel, deviceType, fingerprint, ip, port) -> {
                logger.info("Discovered device via UDP: {} ({}) at {}:{}", alias, deviceType, ip, port);
            });
            udpDiscovery.start();

            logger.info("UDP Discovery started - broadcasting on port {}", UDPDiscoveryService.DISCOVERY_PORT);
        } catch (Exception e) {
            logger.error("Failed to start UDP Discovery", e);
        }
    }

    public void stop() {
        if (udpDiscovery != null) {
            udpDiscovery.stop();
            udpDiscovery = null;
        }
        if (serverChannel != null)
            serverChannel.close();
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
        connectedDevices.clear();
        Platform.runLater(() -> connected.set(false));
        logger.info("WebSocket server stopped");
    }

    public String generatePairingCode() {
        SecureRandom random = new SecureRandom();
        currentPairingCode = String.format("%06d", random.nextInt(1000000));
        pairingExpiry = System.currentTimeMillis() + 300000;
        try {
            currentPublicKey = CryptoUtils.generateKeyPair();
        } catch (Exception e) {
            logger.error("Failed to generate key pair", e);
            currentPublicKey = "";
        }
        logger.info("Generated pairing code: {}", currentPairingCode);
        return currentPairingCode;
    }

    public String getPairingQRContent() {
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            return String.format("{\"ip\":\"%s\",\"port\":%d,\"code\":\"%s\",\"key\":\"%s\"}", localIp,
                    config.getServerPort(), currentPairingCode, currentPublicKey);
        } catch (Exception e) {
            logger.error("Failed to get local IP", e);
            return "";
        }
    }

    public boolean validatePairing(String code, String deviceId, String devicePublicKey) {
        if (System.currentTimeMillis() > pairingExpiry) {
            logger.warn("Pairing code expired");
            return false;
        }
        if (code == null || !code.equals(currentPairingCode)) {
            logger.warn("Invalid pairing code: received '{}', expected '{}'", code, currentPairingCode);
            return false;
        }
        logger.info("Pairing validated for device: {}", deviceId);
        return true;
    }

    public void confirmConnection(String deviceId, String deviceName, Channel channel) {
        connectedDevices.put(deviceId, channel);
        currentPairingCode = null;
        pairingExpiry = 0;
        this.connectedDeviceName = deviceName;
        Platform.runLater(() -> {
            connected.set(true);
            deviceNameProperty.set(deviceName);
        });

        PairedDevice device = new PairedDevice(deviceId, deviceName, "");
        device.setLastConnected(System.currentTimeMillis());
        config.addPairedDevice(device);

        Platform.runLater(() -> connected.set(true));
        logger.info("Device connected and registered: {} ({})", deviceName, deviceId);
    }

    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    public String getConnectedDeviceIP() {
        for (Channel channel : connectedDevices.values()) {
            if (channel.isActive()) {
                java.net.SocketAddress address = channel.remoteAddress();
                if (address instanceof java.net.InetSocketAddress) {
                    return ((java.net.InetSocketAddress) address).getAddress().getHostAddress();
                }
            }
        }
        return null;
    }

    public StringProperty deviceNameProperty() {
        return deviceNameProperty;
    }

    public IntegerProperty batteryLevelProperty() {
        return batteryLevel;
    }

    public void updateBatteryLevel(int level) {
        Platform.runLater(() -> batteryLevel.set(level));
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void sendMessage(String deviceId, Message message) {
        if (deviceId == null) {
            // Broadcast to all connected devices
            broadcast(message);
            return;
        }
        Channel channel = connectedDevices.get(deviceId);
        if (channel != null && channel.isActive()) {
            String json = gson.toJson(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        } else {
            logger.warn("Device not connected: {}", deviceId);
        }
    }

    public void broadcast(Message message) {
        String json = gson.toJson(message);
        TextWebSocketFrame frame = new TextWebSocketFrame(json);
        for (Channel channel : connectedDevices.values()) {
            if (channel.isActive())
                channel.writeAndFlush(frame.copy());
        }
    }

    public BooleanProperty connectedProperty() {
        return connected;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public AppConfig getConfig() {
        return config;
    }

    private class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        private String deviceId;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
            String json = frame.text();
            logger.debug("Received: {}", json);
            try {
                Message message = gson.fromJson(json, Message.class);
                messageHandler.handleMessage(ctx.channel(), message);
            } catch (Exception e) {
                logger.error("Failed to parse message", e);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            logger.info("Client connected: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (deviceId != null) {
                connectedDevices.remove(deviceId);
                logger.info("Device disconnected: {}", deviceId);
            }
            Platform.runLater(() -> connected.set(!connectedDevices.isEmpty()));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("WebSocket error", cause);
            ctx.close();
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
            connectedDevices.put(deviceId, ctx().channel());
            Platform.runLater(() -> connected.set(true));
        }

        private ChannelHandlerContext ctx() {
            return null;
        }
    }
}
