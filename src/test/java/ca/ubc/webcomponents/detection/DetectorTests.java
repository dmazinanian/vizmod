package ca.ubc.webcomponents.detection;

import ca.ubc.webcomponents.util.ResourcesUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import ca.ubc.webcomponents.util.DocumentUtil;


public class DetectorTests {
	
	private static Detector detector;
	
	@BeforeClass
	public static void setUp() {
		detector = new Detector();
	}
	
	@Test
	public void testDetection() {
		String domTest = ResourcesUtil.readResourceFileToString("web-components.html");
		Document document = DocumentUtil.toDocument(domTest);
		for (CloneGroup cloneGroup : detector.detect(document)) {
			System.out.println(cloneGroup.toString());
			System.out.println("---");
		}
	}
	
}
