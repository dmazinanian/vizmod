package ca.ubc.uicomponentrefactorer.browser;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface AbstractBrowser extends AutoCloseable {

    public Object evaluateJavaScript(String js);

    public Object callJSFunction(String functionName, Object... arguments);

    public void navigate(String url);

    public BufferedImage takeScreenshot();

    public BufferedImage takeScreenshot(String elementXPath);

    public String getDOM();

    public Rectangle getBoundingBox(String xpath);

    public void normalizeDOM();

}
