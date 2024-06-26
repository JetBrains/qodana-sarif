package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.*;


/**
 * Describes a single run of an analysis tool, and contains the reported output of that run.
 */
@SuppressWarnings("DuplicatedCode")
public class Run implements PropertyOwner {

    /**
     * The analysis tool that was run.
     * (Required)
     */
    @SerializedName("tool")
    @Expose
    private Tool tool;
    /**
     * Describes the invocation of the analysis tool.
     */
    @SerializedName("invocations")
    @Expose
    private List<Invocation> invocations = null;
    /**
     * Describes how a converter transformed the output of a static analysis tool from the analysis tool's native output format into the SARIF format.
     */
    @SerializedName("conversion")
    @Expose
    private Conversion conversion;
    /**
     * The language of the messages emitted into the log file during this run (expressed as an ISO 639-1 two-letter lowercase culture code) and an optional region (expressed as an ISO 3166-1 two-letter uppercase subculture code associated with a country or region). The casing is recommended but not required (in order for this data to conform to RFC5646).
     */
    @SerializedName("language")
    @Expose
    private String language = "en-US";
    /**
     * Specifies the revision in version control of the artifacts that were scanned.
     */
    @SerializedName("versionControlProvenance")
    @Expose
    private Set<VersionControlDetails> versionControlProvenance = null;
    /**
     * The artifact location specified by each uriBaseId symbol on the machine where the tool originally ran.
     */
    @SerializedName("originalUriBaseIds")
    @Expose
    private OriginalUriBaseIds originalUriBaseIds;
    /**
     * An array of artifact objects relevant to the run.
     */
    @SerializedName("artifacts")
    @Expose
    private Set<Artifact> artifacts = null;
    /**
     * An array of logical locations such as namespaces, types or functions.
     */
    @SerializedName("logicalLocations")
    @Expose
    private Set<LogicalLocation> logicalLocations = null;
    /**
     * An array of zero or more unique graph objects associated with the run.
     */
    @SerializedName("graphs")
    @Expose
    private Set<Graph> graphs = null;
    /**
     * The set of results contained in an SARIF log. The results array can be omitted when a run is solely exporting rules metadata. It must be present (but may be empty) if a log file represents an actual scan.
     * This field can be null, it means it is not read
     */
    @SerializedName("results")
    @Expose
    private List<Result> results = null;
    /**
     * Information that describes a run's identity and role within an engineering system process.
     */
    @SerializedName("automationDetails")
    @Expose
    private RunAutomationDetails automationDetails;
    /**
     * Automation details that describe the aggregate of runs to which this run belongs.
     */
    @SerializedName("runAggregates")
    @Expose
    private Set<RunAutomationDetails> runAggregates = null;
    /**
     * The 'guid' property of a previous SARIF 'run' that comprises the baseline that was used to compute result 'baselineState' properties for the run.
     */
    @SerializedName("baselineGuid")
    @Expose
    private String baselineGuid;
    /**
     * An array of strings used to replace sensitive information in a redaction-aware property.
     */
    @SerializedName("redactionTokens")
    @Expose
    private Set<String> redactionTokens = null;
    /**
     * Specifies the default encoding for any artifact object that refers to a text file.
     */
    @SerializedName("defaultEncoding")
    @Expose
    private String defaultEncoding;
    /**
     * Specifies the default source language for any artifact object that refers to a text file that contains source code.
     */
    @SerializedName("defaultSourceLanguage")
    @Expose
    private String defaultSourceLanguage;
    /**
     * An ordered list of character sequences that were treated as line breaks when computing region information for the run.
     */
    @SerializedName("newlineSequences")
    @Expose
    private Set<String> newlineSequences = new LinkedHashSet<>(Arrays.asList("\r\n", "\n"));
    /**
     * Specifies the unit in which the tool measures columns.
     */
    @SerializedName("columnKind")
    @Expose
    private Run.ColumnKind columnKind;
    /**
     * References to external property files that should be inlined with the content of a root log file.
     */
    @SerializedName("externalPropertyFileReferences")
    @Expose
    private ExternalPropertyFileReferences externalPropertyFileReferences;
    /**
     * An array of threadFlowLocation objects cached at run level.
     */
    @SerializedName("threadFlowLocations")
    @Expose
    private Set<ThreadFlowLocation> threadFlowLocations = null;
    /**
     * An array of toolComponent objects relevant to a taxonomy in which results are categorized.
     */
    @SerializedName("taxonomies")
    @Expose
    private Set<ToolComponent> taxonomies = null;
    /**
     * Addresses associated with this run instance, if any.
     */
    @SerializedName("addresses")
    @Expose
    private List<Address> addresses = null;
    /**
     * The set of available translations of the localized data provided by the tool.
     */
    @SerializedName("translations")
    @Expose
    private Set<ToolComponent> translations = null;
    /**
     * Contains configurations that may potentially override both reportingDescriptor.defaultConfiguration (the tool's default severities) and invocation.configurationOverrides (severities established at run-time from the command line).
     */
    @SerializedName("policies")
    @Expose
    private Set<ToolComponent> policies = null;
    /**
     * An array of request objects cached at run level.
     */
    @SerializedName("webRequests")
    @Expose
    private Set<WebRequest> webRequests = null;
    /**
     * An array of response objects cached at run level.
     */
    @SerializedName("webResponses")
    @Expose
    private Set<WebResponse> webResponses = null;
    /**
     * Defines locations of special significance to SARIF consumers.
     */
    @SerializedName("specialLocations")
    @Expose
    private SpecialLocations specialLocations;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * No args constructor for use in serialization
     */
    public Run() {
    }

