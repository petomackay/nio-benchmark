/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs;

import cz.muni.fi.nbs.benchmarks.*;
import cz.muni.fi.nbs.utils.CleanUpHook;
import cz.muni.fi.nbs.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.openjdk.jmh.runner.RunnerException;

/**
 *
 * @author petom_000
 */
public class Main {
    
    private static String[] benchmarksFull = {
            "ReadFile",
            "WriteFile",
            "ReadMultiple",
            "PartialRead",
            "SingleConnection",
            "MultipleConnections",
            "FileTree",
            "FileListing"
        };
    
    //TODO Exceptions
    public static void main(String[] args) throws RunnerException, IOException {

        String[] benchmarksChosen;
        String resultsDir;
        
        if (args.length == 0) {
            benchmarksChosen = benchmarksFull;
        } else if (args.length == 1 && args[0].equals("help")) {
            printHelp();
            return;
        } else if (validateArgsFormat(args)) {
            benchmarksChosen = args;
        } else {
            System.out.println("Incorrect usage. For help run command with \"help\" argument.");
            return;
        }

        Runtime.getRuntime().addShutdownHook(new CleanUpHook());
        
        resultsDir = FileUtils.createResultsDir();
        
        for (String benchmark: benchmarksChosen) {
            FileUtils.createTemp();
            switch (benchmark) {
                    case "ReadFile":
                        ReadFileBenchmark.runBenchmarks(resultsDir);
                        break;
                    case "WriteFile": 
                        WriteFileBenchmark.runBenchmarks(resultsDir);
                        break;
                    case "ReadMultiple": 
                        ReadMultipleBenchmark.runBenchmarks(resultsDir);
                        break;
                    case "PartialRead": 
                        PartialReadBenchmark.runBenchmarks(resultsDir);
                        break;    
                    case "SingleConnection":
                        SingleConnectionBenchmark.runBenchmarks(resultsDir);
                        break;
                    case "MultipleConnections": 
                        MultipleConnectionsBenchmark.runBenchmarks(resultsDir);
                        break;
                    case "FileTree":
                        FileTreeBenchmark.runBenchmarks(resultsDir);
                        break;
                    case "FileListing": 
                        FileListingBenchmark.runBenchmarks(resultsDir);
                        break;
                    default: break;
            }
            System.out.println();
        }

        System.out.println("Run finished. The results can be found in " + 
                Paths.get(System.getProperty("user.dir"), resultsDir));
      
    }
    
    private static boolean validateArgsFormat(String[] args) {
        List<String> benchmarksList = Arrays.asList(benchmarksFull);
        for (String arg : args) {
            if (!benchmarksList.contains(arg)) return false;
        }
        return true;
    }

    private static void printHelp() {
        System.out.println("Run without any arguments to run the whole suit. Alternatively, you can select which benchmarks to run by passing their names as arguments.");
        System.out.println("The valid benchmark names are:");
        for (String benchmark: benchmarksFull) {
            System.out.println(benchmark);
        }
    }
}
