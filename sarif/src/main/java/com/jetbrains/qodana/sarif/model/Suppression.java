package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;


/**
 * A suppression that is relevant to a result.
 */
public class Suppression {

    /**
     * A stable, unique identifer for the suprression in the form of a GUID.
     */
    @SerializedName("guid")
    @Expose
    private String guid;
    /**
     * A string that indicates where the suppression is persisted.
     * (Required)
     */
    @SerializedName("kind")
    @Expose
    private Suppression.Kind kind;
    /**
     * A string that indicates the review status of the suppression.
     */
    @SerializedName("status")
    @Expose
    private Suppression.Status status;
    /**
     * A string representing the justification for the suppression.
     */
    @SerializedName("justification")
    @Expose
    private String justification;
    /**
     * A location within a programming artifact.
     */
    @SerializedName("location")
    @Expose
    private Location location;
    /**
     * Key/value pairs that provide additional information about the object.
     */
    @SerializedName("properties")
    @Expose
    private PropertyBag properties;

    /**
     * No args constructor for use in serialization
     */
    public Suppression() {
    }

    /**
     * @param kind A string that indicates where the suppression is persisted.
     */
    public Suppression(Kind kind) {
        super();
        this.kind = kind;
    }

    /**
     * A stable, unique identifer for the suprression in the form of a GUID.
     */
    public String getGuid() {
        return guid;
    }

    /**
     * A stable, unique identifer for the suprression in the form of a GUID.
     */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Suppression withGuid(String guid) {
        this.guid = guid;
        return this;
    }

    /**
     * A string that indicates where the suppression is persisted.
     * (Required)
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * A string that indicates where the suppression is persisted.
     * (Required)
     */
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Suppression withKind(Kind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * A string that indicates the review status of the suppression.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * A string that indicates the review status of the suppression.
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    public Suppression withStatus(Status status) {
        this.status = status;
        return this;
    }

    /**
     * A string representing the justification for the suppression.
     */
    public String getJustification() {
        return justification;
    }

    /**
     * A string representing the justification for the suppression.
     */
    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Suppression withJustification(String justification) {
        this.justification = justification;
        return this;
    }

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

    public Suppression withLocation(Location location) {
        this.location = location;
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

    public Suppression withProperties(PropertyBag properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Suppression.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("guid");
        sb.append('=');
        sb.append(((this.guid == null) ? "<null>" : this.guid));
        sb.append(',');
        sb.append("kind");
        sb.append('=');
        sb.append(((this.kind == null) ? "<null>" : this.kind));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null) ? "<null>" : this.status));
        sb.append(',');
        sb.append("justification");
        sb.append('=');
        sb.append(((this.justification == null) ? "<null>" : this.justification));
        sb.append(',');
        sb.append("location");
        sb.append('=');
        sb.append(((this.location == null) ? "<null>" : this.location));
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
        result = ((result * 31) + ((this.kind == null) ? 0 : this.kind.hashCode()));
        result = ((result * 31) + ((this.guid == null) ? 0 : this.guid.hashCode()));
        result = ((result * 31) + ((this.location == null) ? 0 : this.location.hashCode()));
        result = ((result * 31) + ((this.justification == null) ? 0 : this.justification.hashCode()));
        result = ((result * 31) + ((this.properties == null) ? 0 : this.properties.hashCode()));
        result = ((result * 31) + ((this.status == null) ? 0 : this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Suppression) == false) {
            return false;
        }
        Suppression rhs = ((Suppression) other);
        return (((((((this.kind == rhs.kind) || ((this.kind != null) && this.kind.equals(rhs.kind))) && ((this.guid == rhs.guid) || ((this.guid != null) && this.guid.equals(rhs.guid)))) && ((this.location == rhs.location) || ((this.location != null) && this.location.equals(rhs.location)))) && ((this.justification == rhs.justification) || ((this.justification != null) && this.justification.equals(rhs.justification)))) && ((this.properties == rhs.properties) || ((this.properties != null) && this.properties.equals(rhs.properties)))) && ((this.status == rhs.status) || ((this.status != null) && this.status.equals(rhs.status))));
    }


    /**
     * A string that indicates where the suppression is persisted.
     */

    public enum Kind {

        @SerializedName("inSource")
        IN_SOURCE("inSource"),
        @SerializedName("external")
        EXTERNAL("external");
        private final static Map<String, Kind> CONSTANTS = new HashMap<String, Kind>();

        static {
            for (Kind c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private final String value;

        private Kind(String value) {
            this.value = value;
        }

        public static Kind fromValue(String value) {
            Kind constant = CONSTANTS.get(value);
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


    /**
     * A string that indicates the review status of the suppression.
     */

    public enum Status {

        @SerializedName("accepted")
        ACCEPTED("accepted"),
        @SerializedName("underReview")
        UNDER_REVIEW("underReview"),
        @SerializedName("rejected")
        REJECTED("rejected");
        private final static Map<String, Status> CONSTANTS = new HashMap<String, Status>();

        static {
            for (Status c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private final String value;

        private Status(String value) {
            this.value = value;
        }

        public static Status fromValue(String value) {
            Status constant = CONSTANTS.get(value);
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
