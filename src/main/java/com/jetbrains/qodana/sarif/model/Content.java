
package com.jetbrains.qodana.sarif.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum Content {

    @SerializedName("localizedData")
    LOCALIZED_DATA("localizedData"),
    @SerializedName("nonLocalizedData")
    NON_LOCALIZED_DATA("nonLocalizedData");
    private final String value;
    private final static Map<String, Content> CONSTANTS = new HashMap<String, Content>();

    static {
        for (Content c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    private Content(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public String value() {
        return this.value;
    }

    public static Content fromValue(String value) {
        Content constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
