package ca.ubc.customelements.css;

import ca.concordia.cssanalyser.cssmodel.declaration.Declaration;
import ca.ubc.customelements.browser.AbstractBrowser;
import ca.ubc.customelements.css.CSS.CSSSource;
import ca.ubc.customelements.util.ResourcesUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class CSSUtil {

	public static List<CSS> getAppliedCSS(AbstractBrowser browser, String xpath) {
		List<CSS> appliedCSS = new ArrayList<>();
		String getCSSJS = ResourcesUtil.readResourceFileToString(ResourcesUtil.GET_CSS_FOR_ELEMENT_JS);
		browser.evaluateJavaScript(getCSSJS);
		String evaluated = browser.callJSFunction("cssForElementByXPath", xpath).toString();
		JsonArray jsonArray = new JsonParser().parse(evaluated).getAsJsonArray();
		for (JsonElement cssJsonElement : jsonArray) {
			JsonObject cssJSONObject = cssJsonElement.getAsJsonObject();
			String cssText = cssJSONObject.get("css").getAsString();
			CSSSource cssSource = CSSSource.valueOf(cssJSONObject.get("source").getAsString());
			String url = "";
			if (cssSource != CSSSource.EMBEDDED_CSS) {
				url = cssJSONObject.get("url").getAsString();
			}
			CSS css = new CSS(cssSource, url, cssText);
			appliedCSS.add(css);
		}
		return appliedCSS;
	}
}
