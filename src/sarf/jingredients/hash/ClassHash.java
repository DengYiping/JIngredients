package sarf.jingredients.hash;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * ClassHash is a utility class to regard 
 * a SHA1 hash value as a comparable object.
 */
public class ClassHash implements Comparable<ClassHash> {

	private final int SHA1LENGTH = 20;
	private byte[] array;
	private int hashcode;
	
	/**
	 * Create a class hash from a signature string.
	 * This constructor internally translates a given string into a SHA1 hash
	 * and does not keep the original signature string. 
	 * @param signature
	 */
	public ClassHash(String signature) {
		this(getStringHash(signature));
	}
	
	/**
	 * Create an instance to represent a given SHA1 hash.
	 * @param array
	 */
	public ClassHash(byte[] array) {
		assert array.length == SHA1LENGTH;
		this.array = array;
		computeHash();
	}
	
	public ClassHash(ObjectInputStream stream) throws IOException {
		array = new byte[SHA1LENGTH];
		stream.readFully(array);
		computeHash();
	}
	
	public static byte[] getStringHash(String signature) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			return digest.digest(signature.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			assert false;
			return new byte[20];
		}
	}
	
	/**
	 * Write the hash value to a specified output stream.
	 */
	public void writeTo(OutputStream stream) throws IOException {
		stream.write(array);
	}
	
	/**
	 * Compute an internal 
	 */
	private void computeHash() {
		hashcode = 0;
		for (int i=0; i<array.length; ++i) {
			hashcode = (hashcode << 1) ^ array[i]; 
		}
	}
	
	public int compareTo(ClassHash another) {
		assert this.array.length == another.array.length;
		for (int i=0; i<array.length; ++i) {
			byte b1 = this.array[i];
			byte b2 = another.array[i];
			if (b1 != b2) {
				return compareUnsignedByte(b1, b2);
			}
		}
		return 0;
	}

	public static int compareByteArray(byte[] ba1, byte[] ba2) {
		assert ba1.length == ba2.length;
		for (int i=0; i<ba1.length; ++i) {
			byte b1 = ba1[i];
			byte b2 = ba2[i];
			if (b1 != b2) {
				return compareUnsignedByte(b1, b2);
			}
		}
		return 0;
	}

	public static int compareUnsignedByte(byte b1, byte b2) { 
		int i1 = (b1 >= 0) ? b1 : (b1 + 256);
		int i2 = (b2 >= 0) ? b2 : (b2 + 256);
		return i1-i2;
	}
	
	@Override
	public int hashCode() {
		return hashcode;
	}
	
	@Override
	public boolean equals(Object another) {
		if (another instanceof ClassHash) {
			ClassHash h = (ClassHash)another;
			return Arrays.equals(this.array, h.array);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(array.length * 2);
		for (int i=0; i<array.length; ++i) {
			builder.append(String.format("%02x", array[i]));
		}
		return builder.toString();
	}
	
}
