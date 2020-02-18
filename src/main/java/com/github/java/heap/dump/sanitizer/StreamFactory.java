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
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class StreamFactory implements AutoCloseable {

    private static final String LOGGING_CONSOLE = "logging.console";
    private static final PrintStream STDOUT = System.out;
    private static final PrintStream STDERR = System.err;

    private SanitizeCommand command;
    private PrintStream stdOutboundStream;

    public StreamFactory(SanitizeCommand command) {
        this.command = command;
        adjustStdOutgoingStreamsIfNeeded();
    }

    @Override
    public void close() {
        // restore std streams
        System.setOut(STDOUT);
        System.setErr(STDERR);
    }

    private void adjustStdOutgoingStreamsIfNeeded() {
        PrintStream nullStream = new PrintStream(new NullOutputStream());
        if (isOutputToStdout()) {
            // write logs to stderr. write heapdump to real stdout.
            stdOutboundStream = STDOUT;
            System.setOut(nullStream);
            changeLoggingConsole("SYSTEM_ERR");

        } else if (isOutputToStderr()) {
            stdOutboundStream = STDERR;
            System.setErr(nullStream);
            changeLoggingConsole("SYSTEM_OUT");
        }
    }

    private void changeLoggingConsole(String target) {
        System.setProperty(LOGGING_CONSOLE, target);
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.reconfigure();
    }

    public InputStream newInputStream() throws IOException {
        File inputFile = command.getInputFile();
        String name = inputFile.getName();

        InputStream inputStream = StringUtils.equalsAny(name, "-", "stdin", "0")
                ? System.in
                : new FileInputStream(inputFile);

        inputStream = getBufferSize() == 0
                ? inputStream
                : new BufferedInputStream(inputStream, getBufferSize());

        if (command.isTarInput()) {
            TarArchiveInputStream tarStream = new TarArchiveInputStream(inputStream);
            Validate.notNull(tarStream.getNextTarEntry(), "no tar entries");
            return tarStream;
        }
        return inputStream;
    }

    public OutputStream newOutputStream() throws IOException {
        File outputFile = command.getOutputFile();
        OutputStream output;
        if (isOutputToStdout()) {
            output = stdOutboundStream;

        } else if (isOutputToStderr()) {
            output = stdOutboundStream;

        } else {
            Validate.isTrue(!command.getInputFile().equals(outputFile),
                    "input and output files cannot be the same");
            output = new FileOutputStream(outputFile);
        }

        output = getBufferSize() == 0
                ? output
                : new BufferedOutputStream(output, getBufferSize());

        if (command.isZipOutput()) {
            ZipOutputStream zipStream = new ZipOutputStream(output);
            String name = outputFile.getName();
            String entryName = StringUtils.removeEnd(name, ".zip");
            zipStream.putNextEntry(new ZipEntry(entryName));
            return zipStream;
        }
        return output;
    }

    private boolean isOutputToStdout() {
        File outputFile = command.getOutputFile();
        String name = outputFile.getName();
        return StringUtils.equalsAny(name, "-", "stdout", "1");
    }

    private boolean isOutputToStderr() {
        File outputFile = command.getOutputFile();
        String name = outputFile.getName();
        return StringUtils.equalsAny(name, "=", "stderr", "2");
    }

    private int getBufferSize() {
        return command.getBufferByteSize();
    }

}
