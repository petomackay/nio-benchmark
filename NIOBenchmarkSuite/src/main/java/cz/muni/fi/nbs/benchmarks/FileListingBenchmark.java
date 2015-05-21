/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
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
public class FileListingBenchmark {
    
    @State(Scope.Benchmark)
    public static class ListingState {
        @Setup
        public static void generateFiles() throws IOException, InterruptedException{
            FileUtils.createTemp();
            Path tmpPath = Paths.get("tmp");
            FileUtils.createBigDir(tmpPath, 5, 1, "small");
            FileUtils.createBigDir(tmpPath, 1000, 200, "big");
        }
    }
    
    @Benchmark
    public int measureIOFileListingBig(ListingState ls) {
        int counter = 0;
        File dir = new File("tmp/big");
        String[] everythingInThisDir = dir.list();
        for (String name : everythingInThisDir) {
            counter++;
        }
        return counter;
    }
    
    @Benchmark
    public int measureIOFileListingSmall(ListingState ls) {
        int counter = 0;
        File dir = new File("tmp/small");
        String[] everythingInThisDir = dir.list();
        for (String name : everythingInThisDir) {
            counter++;
        }
        return counter;
    }
    
    @Benchmark
    public int measureNIOFileListingBig(ListingState ls) throws IOException {
        int counter = 0;
        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("tmp/big"));
        Iterator<Path> iter = stream.iterator();
        while (iter.hasNext()) {
            Path path = iter.next();
            counter++;
        }
        stream.close();
        return counter;
    }
    
    @Benchmark
    public int measureNIOFileListingSmall(ListingState ls) throws IOException {
        int counter = 0;
        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("tmp/small"));
        Iterator<Path> iter = stream.iterator();
        while (iter.hasNext()) {
            Path path = iter.next();
            counter++;
        }
        stream.close();
        return counter;
    }
    
     public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(FileListingBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(20)
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .build();
        
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
    }
    
}
