package ca.ubc.vizmod.adaptingstrategies;

import ca.ubc.vizmod.model.UIComponent;
import org.w3c.dom.Document;

public abstract class AdaptingStrategy {

    /**
     * Returns a new Document to which the given {@link UIComponent} is applied
     * @param uiComponent
     * @return
     */
    public abstract Document adapt(UIComponent uiComponent);

    /**
     * @return True if the {@link AdaptingStrategy} directly supports parameterization of attributes
     */
    public abstract boolean supportsAttributeParameterization();

}