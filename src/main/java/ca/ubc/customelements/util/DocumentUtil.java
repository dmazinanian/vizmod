package ca.ubc.customelements.util;

import java.io.IOException;
import java.io.StringReader;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class DocumentUtil {

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

}
