package ca.ubc.uicomponentrefactorer.adaptingstrategies.adaptingstrategies.webcomponents;

import ca.ubc.uicomponentrefactorer.adaptingstrategies.model.UIComponent;
import org.w3c.dom.Document;

import javax.annotation.Nullable;

public abstract class AdaptingStrategy {

    public abstract Document adapt(UIComponent uiComponent);

    public abstract boolean supportsAttributeParameterization();
}
