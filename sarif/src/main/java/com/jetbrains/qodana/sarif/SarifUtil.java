package com.jetbrains.qodana.sarif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jetbrains.qodana.sarif.model.streaming.IndexedResult;
import com.jetbrains.qodana.sarif.model.streaming.IndexedResultIterator;
import com.jetbrains.qodana.sarif.model.streaming.ResultIterator;
import com.jetbrains.qodana.sarif.model.streaming.StreamJsonRunsListTypeAdapter;
import com.jetbrains.qodana.sarif.model.streaming.StreamingFieldsExclusionStrategy;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.qodana.sarif.model.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SarifUtil {
    private SarifUtil() {
    }

    public static SarifReport readReport(Reader reader) {
        return readReport(reader, true);
    }

    public static SarifReport readReport(Reader reader, boolean readResults) {
        GsonBuilder gsonBuilder = createGsonBuilder();
        if (!readResults) {
            gsonBuilder.addDeserializationExclusionStrategy(new StreamingFieldsExclusionStrategy());
            gsonBuilder.registerTypeAdapterFactory(StreamJsonRunsListTypeAdapter.makeFactory());
        }
        return gsonBuilder.create().fromJson(reader, SarifReport.class);
    }

    public static SarifReport readReport(Path path) throws IOException {
        return readReport(path, true);
    }

    public static SarifReport readReport(Path path, boolean readResults) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return readReport(reader, readResults);
        }
    }

    public static List<Result> readResultsFromObject(Object o) {
        Gson gson = createGson();
        String json = gson.toJson(o);
        return gson.fromJson(json, new TypeToken<List<Result>>(){}.getType());
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
        return createGsonBuilder().create();
    }

    public static GsonBuilder createGsonBuilder() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(PropertyBag.class, new PropertyBag.PropertyBagTypeAdapter().nullSafe());
    }

    /**
     *
     * @param reader reader in the begging of SarifReport object
     * @param runIndexInReport run's index in runs array of sarif report
     * @return iterator over results, that lazily reads results from reader
     */
    public static Iterator<Result> lazyReadResults(Reader reader, int runIndexInReport) {
        return new ResultIterator(reader, runIndexInReport);
    }

    /**
     *
     * @param reader reader in the begging of SarifReport object
     * @return iterator over pairs of run's index (from runs array of sarif report) and results in that run, that
     * lazily reads pairs from reader
     */
    public static Iterator<IndexedResult> lazyReadIndexedResults(Reader reader) {
        return new IndexedResultIterator(reader);
    }
}
