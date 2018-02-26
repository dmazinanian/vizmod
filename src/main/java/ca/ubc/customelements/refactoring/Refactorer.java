package ca.ubc.customelements.refactoring;

import java.util.*;
import java.util.stream.Collectors;

import ca.concordia.cssanalyser.cssmodel.StyleSheet;
import ca.concordia.cssanalyser.cssmodel.declaration.Declaration;
import ca.concordia.cssanalyser.cssmodel.selectors.*;
import ca.ubc.customelements.browser.AbstractBrowser;
import ca.ubc.customelements.css.CSSUtil;
import ca.ubc.customelements.util.ResourcesUtil;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;
import org.apache.html.dom.HTMLStyleElementImpl;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLElement;

import ca.ubc.customelements.css.CSS;
import ca.ubc.customelements.refactoring.elements.HTMLCustomElement;
import ca.ubc.customelements.refactoring.elements.HTMLSlotElement;
import ca.ubc.customelements.refactoring.elements.HTMLTemplateElement;
import ca.ubc.customelements.util.DocumentUtil;
import org.w3c.dom.html.HTMLStyleElement;

public class Refactorer {
	
	private final List<Node> rootNodes; // Root of the repeated parts
	private final HTMLDocumentImpl originalDocument;
	private final HTMLDocumentImpl newDocument;
	private final String customElementName;
	private final AbstractBrowser browser;
	
	public Refactorer(AbstractBrowser browser, List<String> rootNodeXPaths, String customElementName) {
		this.browser = browser;
		this.originalDocument = (HTMLDocumentImpl) DocumentUtil.toDocument(browser.getDOM());
		this.newDocument = (HTMLDocumentImpl) this.originalDocument.cloneNode(true);
		this.rootNodes = getRootNodesInNewDocument(rootNodeXPaths);
		this.customElementName = customElementName;
	}
	
	private List<Node> getRootNodesInNewDocument(List<String> rootNodeXPaths) {
		List<Node> nodes = new ArrayList<>();
		for (String xpath : rootNodeXPaths) {
			NodeList newNodes = DocumentUtil.queryDocument(newDocument, xpath);
			if (newNodes.getLength() == 0) {
				// TODO: Create appropriate checked exception classes
				throw new RuntimeException(String.format("I can't find %s in the cloned document", xpath));
			} else if (newNodes.getLength() > 1) {
				throw new RuntimeException(String.format("I find multiple %s in the cloned document", xpath));
			}
			nodes.add(newNodes.item(0));
		}
		return nodes;
	}

