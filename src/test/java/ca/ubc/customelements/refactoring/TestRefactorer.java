package ca.ubc.customelements.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ca.ubc.customelements.refactoring.Refactorer;
import ca.ubc.customelements.util.DocumentUtil;
import ca.ubc.customelements.util.IOUtil;

public class TestRefactorer {
	
	@Test
	public void testRefactorer() {
		Document document = DocumentUtil.toDocument(IOUtil.readResourceFileToString("TestWebsite/removed-subtree.html"));
		List<Node> parentNodes = new ArrayList<>();
		parentNodes.add(document.getElementById("div1"));
		parentNodes.add(document.getElementById("div3"));
		Element e1 = document.getElementById("div1");
		Element e2 = document.getElementById("div2");
		Element e3 = document.getElementById("div3");
		System.out.println(DocumentUtil.bfs(e1, false));//.stream().map(DocumentUtil::getXPathExpression).collect(Collectors.toList()));
		System.out.println(DocumentUtil.bfs(e2, false));//.stream().map(DocumentUtil::getXPathExpression).collect(Collectors.toList()));
		System.out.println(DocumentUtil.bfs(e3, false));//.stream().map(DocumentUtil::getXPathExpression).collect(Collectors.toList()));
		//Refactorer refactorer = new Refactorer(parentNodes);
		//refactorer.refactor();
	}
	
}
