package ca.ubc.uicomponentrefactorer.model;

import ca.concordia.cssanalyser.cssmodel.StyleSheet;
import ca.ubc.uicomponentrefactorer.adaptingstrategies.AdaptingStrategy;
import org.w3c.dom.Document;

public class UIComponent extends UIComponentElement {

    private final Document originalDocument;

    private StyleSheet styles; // Styles associated with the component and its subtree nodes
    private JavaScript script;

    public UIComponent(String uiComponentName, Document originalDocument, int templateTreeIndex) {
        super(null, uiComponentName, templateTreeIndex);
        this.originalDocument = originalDocument;
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

    public int getNumberOfTrees() {
        if (children.size() > 0) {
            return children.get(0).getCorrespondingOriginalNodesXPaths().size();
        } else {
            return 0;
        }
    }

    @Override
    public String getUnifyingNodeXPath(AdaptingStrategy adaptingStrategy) {
        if (getChildren().size() == 0 || getChildren().size() > 1) {
            return null;
        } else {
            UIComponentElement unifyingUIComponentElement = getChildren().get(0);
            return unifyingUIComponentElement.getUnifyingNodeXPath(adaptingStrategy);
        }
    }
}