	public Document refactor() {

		// Body of the new document which is manipulated
		Node newDocumentBody = newDocument.getElementsByTagName("body").item(0);
		
		// Keep track of all the slots that are created
		int slotIndex = 0;
		
		// Keeps all BFSs of all subtrees. BFSs.get(0) == first subtree's nodes in BFS order
		List<List<Node>> BFSs = new ArrayList<>();
		
		// Set of all nodes that are already covered in each subtree. 
		// I use XPath since I'm not so sure about hashCode() and equals() for org.w3c.dom.Node's
		List<Set<String>> coveredNodesXPaths = new ArrayList<>();

		/*
		 * Keep track of the corresponding nodes added to the <template>
		 * i.e., for what element in the template (i.e., smallest) subtree a node in the <template> is created
		 * And the slotted elements
		 */
		Map<String, Node> correspondingNewNodes = new HashMap<>();

		/*
		 * Keep track of the XPaths of the slotted nodes in the new document.
		 * This is needed later for getting CSS after refactoring
		 */
		Map<String, String> slottedElementsNewXPaths = new HashMap<>();
		
		// Nodes that need to be removed from the new document
		Map<String, Node> nodesToRemove = new HashMap<>(); // key is xpaths
		
		/*
		 * Slotted nodes that will remain in the original subtree
		 * but are added to the custom element's root.
		 * Keys are slot names, values are nodes
		 */
		List<Map<String, Node>> slottedElements = new ArrayList<>();

		/*
		 * Since we will add the elements under the custom elements' Shadow DOM,
		 * all the common CSS corresponding to the common elements should be added
		 * to the custom element
		 */
		Map<String, List<CSS>> cssForTemplateElements = new LinkedHashMap<>();

		/*
		 * The original CSS for the slotted elements
		 */
		Map<String, List<CSS>> slottedElementsOriginalCSS = new HashMap<>();
		
		// Template subtree. For now, the smallest one.
		Node templateTree = getSmallestTree();
		int templateTreeIndex = -1;
		for (int i = 0; i < rootNodes.size(); i++) {
			if (rootNodes.get(i) == templateTree) {
				templateTreeIndex = i;
			}
			// Leave out empty text nodes (e.g., white spaces)
			// TODO: double check this. white spaces are sometimes important.
			BFSs.add(DocumentUtil.bfs(rootNodes.get(i), true));
			
			// Initialize empty sets/lists for all subtrees
			coveredNodesXPaths.add(new HashSet<>());
			slottedElements.add(new HashMap<>());
		}
		if (templateTreeIndex == -1) {
			// Unlikely to happen
			// TODO What should we do here though?
			throw new RuntimeException("There is no template tree");
		}

		// The <template> node. One for each custom element
		HTMLTemplateElement templateElement = new HTMLTemplateElement(newDocument);
		// The id is used in JS to add the template to the shadow root for each custom element
		templateElement.setAttribute("id", customElementName + "-template");
		newDocumentBody.appendChild(templateElement);

		/*
		 * We start from root nodes and parameterize the whole root when necessary.
		 * This is because some child nodes are dependent on the root node,
		 * e.g. <tr> and <li> cannot be replaced by a custom element.
		 */
		// BFSs of the template subtree
		List<Node> templateTreeBFS = BFSs.get(templateTreeIndex);
		for (int nodeIndex = 0; nodeIndex < templateTreeBFS.size(); nodeIndex++) {
			Node templateTreeNode = templateTreeBFS.get(nodeIndex);
			String templateTreeNodeXPath = DocumentUtil.getXPathExpression(templateTreeNode);

			if (!coveredNodesXPaths.get(templateTreeIndex).contains(templateTreeNodeXPath)) {

				boolean shouldParameterize = false;
				for (int currentTreeIndex = 0; currentTreeIndex < rootNodes.size(); currentTreeIndex++) {
					// For each subtree, check whether parameterization is necessary
					if (currentTreeIndex != templateTreeIndex) { // Don't compare template tree with itself
						List<Node> currentTreeBFS = BFSs.get(currentTreeIndex);
						Node currentTreeNode = currentTreeBFS.get(nodeIndex);
						String currentTreeNodeXPath = DocumentUtil.getXPathExpression(currentTreeNode);

						if (!coveredNodesXPaths.get(currentTreeIndex).contains(currentTreeNodeXPath)) {
							
							// Condition 1: If node types are different, then parameterize
							if (!templateTreeNode.getNodeName().equals(currentTreeNode.getNodeName())) {
								shouldParameterize = true;
								break; // No need to check other subtrees
							} else {
								// Condition 2: If we are dealing with text nodes and the texts are different, then parameterize
								if (templateTreeNode instanceof TextImpl) {
									// TODO We might want to parameterize the parent, let's say if we have <span>
									TextImpl templateTreeText = (TextImpl) templateTreeNode;
									TextImpl currentTreeText = (TextImpl) currentTreeNode;
									if (!templateTreeText.getTextContent().equals(currentTreeText.getTextContent())) {
										shouldParameterize = true;
										break; // No need to check other subtrees
									}
								} else {
									// The following conditions can be checked if this is not a text node:
									
									// Condition 3: If the children's sizes are different, then parameterize
									NodeList templateChildNodes = templateTreeNode.getChildNodes();
									NodeList currentTreeChildNodes = currentTreeNode.getChildNodes();
									if (templateChildNodes.getLength() != currentTreeChildNodes.getLength()) {
										shouldParameterize = true;
										break; // No need to check other subtrees
									}

									// Condition 4: If attributes are different, then parameterize
									HTMLElement templateTreeElement = (HTMLElement) templateTreeNode;
									HTMLElement currentTreeElement = (HTMLElement) currentTreeNode;
									NamedNodeMap templateTreeElementAttrs = templateTreeElement.getAttributes();
									NamedNodeMap currentTreeElementAttrs = currentTreeElement.getAttributes();
									if (templateTreeElementAttrs.getLength() != currentTreeElementAttrs.getLength()) {
										shouldParameterize = true;
										break; // No need to check other subtrees
									} else {
										for (int attrIndex = 0; attrIndex < templateTreeElementAttrs.getLength(); attrIndex++) {
											Attr templateTreeAttr = (Attr) templateTreeElementAttrs.item(attrIndex);
											Attr currentTreeAttribute =
													(Attr) currentTreeElementAttrs.getNamedItem(templateTreeAttr.getName());
											if (currentTreeAttribute == null || 
													!templateTreeAttr.getValue().trim().equals(currentTreeAttribute.getValue().trim())) {
												shouldParameterize = true;
												break; // No need to check for other attributes
											}
										}
										if (shouldParameterize) {
											break; // No need to check for other subtrees
										}
									}

									// Condition 5: CSS
									// No need for inline CSS, it is checked in the attributes
									List<CSS> cssForTemplateNode = CSSUtil.getExternalAndEmbeddedCSS(browser, templateTreeNodeXPath);
									List<CSS> currentNodeOriginalCSS = CSSUtil.getExternalAndEmbeddedCSS(browser, currentTreeNodeXPath);
									if (!cssForTemplateNode.equals(currentNodeOriginalCSS)) {
										// TODO: What to do?!
										//shouldParameterize = true;
										//break;
									}
								}
							}
						}
					}
				}

				// Parameterize, or add the same node to the template if parameterization is not needed
				// currentParent keeps track of the hierarchy of the elements within the template
				Node currentParent = 
						correspondingNewNodes.get(DocumentUtil.getXPathExpression(templateTreeNode.getParentNode()));
				if (null == currentParent) {
					currentParent = templateElement;
				}
				if (shouldParameterize) {
					String slotName = "slot" + slotIndex;
					slotIndex++;
					HTMLSlotElement slotElement = new HTMLSlotElement(newDocument, slotName);
					currentParent.appendChild(slotElement);

					for (int currentTreeIndex = 0; currentTreeIndex < rootNodes.size(); currentTreeIndex++) {
						Node currentTreeNode = BFSs.get(currentTreeIndex).get(nodeIndex);

						/*
						 * Mark all nodes rooted in the parameterized node (including the root)
						 * to skip in further iterations.
						 */
						for (Node child : DocumentUtil.bfs(currentTreeNode, true)) {
							coveredNodesXPaths.get(currentTreeIndex).add(DocumentUtil.getXPathExpression(child));
						}

						// Add slotted elements which would be later added to the root of the custom element
						slottedElements.get(currentTreeIndex).put(slotName, currentTreeNode);
					}
					correspondingNewNodes.put(templateTreeNodeXPath, slotElement);
				} else {
					Node clonedNode = templateTreeNode.cloneNode(false);
					currentParent.appendChild(clonedNode);
					correspondingNewNodes.put(templateTreeNodeXPath, clonedNode);
					for (int currentTreeIndex = 0; currentTreeIndex < rootNodes.size(); currentTreeIndex++) {
						Node node = BFSs.get(currentTreeIndex).get(nodeIndex);
						String nodeXPath = DocumentUtil.getXPathExpression(node);
						coveredNodesXPaths.get(currentTreeIndex).add(nodeXPath);
						nodesToRemove.put(nodeXPath, node);
					}
					/*
					 * If the element is going to be added to the template, it should bring
					 * all its CSS along. This is because it will be added to the Shadow DOM
					 * The CSS should be adapted later though
					 */
					if (!(templateTreeNode instanceof TextImpl)) {
						cssForTemplateElements.put(templateTreeNodeXPath, CSSUtil.getExternalAndEmbeddedCSS(browser, templateTreeNodeXPath));
					}
				}
				
			}
		}

		/*
		 * Add CSS to template. These are for the elements under the Shadow DOM
		 * The CSS should be adapted though
		 */
		HTMLStyleElement styleElement = new HTMLStyleElementImpl(newDocument, "style");
		styleElement.setAttribute("type", "text/css");
		StringBuilder cssText = new StringBuilder();
		for (String templateElementXPath : cssForTemplateElements.keySet()) {
			List<CSS> cssForThisNode = cssForTemplateElements.get(templateElementXPath);
			Node correspondingNode = correspondingNewNodes.get(templateElementXPath);
			//Set<Declaration> declarationsToAdd = getDeclarationsFromCSSList(cssForThisNode);
			StyleSheet styleSheet = adaptCSS(templateElementXPath, correspondingNode, cssForThisNode);
			cssText.append(styleSheet.toString()).append(System.lineSeparator());
		}
		styleElement.setTextContent(cssText.toString());
		templateElement.appendChild(styleElement);

		/*
		 * Add the parameterized (slotted) nodes to the custom elements
		 */
		for (int i = 0; i < rootNodes.size(); i++) {
			Node rootNode = rootNodes.get(i);
			// For each root node, we will have one new custom element
			HTMLCustomElement customElement = new HTMLCustomElement(newDocument, customElementName);

			// Add only those CSS properties that would affect the location of the custom element
			// TODO This is very tricky.
			/*List<CSS> cssForRootNode = cssForTemplateElements.entrySet().iterator().next().getValue();
			Set<Declaration> allOriginalDeclarationsForRootNode = getDeclarationsFromCSSList(cssForRootNode);
			Set<Declaration> cssDeclarationsForCustomElement = getDeclarationsForParent(allOriginalDeclarationsForRootNode);
			addCSSDeclarationsToElementStyle(customElement, cssDeclarationsForCustomElement);
			allOriginalDeclarationsForRootNode.removeAll(cssDeclarationsForCustomElement);*/

			// Replace the root node with the custom element. The root node is under the custom element now
			rootNode.getParentNode().replaceChild(customElement, rootNode);

			// Adding the slotted (i.e., parameterized) nodes to the root of the custom element (light dom)
			for (String slotName : slottedElements.get(i).keySet()) {
				Node originalNodeToAddToThisRoot = slottedElements.get(i).get(slotName);

				Node newNodeToAdd;
				if (originalNodeToAddToThisRoot instanceof TextImpl) {
					// TODO might change this later, according to Condition 2
					// Text nodes cannot be parameterized with slot. They have to be inside another node.
					newNodeToAdd = new HTMLElementImpl(newDocument, "span");
					newNodeToAdd.appendChild(originalNodeToAddToThisRoot.cloneNode(true));
				} else {
					newNodeToAdd = originalNodeToAddToThisRoot.cloneNode(true);
				}
				// Set the corresponding slot to the parent slotted node
				((HTMLElement)newNodeToAdd).setAttribute("slot", slotName);

				customElement.appendChild(newNodeToAdd); // Already deeply cloned

				if (!(originalNodeToAddToThisRoot instanceof TextImpl)) {
					// Get the original CSS for this slotted element with it's children
					List<Node> originalNodeToBeAddedBFS =
							DocumentUtil.bfs(originalNodeToAddToThisRoot, true);
					// Also, find the mapping new nodes int custom element. They have the same index in the traversal
					List<Node> newNodeToBeAddedBFS =
							DocumentUtil.bfs(newNodeToAdd, true);
					for (int originalNodeBFSIndex = 0; originalNodeBFSIndex < originalNodeToBeAddedBFS.size(); originalNodeBFSIndex++) {
						Node originalChildNode = originalNodeToBeAddedBFS.get(originalNodeBFSIndex);
						Node correspondingNewChildNode = newNodeToBeAddedBFS.get(originalNodeBFSIndex);
						if (!(originalChildNode instanceof TextImpl)) {
							String originalChildXPath = DocumentUtil.getXPathExpression(originalChildNode);
							String newChildXPath = DocumentUtil.getXPathExpression(correspondingNewChildNode);
							List<CSS> appliedCSS = CSSUtil.getExternalAndEmbeddedCSS(browser, originalChildXPath);
							slottedElementsOriginalCSS.put(originalChildXPath, appliedCSS);
							slottedElementsNewXPaths.put(originalChildXPath, newChildXPath);
						}
					}
				}

			}

		}

		// TODO: What to do with custom element's CSS?

		/*
		 * Remove nodes:
		 * The removal should happen in the end, since the structure of the document keeps changing,
		 * so the XPath locators will not locate to the designated nodes properly
		 */
		for (String nodeXPath : nodesToRemove.keySet()) {
			Node node = nodesToRemove.get(nodeXPath);
			try {
				if (null != node.getParentNode()) {
					node.getParentNode().removeChild(node);
				}
			} catch (DOMException domException) {
				domException.printStackTrace();
			}
		}

		/*HTMLScriptElement javascriptHTMLElement = new HTMLScriptElementImpl(newDocument, "script");
		javascriptHTMLElement.setAttribute("type", "text/javascript");
		String customElementRegisterScript = ResourcesUtil.readResourceFileToString(ResourcesUtil.CUSTOM_ELEMENT_REGISTER_JS);
		javascriptHTMLElement.setText(String.format(customElementRegisterScript,
				customElementName,
				customElementName + "-template"));
		newDocumentBody.appendChild(javascriptHTMLElement);*/

		replaceDocumentBodyInTheBrowser(newDocument);

		/*
		 * Add JavaScript that defines the custom element to the <head>
		 * This is because adding to the body did not work (the JS will not
		 * be evaluated)
		 */
		String customElementRegisterScript = ResourcesUtil.readResourceFileToString(ResourcesUtil.CUSTOM_ELEMENT_REGISTER_JS);
		customElementRegisterScript = String.format(customElementRegisterScript, customElementName, customElementName + "-template");
		addScriptToHeadInTheBrowser(customElementRegisterScript);

		/*
		 * Check how the new CSS for slotted elements look like
		 * In case of difference, add as inline CSS
		 */
		for (String slottedElementOriginalXPath : slottedElementsNewXPaths.keySet()) {
			String slottedElementNewXPath = slottedElementsNewXPaths.get(slottedElementOriginalXPath);
			List<CSS> originalCSSForSlottedElement = slottedElementsOriginalCSS.get(slottedElementOriginalXPath);
			// In the browser, now we have the new document loaded, so the new XPaths will work to get CSS
			List<CSS> newCSSForSlottedElement = CSSUtil.getExternalAndEmbeddedCSS(browser, slottedElementNewXPath);
			if (!originalCSSForSlottedElement.equals(newCSSForSlottedElement)) {
				// Only add what is different. Apply from the original of course
				Set<Declaration> allOriginalDeclarations = getDeclarationsFromCSSList(originalCSSForSlottedElement);
				Set<Declaration> allNewDeclarations = getDeclarationsFromCSSList(newCSSForSlottedElement);
				Set<Declaration> declarationsToAdd = new LinkedHashSet<>();
				for (Declaration originalDeclaration : allOriginalDeclarations) {
					boolean foundEquivalentDeclaration = false;
					for (Declaration newDeclaration : allNewDeclarations) {
						if (newDeclaration.declarationIsEquivalent(originalDeclaration)) {
							foundEquivalentDeclaration = true;
							break;
						}
					}
					if (!foundEquivalentDeclaration) {
						declarationsToAdd.add(originalDeclaration);
					}
				}
				Node slottedElementInTheNewDocument =
						DocumentUtil.queryDocument(newDocument, slottedElementNewXPath).item(0);
				addCSSDeclarationsToElementStyle(slottedElementInTheNewDocument, declarationsToAdd);
			}
		}

		// Should do it again to make sure that the new CSS stuff are coming trough
		replaceDocumentBodyInTheBrowser(newDocument);

		return newDocument;
		
	}

