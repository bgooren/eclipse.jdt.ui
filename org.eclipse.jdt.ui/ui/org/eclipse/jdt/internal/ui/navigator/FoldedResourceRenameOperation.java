/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.osgi.util.NLS;

import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;

/** Renames every segment of a folded folder path as one undoable operation. */
public final class FoldedResourceRenameOperation extends AbstractOperation {

	private final IContainer fParent;
	private final IPath fOldPath;
	private final IPath fNewPath;
	private boolean fExecuted;
	private boolean fAtNewPath;

	public FoldedResourceRenameOperation(IContainer parent, IPath oldPath, IPath newPath, String label) {
		super(label);
		fParent= parent;
		fOldPath= oldPath;
		fNewPath= newPath;
		addContext(WorkspaceUndoUtil.getWorkspaceUndoContext());
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		IStatus status= move(fOldPath, fNewPath, monitor);
		if (status.isOK()) {
			fExecuted= true;
			fAtNewPath= true;
		}
		return status;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		IStatus status= move(fOldPath, fNewPath, monitor);
		if (status.isOK()) {
			fAtNewPath= true;
		}
		return status;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		IStatus status= move(fNewPath, fOldPath, monitor);
		if (status.isOK()) {
			fAtNewPath= false;
		}
		return status;
	}

	@Override
	public boolean canUndo() {
		return fExecuted && fAtNewPath;
	}

	@Override
	public boolean canRedo() {
		return fExecuted && !fAtNewPath;
	}

	private IStatus move(IPath sourcePath, IPath targetPath, IProgressMonitor monitor) throws ExecutionException {
		try {
			ResourcesPlugin.getWorkspace().run(progress -> moveSegments(sourcePath, targetPath, progress),
					fParent.getProject(), IWorkspace.AVOID_UPDATE, monitor);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	private void moveSegments(IPath sourcePath, IPath targetPath, IProgressMonitor monitor) throws CoreException {
		IPath currentParentPath= IPath.EMPTY;
		for (int i= 0; i < sourcePath.segmentCount(); i++) {
			IPath source= currentParentPath.append(sourcePath.segment(i));
			IPath target= currentParentPath.append(targetPath.segment(i));
			if (!source.equals(target)) {
				fParent.getFolder(source).move(fParent.getFullPath().append(target),
						IResource.FORCE | IResource.KEEP_HISTORY, monitor);
			}
			currentParentPath= target;
		}
	}

	public static IStatus validate(IContainer parent, IPath oldPath, IPath newPath) {
		if (newPath.isAbsolute() || newPath.segmentCount() != oldPath.segmentCount()) {
			return Status.error(PackagesMessages.FoldedResourceRenameAction_segmentCount);
		}
		IStatus pathStatus= parent.getWorkspace().validatePath(parent.getFullPath().append(newPath).toString(),
				IResource.FOLDER);
		if (!pathStatus.isOK()) {
			return pathStatus;
		}
		if (oldPath.equals(newPath)) {
			return Status.error(PackagesMessages.FoldedResourceRenameAction_samePath);
		}
		IPath oldPrefix= IPath.EMPTY;
		IPath newPrefix= IPath.EMPTY;
		for (int i= 0; i < oldPath.segmentCount(); i++) {
			oldPrefix= oldPrefix.append(oldPath.segment(i));
			newPrefix= newPrefix.append(newPath.segment(i));
			IFolder destination= parent.getFolder(newPrefix);
			if (!oldPrefix.equals(newPrefix) && destination.exists()) {
				return Status.error(NLS.bind(PackagesMessages.FoldedResourceRenameAction_existingResource, newPrefix));
			}
		}
		return Status.OK_STATUS;
	}

	public IPath getNewPath() {
		return fNewPath;
	}

	public IPath getOldPath() {
		return fOldPath;
	}

	public IContainer getParent() {
		return fParent;
	}

	static IStatus error(String message, CoreException cause) {
		return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), message, cause);
	}
}
