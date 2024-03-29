package com.jetbrains.qodana.sarif.model;


/**
 * Values of relevant expressions at the start of the thread flow that remain constant.
 */
public class ImmutableState {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ImmutableState.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if (!(other instanceof ImmutableState)) {
            return false;
        }
        ImmutableState rhs = ((ImmutableState) other);
        return true;
    }

}
