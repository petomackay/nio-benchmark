/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.benchmarks;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
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
public class FileTreeBenchmark {
    
    @State(Scope.Benchmark)
    public static class DeepState {
        @Setup
        public static void generateFiles() throws IOException, InterruptedException{
            FileUtils.createTemp();
            Path tmpPath = Paths.get("tmp");
            FileUtils.createDeepTree(tmpPath, 20, "testFileDeep");
        }
    }
    
    @State(Scope.Benchmark)
    public static class WideState {
        @Setup
        public static void generateFiles() throws IOException, InterruptedException{
            FileUtils.createTemp();
            Path tmpPath = Paths.get("tmp");
            FileUtils.createWideTree(tmpPath, 3, 10, "testFileWide", true);
            FileUtils.createTmpFile("tmp/testFileShallow", 0);
        }
    }
    
    @Benchmark
    public int walkFileTreeDeep(DeepState ds) throws IOException {
        FileFinder finder = new FileFinder("testFileDeep");
        Files.walkFileTree(FileSystems.getDefault().getPath("tmp"), finder);
        int visited = finder.getFilesVisited();
        return visited;
    }
    
    @Benchmark
    public int walkFileTreeShallow(WideState ws) throws IOException {
        FileFinder finder = new FileFinder("testFileShallow");
        Files.walkFileTree(FileSystems.getDefault().getPath("tmp"), finder);
        int visited = finder.getFilesVisited();
        return visited;
    }
    
    @Benchmark
    public int walkFileTreeWide(WideState ws) throws IOException {
        FileFinder finder = new FileFinder("testFileWide");
        Files.walkFileTree(FileSystems.getDefault().getPath("tmp"), finder);
        int visited = finder.getFilesVisited();
        return visited;
    }
    
    class FileFinder implements FileVisitor<Path> {
        
        private String fileName;
        private int filesVisited = 0;
        
        public FileFinder(String fileName) {
            this.fileName = fileName;
        }
        
        public int getFilesVisited() {
            return filesVisited;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {    
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = file.getFileName().toString();
            if (!name.equals(fileName)) {
                filesVisited++;
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.TERMINATE;
            }
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            filesVisited++;
            return FileVisitResult.CONTINUE;
        }
        
    }
    
     public static void runBenchmarks(String resultsDir) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(FileTreeBenchmark.class.getSimpleName())
                .warmupIterations(10)
                .measurementIterations(20)
                .forks(1)
                .jvmArgs("-server", "-XX:CompileThreshold=2", "-XX:+AggressiveOpts", "-XX:+UseFastAccessorMethods")
                .build();
        
        Collection<RunResult> runResults = new Runner(opt).run();
        Helpers.processResults(runResults, resultsDir);
       
    }
    
}
