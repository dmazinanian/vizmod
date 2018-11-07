package ca.ubc.vizmod.model.css;

import ca.concordia.cssanalyser.cssmodel.StyleSheet;
import ca.concordia.cssanalyser.cssmodel.declaration.Declaration;
import ca.concordia.cssanalyser.cssmodel.selectors.Selector;
import ca.concordia.cssanalyser.parser.CSSParser;
import ca.concordia.cssanalyser.parser.CSSParserFactory;
import ca.concordia.cssanalyser.parser.ParseException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class CSS {

	private static final CSSParser CSS_PARSER = CSSParserFactory.getCSSParser(CSSParserFactory.CSSParserType.LESS);

    public enum CSSSource {
		INLINE_CSS,
		EMBEDDED_CSS,
		EXTERNAL_CSS,
	}
	
	private final CSSSource source;
	
	private final String url;
	
	private final String cssText;
	
	public CSS(CSSSource source, String url, String cssText) {
		this.source = source;
		this.url = url;
		this.cssText = cssText;
	}

	public CSSSource getSource() {
		return source;
	}

	public String getUrl() {
		return url;
	}

	public String getCssText() {
		return cssText;
	}

	public Set<Declaration> getDeclarations() {
		try {
			String cssTextToParse;
			if (source == CSSSource.INLINE_CSS) {
				cssTextToParse = "dummy { " + cssText + " }";
			} else {
				cssTextToParse = cssText;
			}
			StyleSheet styleSheet = CSS_PARSER.parseCSSString(cssTextToParse);
			Selector selector = new ArrayList<>((Set<Selector>)styleSheet.getAllSelectors()).get(0);
			return (Set<Declaration>)selector.getDeclarations();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new LinkedHashSet<>();
	}


	public Selector getSelector() {
		try {
			if (source == CSSSource.INLINE_CSS) {
				return null;
			} else {
				StyleSheet styleSheet = CSS_PARSER.parseCSSString(cssText);
				return new ArrayList<>((Set<Selector>)styleSheet.getAllSelectors()).get(0);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CSS css = (CSS) o;
		return source == css.source &&
				Objects.equals(url, css.url) &&
				Objects.equals(cssText, css.cssText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, url, cssText);
	}

	@Override
	public String toString() {
		return cssText + " (" + source + " " + url + ")";
	}
}
