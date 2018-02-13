package ca.ubc.customelements.refactoring.elements;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;

public class HTMLCustomElement extends HTMLElementImpl {
	
	private static final long serialVersionUID = 1L;

	public HTMLCustomElement(HTMLDocumentImpl owner, String tagName) {
		super(owner, tagName);
	}
	
}
