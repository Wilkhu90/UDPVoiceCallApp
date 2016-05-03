package edu.auburn.UDPCallAPP;

import java.util.ArrayList;

public class NodeOperation {
	static int rangeMin = 0;
	static int rangeMax = 300;
	static int sizeMatrix = 16;
	static int acceptableRange = 100;

	public static int randomMove() {
		int random = (int) (rangeMin + Math.random() * (rangeMax - rangeMin));
		return random;
	}

	public static ArrayList<ArrayList<Integer>>  linkUpdate (ArrayList<GetNode> nodes) {
		boolean[][] linkMatrix = distanceCalculator(nodes);
		ArrayList<ArrayList<Integer>> listOfLinks = new ArrayList<ArrayList<Integer>>();
		
		for (int i = 0; i < sizeMatrix; i++) {
			ArrayList<Integer> links = new ArrayList<Integer>();
			for (int j = 0; j < sizeMatrix; j++) {
				if (linkMatrix[i][j]) {
					links.add(j+1);
				}
			}
			listOfLinks.add(links);
		}
		return listOfLinks;
	}

	public static boolean[][] distanceCalculator(ArrayList<GetNode> nodes) {

		boolean[][] linkMatrix = new boolean[sizeMatrix][sizeMatrix];

		for (int i = 0; i < sizeMatrix; i++) {
			for (int j = i + 1; j < sizeMatrix; j++) {

				double x = Math.pow(nodes.get(i).getX() - nodes.get(j).getX(), 2);
				double y = Math.pow(nodes.get(i).getY() - nodes.get(j).getY(), 2);
				double result = Math.sqrt(x + y);
				if (result <= acceptableRange) {
					linkMatrix[i][j] = true;
					linkMatrix[j][i] = true;
				} else
					linkMatrix[i][j] = false;
			}

		}
		for (int i = 0; i < sizeMatrix; i++) {
			for (int j = 0; j < sizeMatrix; j++) {
				System.out.print(linkMatrix[i][j] + " ");
			}
			System.out.println();
		}
		return linkMatrix;
	}
}
