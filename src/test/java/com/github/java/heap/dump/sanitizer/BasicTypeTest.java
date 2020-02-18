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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BasicTypeTest {

    @ParameterizedTest
    @EnumSource(BasicType.class)
    void withAllEnumValues(BasicType basicType) {
        assertThat(BasicType.findValueSize(basicType.getU1Code(), 8))
                .isGreaterThan(0);
    }

    @Test
    void unknownU1Tag() {
        assertThatThrownBy(() -> BasicType.findValueSize(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown basic type code: 0");
    }
}
