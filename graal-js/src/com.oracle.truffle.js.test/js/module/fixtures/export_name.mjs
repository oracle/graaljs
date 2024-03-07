/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

const one = 1;
const two = 2;
const three = 3;
const four = 4;
export {
    one,
    two as onePlusOne,
    three as 'onePlusTwo',
    four as "onePlusThree",
};
