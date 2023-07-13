package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * The runtime environment of the analysis tool run.
 */
public class Invocation {

    /**
     * The command line used to invoke the tool.
     */
    @SerializedName("commandLine")
    @Expose
    private String commandLine;
    /**
     * An array of strings, containing in order the command line arguments passed to the tool from the operating system.
     */
    @SerializedName("arguments")
    @Expose
    private List<String> arguments = null;
    /**
     * The locations of any response files specified on the tool's command line.
     */
    @SerializedName("responseFiles")
    @Expose
    private Set<ArtifactLocation> responseFiles = null;
    /**
     * The Coordinated Universal Time (UTC) date and time at which the invocation started. See "Date/time properties" in the SARIF spec for the required format.
     */
    @SerializedName("startTimeUtc")
    @Expose
    private Date startTimeUtc;
    /**
     * The Coordinated Universal Time (UTC) date and time at which the invocation ended. See "Date/time properties" in the SARIF spec for the required format.
     */
    @SerializedName("endTimeUtc")
    @Expose
    private Date endTimeUtc;
    /**
     * The process exit code.
     */
    @SerializedName("exitCode")
    @Expose
    private Integer exitCode;
    /**
     * An array of configurationOverride objects that describe rules related runtime overrides.
     */
    @SerializedName("ruleConfigurationOverrides")
    @Expose
    private Set<ConfigurationOverride> ruleConfigurationOverrides = null;
    /**
     * An array of configurationOverride objects that describe notifications related runtime overrides.
     */
    @SerializedName("notificationConfigurationOverrides")
    @Expose
    private Set<ConfigurationOverride> notificationConfigurationOverrides = null;
    /**
     * A list of runtime conditions detected by the tool during the analysis.
     */
    @SerializedName("toolExecutionNotifications")
    @Expose
    private List<Notification> toolExecutionNotifications = null;
    /**
     * A list of conditions detected by the tool that are relevant to the tool's configuration.
     */
    @SerializedName("toolConfigurationNotifications")
    @Expose
    private List<Notification> toolConfigurationNotifications = null;
    /**
     * The reason for the process exit.
     */
    @SerializedName("exitCodeDescription")
    @Expose
    private String exitCodeDescription;
    /**
     * The name of the signal that caused the process to exit.
     */
    @SerializedName("exitSignalName")
    @Expose
    private String exitSignalName;
    /**
     * The numeric value of the signal that caused the process to exit.
     */
    @SerializedName("exitSignalNumber")
    @Expose
    private Integer exitSignalNumber;
    /**
     * The reason given by the operating system that the process failed to start.
     */
    @SerializedName("processStartFailureMessage")
    @Expose
    private String processStartFailureMessage;
    /**
     * Specifies whether the tool's execution completed successfully.
     * (Required)
     */
    @SerializedName("executionSuccessful")
    @Expose
    private Boolean executionSuccessful;
    /**
     * The machine on which the invocation occurred.
     */
    @SerializedName("machine")
    @Expose
    private String machine;
    /**
     * The account under which the invocation occurred.
     */
    @SerializedName("account")
    @Expose
    private String account;
    /**
     * The id of the process in which the invocation occurred.
     */
    @SerializedName("processId")
    @Expose
    private Integer processId;
    /**
     * Specifies the location of an artifact.
     */
    @SerializedName("executableLocation")
    @Expose
    private ArtifactLocation executableLocation;
    /**
     * Specifies the location of an artifact.
     */
    @SerializedName("workingDirectory")
    @Expose
    private ArtifactLocation workingDirectory;
    /**
     * The environment variables associated with the analysis tool process, expressed as key/value pairs.
     */
    @SerializedName("environmentVariables")
    @Expose
    private EnvironmentVariables environmentVariables;
    /**
     * Specifies the location of an artifact.
     */
    @SerializedName("stdin")
    @Expose
    private ArtifactLocation stdin;
    /**
     * Specifies the location of an artifact.
     */
    @SerializedName("stdout")
    @Expose
    private ArtifactLocation stdout;
    /**
     * Specifies the location of an artifact.
     */
    @SerializedName("stderr")
    @Expose
    private ArtifactLocation stderr;
    /**
     * Specifies the location of an artifact.
     */
    @SerializedName("stdoutStderr")
    @Expose
    private ArtifactLocation stdoutStderr;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * No args constructor for use in serialization
     */
    public Invocation() {
    }

