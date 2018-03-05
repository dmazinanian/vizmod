package ca.ubc.uicomponentrefactorer.adaptingstrategies;

import ca.ubc.uicomponentrefactorer.model.UIComponent;
import org.w3c.dom.Document;

public abstract class AdaptingStrategy {

    public abstract Document adapt(UIComponent uiComponent);

    public abstract boolean supportsAttributeParameterization();

}
