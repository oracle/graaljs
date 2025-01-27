/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.test.external.test262;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link SimpleYAML}.
 */
public class SimpleYAMLTest {
    @Test
    public void testMap() {
        var actual = SimpleYAML.parseMap("""
                        string: Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                        folded-string: >
                         Phasellus id lectus maximus,
                         ullamcorper nisi ac,
                         ullamcorper ex.
                        literal-string: |

                         Integer cursus ante a bibendum lacinia.

                         Pellentesque consequat lectus quis metus volutpat lacinia.
                         Phasellus luctus lorem ac viverra fringilla.

                        list-block:
                         - list item 1 # comment
                         - list item 2
                        list-flow-one-line: [inline item 1, inline item 2]
                        list-flow-multi-line: [
                         flow item 1,
                         flow item 2,
                        ]

                        map-block:
                         mapping1: map value 1 # comment
                         mapping2: map value 2
                        map-flow-one-line: {inline key 1: inline value 1, inline key 2: inline value 2}
                        map-flow-multi-line: {
                         flow key 1: flow value 1,
                         flow key 2: flow value 2,
                        }
                        """);

        var expected = Map.of(
                        "string", "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                        "folded-string", "Phasellus id lectus maximus, ullamcorper nisi ac, ullamcorper ex.",
                        "literal-string", """
                                        Integer cursus ante a bibendum lacinia.

                                        Pellentesque consequat lectus quis metus volutpat lacinia.
                                        Phasellus luctus lorem ac viverra fringilla.
                                        """,
                        "list-block", List.of("list item 1", "list item 2"),
                        "list-flow-one-line", List.of("inline item 1", "inline item 2"),
                        "list-flow-multi-line", List.of("flow item 1", "flow item 2"),
                        "map-block", Map.of("mapping1", "map value 1", "mapping2", "map value 2"),
                        "map-flow-one-line", Map.of("inline key 1", "inline value 1", "inline key 2", "inline value 2"),
                        "map-flow-multi-line", Map.of("flow key 1", "flow value 1", "flow key 2", "flow value 2"));

        expected.forEach((key, value) -> {
            Assert.assertEquals(key, value, actual.get(key));
        });
        Assert.assertEquals(expected.keySet(), actual.keySet());
    }
}
