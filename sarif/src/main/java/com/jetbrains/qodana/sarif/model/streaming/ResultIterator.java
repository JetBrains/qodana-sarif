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
    private final ResultLocation resultLocation;

    public ResultIterator(Reader reader, int runIndexInReport) {
        this(reader, new ResultLocation.InRun(runIndexInReport));
    }

    public ResultIterator(Reader reader, ResultLocation resultLocation) {
        this.resultsReader = new JsonReader(reader);
        this.resultLocation = resultLocation;
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
            if (!findResult()) {
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

    private boolean findResult() throws IOException {
        if (resultLocation instanceof ResultLocation.InRun) {
            int runIndex = ((ResultLocation.InRun) resultLocation).getRunIndex();

            return findRun(runIndex)
                    && StreamingUtil.find(resultsReader, "results");
        } else if (resultLocation instanceof ResultLocation.InProperties) {
            int runIndex = ((ResultLocation.InProperties) resultLocation).getRunIndex();
            String propertyName = ((ResultLocation.InProperties) resultLocation).getPropertyName();

            return findRun(runIndex)
                    && StreamingUtil.find(resultsReader, "properties")
                    && StreamingUtil.find(resultsReader, propertyName);
        } else {
            throw new UnsupportedOperationException("Unhandled ResultLocation");
        }
    }

    private boolean findRun(int runIndex) throws IOException {
        if (!StreamingUtil.find(resultsReader, "runs")) {
            return false;
        }
        resultsReader.beginArray();
        StreamingUtil.skipObjects(resultsReader, runIndex);
        return true;
    }
}
