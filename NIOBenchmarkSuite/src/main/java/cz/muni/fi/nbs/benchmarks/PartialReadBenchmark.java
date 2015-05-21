/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.benchmarks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import cz.muni.fi.nbs.utils.FileUtils;
import cz.muni.fi.nbs.utils.Helpers;

/**
 *
 * @author petom_000
 */
@State(Scope.Benchmark)
public class PartialReadBenchmark {
   
    //@Param({"65536"})
    private int bufferSize = 65536;
    
    private static String fileName = "tmp/partialTestFile";
    private static Path fileNamePath = Paths.get(fileName);
    
    //@Param({"10485760"})
    private static int fileSize = 10485760;
    
    private static int middleIndex, endIndex;
    
    private int barraySize;
    private int channelBufferSize;
    private static long check = -1;
    private static boolean checkResult = true;
    
    @Setup
    public synchronized void prepare() throws IOException, FileNotFoundException, InterruptedException {
        FileUtils.createFile(fileName,fileSize);
        barraySize = bufferSize;
        channelBufferSize = bufferSize;
        middleIndex = fileSize/2 - 65536;
        endIndex = fileSize - 65536;
    }
    
    @TearDown
    public synchronized void cleanUp() throws IOException, InterruptedException {
        FileUtils.deleteFile(fileName);
    }
    
    
    @Benchmark
    public long measureAsynchronousFC() throws IOException, InterruptedException {
        long checkSum = 0L;
        CountDownLatch latch = new CountDownLatch(3);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(fileNamePath,
                StandardOpenOption.READ);
       
        ByteBuffer start = ByteBuffer.allocateDirect(channelBufferSize);
        ByteBuffer middle = ByteBuffer.allocateDirect(channelBufferSize);
        ByteBuffer end = ByteBuffer.allocateDirect(channelBufferSize);
        channel.read(start, 0, latch, new ReadHandler());
        channel.read(middle, middleIndex, latch, new ReadHandler());
        channel.read(end, endIndex, latch, new ReadHandler());
        
        latch.await();
        channel.close();
        
        checkSum = readBuffer(start) + readBuffer(middle) + readBuffer(end);
        performCheck(checkSum);
        
        return checkSum;
    }
    
    @Benchmark
    public long measureMultithreadedRAF() throws InterruptedException {
        long checkSum = 0L;
        byte[] start = new byte[barraySize];
        byte[] middle = new byte[barraySize];
        byte[] end = new byte[barraySize];
        
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(new ReadOperation(0, start, fileName));
        executor.execute(new ReadOperation(middleIndex, middle, fileName));
        executor.execute(new ReadOperation(endIndex, end, fileName));
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        checkSum = readBuffer(start) + readBuffer(middle) + readBuffer(end);
        performCheck(checkSum);
        
        return checkSum;
    }
    
    
    class ReadHandler implements CompletionHandler {

        @Override
        public void completed(Object result, Object attachment) {
            CountDownLatch latch = (CountDownLatch) attachment;
            latch.countDown();
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            //
        }
    }
    
    class ReadOperation implements Runnable {

        public ReadOperation(int index, byte[] buffer, String fileName) {
            this.index = index;
            this.buffer = buffer;
            this.fileName = fileName;
        }

        int index;
        byte[] buffer;
        String fileName;
        
        @Override
        public void run() {
            try (RandomAccessFile raf = new RandomAccessFile(fileName, "r")) {
                raf.seek(index);
                raf.readFully(buffer);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PartialReadBenchmark.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PartialReadBenchmark.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    private static void performCheck(long checkSum) {
         if (check == -1) {
            check = checkSum;
        } else {
            if (check != checkSum) checkResult = false;
        }
    }
    
    private static long readBuffer(ByteBuffer buffer) {
        long checkSum = 0L;
        buffer.flip();
        while (buffer.hasRemaining()){
            checkSum += buffer.get();
        }
        return checkSum;
                
    }
    
    private static long readBuffer(byte[] buffer) {
        long checkSum = 0L;
        for(byte b:buffer) {
            checkSum += b;
        }
        return checkSum;
                
    }
    
    public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(PartialReadBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(20)
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .build();
        
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
       
    }
    
}
