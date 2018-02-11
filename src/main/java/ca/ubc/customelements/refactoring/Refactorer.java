package ca.ubc.customelements.refactoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.ubc.customelements.refactoring.elements.Template;
import ca.ubc.customelements.util.DocumentUtil;
import costmodel.StringUnitCostModel;
import distance.APTED;
import node.StringNodeData;

public class Refactorer {
	
	private final List<Node> nodes;
	
	public Refactorer(List<Node> parentNodes) {
		this.nodes = parentNodes;
	}
	
	public TemplateRefactoring refactor() {
		Template template = inferTemplate();
		return null;
	}

	private Template inferTemplate() {
		
		APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
		 
		Node smallestTree = getSmallestTree();
		node.Node<StringNodeData> t1 = getAPTEDTreeFromNode(smallestTree);
		List<Node> smallestTreeNodesPostOrder = DocumentUtil.dfs(smallestTree, false);
		
		Node templateBody = smallestTree.cloneNode(false);
		Template template = new Template(templateBody);
		
		Map<String, String> nodeMappings = new HashMap<>();
		List<Node> addedNodes = new ArrayList<>();
		
		for (Node tree : nodes) {
			if (tree != smallestTree) {
				node.Node<StringNodeData> t2 = getAPTEDTreeFromNode(tree);
				List<Node> currentTreeNodesPostOrder = DocumentUtil.dfs(tree, false);
				apted.computeEditDistance(t1, t2);
				LinkedList<int[]> computeEditMapping = apted.computeEditMapping();
				for (int[] mapping : computeEditMapping) {
					if (mapping[0] == 0) {
						addedNodes.add(currentTreeNodesPostOrder.get(mapping[1] - 1));
					} else if (mapping[1] == 0) {
						//removedNodes.add(XPathHelper.getXPathExpression(postOrderOld.get(mapping[0] - 1)));
						throw new RuntimeException("Why there is no mapping?");
					} else {
						nodeMappings.put(DocumentUtil.getXPathExpression(smallestTreeNodesPostOrder.get(mapping[0] - 1)), 
								DocumentUtil.getXPathExpression(currentTreeNodesPostOrder.get(mapping[1] - 1)));
					}
				}
				for (Node addedNode : addedNodes) {
					System.out.println(DocumentUtil.getXPathExpression(addedNode));
				}
				
				for (String key : nodeMappings.keySet()) {
					System.out.println(key + " => " + nodeMappings.get(key));
				}
			}
		}
		
		return template;
		
	}

	private node.Node<StringNodeData> getAPTEDTreeFromNode(Node node) {
		if (node.hasChildNodes()) {
			node.Node<StringNodeData> root = new node.Node<StringNodeData>(new StringNodeData(DocumentUtil.nodeString(node)));
			NodeList childNodes = node.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node item = childNodes.item(i);
				node.Node<StringNodeData> childrenRoot = getAPTEDTreeFromNode(item);
				if (null != childrenRoot) {
					root.addChild(childrenRoot);
				}
			}
			return root;
		}
		return null;
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
