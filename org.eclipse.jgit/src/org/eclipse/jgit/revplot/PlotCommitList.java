/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>,
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.revplot;

import java.text.MessageFormat;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.revwalk.RevCommitList;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * An ordered list of {@link PlotCommit} subclasses.
 * <p>
 * Commits are allocated into lanes as they enter the list, based upon their
 * connections between descendant (child) commits and ancestor (parent) commits.
 * <p>
 * The source of the list must be a {@link PlotWalk} and {@link #fillTo(int)}
 * must be used to populate the list.
 *
 * @param <L>
 *            type of lane used by the application.
 */
public class PlotCommitList<L extends PlotLane> extends
		RevCommitList<PlotCommit<L>> {
	static final int MAX_LENGTH = 25;

	private int positionsAllocated;

	private final TreeSet<Integer> freePositions = new TreeSet<Integer>();

	private final HashSet<PlotLane> activeLanes = new HashSet<PlotLane>(32);

	@Override
	public void clear() {
		super.clear();
		positionsAllocated = 0;
		freePositions.clear();
		activeLanes.clear();
	}

	@Override
	public void source(final RevWalk w) {
		if (!(w instanceof PlotWalk))
			throw new ClassCastException(MessageFormat.format(JGitText.get().classCastNotA, PlotWalk.class.getName()));
		super.source(w);
	}

	/**
	 * Find the set of lanes passing through a commit's row.
	 * <p>
	 * Lanes passing through a commit are lanes that the commit is not directly
	 * on, but that need to travel through this commit to connect a descendant
	 * (child) commit to an ancestor (parent) commit. Typically these lanes will
	 * be drawn as lines in the passed commit's box, and the passed commit won't
	 * appear to be connected to those lines.
	 * <p>
	 * This method modifies the passed collection by adding the lanes in any
	 * order.
	 *
	 * @param currCommit
	 *            the commit the caller needs to get the lanes from.
	 * @param result
	 *            collection to add the passing lanes into.
	 */
	public void findPassingThrough(final PlotCommit<L> currCommit,
			final Collection<L> result) {
		for (final PlotLane p : currCommit.passingLanes)
			result.add((L) p);
	}

	@Override
	protected void enter(final int index, final PlotCommit<L> currCommit) {
		setupChildren(currCommit);
		l("entering PlotCommitList.enter: index:", index, ", currCommit",
				currCommit);
		l("  current state: ", d(this));

		final int nChildren = currCommit.getChildCount();
		if (nChildren == 0) {
			l("leaving PlotCommitList.enter: Commit had no child. Do nothing");
			return;
		}

		if (nChildren == 1 && currCommit.children[0].getParentCount() < 2) {
			// Only one child, child has only us as their parent.
			// Stay in the same lane as the child.
			//
			final PlotCommit c = currCommit.children[0];
			if (c.lane == null) {
				// Hmmph. This child must be the first along this lane.
				//
				l("  PlotCommitList.enter: The only of child was not a merge commit and had no lane. Positioned it.");
				c.lane = createLaneOnFreePosition();
				activeLanes.add(c.lane);
			}

			// All commits between the current commit and the currently handled
			// child should get the childs lane as passing lane
			for (int r = index - 1; r >= 0; r--) {
				final PlotCommit rObj = get(r);
				if (rObj == c)
					break;
				rObj.addPassingLane(c.lane);
				l("  PlotCommitList.enter: Commit ", rObj.getName(),
						" got a new passingLange: ", c.lane);
			}

			currCommit.lane = c.lane;
			handleBlockedLanes(index, currCommit, nChildren);
		} else {
			// More than one child, or our child is a merge.
			// Use a different lane.
			//

			// Process all our children. Especially important when there is more
			// than one child (e.g. a commit is processed where other branches
			// fork out). For each child the following is done
			// 1. If no lane was assigned to the child a new lane is created and
			// assigned
			// 2. The lane of the child is closed. If this frees a position,
			// this position will be added freePositions list.
			// If we have multiple children which where previously not on a lane
			// each such child will get his own new lane but all those new lanes
			// will be on the same position. We have to take care that not
			// multiple newly created (in step 1) lanes occupy that position on
			// which the
			// parent's lane will be on. Therefore we delay closing the lane
			// with the parents position until all children are processed.

			// The lane on that position the current commit will be on
			PlotLane reservedLane = null;

			for (int i = 0; i < nChildren; i++) {
				final PlotCommit c = currCommit.children[i];
				l("  PlotCommitList.enter: processing child: "
						+ c.getName());

				// don't forget to position all of your children if they are
				// not already positioned.
				if (c.lane == null) {
					c.lane = createLaneOnFreePosition();
					activeLanes.add(c.lane);
					l("  Child got a new lane. " + d(c.lane));
					if (reservedLane != null) {
						closeLane(c.lane);
						l("  Reserved lane was " + reservedLane
								+ ". Close childs lane!");
					} else {
						reservedLane = c.lane;
						l("  PlotCommitList.enter: Reserved lane was null. Set childs lane as the reserverd lane.");
					}
				} else if (reservedLane == null && activeLanes.contains(c.lane)) {
					l("  PlotCommitList.enter: The child was already put on a lane, reserved lane was null and active lane contained childs lane. Set reserved lane to childs lane.");
					reservedLane = c.lane;
				} else {
					l("  PlotCommitList.enter: Close childs lane");
					closeLane(c.lane);
				}
			}

			// finally all children are processed. We can close the lane on that
			// position our current commit will be on.
			if (reservedLane != null) {
				closeLane(reservedLane);
				l("  PlotCommitList.enter: Close the reserved lane ",
						reservedLane);
			}

			putOnNextFreeLane(currCommit);
			handleBlockedLanes(index, currCommit, nChildren);
		}

		l("leaving PlotCommitList.enter: state: ", this);
	}

	private void putOnNextFreeLane(final PlotCommit c) {
		c.lane = createLaneOnFreePosition();

		activeLanes.add(c.lane);
		l("  PlotCommitList.putOnNextFreeLane: Commit " + c.getName()
				+ " was put on a new lane " + c.lane);
	}

	/**
	 * When connecting a plotcommit to the child make sure the plotcommit is not
	 * located on a lane on which a passed commit is located on. Otherwise we
	 * would have to draw a line through a commit. In such a situation
	 * reposition the plotcommits lane. (The lanes of the children are
	 * untouched)
	 *
	 * @param index
	 * @param commit
	 * @param childrenToProcess
	 */
	private void handleBlockedLanes(final int index,
			final PlotCommit<L> commit, int childrenToProcess) {
		l("entering PlotCommitList.handleBlockedLanes: index:"
				+ index + ", commit:" + commit.getName() + ", nChildren:"
				+ childrenToProcess);

		// Process all commits on top of the plotcommit until you have processed
		// it's last child. Collect all positions on which the commits are
		// located on.
		BitSet blockedPositions = new BitSet();
		if (childrenToProcess > 0) {
			for (int r = index - 1; r >= 0; r--) {
				final PlotCommit rObj = get(r);
				l("  PlotCommitList.handleBlockedLanes: processing successor #",
						r, ": ", rObj);
				if (commit.isChild(rObj)) {
					l("  PlotCommitList.handleBlockedLanes: found a child. Decrease remaining to ",
							childrenToProcess - 1);
					if (--childrenToProcess == 0)
						break;
				}
				if (rObj != null) {
					PlotLane lane = rObj.getLane();
					if (lane != null)
						blockedPositions.set(lane.getPosition());
					rObj.addPassingLane(commit.lane);
					l("  PlotCommitList.handleBlockedLanes: successor's lane was  added to blocking lanes. current commits lane is added to successors passing lanes");
				}
			}
		}
		// Now let's check whether we have to reposition the lane
		if (blockedPositions.get(commit.lane.getPosition())) {
			l("  PlotCommitList.handleBlockedLanes: Commits lane is blocked. Reposition!");
			int newPos = -1;
			for (Integer pos : freePositions)
				if (!blockedPositions.get(pos.intValue())) {
					newPos = pos.intValue();
					l("  PlotCommitList.handleBlockedLanes: pos:",
							pos.intValue(),
							" is free!");
					break;
				} else {
					l("  PlotCommitList.handleBlockedLanes: pos:", pos,
							" is NOT free.");
				}
			if (newPos == -1) {
				newPos = positionsAllocated++;
				l("  PlotCommitList.handleBlockedLanes: new newPos found. Choosed the nex available:",
						newPos);
			}
			l("  PlotCommitList.handleBlockedLanes: Lane "
					+ commit.lane + " goes to a new location " + newPos);
			freePositions.add(Integer.valueOf(commit.lane.getPosition()));
			activeLanes.remove(commit.lane);
			commit.lane.position = newPos;
			activeLanes.add(commit.lane);
			l("Leaving PlotCommitList.handleBlockedLanes: state: ", d(this));
		}
	}

	private static void l(Object... args) {
		for (Object o : args)
			System.out.print((o == null) ? "(null)" : o.toString());
		System.out.println();
	}

	private String d(PlotCommitList<L> o) {
		return (o == null) ? "(null)" : "positionsAllocated="
				+ d(o.positionsAllocated) + ", freePositions:"
				+ d(o.freePositions) + ", activeLanes:" + d(o.activeLanes);
	}

	private static String d(Object o) {
		return (o == null) ? "(null)" : o.toString();
	}

	private void closeLane(PlotLane lane) {
		recycleLane((L) lane);
		if (activeLanes.remove(lane)) {
			freePositions.add(Integer.valueOf(lane.getPosition()));
		}
	}

	@SuppressWarnings("static-method")
	private void setupChildren(final PlotCommit<L> currCommit) {
		final int nParents = currCommit.getParentCount();
		for (int i = 0; i < nParents; i++)
			((PlotCommit) currCommit.getParent(i)).addChild(currCommit);
	}

	private PlotLane createLaneOnFreePosition() {
		final PlotLane p = createLane();
		if (freePositions.isEmpty()) {
			p.position = positionsAllocated++;
		} else {
			final Integer min = freePositions.first();
			p.position = min.intValue();
			freePositions.remove(min);
		}
		return p;
	}

	/**
	 * @return a new Lane appropriate for this particular PlotList.
	 */
	protected L createLane() {
		return (L) new PlotLane();
	}

	/**
	 * Return colors and other reusable information to the plotter when a lane
	 * is no longer needed.
	 *
	 * @param lane
	 */
	protected void recycleLane(final L lane) {
		// Nothing.
	}
}
