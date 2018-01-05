/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast.visitors;

import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.tregex.nfa.ASTNodeSet;
import com.oracle.truffle.regex.tregex.parser.ast.BackReference;
import com.oracle.truffle.regex.tregex.parser.ast.CharacterClass;
import com.oracle.truffle.regex.tregex.parser.ast.Group;
import com.oracle.truffle.regex.tregex.parser.ast.LookAheadAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.LookBehindAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.MatchFound;
import com.oracle.truffle.regex.tregex.parser.ast.PositionAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTSubtreeRootNode;
import com.oracle.truffle.regex.tregex.parser.ast.Sequence;
import com.oracle.truffle.regex.tregex.parser.ast.Term;

import java.util.ArrayList;
import java.util.Set;

/**
 * Special AST visitor that will find all immediate successors of a given Term when the AST is seen
 * as a NFA, in priority order. A successor can either be a {@link CharacterClass} or a
 * {@link MatchFound} node.
 *
 * <pre>
 * {@code
 * Examples:
 *
 * Successors of "b" in (a|b)c:
 * 1.: "c"
 *
 * Successors of "b" in (a|b)*c:
 * 1.: "a"
 * 2.: "b"
 * 3.: "c"
 * }
 * </pre>
 *
 * For every successor, the visitor will provide the full path of AST nodes that have been traversed
 * from the initial node to the successor node, where {@link Group} nodes are treated specially: The
 * path will contain separate entries for <em>entering</em> and <em>leaving</em> a {@link Group},
 * and if a {@link Group} was <em>entered and then left</em> (note the order!) in the same path,
 * both nodes in the path will be marked with the {@link PathElement#isGroupPassThrough()} flag.
 * Furthermore, the visitor will not descend into lookaround assertions, it will jump over them and
 * just add their corresponding {@link LookAheadAssertion} or {@link LookBehindAssertion} node to
 * the path. This visitor is not thread-safe, since it uses the methods of
 * {@link RegexASTVisitorIterable} for traversing groups.
 *
 * <pre>
 * {@code
 * Examples with full path information:
 *
 * Successors of "b" in (a|b)c:
 * 1.: [leave group 1], [CharClass c]
 *
 * Successors of "b" in (a|b)*c:
 * 1.: [leave group 1], [enter group 1], [Sequence 0 of group 1], [CharClass a]
 * 2.: [leave group 1], [enter group 1], [Sequence 1 of group 1], [CharClass b]
 * 3.: [leave group 1], [CharClass c]
 * }
 * </pre>
 */
public abstract class NFATraversalRegexASTVisitor {

    public static final class PathElement {

        private static final byte FLAG_GROUP_ENTER = 1;
        private static final byte FLAG_GROUP_EXIT = 1 << 1;
        private static final byte FLAG_GROUP_PASS_THROUGH = 1 << 2;

        private final RegexASTNode node;
        private byte flags = 0;

        private PathElement(RegexASTNode node) {
            this.node = node;
        }

        public RegexASTNode getNode() {
            return node;
        }

        private boolean isFlagSet(byte flag) {
            return (flags & flag) != 0;
        }

        private void setFlag(byte flag) {
            flags |= flag;
        }

        private void clearFlag(byte flag) {
            flags &= ~flag;
        }

        public boolean isGroupEnter() {
            return isFlagSet(FLAG_GROUP_ENTER);
        }

        public void setGroupEnter() {
            setFlag(FLAG_GROUP_ENTER);
        }

        public boolean isGroupExit() {
            return isFlagSet(FLAG_GROUP_EXIT);
        }

        public void setGroupExit() {
            setFlag(FLAG_GROUP_EXIT);
        }

        public boolean isGroupPassThrough() {
            return isFlagSet(FLAG_GROUP_PASS_THROUGH);
        }

        public void setGroupPassThrough() {
            setFlag(FLAG_GROUP_PASS_THROUGH);
        }

        public void clearGroupPassThrough() {
            clearFlag(FLAG_GROUP_PASS_THROUGH);
        }
    }

    protected final RegexAST ast;
    private final ArrayList<PathElement> curPath = new ArrayList<>();
    private final ASTNodeSet<RegexASTNode> visited;
    private RegexASTNode cur;
    private Set<LookBehindAssertion> traversableLookBehindAssertions;
    private boolean canTraverseCaret = false;
    private boolean reverse = false;
    private boolean done = false;
    private int dollarsInPath = 0;

    protected NFATraversalRegexASTVisitor(RegexAST ast) {
        this.ast = ast;
        this.visited = new ASTNodeSet<>(ast);
    }

    public void setTraversableLookBehindAssertions(Set<LookBehindAssertion> traversableLookBehindAssertions) {
        this.traversableLookBehindAssertions = traversableLookBehindAssertions;
    }

    public boolean canTraverseCaret() {
        return canTraverseCaret;
    }

