package ca.ubc.uicomponentrefactorer.adaptingstrategies.adaptingstrategies.webcomponents.webcomponents;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;

public class HTMLTemplateElement extends HTMLElementImpl {
	
	private static final long serialVersionUID = 1L;

	public HTMLTemplateElement (HTMLDocumentImpl ownerDocument) {
		super(ownerDocument, "template");
	}
	
}
