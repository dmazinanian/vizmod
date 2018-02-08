package ca.ubc.customelements.detection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Detector {
	
	public List<CloneGroup> detect(Document document) {
		List<CloneGroup> cloneGroups = new ArrayList<>();
		Map<String, List<Node>> repeatedRoots = getRepeatedSubtrees(document.getElementsByTagName("body").item(0));
		for (String key : repeatedRoots.keySet()) {
			List<Node> list = repeatedRoots.get(key);
			if (list.size() >= 2) {
				CloneGroup group = new CloneGroup(key, list);
				cloneGroups.add(group);
			}
		}
		return cloneGroups;
	}

	private Map<String, List<Node>> getRepeatedSubtrees(Node rootNode) {
		Map<String, List<Node>> repeatedRoots = new HashMap<>();
		List<Node> allNodes = dfs(rootNode);
		for (Node node : allNodes) {
			 String key = newick(node);
			 List<Node> bucket = repeatedRoots.get(key);
			 if (null == bucket) {
				 bucket = new ArrayList<>();
				 repeatedRoots.put(key, bucket);
			 }
			 bucket.add(node);
		}
		return repeatedRoots;
	}
	
	public String newick(Node root) {
		StringBuilder toReturn = new StringBuilder();
		if (root.hasChildNodes()) { // Don't go for leaf nodes
			NodeList childNodes = root.getChildNodes();
			toReturn.append("(");
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				toReturn.append(newick(child));
				if (child.hasChildNodes() && i < childNodes.getLength() - 2) {
					toReturn.append(",");
				}
			}
			toReturn.append(")");
			toReturn.append(nodeString(root));
		}
		return toReturn.toString();
	}
	
	private List<Node> dfs(Node root) {
		List<Node> toReturn = new ArrayList<>();
		NodeList childNodes = root.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			toReturn.addAll(dfs(child));
		}
		if (root.hasChildNodes()) { // Don't go for leaf nodes
			toReturn.add(root);
		}
		return toReturn;
	}
	
	private List<Node> bfs(Node root) {
		List<Node> toReturn = new ArrayList<>();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Node node = queue.remove();
			if (node.hasChildNodes()) { // Don't go for leaf nodes
				queue.add(node);
				NodeList childNodes = root.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node child = childNodes.item(i);
					queue.add(child);
				}
			}
		}
		return toReturn;
	}
	
	private String stringify(List<Node> nodes) {
		StringBuilder builder = new StringBuilder();
		for (Node node : nodes) {
			builder.append(nodeString(node));
		}
		return builder.toString();
	}

	private String nodeString(Node node) {
		if (node instanceof Element) {
			Element element = (Element) node;
			return element.getTagName();
		} else {
			System.out.println("I don't know what to do with " + node.getClass() + " " + node.getNodeType());
			return "";
		}
	}
	
}
