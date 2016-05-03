package edu.auburn.UDPCallAPP;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UI extends JFrame{
	/**
	 * UI creates the user interface and consist of main function.
	 */
	private static final long serialVersionUID = 1L;
	static HashMap<String, String> configData;
	static HashMap<Integer, String> sysConfig;
	static String file = "/home/cse_h2/szw0069/Configuration_File_1.txt";
	static Thread CaptureAudioThread;
	static Thread floodSendThread;
	static Thread floodReceiveThread; 
	static Thread listenAudioThread;
	static int[] info;
	
    Connect conn;
    JFrame frame;
    HashMap<String, InetAddress> contact;
    String selectedContact;
	
	public static void main(String args[]){
		//Read file to create configuration
		createConfig();		
		
		BlockingQueue<byte[]> SendingQueue = new LinkedBlockingQueue<byte[]>();
		BlockingQueue<byte[]> ReceivingQueue = new LinkedBlockingQueue<byte[]>(30);
		
		Producer prod = new Producer(SendingQueue);
		Consumer cons = new Consumer(SendingQueue);
		
		Producer1 prod1 = new Producer1(ReceivingQueue);
		Consumer1 cons1 = new Consumer1(ReceivingQueue);
		
		CaptureAudioThread = new Thread(prod);
		floodSendThread = new Thread(cons);
		floodReceiveThread = new Thread(prod1);
		listenAudioThread = new Thread(cons1);
		
		// monitor a single file
	    /*TimerTask task = new FileWatcher( new File(file) ) {
	      protected void onChange( File file ) {
	        // here we code the action on a change
	        System.out.println( "File "+ file.getName() +" have change !" );
	        createConfig();
	        for(String item : configData.keySet())
				System.out.println(configData.get(item));
	      }
	    };

	    Timer timer = new Timer();
	    // repeat the check every second
	    timer.schedule( task , new Date(), 20000 );*/
		
        new UI();
    }
	private static void createConfig() {
		configData = new HashMap<String, String>();
		sysConfig = new HashMap<Integer, String>();
		Scanner sc;
		try {
			sc = new Scanner(new FileReader(file));
			while (sc.hasNextLine()) {
	        	String line = sc.nextLine();
	        	String[] nodeDetails = line.split("\\s+");
	        	configData.put(nodeDetails[2], line);
	        	sysConfig.put(Integer.parseInt(nodeDetails[1]), nodeDetails[2]);
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// Creates the UI for the Application.
	public UI() {
		JFrame frame = new JFrame("UDP Call App");
        frame.getContentPane().setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setSize(250, 300);
        frame.getContentPane().setBackground(Color.white);
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        JPanel panel = new JPanel();
        panel.setBackground(Color.white);
        panel.setSize(300,300);
        GridLayout layout = new GridLayout(0,2);
        layout.setHgap(10);
        layout.setVgap(10);
        
        
		
        final JButton capture = new JButton("Call/Answer");
        final JButton stop = new JButton("End Call");
		final JButton connect = new JButton("Connect");
		final JButton disconnect = new JButton("Disconnect");
		final JTextArea txtArea = new JTextArea(10,10);
		final JTextArea heading = new JTextArea(1,1);
		
		
        capture.setEnabled(false);
        stop.setEnabled(false);
		connect.setEnabled(true);
		disconnect.setEnabled(false);
		txtArea.setEnabled(false);
		txtArea.setFont(new Font("Courier New", Font.PLAIN, 12));
		heading.setEnabled(true);
		heading.setText("List of connected Users :");
		heading.setFont(new Font("Courier New", 1, 17));
		heading.setEditable(false);

		connect.addActionListener(
                new ActionListener() {
                    public void actionPerformed(
                            ActionEvent e) {
                        capture.setEnabled(true);
                        connect.setEnabled(false);
                		txtArea.setEnabled(true);
                		txtArea.setCursor(Cursor.getDefaultCursor());
                        disconnect.setEnabled(true);
                        conn = new Connect(configData);
                        displayContacts(conn, txtArea);
                        
                    }
                });
		
		txtArea.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent me){
						int line;
						try {
							line = txtArea.getLineOfOffset( txtArea.getCaretPosition() );
							int start = txtArea.getLineStartOffset( line );
							int end = txtArea.getLineEndOffset( line );
							Highlighter hilite = txtArea.getHighlighter();
							DefaultHighlighter.DefaultHighlightPainter highlightPainter = 
									new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
							hilite.addHighlight(start, end - start -1, highlightPainter);
							selectedContact = txtArea.getDocument().getText(start, end - start-1);
							txtArea.setEditable(false);
							System.out.println("The selected contact is "+ selectedContact+" IP "+ conn.getContacts().get(selectedContact));
						} catch (BadLocationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				
					}
				});
		
		disconnect.addActionListener(
                new ActionListener() {
                    public void actionPerformed(
                            ActionEvent e) {
                    	capture.setEnabled(false);
                        stop.setEnabled(false);
                		connect.setEnabled(true);
                		disconnect.setEnabled(false);
                		txtArea.setText(null);
                		txtArea.setEnabled(false);
                        conn.stopBroadcasting();
                        conn.stopListening();
                        
                    }
                });
        
        capture.addActionListener(
                new ActionListener() {
                    public void actionPerformed(
                            ActionEvent e) {
                        capture.setEnabled(false);
                        stop.setEnabled(true);
                        if(!selectedContact.isEmpty()){
                        	info = makeInfo();
                        	System.out.println(info[0]+","+info[1]+","+info[2]);
                        	CaptureAudioThread.start();
                    		floodSendThread.start();
                    		floodReceiveThread.start();
                    		listenAudioThread.start();
                        }
                    }

					private int[] makeInfo() {
						int[] res = new int[3];
						try {
							String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
							String destId = InetAddress.getByName(selectedContact).getHostName().split("\\.")[0];
							
							int source = Integer.parseInt(configData.get(hostId).split("\\s+")[1]);
							int dest = Integer.parseInt(configData.get(destId).split("\\s+")[1]);
							System.out.println(source+" "+dest);
							res[0] = source;
							res[1] = dest;
							res[2] = source;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						return res;
					}
                });

        stop.addActionListener(
                new ActionListener() {
                    public void actionPerformed(
                            ActionEvent e) {
                        capture.setEnabled(true);
                        stop.setEnabled(false);
                        CaptureAudioThread.interrupt();
                        floodSendThread.interrupt();
                		floodReceiveThread.interrupt();
                		listenAudioThread.interrupt();
                    }
                });
		
		
        
        panel.setLayout(layout);       
        panel.add(connect);
        panel.add(disconnect); 
        panel.add(capture); 
        panel.add(stop); 
        controlPanel.add(panel);
        frame.getContentPane().add(heading);
        frame.getContentPane().add(txtArea);
        frame.add(controlPanel);
        frame.setVisible(true);
    }
	
	public void displayContacts(final Connect conn, final JTextArea text){
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				HashMap<String, InetAddress> listUser = conn.getContacts();
				while(listUser.keySet().size() < 1){
					listUser = conn.getContacts();
				}
				System.out.println("Here");
				for(String name :listUser.keySet()){
					text.append(name+"\n");
				}
				contact = listUser;
				for (String name: contact.keySet())
					System.out.println(name + " : IP :" + contact.get(name));
				
				text.setEnabled(true);
			}
			
		});
		t.start(); 
		
				
	}

}