    /**
     * @param executionSuccessful Specifies whether the tool's execution completed successfully.
     */
    public Invocation(Boolean executionSuccessful) {
        super();
        this.executionSuccessful = executionSuccessful;
    }

    /**
     * The command line used to invoke the tool.
     */
    public String getCommandLine() {
        return commandLine;
    }

    /**
     * The command line used to invoke the tool.
     */
    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public Invocation withCommandLine(String commandLine) {
        this.commandLine = commandLine;
        return this;
    }

    /**
     * An array of strings, containing in order the command line arguments passed to the tool from the operating system.
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * An array of strings, containing in order the command line arguments passed to the tool from the operating system.
     */
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public Invocation withArguments(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }

    /**
     * The locations of any response files specified on the tool's command line.
     */
    public Set<ArtifactLocation> getResponseFiles() {
        return responseFiles;
    }

    /**
     * The locations of any response files specified on the tool's command line.
     */
    public void setResponseFiles(Set<ArtifactLocation> responseFiles) {
        this.responseFiles = responseFiles;
    }

    public Invocation withResponseFiles(Set<ArtifactLocation> responseFiles) {
        this.responseFiles = responseFiles;
        return this;
    }

    /**
     * The Coordinated Universal Time (UTC) date and time at which the invocation started. See "Date/time properties" in the SARIF spec for the required format.
     */
    public Date getStartTimeUtc() {
        return startTimeUtc;
    }

    /**
     * The Coordinated Universal Time (UTC) date and time at which the invocation started. See "Date/time properties" in the SARIF spec for the required format.
     */
    public void setStartTimeUtc(Date startTimeUtc) {
        this.startTimeUtc = startTimeUtc;
    }

    public Invocation withStartTimeUtc(Date startTimeUtc) {
        this.startTimeUtc = startTimeUtc;
        return this;
    }

    /**
     * The Coordinated Universal Time (UTC) date and time at which the invocation ended. See "Date/time properties" in the SARIF spec for the required format.
     */
    public Date getEndTimeUtc() {
        return endTimeUtc;
    }

    /**
     * The Coordinated Universal Time (UTC) date and time at which the invocation ended. See "Date/time properties" in the SARIF spec for the required format.
     */
    public void setEndTimeUtc(Date endTimeUtc) {
        this.endTimeUtc = endTimeUtc;
    }

    public Invocation withEndTimeUtc(Date endTimeUtc) {
        this.endTimeUtc = endTimeUtc;
        return this;
    }

    /**
     * The process exit code.
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * The process exit code.
     */
    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Invocation withExitCode(Integer exitCode) {
        this.exitCode = exitCode;
        return this;
    }

    /**
     * An array of configurationOverride objects that describe rules related runtime overrides.
     */
    public Set<ConfigurationOverride> getRuleConfigurationOverrides() {
        return ruleConfigurationOverrides;
    }

    /**
     * An array of configurationOverride objects that describe rules related runtime overrides.
     */
    public void setRuleConfigurationOverrides(Set<ConfigurationOverride> ruleConfigurationOverrides) {
        this.ruleConfigurationOverrides = ruleConfigurationOverrides;
    }

    public Invocation withRuleConfigurationOverrides(Set<ConfigurationOverride> ruleConfigurationOverrides) {
        this.ruleConfigurationOverrides = ruleConfigurationOverrides;
        return this;
    }

    /**
     * An array of configurationOverride objects that describe notifications related runtime overrides.
     */
    public Set<ConfigurationOverride> getNotificationConfigurationOverrides() {
        return notificationConfigurationOverrides;
    }

    /**
     * An array of configurationOverride objects that describe notifications related runtime overrides.
     */
    public void setNotificationConfigurationOverrides(Set<ConfigurationOverride> notificationConfigurationOverrides) {
        this.notificationConfigurationOverrides = notificationConfigurationOverrides;
    }

