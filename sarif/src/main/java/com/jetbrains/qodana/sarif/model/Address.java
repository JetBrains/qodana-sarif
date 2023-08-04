package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * A physical or virtual address, or a range of addresses, in an 'addressable region' (memory or a binary file).
 */
@SuppressWarnings({"DuplicatedCode", "unused"})
public class Address {

    /**
     * The address expressed as a byte offset from the start of the addressable region.
     */
    @SerializedName("absoluteAddress")
    @Expose
    private Integer absoluteAddress = null;
    /**
     * The address expressed as a byte offset from the absolute address of the top-most parent object.
     */
    @SerializedName("relativeAddress")
    @Expose
    private Integer relativeAddress;
    /**
     * The number of bytes in this range of addresses.
     */
    @SerializedName("length")
    @Expose
    private Integer length;
    /**
     * An open-ended string that identifies the address kind. 'data', 'function', 'header','instruction', 'module', 'page', 'section', 'segment', 'stack', 'stackFrame', 'table' are well-known values.
     */
    @SerializedName("kind")
    @Expose
    private String kind;
    /**
     * A name that is associated with the address, e.g., '.text'.
     */
    @SerializedName("name")
    @Expose
    private String name;
    /**
     * A human-readable fully qualified name that is associated with the address.
     */
    @SerializedName("fullyQualifiedName")
    @Expose
    private String fullyQualifiedName;
    /**
     * The byte offset of this address from the absolute or relative address of the parent object.
     */
    @SerializedName("offsetFromParent")
    @Expose
    private Integer offsetFromParent;
    /**
     * The index within run.addresses of the cached object for this address.
     */
    @SerializedName("index")
    @Expose
    private Integer index = null;
    /**
     * The index within run.addresses of the parent object.
     */
    @SerializedName("parentIndex")
    @Expose
    private Integer parentIndex = null;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * The address expressed as a byte offset from the start of the addressable region.
     */
    @SuppressWarnings("unused")
    public Integer getAbsoluteAddress() {
        return absoluteAddress;
    }

    /**
     * The address expressed as a byte offset from the start of the addressable region.
     */
    @SuppressWarnings("unused")
    public void setAbsoluteAddress(Integer absoluteAddress) {
        this.absoluteAddress = absoluteAddress;
    }

    @SuppressWarnings("unused")
    public Address withAbsoluteAddress(Integer absoluteAddress) {
        this.absoluteAddress = absoluteAddress;
        return this;
    }

    /**
     * The address expressed as a byte offset from the absolute address of the top-most parent object.
     */
    @SuppressWarnings("unused")
    public Integer getRelativeAddress() {
        return relativeAddress;
    }

    /**
     * The address expressed as a byte offset from the absolute address of the top-most parent object.
     */
    @SuppressWarnings("unused")
    public void setRelativeAddress(Integer relativeAddress) {
        this.relativeAddress = relativeAddress;
    }

    @SuppressWarnings("unused")
    public Address withRelativeAddress(Integer relativeAddress) {
        this.relativeAddress = relativeAddress;
        return this;
    }

    /**
     * The number of bytes in this range of addresses.
     */
    @SuppressWarnings("unused")
    public Integer getLength() {
        return length;
    }

    /**
     * The number of bytes in this range of addresses.
     */
    @SuppressWarnings("unused")
    public void setLength(Integer length) {
        this.length = length;
    }

    @SuppressWarnings("unused")
    public Address withLength(Integer length) {
        this.length = length;
        return this;
    }

    /**
     * An open-ended string that identifies the address kind. 'data', 'function', 'header','instruction', 'module', 'page', 'section', 'segment', 'stack', 'stackFrame', 'table' are well-known values.
     */
    @SuppressWarnings("unused")
    public String getKind() {
        return kind;
    }

    /**
     * An open-ended string that identifies the address kind. 'data', 'function', 'header','instruction', 'module', 'page', 'section', 'segment', 'stack', 'stackFrame', 'table' are well-known values.
     */
    @SuppressWarnings("unused")
    public void setKind(String kind) {
        this.kind = kind;
    }

    @SuppressWarnings("unused")
    public Address withKind(String kind) {
        this.kind = kind;
        return this;
    }

