package org.eclipse.jgit.lib;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.junit.Test;

public class SubProjectTest extends RepositoryTestCase {
	@Test
	public void TestCommitWithSubProjects() throws Exception {
		DirCache dc = db.readDirCache();
		DirCacheBuilder dcBuilder = dc.builder();
		dcBuilder.add(makeEntry("f", FileMode.REGULAR_FILE, "content of f"));
		dcBuilder.finish();
		ObjectId commit = commit(db.newObjectInserter(), dc, new ObjectId[] {});
		System.out.println("Commit " + commit.name() + " in repo "
				+ db.getDirectory());
	}

	private ObjectId commit(final ObjectInserter odi, final DirCache treeB,
			final ObjectId[] parentIds) throws Exception {
		final CommitBuilder c = new CommitBuilder();
		c.setTreeId(treeB.writeTree(odi));
		c.setAuthor(new PersonIdent("A U Thor", "a.u.thor", 1L, 0));
		c.setCommitter(c.getAuthor());
		c.setParentIds(parentIds);
		c.setMessage("Tree " + c.getTreeId().name());
		ObjectId id = odi.insert(c);
		odi.flush();
		return id;
	}

	private DirCacheEntry makeEntry(final String path, final FileMode mode,
			final String content) throws Exception {
		final DirCacheEntry ent = new DirCacheEntry(path);
		ent.setFileMode(mode);
		ent.setObjectId(new ObjectInserter.Formatter().idFor(OBJ_BLOB,
				Constants.encode(content)));
		return ent;
	}
}
