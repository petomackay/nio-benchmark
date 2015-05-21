/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author petom_000
 */
public class NIOServer {
    
    private String hostName = "localhost";
    private int port;
    private ByteBuffer buffer = ByteBuffer.allocate(65536);
    private int bufferSize = 65536;
    byte[] bytes = new byte[bufferSize];
    
    public NIOServer(int port) {
        this.port = port;
    }
    
    public void startServer() throws IOException {
        // Create a new selector
        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(new InetSocketAddress(hostName, port));
        SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
        
        while (selector.isOpen()) {
            int num = selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();

            while (it.hasNext()) {
                key = it.next();
                if (key.isAcceptable()) {
                    // Accept the new connection
                    ssc = (ServerSocketChannel)key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    // Add the new connection to the selector
                    SelectionKey newKey = sc.register( selector, SelectionKey.OP_READ );
                    it.remove();
                } else if (key.isReadable()) {
                    // Read the data
                    SocketChannel sc = (SocketChannel)key.channel();
                    while (true) {
                        buffer.clear();
                        Arrays.fill(bytes, (byte) 0);
                        int read = sc.read(buffer);
                        if (read <= 0) break;
                        buffer.flip();
                        buffer.get(bytes, 0, Math.min(buffer.remaining(), bufferSize));
                        String msg = new String(bytes).trim();
                        
                        if (msg.contains("stop_server")) {
                            ssc.close();
                            Iterator<SelectionKey> iterator = selector.keys().iterator();
                            while (iterator.hasNext()) {
                                key = iterator.next();
                                SelectableChannel channel = key.channel();
                                if (channel instanceof SocketChannel) {
                                    channel.close();
                                }
                            }
                            selector.close();
                            break;
                        } 
                        if (msg.contains("exit_connection")) {
                            sc.close();
                            break;
                        }
                    }
                it.remove();
                }
            }
        }
    }
}
