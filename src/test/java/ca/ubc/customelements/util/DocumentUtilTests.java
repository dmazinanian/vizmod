package ca.ubc.customelements.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;

import ca.ubc.customelements.util.DocumentUtil;
import ca.ubc.customelements.util.IOUtil;

public class DocumentUtilTests {
	@Test
	public void testNewick() {
		String domTest = IOUtil.readResourceFileToString("1.html");
		Document document = DocumentUtil.toDocument(domTest);
		String newick = DocumentUtil.newick(document.getElementsByTagName("body").item(0), false);
		assertEquals("((((()TD,()TD)TR,(()TD,()TD)TR,(()TD,()TD)TR,(()TD,()TD)TR)TBODY)TABLE)BODY", newick);
	}
}
