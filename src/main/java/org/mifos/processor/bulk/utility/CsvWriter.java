package org.mifos.processor.bulk.utility;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class CsvWriter {

    private CsvWriter() {}

    public static <T> void writeToCsv(List<T> data, Class<T> tClass, CsvMapper csvMapper, boolean overrideHeader, String filepath)
            throws IOException {
        CsvSchema csvSchema = csvMapper.schemaFor(tClass);
        if (overrideHeader) {
            csvSchema = csvSchema.withHeader();
        } else {
            csvSchema = csvSchema.withoutHeader();
        }

        File file = new File(filepath);
        if (!file.exists()) {
            file.createNewFile();
        }
        if (data.isEmpty()) {
            // Jackson writes the header together with the first row, so an empty list would leave a 0-byte file
            Files.writeString(file.toPath(), overrideHeader ? header(csvSchema) : "", StandardCharsets.UTF_8);
            return;
        }
        try (SequenceWriter writer = csvMapper.writerWithSchemaFor(tClass).with(csvSchema).writeValues(file)) {
            writer.writeAll(data);
        }
    }

    private static String header(CsvSchema csvSchema) {
        StringBuilder header = new StringBuilder();
        for (CsvSchema.Column column : csvSchema) {
            if (header.length() > 0) {
                header.append(csvSchema.getColumnSeparator());
            }
            header.append(column.getName());
        }
        return header.append(csvSchema.getLineSeparator()).toString();
    }

}
