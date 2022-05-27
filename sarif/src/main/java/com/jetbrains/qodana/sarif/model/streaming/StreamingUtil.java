package com.jetbrains.qodana.sarif.model.streaming;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;

public class StreamingUtil {
    private StreamingUtil() {
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean find(JsonReader reader, String name) throws IOException {
        reader.beginObject();
        while (reader.peek() == JsonToken.NAME) {
            if (reader.nextName().equals(name)) {
                return true;
            } else {
                reader.skipValue();
            }
        }
        return false;
    }

    public static void skipObjects(JsonReader reader, int num) throws IOException {
        while (num != 0 && reader.peek() == JsonToken.BEGIN_OBJECT) {
            num--;
            reader.skipValue();
        }
    }

    // reader is expected to start at the begging of field name in json object
    // ends object
    public static void skipToTheEndOfObject(JsonReader reader) throws IOException {
        while (reader.peek() != JsonToken.END_OBJECT) {
            reader.nextName();
            reader.skipValue();
        }
        reader.endObject();
    }
}
