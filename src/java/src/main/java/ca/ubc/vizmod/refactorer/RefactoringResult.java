package ca.ubc.vizmod.refactorer;

import org.w3c.dom.Document;

import java.util.List;

public class RefactoringResult {

    private Document document;
    private String componentBody;
    private List<List<String>> parameterizedValues;

    public RefactoringResult(Document document, String componentBody, List<List<String>> parameterizedValues) {
        this.document = document;
        this.componentBody = componentBody;
        this.parameterizedValues = parameterizedValues;
    }

    public Document getDocument() {
        return document;
    }

    public String getComponentBody() {
        return componentBody;
    }

    public List<List<String>> getParameterizedValues() {
        return parameterizedValues;
    }
}
