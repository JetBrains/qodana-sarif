package com.jetbrains.qodana.sarif.model;


/**
 * A dictionary, each of whose keys specifies a variable or expression, the associated value of which represents the variable or expression value. For an annotation of kind 'continuation', for example, this dictionary might hold the current assumed values of a set of global variables.
 */
public class State {


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(State.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        if (!(other instanceof State)) {
            return false;
        }
        State rhs = ((State) other);
        return true;
    }

}
