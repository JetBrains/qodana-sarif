package com.jetbrains.qodana.sarif.model;


/**
 * The values of relevant expressions after the edge has been traversed.
 */
public class FinalState {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FinalState.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if ((other instanceof FinalState) == false) {
            return false;
        }
        FinalState rhs = ((FinalState) other);
        return true;
    }

}
