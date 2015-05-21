/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.benchmarks;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import cz.muni.fi.nbs.utils.*;

/**
 *
 * @author petom_000
 */
@State(Scope.Benchmark)
public class ReadMultipleBenchmark {
    //@Param({"65536"})
    private int bufferSize = 65536;
    private static int fileCount = 100;
    private static String baseName = "tmp/ReadMultipleTestFile";
    private static Path[] filePaths;
    private String[] fileNames;
    
    
    @Param({Config.MEDIUM_FILE_SIZE_STRING})
    private static int fileSize;
    
    private static int barraySize;
    private static int channelBufferSize;
    private static long check = -1;
    private static boolean checkResult = true;
    
    @Setup
    public synchronized void prepare() throws IOException, FileNotFoundException, InterruptedException {
        fileNames = FileUtils.getNames(fileCount, baseName);       
        FileUtils.createManyFiles(fileNames, fileSize);
        
        filePaths = new Path[fileCount];
        for (int i=0; i<fileCount; i++) {
            filePaths[i] = Paths.get(fileNames[i]);
        }
        
        barraySize = bufferSize;
        channelBufferSize = bufferSize;
        
    }
    
    @TearDown
    public synchronized void cleanUp() throws IOException, InterruptedException {
        FileUtils.deleteManyFiles(fileNames);
    }
    
    
    @Benchmark
    public long measureBufferedBIS() throws FileNotFoundException, IOException {
        BufferedInputStream bis;
        byte[] barray = new byte[barraySize];
        long checkSum = 0L;
        int nRead;
        for (String fileName:fileNames) {
            bis = new BufferedInputStream(new FileInputStream(fileName));
            
            while ((nRead=bis.read(barray, 0, barraySize)) != -1 ) {
                for (int i=0; i<nRead; i++) {
                    checkSum += barray[i];
                }
            }
            performCheck(checkSum);
            bis.close();
        }
        return checkSum;
    }
    
    @Benchmark
    public void measureMultithreadedBufferedBIS() throws InterruptedException {
        ExecutorService executor =Executors.newCachedThreadPool();
        
        for (String fileName:fileNames) {
            executor.execute(new ReadOperation(fileName));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
    }
    
    
    @Benchmark
    public void measureAsynchronousFC() throws IOException, InterruptedException { 
        CountDownLatch latch = new CountDownLatch(fileCount);
        for (Path path: filePaths) {
            AsynchronousFileChannel ch = AsynchronousFileChannel.open(path, 
                    StandardOpenOption.READ);
            ByteBuffer bb = ByteBuffer.allocateDirect(channelBufferSize);
            AsynchReadOperation operation = new AsynchReadOperation(bb, ch, 0);
            operation.setLatch(latch);
            ch.read(bb, 0, operation, new AsynchHandler());
        }
        latch.await();
    }
    
    //TODO: exception handling
    class ReadOperation implements Runnable {

        String name;
        
        ReadOperation(String name) {
            this.name = name;
        }
        
        @Override
        public void run() {
            byte[] barray = new byte[barraySize];
            int nRead;
            long checkSum = 0L;
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(name))) {
                while ((nRead=bis.read(barray, 0, barraySize)) != -1 ) {
                    for (int i=0; i<nRead; i++) {
                        checkSum += barray[i];
                    }
                }
                performCheck(checkSum);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ReadMultipleBenchmark.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ReadMultipleBenchmark.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    class AsynchHandler implements CompletionHandler {
        
        
        @Override
         public void completed(Object result, Object attachment) {
            AsynchReadOperation operation = (AsynchReadOperation)attachment;
            ByteBuffer readBytes = operation.getBuffer();
            AsynchronousFileChannel channel = operation.getChannel();
            long checkSum = operation.getCheckSum();
            int offset = operation.getOffset();
            int nRead = (int) result;
            
            if (nRead == -1) {
                try {
                    channel.close();
                    throw new IllegalStateException("Error in completion handler for MultipleRead");
                } catch (IOException ex) {
                    Logger.getLogger(ReadMultipleBenchmark.class.getName()).log(Level.SEVERE, null, ex);
                }
                return;
            }
            offset += nRead;

            byte[] barray = new byte[barraySize];
            int nGet;
            readBytes.flip();

            while (readBytes.hasRemaining()) {
                nGet = Math.min(barraySize, readBytes.remaining());
                readBytes.get(barray, 0, nGet);
                for (int j=0; j<nGet; j++) {
                    checkSum += barray[j];
                }
            }
            if (offset == fileSize) {
                performCheck(checkSum);
                operation.countDown();
                try {
                    channel.close();
                } catch (IOException ex) {
                    Logger.getLogger(ReadMultipleBenchmark.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                readBytes.clear();
                operation.setCheckSum(checkSum);
                operation.setOffset(offset);
                channel.read(readBytes, offset, attachment, new AsynchHandler());
            }
         }
         @Override
         public void failed(Throwable e, Object attachment) {
             //
         }
    }
    
    public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(ReadMultipleBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(20)
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .param("fileSize", Integer.toString(Config.TINY_FILE_SIZE), Integer.toString(Config.MEDIUM_FILE_SIZE),
                        Integer.toString(Config.BIG_FILE_SIZE))
                .param("bs", Integer.toString(65536))
                .build();
       
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
       
    }
    
    
    
    private static void performCheck(long checkSum) {
         if (check == -1) {
            check = checkSum;
        } else {
            if (check != checkSum) checkResult = false;
        }
    }
}
