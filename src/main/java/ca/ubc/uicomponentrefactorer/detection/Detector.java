package ca.ubc.uicomponentrefactorer.detection;

import ca.ubc.uicomponentrefactorer.browser.AbstractBrowser;
import ca.ubc.uicomponentrefactorer.detection.visualhash.VisualHashCalculator;
import ca.ubc.uicomponentrefactorer.util.DocumentUtil;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.xerces.dom.TextImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLScriptElement;
import org.w3c.dom.html.HTMLStyleElement;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Detector {

	private AbstractBrowser browser;
	private VisualHashCalculator visualHashCalculator;

	public Detector(AbstractBrowser browser, VisualHashCalculator visualHashCalculator) {
		this.browser = browser;
		this.visualHashCalculator = visualHashCalculator;
	}

	public List<CloneGroup> detect(int heightThreshold) {

		browser.normalizeDOM();

		Document document = DocumentUtil.toDocument(browser.getDOM());
		Node body = document.getElementsByTagName("body").item(0);

		List<Node> allNodes = DocumentUtil.dfs(body, false); // DFS since we want to start from the leaves

		Map<String, Node> xpathToNodeMap = new HashMap<>();
        MultiValuedMap<String, String> visualHashToXPathMultiMap = new ArrayListValuedHashMap<>();

        for (Node node : allNodes) {
			if (!(node instanceof TextImpl &&
					node instanceof HTMLScriptElement &&
					node instanceof HTMLStyleElement)) {
				if (heightThreshold > 0 && DocumentUtil.getSubtreeHeight(node) < heightThreshold) {
					continue;
				}
				String xpath = DocumentUtil.getXPathExpression(node);
				xpathToNodeMap.put(xpath, node);

                boolean shouldAdd = true;

				BufferedImage elementScreenshot = browser.takeScreenshot(xpath);
				if (null != elementScreenshot) {
					String visualHash = visualHashCalculator.getVisualHash(elementScreenshot);
					if (visualHashToXPathMultiMap.containsKey(visualHash)) {
						// Replace child with its parent if the visual hashes are the same
						// We are sure that the existing elements can only be children of the current element, since DFS
						List<String> nodeXPathsWithTheSameVisualHash = (List<String>)visualHashToXPathMultiMap.get(visualHash);
						for (int i = 0; i < nodeXPathsWithTheSameVisualHash.size(); i++) {
							String nodeXPathWithTheSameVisualHash = nodeXPathsWithTheSameVisualHash.get(i);
							Node nodeWithTheSameVisualHash = xpathToNodeMap.get(nodeXPathWithTheSameVisualHash);
							List<Node> parents = DocumentUtil.getParents(nodeWithTheSameVisualHash);
							for (Node parent : parents) {
								if (xpath.equals(DocumentUtil.getXPathExpression(parent))) {
									nodeXPathsWithTheSameVisualHash.set(i, xpath);
									shouldAdd = false;
									break;
								}
							}
                            if (!shouldAdd) {
							    break;
                            }
						}

					}
					if (shouldAdd) {
					    // TODO We might want to use a locality-sensitive hashing
                        visualHashToXPathMultiMap.put(visualHash, xpath);
                    }
				}

			}
		}

        List<CloneGroup> clones = new ArrayList<>();
        for (String visualHash : visualHashToXPathMultiMap.keySet()) {
            List<String> elementsWithTheSameVisualHash = (List<String>) visualHashToXPathMultiMap.get(visualHash);
            if (elementsWithTheSameVisualHash.size() >= 2) {
                List<Node> repeatedNodes = elementsWithTheSameVisualHash.stream()
                        .map(xpathToNodeMap::get)
                        //.filter(node -> heightThreshold <= 0 || DocumentUtil.getSubtreeHeight(node) >= heightThreshold)
                        .collect(Collectors.toList());
                if (repeatedNodes.size() >= 2) {
					clones.add(new CloneGroup(visualHash, repeatedNodes));
				}
            }
        }

		return clones;
	}
	
}
