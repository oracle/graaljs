# Graal JavaScript RegExp Support

Graal JavaScript employs two regular expression engines:
* TRegex, a new in-house engine
* [Joni](https://github.com/jruby/joni), an adopted port of an existing engine.

## Compatibility
For a high-level overview of the supported features, see [JavaScriptCompatibility.md](../user/JavaScriptCompatibility.md).

## Implementation

The two engines adopt different strategies when it comes to implementing regular expressions.
TRegex translates a regular expression into a finite state automaton, which can determine whether a match is found on a single pass over the input string: whenever several alternative ways to match the remaining input are admissible, TRegex considers all of them simultaneously.
Joni, on the other hand, implements a backtracking approach to regular expressions: whenever there are several possible ways to match the remaining input, Joni first tries to match each alternative on its own, switching to the next one when one fails to produce a correct match.

The downside of backtracking approaches is that certain regular expressions can take up to an exponential amount of time searching for a match (see https://swtch.com/~rsc/regexp/regexp1.html).
This problem can be eliminated by the use of automaton-based methods, which can match input strings in linear time.
However, this comes with two caveats: more time has to be spent in "compiling" the regular expression, i.e. translating it to a finite state automaton, and extensions of [formal regular expressions](https://en.wikipedia.org/wiki/Regular_expression#Formal_language_theory) are not always compatible with this strategy (one such common example are backreferences).

Therefore, Graal JavaScript adopts the following strategy when compiling a regular expression:

  1) Compile the regular expression using TRegex.
     If TRegex is unable to handle the expression for any of the reasons below, proceed to 2).

      a) If the regular expression uses a feature which is not supported by TRegex, compilation by TRegex is abandoned.
         A feature might be missing in TRegex either because it is not yet implemented or because it would not be compatible with TRegex's approach to translating regular expressions to automata.
      b) If the regular expression would be deemed too costly to translate into a finite state automaton, compilation by TRegex is abandoned.
         This is predicted by measuring and limiting the size of the regular expression's AST and all of the intermediate automaton representations.

  2) Compile the regular expression using Joni.
     If Joni is unable to handle the expression for any of the reasons below, proceed to 3).

      a) If the regular expression uses a feature which is not supported by JOni, compilation by JOni is abandoned.
         Since the port of JOni used in Graal JavaScript is older than TRegex, it lacks support for some ECMAScript RegExp features newer than ECMAScript 5.

  3) The regular expression is not supported by Graal JavaScript.
     If you run into this scenario, please [file an issue](https://github.com/graalvm/graaljs/issues/new)! :-)
     
     This likely means that the regular expression is using at the same time a feature which is not supported by TRegex as well as some other feature which is not supported by Joni.

