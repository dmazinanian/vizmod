package ca.ubc.vizmod.detection;

import ca.ubc.vizmod.browser.AbstractBrowser;
import ca.ubc.vizmod.browser.ChromeBrowser;
import ca.ubc.vizmod.detection.visualhash.PHash;
import ca.ubc.vizmod.util.DocumentUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.fail;


public class DetectorTests {
	
	private static Detector detector;
	private static AbstractBrowser browser;
	
	@BeforeClass
	public static void setUp() {

		browser = new ChromeBrowser("http://localhost:8080/web-components.html", true);

		detector = new Detector(browser, new PHash());
		
	}

	@AfterClass
	public static void tearDown() throws Exception {
		browser.close();
	}

	@Test
	public void testDetection() {

		List<CloneGroup> cloneGroups = detector.detect(3);

		String outputFolder = "/Users/davood/Desktop/Output/";

		for (CloneGroup cloneGroup : cloneGroups) {
			File folder = new File(outputFolder + cloneGroup.getKey());
			folder.mkdir();

			for (Node node : cloneGroup.getRootNodes()) {
				String xPathExpression = DocumentUtil.getXPathExpression(node);
				BufferedImage elementScreenshot = browser.takeScreenshot(xPathExpression);
				try {

					File outFile = new File(folder.getAbsolutePath() + File.separator +
							xPathExpression.replace("/", "_"));
					ImageIO.write(elementScreenshot, "png", outFile);

				} catch (IOException ioEx) {
					ioEx.printStackTrace();
				}
			}

		}

	}

	//@Test
	public void testScreenshot() {

		BufferedImage elementScreenshot =
				browser.takeScreenshot("//*[@id=\"gc-wrapper\"]/div[2]/article/article/div[2]/section[1]");

		try {

			ImageIO.write(elementScreenshot, "png", new File("element.png"));

			ImageIO.write(browser.takeScreenshot(), "png", new File("full-page.png"));

		} catch (IOException ioex) {
			ioex.printStackTrace();
			fail();
		}
	}
}
