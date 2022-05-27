package com.jetbrains.qodana.sarif.model.streaming;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.jetbrains.qodana.sarif.SarifUtil;
import com.jetbrains.qodana.sarif.model.Result;

import java.io.IOException;
import java.io.Reader;

public class IndexedResultIterator extends GeneratingIterator<IndexedResult> {
    private Gson gson = null;
    private final JsonReader resultsReader; // expected to start in the beginning of SarifReport file
    private int currentIndex; // index of current run in runs
    private ResultIterator resultIterator; // iterator over results in run at currentIndex

    public IndexedResultIterator(Reader reader) {
        resultsReader = new JsonReader(reader);
    }

    private Gson getGson() {
        if (gson == null) {
            gson = SarifUtil.createGson();
        }
        return gson;
    }

    @Override
    protected IndexedResult makeInitial() {
        try {
            if (!StreamingUtil.find(resultsReader, "runs")) {
                return null;
            }
            resultsReader.beginArray();
            if (resultsReader.peek() != JsonToken.BEGIN_OBJECT) {
                return null;
            }
            currentIndex = -1;
            resultIterator = tryMakeNextResultIterator();
            return tryReadIndexedResult();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected IndexedResult makeNext(IndexedResult prev) {
        return tryReadIndexedResult();
    }

    private IndexedResult tryReadIndexedResult() {
        if (resultIterator == null) {
            return null;
        }
        if (!resultIterator.hasNext()) {
            skipRestOfRun(resultsReader);
            resultIterator = tryMakeNextResultIterator();
            return tryReadIndexedResult();
        } else {
            return new IndexedResult(currentIndex, resultIterator.next());
        }
    }

    // jsonReader is expected to start at the end of the results array
    private void skipRestOfRun(JsonReader resultsReader) {
        try {
            resultsReader.endArray();
            StreamingUtil.skipToTheEndOfObject(resultsReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // jsonReader is expected to start before run object in runs, or before end of runs array
    private ResultIterator tryMakeNextResultIterator() {
        try {
            currentIndex++;
            if (resultsReader.peek() == JsonToken.END_ARRAY || !StreamingUtil.find(resultsReader, "results")) {
                return null;
            }
            return new ResultIterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // jsonReader is expected to start before results array
    private class ResultIterator extends GeneratingIterator<Result> {
        @Override
        protected Result makeInitial() {
            try {
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
}
