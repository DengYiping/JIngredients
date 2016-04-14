package sarf.jingredients;

import gnu.trove.list.array.TIntArrayList;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import sarf.jingredients.hash.ClassHash;
import sarf.jingredients.hash.ClassHashSet;
import sarf.jingredients.model.Component;
import sarf.jingredients.model.ComponentEntry;
import sarf.jingredients.util.ComponentUtil;
import sarf.jingredients.util.StringList;

public class Database {

	private static boolean DEBUG = false;

	private ArrayList<ClassHashSet> codesigDataset = new ArrayList<>();
	private ArrayList<ClassHashSet> classnameDataset = new ArrayList<>();
	private ArrayList<String> filenames = new ArrayList<>();
	private ArrayList<ClassHash> packageSetList = new ArrayList<>(); 
	private int comparisonSig;
	
	
	public Database(String[] datafiles, int comparisonSig) throws IOException {
		this.comparisonSig = comparisonSig;
		for (String filename: datafiles) {
			classnameDataset.addAll(load(new File(filename + CreateDB.SignatureTypeName[ComponentEntry.SIG_CLASSNAME])));
			codesigDataset.addAll(load(new File(filename + CreateDB.SignatureTypeName[comparisonSig])));
			filenames.addAll(StringList.loadFromFile(new File(filename + CreateDB.FileList)));
			packageSetList.addAll(loadPackageHashList(new File(filename + CreateDB.PackageSetList)));
		}

		assert filenames.size() == classnameDataset.size(): "Inconsistent file list and dataset";
		assert filenames.size() == codesigDataset.size(): "Inconsistent file list and dataset";
		assert filenames.size() == packageSetList.size(): "Inconsistent file list and package set list"; 
	}
	
	public int size() {
		return classnameDataset.size();
	}
	
	public static ArrayList<ClassHashSet> load(File file) throws IOException {
		ArrayList<ClassHashSet> dataset = new ArrayList<>();
		FileInputStream f = new FileInputStream(file);
		GZIPInputStream expanded = new GZIPInputStream(f);
		ObjectInputStream in = new ObjectInputStream(expanded);
		
		ClassHashSet set = ClassHashSet.readFromStream(in);
		while (set != null) {
			dataset.add(set);
			if (DEBUG) {
				System.out.println(set.size());
				for (ClassHash h: set) {
					System.out.println(h.toString());
				}
			}
			set = ClassHashSet.readFromStream(in);
		}
		in.close();
		return dataset;
	}
	
	public static ArrayList<ClassHash> loadPackageHashList(File file) throws IOException {
		ArrayList<ClassHash> list = new ArrayList<>();
		FileInputStream f = new FileInputStream(file);
		ObjectInputStream in = new ObjectInputStream(f);
		try {
			for (;;) {
				list.add(new ClassHash(in));
			}
		} catch (EOFException e) {
			return list;
		} catch (IOException e) {
			throw e;
		}
	}

	
	
	public ArrayList<Report> analyzeIngredients(Component target, boolean filterByPackage) {
		
		ArrayList<Report> result = new ArrayList<>();
		ArrayList<ClassHash> hasharray = target.createHashList(ComponentEntry.SIG_CLASSNAME);
		ClassHashSet nameHash = new ClassHashSet(hasharray);
		
		TargetComponent app = new TargetComponent(target, comparisonSig);
		
		ArrayList<OverlapResult> overlaps = new ArrayList<>(); 
		for (int i=0; i<filenames.size(); i++) {
			if (!filterByPackage || !app.getPackageHash().equals(packageSetList.get(i))) {
				if (nameHash.size() >= classnameDataset.get(i).size()) {
					double nameOverlap = nameHash.overlap(classnameDataset.get(i));
					OverlapResult r = new OverlapResult(nameOverlap, filenames.get(i), codesigDataset.get(i));
					if (r.overlap > 0) {
						overlaps.add(r);
					}
				}
			}
		}
		Collections.sort(overlaps);
		
		
		ArrayList<ArrayList<OverlapResult>> arrays = splitByOverlap(overlaps);
		for (ArrayList<OverlapResult> array: arrays) {
			
			ArrayList<IntersectionResult> intersections = new ArrayList<>(array.size());
			for (int i=0; i<array.size(); ++i) {
				IntersectionResult r = new IntersectionResult(app.getContent(), array.get(i));
				if (r.intersection.size() > 0) {
					intersections.add(r);
				}
			}
			Collections.sort(intersections);

			while (!intersections.isEmpty() && intersections.get(0).intersection.size() > 0) {
				
				// Select the top (and the same content components)
				IntersectionResult top = intersections.get(0);
				ArrayList<IntersectionResult> selected = new ArrayList<>(intersections.size());
				ArrayList<IntersectionResult> remaining = new ArrayList<>(intersections.size());
				selected.add(top);
				int topSize = top.intersection.size();
				assert topSize > 0;
				for (int i=1; i<intersections.size(); ++i) {
					IntersectionResult another = intersections.get(i);
					if (another.intersection.equals(top.intersection)) {
						selected.add(another);
					} else {
						remaining.add(another);
					}
				}
				assert selected.size() + remaining.size() == intersections.size(); 
				
				// Create a report
				ArrayList<String> libnames = new ArrayList<>(selected.size());
				TIntArrayList libsizes = new TIntArrayList(selected.size());
				for (IntersectionResult r: selected) {
					libnames.add(r.libname);
					libsizes.add(r.origin.libcodesig.size());
					//orgIntersections.add(Integer.toString(r.originalIntersection));
				}
				ArrayList<String> matchedNames = app.getNames(top.intersection);
				result.add(new Report(libnames, topSize, libsizes, matchedNames));

				// Remove the selected classes from the remaining lists
				app.getContent().removeAll(top.intersection);
				for (IntersectionResult r: remaining) {
					r.intersection.removeAll(top.intersection);
				}
				Collections.sort(remaining);
				intersections = remaining;
				
				//int originalSize = app.size(); 
				//System.out.println(" -> " + app.size());
			}
		}
		return result;
	}

	
	public ArrayList<Report> analyzeBertilonage(Component target, boolean filterByPackage) {
		
		ArrayList<Report> result = new ArrayList<>();
		TargetComponent app = new TargetComponent(target, comparisonSig);
		
		LinkedList<JaccardResult> jaccard = new LinkedList<>(); 
		for (int i=0; i<filenames.size(); i++) {
			if (!filterByPackage || !app.getPackageHash().equals(packageSetList.get(i))) {
				JaccardResult r = new JaccardResult(app.getContent(), filenames.get(i), codesigDataset.get(i));
				jaccard.add(r);
			}
		}
		Collections.sort(jaccard);
		
		HashSet<String> selectedProjectNames = new HashSet<>();
		
		while (!jaccard.isEmpty() && jaccard.get(0).intersection.size() > 0) {
				
			// Select the top (and the same content components)
			JaccardResult top = jaccard.getFirst();
			if (selectedProjectNames.contains(top.projectname)) {
				jaccard.removeFirst();
				continue;
			}
			
			ArrayList<JaccardResult> selected = new ArrayList<>();
			int topSize = top.intersection.size();
			assert topSize > 0;
			for (Iterator<JaccardResult> it = jaccard.iterator(); it.hasNext(); ) {
				JaccardResult j = it.next();
				if (selectedProjectNames.contains(j.projectname)) {
					it.remove();
				} else if (top.intersection.equals(j.intersection)) {
					selected.add(j);
					it.remove();
				}
			}
			assert selected.size() > 0;
				
			// Create a report
			ArrayList<String> libnames = new ArrayList<>();
			TIntArrayList libsize = new TIntArrayList();
			for (JaccardResult r: selected) {
				libnames.add(r.libname);
				libsize.add(r.libsize);
				selectedProjectNames.add(r.projectname);
			}
			ArrayList<String> matchedNames = app.getNames(top.intersection);
			result.add(new Report(libnames, topSize, libsize, matchedNames));
		}
		
		return result;
	}
	
	


	
	private static class IntersectionResult implements Comparable<IntersectionResult> {

