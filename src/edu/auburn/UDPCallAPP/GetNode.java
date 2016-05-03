package edu.auburn.UDPCallAPP;

import java.util.ArrayList;

public class GetNode {
	private int node;
	private String host;
	private int port;
	private int x;
	private int y;
	private ArrayList<Integer> links;
	public int getNode() {
		return node;                                                       
	}
	public void setNode(int node) {
		this.node = node;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public ArrayList<Integer> getLinks() {
		return links;
	}
	public void setLinks(ArrayList<Integer> links) {
		this.links = links;
	}
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Node " + node + " " + host + " " + port + " " + x + " " + y + " links");
		for(int item : links){
			str.append(" " + item);
		}
		return str.toString();
	}
}
