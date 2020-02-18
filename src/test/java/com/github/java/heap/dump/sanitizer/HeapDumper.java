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

import javax.management.MBeanServer;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class HeapDumper {

    private static final String CLASS_NAME = "com.sun.management.HotSpotDiagnosticMXBean";

    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    public static void dumpHeap(Path path) throws Exception {
        dumpHeap(path, false);
    }

    public static void dumpHeap(Path path, boolean live) throws Exception {
        Class<?> clazz = Class.forName(CLASS_NAME);
        Object mxBean = getHotSpotMxBean(clazz);
        Method method = clazz.getMethod("dumpHeap", String.class, boolean.class);
        method.invoke(mxBean, path.toString(), live);
    }

    private static <T> T getHotSpotMxBean(Class<T> clazz) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        return ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
    }
}
