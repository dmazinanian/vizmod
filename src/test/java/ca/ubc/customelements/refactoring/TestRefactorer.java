package ca.ubc.customelements.refactoring;

import java.util.*;

import ca.ubc.customelements.browser.AbstractBrowser;
import ca.ubc.customelements.browser.ChromeBrowser;
import ca.ubc.customelements.css.CSS;
import ca.ubc.customelements.css.CSSUtil;
import ca.ubc.customelements.util.ResourcesUtil;
import org.apache.html.dom.HTMLDocumentImpl;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ca.ubc.customelements.util.DocumentUtil;
import ca.ubc.customelements.util.IOUtil;

public class TestRefactorer {
	
	//@Test
	public void testRefactorer() {
		Document document = DocumentUtil.toDocument(ResourcesUtil.readResourceFileToString("TestWebsites/removed-subtree.html"));
		List<Node> parentNodes = new ArrayList<>();
		parentNodes.add(document.getElementById("div1"));
		parentNodes.add(document.getElementById("div2"));
		parentNodes.add(document.getElementById("div3"));
		//Refactorer refactorer = new Refactorer((HTMLDocumentImpl) document, parentNodes, new HashMap<>(), "custom-element");
		//Document newDocument = refactorer.refactor();
		//IOUtil.writeStringToFile(DocumentUtil.getElementString(newDocument),
		//		"/Users/davood/Google Drive/Research/Codes/custom-elements-refactorer/src/test/resources/TestWebsites/removed-subtree-refactored.html");
	}
	
	@Test
	public void testRefactorerWebComponentsPage() {
//		String url = "http://localhost:8080/removed-subtree.html";
		String url = "http://localhost:8080/web-components.html";

		AbstractBrowser browser = new ChromeBrowser(url, false);

		List<String> parentNodeXPaths = Arrays.asList(
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[1]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[2]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[3]"
		);

		Refactorer refactorer = new Refactorer(browser, parentNodeXPaths, "custom-element");

		Document newDocument = refactorer.refactor();
		String newDocumentHTML = DocumentUtil.getElementString(newDocument);
		IOUtil.writeStringToFile(newDocumentHTML,
				"/Users/davood/Google Drive/Research/Codes/custom-elements-refactorer/src/test/resources/TestWebsites/web-components-refactored.html");

	}
	
}
