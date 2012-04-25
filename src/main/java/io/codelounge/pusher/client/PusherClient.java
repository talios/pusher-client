package io.codelounge.pusher.client;

/*
 *  Copyright (C) 2012 Justin Schultz
 *  JavaPusherClient, a PusherClient (http://pusherapp.com) client for Java
 *
 *  http://justinschultz.com/
 *  http://publicstaticdroidmain.com/
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;
import io.codelounge.pusher.api.ChannelListener;
import io.codelounge.pusher.api.Pusher;
import io.codelounge.pusher.api.PusherListener;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class PusherClient<T> implements Pusher<T> {
    private static final String PUSHER_CLIENT = "java-android-client";
    private final String VERSION = "1.11";
    private final String HOST = "ws.pusherapp.com";
    private final int WS_PORT = 80;
    private final String PREFIX = "ws://";

    private WebSocket webSocket;
    private String apiKey;
    private final HashMap<String, Channel> channels;

    private PusherListener pusherEventListener;

    public PusherClient(String key) {
        apiKey = key;
        channels = new HashMap<String, Channel>();
    }

    public void connect() {
        String path = "/app/" + apiKey + "?client=" + PUSHER_CLIENT + "&version=" + VERSION;

        try {
            URI url = new URI(PREFIX + HOST + ":" + WS_PORT + path);
            AsyncHttpClient c = new AsyncHttpClient();

            webSocket = c.prepareGet(url.toASCIIString())
                    .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                            new MyWebSocketByteListener()).build()).get();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            webSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return webSocket.isOpen();
    }

    public void setPusherListener(PusherListener listener) {
        pusherEventListener = listener;
    }

    public Channel<T> subscribe(String channelName) {
        Channel c = new Channel(channelName);

        if (webSocket != null && webSocket.isOpen()) {
            try {
                sendSubscribeMessage(c);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        channels.put(channelName, c);
        return c;
    }

    public Channel subscribe(String channelName, String authToken) {
        Channel c = new Channel(channelName);

        if (webSocket != null && webSocket.isOpen()) {
            try {
                sendSubscribeMessage(c, authToken);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        channels.put(channelName, c);
        return c;
    }

    public Channel subscribe(String channelName, String authToken, int userId) {
        Channel c = new Channel(channelName);

        if (webSocket != null && webSocket.isOpen()) {
            try {
                sendSubscribeMessage(c, authToken, userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        channels.put(channelName, c);
        return c;
    }

    public void unsubscribe(String channelName) {
        if (channels.containsKey(channelName)) {
            if (webSocket != null && webSocket.isOpen()) {
                try {
                    sendUnsubscribeMessage(channels.get(channelName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            channels.remove(channelName);
        }
    }

    private void sendSubscribeMessage(Channel c) {

        try {
            c.send("pusher:subscribe", new HashMap());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendSubscribeMessage(Channel c, String authToken) {
        Map data = new HashMap();
        data.put("auth", authToken);

        c.send("pusher:subscribe", data);
    }

    private void sendSubscribeMessage(Channel c, String authToken, int userId) {
        Map message = new HashMap();
        try {
            message.put("auth", authToken);
            Map data = new HashMap();
            data.put("user_id", userId);
            message.put("channel_data", data);
        } catch (Exception ex) {

        }

        c.send("pusher:subscribe", message);
    }

    private void sendUnsubscribeMessage(Channel c) {
        c.send("pusher:unsubscribe", new HashMap());
    }

    private void dispatchChannelEvent(byte[] message, String event) {
        try {

            Map jsonMessage = new ObjectMapper().readValue(message, Map.class);

            String channelName = (String) jsonMessage.get("channel");

            Channel channel = channels.get(channelName);
            if (channel != null) {
                ChannelListener channelListener = channel.channelEvents.get(event);

                if (channelListener != null)
                    channelListener.onMessage(jsonMessage.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String event_name, Object data) {
        Map message = new HashMap();

        try {
            message.put("event", event_name);
            message.put("data", data);
            webSocket.sendMessage(new ObjectMapper().writeValueAsBytes(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class Channel {
        private String channelName;
        private final HashMap<String, ChannelListener> channelEvents;

        public Channel(String _name) {
            channelName = _name;
            channelEvents = new HashMap<String, ChannelListener>();
        }

        public void send(String eventName, T data) {
            Map message = new HashMap();

            try {
                // TODO WTF???
                message.put("channel", channelName);
                message.put("event", eventName);
                message.put("data", data);
                webSocket.sendMessage(new ObjectMapper().writeValueAsBytes(message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void bind(String eventName, ChannelListener channelListener) {
            channelEvents.put(eventName, channelListener);
        }

        @Override
        public String toString() {
            return channelName;
        }
    }

    private class MyWebSocketByteListener implements WebSocketByteListener {

        @Override
        public void onOpen(WebSocket websocket) {
        }

        @Override
        public void onClose(WebSocket websocket) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onMessage(byte[] message) {
            try {
                Map jsonMessage = new ObjectMapper().readValue(message, Map.class);
                String event = (String) jsonMessage.get("event");

                if (event.equals("pusher:connection_established")) {
                    Map data = (Map) jsonMessage.get("data");
                    pusherEventListener.onConnect((String) data.get("socket_id"));
                } else {
                    pusherEventListener.onMessage(new String(message));
                    dispatchChannelEvent(message, event);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFragment(byte[] fragment, boolean last) {
        }
    }
}
