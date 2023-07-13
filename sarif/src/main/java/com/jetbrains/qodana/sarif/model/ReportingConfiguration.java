package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * Information about a rule or notification that can be configured at runtime.
 */
public class ReportingConfiguration {

    /**
     * Specifies whether the report may be produced during the scan.
     */
    @SerializedName("enabled")
    @Expose
    private Boolean enabled = true;
    /**
     * Specifies the failure level for the report.
     */
    @SerializedName("level")
    @Expose
    private Level level = Level.WARNING;
    /**
     * Specifies the relative priority of the report. Used for analysis output only.
     */
    @SerializedName("rank")
    @Expose
    private Double rank = null;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("parameters")
    @Expose
    private PropertyBag parameters;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * Specifies whether the report may be produced during the scan.
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Specifies whether the report may be produced during the scan.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public ReportingConfiguration withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Specifies the failure level for the report.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Specifies the failure level for the report.
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    public ReportingConfiguration withLevel(Level level) {
        this.level = level;
        return this;
    }

    /**
     * Specifies the relative priority of the report. Used for analysis output only.
     */
    public Double getRank() {
        return rank;
    }

    /**
     * Specifies the relative priority of the report. Used for analysis output only.
     */
    public void setRank(Double rank) {
        this.rank = rank;
    }

    public ReportingConfiguration withRank(Double rank) {
        this.rank = rank;
        return this;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    public PropertyBag getParameters() {
        return parameters;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     */
    public void setParameters(PropertyBag parameters) {
        this.parameters = parameters;
    }

    public ReportingConfiguration withParameters(PropertyBag parameters) {
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

    public ReportingConfiguration withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ReportingConfiguration.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("enabled");
        sb.append('=');
        sb.append(((this.enabled == null) ? "<null>" : this.enabled));
        sb.append(',');
        sb.append("level");
        sb.append('=');
        sb.append(((this.level == null) ? "<null>" : this.level));
        sb.append(',');
        sb.append("rank");
        sb.append('=');
        sb.append(((this.rank == null) ? "<null>" : this.rank));
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
        result = ((result * 31) + ((this.rank == null) ? 0 : this.rank.hashCode()));
        result = ((result * 31) + ((this.level == null) ? 0 : this.level.hashCode()));
        result = ((result * 31) + ((this.parameters == null) ? 0 : this.parameters.hashCode()));
        result = ((result * 31) + ((this.enabled == null) ? 0 : this.enabled.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ReportingConfiguration)) {
            return false;
        }
        ReportingConfiguration rhs = ((ReportingConfiguration) other);
        return (((((Objects.equals(this.rank, rhs.rank)) && (Objects.equals(this.level, rhs.level))) && (Objects.equals(this.parameters, rhs.parameters))) && (Objects.equals(this.enabled, rhs.enabled))) && (Objects.equals(this.properties, rhs.properties)));
    }
}