	private StyleSheet adaptCSS(String originalNodeXPath, Node correspondingNode, List<CSS> cssForThisNode) {
		StyleSheet styleSheetToReturn = new StyleSheet();
		for (CSS css : cssForThisNode) {
			if (css.getSource() != CSS.CSSSource.INLINE_CSS) {
				Selector originalSelector = css.getSelector();
				Selector adaptedSelector;
				if (getRootNodeXPaths().contains(originalNodeXPath)) {
					adaptedSelector = CSSUtil.newSelectorFromString(":host");
				} else {
 					adaptedSelector = getAdaptedSelector(originalSelector, correspondingNode);
				}
				boolean selectorFound = false;
				for (Selector existingSelector : styleSheetToReturn.getAllSelectors()) {
					if (adaptedSelector.selectorEquals(existingSelector)) {
						adaptedSelector = existingSelector;
						selectorFound = true;
						break;
					}
				}
				for (Declaration declaration : originalSelector.getDeclarations()) {
					adaptedSelector.addDeclaration(declaration);
				}
				if (!selectorFound) {
					styleSheetToReturn.addSelector(adaptedSelector);
				}
			}
		}
		return styleSheetToReturn;
	}

	private Selector getAdaptedSelector(Selector originalSelector, Node correspondingNode) {
		if (originalSelector instanceof GroupingSelector) {
			GroupingSelector groupingSelector = (GroupingSelector) originalSelector;
			for (BaseSelector baseSelector : groupingSelector) {
				Selector adaptedSelector = getAdaptedBaseSelector(baseSelector, correspondingNode);
				if (null != adaptedSelector) {
					return adaptedSelector;
				}
			}
		} else if (originalSelector instanceof BaseSelector) {
			return getAdaptedBaseSelector((BaseSelector) originalSelector, correspondingNode);
		}
		return null;
	}

