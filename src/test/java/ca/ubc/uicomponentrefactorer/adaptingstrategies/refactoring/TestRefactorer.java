package ca.ubc.uicomponentrefactorer.adaptingstrategies.refactoring;

import java.io.File;
import java.util.*;

import ca.ubc.uicomponentrefactorer.adaptingstrategies.react.ReactAdaptingStrategy;
import ca.ubc.uicomponentrefactorer.adaptingstrategies.webcomponents.WebComponentsAdaptingStrategy;
import ca.ubc.uicomponentrefactorer.browser.AbstractBrowser;
import ca.ubc.uicomponentrefactorer.browser.ChromeBrowser;
import ca.ubc.uicomponentrefactorer.refactorer.UIComponentRefactorer;
import ca.ubc.uicomponentrefactorer.util.DocumentUtil;
import ca.ubc.uicomponentrefactorer.util.IOUtil;
import org.junit.Test;
import org.w3c.dom.Document;

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

		String componentName = "ReactComponent";
		UIComponentRefactorer uiComponentRefactorer = new UIComponentRefactorer(browser, parentNodeXPaths, componentName);

		Document newDocument = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());

		String elementString = DocumentUtil.getElementString(newDocument);
		IOUtil.writeStringToFile(elementString, OUTPUT_PATH + File.separator + "removed-subtree-refactored.html");
	}

	//@Test
	public void testRefactorerWebComponentsPage() {

		String url = "http://localhost:8080/web-components.html";
		AbstractBrowser browser = new ChromeBrowser(url, false);

		List<String> parentNodeXPaths = Arrays.asList(
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[1]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[2]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[3]"
		);

		UIComponentRefactorer uiComponentRefactorer = new UIComponentRefactorer(browser, parentNodeXPaths,"custom-element");

		Document newDocument = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());
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

        UIComponentRefactorer uiComponentRefactorer = new UIComponentRefactorer(browser, parentNodeXPaths,"custom-element");

        Document newDocument = uiComponentRefactorer.refactor(new WebComponentsAdaptingStrategy());
        String newDocumentHTML = DocumentUtil.getElementString(newDocument);
        IOUtil.writeStringToFile(newDocumentHTML, OUTPUT_PATH + File.separator + "web-components-tools-refactored.html");
    }
	
}
