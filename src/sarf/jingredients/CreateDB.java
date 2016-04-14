package sarf.jingredients;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import sarf.jingredients.hash.ClassHash;
import sarf.jingredients.hash.ClassHashSet;
import sarf.jingredients.model.Component;
import sarf.jingredients.util.Concurrent;

public class CreateDB {
	
	public static String FileList = ".txt";
	public static String PackageSetList = ".packages.bin";
	public static String[] SignatureTypeName = { ".cname.bin", ".code.bin", ".file.bin", ".bert.bin" };
	

	public static void main(String[] args) {
		long t = System.currentTimeMillis();
		if (args.length < 3) {
			System.out.println("Usage: CreateDB jar-file-list db-name num-threads [-full]");
			return;
		}

		int numThreads = Integer.parseInt(args[2]);
		Concurrent concurrent = new Concurrent(numThreads);
		
		// by default, ".cname" and ".code" databases (that are necessary for JIngredients) are created but
		// skip the generation of databases for Software Bertillonage.
		final boolean[] skipHashOutput = new boolean[SignatureTypeName.length];
		skipHashOutput[2] = true;
		skipHashOutput[3] = true;
		
		if (args.length >= 4) {
			if (args[3].equalsIgnoreCase("-full")) {
				skipHashOutput[ 2 ] = false;
				skipHashOutput[ 3 ] = false;
			}
		}
			
		try {
			String outputFilename = args[1];
			
			final PrintWriter w = new PrintWriter(new File(outputFilename + FileList));
			LineNumberReader reader = new LineNumberReader(new FileReader(new File(args[0])));

			final ObjectOutputStream packageInfoStream = new ObjectOutputStream(new FileOutputStream(new File(outputFilename + PackageSetList)));

			final ObjectOutputStream[] out = new ObjectOutputStream[SignatureTypeName.length];
			for (int i=0; i<out.length; ++i) {
				if (!skipHashOutput[i]) {
					FileOutputStream f = new FileOutputStream(new File(outputFilename + SignatureTypeName[i]));
					GZIPOutputStream gzipped = new GZIPOutputStream(f);
					out[i] = new ObjectOutputStream(gzipped);
				}
			}

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				final String filename = line;
				concurrent.execute(new Runnable() {
					@Override
					public void run() {
						Component c = Component.load(new File(filename), false);
						if (c != null) {
							@SuppressWarnings("unchecked")
							ArrayList<ClassHash>[] sets = new ArrayList[SignatureTypeName.length];
							for (int i=0; i<SignatureTypeName.length; ++i) {
								if (!skipHashOutput[i]) {
									sets[i] = c.createHashList(i);
								}
							}
							
							ClassHash phash = c.getPackageSetHash();
							synchronized (w) {
								w.println(filename);
								try {
									phash.writeTo(packageInfoStream);
									for (int i=0; i<SignatureTypeName.length; ++i) {
										if (!skipHashOutput[i]) {
											ClassHashSet.writeToStream(out[i], sets[i]);
										}
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							System.out.print(".");
						}
					}
				});
			}
			concurrent.waitComplete();
			for (int i=0; i<SignatureTypeName.length; ++i) {
				if (!skipHashOutput[i]) {
					out[i].close();
				}
			}
			reader.close();
			packageInfoStream.close();
			w.close();
			System.out.println();
			
			System.out.println(System.currentTimeMillis() - t);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	


}
