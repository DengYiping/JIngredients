package sarf.jingredients.util;

import java.io.File;

public class ComponentUtil {


	public static boolean isSameComponent(String name1, String name2) {
		return getProjectPath(name1).equals(getProjectPath(name2));
	}
	
	
	public static String getProjectPath(String filename) {
		int index = filename.lastIndexOf(File.separatorChar);
		index = filename.lastIndexOf(File.separatorChar, index-1);
		if (index >= 0) {
			return filename.substring(0, index+1);
		} else {
			return filename;
		}
	}
	
	
	public static boolean isClassFile(String filename) {
		return filename.endsWith(".class");
	}

	
}
