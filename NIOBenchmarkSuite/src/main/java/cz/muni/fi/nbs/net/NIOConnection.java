/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petom_000
 */
public class NIOConnection implements Runnable{
    NIOBlockingServer server;
    SocketChannel channel;
    byte[] bytes = new byte[65536];
    ByteBuffer buffer = ByteBuffer.allocate(65536);
    
    public NIOConnection(SocketChannel channel, NIOBlockingServer server) {
        this.server = server;
        this.channel = channel;
    }

    @Override
    public void run() {
        int read = 0;
        String msg;
        
        try(SocketChannel sc = channel) {
             
            while (true) {
                read = sc.read(buffer);
                if (read <= 0) {
                    break;
                }
                
                buffer.flip();
                buffer.get(bytes, 0, Math.min(buffer.remaining(), 65536));
                msg = new String(bytes).trim();
                
                if (msg.contains("exit_connection")) {
                    break;
                }
                if (msg.contains("stop_server")) {
                    server.stop();
                    break;
                }
                Arrays.fill(bytes, (byte) 0);
                buffer.clear();
            }
        } catch (IOException ex) {
            Logger.getLogger(NIOConnection.class.getName()).log(Level.SEVERE, null, ex);
        } 
               
    }
    
    
    
}
