package ca.ubc.vizmod.refactoring;

import ca.ubc.vizmod.adaptingstrategies.react.ReactAdaptingStrategy;
import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.browser.ChromeBrowser;
import ca.ubc.vizmod.refactorer.UIComponentRefactorer;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.IOUtil;
import org.junit.After;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractTestRefactorer {

    protected abstract String getTestsPath();

    private AbstractBrowser initializeBrowser(String htmlFile, boolean headless) {
        return ChromeBrowser.getForFile(getTestsPath() + htmlFile, headless);
    }

    protected void refactor(String subjectName, String componentName, List<String> parentNodeXPaths, String refactoredNameSuffix) {

        AbstractBrowser browserBefore = initializeBrowser(subjectName, false);

        try {
            Thread.sleep(4000); // Give some time to the browser
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        UIComponentRefactorer uiComponentRefactorer =
                new UIComponentRefactorer(browserBefore, parentNodeXPaths, componentName);

        Document newDocument = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());

        String elementString = DocumentUtil.getElementString(newDocument);

        String refactoredFile = subjectName.replace(".html", "-" + refactoredNameSuffix + ".html");

        IOUtil.writeStringToFile(elementString, getTestsPath() + refactoredFile);

        AbstractBrowser browserAfter = initializeBrowser(refactoredFile, false);

        domTest(browserBefore, browserAfter, parentNodeXPaths);

        //visualTest();

        //computeSavings(uiComponentRefactorer);

    }

    private void domTest(AbstractBrowser browserBefore, AbstractBrowser browserAfter, List<String> parentNodeXPaths) {

        Document documentBefore = DocumentUtil.toDocument(browserBefore.getDOM());
        Document documentAfter = DocumentUtil.toDocument(browserAfter.getDOM());

        for (String parentNodeXPath : parentNodeXPaths) {
            NodeList nodeBefore = DocumentUtil.queryDocument(documentBefore, parentNodeXPath);
            NodeList nodeAfter = DocumentUtil.queryDocument(documentAfter, parentNodeXPath);
            if (nodeBefore.getLength() != nodeAfter.getLength()) {
                String message = String.format("Elements pointed by %s are different in number", parentNodeXPath);
                fail(message);
            } else if (nodeBefore.getLength() == 1 && nodeAfter.getLength() == 1) {
                String newickBefore = DocumentUtil.newick(nodeBefore.item(0));
                String newickAfter = DocumentUtil.newick(nodeAfter.item(0));
                assertEquals(newickBefore, newickAfter);
            }
        }
    }

}
