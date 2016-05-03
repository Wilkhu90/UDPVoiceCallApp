package edu.auburn.UDPCallAPP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FloodingAlgorithm {
	
	static HashMap<String, String> configData;
	static HashMap<Integer, String> sysConfig;
	static String file = "/home/cse_h2/szw0069/Configuration_File_1.txt";
	static AtomicInteger psHop = new AtomicInteger();
	
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
	
	public static void main(String[] args) {
		
		createConfig();
		OLSRAlgorithm.init();
		BlockingQueue<byte[]> floodingQueue = new LinkedBlockingQueue<byte[]>();
		 
		//DemoRun demo = new DemoRun(floodingQueue);
		FloodReceiver fr = new FloodReceiver(floodingQueue);
		FloodSender fs = new FloodSender(floodingQueue);
		
		//Thread demoRun = new Thread(demo);
		Thread floodingProduce = new Thread(fr);
		Thread floodingConsume = new Thread(fs);
		
		//demoRun.start();
		floodingProduce.start();
		floodingConsume.start();
		
	}
	
}

class DemoRun implements Runnable {
	private BlockingQueue<byte[]> floodingQueue;
	public DemoRun(BlockingQueue<byte[]> floodingQueue) {
		this.floodingQueue = floodingQueue;
	}
	
	public void run() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(int i=0; i< 10; i++){
			long start = System.currentTimeMillis();
			byte[] header = new byte[28];
		    header[0] = (byte) 2;
		    header[1] = (byte) 5;
		    header[2] = (byte) 2;
		    String a = Integer.toString(i);
			System.arraycopy(a.getBytes(), 0, header, 3, a.getBytes().length);
			String message = "This is testing!!";
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(header, 0, header.length);
			baos.write(message.getBytes(), 0, message.getBytes().length);
		    try {
				floodingQueue.put(baos.toByteArray());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    //System.out.println(i+"here");
		    
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long stop = System.currentTimeMillis();
			//System.out.println("Time taken by Demo is "+ (stop - start));
		    }
		}
}

class FloodReceiver implements Runnable {
	
	private final BlockingQueue<byte[]> floodingQueue;
	boolean receiving = true;
	DatagramSocket socket;
	int bufferSize = 1028;
	int headerSize = 28;
	int sequenceNum;
	HashMap<String, String> cacheTable = new HashMap<String, String>();
	
