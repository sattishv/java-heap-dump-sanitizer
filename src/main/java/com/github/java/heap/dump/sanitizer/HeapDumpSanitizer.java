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

import org.apache.commons.io.input.InfiniteCircularInputStream;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

/**
 * Heavily based on: <br>
 * 
 * <a href="http://hg.openjdk.java.net/jdk6/jdk6/jdk/raw-file/tip/src/share/demo/jvmti/hprof/manual.html#mozTocId848088">
 * Heap Dump Binary Format Spec
 * </a> (highly recommended to make sense of the code in any meaningful way)
 * <br>
 * 
 * <a href="https://github.com/openjdk/jdk/blob/a2bbf933d96dc4a911ac4b429519937d8dd83200/src/hotspot/share/services/heapDumper.cpp">
 * JDK heapDumper.cpp
 * </a>
 * <br>
 * 
 * <a href="https://github.com/AdoptOpenJDK/jheappo">
 * JHeappo
 * </a> (clean modern code)
 * <br>
 * 
 * <a href="https://github.com/apache/netbeans/tree/f2611e358c181935500ea4d9d9142fb850504a72/profiler/lib.profiler/src/org/netbeans/lib/profiler/heap">
 * NetBeans/VisualVM HeapDump code (old but reference)
 * </a>
 */
public class HeapDumpSanitizer {

    private static final int TYPE_BYTE = 8;
    private static final int TYPE_CHAR = 5;
    private static final int TAG_HEAP_DUMP = 0x0C;
    private static final int TAG_HEAP_DUMP_SEGMENT = 0x1C;

    private static final Logger logger = LogManager.getLogger();

    // for debugging/testing
    private static final boolean enableSanitization = isFalse(Boolean.getBoolean("disable-sanitization"));

    private InputStream inputStream;
    private OutputStream outputStream;
    private Consumer<Long> progressMonitor;
    private String sanitizationText;

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setProgressMonitor(Consumer<Long> numBytesWrittenMonitor) {
        this.progressMonitor = numBytesWrittenMonitor;
    }

    public void setSanitizationText(String sanitizationText) {
        this.sanitizationText = sanitizationText;
    }

    public void sanitize() throws IOException {
        Validate.notEmpty(sanitizationText);

        Pipe pipe = new Pipe(inputStream, outputStream, progressMonitor);

        /*
         * The basic fields in the binary output are u1 (1 byte), u2 (2 byte), u4 (4 byte), and u8 (8 byte).
         * 
         * The binary output begins with the information:
         * [u1]* An initial NULL terminated series of bytes representing the format name and version
         * u4 size of identifiers. Identifiers are used to represent UTF8 strings, objects, stack traces, etc.
         * u4 high word of number of milliseconds since 0:00 GMT, 1/1/70
         * u4 low word of number of milliseconds since 0:00 GMT, 1/1/70
         */
        String version = pipe.pipeNullTerminatedString().trim();
        logger.debug("Heap Dump Version: {}", version);

        pipe.setIdSize((int) pipe.pipeU4());
        logger.debug("Id Size: {}", pipe.getIdSize());
        pipe.pipe(8);

        /*
         * Followed by a sequence of records that look like:
         * u1		TAG: denoting the type of the record
         * u4		TIME: number of microseconds since the time stamp in the header
         * u4		LENGTH: number of bytes that follow this u4 field and belong to this record
         * [u1]*	BODY: as many bytes as specified in the above u4 field
         */

        while (true) {
            int tag = pipe.pipeU1IfPossible();
            if (tag == -1) {
                break;
            }

            pipe.pipeU4(); // timestamp
            long length = pipe.pipeU4();
            logger.debug("Tag: {}", tag);
            logger.debug("Length: {}", length);

            if (isHeapDumpRecord(tag)) {
                Pipe heapPipe = pipe.newInputBoundedPipe(length);
                copyHeapDumpRecord(heapPipe);
            } else {
                pipe.pipe(length);
            }
        }
    }

