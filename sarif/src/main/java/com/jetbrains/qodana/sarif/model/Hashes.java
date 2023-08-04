package com.jetbrains.qodana.sarif.model;


/**
 * A dictionary, each of whose keys is the name of a hash function and each of whose values is the hashed value of the artifact produced by the specified hash function.
 */
public class Hashes {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Hashes.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if (!(other instanceof Hashes)) {
            return false;
        }
        Hashes rhs = ((Hashes) other);
        return true;
    }

}
