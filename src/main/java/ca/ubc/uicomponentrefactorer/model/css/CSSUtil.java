package ca.ubc.uicomponentrefactorer.model.css;

import ca.concordia.cssanalyser.cssmodel.StyleSheet;
import ca.concordia.cssanalyser.cssmodel.selectors.BaseSelector;
import ca.concordia.cssanalyser.cssmodel.selectors.Selector;
import ca.concordia.cssanalyser.parser.CSSParser;
import ca.concordia.cssanalyser.parser.CSSParserFactory;
import ca.concordia.cssanalyser.parser.ParseException;
import ca.ubc.uicomponentrefactorer.util.DocumentUtil;
import ca.ubc.uicomponentrefactorer.browser.AbstractBrowser;
import ca.ubc.uicomponentrefactorer.util.ResourcesUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.stream.Collectors;

public final class CSSUtil {

	private static final CSSParser CSS_PARSER = CSSParserFactory.getCSSParser(CSSParserFactory.CSSParserType.LESS);

    private static final Set<String> propertiesTranserableToParent = new HashSet<>(
            Arrays.asList(
                    "width",
                    "height",
                    "margin",
                    "padding",
                    "float",

                    "display",

                    //Flex Box
                    "order",
                    "flex-grow",
                    "flex-shrink",
                    "flex-basis",
                    "flex"

            )
    );

	public static List<CSS> getAppliedCSS(AbstractBrowser browser, String xpath) {
		List<CSS> appliedCSS = new ArrayList<>();
		String getCSSJS = ResourcesUtil.readResourceFileToString(ResourcesUtil.GET_CSS_FOR_ELEMENT_JS);
		browser.evaluateJavaScript(getCSSJS);
		String evaluated = browser.callJSFunction("cssForElementByXPath", xpath).toString();
		JsonArray jsonArray = new JsonParser().parse(evaluated).getAsJsonArray();
		for (JsonElement cssJsonElement : jsonArray) {
			JsonObject cssJSONObject = cssJsonElement.getAsJsonObject();
			String cssText = cssJSONObject.get("css").getAsString();
			CSS.CSSSource cssSource = CSS.CSSSource.valueOf(cssJSONObject.get("source").getAsString());
			String url = "";
			if (cssSource != CSS.CSSSource.INLINE_CSS && null != cssJSONObject.get("url") && !"null".equals(cssJSONObject.get("url").toString())) {
				url = cssJSONObject.get("url").getAsString();
			}
			CSS css = new CSS(cssSource, url, cssText);
			appliedCSS.add(css);
		}
		return appliedCSS;
	}

	public static boolean propertyShouldBeAppliedToParent(String property) {
        return propertiesTranserableToParent.contains(property);
    }

	public static List<CSS> getExternalAndEmbeddedCSS(AbstractBrowser browser, String xPath) {
		return getAppliedCSS(browser, xPath)
				.stream().filter(css -> css.getSource() != CSS.CSSSource.INLINE_CSS)
				.collect(Collectors.toList());
	}

	public static boolean matches(BaseSelector selector, Node correspondingNode) {
		Document document = correspondingNode.getOwnerDocument();
		try {
			String ourXPathForCorrespondingNode = DocumentUtil.getXPathExpression(correspondingNode);
			NodeList selectedNodesBySelector = DocumentUtil.queryDocument(document, selector.getXPath());
			for (int i = 0; i < selectedNodesBySelector.getLength(); i++) {
				Node node = selectedNodesBySelector.item(i);
				String ourXPathForSelectedNode =  DocumentUtil.getXPathExpression(node);
				if (ourXPathForCorrespondingNode.equals(ourXPathForSelectedNode)) {
					return true;
				}
			}
		} catch (Selector.UnsupportedSelectorToXPathException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static Selector newSelectorFromString(String selectorText) {
		try {
			StyleSheet styleSheet = CSS_PARSER.parseCSSString(selectorText + "{}");
			return new ArrayList<>((Set<Selector>)styleSheet.getAllSelectors()).get(0);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
