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

import com.github.java.heap.dump.sanitizer.ProgressMonitor;
import com.github.java.heap.dump.sanitizer.SanitizeCommand;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressMonitorTest {

    private StringWriter writer = new StringWriter();

    private WriterAppender appender;

    private Logger logger;

    @BeforeEach
    public void setupLogging() {
        LoggerContext context = LoggerContext.getContext(false);
        logger = context.getLogger(ProgressMonitor.class.getName());

        appender = WriterAppender.newBuilder()
                .setTarget(writer)
                .setLayout(simpleLayout())
                .setName("string-appender")
                .build();

        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    public void afterEach() {
        logger.removeAppender(appender);
    }

    @Test
    public void numBytesWrittenMonitor() {
        SanitizeCommand cmd = new SanitizeCommand();
        cmd.setBufferByteSize(5);

        Consumer<Long> numBytesWrittenMonitor = ProgressMonitor.numBytesWrittenMonitor(cmd, logger);
        numBytesWrittenMonitor.accept(4L);

        assertThat(writer.toString())
                .isEmpty();

        numBytesWrittenMonitor.accept(5L);
        assertThat(writer.toString())
                .hasLineCount(1)
                .contains("Processed 5 bytes");

        numBytesWrittenMonitor.accept(6L);
        assertThat(writer.toString())
                .hasLineCount(1)
                .contains("Processed 5 bytes");

        numBytesWrittenMonitor.accept(11L);
        assertThat(writer.toString())
                .hasLineCount(2)
                .contains("Processed 5 bytes")
                .contains("Processed 11 bytes");
    }

    private PatternLayout simpleLayout() {
        return PatternLayout.newBuilder()
                .withPattern("%m%n")
                .build();
    }
}
