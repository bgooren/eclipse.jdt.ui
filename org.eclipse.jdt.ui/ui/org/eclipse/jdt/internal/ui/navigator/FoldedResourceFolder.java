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

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A folded resource-folder occurrence in the Project Explorer.
 * <p>
 * The same resource can occur in both the physical resource tree and the Java
 * model projection. Keeping the visible parent in this node gives each
 * occurrence a distinct identity and prevents parent and reveal requests from
 * jumping between those projections.
 * </p>
 */
public final class FoldedResourceFolder implements IAdaptable, IWorkbenchAdapter {

	private final Object fParent;
	private final IFolder fFirstFolder;
	private final IFolder fFolder;

	FoldedResourceFolder(Object parent, IFolder firstFolder, IFolder folder) {
		fParent= Objects.requireNonNull(parent);
		fFirstFolder= Objects.requireNonNull(firstFolder);
		fFolder= Objects.requireNonNull(folder);
	}

	Object getParent() {
		return fParent;
	}

	IFolder getFirstFolder() {
		return fFirstFolder;
	}

	IFolder getFolder() {
		return fFolder;
	}

	String getLabel() {
		IPath firstPath= fFirstFolder.getProjectRelativePath();
		IPath folderPath= fFolder.getProjectRelativePath();
		return folderPath.makeRelativeTo(firstPath.removeLastSegments(1)).toString();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return adapter.cast(this);
		}
		if (adapter.isInstance(fFolder)) {
			return adapter.cast(fFolder);
		}
		return fFolder.getAdapter(adapter);
	}

	@Override
	public Object[] getChildren(Object object) {
		try {
			return fFolder.members();
		} catch (CoreException e) {
			return new Object[0];
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		IWorkbenchAdapter adapter= fFolder.getAdapter(IWorkbenchAdapter.class);
		return adapter == null ? null : adapter.getImageDescriptor(fFolder);
	}

	@Override
	public String getLabel(Object object) {
		return getLabel();
	}

	@Override
	public Object getParent(Object object) {
		return fParent;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fParent, fFirstFolder, fFolder);
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof FoldedResourceFolder other
				&& fParent.equals(other.fParent)
				&& fFirstFolder.equals(other.fFirstFolder)
				&& fFolder.equals(other.fFolder);
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
