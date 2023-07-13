package com.jetbrains.qodana.sarif.model.streaming;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.jetbrains.qodana.sarif.model.PropertyBag;
import com.jetbrains.qodana.sarif.model.Run;

public class StreamingFieldsExclusionStrategy implements ExclusionStrategy {
    private final Class<?> fieldDeclaringClass;
    private final String fieldName;

    public StreamingFieldsExclusionStrategy() {
        this.fieldDeclaringClass = Run.class;
        this.fieldName = "results";
    }

    public StreamingFieldsExclusionStrategy(Class<?> fieldDeclaringClass, String fieldName) {
        this.fieldDeclaringClass = fieldDeclaringClass;
        this.fieldName = fieldName;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        Class<?> declaringClass = f.getDeclaringClass();
        String name = f.getName();
        return declaringClass == fieldDeclaringClass && name.equals(fieldName);
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

    public static StreamingFieldsExclusionStrategy results() {
        return new StreamingFieldsExclusionStrategy(Run.class, "results");
    }

    public static StreamingFieldsExclusionStrategy property(String propertyName) {
        return new StreamingFieldsExclusionStrategy(PropertyBag.class, propertyName);
    }
}