	private Selector getAdaptedBaseSelector(BaseSelector baseSelector, Node correspondingNode) {
		if (CSSUtil.matches(baseSelector, correspondingNode)) {
			return baseSelector;
		} else {
			if (baseSelector instanceof Combinator) {
				Combinator combinator = (Combinator) baseSelector;
				if (combinator instanceof DescendantSelector) {
					DescendantSelector descendantSelector = (DescendantSelector) combinator;
					return getAdaptedSelector(descendantSelector.getChildSelector(), correspondingNode);
				} else if (combinator instanceof SiblingSelector) {
					throw new RuntimeException("What should we do?");
				}
			} else if (baseSelector instanceof SimpleSelector) { // This sounds redundant
				if (CSSUtil.matches(baseSelector, correspondingNode)) {
					return baseSelector;
				}
			}
		}
		return null;
	}

	/**
	 * This method returns the XPath of the root nodes, but using our own XPath generator.
	 * This is because the passed XPaths could be very different from what we generate.
	 */
	private Set<String> getRootNodeXPaths() {
		Set<String> rootNodeXPaths = new LinkedHashSet<>();
		for (Node node : rootNodes) {
			rootNodeXPaths.add(DocumentUtil.getXPathExpression(node));
		}
		return rootNodeXPaths;
	}

