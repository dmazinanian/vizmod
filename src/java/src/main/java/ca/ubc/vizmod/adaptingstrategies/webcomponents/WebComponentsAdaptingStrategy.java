package ca.ubc.vizmod.adaptingstrategies.webcomponents;

import ca.ubc.vizmod.adaptingstrategies.AdaptingStrategy;
import ca.ubc.vizmod.model.NonParameterizedUIComponentElement;
import ca.ubc.vizmod.model.ParameterizedUIComponentElement;
import ca.ubc.vizmod.model.UIComponent;
import ca.ubc.vizmod.model.UIComponentElement;
import ca.ubc.vizmod.refactorer.RefactoringResult;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.ResourcesUtil;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLScriptElementImpl;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLScriptElement;

import java.util.*;

public class WebComponentsAdaptingStrategy extends AdaptingStrategy {

    private static final String CUSTOM_ELEMENT_REGISTER_JS =
            "ca/ubc/uicomponentrefactorer/adaptingstrategies/webcomponents/custom-element-register.js";

    @Override
    public RefactoringResult adapt(UIComponent uiComponent) {

        if (uiComponent.getChildren().size() > 1) {
            throw new RuntimeException("Root has more than one child. How come?");
        }

        final String TEMPLATE_ELEMENT_ID = uiComponent.getName() + "-template";

        Document originalDocument = uiComponent.getOriginalDocument();
        HTMLDocumentImpl newDocument = (HTMLDocumentImpl) originalDocument.cloneNode(true);


        Map<String, ParameterizedUIComponentElement> slotsToParameterizedUIComponentElements = new HashMap<>();

        createTemplateElement(uiComponent, newDocument, slotsToParameterizedUIComponentElements, TEMPLATE_ELEMENT_ID);

        // Replace root nodes with custom elements
        replaceSubtrees(uiComponent, newDocument, slotsToParameterizedUIComponentElements);

        addWebComponentsJavascript(uiComponent, newDocument, TEMPLATE_ELEMENT_ID);

        return new RefactoringResult(newDocument, "", new ArrayList<>());

    }

    private void addWebComponentsJavascript(UIComponent uiComponent,
                                            HTMLDocumentImpl newDocument,
                                            String templateElementID) {

        /*
         * Add JavaScript that defines the custom element to the <head>
         * This is because adding to the body did not work (the JS will not
         * be evaluated)
         */
        String customElementRegisterScript =
                ResourcesUtil.readResourceFileToString(CUSTOM_ELEMENT_REGISTER_JS);
        customElementRegisterScript = String.format(customElementRegisterScript, uiComponent.getName(), templateElementID);
        HTMLScriptElement scriptElement = new HTMLScriptElementImpl(newDocument, "script");
        scriptElement.setText(customElementRegisterScript);
        newDocument.getElementsByTagName("body").item(0).appendChild(scriptElement);

    }

    private void createTemplateElement(UIComponent uiComponent,
                                       HTMLDocumentImpl newDocument,
                                       Map<String, ParameterizedUIComponentElement> slotsToParameterizedUIComponentElements,
                                       String templateElementID) {

        // The <template> node. This resembles the UIComponent structure basically.
        HTMLTemplateElement templateElement = new HTMLTemplateElement(newDocument);
        // The id is used in JS to add the template to the shadow root for each custom element

        templateElement.setAttribute("id", templateElementID);
        newDocument.getElementsByTagName("body").item(0).appendChild(templateElement);

        populateTemplate(newDocument, uiComponent, uiComponent, templateElement, slotsToParameterizedUIComponentElements);

    }

    private void replaceSubtrees(UIComponent uiComponent, HTMLDocumentImpl newDocument,
                                 Map<String, ParameterizedUIComponentElement> slotsToParameterizedUIComponentElements) {

        // Keep track of root nodes <-> custom elements
        Map<String, Node> originalNodesBeforeChanges = new HashMap<>();
        Map<String, HTMLCustomElement> rootsToCustomElements = new HashMap<>();
        List<String> correspondingOriginalNodesXPaths =
                uiComponent.getChildren().get(0).getCorrespondingOriginalNodesXPaths();
        for (int treeIndex = 0; treeIndex < correspondingOriginalNodesXPaths.size(); treeIndex++) {
            String rootXPath = correspondingOriginalNodesXPaths.get(treeIndex);
            Node originalRootNode = DocumentUtil.queryDocument(newDocument, rootXPath).item(0);
            originalNodesBeforeChanges.put(rootXPath, originalRootNode);
            // Create a custom element for each repeated tree stored in the UIComponent
            HTMLCustomElement customElementNode = new HTMLCustomElement(newDocument, uiComponent.getName());
            rootsToCustomElements.put(rootXPath, customElementNode);
            // Add the slots to the root node of this custom element
            for (String slotName : slotsToParameterizedUIComponentElements.keySet()) {
                UIComponentElement componentElementForThisSlot = slotsToParameterizedUIComponentElements.get(slotName);
                String slottedElementXPath = componentElementForThisSlot.getCorrespondingOriginalNodesXPaths().get(treeIndex);
                Node slottedElement = DocumentUtil.queryDocument(newDocument, slottedElementXPath).item(0).cloneNode(true);
                if (slottedElement instanceof TextImpl) { // Wrap a different text into a span
                    TextImpl textElement = (TextImpl) slottedElement;
                    slottedElement = new HTMLCustomElement(newDocument,  "span");
                    slottedElement.setTextContent(textElement.getTextContent());
                }
                ((HTMLElement) slottedElement).setAttribute("slot", slotName);
                customElementNode.appendChild(slottedElement);
            }
        }
        for (String xpath : originalNodesBeforeChanges.keySet()) {
            Node originalRootNode = originalNodesBeforeChanges.get(xpath);
            HTMLCustomElement customElementNode = rootsToCustomElements.get(xpath);
            originalRootNode.getParentNode().replaceChild(customElementNode, originalRootNode);
        }

    }

    private void populateTemplate(HTMLDocumentImpl newDocument,
                                  UIComponent rootUIComponent,
                                  UIComponentElement currentUIComponentElement,
                                  Node htmlNodeRoot,
                                  Map<String, ParameterizedUIComponentElement> slotsToParameterizedNodes) {

        for (UIComponentElement uiComponentElementChild : currentUIComponentElement.getChildren()) {
            Node newNode = null;
            if (uiComponentElementChild instanceof NonParameterizedUIComponentElement) {
                List<String> correspondingOriginalNodes = uiComponentElementChild.getCorrespondingOriginalNodesXPaths();
                String templateTreeNodeXPath = correspondingOriginalNodes.get(rootUIComponent.getTemplateTreeIndex());
                Node originalNode = DocumentUtil.queryDocument(newDocument, templateTreeNodeXPath).item(0);
                newNode = originalNode.cloneNode(false);
            } else if (uiComponentElementChild instanceof ParameterizedUIComponentElement) {
                String slotName = "slot" + slotsToParameterizedNodes.size();
                HTMLSlotElement slotElement = new HTMLSlotElement(newDocument, slotName);
                slotsToParameterizedNodes.put(slotName, (ParameterizedUIComponentElement) uiComponentElementChild);
                newNode = slotElement;
            }
            if (null != newNode) {
                htmlNodeRoot.appendChild(newNode);
                populateTemplate(newDocument, rootUIComponent, uiComponentElementChild, newNode, slotsToParameterizedNodes);
            }
        }

    }

    @Override
    public boolean supportsAttributeParameterization() {
        return false;
    }

}