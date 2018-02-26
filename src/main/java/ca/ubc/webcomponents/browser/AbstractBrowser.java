package ca.ubc.webcomponents.browser;

public interface AbstractBrowser extends AutoCloseable {

    public Object evaluateJavaScript(String js);

    public Object callJSFunction(String functionName, Object... arguments);

    public void navigate(String url);


    public String getDOM();

}
