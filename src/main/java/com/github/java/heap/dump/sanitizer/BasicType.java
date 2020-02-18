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

public enum BasicType {
    OBJECT(2),
    BOOLEAN(4),
    CHAR(5),
    FLOAT(6),
    DOUBLE(7),
    BYTE(8),
    SHORT(9),
    INT(10),
    LONG(11);

    private final int u1Code;

    public static int findValueSize(int u1Code, int idSize) {
        BasicType basicType = findByU1Code(u1Code);
        return basicType.getValueSize(idSize);
    }

    private BasicType(int u1Code) {
        this.u1Code = u1Code;
    }

    public int getU1Code() {
        return u1Code;
    }

    private int getValueSize(int idSize) {
        switch (this) {
            case OBJECT:
                return idSize;
            case BOOLEAN:
                return 1;
            case CHAR:
                return 2;
            case FLOAT:
                return 4;
            case DOUBLE:
                return 8;
            case BYTE:
                return 1;
            case SHORT:
                return 2;
            case INT:
                return 4;
            case LONG:
                return 8;
            default:
                throw new IllegalArgumentException("Unknown basic type: " + this);
        }
    }

    private static BasicType findByU1Code(int u1Code) {
        for (BasicType basicType : BasicType.values()) {
            if (basicType.u1Code == u1Code) {
                return basicType;
            }
        }
        throw new IllegalArgumentException("Unknown basic type code: " + u1Code);
    }
}
