package sarf.jingredients.hash;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Multiset of ClassHash objects
 */
public class ClassHashSet implements Iterable<ClassHash> {

	private TObjectIntHashMap<ClassHash> elementMap;
	private int elementCount;
	
	/**
	 * Create a new ClassHashSet for a given array of ClassHash objects.
	 */
	public ClassHashSet(ArrayList<ClassHash> list) {
		elementCount = list.size();
		TObjectIntHashMap<ClassHash> map = new TObjectIntHashMap<ClassHash>(list.size());
		for (ClassHash h: list) {
			map.adjustOrPutValue(h, 1, 1);
		}
		elementMap = map;
	}

	/**
	 * Create an instance by loading a binary stream.
	 * The binary stream must be created by writeToStream method.
	 */
	public static ClassHashSet readFromStream(ObjectInputStream stream) throws IOException {
		try {
			int size = stream.readInt();
			ArrayList<ClassHash> elements = new ArrayList<>(size);
			for (int i=0; i<size; ++i) {
				elements.add(new ClassHash(stream));
			}
			return new ClassHashSet(elements);
		} catch (EOFException e) {
			return null;
		}
	}
	
	/**
	 * Store a list of ClassHash to a binary stream 
	 * so that a program can load the list using readFromStream method. 
	 */
	public static void writeToStream(ObjectOutputStream out, ArrayList<ClassHash> list) throws IOException {
		out.writeInt(list.size());
		for (ClassHash element: list) {
			element.writeTo(out);
		}
	}
	
	/**
	 * An internal constructor directly uses an internal representation. 
	 */
	private ClassHashSet(TObjectIntHashMap<ClassHash> elements, int size) {
		this.elementMap = elements;
		this.elementCount = size;
	}
	
	/**
	 * The total number of elements 
	 */
	public int size() {
		return elementCount;
	}
	
	/**
	 * This method enables to visit hash values.
	 * A hash value is visited at most once even if 
	 * its multiple instances are included in the map.
	 */
	public Iterator<ClassHash> iterator() {
		return elementMap.keySet().iterator();
	}
	
	/**
	 * Compute an intersection between the receiver object and another instance.
	 * This method does not affect the receiver object.
	 */
	public ClassHashSet getIntersectionSet(ClassHashSet another) {
		Intersection in = new Intersection();
		another.elementMap.forEachEntry(in);
		return in.getResult();
	}
	

	private class Intersection implements TObjectIntProcedure<ClassHash> {

		private TObjectIntHashMap<ClassHash> intersectionMap;
		private int count;
		
		public Intersection() {
			this.intersectionMap = new TObjectIntHashMap<ClassHash>();
			this.count = 0;
		}
		
		@Override
		public boolean execute(ClassHash key, int v2) {
			int v1 = elementMap.get(key);
			if (v1 != elementMap.getNoEntryValue()) {
				int value = Math.min(v1, v2);
				intersectionMap.put(key, value);
				count += value;
			}
			return true;
		}
		
		public ClassHashSet getResult() { 
			return new ClassHashSet(intersectionMap, count);
		}
	}
	
	
	/**
	 * Remove given hash values in the receiver object. 
	 */
	public void removeAll(ClassHashSet another) {
		if (this.size() < another.size()) {
			Remover r = new Remover(another.elementMap);
			this.elementMap.forEachEntry(r);
			this.elementCount = r.getResultantSize();
		} else {
			Remover2 r2 = new Remover2();
			another.elementMap.forEachEntry(r2);
			this.elementCount = r2.getResultantSize();
		}
	}
	

	public boolean containsAll(ClassHashSet another) {
		Containment c = new Containment();
		another.elementMap.forEachEntry(c);
		return c.getResult();
	}
	
	
	/**
	 * Compute a overlap coefficient. 
	 * @return |this & another| / |another|
	 */
	public double overlap(ClassHashSet another) {
		if (another.size() > 0) {
			return intersection(another) * 1.0 / another.size();
		} else {
			return 0;
		}
	}

	
	private int intersection(ClassHashSet another) {
		Counter c = new Counter();
		another.elementMap.forEachEntry(c);
		return c.getCount();
	}


	public boolean equals(ClassHashSet another) {
		if (this.size() == another.size()) {
			Equal e = new Equal();
			another.elementMap.forEachEntry(e);
			return e.getResult();
		} else {
			return false;
		}
	}
	

	private class Remover2 implements TObjectIntProcedure<ClassHash> {
		
		private int count;
		
		public Remover2() {
			count = elementCount;
		}
		
		@Override
		public boolean execute(ClassHash key, int v2) {
			int v1 = elementMap.get(key);
			if (v1 != elementMap.getNoEntryValue()) {
				int newValue = v1 - v2;
				if (newValue <= 0) {
					elementMap.remove(key);
					count = count - v1;
				} else {
					elementMap.put(key, newValue);
					count = count - v2;
				}
			}
			return true;
		}
		
		public int getResultantSize() {
			return count;
		}
		
	}

	private class Remover implements TObjectIntProcedure<ClassHash> {
		
		private int count;
		private TObjectIntHashMap<ClassHash> another;
		
		public Remover(TObjectIntHashMap<ClassHash> another) {
			this.another = another;
			count = elementCount;
		}
		
		@Override
		public boolean execute(ClassHash key, int v1) {
			int v2 = another.get(key);
			if (v2 != elementMap.getNoEntryValue()) {
				int newValue = v1 - v2;
				if (newValue <= 0) {
					elementMap.remove(key);
					count = count - v1;
				} else {
					elementMap.put(key, newValue);
					count = count - v2;
				}
			}
			return true;
		}
		
		public int getResultantSize() {
			return count;
		}
		
	}
	
	/**
	 * Count the number of ClassHash included in elementMap.
	 */
	private class Counter implements TObjectIntProcedure<ClassHash> {

		private int count = 0;
		
		@Override
		public boolean execute(ClassHash key, int v2) {
			int v1 = elementMap.get(key);
			if (v1 != elementMap.getNoEntryValue()) {
				count += Math.min(v1, v2);
			}
			return true;
		}
		
		public int getCount() {
			return count;
		}
	}

	private class Containment implements TObjectIntProcedure<ClassHash> {

		private boolean result = true;
		
		@Override
		public boolean execute(ClassHash key, int v2) {
			int v1 = elementMap.get(key);
			if (v1 != elementMap.getNoEntryValue()) {
				if (v1 < v2) {
					result = false;
				}
			} else {
				result = false;
			}
			return result;
		}
		
		public boolean getResult() {
			return result;
		}
	}

	
	private class Equal implements TObjectIntProcedure<ClassHash> {

		private boolean result = true;
		private int count = 0;
		
		@Override
		public boolean execute(ClassHash key, int v2) {
			int v1 = elementMap.get(key);
			if (v1 != elementMap.getNoEntryValue()) {
				if (v1 != v2) {
					result = false;
				} else {
					count += v2;
				}
			} else {
				result = false;
			}
			return result;
		}
		
		public boolean getResult() {
			return result && (count == elementCount);
		}
	}

}
