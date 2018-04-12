package ca.ubc.vizmod.refactoring;

import ca.ubc.vizmod.adaptingstrategies.react.ReactAdaptingStrategy;
import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.browser.ChromeBrowser;
import ca.ubc.vizmod.model.UIComponent;
import ca.ubc.vizmod.refactorer.UIComponentRefactorer;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLScriptElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractTestRefactorer {

    private static final int BROWSER_WAIT_TIME_AFTER_INIT = 5000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestRefactorer.class);

    protected abstract String getTestsPath();

    private AbstractBrowser initializeBrowser(String htmlFile, boolean headless) {
        return ChromeBrowser.getForFile(getTestsPath() + htmlFile, headless);
    }

    protected void refactor(String subjectName, List<List<String>> parentNodeXPaths) {

        AbstractBrowser browserBefore = initializeBrowser(subjectName, false);
        sleep();

        Document documentBefore = DocumentUtil.toDocument(browserBefore.getDOM());
        HTMLElement body = (HTMLElement) documentBefore.getElementsByTagName("body").item(0);

        int fullSizeBodyBeforeRefactoring = getBytesCount(DocumentUtil.getElementString(body));

        int repeatedBytes = 0;

        int totalAddedBytes = 0;

        for (int subtreesIndex = 0; subtreesIndex < parentNodeXPaths.size(); subtreesIndex++) {
            String componentName = "RC" + subtreesIndex;
            List<String> rootNodeXPaths = parentNodeXPaths.get(subtreesIndex);
            UIComponentRefactorer uiComponentRefactorer =
                    new UIComponentRefactorer(browserBefore, rootNodeXPaths, componentName);
            Document newDocument = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());
            repeatedBytes += getRepetitionBytes(uiComponentRefactorer.getOriginalDocument(), rootNodeXPaths);
            totalAddedBytes += getTotalAddedBytes(newDocument, componentName);
        }

        int fullBodySizeAfterRefactoring = fullSizeBodyBeforeRefactoring - repeatedBytes + totalAddedBytes;
        double savingRatio = 1.00 - (double) fullBodySizeAfterRefactoring / fullSizeBodyBeforeRefactoring;

        LOGGER.info("Total body size before refactoring: {}", fullSizeBodyBeforeRefactoring);
        LOGGER.info("Total repeated bytes (removed): {}", repeatedBytes);
        LOGGER.info("Total added bytes: {}", totalAddedBytes);
        LOGGER.info("Total body size after refacoring: {}", fullBodySizeAfterRefactoring);
        LOGGER.info("Savings: {}%", Math.round(savingRatio * 100 * 100) / 100.00);
    }

    protected void refactor(String subjectName, String componentName, List<String> parentNodeXPaths, String refactoredNameSuffix) {

        AbstractBrowser browserBefore = initializeBrowser(subjectName, false);
        //System.out.println(DocumentUtil.bfs(DocumentUtil.toDocument(browserBefore.getDOM()), false).size());
        sleep();

        UIComponentRefactorer uiComponentRefactorer =
                new UIComponentRefactorer(browserBefore, parentNodeXPaths, componentName);

        Document newDocument = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());

        String elementString = DocumentUtil.getElementString(newDocument);

        String refactoredFile = subjectName.replace(".html", "-" + refactoredNameSuffix + ".html");

        IOUtil.writeStringToFile(elementString, getTestsPath() + refactoredFile);

        AbstractBrowser browserAfter = initializeBrowser(refactoredFile, false);

        sleep();

        domTest(browserBefore, browserAfter, parentNodeXPaths);

        //fullDOMTest(browserBefore, browserAfter);

        //visualTest();

        double savingPercent = computeSavingsPercents(uiComponentRefactorer.getOriginalDocument(),
                    newDocument,
                    parentNodeXPaths,
                    componentName);

        LOGGER.info("Savings: {}%", Math.round(savingPercent * 100 * 100) / 100.00);

    }

    protected void sleep() {
        try {
            Thread.sleep(BROWSER_WAIT_TIME_AFTER_INIT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double computeSavingsPercents(Document originalDocument, Document newDocument, List<String> rootXPaths, String componentName) {

        int repetitionBytes = getRepetitionBytes(originalDocument, rootXPaths);

        int totalAddedBytes = getTotalAddedBytes(newDocument, componentName);

        return (double)(repetitionBytes - totalAddedBytes) / repetitionBytes;

    }

    private int getTotalAddedBytes(Document newDocument, String componentName) {
        int jsSize = 0;
        int componentCustomElements = 0;

        NodeList scripts = newDocument.getElementsByTagName("SCRIPT");
        for (int i = 0; i < scripts.getLength(); i++) {
            HTMLScriptElement scriptElement = (HTMLScriptElement) scripts.item(i);
            if ("text/babel".equals(scriptElement.getType())) {
                jsSize = getBytesCount(scriptElement.getTextContent());
                break;
            }
        }

        NodeList customElements = newDocument.getElementsByTagName(componentName);
        for (int i = 0; i < customElements.getLength(); i++) {
            HTMLElement customElement = (HTMLElement) customElements.item(i);
            String nodeString = DocumentUtil.getElementString(customElement);
            componentCustomElements += getBytesCount(nodeString);
        }

        return jsSize + componentCustomElements;
    }

    private int getRepetitionBytes(Document originalDocument, List<String> rootXPaths) {
        StringBuilder builder = new StringBuilder();
        for (String xpath : rootXPaths) {
            Node node = DocumentUtil.queryDocument(originalDocument, xpath).item(0);
            builder.append(DocumentUtil.getElementString(node));
        }
        return getBytesCount(builder.toString());
    }

    private int getBytesCount(String string) {
        return string.getBytes(StandardCharsets.UTF_8).length;
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

    private void fullDOMTest(AbstractBrowser browserBefore, AbstractBrowser browserAfter) {

        Document documentBefore = DocumentUtil.toDocument(browserBefore.getDOM());
        Document documentAfter = DocumentUtil.toDocument(browserAfter.getDOM());

        Document documentAfterRemovedUnnecessaryNodes = removeUnnecessaryNodes(documentAfter);

        String newickBefore = DocumentUtil.newick(documentBefore.getElementsByTagName("body").item(0));
        String newickAfter = DocumentUtil.newick(documentAfterRemovedUnnecessaryNodes.getElementsByTagName("body").item(0));

        assertEquals(newickBefore, newickAfter);

    }

    private Document removeUnnecessaryNodes(Document originalDocument) {

        Document newDocument = (Document) originalDocument.cloneNode(true);

        List<HTMLScriptElement> toRemove = new ArrayList<>();

        NodeList scripts = newDocument.getElementsByTagName("SCRIPT");
        for (int i = 0; i < scripts.getLength(); i++) {
            HTMLScriptElement script = (HTMLScriptElement) scripts.item(i);
            Node attr = script.getAttributes().getNamedItem("data");
            if (null != attr && "VizMod".equals(((Attr)attr).getValue())) {
                toRemove.add(script);
            }
        }
        toRemove.forEach(node -> node.getParentNode().removeChild(node));

        return newDocument;

    }

}
