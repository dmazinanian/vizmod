package ca.ubc.vizmod.model;

public enum ParameterizationReason {
    /**
     * Tag names of the mapping elements are different
     */
    DIFFERENT_TAG_NAMES,
    /**
     * Mapping nodes are text nodes, yet their text contents are different
     */
    DIFFERENT_TEXT_NODE_VALUE,
    /**
     * TODO this might change
     * Tags are the same, number of children are the same, yet the number of children are different
     */
    DIFFERENT_CHILD_COUNT,
    /**
     * Tags are the same, number of children are the same, attribute values are different
     */
    DIFFERENT_ATTRIBUTE_VALUES
}
