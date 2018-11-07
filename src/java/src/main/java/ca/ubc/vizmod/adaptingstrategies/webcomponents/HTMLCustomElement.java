package ca.ubc.vizmod.adaptingstrategies.webcomponents;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;

public class HTMLCustomElement extends HTMLElementImpl {
	
	private static final long serialVersionUID = 1L;

	public HTMLCustomElement(HTMLDocumentImpl owner, String tagName) {
		super(owner, tagName);
	}
	
}
