package com.jetbrains.qodana.sarif.model;

import java.util.*;


/**
 * The artifact location specified by each uriBaseId symbol on the machine where the tool originally ran.
 * Each property name designates a URI base id, and each property value is an artifactLocation object
 * that specifies the absolute URI of that URI base id on the machine where the SARIF producer ran.
 */
public class OriginalUriBaseIds implements Map<String, ArtifactLocation> {

    private final Map<String, ArtifactLocation> additionalProperties = new HashMap<>();

    @Override
    public int size() {
        return additionalProperties.size();
    }

    @Override
    public boolean isEmpty() {
        return additionalProperties.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return additionalProperties.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return additionalProperties.containsValue(value);
    }

    @Override
    public ArtifactLocation get(Object key) {
        return additionalProperties.get(key);
    }

    @Override
    public ArtifactLocation put(String key, ArtifactLocation value) {
        return additionalProperties.put(key, value);
    }

    @Override
    public ArtifactLocation remove(Object key) {
        return additionalProperties.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends ArtifactLocation> m) {
        additionalProperties.putAll(m);
    }

    @Override
    public void clear() {
        additionalProperties.clear();
    }

    @Override
    public Set<String> keySet() {
        return additionalProperties.keySet();
    }

    @Override
    public Collection<ArtifactLocation> values() {
        return additionalProperties.values();
    }

    @Override
    public Set<Entry<String, ArtifactLocation>> entrySet() {
        return additionalProperties.entrySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(OriginalUriBaseIds.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        for (Map.Entry<String, ArtifactLocation> entry : additionalProperties.entrySet()) {
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(((entry.getValue() == null) ? "<null>" : entry.getValue()));
            sb.append(',');
        }
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return additionalProperties.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof OriginalUriBaseIds)) {
            return false;
        }
        OriginalUriBaseIds rhs = ((OriginalUriBaseIds) other);
        return additionalProperties.equals(rhs.additionalProperties);
    }

}
