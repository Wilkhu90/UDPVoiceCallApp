package edu.auburn.UDPCallAPP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

class Producer1 implements Runnable{
	
	private final BlockingQueue<byte[]> sharedQueue;
	boolean receiving = true;
	DatagramSocket socket;
	int bufferSize = 1028;
	int headerSize = 28;
	int sequenceNum;
	HashMap<String, String> cacheTable = new HashMap<String, String>();
	
	public Producer1(BlockingQueue<byte[]> sharedQueue) {
		this.sharedQueue = sharedQueue;
	}
	
	private int getDropRate(int x1, int y1, int x2, int y2) {
		double x = Math.pow(x1 - x2, 2);
		double y = Math.pow(y1 - y2, 2);
		double distance = Math.sqrt(x + y);
		if(distance <= 100){
			double numeratorFn = Math.pow(4*Math.PI*distance, 2);
			double denominatorFn = Math.pow(4*Math.PI, 2);
			double solution = ((numeratorFn/denominatorFn)/100);
			return (int) (distance < 10 ? 0 : Math.round(Math.abs(1-solution)));
		}
		return 100;
	}

	@Override
	public void run() {
		try {
			receiving = true;
			//System.out.println("Trying to receive");
			while(receiving){
				long start = System.currentTimeMillis();
				
				String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
				int port = Integer.parseInt( Processing.configData.get(hostId).split("\\s+")[3] );
				socket = new DatagramSocket(port);
				//socket.setSoTimeout(2000);
				byte[] buffer = new byte[bufferSize];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				System.out.println("waiting to receive!!");
				socket.receive(packet);
				//System.out.println("I received the packet.");
				byte[] bufferRcvData = packet.getData();
				int source = (byte) bufferRcvData[0];
				int dest = (byte) bufferRcvData[1];
				//System.out.println(bufferRcvData[2]);
				Processing.psHop = (byte) bufferRcvData[2];
				byte[] buff = Arrays.copyOfRange(bufferRcvData, 3, headerSize);
				String number = new String(buff);
				if (!number.trim().isEmpty()){
					sequenceNum = Integer.parseInt(number.trim());
					if(!cacheTable.containsKey(sequenceNum+" "+source)){
						cacheTable.put(sequenceNum+" "+source,	Processing.sysConfig.get(Processing.psHop));
						//System.out.println(bufferRcvData.length);
						//System.out.println("The packet received:\n"+new String(bufferRcvData)+ "\nfrom "+ packet.getAddress()+" with s/d/ps"+source+" "+dest+" "+Processing.psHop+" "+sequenceNum);
						// Modify packet to be ready with changed PSHOP.
						//String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
						int SID = Integer.parseInt( Processing.configData.get(hostId).split("\\s+")[1] );
						int x1 = Integer.parseInt(Processing.configData.get(Processing.sysConfig.get(Processing.psHop)).split("\\s+")[4]);
						int y1 = Integer.parseInt(Processing.configData.get(Processing.sysConfig.get(Processing.psHop)).split("\\s+")[5]);
						int x2 = Integer.parseInt(Processing.configData.get(Processing.sysConfig.get(SID)).split("\\s+")[4]);
						int y2 = Integer.parseInt(Processing.configData.get(Processing.sysConfig.get(SID)).split("\\s+")[5]);
						//System.out.println(x1+" "+y1+" "+x2+" "+y2);
						int drop_rate = getDropRate(x1, y1, x2, y2);
						//System.out.println("The drop rate is: "+ drop_rate);
						//Source & destination rejecting the packet.
						if(source != SID /*|| dest != SID || && sequenceNum % drop_rate != 0*/){
							String newPsHop = Processing.configData.get(hostId).split("\\s+")[1];
							bufferRcvData[2] = (byte) Integer.parseInt(newPsHop);
							try {
								//System.out.println("Here");
								sharedQueue.put(bufferRcvData);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}		
							//System.out.println("Added");
							
						}
					}
					else{
						//Drop the packet.
						//System.out.println("Already in Cache table.");
					}
				}
				socket.close();
				socket.disconnect();
				//Thread.sleep(2000);
				
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

class Consumer1 implements Runnable{
	
	private AudioFormat getAudioFormat() {
        int sampleRate = 8000;
        int sampleInbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
    }

	private BlockingQueue<byte[]> sharedQueue;
	
	public Consumer1(BlockingQueue<byte[]> sharedQueue) {
		this.sharedQueue = sharedQueue;
	}
	@Override
	public void run() {
		boolean active = true;
		try{
			AudioFormat adFormat = getAudioFormat();
			DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);
			SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			sourceLine.open(adFormat);
			
			sourceLine.start();
			
			while(active){
				long start = System.currentTimeMillis();
				//System.out.println("Taking Thread 2");
				byte[] receivedBytes = sharedQueue.take();
				//System.out.println(receivedBytes);
				if(receivedBytes.length > 0){
					byte[] soundBytes = Arrays.copyOfRange(receivedBytes, 28, receivedBytes.length);
					//System.out.println("Received length : "+soundBytes.length);
					sourceLine.write(soundBytes, 0, soundBytes.length);
					//Thread.sleep(2000);
				}
				long stop = System.currentTimeMillis();
	        	//System.out.println("Time taken by listening: "+(stop - start));
			}
			sourceLine.stop();
			sourceLine.flush();
			active = false;
		}
		catch(Exception e){
			e.printStackTrace();
			active = false;
		}
	}
	
}