/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.tregex.TRegexOptions;
import com.oracle.truffle.regex.tregex.automaton.StateIndex;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.nfa.ASTNodeSet;
import com.oracle.truffle.regex.tregex.parser.CodePointSet;
import com.oracle.truffle.regex.tregex.parser.Counter;
import com.oracle.truffle.regex.tregex.parser.RegexProperties;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.ASTDebugDumpVisitor;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.List;

public class RegexAST implements StateIndex<RegexASTNode> {

    /**
     * Original pattern as seen by the parser.
     */
    private final RegexSource source;
    private final Counter.ThresholdCounter nodeCount = new Counter.ThresholdCounter(TRegexOptions.TRegexMaxParseTreeSize, "parse tree explosion");
    private final Counter.ThresholdCounter groupCount = new Counter.ThresholdCounter(TRegexOptions.TRegexMaxNumberOfCaptureGroups, "too many capture groups");
    private final RegexProperties properties = new RegexProperties();
    private RegexASTNode[] nodes;
    /**
     * AST as parsed from the expression.
     */
    private Group root;
    /**
     * Possibly wrapped root for NFA generation (see {@link #createPrefix()}).
     */
    private Group wrappedRoot;
    private final List<LookBehindAssertion> lookBehinds = new ArrayList<>();
    private final List<MatchFound> endPoints = new ArrayList<>();
    private final List<PositionAssertion> reachableCarets = new ArrayList<>();
    private final List<PositionAssertion> reachableDollars = new ArrayList<>();
    private ASTNodeSet<PositionAssertion> nfaAnchoredInitialStates;
    private ASTNodeSet<RegexASTNode> hardPrefixNodes;

    public RegexAST(RegexSource source) {
        this.source = source;
    }

    public RegexSource getSource() {
        return source;
    }

    public Group getRoot() {
        return root;
    }

    public void setRoot(Group root) {
        this.root = root;
    }

    public Group getWrappedRoot() {
        return wrappedRoot;
    }

    public boolean rootIsWrapped() {
        return wrappedRoot != null && root != wrappedRoot;
    }

    public Counter.ThresholdCounter getNodeCount() {
        return nodeCount;
    }

    public int getNumberOfNodes() {
        return nodeCount.getCount();
    }

    public Counter.ThresholdCounter getGroupCount() {
        return groupCount;
    }

    public int getNumberOfCaptureGroups() {
        return groupCount.getCount();
    }

    public RegexProperties getProperties() {
        return properties;
    }

    @Override
    public int getNumberOfStates() {
        return nodes.length;
    }

    @Override
    public RegexASTNode getState(int id) {
        return nodes[id];
    }

    public void setIndex(RegexASTNode[] index) {
        this.nodes = index;
    }

    /**
     * @return length of prefix possibly generated by {@link #createPrefix()}.
     */
    public int getWrappedPrefixLength() {
        if (rootIsWrapped()) {
            // The single alternative in the wrappedRoot is composed of N non-optional prefix
            // matchers, 1 group of optional matchers, 1 original root and 1 MatchFound node. By
            // taking size() - 3, we get the number of non-optional prefix matchers.
            return wrappedRoot.getAlternatives().get(0).getTerms().size() - 3;
        }
        return 0;
    }

    /**
     * @return first element of sequence of optional any-char matchers possibly generated by
     *         {@link #createPrefix()}.
     */
    public RegexASTNode getEntryAfterPrefix() {
        if (rootIsWrapped()) {
            return wrappedRoot.getAlternatives().get(0).getTerms().get(getWrappedPrefixLength());
        }
        return wrappedRoot;
    }

    public List<LookBehindAssertion> getLookBehinds() {
        return lookBehinds;
    }

    public List<MatchFound> getEndPoints() {
        return endPoints;
    }

    public List<PositionAssertion> getReachableCarets() {
        return reachableCarets;
    }

    public List<PositionAssertion> getReachableDollars() {
        return reachableDollars;
    }

    public ASTNodeSet<PositionAssertion> getNfaAnchoredInitialStates() {
        return nfaAnchoredInitialStates;
    }

    public ASTNodeSet<RegexASTNode> getHardPrefixNodes() {
        return hardPrefixNodes;
    }

    public RegexASTRootNode createRootNode() {
        final RegexASTRootNode node = new RegexASTRootNode();
        createEndPoint(node);
        return node;
    }

    public BackReference createBackReference(int groupNumber) {
        return register(new BackReference(groupNumber));
    }

    public CharacterClass createCharacterClass(CodePointSet codePointSet) {
        return createCharacterClass(MatcherBuilder.create(codePointSet));
    }

