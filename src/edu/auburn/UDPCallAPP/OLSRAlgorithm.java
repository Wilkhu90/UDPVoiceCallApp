package edu.auburn.UDPCallAPP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Sumeet
 *
 *1) It will read configuration file after the end of pause time.
 *2) Then flood hello message to the links from configuration.
 *3) This way we will sense neighbor and create 1-hop and 2-hop table.
 *4) Then MPR selection will happen and MPR selector table will be prepared.
 *5) Once MPR selector table is there, the node will receive sound data and flood to only MPR nodes from MPR selector table.
 *6) P.S. - The sequence number of MPR table will be used for the freshness of information.
 *
 */

public class OLSRAlgorithm {
	
	public static HashMap<String, String> configData = FloodingAlgorithm.configData;
	public static HashMap<Integer, String> sysConfig = FloodingAlgorithm.sysConfig;
	public static String file = "/home/cse_h2/szw0069/Configuration_File_1.txt";
	public static final int ASYM_NEIGH = 0;
	public static final int SYM_NEIGH = 1;
	public static final int MPR_NEIGH = 2;
	public static final int NOT_NEIGH = 3;
	public static AtomicLong timeout;
	public static AtomicLong start;
	public static boolean IamMPR = false;
	
	public static ConcurrentHashMap<Integer, ArrayList<Integer>> twoHopNeighbours = new ConcurrentHashMap<Integer, ArrayList<Integer>>();
	public static ConcurrentHashMap<Integer, Integer> neighbours = new ConcurrentHashMap<Integer, Integer>();
	//private ConcurrentHashMap oldNeighbours = new ConcurrentHashMap();
	public static CopyOnWriteArrayList<Integer> myMPRs = new CopyOnWriteArrayList<Integer>();
	
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
	
	public static boolean checkMPRList(int neigh){
		for(int mpr : myMPRs){
			if(mpr == neigh){
				return true;
			}
		}
		return false;
	}
	
