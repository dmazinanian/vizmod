package ca.ubc.uicomponentrefactorer.refactorer;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import ca.ubc.uicomponentrefactorer.model.UIComponent;
import ca.ubc.uicomponentrefactorer.model.UIComponentElement;
import ca.ubc.uicomponentrefactorer.util.DocumentUtil;
import ca.ubc.uicomponentrefactorer.adaptingstrategies.AdaptingStrategy;
import ca.ubc.uicomponentrefactorer.browser.AbstractBrowser;
import ca.ubc.uicomponentrefactorer.model.NonParameterizedUIComponentElement;
import ca.ubc.uicomponentrefactorer.model.ParameterizedUIComponentElement;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.*;

import org.w3c.dom.html.HTMLElement;

public class UIComponentRefactorer {
	
	private final Document originalDocument;
	private final AbstractBrowser browser;
	private final List<String> rootNodeXPaths;
	private final List<Node> rootNodes;
	private final String componentName;

	public UIComponentRefactorer(AbstractBrowser browser, List<String> rootNodeXPaths, String componentName) {
		this.browser = browser;
		this.originalDocument = DocumentUtil.toDocument(browser.getDOM());
		this.rootNodes = rootNodeXPaths.stream()
				.map(xpath -> DocumentUtil.queryDocument(originalDocument, xpath).item(0))
				.collect(Collectors.toList());
		this.rootNodeXPaths = rootNodes.stream()
				.map(node -> DocumentUtil.getXPathExpression(node))
				.collect(Collectors.toList());
		this.componentName = componentName;
	}

	public Document refactor(AdaptingStrategy adaptingStrategy) {

		// Keeps all BFSs of all subtrees. BFSs.get(0) == first subtree's nodes in BFS order
		List<List<Node>> BFSs = new ArrayList<>();

		// Set of all nodes that are already covered in each subtree.
		// I use XPath since I'm not so sure about hashCode() and equals() for org.w3c.dom.Node's
		List<Set<String>> coveredNodesXPaths = new ArrayList<>();

		Map<String, UIComponentElement> correspondingStringUIComponentMap = new HashMap<>();


		// Template subtree. For now, the smallest one in terms of number of nodes.
		int templateTreeIndex = getSmallestTreeIndex(this::getNumberOfNodes);
		if (templateTreeIndex == -1) {
			// Unlikely to happen
			// TODO What should we do here though?
			throw new RuntimeException("There is no template tree");
		}
		for (int i = 0; i < rootNodes.size(); i++) {
			// Leave out empty text nodes (e.g., white spaces)
			// TODO: double check this. white spaces are sometimes important.
			BFSs.add(DocumentUtil.bfs(rootNodes.get(i), true));

			// Initialize empty sets/lists for all subtrees
			coveredNodesXPaths.add(new HashSet<>());
		}

		// This is the root of the UIComponent
		UIComponent uiComponent = new UIComponent(componentName, originalDocument, templateTreeIndex);

		/*
		 * We start from root nodes and parameterize the whole root when necessary.
		 * This is because some child nodes are dependent on the root node,
		 * e.g. <tr> and <li> cannot be replaced by a custom element.
		 */
		// BFSs of the template subtree
		List<Node> templateTreeBFS = BFSs.get(templateTreeIndex);
		for (int templateTreeNodeBFSIndex = 0; templateTreeNodeBFSIndex < templateTreeBFS.size(); templateTreeNodeBFSIndex++) {
			Node templateTreeNode = templateTreeBFS.get(templateTreeNodeBFSIndex);
			String templateTreeNodeXPath = DocumentUtil.getXPathExpression(templateTreeNode);

			if (!coveredNodesXPaths.get(templateTreeIndex).contains(templateTreeNodeXPath)) {

				boolean shouldParameterize = false;
				boolean shouldParameterizeAttributes = false;
				for (int currentTreeIndex = 0; currentTreeIndex < rootNodes.size(); currentTreeIndex++) {
					// For each subtree, check whether parameterization is necessary
					if (currentTreeIndex != templateTreeIndex) { // Don't compare template tree with itself
						Node currentTreeNode = BFSs.get(currentTreeIndex).get(templateTreeNodeBFSIndex);
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

									// Condition 4: If attributes are different,
									// React can parameterize them, not Web Components.
									if (attributesAreDifferent(templateTreeNode, currentTreeNode)) {
										shouldParameterize = true;
										if (adaptingStrategy.supportsAttributeParameterization()) {
											shouldParameterizeAttributes = true;
										}
										break;
									}
								}
							}
						}
					}
				}

				UIComponentElement currentParent =
						correspondingStringUIComponentMap.get(DocumentUtil.getXPathExpression(templateTreeNode.getParentNode()));
				if (null == currentParent) {
					currentParent = uiComponent;
				}
				List<String> mappedNodes = getNodesXPathsAtTheSameBFSOrder(BFSs, templateTreeNodeBFSIndex);
				UIComponentElement newChild;
				if (shouldParameterize) {
					newChild = new ParameterizedUIComponentElement(currentParent, mappedNodes, templateTreeNode.getNodeName(), shouldParameterizeAttributes);
					for (int currentTreeIndex = 0; currentTreeIndex < rootNodes.size(); currentTreeIndex++) {
						Node currentTreeNode = BFSs.get(currentTreeIndex).get(templateTreeNodeBFSIndex);
						/*
						 * Mark all nodes rooted in the parameterized node (including the root)
						 * to skip in further iterations.
						 */
						for (Node child : DocumentUtil.bfs(currentTreeNode, true)) {
							coveredNodesXPaths.get(currentTreeIndex).add(DocumentUtil.getXPathExpression(child));
						}
					}
				} else {
					newChild = new NonParameterizedUIComponentElement(currentParent, mappedNodes, templateTreeNode.getNodeName());
					for (int currentTreeIndex = 0; currentTreeIndex < mappedNodes.size(); currentTreeIndex++) {
						coveredNodesXPaths.get(currentTreeIndex).add(mappedNodes.get(currentTreeIndex));
					}
				}
				currentParent.addChild(newChild);
				correspondingStringUIComponentMap.put(DocumentUtil.getXPathExpression(templateTreeNode), newChild);
			}
		}

		return adaptingStrategy.adapt(uiComponent);

	}

	private List<String> getNodesXPathsAtTheSameBFSOrder(List<List<Node>> bfSs, int index) {
		return bfSs.stream()
				.map(bfs -> bfs.get(index))
				.map(DocumentUtil::getXPathExpression)
				.collect(Collectors.toList());
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

	private int getSmallestTreeIndex(ToDoubleFunction<Node> rankFunction) {
		int smallestTreeIndex = -1;
		double smallestTreeSize = Double.MAX_VALUE;
		for (int i = 0; i < rootNodes.size(); i++) {
			double currentTreeSize = rankFunction.applyAsDouble(rootNodes.get(i));
			if (smallestTreeSize > currentTreeSize) {
				smallestTreeSize = currentTreeSize;
				smallestTreeIndex = i;
			}
		}
		return smallestTreeIndex;
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