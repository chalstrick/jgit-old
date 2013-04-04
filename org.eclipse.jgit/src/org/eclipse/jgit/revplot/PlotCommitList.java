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
	@Override
	public void source(final RevWalk w) {
		if (!(w instanceof PlotWalk))
			throw new ClassCastException(MessageFormat.format(JGitText.get().classCastNotA, PlotWalk.class.getName()));
		super.source(w);
	}

	@Override
	protected void enter(final int index, final PlotCommit<L> currCommit) {
		setupChildren(currCommit);
		l("entering PlotCommitList.enter: index:", index, ", currCommit",
				currCommit);

		final int nChildren = currCommit.getChildCount();

		if (nChildren == 0) {
			// A commit with no children -> put him on a new lane

			currCommit.lane = createLane();
			currCommit.lane.position = 0; // A newly created lane can always be
											// positioned in the first column.
											// Later, when passing lanes are
											// added to this commit we may have
											// to shift the lane.
			l("current commit  has no children and was put on a new lane ");
		} else if (nChildren == 1
				&& currCommit.children[0].getParentCount() == 1) {
			// Only one child, child has only us as their parent.
			// Stay in the same lane as the child.
			final PlotCommit c = currCommit.children[0];
			currCommit.lane = c.lane;
			l("current commit has one child ",
					currCommit.children[0],
					" and this has on the current commit as parent. Put him on the same lane.");

			// All commits between the current commit and the currently handled
			// child should get the childs lane as passing lane
			connect(index, c);
		} else if (nChildren > 1) {
			l("current commit has multiple children. Find the youngest.");
			PlotCommit lastFoundChild = findLeftMostChild(currCommit, index);
			// This commit is a branch point which has multiple children. Stay on the same lane as the youngest child
			if (lastFoundChild != null) {
				l("put current commit on the same lane as the youngest child ",
						lastFoundChild);
				currCommit.lane = lastFoundChild.lane;
				connect(index, lastFoundChild);
			} else {
				System.err.println("Fatal: no youngest child was found");
			}
		} else if (nChildren == 1
				&& currCommit.children[0].getParentCount() > 1) {
			// We have one child and this is a merge commit. Put the current
			// commit on a new lane.
			currCommit.lane = createLane();
			currCommit.lane.position = 0; // A newly created lane can always be
											// positioned in the first column.
											// Later, when passing lanes are
											// added to this commit we may have
											// to shift the lane.
			l("current commit  has one child which is a merge commit. put him on a new lane ",
					currCommit.lane);
			connect(index, currCommit.children[0]);
		}

		l("leaving PlotCommitList.enter: currentCommit is now ", currCommit);
	}

	private PlotCommit findLeftMostChild(PlotCommit parent, int parentIndex) {
		int nChildren = parent.getChildCount();
		int foundChildren = 0;
		int i = parentIndex;
		l("find youngest child of commit with index ", parentIndex,
				". Nr of children: ", nChildren);
		PlotCommit lastFoundChild = null;
		while (i > 0 && foundChildren < nChildren) {
			PlotCommit c = get(--i);
			l("inspect child ", c.getId());
			for (int j = 0; j < nChildren; j++) {
				if (parent.children[j].getId().equals(c.getId())) {
					foundChildren++;
					lastFoundChild = c;
					l("found the ", foundChildren, " child ", c.getId());
					break;
				}
			}
		}
		return (foundChildren == nChildren) ? lastFoundChild : null;
	}

	private void connect(final int index, final PlotCommit c) {
		l("add a passing lane between (excluding) the commit with index ",
				index, " and the commit ", c.getId());
		for (int r = index - 1; r >= 0; r--) {
			final PlotCommit rObj = get(r);
			if (rObj == c)
				break;
			l("inspecting object ", rObj.getId());
			addPassingLane(rObj, c.lane);
		}
	}

	private void addPassingLane(PlotCommit rObj, PlotLane l) {
		l("add the passing lane ", l, " to object ", rObj.getId());
		for (PlotLane passingLane : rObj.passingLanes)
			if (passingLane.equals(l)) {
				l("the lane was already a passing lane. return!");
				return;
			}
		int targetPos = l.getPosition();
		if (rObj.getLane().getPosition() >= targetPos) {
			rObj.getLane().position++;
			l("Have to move that lane of the object one to the right. New object:  ",
					rObj);
		}
		for (PlotLane passingLane : rObj.passingLanes) {
			l("Inspecting passing lane ", passingLane);
			if (passingLane.getPosition() >= targetPos) {
				passingLane.position++;
				l("Have to move that lane one to the right. New lane:  ", passingLane);
			} else {
				l("That passing lane is to the left of our target position ",
						targetPos, ". Don't touch this lane");
			}
		}
		rObj.addPassingLane(l);
	}

	private static void l(Object... args) {
		for (Object o : args)
			System.out.print((o == null) ? "(null)" : o.toString());
		System.out.println();
	}

	private static void setupChildren(final PlotCommit currCommit) {
		final int nParents = currCommit.getParentCount();
		for (int i = 0; i < nParents; i++)
			((PlotCommit) currCommit.getParent(i)).addChild(currCommit);
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
