package com.jetbrains.qodana.sarif.model;


/**
 * The response headers.
 */
public class Headers__1 {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Headers__1.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if (!(other instanceof Headers__1)) {
            return false;
        }
        Headers__1 rhs = ((Headers__1) other);
        return true;
    }

}
