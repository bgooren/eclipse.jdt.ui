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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.IExtensionStateModel;

import org.eclipse.jdt.ui.PreferenceConstants;

/** Hides raw resource folders represented by a {@link FoldedResourceFolder}. */
public final class FoldedResourceFolderFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		Object parent= parentElement instanceof TreePath path ? path.getLastSegment() : parentElement;
		if (!PreferenceConstants.getPreferenceStore().getBoolean(
				PreferenceConstants.APPEARANCE_FOLD_RESOURCE_FOLDERS_IN_PACKAGE_EXPLORER)
				|| isFlatLayout(viewer) || !(parent instanceof IContainer)
				|| !(element instanceof IFolder folder)) {
			return true;
		}
		try {
			return folder.equals(JavaNavigatorContentProvider.getFoldedResourceFolder(folder,
					(folderParent, child) -> isVisible(viewer, folderParent, child)));
		} catch (CoreException e) {
			return true;
		}
	}

	private static boolean isFlatLayout(Viewer viewer) {
		if (viewer instanceof CommonViewer commonViewer) {
			IExtensionStateModel stateModel= commonViewer.getNavigatorContentService()
					.findStateModel(JavaNavigatorContentProvider.JDT_EXTENSION_ID);
			return stateModel != null && stateModel.getBooleanProperty(IExtensionStateConstants.Values.IS_LAYOUT_FLAT);
		}
		return false;
	}

	private boolean isVisible(Viewer viewer, Object parent, Object child) {
		if (viewer instanceof StructuredViewer structuredViewer) {
			for (ViewerFilter filter : structuredViewer.getFilters()) {
				if (filter != this && !filter.select(viewer, parent, child)) {
					return false;
				}
			}
		}
		return true;
	}
}
