package sarf.jingredients.model;

import sarf.jingredients.hash.ClassHash;


public class ComponentEntry {

	public static final int SIG_CLASSNAME = 0;
	public static final int SIG_CODE = 1;
	public static final int SIG_FILE = 2;
	public static final int SIG_BERTILLONAGE = 3;

	private String rawname;
	private String name;
	private byte[] hash;
	private String codeSig;
	private String daviesSig;
	
	
	public ComponentEntry(String rawname, String name, byte[] fileHash, String codeSig, String daviesSig) {
		this.rawname = rawname;
		this.name = name;
		this.hash = fileHash;
		this.codeSig = codeSig;
		this.daviesSig = daviesSig;
	}
	
	
	public ClassHash getClassHash(int sig) {
		switch (sig) {
		case SIG_CLASSNAME:
			return new ClassHash(getName());

		case SIG_CODE:
			return new ClassHash(getCodeSig());

		case SIG_FILE:
			return new ClassHash(getFileHash());

		case SIG_BERTILLONAGE:
			return new ClassHash(getDaviesSig());

		default:
			assert false;
			return new ClassHash("");
		}
	}

	
	public String getRawName() {
		return rawname;
	}
	
	public String getName() {
		return name;
	}
	
	public byte[] getFileHash() {
		return hash;
	}
	
	public String getCodeSig() {
		return codeSig;
	}
	
	public String getDaviesSig() {
		return daviesSig;
	}

}
