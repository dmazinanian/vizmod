package ca.ubc.uicomponentrefactorer.adaptingstrategies.adaptingstrategies.webcomponents.webcomponents;

import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.html.dom.HTMLElementImpl;

public class HTMLSlotElement extends HTMLElementImpl {

	private static final long serialVersionUID = 1L;

	public HTMLSlotElement(HTMLDocumentImpl document, String slotName) {
		super(document, "slot");
		this.setAttribute("name", slotName);
	}
	
}
