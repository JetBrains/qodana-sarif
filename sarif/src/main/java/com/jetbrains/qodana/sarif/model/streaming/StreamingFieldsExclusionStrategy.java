package com.jetbrains.qodana.sarif.model.streaming;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;

public class StreamingFieldsExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        Class<?> declaringClass = f.getDeclaringClass();
        String name = f.getName();
        return declaringClass == Run.class && name.equals("results");
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
