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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PipeTest {

    private String data = "hello world\0more-stuff-here";

    private ByteArrayInputStream inputBytes = byteStreamOf(data);

    private ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

    private AtomicLong monitor = new AtomicLong();

    private Pipe pipe = new Pipe(inputBytes, outputBytes, monitor::set);

    @Test
    public void idSizeSetGet() {
        pipe.setIdSize(4);
        assertThat(pipe.getIdSize())
                .isEqualTo(4);

        pipe.setIdSize(8);
        assertThat(pipe.getIdSize())
                .isEqualTo(8);
    }

    @Test
    @DisplayName("check that NPE is thrown")
    public void idSizeNullDefault() {
        assertThatThrownBy(() -> pipe.getIdSize())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void idSize4Or8() {
        assertThatThrownBy(() -> pipe.setIdSize(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown id size: 10");
    }

    @Test
    public void readU1() throws IOException {
        assertThat(pipe.readU1())
                .isEqualTo('h');

        pipe.skipInput(data.length() - 1);
        verifyEoF();

        assertThat(outputBytes.toByteArray())
                .hasSize(0);
    }

    @Test
    public void writeU1() throws IOException {
        pipe.writeU1('z');
        assertThat(outputString())
                .isEqualTo("z");

        verifyInputStreamUnchanged();
    }

    @Test
    public void pipeByLength() throws IOException {
        pipe.pipe(data.length());
        verifyEoF();
        assertThat(outputString())
                .isEqualTo(data);
    }

    @Test
    public void pipeId4() throws IOException {
        pipe.setIdSize(4);
        pipe.pipeId();

        assertThat(outputString())
                .isEqualTo("hell")
                .hasSize(4);
    }

    @Test
    public void pipeId8() throws IOException {
        pipe.setIdSize(8);
        pipe.pipeId();

        assertThat(outputString())
                .isEqualTo("hello wo")
                .hasSize(8);
    }

    @Test
    public void copyFrom() throws IOException {
        String newData = "byte stream data";
        pipe.copyFrom(byteStreamOf(newData), newData.length());

        verifyInputStreamUnchanged();

        assertThat(outputString())
                .isEqualTo(newData);
    }

    @Test
    public void pipeU1() throws IOException {
        int u1 = pipe.pipeU1();
        assertThat(u1)
                .isEqualTo('h');

        assertThat(outputString())
                .isEqualTo("h");
        assertThat(inputBytes.read())
                .isEqualTo('e');
    }

    @Test
    public void pipeU1IfPossible() throws IOException {
        int u1 = pipe.pipeU1IfPossible();
        assertThat(u1)
                .isEqualTo('h');

        assertThat(outputString())
                .isEqualTo("h");
        assertThat(inputBytes.read())
                .isEqualTo('e');
    }

    @Test
    @DisplayName("pipe u1 on exhausted input")
    public void pipeU1IfPossibleNot() throws IOException {
        pipe.pipe(100);
        int u1 = pipe.pipeU1IfPossible();
        assertThat(u1)
                .isEqualTo(-1);

        assertThat(outputString())
                .isEqualTo(data);
    }

    @Test
    public void pipeU2() throws IOException {
        pipe.pipeU2();
        assertThat(inputBytes.read())
                .isEqualTo('l');
        assertThat(outputString())
                .isEqualTo("he");
    }

    @Test
    public void pipeNullTerminatedString() throws IOException {
        assertThat(pipe.pipeNullTerminatedString())
                .isEqualTo("hello world\0")
                .isEqualTo(outputString());
    }

    @Test
    public void newInputBoundedPipe() throws IOException {
        pipe.pipeU1();

        Pipe boundedPipe = pipe.newInputBoundedPipe(4);
        assertThat(boundedPipe.pipeNullTerminatedString())
                .isEqualTo("ello");

        assertThat(outputString())
                .isEqualTo("hello");

        assertThat(pipe.pipeNullTerminatedString())
                .isEqualTo(" world\0");
        assertThat(outputString())
                .isEqualTo("hello world\0");
    }

    @Test
    public void progress() throws IOException {
        pipe.pipeU1();
        assertThat(monitor)
                .hasValue(1);

        pipe.pipe(100);
        assertThat(monitor)
                .hasValue(data.length());
    }

    private void verifyEoF() throws IOException {
        assertThat(pipe.readU1())
                .isEqualTo(-1);
    }

    private void verifyInputStreamUnchanged() {
        assertThat(inputBytes.read())
                .isEqualTo('h');
    }

    private String outputString() throws IOException {
        return outputBytes.toString("UTF-8");
    }

    private ByteArrayInputStream byteStreamOf(String str) {
        return new ByteArrayInputStream(bytesOf(str));
    }

    private byte[] bytesOf(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
