package sarf.jingredients.model;


import org.junit.Assert;
import org.junit.Test;

import sarf.jingredients.model.Signature;

public class SignatureTest {

	@Test
	public void testNormalization() {
		Assert.assertEquals("String", Signature.normalizeType("java/util/String", true));
		Assert.assertEquals("String", Signature.normalizeType("package/String", true));
		Assert.assertEquals("C", Signature.normalizeType("C", true));
		Assert.assertEquals("C%A", Signature.normalizeType("package/C$1", true));
		Assert.assertEquals("C%A%A", Signature.normalizeType("package/C$1$2", true));
		Assert.assertEquals("C%A$Name", Signature.normalizeType("package/C$1$Name", true));
		Assert.assertEquals("List<String>", Signature.normalizeType("java/util/List<java/lang/String>", true));
		Assert.assertEquals("HashMap<String,ArrayList<String>>", Signature.normalizeType("java/util/HashMap<java/lang/String,java/util/ArrayList<java/lang/String>>", true));
	}

}