	public static void init() {
		//createConfig();
		timeout = new AtomicLong(5000);
		start = new AtomicLong(System.currentTimeMillis());
		OLSRAlgorithm olsr = new OLSRAlgorithm();
		OLSRHelloMessageThread helloThread = new OLSRHelloMessageThread(olsr, timeout);
		helloThread.start();
		receiveHelloMessage();
		try {
			helloThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("My Neigbours are: ");
		for(int keys:neighbours.keySet()){
			System.out.print(keys + " : ");
			System.out.print(neighbours.get(keys));
			System.out.println();
		}
		System.out.println("Am I an MPR: "+ IamMPR);
	}
	
	// Some other methods for OLSR goes here to receive the message and do the necessary changes to the neighbors.
	public ArrayList<Integer> getNeighbours() {
		return new ArrayList<Integer>(neighbours.keySet());
	}
	public ConcurrentHashMap<Integer, Integer> getNeighboursMap(){
		return neighbours;
	}
	
	
	public static void receiveHelloMessage(){
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				try{
					//System.out.println(System.currentTimeMillis());
					while(System.currentTimeMillis() - start.get() < timeout.get()){
						
						String hostId = InetAddress.getLocalHost().getHostName().split("\\.")[0];
						int port = Integer.parseInt( OLSRAlgorithm.configData.get(hostId).split("\\s+")[3] )+1;
						DatagramSocket socket = new DatagramSocket(port);
						socket.setSoTimeout(1000);
						byte[] buffer = new byte[1000];
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						//System.out.println("waiting to receive!!");
						socket.receive(packet);
						//System.out.println("The hello message received "+ packet.getData());
						
						ByteArrayInputStream bStream = new ByteArrayInputStream(packet.getData());
						ObjectInput oo = new ObjectInputStream(bStream);
						oo.close();
						OLSRHelloMessage helloMessage = (OLSRHelloMessage) oo.readObject();
						//System.out.println(helloMessage);
						ArrayList<String> nbrs = helloMessage.getNeighbourIds();
						ArrayList<Integer> nbrsStatus = helloMessage.getNeighbourStatus();
						
						Iterator iter = nbrs.iterator();
						Iterator iter1 = nbrsStatus.iterator();
						while(iter.hasNext()){
							Integer status = (Integer) iter1.next();
							String hId = sysConfig.get(Integer.parseInt((String) iter.next()));
							if(hId.equals(hostId)){
								//System.out.println("I'm here at: "+ hId + " "+status);
								if(status == MPR_NEIGH){
									IamMPR = true;
								}
							}
						}
						//System.out.println(sysConfig.containsValue(helloMessage.getSource()));
						for(Integer i: sysConfig.keySet()){
							if(sysConfig.get(i).equals(helloMessage.getSource())){
								neighbours.putIfAbsent(i, SYM_NEIGH);
							}
						}
						
						ArrayList<String> fetchedNeigbourID =  helloMessage.getNeighbourIds();
						ArrayList<Integer> fetchedNeighbourLink = helloMessage.getNeighbourStatus();
						Iterator nIter = fetchedNeigbourID.iterator();
						Iterator nLinkIter = fetchedNeighbourLink.iterator();
						int nodeId =-1;
						int sId =-1;
						for(Integer i: sysConfig.keySet()){
							if(sysConfig.get(i).equals(hostId)){
								nodeId = i;
							}
							if(sysConfig.get(i).equals(helloMessage.getSource())){
								sId = i;
							}
						}
						while(nIter.hasNext()){
							String nId = (String) nIter.next();
							Integer nLink = (Integer) nLinkIter.next();
							//System.out.println(nId+" :: "+ nLink);
							if(Integer.parseInt(nId) != nodeId){
								ArrayList<Integer> tempArray;
								if(twoHopNeighbours.containsKey(Integer.parseInt(nId))){
									tempArray = twoHopNeighbours.get(Integer.parseInt(nId));
									boolean checked = checkIfPresent(tempArray, sId);
									if(!checked){
										tempArray.add(sId);
										twoHopNeighbours.putIfAbsent(Integer.parseInt(nId), tempArray);
									}
								}
								else{
									tempArray = new ArrayList<Integer>();
									tempArray.add(sId);
									twoHopNeighbours.putIfAbsent(Integer.parseInt(nId), tempArray);
								}
								
							}
						}
						
						//System.out.println("2-hop Neigbours are:");
						for(int keyset:twoHopNeighbours.keySet()){
							//System.out.print(keyset + " ::: ");
							//System.out.print(twoHopNeighbours.get(keyset));
							//System.out.println();
						}
						if (System.currentTimeMillis() - start.get() > 3000){
							Iterator mprIter = twoHopNeighbours.keySet().iterator();
							if(mprIter.hasNext()){
								MPRSelection generateMPR = new MPRSelection();
								generateMPR.mpr_select(twoHopNeighbours);
								myMPRs = MPRSelection.mpr;
								for(int mpr : myMPRs){
									//System.out.println("The MPRs are : "+ mpr);
									neighbours.replace(mpr, OLSRAlgorithm.MPR_NEIGH);
								}
							}
						}
						
						socket.close();
						socket.disconnect();
						
					}
				}
				catch (SocketTimeoutException e) {
					// TODO: handle exception
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}

			private boolean checkIfPresent(ArrayList<Integer> tempArray, int sId) {
				Iterator listIter = tempArray.iterator();
				while(listIter.hasNext()){
					Integer item = (Integer) listIter.next();
					if(item == sId){
						return true;
					}
				}
				return false;
			}
			
			
		});
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

/**
 * 
 * @author Sumeet
 * 1) This class will help in creating hello message.
 * 2) hello message will have source along with neighbor table information
 * 3) the most information resent again and again from neighbors will allow to make 1-hop and 2-hop table. 
 *
 */
class OLSRHelloMessage implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String source;
	private ArrayList<String> neighbors = new ArrayList<String>();
	private ArrayList<Integer> neighborStatus = new ArrayList<Integer>();

	public OLSRHelloMessage(String source) {
		this.source = source;
	}

