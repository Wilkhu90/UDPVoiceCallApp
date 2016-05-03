package edu.auburn.UDPCallAPP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;

public class MPRSelection {
	public static CopyOnWriteArrayList<Integer> mpr;
	
	public void mpr_select(ConcurrentHashMap<Integer, ArrayList<Integer>> two_hop_nghb) {
		mpr = new CopyOnWriteArrayList<Integer>();
		HashSet<Integer> interm;
		HashSet<Integer> result;
		HashMap<Integer, ArrayList<Integer>> candidt;
		boolean stat = true;
		int choice = 0;
		
		interm = new HashSet<Integer>();
		result = new HashSet<Integer>();
		candidt = new HashMap<Integer, ArrayList<Integer>>();
			
		Set<Entry<Integer, ArrayList<Integer>>> set1 = two_hop_nghb.entrySet();
		Iterator<Entry<Integer, ArrayList<Integer>>> iterator1 = set1.iterator();
		while (iterator1.hasNext()) {
			int k = iterator1.next().getKey();
			interm.add(k);
			
			ArrayList<Integer> temp = two_hop_nghb.get(k);
			for (int i = 0; i < temp.size(); ++i) {
				int m = temp.get(i);

				if (!candidt.containsKey(m))	{
					candidt.put(m, new ArrayList<Integer>());
				}

				candidt.get(m).add(k);
			}
		}
		
		while (stat) {
			int max_diff = 0;
			
			for (int i : candidt.keySet()) {
				int diff = 0;
				ArrayList<Integer> temp = candidt.get(i);
				
				for (int j = 0; j < temp.size(); ++j) {
					if (!result.contains(temp.get(j))) {
						++diff;
					}
				}
				
				if (diff > max_diff) {
					max_diff = diff;
					choice = i;
				}
			}
			
			ArrayList<Integer> temp = candidt.get(choice);
			for (int j = 0; j < temp.size(); ++j) {
				result.add(temp.get(j));
			}
			
			mpr.add(choice);
			
			stat = false;
			Iterator<Integer> iterator3 = interm.iterator();
			while (iterator3.hasNext()) {
				if (!result.contains(iterator3.next())) {
					stat = true;
					break;
				}
			}
		}
	}
}
