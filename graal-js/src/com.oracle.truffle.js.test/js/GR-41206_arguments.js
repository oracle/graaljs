/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify correct parsing of 'arguments' when there is a parameter scope.
 */

load("assert.js");

const global_arguments = {};
globalThis.arguments = global_arguments;
assertSame(global_arguments, arguments);

function test(fn, expectedError) {
    assertSame("function", typeof fn);
    if (expectedError) {
        assertThrows(fn, expectedError);
    } else {
        return fn();
    }
}

function assertArgumentsObject(arg) {
    assertSame('object', typeof arg);
    assertSame('[object Arguments]', Object.prototype.toString.call(arg));
}

function non_simple_parameters_non_lexical_arguments1(_ = 0) {
    assertArgumentsObject(arguments);
    var arguments;
    return [arguments];
}
test(non_simple_parameters_non_lexical_arguments1);

function non_simple_parameters_non_lexical_arguments2(_ = arguments) {
    assertArgumentsObject(_);
    assertArgumentsObject(arguments);
    var arguments;
    return [_, arguments];
}
test(non_simple_parameters_non_lexical_arguments2);

function non_simple_parameters_arguments_function1(_ = 0) {
    assertSame('function', typeof arguments);
    function arguments(){};
    return [arguments];
}
test(non_simple_parameters_arguments_function1);

function non_simple_parameters_arguments_function2(_ = arguments) {
    assertSame('function', typeof arguments);
    function arguments(){};
    return [_, arguments];
}
test(non_simple_parameters_arguments_function2);

let eval_var_arguments1 = function(p = eval("var arguments = 'param'"), q = () => arguments) {
    var arguments = "local";
    return [arguments, q()];
};
test(eval_var_arguments1, SyntaxError); // redeclaration of let arguments; allowed by V8

let arrow_eval_var_arguments1 = (p = eval("var arguments = 'param'"), q = () => arguments) => {
    var arguments = "local";
    return [arguments, q()];
};
test(arrow_eval_var_arguments1);

let eval_var_arguments2 = function(p = eval("var arguments = 'param'"), q = () => arguments) {
    var arguments;
    return [arguments, q()];
};
test(eval_var_arguments2, SyntaxError); // redeclaration of let arguments; allowed by V8

let arrow_eval_var_arguments2 = (p = eval("var arguments = 'param'"), q = () => arguments) => {
    var arguments;
    return [arguments, q()];
};
test(arrow_eval_var_arguments2);

let var_arguments1 = function(p = arguments, q = () => arguments) {
    var arguments;
    assertArgumentsObject(arguments);
    arguments = "var_arguments";
    assertSame("var_arguments", arguments);
    assertArgumentsObject(p);
    assertArgumentsObject(q());
    return [arguments, q()];
};
test(var_arguments1);

let arrow_var_arguments1 = (p = arguments, q = () => arguments) => {
    var arguments;
    var q;
    assertSame(global_arguments, p);
    assertSame(undefined, arguments);
    arguments = "var_arguments";
    assertSame("var_arguments", arguments);
    assertSame(global_arguments, q());
    return [arguments, q()];
};
test(arrow_var_arguments1);

let eval_var_arguments3 = function f(x = eval("var arguments; arguments;")){return x};
test(eval_var_arguments3, SyntaxError);

let arrow_eval_var_arguments3 = (x = eval("var arguments; arguments;")) => x;
test(arrow_eval_var_arguments3);

function eval_var_arguments4(p = arguments, q = eval("var arguments; arguments;")) { return [p, q];}
test(eval_var_arguments4, SyntaxError);

let arrow_eval_var_arguments4 = (p = arguments, q = eval("var arguments; arguments;")) => [p, q];
test(arrow_eval_var_arguments4);

function eval_var_arguments5(arguments = eval("var arguments;")) { return [arguments];}
test(eval_var_arguments5, SyntaxError);

function eval_var_arguments6(arguments = eval("var arguments; arguments;")) { return [arguments];}
test(eval_var_arguments6, SyntaxError);

// https://github.com/tc39/test262/issues/2478
function eval_var_arguments7(p = eval("var arguments")) {}
test(eval_var_arguments7, SyntaxError);

function eval_var_arguments8(p = eval("var arguments = 'param'")) {
    function arguments() {}
    return arguments;
}
test(eval_var_arguments8, SyntaxError);

function var_fun_arguments1(_ = 0) {
    var arguments;
    assertSame("function", typeof arguments);
    function arguments() {}
    assertSame("function", typeof arguments);
    return [arguments];
}
test(var_fun_arguments1);

