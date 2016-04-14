package sarf.jingredients.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

public class StringList {
	
	public static ArrayList<String> loadFromFile(File f) throws IOException {
		ArrayList<String> content = new ArrayList<String>();
		LineNumberReader reader = new LineNumberReader(new FileReader(f));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			content.add(line);
		}
		reader.close();
		return content;
	}

}
