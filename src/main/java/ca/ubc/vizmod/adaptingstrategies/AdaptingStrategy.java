package ca.ubc.vizmod.adaptingstrategies;

import ca.ubc.vizmod.model.UIComponent;
import ca.ubc.vizmod.refactorer.RefactoringResult;
import org.w3c.dom.html.HTMLElement;

public abstract class AdaptingStrategy {

    /**
     * Returns a new Document to which the given {@link UIComponent} is applied
     * @param uiComponent
     * @return
     */
    public abstract RefactoringResult adapt(UIComponent uiComponent);

    /**
     * @return True if the {@link AdaptingStrategy} directly supports parameterization of attributes
     */
    public abstract boolean supportsAttributeParameterization();

    public void markHTMLElementAsAddedByVizMod(HTMLElement element) {
        element.setAttribute("data", "VizMod");
    }

}
