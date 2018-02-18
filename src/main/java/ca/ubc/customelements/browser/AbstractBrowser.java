package ca.ubc.customelements.browser;

public interface AbstractBrowser {

    public Object evaluateJavaScript(String js);

    public Object callJSFunction(String functionName, Object... arguments);

    public void navigate(String url);

    public void close();

    public String getDOM();

}
