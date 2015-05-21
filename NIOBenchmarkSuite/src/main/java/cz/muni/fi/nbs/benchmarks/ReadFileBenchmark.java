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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;;
import java.util.Collection;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;
import cz.muni.fi.nbs.utils.*;

/**
 *
 * @author petom_000
 */
@State(Scope.Benchmark)
public class ReadFileBenchmark {
    
    //@Param({"65536"})
    private int bufferSize = 65536;
    
    private static String fileName = "tmp/largeTestFile";
    
    @Param({Config.BIG_FILE_SIZE_STRING})
    private static int fileSize;
    
    private static int barraySize;
    private static int channelBufferSize;
    private static long check = -1;
    private static boolean checkResult = true;
    
    @Setup
    public synchronized void prepare() throws IOException, FileNotFoundException, InterruptedException {
        FileUtils.createFile(fileName,fileSize);
        barraySize = bufferSize;
        channelBufferSize = bufferSize;
    }
    
    @TearDown
    public synchronized void cleanUp() throws IOException, InterruptedException {
        FileUtils.deleteFile(fileName);
    }
    
  
    @Benchmark
    public long measureBufferedFIS() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileName);
        byte[] barray = new byte[barraySize];
        long checkSum = 0L;
        int nRead;
        while ((nRead=fis.read(barray, 0, barraySize)) != -1 ) {
            for (int i=0; i<nRead; i++) {
                checkSum += barray[i];
            }
        }
        performCheck(checkSum);
        fis.close();
        return checkSum;
    }
        
    @Benchmark
    public long measureBufferedBIS() throws FileNotFoundException, IOException {
        BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(fileName));
        byte[] barray = new byte[barraySize];
        long checkSum = 0L;
        int nRead;
        while ((nRead=bis.read(barray, 0, barraySize)) != -1 ) {
            for (int i=0; i<nRead; i++) {
                checkSum += barray[i];
            }
        }
        performCheck(checkSum);
        bis.close();
        return checkSum;
    }
    
    @Benchmark
    public long measureBufferedRAF() throws FileNotFoundException, IOException {
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
        byte[] barray = new byte[barraySize];
        long checkSum = 0L;
        int nRead;
        while ((nRead=raf.read(barray, 0, barraySize)) != -1 ) {
            for (int i=0; i<nRead; i++) {
                checkSum += barray[i];
            }
        }
        performCheck(checkSum);
        raf.close();
        return checkSum;
    }

    
    @Benchmark
    public long measureFC() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileName);
        FileChannel ch = fis.getChannel();
        ByteBuffer bb = ByteBuffer.allocate(channelBufferSize);
        long checkSum = 0L;
        int nRead;
        while ((nRead=ch.read(bb)) != -1 ) {
            if (nRead == 0)
                continue;
            bb.flip();
            while (bb.hasRemaining()) {
                checkSum += bb.get();
            }
            bb.clear();
        }
        performCheck(checkSum);
        ch.close();
        return checkSum;
    }
    
       
    @Benchmark
    public long measureWrappedFC() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileName);
        FileChannel ch = fis.getChannel();
        byte[] barray = new byte[barraySize];
        ByteBuffer bb = ByteBuffer.wrap(barray);
        long checkSum = 0L;
        int nRead;
        while ((nRead=ch.read(bb)) != -1 ) {
            for (int i=0; i<nRead; i++) {
                checkSum += barray[i];
            } 
            bb.clear();
        }
        performCheck(checkSum);
        ch.close();
        return checkSum; 
    }
    
    
    @Benchmark
    public long measureDirectFC() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileName);
        FileChannel ch = fis.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(channelBufferSize);
        long checkSum = 0L;
        int nRead;
        while ((nRead=ch.read(bb)) != -1 ) {
            if (nRead == 0)
                continue;
            bb.flip();
            while (bb.hasRemaining()) {
                checkSum += bb.get();
            }
            bb.clear();
        }
        ch.close();
        performCheck(checkSum);
        return checkSum;
    }
    
    
    @Benchmark
    public long measureMappedFC() throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(fileName);
        FileChannel ch = fis.getChannel();
        MappedByteBuffer mb = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size());
        long checkSum = 0L;
        while (mb.hasRemaining()) {
            checkSum += mb.get();
        }
        performCheck(checkSum);
        ch.close();
        return checkSum;
    }
    
     
    private static void performCheck(long checkSum) {
         if (check == -1) {
            check = checkSum;
        } else {
            if (check != checkSum) checkResult = false;
        }
    }
    
    public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(ReadFileBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(20)
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .param("fileSize", Integer.toString(Config.SMALL_FILE_SIZE), Integer.toString(Config.MEDIUM_FILE_SIZE),
                        Integer.toString(Config.BIG_FILE_SIZE))
                .build();
        
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
       
    }
}
