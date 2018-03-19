package ca.ubc.uicomponentrefactorer.browser;

import com.google.gson.Gson;
import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.command.Emulation;
import io.webfolder.cdp.command.Page;
import io.webfolder.cdp.exception.CommandException;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import io.webfolder.cdp.type.dom.Node;
import io.webfolder.cdp.type.dom.Rect;
import io.webfolder.cdp.type.page.GetLayoutMetricsResult;
import io.webfolder.cdp.type.page.Viewport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.webfolder.cdp.type.constant.ImageFormat.Png;

public class ChromeBrowser implements AbstractBrowser {

    public static final int DOCUMENT_READY_TIMEOUT = 20000; // ms

    private final String initialURL;
    private final boolean headless;
    private SessionFactory factory;
    private Session session;

    public ChromeBrowser(String initialURL, boolean headless) {

        this.initialURL = initialURL;
        this.headless = headless;

        Launcher launcher = new Launcher();
        List<String> params;
        if (headless) {
            params = Arrays.asList("--headless", "--disable-gpu");
        } else {
            params = new ArrayList<>();
        }

        try {
            factory = launcher.launch(params);
            session = factory.create();
            session.navigate(initialURL);
            session.waitDocumentReady(DOCUMENT_READY_TIMEOUT);
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                close();
            } catch (Exception closeEx) {
                closeEx.printStackTrace();
            }
        }

    }

    @Override
    public Object evaluateJavaScript(String js) {
        return session.evaluate(js);
    }

    @Override
    public void navigate(String url) {
        session.navigate(url);
    }

    /**
     * The sreenshot is a png file
     * @return
     */
    @Override
    public BufferedImage takeScreenshot() {
        return bufferedImageFromBytes(session.captureScreenshot());
    }

    @Override
    public BufferedImage takeScreenshot(String elementXPath) {

        Rectangle boundingBox = getBoundingBox(elementXPath);

        if (boundingBox != null && boundingBox.getWidth() != 0 && boundingBox.getHeight() != 0) {

            Viewport viewport = new Viewport();
            viewport.setX(boundingBox.getX());
            viewport.setY(boundingBox.getY());
            viewport.setWidth(boundingBox.getWidth());
            viewport.setHeight(boundingBox.getHeight());
            viewport.setScale(1D);
            try {
                byte[] data = session.getCommand().getPage().captureScreenshot(Png, 100, viewport, true);
                resetViewPort();
                if (null != data) {
                    return bufferedImageFromBytes(data);
                }
            } catch (CommandException ex) {
                ex.printStackTrace();
            }

        }
        return null;
    }

    public void resetViewPort() {
        Emulation emulation = session.getCommand().getEmulation();
        emulation.clearDeviceMetricsOverride();
        emulation.resetPageScaleFactor();
    }

    public void fitWindowContents() {
        Page page = session.getCommand().getPage();
        GetLayoutMetricsResult metrics = page.getLayoutMetrics();
        Rect cs = metrics.getContentSize();
        Emulation emulation = session.getCommand().getEmulation();
        emulation.setDeviceMetricsOverride(cs.getWidth().intValue(), cs.getHeight().intValue(), 1D, false);
        emulation.setVisibleSize(cs.getWidth().intValue(), cs.getHeight().intValue());
        emulation.setPageScaleFactor(1D);
    }

    public void normalizeDOM() {
        List<Node> flattenedDocument = session.getCommand().getDOM().getFlattenedDocument();

        for (Node node : flattenedDocument) {
            Integer nodeID = node.getNodeId();
            /*List<CSSComputedStyleProperty> computedStyleForNode = session.getCommand().getCSS().getComputedStyleForNode(nodeID);
            for (CSSComputedStyleProperty cssComputedStyleProperty : computedStyleForNode) {

            }*/

            if (null == node.getShadowRootType()) {
                if (node.getNodeType() == 1) { // Element
                    String name = node.getNodeName();
                    if ("img".equals(name.toLowerCase())) {
                        session.getCommand().getDOM().setAttributeValue(nodeID, "src",
                                "data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAUA\n" +
                                        "AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO\n" +
                                        "9TXL0Y4OHwAAAABJRU5ErkJggg==");
                    }
                } else if (node.getNodeType() == 3) { // Text
                    Node parentNode = session.getCommand().getDOM()
                            .describeNode(node.getParentId(), null, null, 1, false);
                    String parentNodeName = parentNode.getNodeName().toLowerCase();
                    if (!"script".equals(parentNodeName) && !"style".equals(parentNodeName)) {
                        try {
                            session.getCommand().getDOM().setOuterHTML(node.getNodeId(), "//TEXT");
                        } catch (CommandException ce) {
                            //node.getShadowRootType()
                        }
                    }
                }
            }
        }

    }

    @Override
    public String getDOM() {
        return session.getContent();
    }

    @Override
    public Object callJSFunction(String functionName, Object... arguments) {
        return session.callFunction(functionName, String.class, arguments);
    }

    @Override
    public void close() throws Exception {
        if (null != factory) {
            factory.close();
        }
    }

    @Override
    public Rectangle getBoundingBox(String xpath) {
        /*
         * The scrolling should be taken into account:
         * https://developer.mozilla.org/en-US/docs/Web/API/Element/getBoundingClientRect
         */
        Object executedJavaScript;
        String javascript = "window.scrollX";
        executedJavaScript = evaluateJavaScript(javascript);
        int windowScrollX = getIntValue(executedJavaScript);
        javascript = "window.scrollY";
        executedJavaScript = evaluateJavaScript(javascript);
        int windowScrollY = getIntValue(executedJavaScript);
        javascript = "JSON.stringify(document.evaluate('%s', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.getBoundingClientRect())";
        javascript = String.format(javascript, xpath.replace("'", "\\'"));
        executedJavaScript = evaluateJavaScript(javascript);
        if (null != executedJavaScript) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (new Gson()).fromJson(executedJavaScript.toString(), Map.class);
            int x = getIntValue(map.get("left")) + windowScrollX;
            int y = getIntValue(map.get("top")) + windowScrollY;
            int width = getIntValue(map.get("width"));
            int height = getIntValue(map.get("height"));
            return new Rectangle(x, y, width, height);
        }
        return null;
    }

    private int getIntValue(Object object) {
        if (object instanceof Number) {
            Number n = (Number) object;
            return n.intValue();
        }
        return 0;
    }

    private BufferedImage bufferedImageFromBytes(byte[] imageData) {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        try {
            return ImageIO.read(bais);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
