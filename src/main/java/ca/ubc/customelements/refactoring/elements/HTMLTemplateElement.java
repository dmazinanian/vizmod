package ca.ubc.customelements.refactoring.elements;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;

public class HTMLTemplateElement extends HTMLElementImpl {
	
	private static final long serialVersionUID = 1L;

	public HTMLTemplateElement (HTMLDocumentImpl ownerDocument) {
		super(ownerDocument, "template");
	}
	
}