		private String libname;
		private ClassHashSet intersection; 
		private OverlapResult origin;
		

		IntersectionResult(ClassHashSet app, OverlapResult overlap) {
			this.libname = overlap.libname;
			this.origin = overlap;
			this.intersection = app.getIntersectionSet(overlap.libcodesig);
		}
		
		@Override
		public int compareTo(IntersectionResult another) {
			int c = -Integer.compare(this.intersection.size(), another.intersection.size());
			if (c == 0) {
				return this.libname.compareTo(another.libname);
			} else {
				return c;
			}
		}
		
	}
	

	private static ArrayList<ArrayList<OverlapResult>> splitByOverlap(ArrayList<OverlapResult> overlaps) {
		ArrayList<ArrayList<OverlapResult>> splitArrays = new ArrayList<>();
		if (overlaps.size() == 0) return splitArrays;
		
		ArrayList<OverlapResult> items = new ArrayList<>();
		items.add(overlaps.get(0));
		for (int i=1; i<overlaps.size(); i++) {
			OverlapResult current = overlaps.get(i);
			if (current.overlap == items.get(0).overlap) {
				items.add(current);
			} else {
				splitArrays.add(items);
				items = new ArrayList<>();
				items.add(current);
			}
		}
		if (items.get(0).overlap > 0) {
			splitArrays.add(items);
		}
		
		return splitArrays;
	}

	
	private static class OverlapResult implements Comparable<OverlapResult> {

		private String libname;
		private double overlap;
		private ClassHashSet libcodesig;
		
		OverlapResult(double nameOverlap, String libname, ClassHashSet libcodesig) {
			this.libname = libname;
			this.overlap = nameOverlap;
			this.libcodesig = libcodesig;
		}
		
		@Override
		public int compareTo(OverlapResult another) {
			int c = -Double.compare(this.overlap, another.overlap);
			if (c == 0) {
				return this.libname.compareTo(another.libname);
			} else {
				return c;
			}
		}
		
		public String toString() {
			return libname + ";" + Double.toString(overlap); 
		}
		
	}
	
	private static class JaccardResult implements Comparable<JaccardResult> {

		private String libname;
		private String projectname;
		private ClassHashSet intersection;
		private int libsize;
		private double jaccard;
		
		JaccardResult(ClassHashSet app, String libname, ClassHashSet signature) {
			this.libname = libname;
			this.projectname = ComponentUtil.getProjectPath(libname);
			this.libsize = signature.size();
			this.intersection = app.getIntersectionSet(signature);
			this.jaccard = (intersection.size() * 1.0) / (app.size() + signature.size() - intersection.size());
		}
		
		@Override
		public int compareTo(JaccardResult another) {
			int c = -Double.compare(this.jaccard, another.jaccard);
			if (c == 0) {
				return this.libname.compareTo(another.libname);
			} else {
				return c;
			}
		}
		
		public String toString() {
			return libname + ";" + Double.toString(jaccard); 
		}
		
	}
	

}
