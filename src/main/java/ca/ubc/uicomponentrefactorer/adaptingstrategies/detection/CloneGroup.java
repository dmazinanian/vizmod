package ca.ubc.uicomponentrefactorer.adaptingstrategies.detection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

public class CloneGroup {
	
	private final String key;
	private final List<Node> roots;
	
	public CloneGroup(String key) {
		this.key = key;
		this.roots = new ArrayList<>();
	}
	
	public CloneGroup(String key, List<Node> list) {
		this.key = key;
		this.roots = new ArrayList<>(list);
	}

	public String getKey() {
		return this.key;
	}
	
	public void addClone(Node root) {
		this.roots.add(root);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(key);
		builder.append(System.lineSeparator());
		for (Iterator<Node> iterator = roots.iterator(); iterator.hasNext();) {
			Node node = iterator.next();
			builder.append(node);
			if (iterator.hasNext()) {
				builder.append(System.lineSeparator());
			}
		}
		return builder.toString();
	}

	public void addClones(List<Node> list) {
		this.roots.addAll(list);
	}
}
