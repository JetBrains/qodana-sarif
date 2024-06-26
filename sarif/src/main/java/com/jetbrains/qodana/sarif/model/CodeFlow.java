package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;


/**
 * A set of threadFlows which together describe a pattern of code execution relevant to detecting a result.
 */
@SuppressWarnings("DuplicatedCode")
public class CodeFlow implements PropertyOwner {

    /**
     * Encapsulates a message intended to be read by the end user.
     */
    @SerializedName("message")
    @Expose
    private Message message;
    /**
     * An array of one or more unique threadFlow objects, each of which describes the progress of a program through a thread of execution.
     * (Required)
     */
    @SerializedName("threadFlows")
    @Expose
    private List<ThreadFlow> threadFlows = null;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * No args constructor for use in serialization
     */
    public CodeFlow() {
    }

    /**
     * @param threadFlows An array of one or more unique threadFlow objects,
     *                    each of which describes the progress of a program through a thread of execution.
     */
    public CodeFlow(List<ThreadFlow> threadFlows) {
        super();
        this.threadFlows = threadFlows;
    }

    /**
     * Encapsulates a message intended to be read by the end user.
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Encapsulates a message intended to be read by the end user.
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    public CodeFlow withMessage(Message message) {
        this.message = message;
        return this;
    }

    /**
     * An array of one or more unique threadFlow objects, each of which describes the progress of a program through a thread of execution.
     * (Required)
     */
    public List<ThreadFlow> getThreadFlows() {
        return threadFlows;
    }

    /**
     * An array of one or more unique threadFlow objects, each of which describes the progress of a program through a thread of execution.
     * (Required)
     */
    public void setThreadFlows(List<ThreadFlow> threadFlows) {
        this.threadFlows = threadFlows;
    }

    public CodeFlow withThreadFlows(List<ThreadFlow> threadFlows) {
        this.threadFlows = threadFlows;
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

    public CodeFlow withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CodeFlow.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null) ? "<null>" : this.message));
        sb.append(',');
        sb.append("threadFlows");
        sb.append('=');
        sb.append(((this.threadFlows == null) ? "<null>" : this.threadFlows));
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
        result = ((result * 31) + ((this.message == null) ? 0 : this.message.hashCode()));
        result = ((result * 31) + ((this.threadFlows == null) ? 0 : this.threadFlows.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof CodeFlow)) {
            return false;
        }
        CodeFlow rhs = ((CodeFlow) other);
        //noinspection ConstantValue,EqualsReplaceableByObjectsCall,StringEquality,NumberEquality
        return ((((this.message == rhs.message) || ((this.message != null) && this.message.equals(rhs.message))) && ((this.threadFlows == rhs.threadFlows) || ((this.threadFlows != null) && this.threadFlows.equals(rhs.threadFlows)))) && ((this.properties == rhs.properties) || ((this.properties != null) && this.properties.equals(rhs.properties))));
    }

}
