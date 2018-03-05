package ca.ubc.uicomponentrefactorer.model;

import ca.ubc.uicomponentrefactorer.adaptingstrategies.AdaptingStrategy;

import javax.annotation.Nullable;
import java.util.List;

public class NonParameterizedUIComponentElement extends UIComponentElement {

    public NonParameterizedUIComponentElement(UIComponentElement parent,
                                              List<String> mappedNodes,
                                              String name,
                                              int templateTreeIndex) {
        super(parent, mappedNodes, name, templateTreeIndex);
    }

    @Nullable
    @Override
    public String getUnifyingNodeXPath(AdaptingStrategy adaptingStrategy) {
        return correspondingOriginalNodesXPaths.get(templateTreeIndex);
    }
}
