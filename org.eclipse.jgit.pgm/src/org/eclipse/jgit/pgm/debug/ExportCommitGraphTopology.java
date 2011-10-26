package org.eclipse.jgit.pgm.debug;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.pgm.TextBuiltin;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Prints a string which describes the commit-graph topology for the current
 * repo. This string can than be given to the ImportCommitGraphTopology command
 * which will create a new repository only with empty commits which has the same
 * commit-graph topology as the original repo. Means: if you call 'gitk --all'
 * in the orginal repo and in the one created by ImportCommitGraphTopology then
 * you should see graphs with the same topology.
 *
 * <code>
 * Legend (in EBNF):
 * Description = { Commit , [ ReferencedMarker ] } ;
 * Commit = NormalCommit | MergeCommit ;
 * NormalCommit = [ Parent ] , '*' ;
 * (* a NormalCommit without a parent implicitly has as parent the previous commit *)
 *
 * MergeCommit = Parent , Parent , { Parent } , '*' ;
 * Parent = singleCharRef | multipleCharRef ;
 * (* all refs point relative into the past. '1' is the previous commit. '2' is
 *    the commit mentioned two commits before the current one.
 *    We use '1'-'9' and 'a'-'z' where 'a' refers to the 10th commit before the
 *    previous one, 'b' the 11th and so on. To encode the reference
 *    Integer.toString(index, 36) is used. References which consist of one char
 *    only are printed as they are, references which need more then one char
 *    are surrounded by square brackets *)
 *
 * singleCharRef = '0' | '1' | ... '9' | 'a' | 'b' | .... 'z' ;
 * multipleCharRef = '[' , singleCharRef , singleCharRef , { singleCharRef } , ']' ;
 * ReferencedMarker = '!' ;
 * (* If this marker is present then current commit should have a ref pointing
 *    to him *)
 * </code>
 * */
class ExportCommitGraphTopology extends TextBuiltin {
	@Override
	protected void run() throws Exception {
		List<RevCommit> commits = new LinkedList<RevCommit>();
		RevWalk rw = new RevWalk(db.newObjectReader());
		rw.sort(RevSort.COMMIT_TIME_DESC);
		rw.sort(RevSort.REVERSE, true);

		Set<RevCommit> referencedCommits = new HashSet<RevCommit>();
		for (AnyObjectId id : db.getAllRefsByPeeledObjectId().keySet()) {
			RevObject ro = rw.parseAny(id);
			if (ro instanceof RevCommit)
				referencedCommits.add((RevCommit) ro);
		}

		rw.markStart(referencedCommits);
		StringBuilder desc = new StringBuilder();
		RevCommit last = null;
		for (RevCommit c = rw.next(); c != null; c = rw.next()) {
			commits.add(0, c);
			int np = c.getParentCount();
			if (np > 1 || (np == 1 && !c.getParent(0).equals(last)))
				for (int i = 0; i < np; i++) {
					int idx = commits.indexOf(c.getParent(i));
					if (idx >= Character.MAX_RADIX)
						desc.append('[');
					desc.append(Integer.toString(idx, Character.MAX_RADIX));
					if (idx >= Character.MAX_RADIX)
						desc.append(']');
				}
			desc.append('*');
			if (referencedCommits.contains(c))
				desc.append("!");
			last = c;
		}
		System.out.println("processed " + commits.size() + " commits in repo "
				+ db.getDirectory() + ".");
		System.out.println(desc.toString());
	}
}
