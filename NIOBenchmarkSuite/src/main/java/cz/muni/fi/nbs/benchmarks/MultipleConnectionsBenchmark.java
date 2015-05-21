/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.benchmarks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import cz.muni.fi.nbs.net.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import cz.muni.fi.nbs.utils.Helpers;

/**
 *
 * @author petom_000
 */
@State(Scope.Benchmark)
public class MultipleConnectionsBenchmark {
    
    private int defaultPort = 3040;
    private static Process client;
    private static SocketChannel clientConnection;
    private static int connectionsCount = 250;
    
    @Setup(Level.Iteration)
    public static void pause() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ex) {
            Logger.getLogger(MultipleConnectionsBenchmark.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    @Setup
    public static void startClient() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "ClientTest.jar");
        client = pb.start();
        
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ex) {
            Logger.getLogger(MultipleConnectionsBenchmark.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        clientConnection = SocketChannel.open(new InetSocketAddress("localhost", 3000));
    }
    
    @TearDown
    public static void stopClient() throws IOException {
        clientConnection.write(ByteBuffer.wrap("close".getBytes()));
        clientConnection.close();
    }
    
    @Benchmark
    public void measureIOServer() throws IOException{
       requestData(); 
       IOServer server = new IOServer(defaultPort);
       server.startServer();
    }
    
    @Benchmark
    public void measureNIOBlockingServer() throws IOException{
       requestData();
       NIOBlockingServer server = new NIOBlockingServer(defaultPort);
       server.startServer();
    }
    
    @Benchmark
    public void measureNIOServer() throws IOException{
       requestData();
       NIOServer server = new NIOServer(defaultPort);
       server.startServer();
    }
    
    @Benchmark
    public void measureAsynchServer() throws IOException, InterruptedException{
       requestData();
       AsynchServer server = new AsynchServer(defaultPort);
       server.startServer();
    }
    
    
    public static void requestData() throws IOException {
        clientConnection.write(ByteBuffer.wrap(("multiple:"+Integer.toString(connectionsCount)).getBytes()));
    }
    
    public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(MultipleConnectionsBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(10)
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .build();
        
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
       
    }
}