    private void copyHeapDumpRecord(Pipe pipe) throws IOException {
        while (true) {
            int tag = pipe.pipeU1IfPossible();
            if (tag == -1) {
                break;
            }
            logger.debug("Heap Dump Tag: {}", tag);

            pipe.pipeId();
            switch (tag) {
                case 0xFF:
                    break;

                case 0x01:
                    pipe.pipeId();
                    break;

                case 0x02:
                case 0x03:
                    pipe.pipe(4 + 4);
                    break;

                case 0x04:
                    pipe.pipeU4();
                    break;

                case 0x05:
                    break;

                case 0x06:
                    pipe.pipeU4();
                    break;

                case 0x07:
                    break;

                case 0x08:
                    pipe.pipe(4 + 4);
                    break;

                case 0x20:
                    copyHeapDumpClassDump(pipe, tag);
                    break;

                case 0x21:
                    copyHeapDumpInstanceDump(pipe, tag);
                    break;

                case 0x22:
                    copyHeapDumpObjectArrayDump(pipe, tag);
                    break;

                case 0x23:
                    copyHeapDumpPrimitiveArrayDump(pipe, tag);
                    break;

                default:
                    throw new IllegalArgumentException("" + tag);
            }
        }
    }

    private void copyHeapDumpClassDump(Pipe pipe, int id) throws IOException {
        pipe.pipeU4(); // stacktrace
        pipe.pipeId(); // super class object id
        pipe.pipeId(); // class loader object id
        pipe.pipeId(); // signers object id
        pipe.pipeId(); // protection domain
        pipe.pipeId(); // reserved
        pipe.pipeId(); // reserved
        pipe.pipeU4(); // instance size

        int numConstantPoolRecords = pipe.pipeU2();
        for (int i = 0; i < numConstantPoolRecords; i++) {
            pipe.pipeU2();
            int entryType = pipe.pipeU1();
            pipeBasicType(pipe, entryType);
        }

        int numStaticFields = pipe.pipeU2();
        for (int i = 0; i < numStaticFields; i++) {
            pipe.pipeId();
            int entryType = pipe.pipeU1();
            pipeBasicType(pipe, entryType);
        }

        int numInstanceFields = pipe.pipeU2();
        for (int i = 0; i < numInstanceFields; i++) {
            pipe.pipeId();
            pipe.pipeU1();
        }
    }

    private void pipeBasicType(Pipe pipe, int entryType) throws IOException {
        int valueSize = BasicType.findValueSize(entryType, pipe.getIdSize());
        pipe.pipe(valueSize);
    }

    private void copyHeapDumpInstanceDump(Pipe pipe, int id) throws IOException {
        pipe.pipeU4();
        pipe.pipeId();
        long numBytes = pipe.pipeU4();
        pipe.pipe(numBytes);
    }

    private void copyHeapDumpObjectArrayDump(Pipe pipe, int id) throws IOException {
        pipe.pipeU4();
        long numElements = pipe.pipeU4();
        pipe.pipeId();
        for (long i = 0; i < numElements; i++) {
            pipe.pipeId();
        }
    }

    /*
     * PRIMITIVE ARRAY DUMP	 * 	0x23
     * 	ID	array object ID
     * 	u4	stack trace serial number
     * 	u4	number of elements
     * 	u1	element type (See Basic Type)
     * 	[u1]*	elements (packed array) 
     */
    private void copyHeapDumpPrimitiveArrayDump(Pipe pipe, int id) throws IOException {
        pipe.pipeU4();
        long numElements = pipe.pipeU4();

        int elementType = pipe.pipeU1();
        int elementSize = BasicType.findValueSize(elementType, pipe.getIdSize());

        long numBytes = Math.multiplyExact(numElements, elementSize);

        if (enableSanitization && (elementType == TYPE_CHAR || elementType == TYPE_BYTE)) {
            applySanitization(pipe, numBytes);
        } else {
            pipe.pipe(numBytes);
        }
    }

    private void applySanitization(Pipe pipe, long numBytes) throws IOException {
        pipe.skipInput(numBytes);

        byte[] replacementData = sanitizationText.getBytes(StandardCharsets.UTF_8);
        try (InputStream replacementDataStream = new InfiniteCircularInputStream(replacementData)) {
            pipe.copyFrom(replacementDataStream, numBytes);
        }
    }

    private boolean isHeapDumpRecord(int tag) {
        return tag == TAG_HEAP_DUMP || tag == TAG_HEAP_DUMP_SEGMENT;
    }

}
