package com.jetbrains.qodana.sarif;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestUtils {
    private TestUtils() {
    }

    @SafeVarargs
    public static <T> List<T> mutableList(T... items) {
        return Arrays.stream(items).collect(Collectors.toList());
    }

    public static String readStringFromPath(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }
}
