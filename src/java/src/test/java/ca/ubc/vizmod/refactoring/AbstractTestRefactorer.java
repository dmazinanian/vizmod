package ca.ubc.vizmod.refactoring;

import ca.ubc.vizmod.adaptingstrategies.react.ReactAdaptingStrategy;
import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.browser.ChromeBrowser;
import ca.ubc.vizmod.model.UIComponent;
import ca.ubc.vizmod.refactorer.RefactoringResult;
import ca.ubc.vizmod.refactorer.UIComponentRefactorer;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLScriptElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        int totalAddedBytesReact = 0;


        for (int subtreesIndex = 0; subtreesIndex < parentNodeXPaths.size(); subtreesIndex++) {
            String componentName = "RC" + subtreesIndex;

            List<String> rootNodeXPaths = parentNodeXPaths.get(subtreesIndex);

            UIComponentRefactorer uiComponentRefactorer =
                    new UIComponentRefactorer(browserBefore, rootNodeXPaths, componentName);
            RefactoringResult refactoringResult = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());
            repeatedBytes += getRepetitionBytes(uiComponentRefactorer.getOriginalDocument(), rootNodeXPaths);
            totalAddedBytes += getTotalAddedBytes(refactoringResult);
            totalAddedBytesReact += getTotalAddedBytesWithReact(refactoringResult, componentName);

        }

        int fullBodySizeAfterRefactoring = fullSizeBodyBeforeRefactoring - repeatedBytes + totalAddedBytes;
        int fullBodySizeAfterRefactoringReact = fullSizeBodyBeforeRefactoring - repeatedBytes + totalAddedBytesReact;
        double savingRatio = 1.00 - (double) fullBodySizeAfterRefactoring / fullSizeBodyBeforeRefactoring;
        double savingRatioReact = 1.00 - (double) fullBodySizeAfterRefactoringReact / fullSizeBodyBeforeRefactoring;

        LOGGER.info("Total body size before refactoring: {}", fullSizeBodyBeforeRefactoring / 1024.00);
        LOGGER.info("Total repeated bytes (removed): {}", repeatedBytes / 1024.00);
        LOGGER.info("Total added bytes: {}", totalAddedBytes / 1024.00);
        LOGGER.info("Total added bytes (React): {}", totalAddedBytesReact / 1024.00);
        LOGGER.info("Total body size after refacoring: {}", fullBodySizeAfterRefactoring / 1024.00);
        LOGGER.info("Total body size after refacoring (React): {}", fullBodySizeAfterRefactoringReact / 1024.00);
        LOGGER.info("Savings: {}%", Math.round(savingRatio * 100 * 100) / 100.00);
        LOGGER.info("Savings (React): {}%", Math.round(savingRatioReact * 100 * 100) / 100.00);
    }

    protected void refactor(String subjectName, String componentName, List<String> parentNodeXPaths, String refactoredNameSuffix) {

        AbstractBrowser browserBefore = initializeBrowser(subjectName, false);

        sleep();

        UIComponentRefactorer uiComponentRefactorer =
                new UIComponentRefactorer(browserBefore, parentNodeXPaths, componentName);

        RefactoringResult refactoringResult = uiComponentRefactorer.refactor(new ReactAdaptingStrategy());

        Document newDocument = refactoringResult.getDocument();

        String elementString = DocumentUtil.getElementString(newDocument);

        String refactoredFile = subjectName.replace(".html", "-" + refactoredNameSuffix + ".html");

        IOUtil.writeStringToFile(elementString, getTestsPath() + refactoredFile);

        AbstractBrowser browserAfter = initializeBrowser(refactoredFile, false);

        sleep();

        domTest(browserBefore, browserAfter, parentNodeXPaths);

        //fullDOMTest(browserBefore, browserAfter);

        //visualTest();

        double savingPercent = computeSavingsPercents(
                uiComponentRefactorer.getOriginalDocument(),
                refactoringResult,
                parentNodeXPaths, componentName);

        LOGGER.info("Savings: {}%", Math.round(savingPercent * 100 * 100) / 100.00);

    }

    protected void sleep() {
        try {
            Thread.sleep(BROWSER_WAIT_TIME_AFTER_INIT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double computeSavingsPercents(Document originalDocument, RefactoringResult refactoringResult, List<String> rootXPaths, String componentName) {

        int repetitionBytes = getRepetitionBytes(originalDocument, rootXPaths);

        int totalAddedBytes = getTotalAddedBytes(refactoringResult);
        //int totalAddedBytesReact = getTotalAddedBytesWithReact(refactoringResult, componentName);

        return (double) (repetitionBytes - totalAddedBytes) / repetitionBytes;

    }

    private int getTotalAddedBytes(RefactoringResult refactoringResult) {

        int jsSize = getBytesCount(refactoringResult.getComponentBody().replace("this.props.", ""));
        int componentCustomElements = 0;

        for (List<String> args : refactoringResult.getParameterizedValues()) {
            for (String arg : args) {
                componentCustomElements += getBytesCount(arg);
            }
        }

        return jsSize + componentCustomElements;

    }

    private int getTotalAddedBytesWithReact(RefactoringResult refactoringResult, String componentName) {
        int jsSize = 0;
        int componentCustomElements = 0;
        Document newDocument = refactoringResult.getDocument();

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
            if (null != attr && "VizMod".equals(((Attr) attr).getValue())) {
                toRemove.add(script);
            }
        }
        toRemove.forEach(node -> node.getParentNode().removeChild(node));

        return newDocument;

    }
}