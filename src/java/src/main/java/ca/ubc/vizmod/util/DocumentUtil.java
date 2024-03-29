package ca.ubc.vizmod.util;

import org.cyberneko.html.parsers.DOMParser;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;


public final class DocumentUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUtil.class);

    private static final Set<String> VOID_TAGS_SET = new HashSet<>(Arrays.asList(
            "IMG",
            "BR",
            "INPUT",
            "SCRIPT"
            ));
	
	private static final String FULL_XPATH_CACHE = "FULL_XPATH_CACHE";

	public static Document toDocument(String html) {
		DOMParser domParser = new DOMParser();
		try {
			domParser.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
			domParser.setFeature("http://xml.org/sax/features/namespaces", false);
			domParser.parse(new InputSource(new StringReader(html)));
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

		buffer.append(getNodeName(node));

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

	private static String getNodeName(Node node) {
		if (node instanceof Text) {
			return "text()";
		} else if (node instanceof Comment) {
			return "comment()";
		} else {
			return node.getNodeName();
		}
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
		if (node instanceof HTMLDocument) {
			HTMLDocument htmlDocument = (HTMLDocument) node;
			return htmlDocument.getNodeName();
		} else if (node instanceof Element) {
			Element element = (Element) node;
			return element.getTagName();
		} else if (node instanceof CharacterData) {
			CharacterData characterData = (CharacterData) node;
			return characterData.getTextContent().trim();
		} else {
			LOGGER.warn("I don't know what to do with {}", node.getClass());
			return "";
		}
	}
	
	public static String getElementString(Node node) {
		try {
			Source source = new DOMSource(node);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "html");
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String getElementXHTMLString(Node node) {
		return getElementXHTMLString(node, null);
	}

	public static String getElementXHTMLString(Node node, Consumer<org.jsoup.nodes.Element> elementActionConsumer) {
		if (node instanceof Text) {
			return node.getTextContent();
		} else {
			String html = getElementString(node); // Serialize to HTML
			final org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(html); // Parse to XML
			document.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
			org.jsoup.nodes.Element element;
			if (node instanceof Document) {
				element = document;
			} else {
				// The result is a complete DOM, only get the element within the body
				element = document.select("body > " + node.getNodeName().toLowerCase()).first();
			}
			if (null != elementActionConsumer) {
				elementActionConsumer.accept(element);
			}
			return element.toString();
		}
	}
	
	public static String newick(Node root) {
		StringBuilder toReturn = new StringBuilder();
		if (root.hasChildNodes()) {
			toReturn.append("(");
			NodeList childNodes = root.getChildNodes();
			int numberOfNonEmptyNodes = getNumberOfNonEmptyNodes(childNodes);
			int commasAdded = 0;
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if (!isEmptyTextNode(child)) {
					toReturn.append(newick(child));
					if (commasAdded < numberOfNonEmptyNodes - 1) {
						toReturn.append(",");
						commasAdded++;
					}
				}
			}
			toReturn.append(")");
		}
		toReturn.append(DocumentUtil.nodeString(root));
		return toReturn.toString();
	}
	private static int getNumberOfNonEmptyNodes(NodeList childNodes) {
		int nonEmptyNodes = 0;
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (!isEmptyTextNode(node)) {
				nonEmptyNodes++;
			}
		}
		return nonEmptyNodes;
	}

	private static boolean isEmptyTextNode(Node node) {
		return node instanceof Text && "".equals(((Text)node).getTextContent().trim());
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
			if (!(node instanceof Text) || (!onlyTextNodesWithData || !"".equals(node.getTextContent().trim()))) {
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

	public static NodeList queryDocument(Document doc, String XPath) {
		try {
			XPath xPathObj = XPathFactory.newInstance().newXPath();
			return (NodeList) xPathObj.evaluate(XPath, doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}

    public static boolean isVoidTag(HTMLElement htmlElement) {
        return VOID_TAGS_SET.contains(htmlElement.getNodeName());
    }

	public static List<Node> getParents(Node node) {
		List<Node> parents = new ArrayList<>();
		while (null != node) {
			node = node.getParentNode();
			if (null != node) {
				parents.add(node);
			}
		}
		return parents;
	}

	public static int getSubtreeHeight(Node node) {
		int maxHeight = -1;
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			maxHeight = Math.max(maxHeight, getSubtreeHeight(childNodes.item(i)));
		}
		return maxHeight + 1;
	}

    public static List<Node> getElementByAttributeValue(Node rootNode, String attribute, String value) {
		List<Node> nodesToReturn = new ArrayList<>();
		for (Node node : bfs(rootNode, false)) {
			if (node instanceof HTMLElement) {
				HTMLElement element = (HTMLElement) node;
				String attribute1 = element.getAttribute(attribute);
				System.out.println(attribute1);
				System.out.println(attribute1.equals(value));
				if (value.equals(attribute1)) {
					nodesToReturn.add(node);
				}
			}
		}
		return nodesToReturn;
    }
}
