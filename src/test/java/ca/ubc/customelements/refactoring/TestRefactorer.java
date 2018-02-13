package ca.ubc.customelements.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.apache.html.dom.HTMLDocumentImpl;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ca.ubc.customelements.util.DocumentUtil;
import ca.ubc.customelements.util.IOUtil;

public class TestRefactorer {
	
	@Test
	public void testRefactorer() {
		Document document = DocumentUtil.toDocument(IOUtil.readResourceFileToString("TestWebsites/removed-subtree.html"));
		List<Node> parentNodes = new ArrayList<>();
		parentNodes.add(document.getElementById("div1"));
		parentNodes.add(document.getElementById("div2"));
		parentNodes.add(document.getElementById("div3"));
		Refactorer refactorer = new Refactorer((HTMLDocumentImpl) document, parentNodes, "custom-element");
		Document newDocument = refactorer.refactor();
		IOUtil.writeStringToFile(DocumentUtil.getElementString(newDocument), 
				"/Users/davood/Google Drive/Research/Codes/custom-elements-refactorer/src/test/resources/TestWebsites/removed-subtree-refactored.html");
	}
	
	@Test
	public void testRefactorerWebComponentsPage() {
		Document document = DocumentUtil.toDocument(IOUtil.readResourceFileToString("TestWebsites/web-components.html"));
		List<Node> parentNodes = new ArrayList<>();
		parentNodes.add(DocumentUtil.queryDocument(document, "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[1]").item(0));
		parentNodes.add(DocumentUtil.queryDocument(document, "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[2]").item(0));
		parentNodes.add(DocumentUtil.queryDocument(document, "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[3]").item(0));
		Refactorer refactorer = new Refactorer((HTMLDocumentImpl) document, parentNodes, "custom-element");
		Document newDocument = refactorer.refactor();
		IOUtil.writeStringToFile(DocumentUtil.getElementString(newDocument), 
				"/Users/davood/Google Drive/Research/Codes/custom-elements-refactorer/src/test/resources/TestWebsites/web-components-refactored.html");
	}
	
}
