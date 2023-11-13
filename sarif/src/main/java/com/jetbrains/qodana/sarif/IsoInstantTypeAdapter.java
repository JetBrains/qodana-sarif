package com.jetbrains.qodana.sarif;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

/**
 * Reads/Writes instants as SARIF compliant strings.
 * <a href="https://docs.oasis-open.org/sarif/sarif/v2.0/csprd02/sarif-v2.0-csprd02.html#_Toc10127644"/>
 */
final class IsoInstantTypeAdapter extends TypeAdapter<Instant> {
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart()
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_TIME)
            .optionalEnd()
            .toFormatter();


    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        ZonedDateTime utc = ZonedDateTime.ofInstant(value, ZoneId.of("UTC"));

        out.value(FORMATTER.format(utc));
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        TemporalAccessor parsed = FORMATTER.parseBest(in.nextString(), Instant::from, LocalDate::from);
        if (parsed instanceof Instant) {
            return (Instant) parsed;
        } else if (parsed instanceof LocalDate) {
            return ((LocalDate) parsed).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Unreachable");
    }
}