    public void setCanTraverseCaret(boolean canTraverseCaret) {
        this.canTraverseCaret = canTraverseCaret;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    protected void run(Term runRoot) {
        if (runRoot instanceof Group) {
            cur = runRoot;
        } else {
            advanceTerm(runRoot);
        }
        while (!done) {
            while (doAdvance()) {
                // advance until we reach the next node to visit
            }
            if (done) {
                break;
            }
            visit(curPath);
            retreat();
        }
        done = false;
    }

    /**
     * Visit the next successor found.
     * 
     * @param path Path to the successor. Do not modify this list, it will be reused by the visitor
     *            for finding the next successor!
     */
    protected abstract void visit(ArrayList<PathElement> path);

    protected abstract void enterLookAhead(LookAheadAssertion assertion);

    protected abstract void leaveLookAhead(LookAheadAssertion assertion);

    private boolean doAdvance() {
        if (cur.isDead() || !visited.add(cur)) {
            return retreat();
        }
        final PathElement curPathElement = new PathElement(cur);
        curPath.add(curPathElement);
        if (cur instanceof Group) {
            curPathElement.setGroupEnter();
            final Group group = (Group) cur;
            // This path will only be hit when visiting a group for the first time. All groups must
            // have at least one child sequence, so no check is needed here.
            assert group.visitorHasNext();
            cur = group.visitorGetNext(reverse);
            return true;
        } else if (cur instanceof Sequence) {
            final Sequence sequence = (Sequence) cur;
            if (sequence.isEmpty()) {
                pushGroupExit(sequence.getParent());
                return advanceTerm(sequence.getParent());
            } else {
                cur = reverse ? sequence.getLastTerm() : sequence.getFirstTerm();
                return true;
            }
        } else if (cur instanceof PositionAssertion) {
            final PositionAssertion assertion = (PositionAssertion) cur;
            switch (assertion.type) {
                case CARET:
                    if (canTraverseCaret) {
                        return advanceTerm(assertion);
                    } else {
                        return retreat();
                    }
                case DOLLAR:
                    dollarsInPath++;
                    return advanceTerm(assertion);
                default:
                    throw new IllegalStateException();
            }
        } else if (cur instanceof LookAheadAssertion) {
            enterLookAhead((LookAheadAssertion) cur);
            return advanceTerm((LookAheadAssertion) cur);
        } else if (cur instanceof LookBehindAssertion) {
            if (traversableLookBehindAssertions == null || traversableLookBehindAssertions.contains(cur)) {
                return advanceTerm((LookBehindAssertion) cur);
            } else {
                return retreat();
            }
        } else if (cur instanceof CharacterClass) {
            if (!reverse && dollarsInPath > 0) {
                // don't visit CharacterClass nodes if we traversed dollar - PositionAssertions
                // already
                return retreat();
            } else {
                return false;
            }
        } else if (cur instanceof BackReference) {
            throw new UnsupportedRegexException("back-references are not suitable for this visitor!");
        } else if (cur instanceof MatchFound) {
            return false;
        } else {
            throw new IllegalStateException();
        }
    }

    private boolean advanceTerm(Term term) {
        if (ast.isNFAInitialState(term)) {
            assert term instanceof PositionAssertion || term instanceof MatchFound;
            if (term instanceof PositionAssertion) {
                cur = ((PositionAssertion) term).getNext();
            } else {
                cur = ((MatchFound) term).getNext();
            }
            return true;
        }
        Term curTerm = term;
        while (!(curTerm.getParent() instanceof RegexASTSubtreeRootNode)) {
            Sequence parentSeq = (Sequence) curTerm.getParent();
            if (curTerm == (reverse ? parentSeq.getFirstTerm() : parentSeq.getLastTerm())) {
                final Group parentGroup = parentSeq.getParent();
                pushGroupExit(parentGroup);
                if (parentGroup.isLoop()) {
                    cur = parentGroup;
                    return true;
                }
                curTerm = parentGroup;
            } else {
                cur = parentSeq.getTerms().get(curTerm.getSeqIndex() + (reverse ? -1 : 1));
                return true;
            }
        }
        assert curTerm instanceof Group;
        assert curTerm.getParent() instanceof RegexASTSubtreeRootNode;
        cur = curTerm.getSubTreeParent().getMatchFound();
        return true;
    }

    private void pushGroupExit(Group group) {
        PathElement groupPathElement = new PathElement(group);
        groupPathElement.setGroupExit();
        for (int i = curPath.size() - 1; i >= 0; i--) {
            if (curPath.get(i).getNode() == group) {
                curPath.get(i).setGroupPassThrough();
                groupPathElement.setGroupPassThrough();
                break;
            }
        }
        curPath.add(groupPathElement);
    }

    private boolean retreat() {
        if (curPath.isEmpty()) {
            done = true;
            return false;
        }
        PathElement lastVisited = popPath();
        while (true) {
            if (lastVisited.getNode() instanceof Group) {
                Group group = (Group) lastVisited.getNode();
                if (lastVisited.isGroupExit()) {
                    if (lastVisited.isGroupPassThrough()) {
                        for (int i = curPath.size() - 1; i >= 0; i--) {
                            if (curPath.get(i).getNode() == group) {
                                assert curPath.get(i).isGroupEnter();
                                curPath.get(i).clearGroupPassThrough();
                                break;
                            }
                        }
                    }
                } else {
                    assert lastVisited.isGroupEnter();
                    if (group.visitorHasNext()) {
                        curPath.add(lastVisited);
                        cur = group.visitorGetNext(reverse);
                        return true;
                    } else {
                        visited.remove(group);
                        group.resetVisitorIterator();
                    }
                }
            } else {
                visited.remove(lastVisited.getNode());
                if (lastVisited.getNode() instanceof LookAheadAssertion) {
                    leaveLookAhead((LookAheadAssertion) lastVisited.getNode());
                } else if (lastVisited.getNode() instanceof PositionAssertion) {
                    if (((PositionAssertion) lastVisited.getNode()).type == PositionAssertion.Type.DOLLAR) {
                        dollarsInPath--;
                    }
                }
            }
            if (curPath.isEmpty()) {
                done = true;
                return false;
            }
            lastVisited = popPath();
        }
    }

    private PathElement popPath() {
        return curPath.remove(curPath.size() - 1);
    }
}
