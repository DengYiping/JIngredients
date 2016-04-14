package sarf.jingredients.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import sarf.jingredients.hash.ClassHash;
import sarf.jingredients.util.ComponentUtil;
import soba.core.ClassInfo;
import soba.util.files.IClassListCallback;
import soba.util.files.ZipFile;

public class Component implements IClassListCallback {

	
	private boolean error = false;
	private File jar;
	private TreeSet<String> packageNames = new TreeSet<>();
	private ArrayList<ComponentEntry> entries = new ArrayList<>();
	private HashSet<String> classfilenames = new HashSet<>();
	private HashSet<String> rawnames = new HashSet<>();
	
	public static Component load(File jar, boolean searchRecursive) {
		Component c = new Component(jar);
		ZipFile f = new ZipFile(jar);
		if (searchRecursive) f.enableRecursiveSearch();
		f.process(c);
		if (!c.error && c.entries.size() > 0) {
			return c;
		}
		else return null;
	}
	
	private Component(File f) {
		this.jar = f;
	}
	
	public File getFile() {
		return jar;
	}
	
	
	@Override
	public boolean reportError(String name, Exception e) {
		System.out.print("[ERROR] Last Entry: ");
		System.out.println(name);
		e.printStackTrace(System.out);
		error = true; // This setting removes the jar file from analysis.
		return false;
	}
	
	public static byte[] getSHA1(byte[] bytearray) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			return digest.digest(bytearray);
		} catch (NoSuchAlgorithmException e) {
			assert false;
			return null;
		}
	}

	public static MessageDigest getAlgorithm() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			return digest;
		} catch (NoSuchAlgorithmException e) {
			assert false;
			return null;
		}
	}

	@Override
	public void process(String name, InputStream stream) throws IOException {
		// skip a duplicated entry.  
		// A normal jar file contains a class once, but a corrupted zip file may contain a number of instances of the same class. 
		if (classfilenames.contains(name)) {
			return; 
		}
		classfilenames.add(name);

		MessageDigest digest = getAlgorithm();
		DigestInputStream s = new DigestInputStream(stream, digest);
		ClassInfo c = new ClassInfo(name, s);
		byte[] sha1 = digest.digest();
		
		String className = Signature.normalizeType(c.getClassName(),  false);
		String rawClassName = c.getClassName();
		rawnames.add(rawClassName);

		ComponentEntry entry = new ComponentEntry(rawClassName, className, sha1, Signature.getClassSignature(c, false), Signature.getDaviesClassSignature(c));
		entries.add(entry);

		packageNames.add(c.getPackageName());
	}

	
	@Override
	public boolean isTarget(String name) {
		return ComponentUtil.isClassFile(name);
	}
	
	
	public ArrayList<ComponentEntry> getEntries() {
		return entries;
	}
	
	
	public Set<String> getRawNames() {
		return rawnames;
	}
	
	
	public ArrayList<ClassHash> createHashList(int sig) {
		ArrayList<ClassHash> hashes = new ArrayList<>(entries.size());
		for (ComponentEntry e: entries) {
			hashes.add(e.getClassHash(sig));
		}
		Collections.sort(hashes);
		return hashes;
	}

	
	public ClassHash getPackageSetHash() {
		StringBuilder builder = new StringBuilder();
		for (String p: packageNames) {
			builder.append(p);
			builder.append(";");
		}
		ClassHash h = new ClassHash(builder.toString());
		return h;
	}
	
	


}