	public OLSRHelloMessage(String source, ArrayList<String> neighbours, ArrayList<Integer> neighbourStatus) {
		this.source = source;
		this.neighbors = neighbours;
		this.neighborStatus = neighbourStatus;
	}

	public void addNeighbourInformation(String neighbour, Integer neighbourStatus) {
		neighbors.add(neighbour);
		this.neighborStatus.add(neighbourStatus);
	}

	public ArrayList<String> getNeighbourIds() {
		return neighbors;
	}

	public ArrayList<Integer> getNeighbourStatus() {
		return neighborStatus;
	}

	public String getSource() {
		return source;
	}
	
	public String toString(){
		StringBuffer generate = new StringBuffer();
		Iterator iter = neighbors.iterator();
		Iterator iter1 = neighborStatus.iterator();
		generate.append("Neighbour : Status\n");
		while(iter.hasNext()) {
			generate.append(iter.next()+" : "+iter1.next()+"\n");
		}
		return "Source: "+this.source+"\n"+ generate.toString();
	}
}

/**
 * 
 * @author Sumeet
 *1) A thread which send hello messages from time to time as per Pause time.
 *2) The send data will always have latest neighbor information.
 *
 */
class OLSRHelloMessageThread extends Thread{
	
	private AtomicLong helloInterval;
	private OLSRAlgorithm olsr;
	private String source;
	
	public OLSRHelloMessageThread(OLSRAlgorithm olsr, AtomicLong timeout) {
		this.olsr = olsr;
		helloInterval = timeout;
		try {
			source = InetAddress.getLocalHost().getHostName().split("\\.")[0];
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void run() {
		try{
			while(System.currentTimeMillis() - OLSRAlgorithm.start.get() < helloInterval.get()) {
				OLSRHelloMessage helloMessage = new OLSRHelloMessage(source);
				ConcurrentHashMap<Integer, Integer> neigboursData = olsr.getNeighboursMap();
				
				for(Iterator<Map.Entry<Integer, Integer>> it = OLSRAlgorithm.neighbours.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry<Integer, Integer> entry = it.next();
				    Integer neigh = (Integer)entry.getKey();
				    Integer status = (Integer)entry.getValue();
				    //System.out.println("Inside"+neigh);
				    String nextNeighbour = Integer.toString(neigh);
				    if(status == OLSRAlgorithm.MPR_NEIGH && OLSRAlgorithm.checkMPRList(neigh)){
				    	helloMessage.addNeighbourInformation(nextNeighbour, new Integer(OLSRAlgorithm.MPR_NEIGH));
				    }
				    else{
				    	helloMessage.addNeighbourInformation(nextNeighbour, new Integer(OLSRAlgorithm.SYM_NEIGH));
				    }					
				}
								
				//System.out.println("The hello message is: "+ helloMessage);
				
				String[] nodeDetails = OLSRAlgorithm.configData.get(source).split("\\s+");
				ArrayList<String> neighbour = new ArrayList<String>();
				if(nodeDetails.length > 7) {
					for(int i=7; i< nodeDetails.length; i++){
						neighbour.add(nodeDetails[i]);
					}
				}
				Iterator neighbourIter = neighbour.iterator();
				
				DatagramSocket socket = new DatagramSocket();
				while(neighbourIter.hasNext()){
					//Flood the hello message
					String target = OLSRAlgorithm.sysConfig.get(Integer.parseInt((String)neighbourIter.next()));
					//System.out.println("Here"+target);
					if (target instanceof String) {
						InetAddress broadcastIP = InetAddress.getByName(target+".eng.auburn.edu");
						int port = Integer.parseInt(OLSRAlgorithm.configData.get(target).split("\\s+")[3])+1;
						ByteArrayOutputStream bStream = new ByteArrayOutputStream();
						ObjectOutput oo = new ObjectOutputStream(bStream); 
						oo.writeObject(helloMessage);
						oo.close();

						byte[] serializedMessage = bStream.toByteArray();
						DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, broadcastIP, port);
						socket.send(packet);
						//System.out.println("Sent data to "+ target +" that is: "+ serializedMessage);
					}
				}
				socket.close();
				socket.disconnect();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
}