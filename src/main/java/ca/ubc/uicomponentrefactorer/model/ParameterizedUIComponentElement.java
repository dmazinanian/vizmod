package ca.ubc.uicomponentrefactorer.model;

import java.util.List;

public class ParameterizedUIComponentElement extends UIComponentElement {

    private final boolean shouldParameterizeAttributes;

    public ParameterizedUIComponentElement(UIComponentElement parent,
                                           List<String> correspondingOriginalNodes,
                                           String name,
                                           boolean shouldParameterizeAttributes) {
        super(parent, correspondingOriginalNodes, name);

        this.shouldParameterizeAttributes = shouldParameterizeAttributes;
    }
}
