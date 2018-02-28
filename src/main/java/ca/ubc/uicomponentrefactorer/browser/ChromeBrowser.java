package ca.ubc.uicomponentrefactorer.browser;

import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChromeBrowser implements AbstractBrowser {

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
            session.waitDocumentReady();
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
}
