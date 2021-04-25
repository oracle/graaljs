/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that parseToJSON() (used by parse()) produces a valid JSON.
 * This test was motivated by https://github.com/oracle/graaljs/issues/416
 * 
 * @option nashorn-compat
 */

load("nashorn:parser.js");

// checking just that it does not throw
// i.e. that some valid JSON is produced by parseToJSON()
parse("var [x] = [42];");
parse("try {} catch {};");
parse("1e310");
parse("(class {})");
parse("({x, ...y})");
parse("[...[]]");
parse("(function() {})(...x)");
parse("(function*() { yield* []})");
parse("`${42}`");
parse("String.raw`../${42}\..`");