    public Invocation withNotificationConfigurationOverrides(Set<ConfigurationOverride> notificationConfigurationOverrides) {
        this.notificationConfigurationOverrides = notificationConfigurationOverrides;
        return this;
    }

    /**
     * A list of runtime conditions detected by the tool during the analysis.
     */
    public List<Notification> getToolExecutionNotifications() {
        return toolExecutionNotifications;
    }

    /**
     * A list of runtime conditions detected by the tool during the analysis.
     */
    public void setToolExecutionNotifications(List<Notification> toolExecutionNotifications) {
        this.toolExecutionNotifications = toolExecutionNotifications;
    }

    public Invocation withToolExecutionNotifications(List<Notification> toolExecutionNotifications) {
        this.toolExecutionNotifications = toolExecutionNotifications;
        return this;
    }

    /**
     * A list of conditions detected by the tool that are relevant to the tool's configuration.
     */
    public List<Notification> getToolConfigurationNotifications() {
        return toolConfigurationNotifications;
    }

    /**
     * A list of conditions detected by the tool that are relevant to the tool's configuration.
     */
    public void setToolConfigurationNotifications(List<Notification> toolConfigurationNotifications) {
        this.toolConfigurationNotifications = toolConfigurationNotifications;
    }

    public Invocation withToolConfigurationNotifications(List<Notification> toolConfigurationNotifications) {
        this.toolConfigurationNotifications = toolConfigurationNotifications;
        return this;
    }

    /**
     * The reason for the process exit.
     */
    public String getExitCodeDescription() {
        return exitCodeDescription;
    }

    /**
     * The reason for the process exit.
     */
    public void setExitCodeDescription(String exitCodeDescription) {
        this.exitCodeDescription = exitCodeDescription;
    }

    public Invocation withExitCodeDescription(String exitCodeDescription) {
        this.exitCodeDescription = exitCodeDescription;
        return this;
    }

    /**
     * The name of the signal that caused the process to exit.
     */
    public String getExitSignalName() {
        return exitSignalName;
    }

    /**
     * The name of the signal that caused the process to exit.
     */
    public void setExitSignalName(String exitSignalName) {
        this.exitSignalName = exitSignalName;
    }

    public Invocation withExitSignalName(String exitSignalName) {
        this.exitSignalName = exitSignalName;
        return this;
    }

    /**
     * The numeric value of the signal that caused the process to exit.
     */
    public Integer getExitSignalNumber() {
        return exitSignalNumber;
    }

    /**
     * The numeric value of the signal that caused the process to exit.
     */
    public void setExitSignalNumber(Integer exitSignalNumber) {
        this.exitSignalNumber = exitSignalNumber;
    }

    public Invocation withExitSignalNumber(Integer exitSignalNumber) {
        this.exitSignalNumber = exitSignalNumber;
        return this;
    }

    /**
     * The reason given by the operating system that the process failed to start.
     */
    public String getProcessStartFailureMessage() {
        return processStartFailureMessage;
    }

    /**
     * The reason given by the operating system that the process failed to start.
     */
    public void setProcessStartFailureMessage(String processStartFailureMessage) {
        this.processStartFailureMessage = processStartFailureMessage;
    }

    public Invocation withProcessStartFailureMessage(String processStartFailureMessage) {
        this.processStartFailureMessage = processStartFailureMessage;
        return this;
    }

    /**
     * Specifies whether the tool's execution completed successfully.
     * (Required)
     */
    public Boolean getExecutionSuccessful() {
        return executionSuccessful;
    }

    /**
     * Specifies whether the tool's execution completed successfully.
     * (Required)
     */
    public void setExecutionSuccessful(Boolean executionSuccessful) {
        this.executionSuccessful = executionSuccessful;
    }

    public Invocation withExecutionSuccessful(Boolean executionSuccessful) {
        this.executionSuccessful = executionSuccessful;
        return this;
    }

    /**
     * The machine on which the invocation occurred.
     */
    public String getMachine() {
        return machine;
    }

    /**
     * The machine on which the invocation occurred.
     */
    public void setMachine(String machine) {
        this.machine = machine;
    }

    public Invocation withMachine(String machine) {
        this.machine = machine;
        return this;
    }

