package ca.ubc.vizmod.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;

public class DocumentUtilTests {
	@Test
	public void testNewick() {
		String domTest = ResourcesUtil.readResourceFileToString("TestWebsites/removed-subtree.html");
		Document document = DocumentUtil.toDocument(domTest);
		String expected = "(((A)DIV,(B)DIV,((C)A)DIV)DIV,((A)DIV,(B)DIV,((A)A,(A)A,(A)A,(A)A,((C)A,(C)A)DIV)DIV)DIV,((A)DIV,(C)DIV,(((A)A,(C)A)DIV)DIV)DIV)BODY";
		String actual = DocumentUtil.newick(document.getElementsByTagName("body").item(0));
		assertEquals(expected, actual);
	}
}
