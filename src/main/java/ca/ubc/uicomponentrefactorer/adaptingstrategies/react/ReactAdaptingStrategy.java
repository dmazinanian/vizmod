package ca.ubc.uicomponentrefactorer.adaptingstrategies.react;

import ca.ubc.uicomponentrefactorer.adaptingstrategies.AdaptingStrategy;
import ca.ubc.uicomponentrefactorer.adaptingstrategies.webcomponents.HTMLCustomElement;
import ca.ubc.uicomponentrefactorer.model.NonParameterizedUIComponentElement;
import ca.ubc.uicomponentrefactorer.model.ParameterizedUIComponentElement;
import ca.ubc.uicomponentrefactorer.model.UIComponent;
import ca.ubc.uicomponentrefactorer.model.UIComponentElement;
import ca.ubc.uicomponentrefactorer.util.DocumentUtil;
import ca.ubc.uicomponentrefactorer.util.ResourcesUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.html.dom.HTMLDivElementImpl;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLScriptElementImpl;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLDivElement;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLScriptElement;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class ReactAdaptingStrategy extends AdaptingStrategy {

    private static final String[] REACT_JS_FILES = new String[] {
            "https://unpkg.com/react@16/umd/react.development.js",
            "https://unpkg.com/react-dom@16/umd/react-dom.development.js",
            "https://unpkg.com/babel-standalone@6.15.0/babel.min.js"
    };

    private static final String PARAMETERIZED_TREES_JS_OBJECT_NAME = "parameterizedTrees";

    private static final String REACT_COMPONENT_JS =
            "ca/ubc/uicomponentrefactorer/adaptingstrategies/react/react-component-template.jsx";

    private static final String REACT_COMPONENT_TEMPLATE_CLASS_NAME = "${componentClassName}";
    private static final String RECT_COMPONENT_TEMPLATE_BODY = "${componentBody}";
    private static final String REACT_COMPONENT_ATTRIBUTES = "${componentParameterizedTrees}";

    @Override
    public Document adapt(UIComponent uiComponent) {

        HTMLDocumentImpl newDocument = (HTMLDocumentImpl) uiComponent.getOriginalDocument().cloneNode(true);

        // Insert React JS files
        insertReactJSFilesIntoHeader(newDocument);

        MultiValuedMap<Integer, Node> parameterizedTrees = new ArrayListValuedHashMap<>();
        // Root node of the React component.
        HTMLDivElement reactComponentHTMLRootElement = new HTMLDivElementImpl(newDocument, "div");
        populateReactComponent(newDocument, uiComponent, uiComponent, reactComponentHTMLRootElement, parameterizedTrees);

        List<String> originalNodesXPaths = uiComponent.getChildren().get(0).getCorrespondingOriginalNodesXPaths();
        List<Node> originalRootNodes = originalNodesXPaths.stream()
                .map(xpath -> DocumentUtil.queryDocument(newDocument, xpath).item(0))
                .collect(Collectors.toList());

        StringBuffer parameterizedTreeStringArrayValues = new StringBuffer();
        for (int originalRootNodeIndex = 0; originalRootNodeIndex < originalNodesXPaths.size(); originalRootNodeIndex++) {
            Node originalNode = originalRootNodes.get(originalRootNodeIndex);
            HTMLElement newNode = new HTMLCustomElement(newDocument, uiComponent.getName());
            newNode.setAttribute("id", uiComponent.getName() + originalRootNodeIndex);
            originalNode.getParentNode().replaceChild(newNode, originalNode);

            parameterizedTreeStringArrayValues.append("{");
            List<Node> parameterizedTreesRootNodes = (List<Node>) parameterizedTrees.get(originalRootNodeIndex);
            for (int parameterizedNodeIndex = 0;
                 parameterizedNodeIndex < parameterizedTreesRootNodes.size();
                 parameterizedNodeIndex++) {
                parameterizedTreeStringArrayValues
                        .append("\"")
                        .append(PARAMETERIZED_TREES_JS_OBJECT_NAME + parameterizedNodeIndex)
                        .append("\"")
                        .append(":")
                        .append(getNodeStringForReact(parameterizedTreesRootNodes.get(parameterizedNodeIndex)));
                if (parameterizedNodeIndex < parameterizedTreesRootNodes.size() - 1) {
                    parameterizedTreeStringArrayValues.append(",");
                }
            }
            parameterizedTreeStringArrayValues.append("}");
            if (originalRootNodeIndex < originalNodesXPaths.size() - 1) {
                parameterizedTreeStringArrayValues.append(",\n");
            }
        }

        // Wrap the component in the corresponding JavaScript
        String reactComponentTemplate = ResourcesUtil.readResourceFileToString(REACT_COMPONENT_JS);
        String reactComponentText = reactComponentTemplate
                .replace(REACT_COMPONENT_TEMPLATE_CLASS_NAME, uiComponent.getName())
                .replace(RECT_COMPONENT_TEMPLATE_BODY, getNodeStringForReact(reactComponentHTMLRootElement))
                .replace(REACT_COMPONENT_ATTRIBUTES, parameterizedTreeStringArrayValues.toString());

        HTMLScriptElement reactComponentJSElement = new HTMLScriptElementImpl(newDocument, "script");
        reactComponentJSElement.setType("text/babel");
        reactComponentJSElement.setText(reactComponentText);
        newDocument.getElementsByTagName("head").item(0).appendChild(reactComponentJSElement);

        return newDocument;
    }

    private String getNodeStringForReact(Node node) {
        /*
         * We have to use reflection to make tag names lower case.
         * This is because in the HTML standard tag names are capital,
         * while attribute names are lower case.
         * For react, tag names are lower case.
         * Also, we do classname -> className, so we have to force change it.
         * Alternatively, we could parse the nodes in another way,
         * but for now we use this hack.
         */
        Node rootCopy = node.cloneNode(true);
        List<Node> nodes = DocumentUtil.dfs(rootCopy);
        // Replace class attribute with className, since react does not allow it
        for (Node child : nodes) {
            if (child instanceof HTMLElement) {
                HTMLElement htmlElement = (HTMLElement) child;
                setFieldValueViaReflection(htmlElement,"name", htmlElement.getTagName().toLowerCase());
                String classAttrValue = htmlElement.getAttribute("class");
                if (null != classAttrValue && !"".equals(classAttrValue.trim())) {
                    htmlElement.removeAttribute("class");
                    htmlElement.setAttribute("className", classAttrValue);
                    for (int i = 0; i < htmlElement.getAttributes().getLength(); i++) {
                        Node attr = htmlElement.getAttributes().item(i);
                        if ("classname".equals(attr.getNodeName())) {
                            setFieldValueViaReflection(attr,"name", "className");
                        }
                    }
                }
            }
        }
        return DocumentUtil.getElementString(rootCopy);
    }

    private void setFieldValueViaReflection(Object object, String fieldName, Object value) {
        try {
            Class<?> c = object.getClass();
            Field f = getDeclaredField(c, "name");
            f.setAccessible(true);
            f.set(object, value);
        } catch (IllegalAccessException x) {
            x.printStackTrace();
        } catch (IllegalArgumentException x) {
            x.printStackTrace();
        }
    }

    private Field getDeclaredField(Class<?> c, String name) {
        Field f = null;
        do {
            try {
                f = c.getDeclaredField("name");
            } catch (NoSuchFieldException nsf) {}
            c = c.getSuperclass();
        } while (f == null && c != null);
        return f;
    }


    private MultiValuedMap<Integer, Node> populateReactComponent(HTMLDocumentImpl newDocument,
                                                                 UIComponent uiComponent,
                                                                 UIComponentElement rootUIComponentElement,
                                                                 Node htmlElementRootNode,
                                                                 MultiValuedMap<Integer, Node> parameterizedTress) {

        for (UIComponentElement uiComponentChild : rootUIComponentElement.getChildren()) {

            Node newNode = null;
            List<String> correspondingOriginalNodes = uiComponentChild.getCorrespondingOriginalNodesXPaths();
            if (uiComponentChild instanceof NonParameterizedUIComponentElement) {
                String templateTreeNodeXPath = correspondingOriginalNodes.get(uiComponent.getTemplateTreeIndex());
                Node originalTemplateNode = DocumentUtil.queryDocument(newDocument, templateTreeNodeXPath).item(0);
                newNode = originalTemplateNode.cloneNode(false);
            } else if (uiComponentChild instanceof ParameterizedUIComponentElement) {
                // We add a { } in the React component. The
                String newNodeText = "{this.props.%s%s}";
                if (parameterizedTress.keys().size() == 0) {
                    newNodeText = String.format(newNodeText, PARAMETERIZED_TREES_JS_OBJECT_NAME, 0);
                } else {
                    newNodeText = String.format(newNodeText, PARAMETERIZED_TREES_JS_OBJECT_NAME, parameterizedTress.get(0).size());
                }
                newNode = new TextImpl(newDocument, newNodeText);
                for (int i = 0; i < correspondingOriginalNodes.size(); i++) {
                    String originalNodeXPath = correspondingOriginalNodes.get(i);
                    Node originalNode = DocumentUtil.queryDocument(newDocument, originalNodeXPath).item(0);
                    parameterizedTress.put(i, originalNode);
                }
            }
            if (null != newNode) {
                htmlElementRootNode.appendChild(newNode);
                populateReactComponent(newDocument, uiComponent, uiComponentChild, newNode, parameterizedTress);
            }
        }

        return parameterizedTress;

    }

    private boolean attributesAreDifferent(Node templateTreeNode, Node otherTreeNode) {
        HTMLElement templateTreeElement = (HTMLElement) templateTreeNode;
        HTMLElement currentTreeElement = (HTMLElement) otherTreeNode;
        NamedNodeMap templateTreeElementAttrs = templateTreeElement.getAttributes();
        NamedNodeMap currentTreeElementAttrs = currentTreeElement.getAttributes();
        if (templateTreeElementAttrs.getLength() != currentTreeElementAttrs.getLength()) {
            return true;
        } else {
            for (int attrIndex = 0; attrIndex < templateTreeElementAttrs.getLength(); attrIndex++) {
                Attr templateTreeAttr = (Attr) templateTreeElementAttrs.item(attrIndex);
                Attr currentTreeAttribute = (Attr) currentTreeElementAttrs.getNamedItem(templateTreeAttr.getName());
                if (currentTreeAttribute == null ||
                        !templateTreeAttr.getValue().trim().equals(currentTreeAttribute.getValue().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean supportsAttributeParameterization() {
        return true;
    }

    private void insertReactJSFilesIntoHeader(HTMLDocumentImpl newDocument) {

        Node headElement = newDocument.getElementsByTagName("head").item(0);
        for (String jsFile : REACT_JS_FILES) {
            HTMLScriptElement jsScript = new HTMLScriptElementImpl(newDocument, "script");
            jsScript.setSrc(jsFile);
            headElement.appendChild(jsScript);
        }

    }
}
