package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Set;


/**
 * A network of nodes and directed edges that describes some aspect of the structure of the code (for example, a call graph).
 */
@SuppressWarnings("DuplicatedCode")
public class Graph implements PropertyOwner {

    /**
     * Encapsulates a message intended to be read by the end user.
     */
    @SerializedName("description")
    @Expose
    private Message description;
    /**
     * An array of node objects representing the nodes of the graph.
     */
    @SerializedName("nodes")
    @Expose
    private Set<Node> nodes = null;
    /**
     * An array of edge objects representing the edges of the graph.
     */
    @SerializedName("edges")
    @Expose
    private Set<Edge> edges = null;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * Encapsulates a message intended to be read by the end user.
     */
    public Message getDescription() {
        return description;
    }

    /**
     * Encapsulates a message intended to be read by the end user.
     */
    public void setDescription(Message description) {
        this.description = description;
    }

    public Graph withDescription(Message description) {
        this.description = description;
        return this;
    }

    /**
     * An array of node objects representing the nodes of the graph.
     */
    public Set<Node> getNodes() {
        return nodes;
    }

    /**
     * An array of node objects representing the nodes of the graph.
     */
    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    public Graph withNodes(Set<Node> nodes) {
        this.nodes = nodes;
        return this;
    }

    /**
     * An array of edge objects representing the edges of the graph.
     */
    public Set<Edge> getEdges() {
        return edges;
    }

    /**
     * An array of edge objects representing the edges of the graph.
     */
    public void setEdges(Set<Edge> edges) {
        this.edges = edges;
    }

    public Graph withEdges(Set<Edge> edges) {
        this.edges = edges;
        return this;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    @Override
    public PropertyBag getProperties() {
        return properties;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    @Override
    public void setProperties(PropertyBag properties) {
        this.properties = properties;
    }

    public Graph withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Graph.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null) ? "<null>" : this.description));
        sb.append(',');
        sb.append("nodes");
        sb.append('=');
        sb.append(((this.nodes == null) ? "<null>" : this.nodes));
        sb.append(',');
        sb.append("edges");
        sb.append('=');
        sb.append(((this.edges == null) ? "<null>" : this.edges));
        sb.append(',');
        sb.append("properties");
        sb.append('=');
        sb.append(((this.properties == null) ? "<null>" : this.properties));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.edges == null) ? 0 : this.edges.hashCode()));
        result = ((result * 31) + ((this.description == null) ? 0 : this.description.hashCode()));
        result = ((result * 31) + ((this.nodes == null) ? 0 : this.nodes.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Graph)) {
            return false;
        }
        Graph rhs = ((Graph) other);
        //noinspection ConstantValue,EqualsReplaceableByObjectsCall,StringEquality,NumberEquality
        return (((((this.edges == rhs.edges) || ((this.edges != null) && this.edges.equals(rhs.edges))) && ((this.description == rhs.description) || ((this.description != null) && this.description.equals(rhs.description)))) && ((this.nodes == rhs.nodes) || ((this.nodes != null) && this.nodes.equals(rhs.nodes)))) && ((this.properties == rhs.properties) || ((this.properties != null) && this.properties.equals(rhs.properties))));
    }

}
