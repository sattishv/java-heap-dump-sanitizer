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
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.Validate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public class Pipe {

    private DataInputStream input;
    private DataOutputStream output;
    private Integer idSize;

    public Pipe(InputStream input, OutputStream output, Consumer<Long> numBytesWrittenMonitor) {
        this.input = new DataInputStream(input);
        this.output = new DataOutputStream(newCountingStream(output, numBytesWrittenMonitor));
    }

    // copy constructor
    private Pipe(DataInputStream input, DataOutputStream output, Integer idSize) {
        this.input = input;
        this.output = output;
        this.idSize = idSize;
    }

    public Pipe newInputBoundedPipe(long count) {
        DataInputStream boundedInput = new DataInputStream(new BoundedInputStream(input, count));
        return new Pipe(boundedInput, output, idSize);
    }

    public int getIdSize() {
        return idSize;
    }

    public void setIdSize(int idSize) {
        Validate.isTrue(idSize == 4 || idSize == 8, "Unknown id size: %s", idSize);
        this.idSize = idSize;
    }

    public int readU1() throws IOException {
        return input.read();
    }

    public void writeU1(int u1) throws IOException {
        output.write(u1);
    }

    public void copyFrom(InputStream inputStream, long count) throws IOException {
        IOUtils.copyLarge(inputStream, output, 0, count);
    }

    public int pipeU1() throws IOException {
        int u1 = input.read();
        output.write(u1);
        return u1;
    }

    public int pipeU1IfPossible() throws IOException {
        int u1 = input.read();
        if (u1 != -1) {
            output.write(u1);
        }
        return u1;
    }

    public int pipeU2() throws IOException {
        int u2 = input.readShort();
        output.writeShort(u2);
        return u2;
    }

    public long pipeU4() throws IOException {
        int u4 = input.readInt();
        output.writeInt(u4);
        return Integer.toUnsignedLong(u4);
    }

    public long pipeId() throws IOException {
        if (idSize == 4) {
            return pipeU4();
        } else {
            long value = input.readLong();
            output.writeLong(value);
            Validate.isTrue(value >= 0, "Small unsigned long expected");
            return value;
        }
    }

    public void pipe(long count) throws IOException {
        IOUtils.copyLarge(input, output, 0, count);
    }

    public void skipInput(long count) throws IOException {
        IOUtils.skipFully(input, count);
    }

    public String pipeNullTerminatedString() throws IOException {
        int byteValue = Integer.MAX_VALUE;
        StringBuilder sb = new StringBuilder();
        while (byteValue > 0) {
            byteValue = input.read();
            if (byteValue >= 0) {
                output.write(byteValue);
                sb.append((char) byteValue);
            }
        }
        return sb.toString();
    }

    private static OutputStream newCountingStream(OutputStream output, Consumer<Long> writeCountMonitor) {
        return new CountingOutputStream(output) {

            @Override
            protected void beforeWrite(final int n) {
                super.beforeWrite(n);
                writeCountMonitor.accept(getByteCount());
            }
        };
    }
}
