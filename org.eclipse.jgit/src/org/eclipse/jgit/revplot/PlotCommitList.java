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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ObjectId;
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

	Map<ObjectId, PlotLane> activeLanesByNextCommit = new HashMap<ObjectId, PlotLane>();

	BitSet occupiedPositions = new BitSet();


	@Override
	protected void enter(final int index, final PlotCommit<L> currCommit) {
		setupChildren(currCommit);
		l("entering PlotCommitList.enter: index:", index, ", currCommit",
				currCommit, "occupied:", occupiedPositions);

		PlotLane myLane = activeLanesByNextCommit.get(currCommit.getId());
		if (myLane != null) {
			l("a lane was already reserved for this commit. lane:", myLane);

			currCommit.lane = myLane;
			activeLanesByNextCommit.remove(currCommit.getId());
			occupiedPositions.clear(myLane.position);
		} else {
			currCommit.lane = createLane();
			currCommit.lane.position = occupiedPositions.nextClearBit(0);
			l("no lane was reserved for this commit. created a new lane: ",
					currCommit.lane);
		}

		for (PlotLane l : activeLanesByNextCommit.values()) {
			currCommit.addPassingLane(l);
			l("The following lane was added as passing lane to the commit: ", l);
		}

		PlotLane toBeReserved;
		for (int i = 0; i < currCommit.getParentCount(); i++) {
			l("Inspect the following parent: ", currCommit.getParent(i).getId());
			if (i == 0) {
				l("Since I am inspecting the first parent reserve my lane for that parent");
				toBeReserved = currCommit.lane;
			} else {
				toBeReserved = createLane();
				toBeReserved.position = occupiedPositions.nextClearBit(0);
				l("Since I am inspecting not the first parent reserve a new lane for that parent. lane:",
						toBeReserved);
			}
			PlotLane alreadyReserved = activeLanesByNextCommit.get(currCommit.getParent(i).getId());
			if (alreadyReserved == null) {
				l("No lane was reserved for that parent");
				occupiedPositions.set(toBeReserved.position);
				activeLanesByNextCommit.put(currCommit.getParent(i).getId(),
						toBeReserved);
			} else if (alreadyReserved.position > toBeReserved.position) {
				occupiedPositions.set(toBeReserved.position);
				activeLanesByNextCommit.put(currCommit.getParent(i).getId(),
						toBeReserved);
				occupiedPositions.clear(alreadyReserved.position);
			}
		}
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
