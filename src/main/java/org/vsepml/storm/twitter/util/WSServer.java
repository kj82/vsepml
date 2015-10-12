/**
 * Copyright 2015 Nicolas Ferry <${email}>
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
package org.vsepml.storm.twitter.util;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.vsepml.storm.twitter.ReflexiveContainerBolt;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 07/10/2015.
 */
public class WSServer extends WebSocketServer {
    private static final Logger journal = Logger.getLogger(WSServer.class.getName());
    private final ReflexiveContainerBolt rb;

    public WSServer(int port, ReflexiveContainerBolt rb){
        super(
                new InetSocketAddress(port),
                Collections.singletonList((Draft) new Draft_17())
        );
        this.rb=rb;
    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        journal.log(Level.INFO,"A component is connected from: "+webSocket.getRemoteSocketAddress());
        webSocket.send(String.format("!connected\n  yourID : %s\n", webSocket.getRemoteSocketAddress()));
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        journal.log(Level.INFO, webSocket.getRemoteSocketAddress()+" left.");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        journal.log(Level.INFO, "said: "+s);
        rb.loadNewBoltBehavior(s); //TODO: check s
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }
}