	public FloodReceiver(BlockingQueue<byte[]> floodingQueue) {
		this.floodingQueue = floodingQueue;
	}

	
	@Override
	public void run() {
		try {
			receiving = true;
			//System.out.println("Trying to receive");
			while(receiving){
				long start = System.currentTimeMillis();
				String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
				int port = Integer.parseInt( FloodingAlgorithm.configData.get(hostId).split("\\s+")[3] );
				socket = new DatagramSocket(port);
				byte[] buffer = new byte[bufferSize];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				//System.out.println("waiting to receive data!!");
				socket.receive(packet);
				String Hellosource = null;
				try{
					ByteArrayInputStream bStream = new ByteArrayInputStream(packet.getData());
					ObjectInput oo = new ObjectInputStream(bStream);
					oo.close();
					OLSRHelloMessage helloMessage = (OLSRHelloMessage) oo.readObject();
					Hellosource = helloMessage.getSource();
					System.out.println(Hellosource);
					System.out.println(hostId);
				}
				catch(Exception e){
					
				}
				if(Hellosource == null){
					System.out.println("I received the packet.");
					byte[] bufferRcvData = new byte[bufferSize];
					ByteArrayInputStream in = new ByteArrayInputStream(packet.getData());
					in.read(bufferRcvData);
					//System.out.println(new String(bufferRcvData));
					int source = (byte) bufferRcvData[0];
					int dest = (byte) bufferRcvData[1];
					FloodingAlgorithm.psHop.set((byte) bufferRcvData[2]);
					byte[] buff = Arrays.copyOfRange(bufferRcvData, 3, headerSize);
					String number = new String(buff);
					if (!number.trim().isEmpty()){
						sequenceNum = Integer.parseInt(number.trim());
						if(!cacheTable.containsKey(sequenceNum+" "+source)){
							cacheTable.put(sequenceNum+" "+source,	FloodingAlgorithm.sysConfig.get(FloodingAlgorithm.psHop.get()));
							//System.out.println(bufferRcvData.length);
							//System.out.println("The packet received:\n"+new String(bufferRcvData)+ "\nfrom "+ packet.getAddress()+" with s/d/ps"+source+" "+dest+" "+FloodingAlgorithm.psHop.get()+" "+sequenceNum);
							// Modify packet to be ready with changed PSHOP.
							//String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
							int SID = Integer.parseInt( FloodingAlgorithm.configData.get(hostId).split("\\s+")[1] );
							int x1 = Integer.parseInt(FloodingAlgorithm.configData.get(FloodingAlgorithm.sysConfig.get(FloodingAlgorithm.psHop.get())).split("\\s+")[4]);
							int y1 = Integer.parseInt(FloodingAlgorithm.configData.get(FloodingAlgorithm.sysConfig.get(FloodingAlgorithm.psHop.get())).split("\\s+")[5]);
							int x2 = Integer.parseInt(FloodingAlgorithm.configData.get(FloodingAlgorithm.sysConfig.get(SID)).split("\\s+")[4]);
							int y2 = Integer.parseInt(FloodingAlgorithm.configData.get(FloodingAlgorithm.sysConfig.get(SID)).split("\\s+")[5]);
							//System.out.println(x1+" "+y1+" "+x2+" "+y2);
							//int drop_rate = getDropRate(x1, y1, x2, y2);
							//System.out.println("The drop rate is: "+ drop_rate);
							//Source & destination rejecting the packet.
							if(source != SID || dest != SID /*|| sequenceNum % drop_rate != 0*/){
								//System.out.println("1");
								for(int item : OLSRAlgorithm.myMPRs){
									//System.out.println(item + " :: " + FloodingAlgorithm.psHop.get()+" :: "+ source);
									if(item == FloodingAlgorithm.psHop.get() || FloodingAlgorithm.psHop.get() == source){
										//System.out.println("3");
										String newPsHop = FloodingAlgorithm.configData.get(hostId).split("\\s+")[1];
										bufferRcvData[2] = (byte) Integer.parseInt(newPsHop);
										try {
											//System.out.println("here");
											floodingQueue.put(bufferRcvData);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}		
										//System.out.println("Added");
									}
								}
								
								
							}
						}
						else{
							//Drop the packet.
							//System.out.println("Already in Cache table.");
						}
					}
				}
				
				socket.close();
				socket.disconnect();
				//Thread.sleep(200);
				
				long stop = System.currentTimeMillis();
				//System.out.println("Time taken by receiving is "+ (stop - start));
			}
			receiving = false;
		}
		catch(Exception e){
			e.printStackTrace();
			socket.close();
			socket.disconnect();
			receiving = false;
		}
		
	}
	
}

class FloodSender implements Runnable {
	boolean sending = true;
	
	private BlockingQueue<byte[]> floodingQueue;
	
	public FloodSender(BlockingQueue<byte[]> floodingQueue) {
		this.floodingQueue = floodingQueue;
	}

	@Override
	public void run() {
		sending = true;
		try{
			
			while(sending){
					long start = System.currentTimeMillis();
					//System.out.println("Am I Here????");
					String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
					String[] nodeDetails = FloodingAlgorithm.configData.get(hostId).split("\\s+");
					//System.out.println("I'm here");
					DatagramSocket socket = new DatagramSocket();
					int ps = FloodingAlgorithm.psHop.get();
					if(nodeDetails.length > 7) {
						byte[] bufferSendData = floodingQueue.take();
						for(int i=7; i< nodeDetails.length; i++){
							//System.out.println("Sending to: "+Integer.parseInt(nodeDetails[i]) +" psHop "+FloodingAlgorithm.psHop.get());
							//System.out.println(nodeDetails[i] +" :: "+ps);
							if(Integer.parseInt(nodeDetails[i]) != ps && OLSRAlgorithm.IamMPR){
								
								InetAddress broadcastIP = InetAddress.getByName(FloodingAlgorithm.sysConfig.get(Integer.parseInt(nodeDetails[i]))+".eng.auburn.edu");
								System.out.println("Sending data to "+ broadcastIP);
								int port = Integer.parseInt(FloodingAlgorithm.configData.get(FloodingAlgorithm.sysConfig.get(Integer.parseInt(nodeDetails[i]))).split("\\s+")[3] );
								//System.out.println("This is the port used "+port);
								//System.out.println("Sending packet is: "+ new String(bufferSendData));
								
								//System.out.println(bufferSendData);
								DatagramPacket packet = new DatagramPacket(bufferSendData, bufferSendData.length, broadcastIP, port);
								socket.send(packet);
							}
						}
					}	
					socket.disconnect();
					socket.close();
					//Thread.sleep(1000);
					long stop = System.currentTimeMillis();
					//System.out.println("Time taken by sending is "+ (stop - start));
				}
			sending = false;
		}
		
		catch(Exception e){
			e.printStackTrace();
			sending = false;
		}
		
	}
}


