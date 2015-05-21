/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author petomackay
 */
public class CleanUpHook extends Thread{
    
    @Override
    public void run() {
        
        Path dir = Paths.get("tmp");
        
        if (!Files.exists(dir)) return;
        
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {

                    try {
                        FileUtils.deleteFile(file.toString());
                    } catch (InterruptedException ex) {
                        Logger.getLogger(CleanUpHook.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return CONTINUE;
                }

              @Override
              public FileVisitResult postVisitDirectory(Path dir,
                      IOException exc) throws IOException {
                  
                  if (exc == null) {
                      Files.delete(dir);
                      return CONTINUE;
                  } else {
                      throw exc;
                  }
              }

          });
        } catch (IOException ex) {
             Logger.getLogger(CleanUpHook.class.getName()).log(Level.SEVERE, "An error occured while trying to delete the temp direcotry.", ex);
        }
    }
    
}
