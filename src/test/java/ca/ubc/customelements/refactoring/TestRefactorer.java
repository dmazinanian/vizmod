package ca.ubc.customelements.refactoring;

import java.io.File;
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

	private static final String OUTPUT_PATH = "";
	
	@Test
	public void testRefactorerOnlyStructure() {

		String url = "http://localhost:8080/removed-subtree.html";
		AbstractBrowser browser = new ChromeBrowser(url, false);

		List<String> parentNodeXPaths = Arrays.asList(
				"/HTML/BODY/DIV[1]",
				"/HTML/BODY/DIV[2]",
				"/HTML/BODY/DIV[3]"
		);

		Refactorer refactorer = new Refactorer(browser, parentNodeXPaths, "custom-element");

		Document newDocument = refactorer.refactor();

		IOUtil.writeStringToFile(DocumentUtil.getElementString(newDocument), OUTPUT_PATH + File.separator + "removed-subtree-refactored.html");
	}
	
	@Test
	public void testRefactorerWebComponentsPage() {

		String url = "http://localhost:8080/web-components.html";
		AbstractBrowser browser = new ChromeBrowser(url, false);

		List<String> parentNodeXPaths = Arrays.asList(
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[1]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[2]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[3]"
		);

		Refactorer refactorer = new Refactorer(browser, parentNodeXPaths,"custom-element");

		Document newDocument = refactorer.refactor();
		String newDocumentHTML = DocumentUtil.getElementString(newDocument);
		IOUtil.writeStringToFile(newDocumentHTML, OUTPUT_PATH + File.separator + "web-components-refactored.html");

	}

	//@Test
    public void testRefactorerWebComponentsTest2() {

        String url = "http://localhost:8080/web-components-tools.html";
        AbstractBrowser browser = new ChromeBrowser(url, false);

        List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[1]",
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[2]",
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[3]",
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[4]"
        );

        Refactorer refactorer = new Refactorer(browser, parentNodeXPaths,"custom-element");

        Document newDocument = refactorer.refactor();
        String newDocumentHTML = DocumentUtil.getElementString(newDocument);
        IOUtil.writeStringToFile(newDocumentHTML, OUTPUT_PATH + File.separator + "web-components-tools-refactored.html");
    }
	
}
