package sarf.jingredients;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

public class Report {


	private ArrayList<String> libnames;
	private int classCount;
	private TIntArrayList libsize;
	private ArrayList<String> matchedClassNames;

	
	public Report(ArrayList<String> libnames, int fileCount, TIntArrayList libSize, ArrayList<String> additional) {
		this.libnames = libnames;
		this.classCount = fileCount;
		this.libsize = libSize;
		this.matchedClassNames = additional;
	}
	
	
	public int getClassCount() {
		return classCount;
	}
	
	
	public ArrayList<String> getLibNames() {
		return libnames;
	}
	
	
	public ArrayList<String> getMatchedClasses() {
		return matchedClassNames;
	}
	
	
	public String getMatchedClassString() {
		if (matchedClassNames != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(",");
			for (int i=0; i<matchedClassNames.size(); ++i) {
				if (i>0) builder.append(";");
				builder.append(matchedClassNames.get(i));
			}
			return builder.toString();
		} else {
			return "";
		}
	}
	
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(classCount);
		builder.append(",");
		builder.append(libnames.size());
		builder.append(",");
		for (int i=0; i<libnames.size(); ++i) {
			if (i>0) builder.append(";");
			builder.append(libnames.get(i));
			builder.append("[");
			builder.append(libsize.get(i));
			builder.append("]");
		}
		if (matchedClassNames != null) {
			builder.append(",");
			for (int i=0; i<matchedClassNames.size(); ++i) {
				if (i>0) builder.append(";");
				builder.append(matchedClassNames.get(i));
			}
		}
		return builder.toString();
	}
		

}
