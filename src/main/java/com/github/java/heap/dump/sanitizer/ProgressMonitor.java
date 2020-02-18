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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class ProgressMonitor {

    public static Consumer<Long> numBytesWrittenMonitor(SanitizeCommand command, Logger logger) {
        int bufferByteSize = command.getBufferByteSize();
        MutableLong steps = new MutableLong();
        return numBytesWritten -> {
            long currentSteps = numBytesWritten / bufferByteSize;
            if (currentSteps != steps.getValue().longValue()) {
                steps.setValue(currentSteps);
                logger.info("Processed {}", FileUtils.byteCountToDisplaySize(numBytesWritten));
            }
        };
    }

    private ProgressMonitor() {

    }
}
