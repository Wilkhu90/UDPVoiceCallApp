package edu.auburn.UDPCallAPP;

import java.util.*;
import java.io.*;

public class FileWatcherTest {
  public static void main(String args[]) {
    // monitor a single file
    TimerTask task = new FileWatcher( new File("/home/cse_h2/szw0069/Configuration_File.txt") ) {
      protected void onChange( File file ) {
        // here we code the action on a change
        System.out.println( "File "+ file.getName() +" have change !" );
      }
    };

    Timer timer = new Timer();
    // repeat the check every second
    timer.schedule( task , new Date(), 1000 );
  }
}
