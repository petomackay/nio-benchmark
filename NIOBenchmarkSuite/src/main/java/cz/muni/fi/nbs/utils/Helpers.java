/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.utils;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.util.ClassUtils;
import org.openjdk.jmh.util.Statistics;



/**
 *
 * @author Peter Mačkay
 */
public class Helpers {
    
    private static String resultsDir;
    
    public static void processResults(Collection<RunResult> runResults, String resultsDirectory) {
        
        resultsDir = resultsDirectory;
        String benchmarkName = null;
        Map<String, Collection<Result>> results = new HashMap<>();
        
        Iterator<RunResult> it = runResults.iterator();
        
        //process the runs
        while(it.hasNext()) {
            RunResult runResult = it.next();
            Collection<BenchmarkResult> benchmarkResults = runResult.getBenchmarkResults();
            
            //Extract the benchmark class name.
            Collection<String> benchNames = new ArrayList<>();
            benchNames.add(runResult.getParams().getBenchmark());
            Map<String, String> denseClassNames = ClassUtils.denseClassNames(benchNames);
            //TODO: Modify for multiple results!
            String denseClassName = denseClassNames.get(denseClassNames.keySet().iterator().next());
            String[] splitName = denseClassName.split("\\.");
            StringBuilder benchmarkClass = new StringBuilder(splitName[0]);
            benchmarkName = benchmarkClass.toString();
            
            Iterator<BenchmarkResult> brit = benchmarkResults.iterator();
            
            //process the results
            while(brit.hasNext()) {
                
                BenchmarkResult benchmarkResult = brit.next();
                
                // get params
                Iterator<String> iterator = benchmarkResult.getParams().getParamsKeys().iterator();
                while (iterator.hasNext()){
                    String param = iterator.next();
                    String suffix = "_"+param+"_" + benchmarkResult.getParams().getParam(param);
                    benchmarkClass.append(suffix);
                }
                
                if (!results.containsKey(benchmarkClass.toString())) results.put(benchmarkClass.toString(), new ArrayList<Result>());
                
                Result r = benchmarkResult.getPrimaryResult();
                results.get(benchmarkClass.toString()).add(r);
            }
        }
        
        //output the results
        exportToHTML(results, benchmarkName, true);
        exportToPNG(results);
    }
    
