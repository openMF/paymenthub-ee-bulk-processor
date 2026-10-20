package org.mifos.processor.bulk.utility;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mifos.processor.bulk.schema.TransactionResult;

/**
 * Before PHEE-428 an empty list produced a 0-byte result file with no CSV header (Jackson writes the header with the
 * first row), and the writer was never closed.
 */
class CsvWriterTest {

    private final CsvMapper csvMapper = new CsvMapper();

    @Test
    void emptyListWithHeaderWritesTheSameHeaderAsANonEmptyList(@TempDir Path dir) throws Exception {
        Path empty = dir.resolve("Result_empty.csv");
        Path oneRow = dir.resolve("Result_one.csv");

        CsvWriter.writeToCsv(List.of(), TransactionResult.class, csvMapper, true, empty.toString());
        CsvWriter.writeToCsv(List.of(new TransactionResult()), TransactionResult.class, csvMapper, true, oneRow.toString());

        List<String> emptyLines = Files.readAllLines(empty);
        List<String> oneRowLines = Files.readAllLines(oneRow);
        assertThat(emptyLines).hasSize(1);
        assertThat(oneRowLines).hasSize(2);
        assertThat(emptyLines.get(0)).isNotBlank().isEqualTo(oneRowLines.get(0));
    }

    @Test
    void emptyListWithoutHeaderWritesAnEmptyFile(@TempDir Path dir) throws Exception {
        Path empty = dir.resolve("Result_empty.csv");

        CsvWriter.writeToCsv(List.of(), TransactionResult.class, csvMapper, false, empty.toString());

        assertThat(empty).exists();
        assertThat(Files.size(empty)).isZero();
    }
}