function var_fun_arguments2(_ = 0) {
    var arguments;
    assertSame("function", typeof arguments);
    function arguments() {}
    assertSame("function", typeof arguments);
    return [arguments];
}
test(var_fun_arguments2);

function var_fun_arguments3(_ = arguments) {
    assertArgumentsObject(_);
    function arguments() {}
    assertSame("function", typeof arguments);
    var arguments;
    assertSame("function", typeof arguments);
    assertArgumentsObject(_);
    return [arguments];
}
test(var_fun_arguments3);

function var_fun_arguments4(_ = arguments) {
    assertArgumentsObject(_);
    function arguments() {}
    assertSame("function", typeof arguments);
    var arguments;
    assertSame("function", typeof arguments);
    assertArgumentsObject(_);
    return [arguments];
}
test(var_fun_arguments4);

function simple_parameters_and_lexical_arguments_uninitialized(_) {
    return typeof arguments;
    let arguments;
}
test(simple_parameters_and_lexical_arguments_uninitialized, ReferenceError);

function non_simple_parameters_and_lexical_arguments_uninitialized(_ = arguments) {
    return typeof arguments;
    let arguments;
}
test(non_simple_parameters_and_lexical_arguments_uninitialized, ReferenceError);

function lexical_arguments_and_arguments_unused(_) {
    let arguments;
    assertSame('undefined', typeof arguments)
    return arguments;
}
test(lexical_arguments_and_arguments_unused);

function lexical_arguments_and_arguments_used(_ = arguments) {
    let arguments;
    assertArgumentsObject(_);
    assertSame('undefined', typeof arguments)
    return arguments;
}
test(lexical_arguments_and_arguments_used);

function hoisted_arguments_function1() {
    assertArgumentsObject(arguments);
    {
        // overwrites arguments, but only when this block is entered.
        function arguments(){};
    }
    assertSame("function", typeof arguments);
    return [arguments];
}
test(hoisted_arguments_function1);

function hoisted_arguments_function2() {
    var arguments;
    assertArgumentsObject(arguments);
    {
        function arguments(){};
    }
    assertSame("function", typeof arguments);
    return [arguments];
}
test(hoisted_arguments_function2);

function hoisted_arguments_function3(q = () => arguments) {
    assertArgumentsObject(arguments);
    {
        function arguments(){};
    }
    assertSame("function", typeof arguments);
    assertArgumentsObject(q());
    return [arguments];
}
test(hoisted_arguments_function3);

function hoisted_arguments_function4(q = () => arguments) {
    var arguments;
    assertArgumentsObject(arguments);
    {
        function arguments(){};
    }
    assertSame("function", typeof arguments);
    assertArgumentsObject(q());
    return [arguments];
}
test(hoisted_arguments_function4);

function non_hoisted_arguments_function(arguments) {
    assertSame("undefined", typeof arguments);
    {
        function arguments(){};
    }
    assertSame("undefined", typeof arguments);
    return [arguments];
}
test(non_hoisted_arguments_function);

function no_vardeclared_arguments(p = arguments, q = () => arguments) {
    assertArgumentsObject(arguments);
    assertArgumentsObject(q());
    arguments = "mutated";
    assertArgumentsObject(p);
    assertSame("mutated", q());
    return [arguments, q()];
}
test(no_vardeclared_arguments);

function arguments_function_only() {
    // no arguments object needed
    assertSame('function', typeof arguments);
    return typeof arguments;
    function arguments() {}
}
test(arguments_function_only);

function arguments_function_only2() {
    // no arguments object needed
    var arguments;
    assertSame('function', typeof arguments);
    return typeof arguments;
    function arguments() {}
}
test(arguments_function_only2);

function arguments_function_only3() {
    // no arguments object needed
    function arguments() {}
    var arguments;
    assertSame('function', typeof arguments);
    return typeof arguments;
}
test(arguments_function_only3);

function function_self_ref(_ = function_self_ref) {
    var function_self_ref;
    assertSame('function', typeof _);
    assertSame('undefined', typeof function_self_ref);
    return [_, function_self_ref];
}
test(function_self_ref);

function function_self_ref_hoisted(_ = function_self_ref_hoisted) {
    assertSame('function', typeof _);
    assertSame('undefined', typeof function_self_ref_hoisted);
    {
        function function_self_ref_hoisted(){}
    }
    assertSame('function', typeof function_self_ref_hoisted);
    return [_, function_self_ref_hoisted];
}
test(function_self_ref_hoisted);
