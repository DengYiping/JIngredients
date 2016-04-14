package sarf.jingredients.util;

import java.util.ArrayList;

public class Overlap {


	
	public static double getOverlap(ArrayList<String> first, ArrayList<String> second, double threshold) {
		int maxUnmatch = (int)Math.floor(second.size() * (1.0-threshold));
		
		int i = 0;
		int j = 0;
		int intersection = 0;
		int unmatch = 0;
		while (i < first.size() && j < second.size()) {
			int c = first.get(i).compareTo(second.get(j));
			if (c == 0) {
				i++;
				j++;
				intersection++;
			} else if (c < 0) {
				i++;
			} else {
				j++;
				unmatch++;
			}
			if (unmatch > maxUnmatch) {
				// terminate because there is no chance to reach the threshold
				return 0;
			}
		}
		
		return intersection * 1.0 / second.size();
	}

	public static double getJaccard(ArrayList<String> first, ArrayList<String> second) {
		int i = 0;
		int j = 0;
		int intersection = 0;
		while (i < first.size() && j < second.size()) {
			int c = first.get(i).compareTo(second.get(j));
			if (c == 0) {
				i++;
				j++;
				intersection++;
			} else if (c < 0) {
				i++;
			} else {
				j++;
			}
		}
		return intersection * 1.0 / (first.size() + second.size() - intersection);
	}

}
