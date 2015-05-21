/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petom_000
 */
public class IOConnection implements Runnable{
    IOServer server;
    Socket socket;
    byte[] bytes = new byte[65536];
    
    public IOConnection(Socket socket, IOServer server) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        int read = 0;
        String msg;
        
        try(Socket s = socket) {
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream()); 
            while (true) {
                read = bis.read(bytes, 0, 65536);
                msg = new String(bytes).trim();
                
                if (msg.contains("exit_connection") || read == -1) {
                    break;
                }
                if (msg.length() >= 11 && msg.substring(msg.length()-11).equals("stop_server")) {
                    server.stop();
                    break;
                }    
                Arrays.fill(bytes, (byte) 0);
                
            }
        } catch (IOException ex) {
            Logger.getLogger(IOConnection.class.getName()).log(Level.SEVERE, null, ex);
        } 
               
    }
    
    
    
}
