package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;


/**
 * Describes an HTTP request.
 */
public class WebRequest {

    /**
     * The index within the run.webRequests array of the request object associated with this result.
     */
    @SerializedName("index")
    @Expose
    private Integer index = null;
    /**
     * The request protocol. Example: 'http'.
     */
    @SerializedName("protocol")
    @Expose
    private String protocol;
    /**
     * The request version. Example: '1.1'.
     */
    @SerializedName("version")
    @Expose
    private String version;
    /**
     * The target of the request.
     */
    @SerializedName("target")
    @Expose
    private String target;
    /**
     * The HTTP method. Well-known values are 'GET', 'PUT', 'POST', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'TRACE', 'CONNECT'.
     */
    @SerializedName("method")
    @Expose
    private String method;
    /**
     * The request headers.
     */
    @SerializedName("headers")
    @Expose
    private Headers headers;
    /**
     * The request parameters.
     */
    @SerializedName("parameters")
    @Expose
    private Parameters parameters;
    /**
     * Represents the contents of an artifact.
     */
    @SerializedName("body")
    @Expose
    private ArtifactContent body;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * The index within the run.webRequests array of the request object associated with this result.
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * The index within the run.webRequests array of the request object associated with this result.
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    public WebRequest withIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * The request protocol. Example: 'http'.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * The request protocol. Example: 'http'.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public WebRequest withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * The request version. Example: '1.1'.
     */
    public String getVersion() {
        return version;
    }

    /**
     * The request version. Example: '1.1'.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public WebRequest withVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * The target of the request.
     */
    public String getTarget() {
        return target;
    }

    /**
     * The target of the request.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    public WebRequest withTarget(String target) {
        this.target = target;
        return this;
    }

    /**
     * The HTTP method. Well-known values are 'GET', 'PUT', 'POST', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'TRACE', 'CONNECT'.
     */
    public String getMethod() {
        return method;
    }

    /**
     * The HTTP method. Well-known values are 'GET', 'PUT', 'POST', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS', 'TRACE', 'CONNECT'.
     */
    public void setMethod(String method) {
        this.method = method;
    }

    public WebRequest withMethod(String method) {
        this.method = method;
        return this;
    }

    /**
     * The request headers.
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * The request headers.
     */
    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public WebRequest withHeaders(Headers headers) {
        this.headers = headers;
        return this;
    }

    /**
     * The request parameters.
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * The request parameters.
     */
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public WebRequest withParameters(Parameters parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Represents the contents of an artifact.
     */
    public ArtifactContent getBody() {
        return body;
    }

    /**
     * Represents the contents of an artifact.
     */
    public void setBody(ArtifactContent body) {
        this.body = body;
    }

    public WebRequest withBody(ArtifactContent body) {
        this.body = body;
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

    public WebRequest withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(WebRequest.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("index");
        sb.append('=');
        sb.append(((this.index == null) ? "<null>" : this.index));
        sb.append(',');
        sb.append("protocol");
        sb.append('=');
        sb.append(((this.protocol == null) ? "<null>" : this.protocol));
        sb.append(',');
        sb.append("version");
        sb.append('=');
        sb.append(((this.version == null) ? "<null>" : this.version));
        sb.append(',');
        sb.append("target");
        sb.append('=');
        sb.append(((this.target == null) ? "<null>" : this.target));
        sb.append(',');
        sb.append("method");
        sb.append('=');
        sb.append(((this.method == null) ? "<null>" : this.method));
        sb.append(',');
        sb.append("headers");
        sb.append('=');
        sb.append(((this.headers == null) ? "<null>" : this.headers));
        sb.append(',');
        sb.append("parameters");
        sb.append('=');
        sb.append(((this.parameters == null) ? "<null>" : this.parameters));
        sb.append(',');
        sb.append("body");
        sb.append('=');
        sb.append(((this.body == null) ? "<null>" : this.body));
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
        result = ((result * 31) + ((this.headers == null) ? 0 : this.headers.hashCode()));
        result = ((result * 31) + ((this.protocol == null) ? 0 : this.protocol.hashCode()));
        result = ((result * 31) + ((this.method == null) ? 0 : this.method.hashCode()));
        result = ((result * 31) + ((this.index == null) ? 0 : this.index.hashCode()));
        result = ((result * 31) + ((this.body == null) ? 0 : this.body.hashCode()));
        result = ((result * 31) + ((this.version == null) ? 0 : this.version.hashCode()));
        result = ((result * 31) + ((this.parameters == null) ? 0 : this.parameters.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        result = ((result * 31) + ((this.target == null) ? 0 : this.target.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof WebRequest)) {
            return false;
        }
        WebRequest rhs = ((WebRequest) other);
        return (((((((((Objects.equals(this.headers, rhs.headers)) && (Objects.equals(this.protocol, rhs.protocol))) && (Objects.equals(this.method, rhs.method))) && (Objects.equals(this.index, rhs.index))) && (Objects.equals(this.body, rhs.body))) && (Objects.equals(this.version, rhs.version))) && (Objects.equals(this.parameters, rhs.parameters))) && (Objects.equals(this.properties, rhs.properties))) && (Objects.equals(this.target, rhs.target)));
    }

}
