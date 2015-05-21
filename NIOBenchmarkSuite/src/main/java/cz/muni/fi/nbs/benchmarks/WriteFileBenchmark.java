/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.benchmarks;

import cz.muni.fi.nbs.utils.Config;
import cz.muni.fi.nbs.utils.FileUtils;
import cz.muni.fi.nbs.utils.Helpers;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 *
 * @author petom_000
 */

@State(Scope.Benchmark)
public class WriteFileBenchmark {
    private static final int SEED = 111;
    
    @Param({"10485760"})
    private static int fileSize;
    
    private static String fileName = "tmp/WriteTestFile";
    private static long checkSum = 0L;
    
    

    
    @State(Scope.Thread)
    public static class ThreadState {
        private static String threadFileName;
        private static int threadCheckSum;
        private static Random r = new Random(SEED);
        private static byte[] data = new byte[fileSize];
        
        public static byte[] generateData() {
            r.nextBytes(data);
            return data;
        }
        
        @Setup
        public void prepare(ThreadParams tp){
            threadFileName = fileName + '_' + tp.hashCode()+ new Random().nextInt(100);       
        }
        
        @TearDown
        public void cleanUp() throws InterruptedException, IOException {
            FileUtils.deleteFile(threadFileName);
        }
    }
    
    @State(Scope.Thread)
    public static class MappedThreadState {
        private static String threadFileName;
        private static int threadCheckSum;
        private static Random r = new Random(SEED);
        private static byte[] data = new byte[fileSize];
        
        public static byte[] generateData() {
            r.nextBytes(data);
            return data;
        }
        
        @Setup
        public void prepare(ThreadParams tp){
            threadFileName = fileName + '_' + tp.hashCode()+ new Random().nextInt(100);       
        }
        
    }
    
    @Benchmark
    public void measureFOS(ThreadState ts) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(ts.threadFileName);
        byte[] bufferArray = Arrays.copyOf(ts.generateData(), fileSize);
        fos.write(bufferArray);
        fos.close();
    }
    
    @Benchmark
    public void measureBOS(ThreadState ts) throws FileNotFoundException, IOException {
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(fileName));
        byte[] bufferArray = Arrays.copyOf(ts.generateData(), fileSize);
        bos.write(bufferArray);
        bos.close();
    }
        
    @Benchmark
    public void measureWrappedFC(ThreadState ts) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(ts.threadFileName);
        FileChannel ch = fos.getChannel();
        byte[] bufferArray = Arrays.copyOf(ts.generateData(), fileSize);
        ByteBuffer bb = ByteBuffer.wrap(bufferArray);
        ch.write(bb);
        ch.close();
    }
    
    @Benchmark
    public void measureDirectFC(ThreadState ts) throws FileNotFoundException, IOException {
        FileOutputStream fos  = new FileOutputStream(ts.threadFileName);
        FileChannel ch = fos.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(fileSize);
        bb.put(ts.generateData());
        bb.flip();
        ch.write(bb);
        ch.close();
    }
    
    @Benchmark
    public void measureRAF(ThreadState ts) throws FileNotFoundException, IOException {
        RandomAccessFile raf = new RandomAccessFile(ts.threadFileName, "rw");
        byte[] bufferArray = Arrays.copyOf(ts.generateData(), fileSize);
        raf.write(bufferArray);
        raf.close();
    }
    
    @Benchmark
    public void measureFC(ThreadState ts) throws FileNotFoundException, IOException {
        FileOutputStream fos  = new FileOutputStream(ts.threadFileName);
        FileChannel ch = fos.getChannel();
        ByteBuffer bb = ByteBuffer.allocate(fileSize);
        bb.put(ts.generateData());
        bb.flip();
        ch.write(bb);
        ch.close();
    }
    
    @Benchmark
    public void measureMappedFC(MappedThreadState ts) throws FileNotFoundException, IOException {
        RandomAccessFile raf = new RandomAccessFile(ts.threadFileName, "rw");
        FileChannel ch = raf.getChannel();
        MappedByteBuffer mb = ch.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
        
        mb.put(ts.generateData());
        ch.close();
    }
    
    public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
                
        Options opt = new OptionsBuilder()
                .include(WriteFileBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(20)
                .param("fileSize", Integer.toString(Config.SMALL_FILE_SIZE), Integer.toString(Config.MEDIUM_FILE_SIZE),
                        Integer.toString(Config.BIG_FILE_SIZE), Integer.toString(Config.LARGE_FILE_SIZE))
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .build();
        
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
    }
}
