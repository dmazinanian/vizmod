package ca.ubc.uicomponentrefactorer.adaptingstrategies.model;

import java.util.List;

public class NonParameterizedUIComponentElement extends UIComponentElement {

    public NonParameterizedUIComponentElement(UIComponentElement parent, List<String> mappedNodes, String name) {
        super(parent, mappedNodes, name);
    }

}
