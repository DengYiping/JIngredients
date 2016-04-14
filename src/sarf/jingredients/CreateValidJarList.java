package sarf.jingredients;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import sarf.jingredients.model.Component;
import sarf.jingredients.util.Concurrent;

public class CreateValidJarList {

	/**
	 * Create a list of jar files in a selected dir (the first argument) 
	 * and output the list to a text file (the second argument).
	 * This method excludes corrupted jar files, jar files including 
	 * corrupted class files, and jar files without class files.
	 */
	public static void main(String[] args) {
		long t = System.currentTimeMillis();
		
		if (args.length < 3) {
			System.out.println("CreateValidJarList search-file-dir output-file-name num-threads");
			return;
		}
		File outputFile = new File(args[1]);
		ArrayList<File> files = searchJars(new File(args[0]));
		final boolean[] valid = new boolean[files.size()];
		int threads = Integer.parseInt(args[2]);
		
		System.out.println(Long.toString(System.currentTimeMillis() - t) + "ms to list " + files.size() + " files.");
		t = System.currentTimeMillis();
		
		Concurrent c = new Concurrent(threads); 
		for (int i=0; i<files.size(); ++i) {
			final File target = files.get(i);
			final int fileIndex = i;
			c.execute(new Runnable() {
				@Override
				public void run() {
					Component comp = Component.load(target, false);
					valid[fileIndex] = (comp != null);
				}
			});
		}
		c.waitComplete();
		
		try {
			PrintWriter w = new PrintWriter(outputFile);
			for (int i=0; i<files.size(); ++i) {
				if (valid[i]) {
					w.println(files.get(i).getAbsolutePath());
				}
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(Long.toString(System.currentTimeMillis() - t) + "ms to process " + files.size() + " files.");
	}
	
	public static ArrayList<File> searchJars(File root) {
		ArrayList<File> result = new ArrayList<>();
		LinkedList<File> worklist = new LinkedList<>();
		worklist.add(root);
		while (!worklist.isEmpty()) {
			File dir = worklist.removeFirst();
			File[] files = dir.listFiles();
			for (File f: files) {
				if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
					worklist.add(f);
				} else if (f.isFile() && (f.getName().endsWith(".jar") || f.getName().endsWith(".zip"))) {
					result.add(f);
				}
			}
		}
		return result;
	}

}