	private Set<Declaration> getDeclarationsForParent(Set<Declaration> allOriginalDeclarationsForRootNode) {
		return allOriginalDeclarationsForRootNode.stream().
				filter(declaration -> CSSUtil.propertyShouldBeAppliedToParent(declaration.getProperty())).
				collect(Collectors.toSet());
	}

	private Set<Declaration> getDeclarationsFromCSSList(List<CSS> cssForThisNode) {
		Set<Declaration> declarationsToAdd = new HashSet<>();
		for (CSS css : cssForThisNode) {
            for (Declaration declaration : css.getDeclarations()) {
                declarationsToAdd.add(declaration);
            }
        }
		return declarationsToAdd;
	}

	private void addCSSDeclarationsToElementStyle(Node node, Set<Declaration> declarationsToAdd) {
		StringBuilder cssTextBuilder = new StringBuilder();
		for (Declaration declarationToAdd : declarationsToAdd) {
            cssTextBuilder.append(declarationToAdd.toString()).append(";");
        }
		if (node.hasAttributes() &&
                null != node.getAttributes().getNamedItem("style")) {
            Attr currentStyle = (Attr) node.getAttributes().getNamedItem("style");
            cssTextBuilder = new StringBuilder(currentStyle.getValue()).append(cssTextBuilder);
        }
		((HTMLElement)node).setAttribute("style", cssTextBuilder.toString());
	}

	private Node getSmallestTree() {
		return Collections.min(rootNodes, (node1, node2) -> {
			return Integer.compare(getNumberOfNodes(node1), getNumberOfNodes(node2));
		});
	}
	
	private int getNumberOfNodes(Node node) {
		int numberOfNodes = 1;
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			numberOfNodes += getNumberOfNodes(item);
		}
		return numberOfNodes;
	}

	private void replaceDocumentBodyInTheBrowser(Document document) {
		String body = DocumentUtil.getElementString(document.getElementsByTagName("body").item(0));
		String bodyEscaped = body.replace("\"", "\\\"").replace("\n", "\\n");
		String s = String.format("document.getElementsByTagName(\"body\")[0].innerHTML = \"%s\"", bodyEscaped);
		browser.evaluateJavaScript(s);
	}

	private void addScriptToHeadInTheBrowser(String code) {
		code = code.replace("\"", "\\\"").replace("\n", "\\n");
		String script = String.format("var script = document.createElement('script');\n" +
						"script.text = \"%s\";\n" +
						"document.head.appendChild(script).parentNode.removeChild( script )",
				code);
		browser.evaluateJavaScript(script);
	}

}
