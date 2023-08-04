package com.jetbrains.qodana.sarif.model;


/**
 * The request headers.
 */
public class Headers {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Headers.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Headers)) {
            return false;
        }
        Headers rhs = ((Headers) other);
        return true;
    }

}
