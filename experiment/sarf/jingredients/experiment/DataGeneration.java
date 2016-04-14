package sarf.jingredients.experiment;

import gnu.trove.TCollections;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import sarf.jingredients.CreateDB;
import sarf.jingredients.Database;
import sarf.jingredients.hash.ClassHash;
import sarf.jingredients.hash.ClassHashSet;
import sarf.jingredients.model.ComponentEntry;
import sarf.jingredients.model.Signature;
import sarf.jingredients.util.ComponentUtil;
import sarf.jingredients.util.Concurrent;
import sarf.jingredients.util.StringList;

public class DataGeneration {

	// Parameters to specify the location of the sourcerer dataset  
	private static final String DBFileName = "sourcerer";
	private static final File FileNameList = new File(DBFileName + CreateDB.FileList);
	private static final File SignatureFile = new File(DBFileName + CreateDB.SignatureTypeName[ComponentEntry.SIG_FILE]);

	// The data members shared by multiple threads
	private static ArrayList<String> filenames;
	private static ArrayList<ClassHashSet> hashList;
	private static boolean loaded;

	// Parameters to control the number of generated files in a golden dataset 
	private static final int NUM_FILES_PER_SIZE = 100;
	private static final int MIN_SIZE = 10;
	private static final int MAX_SIZE = 1000;
	private static final int STEP_SIZE = 10;

	// Parameter to control the number of threads to create a golden dataset
	private static final int NUM_THREADS = 12;

	static Random rand = new Random();
	static final int BUFFER = 512 * 1024 * 1024;