    /**
     * The account under which the invocation occurred.
     */
    public String getAccount() {
        return account;
    }

    /**
     * The account under which the invocation occurred.
     */
    public void setAccount(String account) {
        this.account = account;
    }

    public Invocation withAccount(String account) {
        this.account = account;
        return this;
    }

    /**
     * The id of the process in which the invocation occurred.
     */
    public Integer getProcessId() {
        return processId;
    }

    /**
     * The id of the process in which the invocation occurred.
     */
    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public Invocation withProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    /**
     * Specifies the location of an artifact.
     */
    public ArtifactLocation getExecutableLocation() {
        return executableLocation;
    }

    /**
     * Specifies the location of an artifact.
     */
    public void setExecutableLocation(ArtifactLocation executableLocation) {
        this.executableLocation = executableLocation;
    }

    public Invocation withExecutableLocation(ArtifactLocation executableLocation) {
        this.executableLocation = executableLocation;
        return this;
    }

    /**
     * Specifies the location of an artifact.
     */
    public ArtifactLocation getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Specifies the location of an artifact.
     */
    public void setWorkingDirectory(ArtifactLocation workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Invocation withWorkingDirectory(ArtifactLocation workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    /**
     * The environment variables associated with the analysis tool process, expressed as key/value pairs.
     */
    public EnvironmentVariables getEnvironmentVariables() {
        return environmentVariables;
    }

    /**
     * The environment variables associated with the analysis tool process, expressed as key/value pairs.
     */
    public void setEnvironmentVariables(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Invocation withEnvironmentVariables(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        return this;
    }

    /**
     * Specifies the location of an artifact.
     */
    public ArtifactLocation getStdin() {
        return stdin;
    }

    /**
     * Specifies the location of an artifact.
     */
    public void setStdin(ArtifactLocation stdin) {
        this.stdin = stdin;
    }

    public Invocation withStdin(ArtifactLocation stdin) {
        this.stdin = stdin;
        return this;
    }

    /**
     * Specifies the location of an artifact.
     */
    public ArtifactLocation getStdout() {
        return stdout;
    }

    /**
     * Specifies the location of an artifact.
     */
    public void setStdout(ArtifactLocation stdout) {
        this.stdout = stdout;
    }

    public Invocation withStdout(ArtifactLocation stdout) {
        this.stdout = stdout;
        return this;
    }

    /**
     * Specifies the location of an artifact.
     */
    public ArtifactLocation getStderr() {
        return stderr;
    }

    /**
     * Specifies the location of an artifact.
     */
    public void setStderr(ArtifactLocation stderr) {
        this.stderr = stderr;
    }

    public Invocation withStderr(ArtifactLocation stderr) {
        this.stderr = stderr;
        return this;
    }

    /**
     * Specifies the location of an artifact.
     */
    public ArtifactLocation getStdoutStderr() {
        return stdoutStderr;
    }

    /**
     * Specifies the location of an artifact.
     */
    public void setStdoutStderr(ArtifactLocation stdoutStderr) {
        this.stdoutStderr = stdoutStderr;
    }

    public Invocation withStdoutStderr(ArtifactLocation stdoutStderr) {
        this.stdoutStderr = stdoutStderr;
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

    public Invocation withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Invocation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("commandLine");
        sb.append('=');
        sb.append(((this.commandLine == null) ? "<null>" : this.commandLine));
        sb.append(',');
        sb.append("arguments");
        sb.append('=');
        sb.append(((this.arguments == null) ? "<null>" : this.arguments));
        sb.append(',');
        sb.append("responseFiles");
        sb.append('=');
        sb.append(((this.responseFiles == null) ? "<null>" : this.responseFiles));
        sb.append(',');
        sb.append("startTimeUtc");
        sb.append('=');
        sb.append(((this.startTimeUtc == null) ? "<null>" : this.startTimeUtc));
        sb.append(',');
        sb.append("endTimeUtc");
        sb.append('=');
        sb.append(((this.endTimeUtc == null) ? "<null>" : this.endTimeUtc));
        sb.append(',');
        sb.append("exitCode");
        sb.append('=');
        sb.append(((this.exitCode == null) ? "<null>" : this.exitCode));
        sb.append(',');
        sb.append("ruleConfigurationOverrides");
        sb.append('=');
        sb.append(((this.ruleConfigurationOverrides == null) ? "<null>" : this.ruleConfigurationOverrides));
        sb.append(',');
        sb.append("notificationConfigurationOverrides");
        sb.append('=');
        sb.append(((this.notificationConfigurationOverrides == null) ? "<null>" : this.notificationConfigurationOverrides));
        sb.append(',');
        sb.append("toolExecutionNotifications");
        sb.append('=');
        sb.append(((this.toolExecutionNotifications == null) ? "<null>" : this.toolExecutionNotifications));
        sb.append(',');
        sb.append("toolConfigurationNotifications");
        sb.append('=');
        sb.append(((this.toolConfigurationNotifications == null) ? "<null>" : this.toolConfigurationNotifications));
        sb.append(',');
        sb.append("exitCodeDescription");
        sb.append('=');
        sb.append(((this.exitCodeDescription == null) ? "<null>" : this.exitCodeDescription));
        sb.append(',');
        sb.append("exitSignalName");
        sb.append('=');
        sb.append(((this.exitSignalName == null) ? "<null>" : this.exitSignalName));
        sb.append(',');
        sb.append("exitSignalNumber");
        sb.append('=');
        sb.append(((this.exitSignalNumber == null) ? "<null>" : this.exitSignalNumber));
        sb.append(',');
        sb.append("processStartFailureMessage");
        sb.append('=');
        sb.append(((this.processStartFailureMessage == null) ? "<null>" : this.processStartFailureMessage));
        sb.append(',');
        sb.append("executionSuccessful");
        sb.append('=');
        sb.append(((this.executionSuccessful == null) ? "<null>" : this.executionSuccessful));
        sb.append(',');
        sb.append("machine");
        sb.append('=');
        sb.append(((this.machine == null) ? "<null>" : this.machine));
        sb.append(',');
        sb.append("account");
        sb.append('=');
        sb.append(((this.account == null) ? "<null>" : this.account));
        sb.append(',');
        sb.append("processId");
        sb.append('=');
        sb.append(((this.processId == null) ? "<null>" : this.processId));
        sb.append(',');
        sb.append("executableLocation");
        sb.append('=');
        sb.append(((this.executableLocation == null) ? "<null>" : this.executableLocation));
        sb.append(',');
        sb.append("workingDirectory");
        sb.append('=');
        sb.append(((this.workingDirectory == null) ? "<null>" : this.workingDirectory));
        sb.append(',');
        sb.append("environmentVariables");
        sb.append('=');
        sb.append(((this.environmentVariables == null) ? "<null>" : this.environmentVariables));
        sb.append(',');
        sb.append("stdin");
        sb.append('=');
        sb.append(((this.stdin == null) ? "<null>" : this.stdin));
        sb.append(',');
        sb.append("stdout");
        sb.append('=');
        sb.append(((this.stdout == null) ? "<null>" : this.stdout));
        sb.append(',');
        sb.append("stderr");
        sb.append('=');
        sb.append(((this.stderr == null) ? "<null>" : this.stderr));
        sb.append(',');
        sb.append("stdoutStderr");
        sb.append('=');
        sb.append(((this.stdoutStderr == null) ? "<null>" : this.stdoutStderr));
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
        result = ((result * 31) + ((this.endTimeUtc == null) ? 0 : this.endTimeUtc.hashCode()));
        result = ((result * 31) + ((this.stdin == null) ? 0 : this.stdin.hashCode()));
        result = ((result * 31) + ((this.stdout == null) ? 0 : this.stdout.hashCode()));
        result = ((result * 31) + ((this.workingDirectory == null) ? 0 : this.workingDirectory.hashCode()));
        result = ((result * 31) + ((this.exitSignalNumber == null) ? 0 : this.exitSignalNumber.hashCode()));
        result = ((result * 31) + ((this.exitCodeDescription == null) ? 0 : this.exitCodeDescription.hashCode()));
        result = ((result * 31) + ((this.executableLocation == null) ? 0 : this.executableLocation.hashCode()));
        result = ((result * 31) + ((this.processId == null) ? 0 : this.processId.hashCode()));
        result = ((result * 31) + ((this.exitCode == null) ? 0 : this.exitCode.hashCode()));
        result = ((result * 31) + ((this.toolConfigurationNotifications == null) ? 0 : this.toolConfigurationNotifications.hashCode()));
        result = ((result * 31) + ((this.notificationConfigurationOverrides == null) ? 0 : this.notificationConfigurationOverrides.hashCode()));
        result = ((result * 31) + ((this.processStartFailureMessage == null) ? 0 : this.processStartFailureMessage.hashCode()));
        result = ((result * 31) + ((this.stderr == null) ? 0 : this.stderr.hashCode()));
        result = ((result * 31) + ((this.ruleConfigurationOverrides == null) ? 0 : this.ruleConfigurationOverrides.hashCode()));
        result = ((result * 31) + ((this.toolExecutionNotifications == null) ? 0 : this.toolExecutionNotifications.hashCode()));
        result = ((result * 31) + ((this.machine == null) ? 0 : this.machine.hashCode()));
        result = ((result * 31) + ((this.environmentVariables == null) ? 0 : this.environmentVariables.hashCode()));
        result = ((result * 31) + ((this.stdoutStderr == null) ? 0 : this.stdoutStderr.hashCode()));
        result = ((result * 31) + ((this.arguments == null) ? 0 : this.arguments.hashCode()));
        result = ((result * 31) + ((this.responseFiles == null) ? 0 : this.responseFiles.hashCode()));
        result = ((result * 31) + ((this.commandLine == null) ? 0 : this.commandLine.hashCode()));
        result = ((result * 31) + ((this.executionSuccessful == null) ? 0 : this.executionSuccessful.hashCode()));
        result = ((result * 31) + ((this.startTimeUtc == null) ? 0 : this.startTimeUtc.hashCode()));
        result = ((result * 31) + ((this.account == null) ? 0 : this.account.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        result = ((result * 31) + ((this.exitSignalName == null) ? 0 : this.exitSignalName.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Invocation)) {
            return false;
        }
        Invocation rhs = ((Invocation) other);
        return ((((((((((((((((((((((((((Objects.equals(this.endTimeUtc, rhs.endTimeUtc)) && (Objects.equals(this.stdin, rhs.stdin))) && (Objects.equals(this.stdout, rhs.stdout))) && (Objects.equals(this.workingDirectory, rhs.workingDirectory))) && (Objects.equals(this.exitSignalNumber, rhs.exitSignalNumber))) && (Objects.equals(this.exitCodeDescription, rhs.exitCodeDescription))) && (Objects.equals(this.executableLocation, rhs.executableLocation))) && (Objects.equals(this.processId, rhs.processId))) && (Objects.equals(this.exitCode, rhs.exitCode))) && (Objects.equals(this.toolConfigurationNotifications, rhs.toolConfigurationNotifications))) && (Objects.equals(this.notificationConfigurationOverrides, rhs.notificationConfigurationOverrides))) && (Objects.equals(this.processStartFailureMessage, rhs.processStartFailureMessage))) && (Objects.equals(this.stderr, rhs.stderr))) && (Objects.equals(this.ruleConfigurationOverrides, rhs.ruleConfigurationOverrides))) && (Objects.equals(this.toolExecutionNotifications, rhs.toolExecutionNotifications))) && (Objects.equals(this.machine, rhs.machine))) && (Objects.equals(this.environmentVariables, rhs.environmentVariables))) && (Objects.equals(this.stdoutStderr, rhs.stdoutStderr))) && (Objects.equals(this.arguments, rhs.arguments))) && (Objects.equals(this.responseFiles, rhs.responseFiles))) && (Objects.equals(this.commandLine, rhs.commandLine))) && (Objects.equals(this.executionSuccessful, rhs.executionSuccessful))) && (Objects.equals(this.startTimeUtc, rhs.startTimeUtc))) && (Objects.equals(this.account, rhs.account))) && (Objects.equals(this.properties, rhs.properties))) && (Objects.equals(this.exitSignalName, rhs.exitSignalName)));
    }

}
