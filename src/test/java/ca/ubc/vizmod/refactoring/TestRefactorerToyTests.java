package ca.ubc.vizmod.refactoring;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;

import ca.ubc.vizmod.adaptingstrategies.react.ReactAdaptingStrategy;
import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.browser.ChromeBrowser;
import ca.ubc.vizmod.refactorer.UIComponentRefactorer;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.IOUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestRefactorerToyTests extends AbstractTestRefactorer {

	@Override
	protected String getTestsPath() {
		return "";
	}

    @Test
	public void testRefactorerOnlyStructure() {

		String subjectName = "removed-subtree.html";

		List<String> parentNodeXPaths = Arrays.asList(
				"/HTML/BODY/DIV[1]",
				"/HTML/BODY/DIV[2]",
				"/HTML/BODY/DIV[3]"
		);

		refactor(subjectName, "ReactComponent", parentNodeXPaths, "refactored");
	}

	@Test
	public void testRefactorerWebComponentsPage() {

		String subjectName = "web-components.html";

		List<String> parentNodeXPaths = Arrays.asList(
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[1]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[2]",
				"//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/DIV[2]/SECTION[3]"
		);

		refactor(subjectName, "ReactComponent", parentNodeXPaths, "refactored");

	}

	@Test
    public void testRefactorerWebComponentsTest2() {

		String subjectName = "web-components-tools.html";

		List<String> parentNodeXPaths = Arrays.asList(
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[1]",
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[2]",
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[3]",
                "//*[@id=\"gc-wrapper\"]/DIV[2]/ARTICLE/ARTICLE/SECTION[1]/DIV/DIV[4]"
        );

		refactor(subjectName, "ReactComponent", parentNodeXPaths, "refactored");
    }

}
