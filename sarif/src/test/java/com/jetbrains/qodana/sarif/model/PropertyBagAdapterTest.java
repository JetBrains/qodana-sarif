package com.jetbrains.qodana.sarif.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.qodana.sarif.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.security.ec.point.ProjectivePoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class PropertyBagAdapterTest {

    private String jsonString;
    private Map<String, Object> jsonMap;
    private static final Gson defaultGson = new Gson();

    @BeforeEach
    void setUp() throws IOException {
        jsonString = TestUtils.readStringFromPath("src/test/resources/testData/serializeTest/ignoringKeys.json").replaceAll("\\s", "");
        jsonMap = defaultGson.fromJson(jsonString, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    @Test
    void shouldDeserializeNotIgnoringKeys() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        PropertyBag result = gson.fromJson(jsonString, PropertyBag.class);

        Assertions.assertEquals(jsonMap, result);
    }

    @Test
    void shouldDeserializeIgnoringKeys() {
        Collection<String> ignoreKeys = new ArrayList<>();
        Collections.addAll(ignoreKeys, "k2", "k3");

        Gson gson = createGsonIgnoringKeys(ignoreKeys);

        PropertyBag result = gson.fromJson(jsonString, PropertyBag.class);

        Assertions.assertEquals(2, result.size());
        Assertions.assertNull(result.get("k2"));
        Assertions.assertNull(result.get("k3"));
        Assertions.assertEquals(jsonMap.get("k1"), result.get("k1"));
        Assertions.assertEquals(jsonMap.get("k4"), result.get("k4"));
    }

    @Test
    void shouldSerializeNotIgnoringKeys() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());
        PropertyBag propertyBag = new PropertyBag();
        propertyBag.putAll(jsonMap);

        String result = gson.toJson(propertyBag, PropertyBag.class);

        assertJson(jsonString, result);
    }

    @Test
    void shouldSerializeIgnoringKeys() {
        Collection<String> ignoreKeys = new ArrayList<>();
        Collections.addAll(ignoreKeys, "k2", "k3");
        Gson gson = createGsonIgnoringKeys(ignoreKeys);
        PropertyBag propertyBag = new PropertyBag();
        propertyBag.putAll(jsonMap);
        String expectedResult = "{\"k1\":{\"k1_k1\":{\"k1_k1_k1\":\"v1\"},\"k1_k2\":2.0,\"k1_k3\":\"v3\"},\"k4\":4.0}";

        String result = gson.toJson(propertyBag, PropertyBag.class);

        assertJson(expectedResult, result);
    }

    @Test
    void shouldThrowErrorOnDeserializeArray() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Assertions.assertThrows(JsonSyntaxException.class, () ->
                gson.fromJson("[]", PropertyBag.class)
        );
    }

    @Test
    void shouldThrowErrorOnDeserializeDuplicateKeys() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Assertions.assertThrows(JsonSyntaxException.class, () ->
                gson.fromJson("{\"a\":1,\"a\":1}", PropertyBag.class)
        );
    }

    @Test
    void shouldSerializeNull() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        String result = gson.toJson(null, PropertyBag.class);

        Assertions.assertEquals("null", result);
    }

    @Test
    void shouldDeserializeNull() {
        Gson gson = createGsonIgnoringKeys(Collections.emptyList());

        Map<String, Object> result = gson.fromJson("null", PropertyBag.class);

        Assertions.assertNull(result);
    }

    private Gson createGsonIgnoringKeys(Collection<String> ignoreKeys) {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new PropertyBag.PropertyBagTypeAdapterFactory(ignoreKeys))
                .create();
    }

    private void assertJson(String expected, String actual) {
        Assertions.assertEquals(defaultGson.fromJson(expected, Map.class), defaultGson.fromJson(actual, Map.class));
    }
}
