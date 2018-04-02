package ca.ubc.vizmod.adaptingstrategies.react;

import ca.concordia.cssanalyser.cssmodel.declaration.Declaration;
import ca.ubc.vizmod.adaptingstrategies.AdaptingStrategy;
import ca.ubc.vizmod.adaptingstrategies.webcomponents.HTMLCustomElement;
import ca.ubc.vizmod.model.*;
import ca.ubc.vizmod.model.css.CSSUtil;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.ResourcesUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLScriptElementImpl;
import org.apache.xerces.dom.TextImpl;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.w3c.dom.*;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLScriptElement;

import java.util.*;
import java.util.stream.Collectors;

public class ReactAdaptingStrategy extends AdaptingStrategy {

    /*
     * Necessary JS files for React, to be added to the <head>
     */
    private static final String[] REACT_JS_FILES = new String[] {
            "https://unpkg.com/react@16/umd/react.development.js",
            "https://unpkg.com/react-dom@16/umd/react-dom.development.js",
            "https://unpkg.com/babel-standalone@6.15.0/babel.min.js"
    };

    /**
     * Attribute names for replacement for React. See renameAttributesForReact
     */
    private static final Map<String, String> REACT_EQUIVALENCE_ATTRIBUTE_NAMES = new HashMap<>();
    {
        REACT_EQUIVALENCE_ATTRIBUTE_NAMES.put("class", "className");
        REACT_EQUIVALENCE_ATTRIBUTE_NAMES.put("itemprop", "itemProp");
        REACT_EQUIVALENCE_ATTRIBUTE_NAMES.put("srcset", "srcSet");
        REACT_EQUIVALENCE_ATTRIBUTE_NAMES.put("itemscope", "itemScope");
        REACT_EQUIVALENCE_ATTRIBUTE_NAMES.put("itemtype", "itemType");
    }

    /**
     * The template for the react component
     */
    private static final String REACT_COMPONENT_JS =
            "ca/ubc/vizmod/adaptingstrategies/react/react-component-template.jsx";
    private static final String PARAMETERIZED_TREES_JS_OBJECT_NAME = "p";
    private static final String REACT_COMPONENT_TEMPLATE_CLASS_NAME = "${componentClassName}";
    private static final String RECT_COMPONENT_TEMPLATE_BODY = "${componentBody}";
    private static final String REACT_COMPONENT_ATTRIBUTES = "${componentParameterizedTrees}";

    @Override
    public Document adapt(UIComponent uiComponent) {

        HTMLDocumentImpl newDocument = (HTMLDocumentImpl) uiComponent.getOriginalDocument().cloneNode(true);

        // Insert React JS files
        insertReactJSFilesIntoHeader(newDocument);

        // This is the list of all elements within the React component where one attribute has JS object as value
        // This list is later used to remove extra double quotes from such values
        List<Node> reactComponentElementsWithObjectValuedAttributes = new ArrayList<>();

        // The map from the index of the root node to the list of elements which are parameterized
        MultiValuedMap<Integer, Node> parameterizedTrees = new ArrayListValuedHashMap<>();
        
        Node reactComponentHTMLRootElement =
                populateReactComponent(newDocument, uiComponent, uiComponent,
                        null, parameterizedTrees, reactComponentElementsWithObjectValuedAttributes);

        List<String> originalNodesXPaths = uiComponent.getChildren().get(0).getCorrespondingOriginalNodesXPaths();
        List<Node> originalRootNodes = originalNodesXPaths.stream()
                .map(xpath -> DocumentUtil.queryDocument(newDocument, xpath).item(0))
                .collect(Collectors.toList());
        /*
         * Object containing the parameterized values for the nodes. This will replace
         * the corresponding placeholder in the component's JS file.
         * This holds an array of objects, each object pertaining to an original element being parameterized.
         */
        StringBuilder parameterizedTreeStringArrayValues = new StringBuilder();

        for (int originalRootNodeIndex = 0; originalRootNodeIndex < originalNodesXPaths.size(); originalRootNodeIndex++) {
            HTMLElement originalNode = (HTMLElement) originalRootNodes.get(originalRootNodeIndex);
            // The original node is replaced by a custom component, and acts as a placeholder for the
            // component's body (used in the React's Render() function)
            // At runtime, this node will be replaced by the original parent.
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
                        .append(getNodeStringForReact(
                                    parameterizedTreesRootNodes.get(parameterizedNodeIndex), true));
                if (parameterizedNodeIndex < parameterizedTreesRootNodes.size() - 1) {
                    parameterizedTreeStringArrayValues.append(",");
                }
            }
            parameterizedTreeStringArrayValues.append("}");
            if (originalRootNodeIndex < originalNodesXPaths.size() - 1) {
                parameterizedTreeStringArrayValues.append(",");
            }
        }

