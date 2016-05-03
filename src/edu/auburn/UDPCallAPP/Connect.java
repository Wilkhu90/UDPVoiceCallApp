package edu.auburn.UDPCallAPP;

import java.net.*;
import java.util.HashMap;

public class Connect {
	String name ;
	HashMap<String, InetAddress> contactList;
	int BROADCAST_PORT = 10142;
	boolean LISTEN = true;
	boolean BROADCAST = true;
	int buffer_size = 5000;
	static HashMap<String, String> configData;
	
	public Connect(HashMap<String, String> config){
		try{
			configData = config;
				
			
			contactList = new HashMap<String, InetAddress>();
			
			
			for(String node : configData.keySet()){
				String IPToContact = node;
				System.out.println("Here:"+IPToContact);
				InetAddress IPaddress = InetAddress.getByName(IPToContact);
				System.out.println(IPaddress);
				name = InetAddress.getLocalHost().getHostName();	
				broadCastListener(name, IPaddress);
			}
			contactListener();
			
		}
		catch(Exception e){
			LISTEN = false;
			BROADCAST = false;
		}
	}
	
	// Ends the broadcasting thread
	public void stopBroadcasting() {
		BROADCAST = false;
	}
	
	// Stops the listener thread
	public void stopListening() {
		LISTEN = false;
	}
	// Sending packets for network presence.
	public void broadCastListener(final String name1, final InetAddress broadcastIP) {
		Thread broadcastThread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					
					String request = name1;
					byte[] message = request.getBytes();
					DatagramSocket socket = new DatagramSocket();
					socket.setBroadcast(true);
					DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
					while(BROADCAST) {
						
						socket.send(packet);
						//System.out.println(broadcastIP);
						//System.out.println("Packet sent from broadcast "+ new String(packet.getData()));
						Thread.sleep(3000);
					}
					socket.disconnect();
					socket.close();
					return;
				}
				catch(Exception e) {
					BROADCAST = false;
					e.printStackTrace();
					return;
				}
			}
		});
		broadcastThread.start();
	}

	public HashMap<String, InetAddress> getContacts() {
		return contactList;
	}
	
	public void addContact(String name, InetAddress address) {
		if(!contactList.containsKey(name)) {
			contactList.put(name, address);
			return;
		}
		for (String names : contactList.keySet()){
			//System.out.println(names+ " : IP :" + contactList.get(names));
		}
		return;
		
	}
	// Listening to packets for creating HashMap.
	//HashMap helps in creating Contact manager.
	public void contactListener() {
		Thread listenThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					
					DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
					byte[] buffer = new byte[30000];
					
					while(LISTEN) {
						listen(socket, buffer);
					}
					socket.disconnect();
					socket.close();
					return;
				} 
				catch (Exception e) {
					LISTEN = false;
				}
				
			}
			
			

			public void listen(DatagramSocket socket, byte[] buffer) {
				try{
					DatagramPacket packet = new DatagramPacket(buffer, buffer_size);
					socket.setSoTimeout(15000);
					socket.receive(packet);
					//System.out.println("Packet received from broadcast "+new String(packet.getData()));
					String addData = new String(packet.getData());
					addContact(addData, InetAddress.getByName(packet.getAddress().getHostName()));
				}
				catch(Exception e){
					if(LISTEN) {
						listen(socket, buffer);
					}
				}
				
			}
		});
		listenThread.start();
		
	}
}
