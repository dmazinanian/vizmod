package ca.ubc.vizmod.model.css;

import ca.concordia.cssanalyser.cssmodel.StyleSheet;
import ca.concordia.cssanalyser.cssmodel.declaration.Declaration;
import ca.concordia.cssanalyser.cssmodel.selectors.BaseSelector;
import ca.concordia.cssanalyser.cssmodel.selectors.Selector;
import ca.concordia.cssanalyser.parser.CSSParser;
import ca.concordia.cssanalyser.parser.CSSParserFactory;
import ca.concordia.cssanalyser.parser.ParseException;
import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.util.DocumentUtil;
import ca.ubc.vizmod.util.ResourcesUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.stream.Collectors;

public final class CSSUtil {

	private static final CSSParser CSS_PARSER = CSSParserFactory.getCSSParser(CSSParserFactory.CSSParserType.LESS);

	private static final Set<String> PROPERTIES_TRANSFERABLE_TO_PARENT = new HashSet<>(
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
		return PROPERTIES_TRANSFERABLE_TO_PARENT.contains(property);
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
				String ourXPathForSelectedNode = DocumentUtil.getXPathExpression(node);
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
			return new ArrayList<>((Set<Selector>) styleSheet.getAllSelectors()).get(0);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Declaration> getCSSDeclarationsFromInlineStyle(String value) {
		try {
			StyleSheet styleSheet = CSS_PARSER.parseCSSString(".dummy {" + value + "}");
			BaseSelector selector = styleSheet.getAllBaseSelectors().get(0);
			return new ArrayList<>((Set<Declaration>) selector.getDeclarations());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private static final BidiMap<String, String> CSS_PROPERTY_TO_JS =
			new DualHashBidiMap<String, String>() {{
				put("background", "background");
				put("background-attachment", "backgroundAttachment");
				put("background-color", "backgroundColor");
				put("background-image", "backgroundImage");
				put("background-position", "backgroundPosition");
				put("background-repeat", "backgroundRepeat");
				put("border", "border");
				put("border-bottom", "borderBottom");
				put("border-bottom-color", "borderBottomColor");
				put("border-bottom-style", "borderBottomStyle");
				put("border-bottom-width", "borderBottomWidth");
				put("border-color", "borderColor");
				put("border-left", "borderLeft");
				put("border-left-color", "borderLeftColor");
				put("border-left-style", "borderLeftStyle");
				put("border-left-width", "borderLeftWidth");
				put("border-right", "borderRight");
				put("border-right-color", "borderRightColor");
				put("border-right-style", "borderRightStyle");
				put("border-right-width", "borderRightWidth");
				put("border-style", "borderStyle");
				put("border-top", "borderTop");
				put("border-top-color", "borderTopColor");
				put("border-top-style", "borderTopStyle");
				put("border-top-width", "borderTopWidth");
				put("border-width", "borderWidth");
				put("clear", "clear");
				put("clip", "clip");
				put("color", "color");
				put("cursor", "cursor");
				put("display", "display");
				put("filter", "filter");
				put("float", "cssFloat");
				put("font", "font");
				put("font-family", "fontFamily");
				put("font-size", "fontSize");
				put("font-variant", "fontVariant");
				put("font-weight", "fontWeight");
				put("height", "height");
				put("left", "left");
				put("letter-spacing", "letterSpacing");
				put("line-height", "lineHeight");
				put("list-style", "listStyle");
				put("list-style-image", "listStyleImage");
				put("list-style-position", "listStylePosition");
				put("list-style-type", "listStyleType");
				put("margin", "margin");
				put("margin-bottom", "marginBottom");
				put("margin-left", "marginLeft");
				put("margin-right", "marginRight");
				put("margin-top", "marginTop");
				put("overflow", "overflow");
				put("padding", "padding");
				put("padding-bottom", "paddingBottom");
				put("padding-left", "paddingLeft");
				put("padding-right", "paddingRight");
				put("padding-top", "paddingTop");
				put("page-break-after", "pageBreakAfter");
				put("page-break-before", "pageBreakBefore");
				put("position", "position");
				put("stroke-dasharray", "strokeDasharray");
				put("stroke-dashoffset", "strokeDashoffset");
				put("stroke-width", "strokeWidth");
				put("text-align", "textAlign");
				put("text-indent", "textIndent");
				put("text-transform", "textTransform");
				put("top", "top");
				put("vertical-align", "verticalAlign");
				put("visibility", "visibility");
				put("width", "width");
				put("z-index", "zIndex");

				put("text-decoration", "textDecoration");
			}};

	public static String getCSSPropertyJSName(Declaration declaration) {
		if ("text-decoration".equals(declaration.getProperty())) {
			//text-decoration: blink, "textDecorationBlink");
			//text-decoration: line-through", "textDecorationLineThrough");
			//text-decoration: none, "textDecorationNone");
			//text-decoration: overline, "textDecorationOverline");
			//text-decoration: underline", "textDecorationUnderline");
		} else {
			return CSS_PROPERTY_TO_JS.get(declaration.getProperty());
		}
		return null;
	}

	public static String getValueString(Declaration cssDeclaration) {
		return cssDeclaration.toString()
				.replace(cssDeclaration.getProperty() + ": ", "");
	}
}
