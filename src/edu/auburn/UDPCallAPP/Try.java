package edu.auburn.UDPCallAPP;

import java.io.IOException;
import java.util.Arrays;

public class Try {
	static double distance;
	//Trying drop rate function
	public static int getDropRate(int x1, int y1, int x2, int y2){
		double x = Math.pow(x1 - x2, 2);
		double y = Math.pow(y1 - y2, 2);
		distance = Math.sqrt(x + y);
		if(distance <= 100){
			double numeratorFn = Math.pow(4*Math.PI*distance, 2);
			double denominatorFn = Math.pow(4*Math.PI, 2);
			double solution = ((numeratorFn/denominatorFn)/100);
			return (int)(distance < 10 ? 0 : Math.round(Math.abs(1-solution)));
		}
		return 100;
	}
	
	public static void main(String[] args) throws IOException{
		
		byte[] buffer = new byte[28];
		int sequence = 1;
		int[] info = {13,12,16};
        
		while(sequence < 50){
			buffer[0] = (byte) info[0];
			buffer[1] = (byte) info[1];
			buffer[2] = (byte) info[2];
			String a = Integer.toString(sequence);
			System.arraycopy(a.getBytes(), 0, buffer, 3, a.getBytes().length);
			for(byte b:buffer){
				System.out.println(b);
			}
			System.out.println();
			byte[] buff = Arrays.copyOfRange(buffer, 3, buffer.length);
			System.out.println(new String(buff));
			String number = new String(buff);
			System.out.println(number);
			int in = Integer.parseInt(number.trim());
			System.out.println(in);
			sequence++;
		}
		//Trying drop rate
		System.out.println("The packet drop rate:");
		System.out.println("At 5 meters apart: "+getDropRate(0, 100, 5, 100));
		System.out.println("At 10 meters apart: "+getDropRate(0, 100, 10, 100));
		System.out.println("At 20 meters apart: "+getDropRate(0, 100, 20, 100));
		System.out.println("At 30 meters apart: "+getDropRate(0, 100, 30, 100));
		System.out.println("At 40 meters apart: "+getDropRate(0, 100, 40, 100));
		System.out.println("At 50 meters apart: "+getDropRate(0, 100, 50, 100));
		System.out.println("At 60 meters apart: "+getDropRate(0, 100, 60, 100));
		System.out.println("At 70 meters apart: "+getDropRate(0, 100, 70, 100));
		System.out.println("At 80 meters apart: "+getDropRate(0, 100, 80, 100));
		System.out.println("At 90 meters apart: "+getDropRate(0, 100, 90, 100));
		System.out.println("At 99 meters apart: "+getDropRate(0, 100, 99, 100));
		int a = getDropRate(0, 100, 200, 100);
		System.out.println("At "+ distance +" meters apart: "+a);
	}
}
