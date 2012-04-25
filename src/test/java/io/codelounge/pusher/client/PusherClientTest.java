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

import io.codelounge.pusher.api.ChannelListener;
import io.codelounge.pusher.api.PusherListener;
import io.codelounge.pusher.client.PusherClient.Channel;
import org.json.JSONObject;

public class PusherClientTest {
	private static final String PUSHER_API_KEY = "80bbbe17a2e65338705a";
	private static final String PUSHER_CHANNEL = "test-channel";
	private static PusherClient pusherClient;

	public static void main(String[] args) {
		PusherListener eventListener = new PusherListener() {
			Channel channel;

			@Override
			public void onConnect(String socketId) {
				System.out.println("PusherClient connected. Socket Id is: " + socketId);
				channel = pusherClient.subscribe(PUSHER_CHANNEL);
				System.out.println("Subscribed to channel: " + channel);
				channel.send("client-event-test", "test");

				channel.bind("price-updated", new ChannelListener() {
					@Override
					public void onMessage(String message) {
						System.out.println("Received bound channel message: " + message);
					}
				});
			}

			@Override
			public void onMessage(String message) {
				System.out.println("Received message from PusherClient: " + message);
			}

			@Override
			public void onDisconnect() {
				System.out.println("PusherClient disconnected.");
			}
		};

		pusherClient = new PusherClient(PUSHER_API_KEY);
		pusherClient.setPusherListener(eventListener);
		pusherClient.connect();
	}
}
