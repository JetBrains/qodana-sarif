package com.jetbrains.qodana.sarif;

import com.jetbrains.qodana.sarif.model.Run;
import com.jetbrains.qodana.sarif.model.SarifReport;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

abstract public class StreamParseTest {
    protected static String sanityPath = "src/test/resources/testData/readWriteTest/qodanaReportWithSanity.json";

    @FunctionalInterface
    protected interface ExceptionConsumer<T> extends Consumer<T> {
        void throwingAccept(T t) throws Exception;

        @Override
        default void accept(T t) {
            try {
                throwingAccept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected interface ExceptionSupplier<T> extends Supplier<T> {
        T throwingGet() throws Exception;

        @Override
        default T get() {
            try {
                return throwingGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void forEachInput(ExceptionConsumer<String> test) {
        File[] files = new File("src/test/resources/testData/readWriteTest/").listFiles();
        Arrays
                .stream(Objects.requireNonNull(files))
                .map(file -> file.toPath().toString())
                .forEach(test);
    }

    protected void assertEqualContents(
        SarifReport expected,
        StreamingSarifReport actual,
        TriConsumer<Run, StreamingRun, Integer> assertEqualRunContents
    ) throws IOException {
        Iterator<Run> expectedRuns = expected.getRuns().iterator();
        Iterator<Run> actualRuns = actual.getSarifReport().getRuns().iterator();
        int currentIndex = 0;
        while (expectedRuns.hasNext()) {
            assertTrue(actualRuns.hasNext());
            try (Reader reader = actual.getMakeReader().get()) {
                assertEqualRunContents.accept(
                    expectedRuns.next(),
                    new StreamingRun(actualRuns.next(), reader),
                    currentIndex
                );
            }
            currentIndex++;
        }
        assertEquals(expected.withRuns(null), actual.getSarifReport().withRuns(null));
        assertFalse(actualRuns.hasNext());
    }

    protected SarifReport read(String path, boolean streaming) throws IOException {
        try (Reader reader = makeReader(path)) {
            List<String> skippedProperties;
            if (streaming) {
                skippedProperties = Collections.singletonList("qodana.sanity.results");
            } else {
                skippedProperties = Collections.emptyList();
            }
            return SarifUtil.readReport(reader, !streaming, skippedProperties);
        }
    }

    protected Reader makeReader(String path) throws FileNotFoundException {
        Path input = Paths.get(path);
        return new FileReader(input.toFile());
    }

    @FunctionalInterface
    protected interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    protected static class StreamingSarifReport {
        private final SarifReport sarifReport;
        private final ExceptionSupplier<Reader> makeReader;

        public StreamingSarifReport(SarifReport sarifReport, ExceptionSupplier<Reader> makeReader) {
            this.sarifReport = sarifReport;
            this.makeReader = makeReader;
        }

        public SarifReport getSarifReport() {
            return sarifReport;
        }

        public ExceptionSupplier<Reader> getMakeReader() {
            return makeReader;
        }
    }

    protected static class StreamingRun {
        private final Run run;
        private final Reader reader;

        public StreamingRun(Run run, Reader reader) {
            this.run = run;
            this.reader = reader;
        }

        public Run getRun() {
            return run;
        }

        public Reader getReader() {
            return reader;
        }
    }
}
