package edu.auburn.UDPCallAPP;

import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class FileWatcher extends TimerTask {
  private long timeStamp;
  private File file;

  public FileWatcher( File file ) {
    this.file = file;
    this.timeStamp = file.lastModified();
  }

  public final void run() {
	  
	  FileUtils f = new FileUtils();
	  String path = "/home/cse_h2/szw0069/Configuration_File_1.txt";
	  String hostId;
	try {
		hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
		if(hostId.equals("tux202")){
			  ArrayList<GetNode> lists = f.readFile(path);
			  f.WriteFile(lists, path);
		  }
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	  long timeStamp = file.lastModified();

	  if( this.timeStamp != timeStamp ) {
		  this.timeStamp = timeStamp;
		  onChange(file);
	  }
	}

  protected abstract void onChange( File file );
}
