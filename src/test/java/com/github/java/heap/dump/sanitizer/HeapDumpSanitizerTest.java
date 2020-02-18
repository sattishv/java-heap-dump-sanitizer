/*-
 * ​​​
 * java-heap-dump-sanitizer
 * ⁣⁣⁣
 * Copyright (C) 2020 the original author or authors.
 * ⁣⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ﻿﻿﻿﻿﻿
 */

package com.github.java.heap.dump.sanitizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(Random.class)
public class HeapDumpSanitizerTest {

    private static final Logger logger = LogManager.getLogger();

    @TempDir
    static Path tempDir;

    // "his-secret-value" with each letter incremented by 1
    private String hisSecretValue = "ijt.tfdsfu.wbmvf";
    private String herSecretValue = "ifs.tfdsfu.wbmvf";
    private String itsSecretValue = "jut.tfdsfu.wbmvf";

    // "his-classified-value" with each letter incremented by 1
    private String hisClassifiedValue = "ijt.dmbttjgjfe.wbmvf";
    private String herClassifiedValue = "ifs.dmbttjgjfe.wbmvf";
    private String itsClassifiedValue = "jut.dmbttjgjfe.wbmvf";

    @BeforeEach
    public void beforeEach(TestInfo info) {
        logger.info("Test - {}:", info.getDisplayName());
        System.gc();
    }

    @Test
    @DisplayName("Verify that heap dump normally contains sensitive data")
    public void secretsAreInHeapDump() throws Exception {

        // keep as byte array in mem
        byte[] actualHisSecretValue = adjustLettersToByteArray(hisSecretValue);

        // keep as char array in mem
        String actualHerSecretValue = new String(actualHisSecretValue, StandardCharsets.UTF_8)
                .replace("his", "her");

        // interned
        lengthenAndInternItsValue(actualHisSecretValue);

        actualHisSecretValue = lengthen(actualHisSecretValue, DataSize.ofMegabytes(1));
        actualHerSecretValue = lengthen(actualHerSecretValue, DataSize.ofMegabytes(1));

        byte[] heapDump = loadHeapDump();

        byte[] expectedHisSecretValueBytes = adjustLettersToByteArray(hisSecretValue);
        byte[] expectedHerSecretValueBytes = adjustLettersToByteArray(herSecretValue);
        byte[] expectedItsSecretValueBytes = adjustLettersToByteArray(itsSecretValue);

        assertThat(heapDump)
                .overridingErrorMessage("sequences do not match") // normal error message would be long and not helpful at all
                .containsSequence(expectedHisSecretValueBytes)
                .containsSequence(expectedHerSecretValueBytes)
                .containsSequence(expectedItsSecretValueBytes);
    }

    @Test
    @DisplayName("Verify that sanitized heap dump does not contains sensitive data")
    public void confidentialsNotInHeapDump() throws Exception {

        byte[] actualHisConfidentialValue = IOUtils.resourceToByteArray("/classifieds.txt");
        String actualHerConfidentialValue = new String(actualHisConfidentialValue, StandardCharsets.UTF_8)
                .replace("his", "her");
        lengthenAndInternItsValue(actualHisConfidentialValue);

        actualHisConfidentialValue = lengthen(actualHisConfidentialValue, DataSize.ofMegabytes(1));
        actualHerConfidentialValue = lengthen(actualHerConfidentialValue, DataSize.ofMegabytes(1));

        byte[] heapDump = loadSanitizedHeapDump();

        byte[] expectedHisClassifiedValueBytes = adjustLettersToByteArray(hisClassifiedValue);
        byte[] expectedHerClassifiedValueBytes = adjustLettersToByteArray(herClassifiedValue);
        byte[] expectedItsClassifiedValueBytes = adjustLettersToByteArray(itsClassifiedValue);

        verifyDoesNotContainsSequence(heapDump, expectedHisClassifiedValueBytes);
        verifyDoesNotContainsSequence(heapDump, expectedHerClassifiedValueBytes);
        verifyDoesNotContainsSequence(heapDump, expectedItsClassifiedValueBytes);
    }

    private void verifyDoesNotContainsSequence(byte[] big, byte[] small) {
        String corrId = System.currentTimeMillis() + "";
        try {
            assertThat(big)
                    .withFailMessage(corrId)
                    .containsSequence(small);
        } catch (AssertionError e) {
            if (StringUtils.contains(e.getMessage(), corrId)) {
                return; // good
            }
        }
        throw new AssertionError("does in fact contains sequence");
    }

    private void lengthenAndInternItsValue(byte[] value) {
        String itsValue = new String(value, StandardCharsets.UTF_8)
                .replace("his", "its");
        itsValue = lengthen(itsValue, DataSize.ofMegabytes(1));
        itsValue.intern();
    }

    private byte[] lengthen(byte[] input, DataSize wantedDataSize) {
        return Arrays.copyOf(input, (int) wantedDataSize.toBytes());
    }

    private String lengthen(String input, DataSize wantedDataSize) {
        byte[] currentBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] newBytes = lengthen(currentBytes, wantedDataSize);
        return new String(newBytes, StandardCharsets.UTF_8);
    }

    private byte[] adjustLettersToByteArray(String str) {
        return adjustLetters(str, -1)
                .getBytes(StandardCharsets.UTF_8);
    }

    private String adjustLetters(String str, int adjustment) {
        return str.chars()
                .map(chr -> chr + adjustment)
                .mapToObj(chr -> String.valueOf((char) chr))
                .collect(Collectors.joining(""));
    }

    private Path triggerHeapDump() throws Exception {
        Path heapDumpPath = newTempFilePath();

        logger.info("Heap dumping to {}", heapDumpPath);
        HeapDumper.dumpHeap(heapDumpPath);

        return heapDumpPath;
    }

    private byte[] loadHeapDump() throws Exception {
        return loadHeapDump(triggerHeapDump());
    }

    private byte[] loadHeapDump(Path heapDumpPath) throws IOException, Exception {
        long size = Files.size(heapDumpPath);
        logger.info("Loading heap dump. size={} name={}", byteCountToDisplaySize(size), heapDumpPath.getFileName());
        return Files.readAllBytes(heapDumpPath);
    }

    private byte[] loadSanitizedHeapDump() throws Exception {
        Path heapDump = triggerHeapDump();
        Path sanitizedHeapDumpPath = newTempFilePath();
        SanitizeCommand.main(heapDump.toString(), sanitizedHeapDumpPath.toString());
        return loadHeapDump(sanitizedHeapDumpPath);
    }

    private Path newTempFilePath() throws IOException {
        Path path = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
        Files.delete(path);
        return path;
    }

}
