package sarf.jingredients.experiment;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import sarf.jingredients.Analysis;
import sarf.jingredients.Database;
import sarf.jingredients.Report;
import sarf.jingredients.Analysis.AnalysisType;
import sarf.jingredients.model.Component;
import sarf.jingredients.model.ComponentEntry;
import sarf.jingredients.model.Signature;
import sarf.jingredients.util.ComponentUtil;
import sarf.jingredients.util.Concurrent;

public class AnalyzeTestData {

	public static File DataDir;
	public static File OutputDir;
	private static String[] datafiles;

	

	public static void main(String[] args) {
		if (args.length != 5) {
			System.out.println("AnalyzeTestData data-dir output-dir dbfiles type(REGULAR|BERT) sig(SIG_CODE|SIG_FILE|SIG_BERT) num-threads");
			return;
		}
		DataDir = new File(args[0]);
		OutputDir = new File(args[1]);
		datafiles = args[2].split(",");
		final AnalysisType analysisOption = args[3].equalsIgnoreCase("BERT") ? AnalysisType.BERT : AnalysisType.REGULAR; 
		int comparisonSig;
		if (args[4].equalsIgnoreCase("SIG_BERT")) {
			comparisonSig = ComponentEntry.SIG_BERTILLONAGE;
		} else if (args[4].equalsIgnoreCase("SIG_FILE")) {
			comparisonSig = ComponentEntry.SIG_FILE;
		} else {
			comparisonSig = ComponentEntry.SIG_CODE;
		}
		int threads = Integer.parseInt(args[5]);
		
		
		try {
			long t = System.currentTimeMillis();
			final Database db = new Database(datafiles, comparisonSig);
			
			File[] files = DataDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("testdata-") && name.endsWith("0.zip");
				}
			});
			
			if (!OutputDir.exists()) {
				OutputDir.mkdirs();
			}
			
			System.out.print("Database loaded: ");
			System.out.println(System.currentTimeMillis() - t);
			
			Concurrent c = new Concurrent(threads);
			
			
			for (int i=0; i<files.length; i++) {
				final File targetFile = files[i];
				
				c.execute(new Runnable() {
					public void run() {
						File resultFile = new File(OutputDir, targetFile.getName().replace(".zip", "-summary.txt"));
						if (resultFile.exists() && resultFile.length() > 0) return;
						
						long t = System.currentTimeMillis();
						Component target = Component.load(targetFile, true);
						if (target != null) {
							ArrayList<Report> result = Analysis.execute(db, target, analysisOption, false);
							long timeEllapsed = System.currentTimeMillis() - t;
							
							try {
								HashMap<String, ActualData> answer = loadAnswer(targetFile);
								String accuracylog = computeAccuracy(targetFile.getName(), result, answer, timeEllapsed);
								PrintWriter w = new PrintWriter(resultFile);
								w.println(accuracylog);
								w.close();
							} catch (IOException e) {
								System.out.println("Failed to load answer file for " + targetFile.getAbsolutePath());
							}
						} else {
							System.out.println("Failed to load " + targetFile.getAbsolutePath());
						}
						System.out.print(".");
					}
				});
			}
			
			c.waitComplete();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static String computeAccuracy(String filename, ArrayList<Report> result, HashMap<String, ActualData> answer, long time) {
		int tp = 0;
		int fp = 0;
		int variants = 0;
		ArrayList<String> detailReport = new ArrayList<String>();
		ArrayList<String> found = new ArrayList<>();
		for (Report r: result) {
			boolean hit = false;
			for (String libname: r.getLibNames()) {
				ActualData d = answer.get(libname);
				if (d != null) {
					hit = true;
					found.add(libname);
					tp++;
					break;
				}
			}
			variants += r.getLibNames().size();
			if (!hit) {
				fp++;
			}
			
			String code = hit ? "TP" : "FP"; 
			detailReport.add(Signature.concat(r.getClassCount() + "," + code + "," + r.getLibNames().size() + ",", r.getLibNames(), ";") + r.getMatchedClassString());
		}
		
		HashSet<String> variantFilenames = new HashSet<String>();
		HashSet<String> variantProjects = new HashSet<String>(); 
		for (ActualData a: answer.values()) {
			variantFilenames.addAll(a.filenames);
			variantProjects.addAll(a.projects);
		}
		int tv = 0;
		int fv = 0;
		for (Report r: result) {
			for (String libname: r.getLibNames()) {
				if (variantFilenames.contains(libname)) tv++;
				else fv++;
			}
		}
		
		
		try {
			PrintWriter w = new PrintWriter(new File(OutputDir, filename.replace(".zip", "-result.txt")));
			w.println("CLASS,JUDGE,VAR,LIBS,CLASSNAMES");
			for (String d: detailReport) {
				w.println(d);
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		int fn = answer.keySet().size() - found.size();
		return filename + "," + tp + "," + fp + "," + fn + "," + variants + "," + tv + "," + fv + "," + time;
	}

	private static HashMap<String, ActualData> loadAnswer(File targetZip) throws IOException {
		HashMap<String, ActualData> answers = new HashMap<>();
		File resultFile = new File(targetZip.getAbsolutePath().replace(".zip", ".txt"));
		LineNumberReader reader = new LineNumberReader(new FileReader(resultFile));
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			ActualData data = new ActualData(line);
			answers.put(data.filename, data);
		}
		reader.close();
		return answers;
	}
	
	private static class ActualData {

		private String filename;
		private HashSet<String> filenames;
		private HashSet<String> projects;
		
		public ActualData(String line) { 
			String[] tokens = line.split(",");
			assert tokens.length == 6;
			filename = tokens[0];
			
			filenames = new HashSet<>();
			projects = new HashSet<>();
			for (String s: tokens[5].split(";")) {
				if (s.length() > 0) {
					filenames.add(s);
					projects.add(ComponentUtil.getProjectPath(s));
				}
			}
		}
		
	}

}
