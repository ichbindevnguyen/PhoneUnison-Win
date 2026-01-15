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

import java.util.Map;
import java.util.UUID;

public class Message {

    private int version = 1;
    private String type;
    private String id;
    private long timestamp;
    private Map<String, Object> data;

    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String type) {
        this();
        this.type = type;
    }

    public Message(String type, Map<String, Object> data) {
        this(type);
        this.data = data;
    }

    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String PAIRING_REQUEST = "PAIRING_REQUEST";
    public static final String PAIRING_RESPONSE = "PAIRING_RESPONSE";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String NOTIFICATION_ACTION = "NOTIFICATION_ACTION";
    public static final String SMS_LIST = "SMS_LIST";
    public static final String SMS_MESSAGES = "SMS_MESSAGES";
    public static final String SMS_SEND = "SMS_SEND";
    public static final String SMS_RECEIVED = "SMS_RECEIVED";
    public static final String CALL_STATE = "CALL_STATE";
    public static final String CALL_ACTION = "CALL_ACTION";
    public static final String CALL_DIAL = "CALL_DIAL";
    public static final String SIM_LIST = "SIM_LIST";
    public static final String SIM_LIST_REQUEST = "SIM_LIST_REQUEST";
    public static final String CLIPBOARD = "CLIPBOARD";
    public static final String FILE_OFFER = "FILE_OFFER";
    public static final String FILE_ACCEPT = "FILE_ACCEPT";
    public static final String FILE_CHUNK = "FILE_CHUNK";
    public static final String FILE_COMPLETE = "FILE_COMPLETE";
    public static final String ERROR = "ERROR";

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDataField(String key) {
        if (data == null)
            return null;
        return (T) data.get(key);
    }

    @Override
    public String toString() {
        return "Message{type='" + type + "', id='" + id + "'}";
    }
}
