package ca.ubc.uicomponentrefactorer.model;

import ca.concordia.cssanalyser.cssmodel.StyleSheet;
import org.w3c.dom.Document;

public class UIComponent extends UIComponentElement {

    private final Document originalDocument;
    private final int templateTreeIndex;
    private StyleSheet styles; // Styles associated with the component and its subtree nodes
    private JavaScript script;

    public UIComponent(String uiComponentName, Document originalDocument, int templateTreeIndex) {
        super(null, uiComponentName);
        this.originalDocument = originalDocument;
        this.templateTreeIndex = templateTreeIndex;
    }

    public Document getOriginalDocument() {
        return originalDocument;
    }

    /**
     * Returns the index of the tree that all other trees were compared to.
     */
    public int getTemplateTreeIndex() {
        return templateTreeIndex;
    }

}
