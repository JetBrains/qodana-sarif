package com.jetbrains.qodana.sarif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jetbrains.qodana.sarif.model.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static SarifReport emptyReport(String toolName) {
        Run run = new Run(new Tool(new ToolComponent(toolName))).withResults(new ArrayList<>());
        return new SarifReport().withRuns(List.of(run));
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
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(PropertyBag.class, new PropertyBag.PropertyBagTypeAdapter().nullSafe())
                .create();
    }
}
