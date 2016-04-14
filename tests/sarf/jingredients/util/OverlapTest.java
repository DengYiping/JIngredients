package sarf.jingredients.util;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import sarf.jingredients.util.Overlap;

public class OverlapTest {

	private static final double DELTA = 0.0001;

	@Test
	public void testStringOverlap() {
		ArrayList<String> first = new ArrayList<String>();
		ArrayList<String> second = new ArrayList<String>();
		
		first.add("A");
		first.add("B");
		first.add("C");
		first.add("C");
		first.add("D");
		Collections.sort(first);
		
		second.add("A");
		Collections.sort(first);
		Assert.assertEquals(1.0, Overlap.getOverlap(first, second, 0), DELTA);

		second.add("A");
		second.add("B");
		second.add("C");
		second.add("C");
		Collections.sort(first);
		Assert.assertEquals(0.8, Overlap.getOverlap(first, second, 0), DELTA);

		second.remove("A");
		second.add("D");
		Assert.assertEquals(1.0, Overlap.getOverlap(first, second, 0), DELTA);

		second.add("D");
		second.add("D");
		second.add("D");
		second.add("D");
		second.add("D");
		Assert.assertEquals(0.5, Overlap.getOverlap(first, second, 0), DELTA);
		Assert.assertEquals(0.5, Overlap.getOverlap(first, second, 0.5), DELTA);
		
		first.add("E");
		Assert.assertEquals(0.0, Overlap.getOverlap(first, second, 0.6), DELTA);
	}
	
	@Test
	public void testStringIntersection() {
		ArrayList<String> first = new ArrayList<String>();
		ArrayList<String> second = new ArrayList<String>();
		
		first.add("A");
		first.add("B");
		first.add("C");
		first.add("C");
		first.add("D");
		Collections.sort(first);
		
		second.add("A");
		Collections.sort(first);
		Assert.assertEquals(0.2, Overlap.getJaccard(first, second), DELTA);

		second.add("B");
		second.add("C");
		second.add("C");
		Collections.sort(first);
		Assert.assertEquals(0.8, Overlap.getJaccard(first, second), DELTA);

		second.add("D");
		Assert.assertEquals(1.0, Overlap.getJaccard(first, second), DELTA);

		second.add("A");
		second.add("A");
		second.add("D");
		second.add("F");
		second.add("F");
		Collections.sort(first);
		Assert.assertEquals(0.5, Overlap.getJaccard(first, second), DELTA);
		
	}
}
