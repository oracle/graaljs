/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex;

/*
 * current status:
 *
 * truffle.regex.tregex can handle quantifiers, character classes, alternations,
 * positive look-aheads, and positive look-behinds of fixed length.
 * Counted repetitions are implemented by transforming them to alternations
 * (e.g. a{2,4} => aa|aaa|aaaa).
 *
 * basic structure of truffle.regex.tregex:
 *
 * tregex parses a regular expression using the custom RegexParser and transforms it to a non-deterministic finite
 * automaton (NFA) using the data structures found in tregex.nfa.
 *
 * The NFA is compiled to a DFA (deterministic finite automaton) during pattern matching. Each DFA stateSet is a
 * set of NFA states, which is stored as a BitSet where each bit corresponds to a slot in the NFA array.
 *
 */

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexEngine;
import com.oracle.truffle.regex.RegexNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.RegexSyntaxException;
import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.dead.DeadRegexNode;
import com.oracle.truffle.regex.literal.LiteralRegexEngine;
import com.oracle.truffle.regex.result.PreCalculatedResultFactory;
import com.oracle.truffle.regex.tregex.buffer.CompilationBuffer;
import com.oracle.truffle.regex.tregex.dfa.DFAGenerator;
import com.oracle.truffle.regex.tregex.nfa.NFA;
import com.oracle.truffle.regex.tregex.nfa.NFAGenerator;
import com.oracle.truffle.regex.tregex.nfa.NFATraceFinderGenerator;
import com.oracle.truffle.regex.tregex.nodes.TRegexDFAExecutorNode;
import com.oracle.truffle.regex.tregex.nodes.TRegexRootNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputIterator;
import com.oracle.truffle.regex.tregex.parser.RegexParser;
import com.oracle.truffle.regex.tregex.parser.RegexProperties;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.ASTLaTexExportVisitor;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.PreCalcResultVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;
import com.oracle.truffle.regex.tregex.util.NFAExport;

public final class TRegexEngine implements RegexEngine {

    private final DebugUtil.DebugLogger logBailout = new DebugUtil.DebugLogger("TRegex Bailout: ", DebugUtil.LOG_BAILOUT_MESSAGES);
    private final DebugUtil.DebugLogger logPhases = new DebugUtil.DebugLogger("TRegex Phase: ", DebugUtil.LOG_PHASES);
    private final DebugUtil.DebugLogger logSizes = new DebugUtil.DebugLogger("", DebugUtil.LOG_AUTOMATON_SIZES);
    private final DebugUtil.Timer timer = DebugUtil.LOG_PHASES ? new DebugUtil.Timer() : null;