	static {
		try {
			filenames = StringList.loadFromFile(FileNameList);
			hashList = Database.load(SignatureFile);
			assert hashList.size() == filenames.size();
			loaded = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * A jar list must be created by CreateValidJarList in order to exclude corrupted files from the list.
	 * @param args 
	 */
	public static void main(String[] args) {
		if (!loaded) return;
		if (args.length < 2) {
			System.out.println("DataGeneration jar-list output-dir");
			return;
		}
		File inputJarList = new File(args[0]);
		File outputDataDir = new File(args[1]);

		if (!outputDataDir.exists()) outputDataDir.mkdirs();
		if (!outputDataDir.exists()) { 
			System.out.println("Output DataDir (" + outputDataDir.getAbsolutePath() + ") is not available.");
			return;
		}
 		
		// Load a list of jar files from a given file.
		// Jar files are classified by their component name;
		// an element in "components" includes versions of a component.  
		final ArrayList<ArrayList<String>> components = new ArrayList<>();
		try {
			ArrayList<String> files = new ArrayList<String>();
			String componentName = null;
			LineNumberReader reader = new LineNumberReader(new FileReader(inputJarList));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				
				if (componentName == null) componentName = line;
				
				if (ComponentUtil.isSameComponent(componentName, line)) {
					files.add(line);
				} else {
					components.add(files);
					files = new ArrayList<String>();
					files.add(line);
					componentName = line;
				}
				
			}
			reader.close();
			components.add(files);
			System.out.println(components.size() + " components");
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// Create a dataset.
		Concurrent c = new Concurrent(NUM_THREADS);
		final TIntSet syncErrorFiles = TCollections.synchronizedSet(new TIntHashSet());
		for (int N=MIN_SIZE; N<=MAX_SIZE; N+=STEP_SIZE) {
			for (int i=0; i<NUM_FILES_PER_SIZE; i++) {
				// Execute a task for each parameter configuration  
				final String filename = String.format("testdata-%05d-%03d", N, i);
				final int NUM = N;
				c.execute(new Runnable() {
					@Override
					public void run() {
						createMixJar(outputDataDir, NUM, filename, components, syncErrorFiles);
					}
				});
			}
		}
		c.waitComplete();
	}
	

	
	
	/**
	 * Create a jar file by mixing N jar files.
	 * The jar files must be different components.
	 * This method also creates a text file recording the content of a created jar file.
	 * Each line in the text file includes a original jar file name, the number of copied files from the jar file,
	 * the number of skipped files, the number of all files, 
	 * the number of jar files that could provide the same class files, and the jar file names. 
	 * 
	 * @param N specifies the number of jar files.
	 * @param filename specifies a jar file name to be created.
	 * @param components is a list of jar files for each component.
	 * @param errorFiles record a set of jar files that could not be loaded; 
	 * they are excluded from the data generation process.  
	 */
	public static void createMixJar(File outputDataDir, int N, String filename, ArrayList<ArrayList<String>> components, TIntSet errorFiles) {
		
		try {
			boolean success;
			ByteArrayOutputStream bytearray = new ByteArrayOutputStream(BUFFER);
			ArrayList<String> content = new ArrayList<String>();
			do {
				success = true;
				content.clear();
				bytearray.reset();
				ZipOutputStream zip = new ZipOutputStream(bytearray);
				TIntHashSet selected = new TIntHashSet();
				HashSet<String> copiedFiles = new HashSet<String>();
				
				ArrayList<Integer> indices = new ArrayList<>(components.size());
				for (int i=0; i<components.size(); i++) indices.add(i);
				Collections.shuffle(indices);
				int idx = 0;
	
				int count = 0;
				while (count < N) {
					// Randomly select an index in a package list 
					int index = indices.get(idx);
					idx++;
					while (errorFiles.contains(index)) {
						index = indices.get(idx);
						idx++;
					}
					selected.add(index);
					ArrayList<String> files = components.get(index);
					
					// Randomly select a file in the files
					String pickedJar = files.get(rand.nextInt(files.size()));
					
					String copyReport = copyContent(zip, copiedFiles, pickedJar);
					
					if (copyReport != null && copyReport.length() > 0) {
						content.add(copyReport);
						count++;
					} else if (copyReport == null) {
						errorFiles.add(index); // record error to avoid a retry using the same file
						success = false;
						break;
					}
				}
				
				zip.close();
			} while (!success);

			File datafile = new File(outputDataDir, filename + ".zip");
			FileOutputStream out = new FileOutputStream(datafile);
			bytearray.writeTo(out);
			out.close();

			PrintWriter w = new PrintWriter(new File(outputDataDir, filename + ".txt"));
			for (String c: content) {
				w.println(c);
			}
			w.close();
			
		} catch (IOException e) {
			System.err.println("Error during creation of " + filename);
			e.printStackTrace();
		}
	}
	

	/**
	 * Copy the contet
	 * @param zip
	 * @param copiedFiles record file names that have been copied to a zip file in order to avoid overwriting.
	 * @param jarfilename
	 * @return the number of copied class files 
	 */
	public static String copyContent(ZipOutputStream zip, HashSet<String> copiedFiles, String jarfilename) {
		ArrayList<ClassHash> classhash = new ArrayList<>();
	    byte[] buf = new byte[65536];
		int count = 0;
		int skipped = 0;
		try {
		    MessageDigest digest = MessageDigest.getInstance("SHA-1");
			ZipFile file = new ZipFile(jarfilename);
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (!copiedFiles.contains(entryName) && ComponentUtil.isClassFile(entryName)) {
					ZipEntry newEntry = new ZipEntry(entryName);
					zip.putNextEntry(newEntry);
					digest.reset();
					InputStream fstream = file.getInputStream(entry);
					DigestInputStream stream = new DigestInputStream(fstream, digest);
				    int i = 0;
				    while ((i = stream.read(buf)) != -1) {
				        zip.write(buf, 0, i);
				    }
				    zip.closeEntry();
					copiedFiles.add(entryName);
					count++;
					ClassHash hash = new ClassHash(digest.digest());
					classhash.add(hash);
				} else if (ComponentUtil.isClassFile(entryName)) {
					skipped++;
				}
			}
			file.close();
			if (count > 0) {
				Collections.sort(classhash);
				ClassHashSet set = new ClassHashSet(classhash);
				ArrayList<String> variants = identifyAllComponentsIncludingFiles(set);
				return jarfilename + "," + count + "," + skipped + "," + (count+skipped) + "," + variants.size() + "," + Signature.concat("", variants, ";");
			} else {
				return "";
			}
		} catch (IOException|NoSuchAlgorithmException e) {
			System.err.println("Error during copy " + jarfilename);
			e.printStackTrace();
			return null;
		}
		
	}
	
	
	/**
	 * Identify all the components that could provide classes in a given set.
	 * This is corresponding to "V(t)" in the JIngredients paper. 
	 */
	public static ArrayList<String> identifyAllComponentsIncludingFiles(ClassHashSet hashset) {
		assert loaded: "Files must be loaded.";
		ArrayList<String> result = new ArrayList<String>();
		for (int i=0; i<hashList.size(); ++i) {
			if (hashList.get(i).containsAll(hashset)) {
				result.add(filenames.get(i));
			}
		}
		assert result.size() > 0;
		return result;
	}

}
