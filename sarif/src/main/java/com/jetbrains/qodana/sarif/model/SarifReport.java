
package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Static Analysis Results Format (SARIF) Version 2.1.0 JSON Schema
 * <p>
 * Static Analysis Results Format (SARIF) Version 2.1.0 JSON Schema: a standard format for the output of static analysis tools.
 * 
 */

public class SarifReport {

    /**
     * The URI of the JSON schema corresponding to the version.
     * 
     */
    @SerializedName("$schema")
    @Expose
    private URI $schema;
    /**
     * The SARIF format version of this log file.
     * (Required)
     * 
     */
    @SerializedName("version")
    @Expose
    private SarifReport.Version version;
    /**
     * The set of runs contained in this log file.
     * (Required)
     * 
     */
    @SerializedName("runs")
    @Expose
    private List<Run> runs = null;
    /**
     * References to external property files that share data between runs.
     * 
     */
    @SerializedName("inlineExternalProperties")
    @Expose
    private Set<ExternalProperties> inlineExternalProperties = null;
    /**
     * Key/value pairs that provide additional information about the object.
     * 
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * No args constructor for use in serialization
     * 
     */
    public SarifReport() {
    }

    /**
     * 
     * @param version
     * @param runs
     */
    public SarifReport(Version version, List<Run> runs) {
        super();
        this.version = version;
        this.runs = runs;
    }

    /**
     * The URI of the JSON schema corresponding to the version.
     *
     */
    public URI get$schema() {
        return $schema;
    }

    /**
     * The URI of the JSON schema corresponding to the version.
     *
     */
    public void set$schema(URI $schema) {
        this.$schema = $schema;
    }

    public SarifReport with$schema(URI $schema) {
        this.$schema = $schema;
        return this;
    }

    /**
     * The SARIF format version of this log file.
     * (Required)
     *
     */
    public Version getVersion() {
        return version;
    }

    /**
     * The SARIF format version of this log file.
     * (Required)
     *
     */
    public void setVersion(Version version) {
        this.version = version;
    }

    public SarifReport withVersion(Version version) {
        this.version = version;
        return this;
    }

    /**
     * The set of runs contained in this log file.
     * (Required)
     *
     */
    public List<Run> getRuns() {
        return runs;
    }

    /**
     * The set of runs contained in this log file.
     * (Required)
     *
     */
    public void setRuns(List<Run> runs) {
        this.runs = runs;
    }

    public SarifReport withRuns(List<Run> runs) {
        this.runs = runs;
        return this;
    }

    /**
     * References to external property files that share data between runs.
     *
     */
    public Set<ExternalProperties> getInlineExternalProperties() {
        return inlineExternalProperties;
    }

    /**
     * References to external property files that share data between runs.
     *
     */
    public void setInlineExternalProperties(Set<ExternalProperties> inlineExternalProperties) {
        this.inlineExternalProperties = inlineExternalProperties;
    }

    public SarifReport withInlineExternalProperties(Set<ExternalProperties> inlineExternalProperties) {
        this.inlineExternalProperties = inlineExternalProperties;
        return this;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     *
     */
    public PropertyBag getProperties() {
        return properties;
    }

    /**
     * Key/value pairs that provide additional information about the object.
     *
     */
    public void setProperties(PropertyBag properties) {
        this.properties = properties;
    }

    public SarifReport withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SarifReport.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("$schema");
        sb.append('=');
        sb.append(((this.$schema == null)?"<null>":this.$schema));
        sb.append(',');
        sb.append("version");
        sb.append('=');
        sb.append(((this.version == null)?"<null>":this.version));
        sb.append(',');
        sb.append("runs");
        sb.append('=');
        sb.append(((this.runs == null)?"<null>":this.runs));
        sb.append(',');
        sb.append("inlineExternalProperties");
        sb.append('=');
        sb.append(((this.inlineExternalProperties == null)?"<null>":this.inlineExternalProperties));
        sb.append(',');
        sb.append("properties");
        sb.append('=');
        sb.append(((this.properties == null)?"<null>":this.properties));
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
        result = ((result* 31)+((this.inlineExternalProperties == null)? 0 :this.inlineExternalProperties.hashCode()));
        result = ((result* 31)+((this.$schema == null)? 0 :this.$schema.hashCode()));
        result = ((result* 31)+((this.version == null)? 0 :this.version.hashCode()));
        result = ((result* 31)+((this.runs == null)? 0 :this.runs.hashCode()));
        result = ((result* 31)+((this.properties == null)? 0 :this.properties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SarifReport) == false) {
            return false;
        }
        SarifReport rhs = ((SarifReport) other);
        return ((((((this.inlineExternalProperties == rhs.inlineExternalProperties)||((this.inlineExternalProperties!= null)&&this.inlineExternalProperties.equals(rhs.inlineExternalProperties)))&&((this.$schema == rhs.$schema)||((this.$schema!= null)&&this.$schema.equals(rhs.$schema))))&&((this.version == rhs.version)||((this.version!= null)&&this.version.equals(rhs.version))))&&((this.runs == rhs.runs)||((this.runs!= null)&&this.runs.equals(rhs.runs))))&&((this.properties == rhs.properties)||((this.properties!= null)&&this.properties.equals(rhs.properties))));
    }


    /**
     * The SARIF format version of this log file.
     *
     */

    public enum Version {

        @SerializedName("2.1.0")
        _2_1_0("2.1.0");
        private final String value;
        private final static Map<String, Version> CONSTANTS = new HashMap<String, Version>();

        static {
            for (Version c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Version(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Version fromValue(String value) {
            Version constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