    @CompilerDirectives.TruffleBoundary
    @Override
    public CompiledRegex compile(RegexSource source) throws RegexSyntaxException {
        try {
            CompilationBuffer compilationBuffer = new CompilationBuffer();
            // System.out.println(source);
            // System.out.println(new RegexUnifier(pattern, flags).getUnifiedPattern());
            phaseStart("Parser");
            RegexAST ast = new RegexParser(source).parse();
            phaseEnd("Parser");
            debugAST(ast);
            RegexProperties properties = ast.getProperties();
            if (properties.hasBackReferences() ||
                            properties.hasLargeCountedRepetitions() ||
                            properties.hasNegativeLookAheadAssertions() ||
                            properties.hasComplexLookBehindAssertions()) {
                // features not supported by DFA
                logBailout.log("unsupported feature: " + source);
                return null;
            }
            if (ast.getRoot().isDead()) {
                return new CompiledRegex(source, Truffle.getRuntime().createCallTarget(new DeadRegexNode(source.getPattern())));
            }
            RegexNode literal = LiteralRegexEngine.createNode(ast);
            if (literal != null) {
                logSizes.log(String.format("\"/%s/\", \"%s\", %d, %d, %d, %d, %d, \"literal\"", source.getPattern(), source.getFlags(), 0, 0, 0, 0, 0));
                return new CompiledRegex(source, Truffle.getRuntime().createCallTarget(literal));
            }
            PreCalculatedResultFactory[] preCalculatedResults = null;
            if (!(properties.hasAlternations() || properties.hasLookAroundAssertions())) {
                preCalculatedResults = new PreCalculatedResultFactory[]{PreCalcResultVisitor.createResultFactory(ast)};
            }
            phaseStart("NFA");
            NFA nfa = NFAGenerator.createNFA(ast, compilationBuffer);
            phaseEnd("NFA");
            debugNFA(nfa);
            NFA traceFinder = null;
            if (preCalculatedResults == null && TRegexOptions.TRegexEnableTraceFinder &&
                            (properties.hasCaptureGroups() || properties.hasLookAroundAssertions()) && !properties.hasLoops()) {
                try {
                    phaseStart("TraceFinder");
                    traceFinder = NFATraceFinderGenerator.generateTraceFinder(nfa);
                    preCalculatedResults = traceFinder.getPreCalculatedResults();
                    phaseEnd("TraceFinder");
                    debugTraceFinder(traceFinder);
                } catch (UnsupportedRegexException e) {
                    phaseEnd("TraceFinder Bailout");
                    logBailout.log("TraceFinder: " + e.getMessage());
                    // handle with capture group aware DFA, bailout will always happen before
                    // assigning preCalculatedResults
                }
            }
            final boolean createCaptureGroupTracker = (properties.hasCaptureGroups() || properties.hasLookAroundAssertions()) && preCalculatedResults == null;
            FrameDescriptor frameDescriptor = new FrameDescriptor();
            FrameSlot inputString = frameDescriptor.addFrameSlot("input", FrameSlotKind.Object);
            FrameSlot fromIndex = frameDescriptor.addFrameSlot("fromIndex", FrameSlotKind.Int);
            FrameSlot index = frameDescriptor.addFrameSlot("index", FrameSlotKind.Int);
            FrameSlot maxIndex = frameDescriptor.addFrameSlot("maxIndex", FrameSlotKind.Int);
            FrameSlot curMaxIndex = frameDescriptor.addFrameSlot("curMaxIndex", FrameSlotKind.Int);
            FrameSlot result = frameDescriptor.addFrameSlot("result", FrameSlotKind.Int);
            FrameSlot captureGroupResult = createCaptureGroupTracker ? frameDescriptor.addFrameSlot("captureGroupResult", FrameSlotKind.Object) : null;
            FrameSlot lastTransition = createCaptureGroupTracker ? frameDescriptor.addFrameSlot("lastTransition", FrameSlotKind.Int) : null;
            TRegexDFAExecutorNode captureGroupExecutor = null;
            phaseStart("Forward DFA");
            TRegexDFAExecutorNode executorNode = DFAGenerator.createForwardDFAExecutor(
                            nfa, new InputIterator(inputString, fromIndex, index, maxIndex, curMaxIndex, null, result, true), false, compilationBuffer);
            phaseEnd("Forward DFA");
            if (createCaptureGroupTracker) {
                phaseStart("CG DFA");
                captureGroupExecutor = DFAGenerator.createForwardDFAExecutor(
                                nfa, new InputIterator(inputString, fromIndex, index, maxIndex, curMaxIndex, lastTransition, captureGroupResult, true), true, compilationBuffer);
                phaseEnd("CG DFA");
            }
            phaseStart("Backward DFA");
            TRegexDFAExecutorNode executorNodeB = DFAGenerator.createBackwardDFAExecutor(
                            (preCalculatedResults != null && preCalculatedResults.length > 1) ? traceFinder : nfa,
                            new InputIterator(inputString, fromIndex, index, maxIndex, curMaxIndex, null, result, false), compilationBuffer);
            phaseEnd("Backward DFA");
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new TRegexRootNode.TRegexForwardSearchRootNode(
                            frameDescriptor,
                            inputString,
                            fromIndex,
                            index,
                            maxIndex,
                            result,
                            captureGroupResult,
                            source.getPattern(),
                            source.getFlags().isUnicode(),
                            preCalculatedResults,
                            executorNode,
                            executorNodeB,
                            captureGroupExecutor));
            if (DebugUtil.LOG_AUTOMATON_SIZES) {
                logAutomatonSizes(source, ast, nfa, traceFinder, captureGroupExecutor, executorNode, executorNodeB);
                logAutomatonSizesCSV(source, ast, nfa, traceFinder, captureGroupExecutor, executorNode, executorNodeB);
            }
            return new CompiledRegex(source, callTarget);
        } catch (UnsupportedRegexException e) {
            phaseEnd("DFA Bailout");
            logBailout.log(e.getMessage() + ": " + source);
            return null;
        }
    }

    private static void debugAST(RegexAST ast) {
        if (DebugUtil.DEBUG) {
            System.out.println(ast);
            ASTLaTexExportVisitor.exportLatex(ast, "./ast.tex", ASTLaTexExportVisitor.DrawPointers.LOOKBEHIND_ENTRIES);
            System.out.println(ast.getWrappedRoot().toTable());
        }
    }

    private static void debugNFA(NFA nfa) {
        if (DebugUtil.DEBUG) {
            NFAExport.exportDot(nfa, "./nfa.gv", true);
            NFAExport.exportLaTex(nfa, "./nfa.tex", true);
            NFAExport.exportDotReverse(nfa, "./nfa_reverse.gv", true);
        }
    }

    private static void debugTraceFinder(NFA traceFinder) {
        if (DebugUtil.DEBUG) {
            NFAExport.exportDotReverse(traceFinder, "./trace_finder.gv", true);
            NFAExport.exportDotReverse(traceFinder, "./trace_finder.gv", true);
            for (int i = 0; i < traceFinder.getPreCalculatedResults().length; i++) {
                System.out.println(i + ": " + traceFinder.getPreCalculatedResults()[i].toTable());
            }
        }
    }

    private void phaseStart(String phase) {
        if (DebugUtil.LOG_PHASES) {
            logPhases.log(phase + " Start");
            timer.start();
        }
    }

    private void phaseEnd(String phase) {
        if (DebugUtil.LOG_PHASES) {
            logPhases.log(phase + " End, " + timer.elapsedToString());
        }
    }

    private void logAutomatonSizes(RegexSource source, RegexAST ast, NFA nfa, NFA traceFinder,
                    TRegexDFAExecutorNode captureGroupExecutor, TRegexDFAExecutorNode executorNode, TRegexDFAExecutorNode executorNodeB) {
        logSizes.log(new DebugUtil.Table("AutomatonSizes",
                        new DebugUtil.Value("pattern", source.getPattern()),
                        new DebugUtil.Value("flags", source.getFlags()),
                        new DebugUtil.Value("ASTNodes", ast.getNumberOfNodes()),
                        new DebugUtil.Value("NFAStates", nfa.getStates().length),
                        new DebugUtil.Value("DFAStatesFwd", executorNode.getNumberOfStates()),
                        new DebugUtil.Value("DFAStatesBck", traceFinder == null ? executorNodeB.getNumberOfStates() : 0),
                        new DebugUtil.Value("TraceFinderStates", traceFinder == null ? 0 : executorNodeB.getNumberOfStates()),
                        new DebugUtil.Value("CGDFAStates", captureGroupExecutor == null ? 0 : captureGroupExecutor.getNumberOfStates())).toString());
    }

    private void logAutomatonSizesCSV(RegexSource source, RegexAST ast, NFA nfa, NFA traceFinder,
                    TRegexDFAExecutorNode captureGroupExecutor, TRegexDFAExecutorNode executorNode, TRegexDFAExecutorNode executorNodeB) {
        logSizes.log(String.format("\"/%s/\", \"%s\", %d, %d, %d, %d, %d, %d, \"dfa\"",
                        source.getPattern(), source.getFlags(),
                        ast.getNumberOfNodes(),
                        nfa.getStates().length,
                        executorNode.getNumberOfStates(),
                        traceFinder == null ? executorNodeB.getNumberOfStates() : 0,
                        traceFinder == null ? 0 : executorNodeB.getNumberOfStates(),
                        captureGroupExecutor == null ? 0 : captureGroupExecutor.getNumberOfStates()));
    }
}
