This document serves as migration guide for code previously targeted to the Rhino engine.
See the [JavaInterop.md](JavaInterop.md) for an overview of supported Java interoperability features.

## Overview
Both Rhino and Graal JavaScript support a similar set of syntax and semantics for Java interoperability.
The most important differences relevant for migration are listed here.

### `Java.type(typename)` instead of `java.a.b.c.typename`
Graal JavaScript does not put available Java classes in the JavaScript scope.
You have to explicitly load the classes using `Java.type(typename)`.
Graal JavaScript supports the `Packages` global object, but loading the classes explicitly is still encouraged.

### Console output of Java classes and Java objects
Graal JavaScript provides a `print` builtin function.
It tries to special-case its behavior on Java classes and Java objects to provide the most useful output.

Note that Graal JavaScript also provides a `console.log` function.
This is an alias for `print` in pure JavaScript mode, but uses an implementation provided by Node.js when in Node mode.
Behavior around interop objects differs for `console.log` in Node mode as it does not implement special treatment for such objects.

### JavaScript vs Java Strings
Graal JavaScript uses Java strings internally to represent JavaScript strings.
This makes it impossible to differentiate whether a specific string was created by JavaScript or by Java code.
In Graal JavaScript, the JavaScript properties take precedence over Java fields or methods.
For instance, you can query the `length` property (of JavaScript) but you cannot call the `length` function (of Java) on JavaScript strings - `length` behaves like a data property, not like a function.

### JavaImporter
The `JavaImporter` is not supported by Graal JavaScript.

