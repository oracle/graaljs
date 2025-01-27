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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Grossly simplified YAML parser sufficient for parsing the YAML format used in test262 headers.
 *
 * Values can be plain unquoted string scalars (including folded and literal block scalar styles),
 * and simple non-nested block-style and flow-style sequences and mappings.
 */
public class SimpleYAML {

    private static final String PLAIN_FIRST = "[^-?:,\\[\\]{}#&*!|>'\"%@`\\s]";
    private static final String PLAIN_SAFE_IN = "[^:#,\\[\\]{}\\s]";
    private static final String KEY_IN = PLAIN_FIRST + "(?:[ \\t]|" + PLAIN_SAFE_IN + ")*";

    /** Matches {@code key:} or {@code key: value}. */
    private static final Pattern MAPPING_START_PATTERN = Pattern.compile("^(?<key>" + KEY_IN + ")[ \\t]*:(?:[ \\t]+(?<value>.+?))?[ \\t]*(?:[ \\t]#.*)?$", Pattern.MULTILINE);
    /** Matches a block of indented lines. */
    private static final Pattern INDENTED_BLOCK_PATTERN = Pattern.compile("\\R*(?<block>(?:^(?:[ \\t]+.*)?$\\R?)+)", Pattern.MULTILINE);
    /** Matches a block of list item lines (starting with "{@code - }"). */
    private static final Pattern LIST_BLOCK_PATTERN = Pattern.compile("\\R*(?<block>(?:^(?:[ \\t]*-[ \\t]+.*)?$\\R?)+)", Pattern.MULTILINE);
    /** Matches a simple list item line. */
    private static final Pattern LIST_ITEM_LINE_PATTERN = Pattern.compile("^[ \\t]*-[ \\t]+(?<value>.+?)[ \\t]*(?:[ \\t]#.*)?$", Pattern.MULTILINE);

    private static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");
    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile("\\s*:\\s+");
    private static final Pattern NEWLINE_SPLIT_PATTERN = Pattern.compile("\\R");

    public static Map<String, Object> parseMap(String yaml) {
        Map<String, Object> map = new LinkedHashMap<>();
        parseMapBlock(yaml, map::put);
        return Collections.unmodifiableMap(map);
    }

    private static void parseMapBlock(String yaml, BiConsumer<String, Object> entries) {
        int end = 0;
        while (end < yaml.length()) {
            // Find start of next key-value pair.
            var keyMatcher = MAPPING_START_PATTERN.matcher(yaml).region(end, yaml.length());
            if (keyMatcher.find()) {
                String key = keyMatcher.group("key");
                String value = keyMatcher.group("value");
                end = keyMatcher.end();
                if (value != null && !value.isEmpty()) {
                    char firstChar = value.charAt(0);
                    switch (firstChar) {
                        case '>', '|' -> {
                            // block scalar with folded ('>') or literal ('|') line breaks.
                            var blockMatcher = INDENTED_BLOCK_PATTERN.matcher(yaml).region(end, yaml.length());
                            if (blockMatcher.lookingAt()) {
                                String string = blockMatcher.group("block").stripTrailing().stripIndent() + "\n";
                                if (firstChar == '>') {
                                    string = NEWLINE_SPLIT_PATTERN.splitAsStream(string).collect(Collectors.joining(" "));
                                }
                                entries.accept(key, string);
                                end = blockMatcher.end();
                            }
                        }
                        case '[', '{' -> {
                            // flow sequence ('[') or mappings ('{').
                            int valueStart = keyMatcher.start("value") + 1;
                            int valueEnd = yaml.indexOf(firstChar == '[' ? ']' : '}', valueStart);
                            if (valueEnd == -1) {
                                break;
                            }
                            String enclosed = yaml.substring(valueStart, valueEnd);
                            List<String> list = COMMA_SPLIT_PATTERN.splitAsStream(enclosed.trim()).toList();
                            if (firstChar == '[') {
                                entries.accept(key, list);
                            } else {
                                assert firstChar == '{';
                                var mapValue = list.stream().map(e -> COLON_SPLIT_PATTERN.split(e, 2)).filter(a -> a.length == 2).collect(
                                                Collectors.toUnmodifiableMap(a -> a[0], a -> a[1]));
                                entries.accept(key, mapValue);
                            }
                            end = valueEnd + 1;
                        }
                        default -> {
                            // plain scalar
                            entries.accept(key, value);
                        }
                    }
                } else {
                    // block sequence or mappings
                    var blockMatcher = LIST_BLOCK_PATTERN.matcher(yaml).region(end, yaml.length());
                    String block;
                    if (blockMatcher.lookingAt() && (block = blockMatcher.group("block")) != null && !block.isBlank()) {
                        var itemMatcher = LIST_ITEM_LINE_PATTERN.matcher(block.stripTrailing().stripIndent());
                        var list = new ArrayList<String>();
                        while (itemMatcher.find()) {
                            list.add(itemMatcher.group("value"));
                        }
                        entries.accept(key, Collections.unmodifiableList(list));
                        end = blockMatcher.end();
                    } else {
                        blockMatcher = INDENTED_BLOCK_PATTERN.matcher(yaml).region(end, yaml.length());
                        if (blockMatcher.lookingAt() && (block = blockMatcher.group("block")) != null && !block.isBlank()) {
                            var mapValue = parseMap(block.stripTrailing().stripIndent());
                            entries.accept(key, mapValue);
                            end = blockMatcher.end();
                        }
                    }
                }
            } else {
                break;
            }
        }
    }
}
