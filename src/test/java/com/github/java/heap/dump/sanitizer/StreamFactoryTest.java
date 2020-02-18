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

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StreamFactoryTest {

    @TempDir
    static Path tempDir;

    private static final PrintStream STDOUT = System.out;
    private static final PrintStream STDERR = System.err;

    private StreamFactory streamFactory;

    @AfterEach
    public void afterEach() {
        if (streamFactory != null) {
            streamFactory.close();
        }
    }

    @Test
    public void stdinInputStream() throws IOException {
        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(new File("-"));
        cmd.setBufferByteSize(0);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isEqualTo(System.in);
    }

    @Test
    public void stdoutOutputStream() throws IOException {
        SanitizeCommand cmd = newCommand();
        cmd.setOutputFile(new File("-"));
        cmd.setBufferByteSize(0);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isEqualTo(STDOUT);
    }

    @Test
    public void stderrOutputStream() throws IOException {
        SanitizeCommand cmd = newCommand();
        cmd.setOutputFile(new File("2"));
        cmd.setBufferByteSize(0);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isEqualTo(STDERR);
    }

    @Test
    public void bufferedInputStream() throws IOException {
        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(new File("-"));

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isInstanceOf(BufferedInputStream.class)
                .isNotSameAs(System.in);
    }

    @Test
    public void bufferedOutputStream() throws IOException {
        SanitizeCommand cmd = newCommand();
        cmd.setOutputFile(new File("-"));

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isInstanceOf(BufferedOutputStream.class);
    }

    @Test
    public void fileInputStream() throws IOException {
        Path inputFile = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(inputFile.toFile());
        cmd.setBufferByteSize(0);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isInstanceOf(FileInputStream.class);
    }

    @Test
    public void fileOutputStream() throws IOException {
        Path file = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(new File("foo"));
        cmd.setOutputFile(file.toFile());
        cmd.setBufferByteSize(0);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isInstanceOf(FileOutputStream.class);
    }

    @Test
    public void tarInputStream() throws IOException {
        Path inputFile = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
        writeTar(inputFile);

        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(inputFile.toFile());
        cmd.setBufferByteSize(0);
        cmd.setTarInput(true);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newInputStream())
                .isInstanceOf(TarArchiveInputStream.class);
    }

    @Test
    public void testZipOutputStream() throws IOException {
        Path outputFile = Files.createTempFile(tempDir, getClass().getSimpleName(), ".zip");

        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(new File("foo"));
        cmd.setOutputFile(outputFile.toFile());
        cmd.setBufferByteSize(0);
        cmd.setZipOutput(true);

        streamFactory = new StreamFactory(cmd);
        assertThat(streamFactory.newOutputStream())
                .isInstanceOf(ZipOutputStream.class);
    }

    @Test
    public void sameInputOutput() {
        SanitizeCommand cmd = newCommand();
        cmd.setInputFile(new File("foo"));
        cmd.setOutputFile(new File("foo"));

        streamFactory = new StreamFactory(cmd);
        assertThatThrownBy(() -> streamFactory.newOutputStream())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void log4j2Config() throws IOException {
        String content = IOUtils.resourceToString("/log4j2.xml", StandardCharsets.UTF_8);
        assertThat(content)
                .contains("${sys:logging.console:-SYSTEM_OUT}");
    }

    private void writeTar(Path destPath) throws IOException {
        byte[] srcBytes = IOUtils.resourceToByteArray("/sample.tar");
        Files.write(destPath, srcBytes, TRUNCATE_EXISTING);
    }

    private SanitizeCommand newCommand() {
        SanitizeCommand cmd = new SanitizeCommand();
        cmd.setInputFile(new File("input.txt"));
        cmd.setOutputFile(new File("output.txt"));
        return cmd;
    }
}