    /**
     * A name that is associated with the address, e.g., '.text'.
     */
    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    /**
     * A name that is associated with the address, e.g., '.text'.
     */
    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unused")
    public Address withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * A human-readable fully qualified name that is associated with the address.
     */
    @SuppressWarnings("unused")
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    /**
     * A human-readable fully qualified name that is associated with the address.
     */
    @SuppressWarnings("unused")
    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    @SuppressWarnings("unused")
    public Address withFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
        return this;
    }

    /**
     * The byte offset of this address from the absolute or relative address of the parent object.
     */
    @SuppressWarnings("unused")
    public Integer getOffsetFromParent() {
        return offsetFromParent;
    }

    /**
     * The byte offset of this address from the absolute or relative address of the parent object.
     */
    @SuppressWarnings("unused")
    public void setOffsetFromParent(Integer offsetFromParent) {
        this.offsetFromParent = offsetFromParent;
    }

    @SuppressWarnings("unused")
    public Address withOffsetFromParent(Integer offsetFromParent) {
        this.offsetFromParent = offsetFromParent;
        return this;
    }

    /**
     * The index within run.addresses of the cached object for this address.
     */
    @SuppressWarnings("unused")
    public Integer getIndex() {
        return index;
    }

    /**
     * The index within run.addresses of the cached object for this address.
     */
    @SuppressWarnings("unused")
    public void setIndex(Integer index) {
        this.index = index;
    }

    @SuppressWarnings("unused")
    public Address withIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * The index within run.addresses of the parent object.
     */
    @SuppressWarnings("unused")
    public Integer getParentIndex() {
        return parentIndex;
    }

    /**
     * The index within run.addresses of the parent object.
     */
    @SuppressWarnings("unused")
    public void setParentIndex(Integer parentIndex) {
        this.parentIndex = parentIndex;
    }

    @SuppressWarnings("unused")
    public Address withParentIndex(Integer parentIndex) {
        this.parentIndex = parentIndex;
        return this;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SuppressWarnings("unused")
    public PropertyBag getProperties() {
        return properties;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SuppressWarnings("unused")
    public void setProperties(PropertyBag properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unused")
    public Address withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Address.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("absoluteAddress");
        sb.append('=');
        sb.append(((this.absoluteAddress == null) ? "<null>" : this.absoluteAddress));
        sb.append(',');
        sb.append("relativeAddress");
        sb.append('=');
        sb.append(((this.relativeAddress == null) ? "<null>" : this.relativeAddress));
        sb.append(',');
        sb.append("length");
        sb.append('=');
        sb.append(((this.length == null) ? "<null>" : this.length));
        sb.append(',');
        sb.append("kind");
        sb.append('=');
        sb.append(((this.kind == null) ? "<null>" : this.kind));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null) ? "<null>" : this.name));
        sb.append(',');
        sb.append("fullyQualifiedName");
        sb.append('=');
        sb.append(((this.fullyQualifiedName == null) ? "<null>" : this.fullyQualifiedName));
        sb.append(',');
        sb.append("offsetFromParent");
        sb.append('=');
        sb.append(((this.offsetFromParent == null) ? "<null>" : this.offsetFromParent));
        sb.append(',');
        sb.append("index");
        sb.append('=');
        sb.append(((this.index == null) ? "<null>" : this.index));
        sb.append(',');
        sb.append("parentIndex");
        sb.append('=');
        sb.append(((this.parentIndex == null) ? "<null>" : this.parentIndex));
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
        result = ((result * 31) + ((this.offsetFromParent == null) ? 0 : this.offsetFromParent.hashCode()));
        result = ((result * 31) + ((this.parentIndex == null) ? 0 : this.parentIndex.hashCode()));
        result = ((result * 31) + ((this.relativeAddress == null) ? 0 : this.relativeAddress.hashCode()));
        result = ((result * 31) + ((this.kind == null) ? 0 : this.kind.hashCode()));
        result = ((result * 31) + ((this.length == null) ? 0 : this.length.hashCode()));
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.index == null) ? 0 : this.index.hashCode()));
        result = ((result * 31) + ((this.fullyQualifiedName == null) ? 0 : this.fullyQualifiedName.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        result = ((result * 31) + ((this.absoluteAddress == null) ? 0 : this.absoluteAddress.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Address)) {
            return false;
        }
        Address rhs = ((Address) other);
        //noinspection ConstantValue,EqualsReplaceableByObjectsCall,StringEquality,NumberEquality
        return (((((((((((this.offsetFromParent == rhs.offsetFromParent) || ((this.offsetFromParent != null) && this.offsetFromParent.equals(rhs.offsetFromParent))) && ((this.parentIndex == rhs.parentIndex) || ((this.parentIndex != null) && this.parentIndex.equals(rhs.parentIndex)))) && ((this.relativeAddress == rhs.relativeAddress) || ((this.relativeAddress != null) && this.relativeAddress.equals(rhs.relativeAddress)))) && ((this.kind == rhs.kind) || ((this.kind != null) && this.kind.equals(rhs.kind)))) && ((this.length == rhs.length) || ((this.length != null) && this.length.equals(rhs.length)))) && ((this.name == rhs.name) || ((this.name != null) && this.name.equals(rhs.name)))) && ((this.index == rhs.index) || ((this.index != null) && this.index.equals(rhs.index)))) && ((this.fullyQualifiedName == rhs.fullyQualifiedName) || ((this.fullyQualifiedName != null) && this.fullyQualifiedName.equals(rhs.fullyQualifiedName)))) && ((this.properties == rhs.properties) || ((this.properties != null) && this.properties.equals(rhs.properties)))) && ((this.absoluteAddress == rhs.absoluteAddress) || ((this.absoluteAddress != null) && this.absoluteAddress.equals(rhs.absoluteAddress))));
    }

}
