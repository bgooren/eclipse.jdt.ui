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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;

/** Rename support for a complete folded resource-folder path. */
public final class FoldedResourceRenameAction {

	private FoldedResourceRenameAction() {
	}

	public static FoldedResourceFolder getFoldedFolder(IStructuredSelection selection) {
		return selection.size() == 1 && selection.getFirstElement() instanceof FoldedResourceFolder folded
				? folded
				: null;
	}

	public static void run(FoldedResourceFolder folded, Shell shell) {
		IContainer parent= folded.getFirstFolder().getParent();
		IPath oldPath= Path.fromPortableString(folded.getLabel());
		InputDialog dialog= new InputDialog(shell, PackagesMessages.FoldedResourceRenameAction_dialogTitle,
				PackagesMessages.FoldedResourceRenameAction_dialogMessage, oldPath.toPortableString(), value -> {
					IStatus status= FoldedResourceRenameOperation.validate(parent, oldPath,
							Path.fromPortableString(value));
					return status.isOK() ? null : status.getMessage();
				});
		if (dialog.open() != Window.OK) {
			return;
		}
		IPath newPath= Path.fromPortableString(dialog.getValue());
		FoldedResourceRenameOperation operation= new FoldedResourceRenameOperation(parent, oldPath, newPath,
				PackagesMessages.FoldedResourceRenameAction_operation);
		try {
			IStatus status= PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(operation, null, //
					WorkspaceUndoUtil.getUIInfoAdapter(shell));
			if (!status.isOK()) {
				ErrorDialog.openError(shell, PackagesMessages.FoldedResourceRenameAction_errorTitle, null, status);
			}
		} catch (ExecutionException e) {
			IStatus status= FoldedResourceRenameOperation.error(e.getMessage(),
					e.getCause() instanceof CoreException core ? core : null);
			ErrorDialog.openError(shell, PackagesMessages.FoldedResourceRenameAction_errorTitle, null, status);
		}
	}
}
