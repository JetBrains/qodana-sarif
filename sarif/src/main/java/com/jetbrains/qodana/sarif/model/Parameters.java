package com.jetbrains.qodana.sarif.model;


/**
 * The request parameters.
 */
public class Parameters {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Parameters.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if (!(other instanceof Parameters)) {
            return false;
        }
        Parameters rhs = ((Parameters) other);
        return true;
    }

}
