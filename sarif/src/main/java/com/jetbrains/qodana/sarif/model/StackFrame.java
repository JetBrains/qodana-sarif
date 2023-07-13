package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;


/**
 * A function call within a stack trace.
 */
public class StackFrame {

    /**
     * A location within a programming artifact.
     */
    @SerializedName("location")
    @Expose
    private Location location;
    /**
     * The name of the module that contains the code of this stack frame.
     */
    @SerializedName("module")
    @Expose
    private String module;
    /**
     * The thread identifier of the stack frame.
     */
    @SerializedName("threadId")
    @Expose
    private Integer threadId;
    /**
     * The parameters of the call that is executing.
     */
    @SerializedName("parameters")
    @Expose
    private List<String> parameters = null;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * A location within a programming artifact.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * A location within a programming artifact.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    public StackFrame withLocation(Location location) {
        this.location = location;
        return this;
    }

    /**
     * The name of the module that contains the code of this stack frame.
     */
    public String getModule() {
        return module;
    }

    /**
     * The name of the module that contains the code of this stack frame.
     */
    public void setModule(String module) {
        this.module = module;
    }

    public StackFrame withModule(String module) {
        this.module = module;
        return this;
    }

    /**
     * The thread identifier of the stack frame.
     */
    public Integer getThreadId() {
        return threadId;
    }

    /**
     * The thread identifier of the stack frame.
     */
    public void setThreadId(Integer threadId) {
        this.threadId = threadId;
    }

    public StackFrame withThreadId(Integer threadId) {
        this.threadId = threadId;
        return this;
    }

    /**
     * The parameters of the call that is executing.
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * The parameters of the call that is executing.
     */
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public StackFrame withParameters(List<String> parameters) {
        this.parameters = parameters;
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

    public StackFrame withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(StackFrame.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("location");
        sb.append('=');
        sb.append(((this.location == null) ? "<null>" : this.location));
        sb.append(',');
        sb.append("module");
        sb.append('=');
        sb.append(((this.module == null) ? "<null>" : this.module));
        sb.append(',');
        sb.append("threadId");
        sb.append('=');
        sb.append(((this.threadId == null) ? "<null>" : this.threadId));
        sb.append(',');
        sb.append("parameters");
        sb.append('=');
        sb.append(((this.parameters == null) ? "<null>" : this.parameters));
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
        result = ((result * 31) + ((this.threadId == null) ? 0 : this.threadId.hashCode()));
        result = ((result * 31) + ((this.location == null) ? 0 : this.location.hashCode()));
        result = ((result * 31) + ((this.parameters == null) ? 0 : this.parameters.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        result = ((result * 31) + ((this.module == null) ? 0 : this.module.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof StackFrame)) {
            return false;
        }
        StackFrame rhs = ((StackFrame) other);
        return (((((Objects.equals(this.threadId, rhs.threadId)) && (Objects.equals(this.location, rhs.location))) && (Objects.equals(this.parameters, rhs.parameters))) && (Objects.equals(this.properties, rhs.properties))) && (Objects.equals(this.module, rhs.module)));
    }

}
