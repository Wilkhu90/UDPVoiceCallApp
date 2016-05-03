package edu.auburn.UDPCallAPP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

public class FileUtils {
	
	public ArrayList<GetNode> readFile(String path){
		
		ArrayList<GetNode> lists = new ArrayList<GetNode>();
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try{
			String line = "";
			
			fis = new FileInputStream(path);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			
			while( (line = br.readLine()) != null){
				String[] strs = line.split("\\s+");
				GetNode n = new GetNode();
				n.setNode(Integer.parseInt(strs[1]));
				n.setHost(strs[2]);
				n.setPort(Integer.parseInt(strs[3]));
				
				n.setX(NodeOperation.randomMove());
				n.setY(NodeOperation.randomMove());
				
				ArrayList<Integer> links = new ArrayList<Integer>();
				for(int i =0; i < strs.length-7; i++){
					links.add(Integer.parseInt(strs[7+i]));
				}
				n.setLinks(links);
				lists.add(n);
			}
			 ArrayList<ArrayList<Integer>> listOfLinks = NodeOperation.linkUpdate(lists);
			 for(int i = 0; i < listOfLinks.size(); i++){
				 System.out.println(lists.get(i));
				 lists.get(i).setLinks(listOfLinks.get(i));
				 System.out.println(lists.get(i));
			 }
		}
		catch (FileNotFoundException e) {
			System.out.println("No file");
		} catch (IOException e) {
			System.out.println("File reading failed");
		} finally {
			try {
				br.close();
				isr.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return lists;
	}

	public void WriteFile(ArrayList<GetNode> lists,String path){
		
		Iterator iterator = (lists).iterator();
	  	File file = new File(path);
        FileWriter fw = null;
        BufferedWriter writer = null;
        
        try {
            fw = new FileWriter(file);
            writer = new BufferedWriter(fw);
            while(iterator.hasNext()){
                writer.write(iterator.next().toString());
                writer.newLine();
            }
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                writer.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		
	}
}