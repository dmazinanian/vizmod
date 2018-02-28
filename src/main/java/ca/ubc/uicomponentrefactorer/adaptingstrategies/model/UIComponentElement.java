package ca.ubc.uicomponentrefactorer.adaptingstrategies.model;

import java.util.*;

public class UIComponentElement {

    protected final UIComponentElement parent;
    protected final String name;
    protected final Map<String, AttributeValue> attributes = new HashMap<>();
    protected final List<UIComponentElement> children = new ArrayList<>();
    protected final List<String> correspondingOriginalNodesXPaths;

    public UIComponentElement(UIComponentElement parent, List<String> correspondingOriginalNodeXPaths, String name) {
        this.parent = parent;
        this.name = name;
        if (null != correspondingOriginalNodeXPaths) {
            this.correspondingOriginalNodesXPaths = new ArrayList<>(correspondingOriginalNodeXPaths);
        } else {
            this.correspondingOriginalNodesXPaths = null;
        }
    }

    public UIComponentElement(List<String> correspondingOriginalNodeXPaths, String name) {
        this(null, correspondingOriginalNodeXPaths, name);
    }

    public UIComponentElement getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void addAttribute(String name, AttributeValue value) {
        attributes.put(name, value);
    }

    public Map<String, AttributeValue> getAttributes() {
        return attributes;
    }

    public void addChild(UIComponentElement child) {
        this.children.add(child);
    }

    public List<UIComponentElement> getChildren() {
        return children;
    }

    /**
     * For what original nodes this {@link UIComponentElement} has been created
     */
    public List<String> getCorrespondingOriginalNodesXPaths() {
        if (null != correspondingOriginalNodesXPaths) {
            return new ArrayList<>(correspondingOriginalNodesXPaths);
        } else {
            return new ArrayList<>();
        }
    }

    public List<UIComponentElement> traverseChildrenBFS() {
        List<UIComponentElement> toReturn = new ArrayList<>();
        Queue<UIComponentElement> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            UIComponentElement node = queue.remove();
            toReturn.add(node);
            queue.addAll(node.getChildren());
        }
        return toReturn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UIComponentElement that = (UIComponentElement) o;
        return Objects.equals(parent, that.parent) &&
                Objects.equals(name, that.name) &&
                Objects.equals(correspondingOriginalNodesXPaths, that.correspondingOriginalNodesXPaths);
    }

    private int hashCode = -1;
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = Objects.hash(parent, name, correspondingOriginalNodesXPaths);
        }
        return hashCode;
    }
}
