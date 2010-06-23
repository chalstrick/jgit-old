/*
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
package org.eclipse.jgit.api.plumbing;

import java.io.IOException;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.ReadTreeResult;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * A class used to execute a {@code Read-Tree} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command. Each instance of this class should only be
 * used for one invocation of the command (means: one call to {@link #call()})
 * <p>
 *
 * @see <a
 *      href="http://www.kernel.org/pub/software/scm/git/docs/git-read-tree.html"
 *      >Git documentation about Read-Tree</a>
 */
public class ReadTreeCommand extends GitCommand<ReadTreeResult> {
	private boolean merge;

	private boolean update;

	/**
	 * @param repo
	 *            the repository for which this command is executed
	 * @param merge
	 *            if set to <code>true</code> a merge is performed, not just a
	 *            read. The command will refuse to run if your index file has
	 *            unmerged entries, indicating that you have not finished
	 *            previous merge you started.
	 */
	protected ReadTreeCommand(Repository repo, boolean merge) {
		super(repo);
		this.merge=merge;
	}

	private Tree[] trees;

	public ReadTreeResult call() throws Exception {
		if (merge) {
			DirCacheCheckout dirCacheCheckout = null;
			if (trees.length == 1)
				// a initial checkout of tree
				dirCacheCheckout = new DirCacheCheckout(repo, null,
						DirCache.lock(repo), trees[0]);
			else if (trees.length == 2)
				// a fast forward from head to merge
				dirCacheCheckout = new DirCacheCheckout(repo, trees[0],
						DirCache.lock(repo), trees[1]);
			else if (trees.length == 3)
				// a merge between ours and theirs where commonPred is the
				// common
				// predecessor
				dirCacheCheckout = new DirCacheCheckout(repo, trees[0],
						DirCache.lock(repo), trees[1]);
			dirCacheCheckout.checkout();
			setCallable(false);
			return dirCacheCheckout;
		} else {
			fillIndex();
			setCallable(false);
			return null;
		}
	}

	private void fillIndex() throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		DirCache dc = DirCache.lock(repo);
		DirCacheBuilder builder=dc.builder();

		if (trees.length!=1) {
			throw new UnsupportedOperationException("Non-merge read-trees only allowed for input tree");
		}

		TreeWalk tw=new TreeWalk(repo);
		tw.reset();
		tw.setRecursive(true);
		for (Tree tree : trees)
			tw.addTree(tree.getId());
		while(tw.next()) {
			builder.add(createDirCacheEntry(tw.getTree(0, CanonicalTreeParser.class), 0));
		}
		builder.commit();
	}

	private DirCacheEntry createDirCacheEntry(CanonicalTreeParser tree, int stage) {
		DirCacheEntry dce = new DirCacheEntry(tree.getEntryPathString(), stage);
		dce.setFileMode(tree.getEntryFileMode());
		dce.setObjectId(tree.getEntryObjectId());
		return dce;
	}

	/**
	 * Sets a single tree to be read into the index. This option is used for
	 * example when you do the initial checkout of master branch after cloning a
	 * repo
	 *
	 * @param tree the tree to be read into the index
	 */
	public void setTrees(Tree tree) {
		checkCallable();
		trees = new Tree[] { tree };
	}

	/**
	 * @param head
	 * @param merge
	 */
	public void setTrees(Tree head, Tree merge) {
		checkCallable();
		trees = new Tree[] { head, merge };
	}

	/**
	 * @param commonPred
	 * @param ours
	 * @param theirs
	 */
	public void setTrees(Tree commonPred, Tree ours, Tree theirs) {
		checkCallable();
		trees = new Tree[] { commonPred, ours, theirs };
	}

	/**
	 * @return the trees given to this command
	 */
	public Tree[] getTrees() {
		return trees;
	}

	/**
	 * @param merge
	 *            if set to <code>true</code> a merge is performed, not just a
	 *            read. The command will refuse to run if your index file has
	 *            unmerged entries, indicating that you have not finished
	 *            previous merge you started.
	 *            <p>
	 *            For standard checkout operations this option should be enabled
	 */
	public void setMerge(boolean merge) {
		checkCallable();
		this.merge = merge;
	}

	/**
	 * @return the status of the merge option
	 * @see #setMerge(boolean)
	 */
	public boolean isMerge() {
		return merge;
	}

	/**
	 * @param update
	 *            if set to true this command will after a successful merge
	 *            update the files in the work tree with the result of the
	 *            merge. if set to <code>false</code> the work tree is not
	 *            touched
	 *            <p>
	 *            For standard checkout operations this option should be enabled
	 */
	public void setUpdate(boolean update) {
		checkCallable();
		this.update = update;
	}

	/**
	 * @return the status of the update option
	 * @see #setUpdate(boolean)
	 */
	public boolean isUpdate() {
		return update;
	}
}
