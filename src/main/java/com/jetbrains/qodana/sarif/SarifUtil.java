package com.jetbrains.qodana.sarif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jetbrains.qodana.sarif.model.PropertyBag;
import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SarifUtil {
    private SarifUtil() {
    }

    public static SarifReport readReport(Reader reader) {
        Gson gson = createGson();
        return gson.fromJson(reader, SarifReport.class);
    }
    public static SarifReport readReport(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return readReport(reader);
        }
    }

    public static void writeReport(Writer writer, SarifReport report) {
        Gson gson = createGson();
        gson.toJson(report, writer);
    }

    public static void writeReport(Path path, SarifReport report) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writeReport(writer, report);
        }
    }

    public static Gson createGson() {
        Field declaredField;
        try {
            declaredField = Run.class.getDeclaredField("results");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        Type resultsType = declaredField.getGenericType();

        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(resultsType, new IterableTypeAdapter().nullSafe())
                .registerTypeAdapter(PropertyBag.class, new PropertyBag.PropertyBagTypeAdapter().nullSafe())
                .create();
    }
}
