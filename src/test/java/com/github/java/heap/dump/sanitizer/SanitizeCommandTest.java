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

import com.github.java.heap.dump.sanitizer.SanitizeCommand.ManifestVersionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.meanbean.test.BeanVerifications.verifyThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SanitizeCommandTest {

    @AfterEach
    public void afterEach() {
        SanitizeCommand.commandLine = SanitizeCommand.newCommandLine();
    }

    @Test
    public void sanitizationText() {
        assertThat(escapedSanitizationText("\\0"))
                .isEqualTo("\0");
        assertThat(escapedSanitizationText("\0"))
                .isEqualTo("\0");

        assertThat(escapedSanitizationText("foobar"))
                .isEqualTo("foobar");
    }

    @Test
    public void testBean() {
        verifyThat(SanitizeCommand.class)
                .isValidJavaBean()
                .hasValidToStringMethod();
    }

    @Test
    public void call() throws Exception {
        SanitizeCommandRunner runner = mock(SanitizeCommandRunner.class);
        SanitizeCommand cmd = spy(new SanitizeCommand());
        doReturn(runner).when(cmd).newRunner();

        cmd.call();

        verify(runner).run();
    }

    @Test
    public void versionProvider() throws Exception {
        String[] version = new ManifestVersionProvider().getVersion();
        assertThat(version)
                .containsAnyOf(getClass().getPackage().getImplementationVersion(), "unknown");
    }

    @Test
    public void main() throws Exception {
        SanitizeCommand cmd = spy(new SanitizeCommand());
        SanitizeCommand.commandLine = new CommandLine(cmd);
        doNothing().when(cmd).call();

        SanitizeCommand.main("--tar-input", "--buffer-size", "50MB", "--text", "xxx", "-z", "my-input", "my-output");

        assertThat(cmd.isTarInput())
                .isTrue();
        assertThat(cmd.getBufferByteSize())
                .isEqualTo(DataSize.ofMegabytes(50).toBytes());
        assertThat(cmd.getSanitizationText())
                .isEqualTo("xxx");
        assertThat(cmd.getInputFile())
                .isEqualTo(new File("my-input"));
        assertThat(cmd.isZipOutput())
                .isTrue();
        assertThat(cmd.getOutputFile())
                .isEqualTo(new File("my-output"));
    }

    private String escapedSanitizationText(String sanitizationText) {
        SanitizeCommand cmd = new SanitizeCommand();
        cmd.setSanitizationText(sanitizationText);
        return cmd.getSanitizationText();
    }

}
