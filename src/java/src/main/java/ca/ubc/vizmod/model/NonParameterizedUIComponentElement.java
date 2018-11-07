package ca.ubc.vizmod.model;

import ca.ubc.vizmod.adaptingstrategies.AdaptingStrategy;

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
