package ca.ubc.uicomponentrefactorer.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xerces.dom.TextImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public final class DocumentUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUtil.class);
	
	private static final String FULL_XPATH_CACHE = "FULL_XPATH_CACHE";

	public static Document toDocument(String domTest) {
		DOMParser domParser = new DOMParser();
		try {
			domParser.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
			domParser.setFeature("http://xml.org/sax/features/namespaces", false);
			domParser.parse(new InputSource(new StringReader(domTest)));
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}
		return domParser.getDocument();
	}
	
	public static String getXPathExpression(Node node) {
		
		Object xpathCache = node.getUserData(FULL_XPATH_CACHE);
		if (xpathCache != null) {
			return xpathCache.toString();
		}
		Node parent = node.getParentNode();

		if ((parent == null) || parent.getNodeName().contains("#document")) {
			String xPath = "/" + node.getNodeName() + "[1]";
			node.setUserData(FULL_XPATH_CACHE, xPath, null);
			return xPath;
		}
		
		if(node.hasAttributes() && node.getAttributes().getNamedItem("id") != null) {
			String xPath = "//" + node.getNodeName() + "[@id = '" 
					+ node.getAttributes().getNamedItem("id").getNodeValue() + "']";
			node.setUserData(FULL_XPATH_CACHE, xPath, null);
			return xPath;
		}

		StringBuffer buffer = new StringBuffer();

		if (parent != node) {
			buffer.append(getXPathExpression(parent));
			buffer.append("/");
		}

		buffer.append(node.getNodeName().replace("#text", "text()"));

		List<Node> mySiblings = getSiblings(parent, node);

		for (int i = 0; i < mySiblings.size(); i++) {
			Node el = mySiblings.get(i);

			if (el.equals(node)) {
				buffer.append('[').append(Integer.toString(i + 1)).append(']');
				// Found so break;
				break;
			}
		}
		String xPath = buffer.toString();
		node.setUserData(FULL_XPATH_CACHE, xPath, null);
		return xPath;
	}
	
	public static List<Node> getSiblings(Node parent, Node element) {
		List<Node> result = new ArrayList<>();
		NodeList list = parent.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {
			Node el = list.item(i);

			if (el.getNodeName().equals(element.getNodeName())) {
				result.add(el);
			}
		}

		return result;
	}
	
	public static String nodeString(Node node) {
		if (node instanceof Element) {
			Element element = (Element) node;
			return element.getTagName();
		} else if (node instanceof CharacterData) {
			CharacterData characterData = (CharacterData) node;
			return characterData.getTextContent();
		} else {
			LOGGER.warn("I don't know what to do with {}", node.getClass());
			return "";
		}
	}
	
	public static String getElementString(Node dom) {
		try {
			Source source = new DOMSource(dom);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "html");
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String newick(Node root, boolean leaves) {
		StringBuilder toReturn = new StringBuilder();
		if (leaves || root.hasChildNodes()) { // Don't go for leaf nodes if not needed
			NodeList childNodes = root.getChildNodes();
			toReturn.append("(");
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				toReturn.append(newick(child, leaves));
				if (child.hasChildNodes() && i < childNodes.getLength() - 2) {
					toReturn.append(",");
				}
			}
			toReturn.append(")");
			toReturn.append(DocumentUtil.nodeString(root));
		}
		return toReturn.toString();
	}
	
	public static List<Node> dfs(Node root) {
		return dfs(root, true);
	}
	
	public static List<Node> dfs(Node root, boolean leaves) {
		List<Node> toReturn = new ArrayList<>();
		NodeList childNodes = root.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			toReturn.addAll(dfs(child, leaves));
		}
		if (leaves || root.hasChildNodes()) { // Don't go for leaf nodes if not needed
			toReturn.add(root);
		}
		return toReturn;
	}
	
	/*public static List<Node> bfs(Node root, boolean leaves) {
		List<Node> toReturn = new ArrayList<>();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Node node = queue.remove();
			if (leaves || node.hasChildNodes()) { // Don't go for leaf nodes if not needed
				toReturn.add(node);
				NodeList childNodes = node.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node child = childNodes.item(i);
					queue.add(child);
				}
			}
		}
		return toReturn;
	}*/
	
	public static List<Node> bfs(Node root, boolean onlyTextNodesWithData) {
		List<Node> toReturn = new ArrayList<>();
		Queue<Node> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Node node = queue.remove();
			if (!(node instanceof TextImpl) || (onlyTextNodesWithData && !"".equals(((TextImpl)node).getTextContent().trim()))) {
				toReturn.add(node);
				NodeList childNodes = node.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node child = childNodes.item(i);
					queue.add(child);
				}
			}
		}
		return toReturn;
	}
	
	public static String stringify(List<Node> nodes) {
		StringBuilder builder = new StringBuilder();
		for (Node node : nodes) {
			builder.append(nodeString(node));
		}
		return builder.toString();
	}
	
	public static NodeList queryDocument(Document doc, String XPath) {
		try {
			XPath xPathObj = XPathFactory.newInstance().newXPath();
			return (NodeList) xPathObj.evaluate(XPath, doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}

}