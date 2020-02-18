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

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static com.github.java.heap.dump.sanitizer.ProgressMonitor.numBytesWrittenMonitor;

public class SanitizeCommandRunner {

    private static final Logger logger = LogManager.getLogger();

    private SanitizeCommand command;

    private StreamFactory streamFactory;

    public SanitizeCommandRunner(SanitizeCommand command) {
        this.command = command;
    }

    public void run() throws Exception {
        Validate.isTrue(command.getBufferByteSize() >= 0, "Invalid buffer size");
        try (AutoCloseable closeable = streamFactory = newStreamFactory(command)) {

            doRun();
        }
    }

    public void doRun() throws Exception {

        logger.info("Starting heap dump sanitization");
        logger.info("Input File: {}", command.getInputFile());
        logger.info("Output File: {}", command.getOutputFile());

        Instant now = Instant.now();
        try (InputStream inputStream = streamFactory.newInputStream();
                OutputStream outputStream = streamFactory.newOutputStream()) {

            HeapDumpSanitizer sanitizer = newSanitizer();
            sanitizer.setInputStream(inputStream);
            sanitizer.setOutputStream(outputStream);
            sanitizer.setProgressMonitor(numBytesWrittenMonitor(command, logger));
            sanitizer.setSanitizationText(command.getSanitizationText());
            sanitizer.sanitize();
        }
        logger.info("Finished in {}", friendlyDuration(now));
    }

    protected StreamFactory newStreamFactory(SanitizeCommand command) {
        return new StreamFactory(command);
    }

    protected HeapDumpSanitizer newSanitizer() {
        return new HeapDumpSanitizer();
    }

    private String friendlyDuration(Instant now) {
        Duration duration = Duration.between(now, Instant.now());
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase(Locale.ENGLISH);
    }

}
