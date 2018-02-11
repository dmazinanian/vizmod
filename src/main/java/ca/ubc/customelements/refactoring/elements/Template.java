package ca.ubc.customelements.refactoring.elements;

import org.w3c.dom.Node;

import ca.ubc.customelements.util.DocumentUtil;

public class Template {
	
	private String id = "";
	private String style = "";
	private String script = "";
	private final Node body;
	
	public Template(Node body) {
		this.body = body;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setStyle(String style) {
		this.style = style;
	}
	
	public void setScript(String script) {
		this.script = script;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<template");
		if (!"".equals(id.trim())) {
			builder.append(" id=\"").append(id).append("\"");
		}
		builder.append(">");
		builder.append(System.lineSeparator());
		builder.append(DocumentUtil.getElementString(body));
		builder.append(System.lineSeparator());
		builder.append("</template>");
		return builder.toString();
	}
	
}