        // Wrap the component in the corresponding JavaScript
        String reactComponentTemplate = ResourcesUtil.readResourceFileToString(REACT_COMPONENT_JS);
        String reactComponentBody = getNodeStringForReact(reactComponentHTMLRootElement, false);
        // Replace double quotes in parameterized attributes with
        reactComponentBody = removeExtraDoubleQuotes(reactComponentBody,
                reactComponentElementsWithObjectValuedAttributes);
        // Replace placeholders in the template
        String reactComponentText = reactComponentTemplate
                .replace(REACT_COMPONENT_TEMPLATE_CLASS_NAME, uiComponent.getName())
                .replace(RECT_COMPONENT_TEMPLATE_BODY, reactComponentBody)
                .replace(REACT_COMPONENT_ATTRIBUTES, parameterizedTreeStringArrayValues.toString());
        // Create a <script> tag
        HTMLScriptElement reactComponentJSElement = new HTMLScriptElementImpl(newDocument, "script");
        reactComponentJSElement.setType("text/babel"); // Necessary for Babel scripts, since one might not support ES6.
        reactComponentJSElement.setText(reactComponentText);
        markHTMLElementAsAddedByVizMod(reactComponentJSElement);
        newDocument.getElementsByTagName("head").item(0).appendChild(reactComponentJSElement);

