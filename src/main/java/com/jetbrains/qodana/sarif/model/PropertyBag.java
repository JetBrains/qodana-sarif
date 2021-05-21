
package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.processing.Generated;
import java.util.Set;


/**
 * Key/value pairs that provide additional information about the object.
 * 
 */
@Generated("jsonschema2pojo")
public class PropertyBag {

    /**
     * A set of distinct strings that provide additional information.
     * 
     */
    @SerializedName("tags")
    @Expose
    private Set<String> tags = null;

    /**
     * A set of distinct strings that provide additional information.
     * 
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * A set of distinct strings that provide additional information.
     * 
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public PropertyBag withTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PropertyBag.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("tags");
        sb.append('=');
        sb.append(((this.tags == null)?"<null>":this.tags));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.tags == null)? 0 :this.tags.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PropertyBag) == false) {
            return false;
        }
        PropertyBag rhs = ((PropertyBag) other);
        return ((this.tags == rhs.tags)||((this.tags!= null)&&this.tags.equals(rhs.tags)));
    }

}
