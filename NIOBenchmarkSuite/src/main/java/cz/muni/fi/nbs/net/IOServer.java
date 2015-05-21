/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petom_000
 */
public class IOServer {
    
    private int port;
    String hostName = "localhost";
    private boolean cont = true;
    private ServerSocket serverSocket;
  
    
    public IOServer(int port) {
        this.port = port;
    }
    
    public void startServer() throws IOException {
        try (ServerSocket ss = new ServerSocket()) {
            serverSocket = ss;
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(hostName, port));
            
            while (cont) {
                try {
                    Socket s = ss.accept();
                    new Thread(new IOConnection(s, this)).start();
                } catch (java.net.SocketException ste) {
                    Logger.getLogger(IOServer.class.getName()).log(Level.FINE, null, ste);
                }
            }
        }
    }
    
    public void stop() throws IOException {
        cont = false;
        serverSocket.close();
    }
      
}