        return newDocument;
    }

    /**
     * Given a node, this method replaces the value of the style attribute in this element
     * and all its subtree nodes with JS objects, since react only accepts inline CSS using JS objects
     * @param root Root of the subtree where the changes should be done
     * @return The affected elements
     */
    private List<Node> replaceInlineStylesWithJSObjects(HTMLElement root) {
        List<Node> affectedNodes = new ArrayList<>();
        for (Node node : DocumentUtil.dfs(root)) {
            if (node instanceof HTMLElement) {
                HTMLElement htmlElement = (HTMLElement) node;
                NamedNodeMap attributes = htmlElement.getAttributes();
                Attr styleAttribute = (Attr)attributes.getNamedItem("style");
                if (null != styleAttribute) {
                    String value = styleAttribute.getValue();
                    if (!value.trim().startsWith("{") && !value.trim().endsWith("}")) {
                        htmlElement.setAttribute("style",
                                "{" + convertStyleToJSObject(value) + "}");
                        affectedNodes.add(node);
                    }
                }
            }
        }
        return affectedNodes;
    }

    /**
     * Given the value for a style attribute (i.e., inline css), this method returns JS object
     * appropriate to be used with React
     * @param inlineCSS
     * @return
     */
    private String convertStyleToJSObject(String inlineCSS) {
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("{");
        List<Declaration> cssDeclarations = CSSUtil.getCSSDeclarationsFromInlineStyle(inlineCSS);

        for (Iterator<Declaration> iterator = cssDeclarations.iterator(); iterator.hasNext(); ) {
            Declaration cssDeclaration = iterator.next();
            String cssPropertyJSName = CSSUtil.getCSSPropertyJSName(cssDeclaration);
            toReturn.append(cssPropertyJSName)
                    .append(":")
                    .append("'")
                    .append(escapeValueForJSObject(CSSUtil.getValueString(cssDeclaration)))
                    .append("'");
            if (iterator.hasNext()) {
                 toReturn.append(",");
            }

        }
        toReturn.append("}");
        return toReturn.toString();
    }



    /**
     * When parameterizing attributes for react components, after serialization, we should remove double quotes
     * from the attributes in the text. For instance, <span class="{this.props.something}"></span> does not work,
     * but it should become <span class={this.props.something}></span>
     * @param reactComponentBodyString The complete body of the react componen
     * @param reactComponentElementsWithParameterizedAttributes List of elements which have parameterized attributes
     * @return
     */
    private String removeExtraDoubleQuotes(String reactComponentBodyString,
                                           List<Node> reactComponentElementsWithParameterizedAttributes) {
        // This is a simplistic approach, I haven't thought of a more elegant algorithm
        for (Node elementWithParameterizedAttribute : reactComponentElementsWithParameterizedAttributes) {
            String nodeString = getNodeStringForReact(elementWithParameterizedAttribute, false);
            // We need to get only the opening tag part, since that part is going to be replaced
            // Also, the node might have children
            // Assumption: There is no ">" in the attributes :-)
            String openingTagOnly = nodeString.substring(0, nodeString.indexOf(">") + 1);

            String newOpeningTag = openingTagOnly
                    .replace("\"{", "{")
                    .replace("}\"", "}");
            reactComponentBodyString = reactComponentBodyString.replace(openingTagOnly, newOpeningTag);
        }

        return reactComponentBodyString;
    }

    /**
     * Returns a string corresponding to a w3c.dom.node for JSX + React
     * @param node
     * @param quoteStringNodes
     * @return
     */
    private String getNodeStringForReact(Node node, boolean quoteStringNodes) {
        if (quoteStringNodes && node instanceof Text) {
            Text textNode = (Text) node;
            String trimmedContent = textNode.getTextContent().trim();
            if (trimmedContent.startsWith("{") && trimmedContent.endsWith("}")) {
                // JS Object Literals do not need double quotes
               return textNode.getWholeText();
           } else {
               return String.format("\"%s\"", textNode.getWholeText());
           }
        } else {
            String elementXHTMLString =
                    DocumentUtil.getElementXHTMLString(node, ReactAdaptingStrategy::renameAttributesForReact);

            return elementXHTMLString;
        }
    }

    /**
     * JSX does not allow certain attribute property names (e.g., class), and they should be replaced
     * by correct ones.
     * <b>Warning</b> The original order of elements changes
     * @param element JSoup element for which attribute names should be changed
     * @return
     */
    private static Element renameAttributesForReact(Element element) {
        Set<String> attributesToRemove = new HashSet<>();
        for (Attribute attribute : element.attributes()) {
            String key = attribute.getKey();
            String value = attribute.getValue();
            if (REACT_EQUIVALENCE_ATTRIBUTE_NAMES.containsKey(key)) {
                element.attributes().put(REACT_EQUIVALENCE_ATTRIBUTE_NAMES.get(key), value);
                attributesToRemove.add(key);
            }
        }

        attributesToRemove.forEach(attributeKey -> element.attributes().remove(attributeKey));

        element.children().forEach(ReactAdaptingStrategy::renameAttributesForReact);

        return element;
    }

    /**
     * Populates the structure of the react component.
     * In addition, computes and fills some collections needed
     * @param newDocument The new {@link HTMLDocumentImpl} where the {@link UIComponent} will be added
     * @param uiComponent The {@link UIComponent} for which the structure is created
     * @param rootUIComponentElement The root of the current {@link UIComponent}
     * @param htmlElementRootNode The HTML node to which the unified children of rootUIComponentElement  will be added
     * @param parameterizedTrees The MultiValuedMap containing the original nodes being parameterized.
     *                           The key is the tree index, the value is a collection with the original nodes.
     * @param reactComponentElementsWithJSObjectAttributeValues Elements appearing in the body of the component
     *                                                          for which some attributes are parameterized will appear
     *                                                          as JS objects. For these, extra double quotes should be
     *                                                          removed from the final values.
     * @return The root HTML node of the React component's body
     */
    private Node populateReactComponent(HTMLDocumentImpl newDocument,
                                        UIComponent uiComponent,
                                        UIComponentElement rootUIComponentElement,
                                        Node htmlElementRootNode,
                                        MultiValuedMap<Integer, Node> parameterizedTrees,
                                        List<Node> reactComponentElementsWithJSObjectAttributeValues) {

        for (UIComponentElement uiComponentChild : rootUIComponentElement.getChildren()) {

            Node unifyingNode = null;
            List<String> correspondingOriginalNodesXPaths = uiComponentChild.getCorrespondingOriginalNodesXPaths();
            String templateTreeNodeXPath = correspondingOriginalNodesXPaths.get(uiComponent.getTemplateTreeIndex());
            Node originalTemplateNode = DocumentUtil.queryDocument(newDocument, templateTreeNodeXPath).item(0);

            if (uiComponentChild instanceof NonParameterizedUIComponentElement) {
                // The node will directly go to the template
                unifyingNode = originalTemplateNode.cloneNode(false);
            } else if (uiComponentChild instanceof ParameterizedUIComponentElement) {
                ParameterizedUIComponentElement parameterizedUIComponentElement =
                        (ParameterizedUIComponentElement) uiComponentChild;

                if (parameterizedUIComponentElement.getParameterizationReason()
                        == ParameterizationReason.DIFFERENT_ATTRIBUTE_VALUES) {
                    // Keep the original node, parameterize attributes
                    unifyingNode = originalTemplateNode.cloneNode(false);
                    // Get the differences in attributes
                    MultiValuedMap<String, String> attributeDifferences =
                        getAttributeDifferences(parameterizedUIComponentElement.getCorrespondingOriginalNodesXPaths(), newDocument);
                    for (String attributeKey : attributeDifferences.keySet()) {
                        // For each varying attribute, make one parameter in the corresponding elements
                        List<String> valuesForThisKey = (List<String>)attributeDifferences.get(attributeKey);
                        // Parameterized attributes/tags are put into { } s
                        String parameterName = getParameterName(parameterizedTrees);
                        for (int i = 0; i < correspondingOriginalNodesXPaths.size(); i++) { // The same as valuesForThisKey.size()
                            String valueForThisKeyAndTree = valuesForThisKey.get(i);
                            // JSX does not work with inline styles in the text format, should be converted to JS objects
                            if ("style".equals(attributeKey)) {
                                valueForThisKeyAndTree = convertStyleToJSObject(valueForThisKeyAndTree);
                            }
                            Text placeHolderForValue = new TextImpl(newDocument, escapeValueForJSObject(valueForThisKeyAndTree));
                            parameterizedTrees.put(i, placeHolderForValue);
                        }
                        // The corresponding attribute would be a parameter
                        // We are sure about the casting, because of DIFFERENT_ATTRIBUTE_VALUES
                        ((HTMLElement) unifyingNode).setAttribute(attributeKey, parameterName);
                    }
                    reactComponentElementsWithJSObjectAttributeValues.add(unifyingNode);
                } else {
                    // The entire node (and its subtree) should be parameterized
                    String parameterName = getParameterName(parameterizedTrees);
                    unifyingNode = new TextImpl(newDocument, parameterName);
                    for (int i = 0; i < correspondingOriginalNodesXPaths.size(); i++) {
                        String originalNodeXPath = correspondingOriginalNodesXPaths.get(i);
                        Node originalNode = DocumentUtil.queryDocument(newDocument, originalNodeXPath).item(0);
                        Node newNode = originalNode.cloneNode(true);
                        if (newNode instanceof Text) {
                            newNode = escapeTextNodeForReactObject((Text) newNode);
                        }
                        parameterizedTrees.put(i, newNode);
                    }
                }
            }
            if (null != unifyingNode) {
                // JSX does not work with inline styles in the text format, should be converted to JS objects
                if (unifyingNode instanceof HTMLElement) {
                    replaceInlineStylesWithJSObjects((HTMLElement) unifyingNode);
                }
                if (null == htmlElementRootNode) {
                    htmlElementRootNode = unifyingNode; // Just the first time
                } else {
                    htmlElementRootNode.appendChild(unifyingNode);
                }
                populateReactComponent(newDocument, uiComponent, uiComponentChild,
                        unifyingNode, parameterizedTrees, reactComponentElementsWithJSObjectAttributeValues);
            } else {
                System.out.println("The unifying node is null. I'm not sure how this can happen.");
            }

        }

        return htmlElementRootNode;

    }

    /**
     * Given a string, escapes it so that it can be used as a value in a JS object literal.
     * Replaces " with \", and \n with \\n
     * @param value The string to escape
     * @return The escaped string
     */
    private String escapeValueForJSObject(String value) {
        return value.replace("\"", "\\\"")
                    .replace("\n", "\\\n");
    }

    /**
     * Escapes a text node's value to put in JS object
     */
    private Node escapeTextNodeForReactObject(Text textNode) {
        String wholeText = textNode.getWholeText();
        textNode.setTextContent(escapeValueForJSObject(wholeText));
        return  textNode;
    }

    /**
     * Gets the name of the parameter used in React component, given the parameterized nodes so far
      */
    private String getParameterName(MultiValuedMap<Integer, Node> parameterizedTress) {
        String parameterName = "{this.props.%s%s}";
        if (parameterizedTress.keySet().size() == 0) {
            parameterName = String.format(parameterName, PARAMETERIZED_TREES_JS_OBJECT_NAME, 0);
        } else {
            parameterName = String.format(parameterName, PARAMETERIZED_TREES_JS_OBJECT_NAME, parameterizedTress.get(0).size());
        }
        return parameterName;
    }

    /**
     * Given the XPath of the original nodes and the corresponding document, This method returns the difference in
     * attributes.
     * @param originalNodesXPaths The XPaths of the original nodes.
     * @param document The document to which the original nodes belong.
     * @return A {@link MultiValuedMap}, wehre the key is the attribute name, and the value is a collection where
     *         each item is the value of the attribute, sorted based on the tree indices
     */
    private MultiValuedMap<String, String> getAttributeDifferences(List<String> originalNodesXPaths, Document document) {

        MultiValuedMap<String, String> attributeDifferences = new ArrayListValuedHashMap<>();

        List<HTMLElement> originalNodes = originalNodesXPaths.stream()
                .map(xpath -> DocumentUtil.queryDocument(document, xpath).item(0))
                .map(node -> (HTMLElement) node)
                .collect(Collectors.toList());

        // Get the union of all attribute names. This should be tested for all nodes
        Set<String> attributeKeysUnion = getAttributesUnion(originalNodes);

        for (String attributeKey : attributeKeysUnion) {
            List<String> values = new ArrayList<>();
            boolean shouldAdd = false;
            for (int i = 0; i < originalNodesXPaths.size(); i++) {
                HTMLElement element = originalNodes.get(i);
                Node attribute = element.getAttributes().getNamedItem(attributeKey);
                if (null == attribute) {
                    values.add("");
                } else {
                    values.add(attribute.getNodeValue());
                }
                if (values.size() > 1) {
                    if (!Objects.equals(values.get(values.size() - 2), values.get(values.size() - 1))) {
                        shouldAdd = true;
                    }
                }
            }
            if (shouldAdd) {
                attributeDifferences.putAll(attributeKey, values);
            }
        }

        return attributeDifferences;
    }

    /**
     * Returns the union of the attribute names across the given elements
     * @param originalElements The elements of interest
     * @return The union of the attribute names
     */
    private Set<String> getAttributesUnion(List<HTMLElement> originalElements) {
        return originalElements.stream()
                .map(element -> {
                    Set<String> keys = new HashSet<>();
                    NamedNodeMap attributes = element.getAttributes();
                    for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {
                        Attr templateTreeAttr = (Attr) attributes.item(attrIndex);
                        keys.add(templateTreeAttr.getName());
                    }
                    return keys;
                })
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Attaches the JS scripts to the <head></head> of the given document
     * @param document Document to which the new JS files should be attached
     */
    private void insertReactJSFilesIntoHeader(HTMLDocumentImpl document) {

        Node headElement = document.getElementsByTagName("head").item(0);
        for (String jsFile : REACT_JS_FILES) {
            HTMLScriptElement jsScript = new HTMLScriptElementImpl(document, "script");
            jsScript.setSrc(jsFile);
            markHTMLElementAsAddedByVizMod(jsScript);
            headElement.appendChild(jsScript);
        }

    }

    @Override
    public boolean supportsAttributeParameterization() {
        return true;
    }
}
