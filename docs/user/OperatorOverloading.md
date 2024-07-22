---
layout: docs
toc_group: js
link_title: Operator Overloading
permalink: /reference-manual/js/OperatorOverloading/
---

# Operator Overloading

GraalJS provides an early implementation of the ECMAScript [operator overloading proposal](https://github.com/tc39/proposal-operator-overloading).
This lets you overload the behavior of JavaScript's operators on your JavaScript classes.

If you want to experiment with this feature, enable it.
Since both the proposal and the GraalJS implementation of it are in early stages, you need to pass the `--experimental-options` option:
```shell
js --experimental-options --js.operator-overloading
```

After setting the option, you will see a new builtin in the global namespace, the `Operators` function.
You can call this function, passing it a JavaScript object as an argument.
The object should have a property for every operator you wish to overload, with the key being the name of the operator and the value being a function that implements it.
The return value of the `Operators` function is a constructor that you can then subclass when defining your type.
By subclassing this constructor, you get a class whose objects inherit the overloaded operator behavior that you defined in your argument to the `Operators` function.

## Basic Example

Look at an example from the original proposal featuring vectors:
```java
const VectorOps = Operators({
  "+"(a, b) {
    return new Vector(a.contents.map((elt, i) => elt + b.contents[i]));
  },
  "=="(a, b) {
    return a.contents.length === b.contents.length &&
           a.contents.every((elt, i) => elt == b.contents[i]);
  },
});

class Vector extends VectorOps {
  contents;
  constructor(contents) {
    super();
    this.contents = contents;
  }
}
```

Here two operators, `+` and `==`, are overloaded.
Calling the `Operators` function with the table of overloaded operators yields the `VectorOps` class.
Then the `Vector` class is defined as a subclass of `VectorOps`.

If you create instances of `Vector`, you can observe that they follow the overloaded operator definitions:
```
> new Vector([1, 2, 3]) + new Vector([4, 5, 6]) == new Vector([5, 7, 9])
true
```

## Example with Mixed Types

It is also possible to overload operators between values of different types, allowing, for example, multiplication of vectors by numbers:
```java
const VectorOps = Operators({
    "+"(a, b) {
        return new Vector(a.contents.map((elt, i) => elt + b.contents[i]));
    },
    "=="(a, b) {
        return a.contents.length === b.contents.length &&
            a.contents.every((elt, i) => elt == b.contents[i]);
    },
}, {
    left: Number,
    "*"(a, b) {
        return new Vector(b.contents.map(elt => elt * a));
    }
});

class Vector extends VectorOps {
    contents;
    constructor(contents) {
        super();
        this.contents = contents;
    }
}
```

To define mixed-type operators, pass additional objects to the `Operators` function.
These extra tables should each have either a `left` property or a `right` property, depending on whether you overload the behavior of operators with some other type on the left or on the right side of the operator.
In the example, the `*` operator is overloaded for cases when there is a `Number` on the left and the type, `Vector`, on the right.
Each extra table can have either a `left` property or a `right` property and then any number of operator overloads that will apply to that particular case.

Running this example you see:
```
> 2 * new Vector([1, 2, 3]) == new Vector([2, 4, 6])
true
```

## Usage Documentation

The function `Operators(table, extraTables...)` returns a class with overloaded operators.
Users should define their own class which extends that class.

The `table` argument must be an object with one property for every overloaded operator.
The property key must be the name of the operator.
These are the names of operators which can be overloaded:
  * binary operators: `"+"`, `"-"`, `"*"`, `"/"`, `"%"`, `"**"`, `"&"`, `"^"`, `"|"`, `"<<"`, `">>"`, `">>>"`, `"=="`, `"<"`
  * unary operators: `"pos"`, `"neg"`, `"++"`, `"--"`, `"~"`

The `"pos"` and `"neg"` operator names correspond to unary `+` and unary `-`, respectively.
Overloading `"++"` works both for pre-increments `++x` and post-increments `x++`, the same goes for `"--"`.
The overload for `"=="` is used both for equality `x == y` and inequality `x != y` tests.
Similarly, the overload for `"<"` is used for all comparison operators (`x < y`, `x <= y`, `x > y`, `x >= y`) by swapping the arguments and/or negating the result.

The value assigned to an operator name must be a function of two arguments in the case of binary operators or a function of one argument in the case of unary operators.

The `table` argument can also have an `open` property.
If so, the value of that property must be an array of operator names.
These are the operators that future classes will be able to overload on this type (for example, a `Vector` type might declare `"*"` to be open so that later a `Matrix` type might overload the operations `Vector * Matrix` and `Matrix * Vector`).
If the `open` property is missing, all operators are considered to be open for future overloading with other types.

Following the first argument `table` are optional arguments `extraTables`.
Each of these must also be an object.
Each extra table must have either a `left` property or a `right` property, not both.
The value of that property must be one of the following JavaScript constructors:
  * `Number`
  * `BigInt`
  * `String`
  * any class with overloaded operators (i.e. extended from a constructor returned by `Operators`)

The other properties of the extra table should be operator overloads as in the first `table` argument (operator name as key, function implementing the operator as value).

These extra tables define the behavior of operators when one of the operand types is of a type other than the one being defined.
If the extra table has a `left` property, its operator definitions will apply to cases when the left operand is of the type named by the `left` property and the right operand is of the type whose operators are being defined.
Similarly for the `right` property, if the extra table has a `right` property, the table's operator definitions will apply when the right operand has the named type and the left operand has the type whose operators are being defined.

Note that you are free to overload any of the binary operators between your custom type and the JavaScript numeric types `Number` and `BigInt`.
However, the only operators you are allowed to overload between your custom type and the `String` type are `"=="` and `"<"`.

The `Operators` function returns a constructor that you will usually want to extend in your own class.
Instances of that class will respect your overloaded operator definitions.
Whenever you use an operator on an object with overloaded operators, the following happens:
  1) Every operand that does *not* have overloaded operators is coerced to a primitive.
  2) If there is an applicable overload for this pairing of operands, it is called. Otherwise, a `TypeError` is thrown.

Notably, your objects with overloaded operators will not be coerced to primitives when applying operators and you can get `TypeError`s when applying undefined operators to them.
There are two exceptions to this:
  1) If you are using the `+` operator and one of the arguments is a String (or an object without overloaded operators that coerces to a String via `ToPrimitive`), then the result will be a concatenation of the `ToString` values of the two operands.
  2) If you are using the `==` operator and there is no applicable overload found, the two operands are assumed to be different (`x == y` will return `false` and `x != y` will return `true`).

## Differences from the Proposal

There a few differences between the proposal (as defined by its specification and prototype implementation) and GraalJS implementation:
  * You do not have to use the `with operators from` construction to enable the use of overloaded operators. When you overload operators for a class, those operators can then be used anywhere without using `with operators from`. Furthermore, the parser will not accept the `with operators from` clause as valid JavaScript.
  * You cannot use decorators to define overloaded operators. At the time of implementing this proposal, GraalJS does not support decorators (these are still an in-progress proposal).
  * You cannot overload the `"[]"` and `"[]="` operators for reading and writing integer-indexed elements. These two operators require more complex treatment and are not currently supported.

### Related Documentation

* [ECMAScript operator overloading proposal](https://github.com/tc39/proposal-operator-overloading)