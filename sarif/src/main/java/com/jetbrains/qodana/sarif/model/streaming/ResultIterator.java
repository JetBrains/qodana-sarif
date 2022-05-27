package com.jetbrains.qodana.sarif.model.streaming;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.jetbrains.qodana.sarif.SarifUtil;
import com.jetbrains.qodana.sarif.model.Result;

import java.io.IOException;
import java.io.Reader;

public class ResultIterator extends GeneratingIterator<Result> {
    private Gson gson = null;
    private final JsonReader resultsReader; // expected to start in the beginning of SarifReport file
    private final int runIndexInReport;

    public ResultIterator(Reader reader, int runIndexInReport) {
        resultsReader = new JsonReader(reader);
        this.runIndexInReport = runIndexInReport;
    }

    private Gson getGson() {
        if (gson == null) {
            gson = SarifUtil.createGson();
        }
        return gson;
    }

    @Override
    protected Result makeInitial() {
        try {
            if (!StreamingUtil.find(resultsReader, "runs")) {
                return null;
            }
            resultsReader.beginArray();
            StreamingUtil.skipObjects(resultsReader, runIndexInReport);
            if (!StreamingUtil.find(resultsReader, "results")) {
                return null;
            }
            resultsReader.beginArray();
            return tryReadResult(resultsReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Result makeNext(Result prev) {
        return tryReadResult(resultsReader);
    }

    private Result tryReadResult(JsonReader reader) {
        try {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                return null;
            }
            return getGson().fromJson(reader, Result.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
