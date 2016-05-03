package edu.auburn.UDPCallAPP;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class Processing {
	
	static HashMap<String, String> configData = UI.configData;
	static HashMap<Integer, String> sysConfig = UI.sysConfig;
	static String file = "/home/cse_h2/szw0069/Configuration_File_1.txt";
	static int psHop;
	
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
	
	/*public static void main(String[] args){
		
		createConfig();
		
		BlockingQueue<byte[]> SendingQueue = new LinkedBlockingQueue<byte[]>();
		BlockingQueue<byte[]> ReceivingQueue = new LinkedBlockingQueue<byte[]>();
		
		Producer prod = new Producer(SendingQueue);
		Consumer cons = new Consumer(SendingQueue);
		
		Producer1 prod1 = new Producer1(ReceivingQueue);
		Consumer1 cons1 = new Consumer1(ReceivingQueue);
		
		Thread CaptureAudioThread = new Thread(prod);
		Thread floodSendThread = new Thread(cons);
		Thread floodReceiveThread = new Thread(prod1);
		Thread listenAudioThread = new Thread(cons1);
		
		CaptureAudioThread.start();
		floodSendThread.start();
		floodReceiveThread.start();
		listenAudioThread.start();
		
	}*/

}

class Producer implements Runnable{
	
	private final BlockingQueue<byte[]> sharedQueue;
	static int sequence_number = 1;
	static int header_size = 28;
	
	private AudioFormat getAudioFormat() {
        int sampleRate = 8000;
        int sampleInbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
    }
	
	public byte[] createHeader(int[] info) {
    	
    	byte[] header = new byte[header_size];
    	header[0] = (byte) info[0];
    	header[1] = (byte) info[1];
    	header[2] = (byte) info[2];
    	String a = Integer.toString(sequence_number);
		System.arraycopy(a.getBytes(), 0, header, 3, a.getBytes().length);
    	sequence_number++;
    	return header;
    }
	
	public Producer(BlockingQueue<byte[]> sharedQueue) {
		this.sharedQueue = sharedQueue;
	}

	@Override
	public void run() {
		boolean stopaudioCapture = true;
		AudioFormat adFormat = getAudioFormat();
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
        TargetDataLine targetDataLine;
		try {
			targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
			targetDataLine.open(adFormat);
	        targetDataLine.start();
	        byte[] tempBuffer = new byte[1000];
	        while(stopaudioCapture){
	        	long start = System.currentTimeMillis();
	        	int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
	        	if(cnt > 0){
	        		//System.out.println("putting Thread 1");
	        		
	        		//Temp
	        		int[] Info = UI.info;
	        		byte[] header = createHeader(Info);
		            ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    baos.write(header, 0, header.length);
				    baos.write(tempBuffer, 0, cnt);
				    baos.close();
				    //System.out.println("Sound length : "+baos.toByteArray().length);
	        		sharedQueue.put(baos.toByteArray());
	        	}
	        	//Thread.sleep(2000);
	        	long stop = System.currentTimeMillis();
	        	//System.out.println("sound capture take: "+(stop - start));
	        }
	        targetDataLine.stop();
            targetDataLine.flush();
            stopaudioCapture = false;
            
		} catch (Exception e) {
            StackTraceElement stackEle[] = e.getStackTrace();
            for (StackTraceElement val : stackEle) {
                //System.out.println(val);
            }
            stopaudioCapture = true;
        }
	}
	
}

class Consumer implements Runnable{

	boolean sending = true;
	private BlockingQueue<byte[]> sharedQueue;
	
	public Consumer(BlockingQueue<byte[]> sharedQueue) {
		this.sharedQueue = sharedQueue;
	}
	@Override
	public void run() {
		sending = true;
		boolean active = true;
		try{
			while(active){
				long start = System.currentTimeMillis();
				
				//System.out.println("Taking Thread 1");
				//System.out.println("Am I Here????");
				String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
				String[] nodeDetails = Processing.configData.get(hostId).split("\\s+");
				//System.out.println("I'm here");
				DatagramSocket socket = new DatagramSocket();
				byte[] bufferSendData = sharedQueue.take();
				if(nodeDetails.length > 7 && bufferSendData.length > 0) {
					for(int i=7; i< nodeDetails.length; i++){
						//if(Integer.parseInt(nodeDetails[i]) != Processing.psHop){
							//System.out.println(nodeDetails[i]);
							
							InetAddress broadcastIP = InetAddress.getByName(Processing.sysConfig.get(Integer.parseInt(nodeDetails[i]))+".eng.auburn.edu");
							System.out.println("Sending data to "+ broadcastIP);
							int port = Integer.parseInt(Processing.configData.get(Processing.sysConfig.get(Integer.parseInt(nodeDetails[i]))).split("\\s+")[3] );
							//System.out.println("This is the port used "+port);
							//System.out.println("Sending packet is: "+ bufferSendData.length+"sequence: "+bufferSendData[3]);
							
							DatagramPacket packet = new DatagramPacket(bufferSendData, bufferSendData.length, broadcastIP, port);
							socket.send(packet);
						//}
					}
				}	
				socket.disconnect();
				socket.close();
				//Thread.sleep(2000);
				long stop = System.currentTimeMillis();
	        	//System.out.println("Sound sending take"+(stop - start));
			}
			sending = false;
		}
		catch(Exception e){
			e.printStackTrace();
			sending = false;
		}
	}
	
}