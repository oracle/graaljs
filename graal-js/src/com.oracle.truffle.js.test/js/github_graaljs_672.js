/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * [Bug]: Returning void await this in async function with parameter.
 * https://github.com/oracle/graaljs/issues/672
 *
 * @option unhandled-rejections=throw
 */

load('assert.js');

let f = async (x) => void await this;
f();
