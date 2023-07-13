package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * Identifies a particular toolComponent object, either the driver or an extension.
 */
public class ToolComponentReference {

    /**
     * The 'name' property of the referenced toolComponent.
     */
    @SerializedName("name")
    @Expose
    private String name;
    /**
     * An index into the referenced toolComponent in tool.extensions.
     */
    @SerializedName("index")
    @Expose
    private Integer index = null;
    /**
     * The 'guid' property of the referenced toolComponent.
     */
    @SerializedName("guid")
    @Expose
    private String guid;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * The 'name' property of the referenced toolComponent.
     */
    public String getName() {
        return name;
    }

    /**
     * The 'name' property of the referenced toolComponent.
     */
    public void setName(String name) {
        this.name = name;
    }

    public ToolComponentReference withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * An index into the referenced toolComponent in tool.extensions.
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * An index into the referenced toolComponent in tool.extensions.
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    public ToolComponentReference withIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * The 'guid' property of the referenced toolComponent.
     */
    public String getGuid() {
        return guid;
    }

    /**
     * The 'guid' property of the referenced toolComponent.
     */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    public ToolComponentReference withGuid(String guid) {
        this.guid = guid;
        return this;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    public PropertyBag getProperties() {
        return properties;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    public void setProperties(PropertyBag properties) {
        this.properties = properties;
    }

    public ToolComponentReference withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ToolComponentReference.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null) ? "<null>" : this.name));
        sb.append(',');
        sb.append("index");
        sb.append('=');
        sb.append(((this.index == null) ? "<null>" : this.index));
        sb.append(',');
        sb.append("guid");
        sb.append('=');
        sb.append(((this.guid == null) ? "<null>" : this.guid));
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
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.index == null) ? 0 : this.index.hashCode()));
        result = ((result * 31) + ((this.guid == null) ? 0 : this.guid.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ToolComponentReference)) {
            return false;
        }
        ToolComponentReference rhs = ((ToolComponentReference) other);
        return ((((Objects.equals(this.name, rhs.name)) && (Objects.equals(this.index, rhs.index))) && (Objects.equals(this.guid, rhs.guid))) && (Objects.equals(this.properties, rhs.properties)));
    }

}
