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

import java.util.List;

import org.eclipse.core.commands.ExecutionException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.osgi.util.NLS;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.DeleteResourcesOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;

/** Delete support that retains the complete folded path in its confirmation. */
public final class FoldedResourceDeleteAction {

	private FoldedResourceDeleteAction() {
	}

	public static boolean runIfFolded(ISelectionProvider selectionProvider, Shell shell) {
		if (!(selectionProvider instanceof FoldedResourceSelectionProvider foldedProvider)) {
			return false;
		}
		ISelection viewerSelection= foldedProvider.getViewerSelection();
		if (!(viewerSelection instanceof IStructuredSelection structured) || structured.isEmpty()
				|| !structured.toList().stream().allMatch(FoldedResourceFolder.class::isInstance)) {
			return false;
		}
		List<FoldedResourceFolder> foldedFolders= structured.toList().stream() //
				.map(FoldedResourceFolder.class::cast) //
				.toList();
		String message= foldedFolders.size() == 1
				? NLS.bind(PackagesMessages.FoldedResourceDeleteAction_confirmSingle, foldedFolders.get(0).getLabel())
				: NLS.bind(PackagesMessages.FoldedResourceDeleteAction_confirmMultiple, foldedFolders.size());
		if (!MessageDialog.openQuestion(shell, PackagesMessages.FoldedResourceDeleteAction_title, message)) {
			return true;
		}
		IResource[] resources= foldedFolders.stream() //
				.map(FoldedResourceFolder::getFirstFolder) //
				.toArray(IResource[]::new);
		Job job= Job.create(PackagesMessages.FoldedResourceDeleteAction_job, monitor -> {
			DeleteResourcesOperation operation= new DeleteResourcesOperation(resources,
					PackagesMessages.FoldedResourceDeleteAction_operation, false);
			try {
				return PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(operation, monitor, //
						WorkspaceUndoUtil.getUIInfoAdapter(shell));
			} catch (ExecutionException e) {
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), e.getMessage(), e);
			}
		});
		job.setUser(true);
		job.schedule();
		return true;
	}
}