    /**
     * @param tool The analysis tool that was run.
     */
    public Run(Tool tool) {
        super();
        this.tool = tool;
    }

    /**
     * The analysis tool that was run.
     * (Required)
     */
    public Tool getTool() {
        return tool;
    }

    /**
     * The analysis tool that was run.
     * (Required)
     */
    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Run withTool(Tool tool) {
        this.tool = tool;
        return this;
    }

    /**
     * Describes the invocation of the analysis tool.
     */
    public List<Invocation> getInvocations() {
        return invocations;
    }

    /**
     * Describes the invocation of the analysis tool.
     */
    public void setInvocations(List<Invocation> invocations) {
        this.invocations = invocations;
    }

    public Run withInvocations(List<Invocation> invocations) {
        this.invocations = invocations;
        return this;
    }

    /**
     * Describes how a converter transformed the output of a static analysis tool from the analysis tool's native output format into the SARIF format.
     */
    public Conversion getConversion() {
        return conversion;
    }

    /**
     * Describes how a converter transformed the output of a static analysis tool from the analysis tool's native output format into the SARIF format.
     */
    public void setConversion(Conversion conversion) {
        this.conversion = conversion;
    }

    public Run withConversion(Conversion conversion) {
        this.conversion = conversion;
        return this;
    }

    /**
     * The language of the messages emitted into the log file during this run (expressed as an ISO 639-1 two-letter lowercase culture code) and an optional region (expressed as an ISO 3166-1 two-letter uppercase subculture code associated with a country or region). The casing is recommended but not required (in order for this data to conform to RFC5646).
     */
    public String getLanguage() {
        return language;
    }

    /**
     * The language of the messages emitted into the log file during this run (expressed as an ISO 639-1 two-letter lowercase culture code) and an optional region (expressed as an ISO 3166-1 two-letter uppercase subculture code associated with a country or region). The casing is recommended but not required (in order for this data to conform to RFC5646).
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public Run withLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * Specifies the revision in version control of the artifacts that were scanned.
     */
    public Set<VersionControlDetails> getVersionControlProvenance() {
        return versionControlProvenance;
    }

    /**
     * Specifies the revision in version control of the artifacts that were scanned.
     */
    public void setVersionControlProvenance(Set<VersionControlDetails> versionControlProvenance) {
        this.versionControlProvenance = versionControlProvenance;
    }

    public Run withVersionControlProvenance(Set<VersionControlDetails> versionControlProvenance) {
        this.versionControlProvenance = versionControlProvenance;
        return this;
    }

