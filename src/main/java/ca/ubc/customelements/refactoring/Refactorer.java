package ca.ubc.customelements.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;
import org.apache.html.dom.HTMLScriptElementImpl;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLElement;
import org.w3c.dom.html.HTMLScriptElement;

import ca.ubc.customelements.refactoring.elements.HTMLCustomElement;
import ca.ubc.customelements.refactoring.elements.HTMLSlotElement;
import ca.ubc.customelements.refactoring.elements.HTMLTemplateElement;
import ca.ubc.customelements.util.DocumentUtil;
import ca.ubc.customelements.util.IOUtil;

public class Refactorer {
	
	private final List<Node> nodes; // Root of the repeated parts
	private final HTMLDocumentImpl originalDocument;
	private final HTMLDocumentImpl newDocument;
	private final String customElementName;
	
	public Refactorer(HTMLDocumentImpl ownerDocument, List<Node> rootNodes, String customElementName) {
		this.originalDocument = ownerDocument;
		this.newDocument = (HTMLDocumentImpl) this.originalDocument.cloneNode(true);
		this.nodes = getRootNodesInNewDocument(rootNodes);
		this.customElementName = customElementName;
	}
	
	private List<Node> getRootNodesInNewDocument(List<Node> rootNodes) {
		List<Node> nodes = new ArrayList<>();
		for (Node node : rootNodes) {
			String xpath = DocumentUtil.getXPathExpression(node);
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
		// I use XPatsh since I'm not so sure about hashCode() and equals() for org.w3c.dom.Node's
		List<Set<String>> coveredNodesXPaths = new ArrayList<>();
		
		// Nodes that need to be removed from the new document
		Map<String, Node> nodesToRemove = new HashMap<>(); // key is xpaths
		
		// Slotted nodes that will remain in the original subtree
		// but are added to the custom element's root  
		List<List<Node>> slottedElements = new ArrayList<>();
		
		// Template subtree. For now, the smallest one.
		Node templateTree = getSmallestTree();
		int templateTreeIndex = -1;
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i) == templateTree) {
				templateTreeIndex = i;
			}
			// Leave out empty text nodes (e.g., white spaces)
			// TODO: double check this. white spaces are sometimes important.
			BFSs.add(DocumentUtil.bfs(nodes.get(i), true));
			
			// Initialize empty sets/lists for all subtrees
			coveredNodesXPaths.add(new HashSet<>());
			slottedElements.add(new ArrayList<>());
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
		
		// Keep track of the corresponding nodes added to the <template>.
		// I.e., for what element in the template subtree a node in the <template> is created
		Map<String, Node> correspondingTemplateNodes = new HashMap<>();

		// BFSs of the template subtree
		List<Node> templateTreeBFS = BFSs.get(templateTreeIndex);
		
		for (int nodeIndex = 0; nodeIndex < templateTreeBFS.size(); nodeIndex++) {
			Node templateTreeNode = templateTreeBFS.get(nodeIndex);
			String templateTreeNodeXPath = DocumentUtil.getXPathExpression(templateTreeNode);
			if (!coveredNodesXPaths.get(templateTreeIndex).contains(templateTreeNodeXPath)) {
				boolean shouldParameterize = false;
				for (int currentTreeIndex = 0; currentTreeIndex < nodes.size(); currentTreeIndex++) { 
					// For each subtree, check whether parameterization is necessary
					if (currentTreeIndex != templateTreeIndex) { // Dont' compare template tree with itself
						List<Node> currentTreeBFS = BFSs.get(currentTreeIndex);
						Node currentTreeNode = currentTreeBFS.get(nodeIndex);
						if (!coveredNodesXPaths.get(currentTreeIndex).contains(DocumentUtil.getXPathExpression(currentTreeNode))) {
							
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
								}
							}
						}
					}
				}

				// Parameterize, or add the same node to the template if parameterization is not needed
				// currentParent keeps track of the hierarchy of the elements within the template
				Node currentParent = 
						correspondingTemplateNodes.get(DocumentUtil.getXPathExpression(templateTreeNode.getParentNode()));
				if (null == currentParent) {
					currentParent = templateElement;
				}
				if (shouldParameterize) {
					String slotName = "slot" + slotIndex;
					slotIndex++;
					HTMLSlotElement slotElement = new HTMLSlotElement(newDocument, slotName);
					currentParent.appendChild(slotElement);
					// Mark children to exclude from further mapping
					for (int currentTreeIndex = 0; currentTreeIndex < nodes.size(); currentTreeIndex++) { 
						Node currentTreeNode = BFSs.get(currentTreeIndex).get(nodeIndex);
						List<Node> children = DocumentUtil.bfs(currentTreeNode, true);
						for (Node child : children) {
							coveredNodesXPaths.get(currentTreeIndex).add(DocumentUtil.getXPathExpression(child));
						}
						
						Node currentTreeNodeToAdd;
						if (currentTreeNode instanceof TextImpl) {
							// TODO might change this later, according to Condition 2
							// Text nodes cannot be parameterized with slot. They have to be inside 
							// another node.
							currentTreeNodeToAdd = new HTMLElementImpl(newDocument, "span");
							currentTreeNodeToAdd.appendChild(currentTreeNode.cloneNode(true));
						} else {
							currentTreeNodeToAdd = currentTreeNode.cloneNode(true);
						}
						// Set the corresponding slot
						((HTMLElement)currentTreeNodeToAdd).setAttribute("slot", slotName);
						// Slotted elements should be added to the root of the custom element
						slottedElements.get(currentTreeIndex).add(currentTreeNodeToAdd);
					}
					correspondingTemplateNodes.put(templateTreeNodeXPath, slotElement);
				} else {
					Node clonedNode = templateTreeNode.cloneNode(false);
					currentParent.appendChild(clonedNode);
					correspondingTemplateNodes.put(templateTreeNodeXPath, clonedNode);
					for (int currentTreeIndex = 0; currentTreeIndex < nodes.size(); currentTreeIndex++) { 
						Node node = BFSs.get(currentTreeIndex).get(nodeIndex);
						String nodeXPath = DocumentUtil.getXPathExpression(node);
						coveredNodesXPaths.get(currentTreeIndex).add(nodeXPath);
						nodesToRemove.put(nodeXPath, node);
					}
				}
				
			}
		}
		
		/*
		 * Add the parameterized (slotted) nodes to the custom elements
		 */
		for (int i = 0; i < nodes.size(); i++) {
			Node rootNode = nodes.get(i);
			/*
			 * Replace parent roots with custom element
			 */
			HTMLCustomElement customElement = new HTMLCustomElement(newDocument, customElementName);
			for (Node nodeToAddToThisRoot : slottedElements.get(i)) {
				customElement.appendChild(nodeToAddToThisRoot); // Already deeply cloned
			}
			rootNode.getParentNode().replaceChild(customElement, rootNode);
		}
		
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
		
		/*
		 * Add JavaScript that defines the custom element
		 *  
		 */
		HTMLScriptElement javascriptHTMLElement = new HTMLScriptElementImpl(newDocument, "script");
		javascriptHTMLElement.setAttribute("type", "text/javascript");
		String customElementRegisterScript = IOUtil.readResourceFileToString("custom-element-register.js");
		TextImpl scriptText = new TextImpl(newDocument, String.format(customElementRegisterScript, 
				customElementName,
				customElementName + "-template"));
		javascriptHTMLElement.appendChild(scriptText);
		newDocumentBody.appendChild(javascriptHTMLElement);
		
		return newDocument;
		
	}

	private Node getSmallestTree() {
		return Collections.min(nodes, (node1, node2) -> {
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

}
