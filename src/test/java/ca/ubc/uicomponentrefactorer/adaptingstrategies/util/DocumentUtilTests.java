package ca.ubc.uicomponentrefactorer.adaptingstrategies.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;

public class DocumentUtilTests {
	@Test
	public void testNewick() {
		String domTest = ResourcesUtil.readResourceFileToString("1.html");
		Document document = DocumentUtil.toDocument(domTest);
		String newick = DocumentUtil.newick(document.getElementsByTagName("body").item(0), false);
		assertEquals("((((()TD,()TD)TR,(()TD,()TD)TR,(()TD,()TD)TR,(()TD,()TD)TR)TBODY)TABLE)BODY", newick);
	}
}