    public CharacterClass createCharacterClass(MatcherBuilder matcherBuilder) {
        return register(new CharacterClass(matcherBuilder));
    }

    public Group createGroup() {
        return register(new Group());
    }

    public Group createCaptureGroup(int groupNumber) {
        return register(new Group(groupNumber));
    }

    public LookAheadAssertion createLookAheadAssertion(boolean negated) {
        final LookAheadAssertion assertion = new LookAheadAssertion(negated);
        createEndPoint(assertion);
        return register(assertion);
    }

    public LookBehindAssertion createLookBehindAssertion() {
        final LookBehindAssertion assertion = new LookBehindAssertion();
        createEndPoint(assertion);
        return register(assertion);
    }

    public void createEndPoint(RegexASTSubtreeRootNode assertion) {
        nodeCount.inc();
        MatchFound end = new MatchFound();
        endPoints.add(end);
        assertion.setMatchFound(end);
    }

    public PositionAssertion createPositionAssertion(PositionAssertion.Type type) {
        return register(new PositionAssertion(type));
    }

    public Sequence createSequence() {
        return register(new Sequence());
    }

    public BackReference register(BackReference backReference) {
        nodeCount.inc();
        properties.setBackReferences();
        return backReference;
    }

    public CharacterClass register(CharacterClass characterClass) {
        nodeCount.inc();
        if (!characterClass.getMatcherBuilder().matchesSingleChar()) {
            properties.setCharClasses();
        }
        return characterClass;
    }

    public Group register(Group group) {
        nodeCount.inc();
        if (group.isCapturing() && group.getGroupNumber() != 0) {
            properties.setCaptureGroups();
        }
        return group;
    }

    public LookAheadAssertion register(LookAheadAssertion lookAheadAssertion) {
        nodeCount.inc();
        properties.setLookAheadAssertions();
        if (lookAheadAssertion.isNegated()) {
            properties.setNegativeLookAheadAssertions();
        }
        return lookAheadAssertion;
    }

    public LookBehindAssertion register(LookBehindAssertion lookBehindAssertion) {
        nodeCount.inc();
        properties.setLookBehindAssertions();
        lookBehinds.add(lookBehindAssertion);
        return lookBehindAssertion;
    }

    public PositionAssertion register(PositionAssertion positionAssertion) {
        nodeCount.inc();
        switch (positionAssertion.type) {
            case CARET:
                reachableCarets.add(positionAssertion);
                break;
            case DOLLAR:
                reachableDollars.add(positionAssertion);
                break;
        }
        return positionAssertion;
    }

    public Sequence register(Sequence sequence) {
        nodeCount.inc();
        return sequence;
    }

    public void removeUnreachablePositionAssertions() {
        reachableCarets.removeIf(RegexASTNode::isDead);
        reachableDollars.removeIf(RegexASTNode::isDead);
    }

    public boolean isNFAInitialState(RegexASTNode node) {
        return node.getId() >= 1 && node.getId() <= getWrappedPrefixLength() * 2 + 2;
    }

    private void createNFAInitialStates() {
        if (nfaAnchoredInitialStates != null) {
            return;
        }
        hardPrefixNodes = new ASTNodeSet<>(this);
        nfaAnchoredInitialStates = new ASTNodeSet<>(this);
        int nextID = 1;
        MatchFound mf = new MatchFound();
        initNodeId(mf, nextID++);
        mf.setNext(getEntryAfterPrefix());
        PositionAssertion pos = new PositionAssertion(PositionAssertion.Type.CARET);
        initNodeId(pos, nextID++);
        nfaAnchoredInitialStates.add(pos);
        pos.setNext(getEntryAfterPrefix());
        for (int i = getWrappedPrefixLength() - 1; i >= 0; i--) {
            RegexASTNode prefixNode = getWrappedRoot().getAlternatives().get(0).getTerms().get(i);
            hardPrefixNodes.add(prefixNode);
            mf = new MatchFound();
            initNodeId(mf, nextID++);
            mf.setNext(prefixNode);
            pos = new PositionAssertion(PositionAssertion.Type.CARET);
            initNodeId(pos, nextID++);
            nfaAnchoredInitialStates.add(pos);
            pos.setNext(prefixNode);
        }
    }

    public MatchFound getNFAUnAnchoredInitialState(int prefixOffset) {
        createNFAInitialStates();
        assert nodes[prefixOffset * 2 + 1] != null;
        return (MatchFound) nodes[prefixOffset * 2 + 1];
    }

