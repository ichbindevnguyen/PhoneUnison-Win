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
package com.phoneunison.desktop.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(UDPDiscoveryService.class);
    private static final Gson gson = new GsonBuilder().create();

    public static final String MULTICAST_ADDRESS = "224.0.0.167";
    public static final int DISCOVERY_PORT = 53318;

    private final int serverPort;
    private final String deviceId;
    private final String deviceName;

    private MulticastSocket multicastSocket;
    private DatagramSocket unicastSocket;
    private InetAddress multicastGroup;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private DiscoveryListener listener;

    public interface DiscoveryListener {
        void onDeviceDiscovered(String alias, String deviceModel, String deviceType,
                String fingerprint, String ip, int port);
    }

    public UDPDiscoveryService(int serverPort, String deviceId, String deviceName) {
        this.serverPort = serverPort;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    public void setListener(DiscoveryListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running.get()) {
            logger.warn("UDP Discovery already running");
            return;
        }

        executor = Executors.newFixedThreadPool(2);
        running.set(true);

        try {
            multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS);

            multicastSocket = new MulticastSocket(DISCOVERY_PORT);
            multicastSocket.setReuseAddress(true);
            multicastSocket.joinGroup(new InetSocketAddress(multicastGroup, DISCOVERY_PORT),
                    NetworkInterface.getByInetAddress(getLocalAddress()));

            unicastSocket = new DatagramSocket();

            executor.submit(this::listenForAnnouncements);
            executor.submit(this::periodicAnnounce);

            logger.info("UDP Discovery started on {}:{}", MULTICAST_ADDRESS, DISCOVERY_PORT);

        } catch (Exception e) {
            logger.error("Failed to start UDP Discovery", e);
            stop();
        }
    }

    public void stop() {
        running.set(false);

        try {
            if (multicastSocket != null && !multicastSocket.isClosed()) {
                multicastSocket.leaveGroup(new InetSocketAddress(multicastGroup, DISCOVERY_PORT),
                        NetworkInterface.getByInetAddress(getLocalAddress()));
                multicastSocket.close();
            }
        } catch (Exception e) {
            logger.warn("Error leaving multicast group", e);
        }

        if (unicastSocket != null && !unicastSocket.isClosed()) {
            unicastSocket.close();
        }

        if (executor != null) {
            executor.shutdownNow();
        }

        logger.info("UDP Discovery stopped");
    }

    public void announce() {
        try {
            Map<String, Object> message = createAnnouncementMessage(true);
            sendMulticast(message);
            logger.debug("Sent announcement");
        } catch (Exception e) {
            logger.error("Failed to send announcement", e);
        }
    }

    private void periodicAnnounce() {
        while (running.get()) {
            try {
                announce();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in periodic announce", e);
            }
        }
    }

    private void listenForAnnouncements() {
        byte[] buffer = new byte[4096];

        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                String senderIp = packet.getAddress().getHostAddress();

                handleReceivedMessage(json, senderIp);

            } catch (SocketException e) {
                if (running.get()) {
                    logger.error("Socket error while listening", e);
                }
            } catch (Exception e) {
                if (running.get()) {
                    logger.error("Error processing UDP message", e);
                }
            }
        }
    }

    private void handleReceivedMessage(String json, String senderIp) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = gson.fromJson(json, Map.class);

            String fingerprint = (String) message.get("fingerprint");

            if (deviceId.equals(fingerprint)) {
                return;
            }

            String alias = (String) message.get("alias");
            String deviceModel = (String) message.get("deviceModel");
            String deviceType = (String) message.get("deviceType");
            Double portDouble = (Double) message.get("port");
            int port = portDouble != null ? portDouble.intValue() : 8765;
            Boolean isAnnounce = (Boolean) message.get("announce");

            logger.info("Discovered device: {} ({}) at {}:{}", alias, deviceType, senderIp, port);

            if (listener != null) {
                listener.onDeviceDiscovered(alias, deviceModel, deviceType, fingerprint, senderIp, port);
            }

            if (Boolean.TRUE.equals(isAnnounce)) {
                sendResponseTo(senderIp, port);
            }

        } catch (Exception e) {
            logger.warn("Failed to parse discovery message: {}", json, e);
        }
    }

    private void sendResponseTo(String targetIp, int targetPort) {
        try {
            Map<String, Object> response = createAnnouncementMessage(false);
            String json = gson.toJson(response);
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            InetAddress targetAddress = InetAddress.getByName(targetIp);
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, DISCOVERY_PORT);
            unicastSocket.send(packet);

            logger.debug("Sent response to {}:{}", targetIp, targetPort);

        } catch (Exception e) {
            logger.error("Failed to send response to {}", targetIp, e);
        }
    }

    private void sendMulticast(Map<String, Object> message) throws Exception {
        String json = gson.toJson(message);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        DatagramPacket packet = new DatagramPacket(data, data.length, multicastGroup, DISCOVERY_PORT);
        multicastSocket.send(packet);
    }

    private Map<String, Object> createAnnouncementMessage(boolean isAnnounce) {
        Map<String, Object> message = new HashMap<>();
        message.put("alias", deviceName);
        message.put("version", "1.0");
        message.put("deviceModel", System.getProperty("os.name"));
        message.put("deviceType", "desktop");
        message.put("fingerprint", deviceId);
        message.put("port", serverPort);
        message.put("protocol", "ws");
        message.put("announce", isAnnounce);
        return message;
    }

    private InetAddress getLocalAddress() throws SocketException, UnknownHostException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress();
        } catch (Exception e) {
            return InetAddress.getLocalHost();
        }
    }

    public String getLocalIpAddress() {
        try {
            return getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
