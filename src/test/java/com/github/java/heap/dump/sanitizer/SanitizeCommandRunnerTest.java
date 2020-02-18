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

import com.github.java.heap.dump.sanitizer.HeapDumpSanitizer;
import com.github.java.heap.dump.sanitizer.SanitizeCommand;
import com.github.java.heap.dump.sanitizer.SanitizeCommandRunner;
import com.github.java.heap.dump.sanitizer.StreamFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SanitizeCommandRunnerTest {

    private HeapDumpSanitizer sanitizer = mock(HeapDumpSanitizer.class);

    private StreamFactory streamFactory = mock(StreamFactory.class);

    private SanitizeCommand command = new SanitizeCommand();

    private SanitizeCommandRunner runner = spy(new SanitizeCommandRunner(command));

    @BeforeEach
    public void beforeAll() throws IOException {
        doNothing().when(sanitizer).sanitize();
        doReturn(null).when(streamFactory).newInputStream();
        doReturn(null).when(streamFactory).newOutputStream();

        doReturn(sanitizer).when(runner).newSanitizer();
        doReturn(streamFactory).when(runner).newStreamFactory(command);

        command.setInputFile(new File("input"));
        command.setOutputFile(new File("output"));
    }

    @Test
    public void bufferSizeValidation() throws Exception {
        command.setBufferByteSize(-1);
        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid buffer size");
    }

    @Test
    public void run() throws Exception {
        runner.run();

        verify(runner).newSanitizer();
        verify(runner).newStreamFactory(command);
        verify(sanitizer).sanitize();
    }

    @Test
    public void newSanitizer() {
        assertThatCode(() -> {

            new SanitizeCommandRunner(command).newSanitizer();
            new SanitizeCommandRunner(command).newStreamFactory(command);

        }).doesNotThrowAnyException();
    }
}
