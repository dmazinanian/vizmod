package ca.ubc.webcomponents.detection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ca.ubc.webcomponents.util.DocumentUtil;

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
		List<Node> allNodes = DocumentUtil.dfs(rootNode);
		for (Node node : allNodes) {
			 String key = DocumentUtil.newick(node, false);
			 List<Node> bucket = repeatedRoots.get(key);
			 if (null == bucket) {
				 bucket = new ArrayList<>();
				 repeatedRoots.put(key, bucket);
			 }
			 bucket.add(node);
		}
		return repeatedRoots;
	}
	
	
	
}
