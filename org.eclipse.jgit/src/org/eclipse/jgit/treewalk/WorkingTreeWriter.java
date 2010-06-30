/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com> and
 * other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v1.0 which accompanies this
 * distribution, is reproduced below, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.jgit.treewalk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.eclipse.jgit.lib.FileMode;

/**
 * Interface of classes which are capable to create/update/delete files in the
 * working tree.
 * <p>
 * Most applications will want to use the standard implementation of this ,
 * {@link FileWorkingTreeWriter}, as that does all IO through the standard
 * <code>java.io</code> package. But some applications want to do something more
 * than just java File I/O. E.g. they may have cached information about files in
 * the working tree and want to react on changes to the WorkingTree. Or they may
 * not want to store the WorkingTree files in the filesystem but e.g. in some
 * database. By using own implementations of {@link WorkingTreeIterator} for
 * traversal of the working tree and implementations of this interface for write
 * operations applications get more freedom on how to persist the working tree
 */
public abstract class WorkingTreeWriter {
	/**
	 * @param path
	 * @param mode
	 * @param length
	 * @param content
	 * @return the last modification time as described in {@link File#lastModified()}
	 */
	public abstract long create(String path, FileMode mode, InputStream content, int length);

	/**
	 * @param path
	 * @param mode
	 * @param content
	 * @param offset
	 * @param length
	 * @return
	 */
	public long create(String path, FileMode mode, byte[] content, int offset, int length) {
		return create(path, mode, new ByteArrayInputStream(content, offset, length), length);
	}

	/**
	 * @param path
	 * @param mode
	 * @param length
	 * @param content
	 * @return
	 */
	public abstract long update(String path, FileMode mode, InputStream content, int length);

	/**
	 * @param path
	 * @param mode
	 * @param content
	 * @param offset
	 * @param length
	 * @return
	 */
	public long update(String path, FileMode mode, byte[] content, int offset, int length) {
		return update(path, mode, new ByteArrayInputStream(content, offset, length), length);
	}

	/**
	 * @param path
	 * @return
	 */
	public abstract boolean delete(String path);
}
