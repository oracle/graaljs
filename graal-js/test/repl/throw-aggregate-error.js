/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
throw new AggregateError([
    (function hoge(){ return new Error("lorem"); })(),
    (function fuga(){ return new Error("ipsum"); })(),
], "dolor");