    /**
     * The artifact location specified by each uriBaseId symbol on the machine where the tool originally ran.
     */
    public OriginalUriBaseIds getOriginalUriBaseIds() {
        return originalUriBaseIds;
    }

    /**
     * The artifact location specified by each uriBaseId symbol on the machine where the tool originally ran.
     */
    public void setOriginalUriBaseIds(OriginalUriBaseIds originalUriBaseIds) {
        this.originalUriBaseIds = originalUriBaseIds;
    }

    public Run withOriginalUriBaseIds(OriginalUriBaseIds originalUriBaseIds) {
        this.originalUriBaseIds = originalUriBaseIds;
        return this;
    }

    /**
     * An array of artifact objects relevant to the run.
     */
    public Set<Artifact> getArtifacts() {
        return artifacts;
    }

    /**
     * An array of artifact objects relevant to the run.
     */
    public void setArtifacts(Set<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public Run withArtifacts(Set<Artifact> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    /**
     * An array of logical locations such as namespaces, types or functions.
     */
    public Set<LogicalLocation> getLogicalLocations() {
        return logicalLocations;
    }

    /**
     * An array of logical locations such as namespaces, types or functions.
     */
    public void setLogicalLocations(Set<LogicalLocation> logicalLocations) {
        this.logicalLocations = logicalLocations;
    }

    public Run withLogicalLocations(Set<LogicalLocation> logicalLocations) {
        this.logicalLocations = logicalLocations;
        return this;
    }

    /**
     * An array of zero or more unique graph objects associated with the run.
     */
    public Set<Graph> getGraphs() {
        return graphs;
    }

    /**
     * An array of zero or more unique graph objects associated with the run.
     */
    public void setGraphs(Set<Graph> graphs) {
        this.graphs = graphs;
    }

    public Run withGraphs(Set<Graph> graphs) {
        this.graphs = graphs;
        return this;
    }

    /**
     * The set of results contained in an SARIF log. The results array can be omitted when a run is solely exporting rules metadata. It must be present (but may be empty) if a log file represents an actual scan.
     * This can return null, it means it is not read
     */
    public List<Result> getResults() {
        return results;
    }

    /**
     * The set of results contained in an SARIF log. The results array can be omitted when a run is solely exporting rules metadata. It must be present (but may be empty) if a log file represents an actual scan.
     */
    public void setResults(List<Result> results) {
        this.results = results;
    }

    public Run withResults(List<Result> results) {
        this.results = results;
        return this;
    }

    /**
     * Information that describes a run's identity and role within an engineering system process.
     */
    public RunAutomationDetails getAutomationDetails() {
        return automationDetails;
    }

    /**
     * Information that describes a run's identity and role within an engineering system process.
     */
    public void setAutomationDetails(RunAutomationDetails automationDetails) {
        this.automationDetails = automationDetails;
    }

    public Run withAutomationDetails(RunAutomationDetails automationDetails) {
        this.automationDetails = automationDetails;
        return this;
    }

    /**
     * Automation details that describe the aggregate of runs to which this run belongs.
     */
    public Set<RunAutomationDetails> getRunAggregates() {
        return runAggregates;
    }

    /**
     * Automation details that describe the aggregate of runs to which this run belongs.
     */
    public void setRunAggregates(Set<RunAutomationDetails> runAggregates) {
        this.runAggregates = runAggregates;
    }

    public Run withRunAggregates(Set<RunAutomationDetails> runAggregates) {
        this.runAggregates = runAggregates;
        return this;
    }

    /**
     * The 'guid' property of a previous SARIF 'run' that comprises the baseline that was used to compute result 'baselineState' properties for the run.
     */
    public String getBaselineGuid() {
        return baselineGuid;
    }

    /**
     * The 'guid' property of a previous SARIF 'run' that comprises the baseline that was used to compute result 'baselineState' properties for the run.
     */
    public void setBaselineGuid(String baselineGuid) {
        this.baselineGuid = baselineGuid;
    }

    public Run withBaselineGuid(String baselineGuid) {
        this.baselineGuid = baselineGuid;
        return this;
    }

    /**
     * An array of strings used to replace sensitive information in a redaction-aware property.
     */
    public Set<String> getRedactionTokens() {
        return redactionTokens;
    }

    /**
     * An array of strings used to replace sensitive information in a redaction-aware property.
     */
    public void setRedactionTokens(Set<String> redactionTokens) {
        this.redactionTokens = redactionTokens;
    }

    public Run withRedactionTokens(Set<String> redactionTokens) {
        this.redactionTokens = redactionTokens;
        return this;
    }

    /**
     * Specifies the default encoding for any artifact object that refers to a text file.
     */
    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Specifies the default encoding for any artifact object that refers to a text file.
     */
    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public Run withDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
        return this;
    }

    /**
     * Specifies the default source language for any artifact object that refers to a text file that contains source code.
     */
    public String getDefaultSourceLanguage() {
        return defaultSourceLanguage;
    }

    /**
     * Specifies the default source language for any artifact object that refers to a text file that contains source code.
     */
    public void setDefaultSourceLanguage(String defaultSourceLanguage) {
        this.defaultSourceLanguage = defaultSourceLanguage;
    }

    public Run withDefaultSourceLanguage(String defaultSourceLanguage) {
        this.defaultSourceLanguage = defaultSourceLanguage;
        return this;
    }

    /**
     * An ordered list of character sequences that were treated as line breaks when computing region information for the run.
     */
    public Set<String> getNewlineSequences() {
        return newlineSequences;
    }

    /**
     * An ordered list of character sequences that were treated as line breaks when computing region information for the run.
     */
    public void setNewlineSequences(Set<String> newlineSequences) {
        this.newlineSequences = newlineSequences;
    }

    public Run withNewlineSequences(Set<String> newlineSequences) {
        this.newlineSequences = newlineSequences;
        return this;
    }

    /**
     * Specifies the unit in which the tool measures columns.
     */
    public ColumnKind getColumnKind() {
        return columnKind;
    }

    /**
     * Specifies the unit in which the tool measures columns.
     */
    public void setColumnKind(ColumnKind columnKind) {
        this.columnKind = columnKind;
    }

    public Run withColumnKind(ColumnKind columnKind) {
        this.columnKind = columnKind;
        return this;
    }

    /**
     * References to external property files that should be inlined with the content of a root log file.
     */
    public ExternalPropertyFileReferences getExternalPropertyFileReferences() {
        return externalPropertyFileReferences;
    }

    /**
     * References to external property files that should be inlined with the content of a root log file.
     */
    public void setExternalPropertyFileReferences(ExternalPropertyFileReferences externalPropertyFileReferences) {
        this.externalPropertyFileReferences = externalPropertyFileReferences;
    }

    public Run withExternalPropertyFileReferences(ExternalPropertyFileReferences externalPropertyFileReferences) {
        this.externalPropertyFileReferences = externalPropertyFileReferences;
        return this;
    }

    /**
     * An array of threadFlowLocation objects cached at run level.
     */
    public Set<ThreadFlowLocation> getThreadFlowLocations() {
        return threadFlowLocations;
    }

    /**
     * An array of threadFlowLocation objects cached at run level.
     */
    public void setThreadFlowLocations(Set<ThreadFlowLocation> threadFlowLocations) {
        this.threadFlowLocations = threadFlowLocations;
    }

    public Run withThreadFlowLocations(Set<ThreadFlowLocation> threadFlowLocations) {
        this.threadFlowLocations = threadFlowLocations;
        return this;
    }

    /**
     * An array of toolComponent objects relevant to a taxonomy in which results are categorized.
     */
    public Set<ToolComponent> getTaxonomies() {
        return taxonomies;
    }

    /**
     * An array of toolComponent objects relevant to a taxonomy in which results are categorized.
     */
    public void setTaxonomies(Set<ToolComponent> taxonomies) {
        this.taxonomies = taxonomies;
    }

    public Run withTaxonomies(Set<ToolComponent> taxonomies) {
        this.taxonomies = taxonomies;
        return this;
    }

    /**
     * Addresses associated with this run instance, if any.
     */
    public List<Address> getAddresses() {
        return addresses;
    }

    /**
     * Addresses associated with this run instance, if any.
     */
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public Run withAddresses(List<Address> addresses) {
        this.addresses = addresses;
        return this;
    }

    /**
     * The set of available translations of the localized data provided by the tool.
     */
    public Set<ToolComponent> getTranslations() {
        return translations;
    }

    /**
     * The set of available translations of the localized data provided by the tool.
     */
    public void setTranslations(Set<ToolComponent> translations) {
        this.translations = translations;
    }

    public Run withTranslations(Set<ToolComponent> translations) {
        this.translations = translations;
        return this;
    }

    /**
     * Contains configurations that may potentially override both reportingDescriptor.defaultConfiguration (the tool's default severities) and invocation.configurationOverrides (severities established at run-time from the command line).
     */
    public Set<ToolComponent> getPolicies() {
        return policies;
    }

    /**
     * Contains configurations that may potentially override both reportingDescriptor.defaultConfiguration (the tool's default severities) and invocation.configurationOverrides (severities established at run-time from the command line).
     */
    public void setPolicies(Set<ToolComponent> policies) {
        this.policies = policies;
    }

    public Run withPolicies(Set<ToolComponent> policies) {
        this.policies = policies;
        return this;
    }

    /**
     * An array of request objects cached at run level.
     */
    public Set<WebRequest> getWebRequests() {
        return webRequests;
    }

    /**
     * An array of request objects cached at run level.
     */
    public void setWebRequests(Set<WebRequest> webRequests) {
        this.webRequests = webRequests;
    }

    public Run withWebRequests(Set<WebRequest> webRequests) {
        this.webRequests = webRequests;
        return this;
    }

    /**
     * An array of response objects cached at run level.
     */
    public Set<WebResponse> getWebResponses() {
        return webResponses;
    }

    /**
     * An array of response objects cached at run level.
     */
    public void setWebResponses(Set<WebResponse> webResponses) {
        this.webResponses = webResponses;
    }

    public Run withWebResponses(Set<WebResponse> webResponses) {
        this.webResponses = webResponses;
        return this;
    }

    /**
     * Defines locations of special significance to SARIF consumers.
     */
    public SpecialLocations getSpecialLocations() {
        return specialLocations;
    }

    /**
     * Defines locations of special significance to SARIF consumers.
     */
    public void setSpecialLocations(SpecialLocations specialLocations) {
        this.specialLocations = specialLocations;
    }

    public Run withSpecialLocations(SpecialLocations specialLocations) {
        this.specialLocations = specialLocations;
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

    public Run withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Run.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("tool");
        sb.append('=');
        sb.append(((this.tool == null) ? "<null>" : this.tool));
        sb.append(',');
        sb.append("invocations");
        sb.append('=');
        sb.append(((this.invocations == null) ? "<null>" : this.invocations));
        sb.append(',');
        sb.append("conversion");
        sb.append('=');
        sb.append(((this.conversion == null) ? "<null>" : this.conversion));
        sb.append(',');
        sb.append("language");
        sb.append('=');
        sb.append(((this.language == null) ? "<null>" : this.language));
        sb.append(',');
        sb.append("versionControlProvenance");
        sb.append('=');
        sb.append(((this.versionControlProvenance == null) ? "<null>" : this.versionControlProvenance));
        sb.append(',');
        sb.append("originalUriBaseIds");
        sb.append('=');
        sb.append(((this.originalUriBaseIds == null) ? "<null>" : this.originalUriBaseIds));
        sb.append(',');
        sb.append("artifacts");
        sb.append('=');
        sb.append(((this.artifacts == null) ? "<null>" : this.artifacts));
        sb.append(',');
        sb.append("logicalLocations");
        sb.append('=');
        sb.append(((this.logicalLocations == null) ? "<null>" : this.logicalLocations));
        sb.append(',');
        sb.append("graphs");
        sb.append('=');
        sb.append(((this.graphs == null) ? "<null>" : this.graphs));
        sb.append(',');
        sb.append("results");
        sb.append('=');
        sb.append(((this.results == null) ? "<null>" : this.results));
        sb.append(',');
        sb.append("automationDetails");
        sb.append('=');
        sb.append(((this.automationDetails == null) ? "<null>" : this.automationDetails));
        sb.append(',');
        sb.append("runAggregates");
        sb.append('=');
        sb.append(((this.runAggregates == null) ? "<null>" : this.runAggregates));
        sb.append(',');
        sb.append("baselineGuid");
        sb.append('=');
        sb.append(((this.baselineGuid == null) ? "<null>" : this.baselineGuid));
        sb.append(',');
        sb.append("redactionTokens");
        sb.append('=');
        sb.append(((this.redactionTokens == null) ? "<null>" : this.redactionTokens));
        sb.append(',');
        sb.append("defaultEncoding");
        sb.append('=');
        sb.append(((this.defaultEncoding == null) ? "<null>" : this.defaultEncoding));
        sb.append(',');
        sb.append("defaultSourceLanguage");
        sb.append('=');
        sb.append(((this.defaultSourceLanguage == null) ? "<null>" : this.defaultSourceLanguage));
        sb.append(',');
        sb.append("newlineSequences");
        sb.append('=');
        sb.append(((this.newlineSequences == null) ? "<null>" : this.newlineSequences));
        sb.append(',');
        sb.append("columnKind");
        sb.append('=');
        sb.append(((this.columnKind == null) ? "<null>" : this.columnKind));
        sb.append(',');
        sb.append("externalPropertyFileReferences");
        sb.append('=');
        sb.append(((this.externalPropertyFileReferences == null) ? "<null>" : this.externalPropertyFileReferences));
        sb.append(',');
        sb.append("threadFlowLocations");
        sb.append('=');
        sb.append(((this.threadFlowLocations == null) ? "<null>" : this.threadFlowLocations));
        sb.append(',');
        sb.append("taxonomies");
        sb.append('=');
        sb.append(((this.taxonomies == null) ? "<null>" : this.taxonomies));
        sb.append(',');
        sb.append("addresses");
        sb.append('=');
        sb.append(((this.addresses == null) ? "<null>" : this.addresses));
        sb.append(',');
        sb.append("translations");
        sb.append('=');
        sb.append(((this.translations == null) ? "<null>" : this.translations));
        sb.append(',');
        sb.append("policies");
        sb.append('=');
        sb.append(((this.policies == null) ? "<null>" : this.policies));
        sb.append(',');
        sb.append("webRequests");
        sb.append('=');
        sb.append(((this.webRequests == null) ? "<null>" : this.webRequests));
        sb.append(',');
        sb.append("webResponses");
        sb.append('=');
        sb.append(((this.webResponses == null) ? "<null>" : this.webResponses));
        sb.append(',');
        sb.append("specialLocations");
        sb.append('=');
        sb.append(((this.specialLocations == null) ? "<null>" : this.specialLocations));
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
        return Objects.hash(
                addresses, logicalLocations, policies, language, invocations, graphs, baselineGuid, translations,
                newlineSequences, webResponses, externalPropertyFileReferences, defaultSourceLanguage, webRequests,
                results, automationDetails, conversion, artifacts, originalUriBaseIds, specialLocations,
                defaultEncoding, tool, versionControlProvenance, runAggregates, redactionTokens, taxonomies,
                columnKind, threadFlowLocations, properties
        );
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Run)) {
            return false;
        }
        Run rhs = ((Run) other);
        //noinspection ConstantValue,EqualsReplaceableByObjectsCall,StringEquality,NumberEquality
        return (((((((((((((((((((((((((((((this.addresses == rhs.addresses) || ((this.addresses != null) && this.addresses.equals(rhs.addresses))) && ((this.logicalLocations == rhs.logicalLocations) || ((this.logicalLocations != null) && this.logicalLocations.equals(rhs.logicalLocations)))) && ((this.policies == rhs.policies) || ((this.policies != null) && this.policies.equals(rhs.policies)))) && ((this.language == rhs.language) || ((this.language != null) && this.language.equals(rhs.language)))) && ((this.invocations == rhs.invocations) || ((this.invocations != null) && this.invocations.equals(rhs.invocations)))) && ((this.graphs == rhs.graphs) || ((this.graphs != null) && this.graphs.equals(rhs.graphs)))) && ((this.baselineGuid == rhs.baselineGuid) || ((this.baselineGuid != null) && this.baselineGuid.equals(rhs.baselineGuid)))) && ((this.translations == rhs.translations) || ((this.translations != null) && this.translations.equals(rhs.translations)))) && ((this.newlineSequences == rhs.newlineSequences) || ((this.newlineSequences != null) && this.newlineSequences.equals(rhs.newlineSequences)))) && ((this.webResponses == rhs.webResponses) || ((this.webResponses != null) && this.webResponses.equals(rhs.webResponses)))) && ((this.externalPropertyFileReferences == rhs.externalPropertyFileReferences) || ((this.externalPropertyFileReferences != null) && this.externalPropertyFileReferences.equals(rhs.externalPropertyFileReferences)))) && ((this.defaultSourceLanguage == rhs.defaultSourceLanguage) || ((this.defaultSourceLanguage != null) && this.defaultSourceLanguage.equals(rhs.defaultSourceLanguage)))) && ((this.webRequests == rhs.webRequests) || ((this.webRequests != null) && this.webRequests.equals(rhs.webRequests)))) && ((this.results == rhs.results) || ((this.results != null) && this.results.equals(rhs.results)))) && ((this.automationDetails == rhs.automationDetails) || ((this.automationDetails != null) && this.automationDetails.equals(rhs.automationDetails)))) && ((this.conversion == rhs.conversion) || ((this.conversion != null) && this.conversion.equals(rhs.conversion)))) && ((this.artifacts == rhs.artifacts) || ((this.artifacts != null) && this.artifacts.equals(rhs.artifacts)))) && ((this.originalUriBaseIds == rhs.originalUriBaseIds) || ((this.originalUriBaseIds != null) && this.originalUriBaseIds.equals(rhs.originalUriBaseIds)))) && ((this.specialLocations == rhs.specialLocations) || ((this.specialLocations != null) && this.specialLocations.equals(rhs.specialLocations)))) && ((this.defaultEncoding == rhs.defaultEncoding) || ((this.defaultEncoding != null) && this.defaultEncoding.equals(rhs.defaultEncoding)))) && ((this.tool == rhs.tool) || ((this.tool != null) && this.tool.equals(rhs.tool)))) && ((this.versionControlProvenance == rhs.versionControlProvenance) || ((this.versionControlProvenance != null) && this.versionControlProvenance.equals(rhs.versionControlProvenance)))) && ((this.runAggregates == rhs.runAggregates) || ((this.runAggregates != null) && this.runAggregates.equals(rhs.runAggregates)))) && ((this.redactionTokens == rhs.redactionTokens) || ((this.redactionTokens != null) && this.redactionTokens.equals(rhs.redactionTokens)))) && ((this.taxonomies == rhs.taxonomies) || ((this.taxonomies != null) && this.taxonomies.equals(rhs.taxonomies)))) && ((this.columnKind == rhs.columnKind) || ((this.columnKind != null) && this.columnKind.equals(rhs.columnKind)))) && ((this.threadFlowLocations == rhs.threadFlowLocations) || ((this.threadFlowLocations != null) && this.threadFlowLocations.equals(rhs.threadFlowLocations)))) && ((this.properties == rhs.properties) || ((this.properties != null) && this.properties.equals(rhs.properties))));
    }


    /**
     * Specifies the unit in which the tool measures columns.
     */

    public enum ColumnKind {

        @SerializedName("utf16CodeUnits")
        UTF_16_CODE_UNITS("utf16CodeUnits"),
        @SerializedName("unicodeCodePoints")
        UNICODE_CODE_POINTS("unicodeCodePoints");
        private final static Map<String, ColumnKind> CONSTANTS = new HashMap<>();

        static {
            for (ColumnKind c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private final String value;

        ColumnKind(String value) {
            this.value = value;
        }

        public static ColumnKind fromValue(String value) {
            ColumnKind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

    }

}