    public PositionAssertion getNFAAnchoredInitialState(int prefixOffset) {
        createNFAInitialStates();
        assert nodes[prefixOffset * 2 + 2] != null;
        return (PositionAssertion) nodes[prefixOffset * 2 + 2];
    }

    /**
     * Inserts a prefix of matchers that match any characters at the beginning of the AST. The
     * length of the prefix is determined by the look-behind assertions present in the regex. Any
     * necessary context that could be matched by the look-behind assertions but not by the original
     * regex can be captured by the prefix. Exemplary prefix: {@code
     * regex: /(?<=ab)/
     *  -> prefix length: 2
     *  -> result: /(?:[_any_][_any_](?:|[_any_](?:|[_any_])))(?<=ab)/
     *      -> the non-optional [_any_] - matchers will be used if fromIndex > 0, the optional matchers
     *         will always be used
     * }
     */
    public void createPrefix() {
        if (root.startsWithCaret()) {
            wrappedRoot = root;
            return;
        }
        int prefixLength = 0;
        for (LookBehindAssertion lb : lookBehinds) {
            int minPath = lb.getMinPath();
            RegexASTSubtreeRootNode laParent = lb.getSubTreeParent();
            while (!(laParent instanceof RegexASTRootNode)) {
                if (laParent instanceof LookBehindAssertion) {
                    throw new UnsupportedRegexException("nested look-behind assertions");
                }
                minPath += laParent.getMinPath();
                laParent = laParent.getSubTreeParent();
            }
            prefixLength = Math.max(prefixLength, lb.getLength() - minPath);
        }
        if (prefixLength == 0) {
            wrappedRoot = root;
            return;
        }
        final Group wrapRoot = createGroup();
        wrapRoot.setPrefix();
        final Sequence wrapRootSeq = createSequence();
        wrapRoot.add(wrapRootSeq);
        wrapRootSeq.setPrefix();
        // create non-optional matchers ([_any_][_any_]...)
        for (int i = 0; i < prefixLength; i++) {
            wrapRootSeq.add(createPrefixAnyMatcher());
        }
        Group prevOpt = null;
        // create optional matchers ((?:|[_any_](?:|[_any_]))...)
        for (int i = 0; i < prefixLength; i++) {
            Group opt = createGroup();
            opt.setPrefix();
            opt.add(createSequence());
            opt.add(createSequence());
            opt.getAlternatives().get(0).setPrefix();
            opt.getAlternatives().get(1).setPrefix();
            opt.getAlternatives().get(1).add(createPrefixAnyMatcher());
            if (prevOpt != null) {
                opt.getAlternatives().get(1).add(prevOpt);
            }
            prevOpt = opt;
        }
        root.getSubTreeParent().setGroup(wrapRoot);
        final MatchFound matchFound = root.getSubTreeParent().getMatchFound();
        wrapRootSeq.add(prevOpt);
        wrapRootSeq.add(root);
        wrapRootSeq.add(matchFound);
        wrappedRoot = wrapRoot;
    }

    /**
     * Creates a {@link CharacterClass} node which matches any character and whose 'prefix' flag is
     * set to true.
     */
    private CharacterClass createPrefixAnyMatcher() {
        final CharacterClass anyMatcher = createCharacterClass(MatcherBuilder.createFull());
        anyMatcher.setPrefix();
        return anyMatcher;
    }

    public CharacterClass createLoopBackMatcher() {
        CharacterClass loopBackCC = new CharacterClass(MatcherBuilder.createFull());
        initNodeId(loopBackCC, nodes.length - 1);
        return loopBackCC;
    }

    private void addToIndex(RegexASTNode node) {
        assert node.getId() >= 0;
        assert node.getId() < nodes.length;
        assert nodes[node.getId()] == null;
        nodes[node.getId()] = node;
    }

    private void initNodeId(RegexASTNode node, int id) {
        node.setId(id);
        addToIndex(node);
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("RegexAST",
                        source.toTable(),
                        new DebugUtil.Value("root", root),
                        new DebugUtil.Value("debugAST", ASTDebugDumpVisitor.getDump(wrappedRoot)),
                        new DebugUtil.Value("wrappedRoot", wrappedRoot),
                        new DebugUtil.Value("reachableCarets", reachableCarets),
                        new DebugUtil.Value("startsWithCaret", root.startsWithCaret()),
                        new DebugUtil.Value("endsWithDollar", root.endsWithDollar()),
                        new DebugUtil.Value("reachableDollars", reachableDollars)).append(properties.toTable());
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return toTable().toString();
    }
}
