package ca.ubc.uicomponentrefactorer.model;

import ca.ubc.uicomponentrefactorer.adaptingstrategies.AdaptingStrategy;

import javax.annotation.Nullable;
import java.util.List;

public class ParameterizedUIComponentElement extends UIComponentElement {

    private ParameterizationReason parameterizationReason;

    public ParameterizedUIComponentElement(UIComponentElement parent,
                                           List<String> correspondingOriginalNodes,
                                           String name,
                                           ParameterizationReason parameterizationReason,
                                           int templateTreeIndex) {
        super(parent, correspondingOriginalNodes, name, templateTreeIndex);

        this.parameterizationReason = parameterizationReason;
    }

    public ParameterizationReason getParameterizationReason() {
        return parameterizationReason;
    }

    @Nullable
    @Override
    public String getUnifyingNodeXPath(AdaptingStrategy adaptingStrategy) {
        switch (parameterizationReason) {
            case DIFFERENT_TAG_NAMES:
            case DIFFERENT_TEXT_NODE_VALUE:
                return null;
            case DIFFERENT_CHILD_COUNT:
                return correspondingOriginalNodesXPaths.get(templateTreeIndex);
            case DIFFERENT_ATTRIBUTE_VALUE:
                if (adaptingStrategy.supportsAttributeParameterization()) {
                    return correspondingOriginalNodesXPaths.get(templateTreeIndex);
                } else {
                    return null;
                }
        }
        return null;
    }
}
