package sarf.jingredients;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import sarf.jingredients.hash.ClassHash;
import sarf.jingredients.hash.ClassHashSet;
import sarf.jingredients.model.Component;
import sarf.jingredients.model.ComponentEntry;

public class TargetComponent {
		
	private ClassHashSet hash;
	private HashMap<ClassHash, String> hashToName;
	private HashMap<ClassHash, ArrayList<String>> conflictNames;
	private ClassHash packageHash;
		
	public TargetComponent(Component target, int comparisonSig) {
		ArrayList<ClassHash> hashset = new ArrayList<ClassHash>();
		hashToName = new HashMap<>();
		conflictNames = new HashMap<>();
		for (ComponentEntry e: target.getEntries()) {
			String className = e.getRawName();
			ClassHash hash = e.getClassHash(comparisonSig);
			String registeredName = hashToName.get(hash); 
			if (registeredName == null) {
				hashToName.put(hash, className);
			} else {
				System.err.println("A hash for " + className + " conflicts with " + registeredName + ".");
				ArrayList<String> names = conflictNames.get(hash);
				if (names == null) {
					names = new ArrayList<String>();
					conflictNames.put(hash, names);
				}
				names.add(className);
			}
			hashset.add(hash);
		}
		Collections.sort(hashset);
		hash = new ClassHashSet(hashset);
		packageHash = target.getPackageSetHash();
	}
	
	/**
	 * Translate a class hash set to its original name. 
	 * A hash may be translated into multiple names, if  
	 * two or more classes have the same hash. 
	 * @return 
	 */
	public ArrayList<String> getNames(ClassHashSet intersection) {
		ArrayList<String> matchedNames = new ArrayList<>(intersection.size());
		for (ClassHash h: intersection) {
			String cName = hashToName.get(h);
			assert cName != null: "A matched hash is not found in the target";
			if (cName != null) {
				matchedNames.add(cName);
				ArrayList<String> otherNames = conflictNames.get(cName);
				if (otherNames != null) {
					matchedNames.addAll(otherNames);
				}
			}
		}
		return matchedNames;
	}
	
	public ClassHash getPackageHash() {
		return packageHash;
	}
	
	public ClassHashSet getContent() {
		return hash;
	}

}
