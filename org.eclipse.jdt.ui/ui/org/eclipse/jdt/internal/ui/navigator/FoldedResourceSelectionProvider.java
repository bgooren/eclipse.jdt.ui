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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Presents folded resource folders as their first {@code IFolder} to actions,
 * while leaving the actual viewer selection unchanged.
 */
public final class FoldedResourceSelectionProvider implements ISelectionProvider {

	private final ISelectionProvider fDelegate;
	private final Map<ISelectionChangedListener, ISelectionChangedListener> fListeners= new IdentityHashMap<>();

	public FoldedResourceSelectionProvider(ISelectionProvider delegate) {
		fDelegate= delegate;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fListeners.containsKey(listener)) {
			return;
		}
		ISelectionChangedListener translatingListener= event -> listener.selectionChanged(
				new SelectionChangedEvent(this, toResourceSelection(event.getSelection())));
		fListeners.put(listener, translatingListener);
		fDelegate.addSelectionChangedListener(translatingListener);
	}

	@Override
	public ISelection getSelection() {
		return toResourceSelection(fDelegate.getSelection());
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		ISelectionChangedListener translatingListener= fListeners.remove(listener);
		if (translatingListener != null) {
			fDelegate.removeSelectionChangedListener(translatingListener);
		}
	}

	@Override
	public void setSelection(ISelection selection) {
		fDelegate.setSelection(selection);
	}

	static ISelection toResourceSelection(ISelection selection) {
		if (!(selection instanceof IStructuredSelection structuredSelection)
				|| structuredSelection.toList().stream().noneMatch(FoldedResourceFolder.class::isInstance)) {
			return selection;
		}
		List<Object> elements= new ArrayList<>(structuredSelection.size());
		for (Object element : structuredSelection) {
			elements.add(element instanceof FoldedResourceFolder folded ? folded.getFirstFolder() : element);
		}
		return new StructuredSelection(elements);
	}
}
