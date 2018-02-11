package ca.ubc.customelements.detection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import ca.ubc.customelements.util.DocumentUtil;
import ca.ubc.customelements.util.IOUtil;


public class DetectorTests {
	
	private static Detector detector;
	
	@BeforeClass
	public static void setUp() {
		detector = new Detector();
	}
	
	@Test
	public void testDetection() {
		String domTest = IOUtil.readResourceFileToString("web-components.html");
		Document document = DocumentUtil.toDocument(domTest);
		for (CloneGroup cloneGroup : detector.detect(document)) {
			System.out.println(cloneGroup.toString());
			System.out.println("---");
		}
	}
	
}
