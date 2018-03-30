package ca.ubc.vizmod.refactoring;

import ca.ubc.vizmod.adaptingstrategies.react.ReactAdaptingStrategy;
import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.browser.ChromeBrowser;
import ca.ubc.vizmod.refactorer.UIComponentRefactorer;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.IOUtil;
import org.junit.After;
import org.w3c.dom.Document;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

public abstract class AbstractTestRefactorer {

    private AbstractBrowser browser;

    protected abstract String getTestsPath();

    private static String getResourcesPath(String resourceFileName) {
        ClassLoader classLoader = TestRefactorerToyTests.class.getClassLoader();
        URL testWebsitesURL = classLoader.getResource(resourceFileName);
        try {
            String decodedURL = URLDecoder.decode(testWebsitesURL.getPath().toString(), "UTF-8");
            if (!decodedURL.endsWith("/")) {
                decodedURL += "/";
            }
            return decodedURL;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void initializeBrowser(String htmlFile, boolean headless) {
        browser = ChromeBrowser.getForFile(getTestsPath() + htmlFile, headless);
    }

    @After
    public void tearDown() throws Exception {
//        if (null != browser) {
//			browser.close();
//		}
    }

    protected void refactor(String subjectName, String componentName, List<String> parentNodeXPaths, String refactoredNameSuffix) {

        initializeBrowser(subjectName, false);

        UIComponentRefactorer uiComponentRefactorer =
                new UIComponentRefactorer(browser, parentNodeXPaths, componentName);

        Document newDocument = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());

        String elementString = DocumentUtil.getElementString(newDocument);

        String refactoredFile = subjectName.replace(".html", "-" + refactoredNameSuffix + ".html");

        IOUtil.writeStringToFile(elementString, getTestsPath() + refactoredFile);

        initializeBrowser(refactoredFile, false);

    }
}
