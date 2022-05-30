/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that various invalid CoverInitializedName appearances in object literals result in a SyntaxError.
 */

function assertSyntaxError(code) {
    try {
        eval(code);
        throw new Error(`SyntaxError expected but not thrown:\n${code}`);
    } catch (e) {
        if (!(e instanceof SyntaxError)) {
            throw e;
        }
    }
}

// MemberExpression followed by [ Expression ], . IdentifierName, or TemplateLiteral.
assertSyntaxError('({x = init}.x   )');
assertSyntaxError('({x = init}["x"])');
assertSyntaxError('({x = init}`etc`)');
assertSyntaxError('({x = init}.x    = {})');
assertSyntaxError('({x = init}["x"] = {})');

// CallExpression (LeftHandSideExpression)
assertSyntaxError('({x = init}())');
assertSyntaxError('({x = init}() = {})');
assertSyntaxError('({x = init}().x = {})');
assertSyntaxError('({x = init}()["x"] = {})');

// OptionalExpression
assertSyntaxError('({x = init}?.x    )');
assertSyntaxError('({x = init}?.["x"])');
assertSyntaxError('({x = init}?.("x"))');
