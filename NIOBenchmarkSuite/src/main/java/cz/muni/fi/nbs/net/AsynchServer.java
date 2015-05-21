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
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petom_000
 */
public class AsynchServer {
    
    private boolean stop = false;
    private int port;
    private String hostName = "localhost";
    int bufferSize = 65536;
    Object LOCK = new Object();
    
    public AsynchServer(int port) {
        this.port = port;
    }
    
    public void startServer() throws IOException, InterruptedException {
        final AsynchronousServerSocketChannel ssc = AsynchronousServerSocketChannel.open();
        ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ssc.bind(new InetSocketAddress(hostName, port));
        
       
        ssc.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
            @Override
            public void completed(AsynchronousSocketChannel sc, Void att) {
                ssc.accept(null, this);
                try {
                    receiveMessage(sc);
                } catch (IOException | InterruptedException | ExecutionException ex) {
                    Logger.getLogger(AsynchServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            @Override
            public void failed(Throwable exc, Void att) {
                //
            }
        });
        synchronized (LOCK) {
            try{LOCK.wait();}
            catch(InterruptedException ie) {
                //
            }
        }
        ssc.close();       
    }
    
    private void receiveMessage(AsynchronousSocketChannel sc) throws IOException, InterruptedException, ExecutionException{
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        byte[] bytes = new byte[bufferSize];
        // Read the data
        while (true) {  
            int read = sc.read(buffer).get();
            if (read <= 0) break;
            buffer.flip();
            buffer.get(bytes, 0, Math.min(buffer.remaining(), bufferSize));
            String msg = new String(bytes).trim();

            if (msg.contains("exit_connection")) {
                sc.close();
                break;
            }
            if (msg.contains("stop_server")) {
                sc.close();
                synchronized (LOCK) {
                    LOCK.notifyAll();
                }
                break;
            }
            Arrays.fill(bytes, (byte) 0);
            buffer.clear();
        }
    }
    
    public boolean isStopped() {
        return stop;
    }
    
}
