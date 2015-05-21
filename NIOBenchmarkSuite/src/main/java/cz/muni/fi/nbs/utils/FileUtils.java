/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petom_000
 */
public class FileUtils {
    
     private static final int SEED = 111;
    
    //max 1351 MB
    public static void createFile(String name, int size) throws IOException, InterruptedException{
        
        File f = new File(name);
        f.deleteOnExit();
        if (f.exists()) {
            //System.out.println("ALREADY EXISTS");
            deleteFile(name);
            
            if (f.exists()) {
                System.out.println("WARNING: UNABLE TO REWRITE FILE!");
                System.out.println("Some measuring results could be wrong.");
            } else {
                create(f, size);
            }
        } else {
            create(f, size);
        }
        
        
    }
    
    public static void createManyFiles(String[] names, int size) throws IOException, InterruptedException {
        int n = names.length;
        for(int i=0; i<n; i++) {
            createFile(names[i], size);
        }
    }
    
    public static void createTmpFile(String name, int size) throws IOException, InterruptedException {
        createFile(name, size);
        new File(name).deleteOnExit();
    }
    
    public static void createManyTmpFiles(String[] names, int size) throws IOException, InterruptedException {
        int n = names.length;
        for(int i=0; i<n; i++) {
            createTmpFile(names[i], size);
        }
    }
    
    public static String[] getNames(int n, String baseName){
        String[] names = new String[n];
        for(int i=0; i<n; i++) {
            names[i] = baseName+'_'+i;
        }
        return names;
    }
    
    public static void deleteFile(String name) throws InterruptedException, IOException {
  
            File f = new File(name);
            if (f.exists()) {
                try {
                    Files.delete(f.toPath());
                } catch (IOException ex) {
                    Logger.getLogger(FileUtils.class.getName()).log(Level.FINE, null, ex);
                }
                
                // If not successful try to force GC in case of a lingering mapping and try again.
                if (f.exists()) {
                    System.gc();
                    TimeUnit.SECONDS.sleep(10);
                    Files.delete(f.toPath());
                }
            }
    }
    
    public static void deleteManyFiles(String[] names) throws InterruptedException, IOException {
        for (String name:names) {
            deleteFile(name);
        }
    }
    
    public static void createTemp() throws IOException{
        
        File tmp = new File("tmp");
        tmp.deleteOnExit();
        if (!tmp.exists()) Files.createDirectory(tmp.toPath());
    }
    
    public static String createResultsDir() throws IOException{
        StringBuilder nameBuilder = new StringBuilder("results_");
        
        Calendar now = Calendar.getInstance();
        nameBuilder.append(now.get(Calendar.HOUR)).append("_")
                .append(now.get(Calendar.MINUTE)).append("_")
                .append(now.get(Calendar.DATE)).append("_")
                .append(now.get(Calendar.MONTH)+1).append("_")
                .append(now.get(Calendar.YEAR));
        
        String name = nameBuilder.toString();
        Path resultsPath = Paths.get(name, "charts");
        Files.createDirectories(resultsPath);
        return name;
    }
    
    public static void createDeepTree(Path root, int depth, String testFileName) throws IOException, FileNotFoundException, InterruptedException {
        if (depth == 1) {
            String fileName = root.toString() + "\\" + testFileName;
            createTmpFile(fileName, 0);
            Path newDir = Files.createTempDirectory(root, "dir");
            newDir.toFile().deleteOnExit();
            fileName = newDir.toString() + "\\" + "lastFile";
            createTmpFile(fileName, 0);
            
        } else {
            Files.createTempFile(root, "file", null).toFile().deleteOnExit();
            Path newDir = Files.createTempDirectory(root, "dir");
            newDir.toFile().deleteOnExit();
            createDeepTree(newDir, depth-1, testFileName);
        }
        
    }
    
    public static void createWideTree(Path root, int depth, int width, String testFileName, boolean rightmost) throws IOException, InterruptedException {
        
        if (depth == 0) {
            int n = rightmost ? width-1 : width;
            String fileName = root.toString() + "\\" + "0file";
            createManyTmpFiles(getNames(n, fileName), 0);
            if (rightmost) {
                fileName = root.toString() + "\\" + testFileName;
                createTmpFile(fileName, 0);
            }
        } else {
            for (int i=0; i<width; i++) {
               boolean newRightmost = rightmost && (i==width-1);
               String dirName = newRightmost ? "dir" : "_0dir";
               Path newDir = Files.createTempDirectory(root, dirName);
               newDir.toFile().deleteOnExit();
               createWideTree(newDir, depth-1, width, testFileName, newRightmost);
            }
        }
    }
    
    public static void createBigDir(Path parent, int fileCount, int dirCount, String name) throws IOException {
        Path path = Paths.get(parent.toString(), name);
        Files.createDirectory(path).toFile().deleteOnExit();
        
        for (int i=0; i<fileCount; i++) {
            Files.createTempFile(path, "file", null).toFile().deleteOnExit();
        }
        for (int i=0; i<dirCount; i++) {
            Files.createTempDirectory(path, "dir").toFile().deleteOnExit();
        }
    }
    
    private static void create(File f, int size) throws IOException{
        
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.setLength(size);
        
        if (size > 0) {
            byte[] randomBytes = new byte[size];
            new Random(SEED).nextBytes(randomBytes);

            ByteBuffer buf = ByteBuffer.wrap(randomBytes);
            raf.getChannel().write(buf);
        }
        
        raf.close();
    }
    
}