    private static void exportToPNG(Map<String, Collection<Result>> results) {
        
        for (Entry<String, Collection<Result>> entry: results.entrySet()) {
            double number = 0;
            String key = entry.getKey();
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            Iterator<Result> resultsIterator = results.get(key).iterator();
            while(resultsIterator.hasNext()) {
                Result r = resultsIterator.next();
                dataset.addValue(r.getScore(), r.getLabel(), "");
                number+=0.8;
            }
            double width = number>1 ? number*200 : 300;    
            String unit = entry.getValue().iterator().next().getScoreUnit();
            String[] splitKey = key.split("_");
            
            JFreeChart chart = ChartFactory.createBarChart3D(splitKey[0], null, unit, dataset);
            
            int len = splitKey.length/2;
            for (int i=0; i<len; i++){
                String subtitle = splitKey[i*2+1]+":"+splitKey[i*2+2];
                TextTitle title = new TextTitle(subtitle);
                Font oldFont = title.getFont();
                int fontSize = (int) Math.round(oldFont.getSize()*1.2);
                int fontStyle = oldFont.getStyle();
                String fontName = oldFont.getName();
                
                title.setFont(new Font(fontName, fontStyle, fontSize));
                chart.addSubtitle(title);
            }
            try {
                ChartUtilities.saveChartAsPNG(new File(resultsDir + "/charts/"+ key+"Chart.png"), chart, (int)Math.round(width), 800);
            } catch (IOException ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "Could not export chart to PNG file for " + key, ex);
            }
        }
    }
    
    private static void exportToHTML(Map<String, Collection<Result>> results, String benchmarkName, boolean includeChart) {
        File destination = new File(resultsDir + "/" + benchmarkName + ".html");
        
        try (BufferedWriter writer = new BufferedWriter(new PrintWriter(destination))) {
            
            // write the doctype and title tags
            writer.append("<!doctype html>");
            writer.newLine();
            writer.append("<title>" + benchmarkName + " Benchmark Results</title>");
            writer.newLine();

            // add padding to table cells
            writer.append("<style>");
            writer.newLine();
            writer.append("\ttd, th {padding:10px;}");
            writer.newLine();
            writer.append("\tp {padding:20px;}");
            writer.newLine();
            writer.append("</style>");
            writer.newLine();
            writer.newLine();

            writer.append("<h1>" + benchmarkName + " Benchmark Results</h1>");
            writer.newLine();
            
            for (Entry<String, Collection<Result>> entry: results.entrySet()) {
                
                String fullName = entry.getKey();
                Iterator<Result> resultsIterator = results.get(fullName).iterator();
                
                String[] fullNameParts = fullName.split("_");
                int numberOfParams = (fullNameParts.length -1)/2;
                
                 // write benchmark parameters with values if chart is not included
                if (!includeChart) {
                    for (int i=0; i<numberOfParams; i++) {
                        String param = fullNameParts[(i*2)+1];
                        String value = fullNameParts[(i*2)+2];
                        writer.append("<h3>" + param + ": " + value + "</h3>");
                        writer.newLine();
                    }
                }
                
                // add the chart of benchmark results
                if (includeChart) {
                    writer.append("<img src=\"charts/" + fullName + "Chart.png\"><br>");
                    writer.newLine();
                }
                
                // add test paramaters with values before table
                for (int i=0; i<numberOfParams; i++) {
                    String param = fullNameParts[(i*2)+1];
                    String value = fullNameParts[(i*2)+2];
                    writer.append("<strong>" + param + "</strong>: " + value + "<br>");
                     writer.newLine();
                }
                
                // add a table containing data
                writer.append("<table>");
                writer.newLine();
                writer.append("\t<tr><th>Name"
                        + "<th>Result"
                        + "<th>Min"
                        + "<th>Average"
                        + "<th>Max"
                        + "<th>Standard Deviation"
                        + "<th>Error<th>Confidence Interval (99.9%)");
                writer.newLine();
                
                while(resultsIterator.hasNext()) {
                    Result r = resultsIterator.next();
                    Statistics stats = r.getStatistics();
                    double[] confidenceInterval = stats.getConfidenceIntervalAt(0.999);
                    
                    writer.append("<tr>" + "<td>" + r.getLabel() +
                            "<td>" + String.format("%.3f",r.getScore()) +
                            "<td>" + String.format("%.3f",stats.getMin()) +
                            "<td>" + String.format("%.3f",stats.getMean()) +
                            "<td>" + String.format("%.3f",stats.getMax()) +
                            "<td>" + String.format("%.3f",stats.getStandardDeviation()) +
                            "<td>" + String.format("%.3f",r.getScoreError()) +
                            "<td>" + "[" + String.format("%.3f",confidenceInterval[0])  + ", " + String.format("%.3f",confidenceInterval[1]) + "]"
                    );
                    
                    writer.newLine();
                }
                
                writer.append("</table>");
                writer.newLine();
                writer.append("<p>");
                writer.newLine();
            }
            writer.append("<p>");
            writer.append("<strong>JVM used for testing:</strong> " + getJVMVersion());
        } catch (FileNotFoundException ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "Unable to find HTML file to export results of " + benchmarkName, ex);
        } catch (IOException ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "Error while exporting results of" + benchmarkName +" to HTML.", ex);
        }
    }

    private static CharSequence getJVMVersion() {
        String version;
        Properties p = System.getProperties();
        version = p.getProperty("java.vendor") + ": " + p.getProperty("java.version");
        return version;
    }
}
