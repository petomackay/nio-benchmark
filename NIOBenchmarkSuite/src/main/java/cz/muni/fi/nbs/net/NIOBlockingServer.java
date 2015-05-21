/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petom_000
 */
public class NIOBlockingServer {
    
    private String hostName = "localhost";
    private int port;
    private boolean cont = true;
    private ServerSocketChannel serverChannel;
  
    
    public NIOBlockingServer(int port) {
        this.port = port;
    }
    
    public void startServer() throws IOException  {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            serverChannel = ssc;
            ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            ssc.bind(new InetSocketAddress(hostName, port));
            
            while (cont) {
                try {
                    SocketChannel sc = ssc.accept();
                    new Thread(new NIOConnection(sc, this)).start();
                } catch (java.nio.channels.AsynchronousCloseException ex) {
                    Logger.getLogger(NIOBlockingServer.class.getName()).log(Level.FINE, null, ex);
                }
            }
        } 
    }
    
    public void stop() throws IOException {
        cont = false;
        serverChannel.close();
    }
      
}
