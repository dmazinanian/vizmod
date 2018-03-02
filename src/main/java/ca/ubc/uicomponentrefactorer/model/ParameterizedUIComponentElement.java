package ca.ubc.uicomponentrefactorer.model;

import java.util.List;

public class ParameterizedUIComponentElement extends UIComponentElement {

    private ParameterizationReason parameterizationReason;

    public ParameterizedUIComponentElement(UIComponentElement parent,
                                           List<String> correspondingOriginalNodes,
                                           String name,
                                           ParameterizationReason parameterizationReason) {
        super(parent, correspondingOriginalNodes, name);

        this.parameterizationReason = parameterizationReason;
    }
}
