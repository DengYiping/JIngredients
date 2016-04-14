package sarf.jingredients;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import sarf.jingredients.model.Component;
import sarf.jingredients.model.ComponentEntry;
import sarf.jingredients.util.Timer;

public class Analysis {

	private static final String USAGE = "Analysis dbfiles output-dest analysis-mode(DEFAULT|INSIDE) target-jar [2nd-target-jar ...]";
	public static enum AnalysisType { REGULAR, BERT };
	
	public static void main(String[] args) {
		Timer t = new Timer();
		
		if (args.length < 4) {
			System.out.println(USAGE);
			System.out.println("Multiple database files are listed by \",\"");
			return;
		}
		
		String outputFile = args[1];
		if (outputFile.endsWith(".jar") || outputFile.endsWith(".zip")) {
			System.out.println(USAGE);
			System.out.println("The second argument must be an output file.");
			return;
		}
		
		boolean filterByPackage = args[2].equalsIgnoreCase("INSIDE");
		final int FILE_START_INDEX = 3;
		
		try {
			String[] datafiles = args[0].split(",");
			Database db = new Database(datafiles, ComponentEntry.SIG_CODE);
			System.out.println(db.size() + " jar files in the database");
			t.printTime("Load Database: ");
			printMemory();
			
			for (int i=FILE_START_INDEX; i<args.length; ++i) {
				File targetFile = new File(args[i]);
				Component target = Component.load(targetFile, true);
				t.printTime("Load Target: ");
				printMemory();

				if (target != null) {
					String index = (args.length > FILE_START_INDEX+1) ? "-" + Integer.toString(i-FILE_START_INDEX+1) : "";
					FileBasedOutput filebased = new FileBasedOutput();
					ArrayList<Report> result = execute(db, target, AnalysisType.REGULAR, filterByPackage);
					PrintWriter w = new PrintWriter(new File(outputFile + index + "-rank.txt"));
					for (Report r: result) {
						w.println(r.toString());
						filebased.add(r);
					}
					w.close();
					filebased.registerOtherRawNames(target.getRawNames());
					filebased.outputFile(new File(outputFile + index + "-file.txt"));
				} else {
					System.out.println("Failed to load " + targetFile.getAbsolutePath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		t.printTime("Analysis: ");
	}
	
	public static class FileBasedOutput {
		
		private TreeMap<String, ArrayList<ArrayList<String>>> fileToLibs;

		public FileBasedOutput() {
			fileToLibs = new TreeMap<String, ArrayList<ArrayList<String>>>();
		}
		
		public void add(Report output) {
			ArrayList<String> classnames = output.getMatchedClasses();
			if (classnames != null) {
				for (String c: classnames) {
					ArrayList<ArrayList<String>> libsList = fileToLibs.get(c);
					if (libsList == null) {
						libsList = new ArrayList<ArrayList<String>>();
						fileToLibs.put(c, libsList);
					}
					libsList.add(output.getLibNames());
				}
			}
		}
		
		public void registerOtherRawNames(Set<String> files) {
			for (String f: files) {
				if (!fileToLibs.containsKey(f)) fileToLibs.put(f, null);
			}
		}
		
		public void outputFile(File f) {
			try {
				PrintWriter w = new PrintWriter(f);
				for (String file: fileToLibs.keySet()) {
					ArrayList<ArrayList<String>> libsList = fileToLibs.get(file);
					w.print(file);
					w.print("\t");
					if (libsList != null) {
						for (ArrayList<String> libs: libsList) {
							for (String lib: libs) {
								w.print(lib + ";");
							}
							w.print("\t");
						}
					}
					w.println();
				}
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void printMemory() {
		System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() + " bytes used");
	}
	
	public static ArrayList<Report> execute(Database db, Component target, AnalysisType opt, boolean filterByPackage) {
		switch (opt) {
		case REGULAR:
			return db.analyzeIngredients(target, filterByPackage);
		
		case BERT:
			return db.analyzeBertilonage(target, filterByPackage);
			
		default:
			assert false: "Unreachable";
			return new ArrayList<>();
		}
	}

	
}
