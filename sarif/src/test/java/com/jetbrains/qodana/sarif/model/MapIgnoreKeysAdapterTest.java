package com.jetbrains.qodana.sarif.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.qodana.sarif.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

class MapIgnoreKeysAdapterTest {

    private String jsonString;
    private Map<String, Object> jsonMap;

    @BeforeEach
    void setUp() throws IOException {
        jsonString = TestUtils.readStringFromPath("src/test/resources/testData/serializeTest/ignoringKeys.json").replaceAll("\\s", "");

        Map<String, Object> nestedK1 = new LinkedTreeMap<>();
        nestedK1.put("k1_k1_k1", "v1");

        Map<String, Object> k1 = new LinkedTreeMap<>();
        k1.put("k1_k1", nestedK1);
        k1.put("k1_k2", 2.0);
        k1.put("k1_k3", "v3");

        Collection<Object> k2 = new ArrayList<>();
        Collections.addAll(k2, 1.0, 2.0);

        jsonMap = new HashMap<>();
        jsonMap.put("k1", k1);
        jsonMap.put("k2", k2);
        jsonMap.put("k3", "v3");
        jsonMap.put("k4", 4.0);
    }

    @Test
    void shouldDeserializeNotIgnoringKeys() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Map<String, Object> result = gson.fromJson(jsonString, new TypeToken<Map<String, Object>>(){}.getType());

        Assertions.assertEquals(jsonMap, result);
    }

    @Test
    void shouldDeserializeIgnoringKeys() {
       Collection<String> ignoreKeys = new ArrayList<>();
       Collections.addAll(ignoreKeys, "k2", "k3");

        Gson gson = createGsonIgnoringKeys(ignoreKeys);

        Map<String, Object> result = gson.fromJson(jsonString, new TypeToken<Map<String, Object>>(){}.getType());

        Assertions.assertEquals(2, result.size());
        Assertions.assertNull(result.get("k2"));
        Assertions.assertNull(result.get("k3"));
        Assertions.assertEquals(jsonMap.get("k1"), result.get("k1"));
        Assertions.assertEquals(jsonMap.get("k4"), result.get("k4"));
    }

    @Test
    void shouldSerializeNotIgnoringKeys() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        String result = gson.toJson(jsonMap, new TypeToken<Map<String, Object>>(){}.getType());

        Assertions.assertEquals(jsonString, result);
    }

    @Test
    void shouldSerializeIgnoringKeys() {
        Collection<String> ignoreKeys = new ArrayList<>();
        Collections.addAll(ignoreKeys, "k2", "k3");
        Gson gson = createGsonIgnoringKeys(ignoreKeys);
        String expectedResult = "{\"k1\":{\"k1_k1\":{\"k1_k1_k1\":\"v1\"},\"k1_k2\":2.0,\"k1_k3\":\"v3\"},\"k4\":4.0}";

        String result = gson.toJson(jsonMap, new TypeToken<Map<String, Object>>(){}.getType());

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowErrorOnDeserializeArray() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Assertions.assertThrows(JsonSyntaxException.class, () ->
                gson.fromJson("[]", new TypeToken<Map<String, Object>>(){}.getType())
        );
    }

    @Test
    void shouldThrowErrorOnDeserializeDuplicateKeys() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Assertions.assertThrows(JsonSyntaxException.class, () ->
            gson.fromJson("{\"a\":1,\"a\":1}", new TypeToken<Map<String, Object>>(){}.getType())
        );
    }

    @Test
    void shouldSerializeNull() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        String result = gson.toJson(null, new TypeToken<Map<String, Object>>(){}.getType());

        Assertions.assertEquals("null", result);
    }

    @Test
    void shouldDeserializeNull() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Map<String, Object> result = gson.fromJson("null", new TypeToken<Map<String, Object>>(){}.getType());

        Assertions.assertNull(result);
    }

    private Gson createGsonIgnoringKeys(Collection<String> ignoreKeys) {
        return new GsonBuilder()
                .registerTypeAdapter(
                        new TypeToken<Map<String, Object>>(){}.getType(),
                        new MapIgnoreKeysAdapter(ignoreKeys)
                )
                .create();
    }

}