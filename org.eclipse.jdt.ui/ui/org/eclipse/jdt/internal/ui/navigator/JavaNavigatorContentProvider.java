/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

public class JavaNavigatorContentProvider extends
		PackageExplorerContentProvider implements IPipelinedTreeContentProvider {

	public JavaNavigatorContentProvider() {
		super(false);
	}

	public JavaNavigatorContentProvider(boolean provideMembers) {
		super(provideMembers);
	}

	public static final String JDT_EXTENSION_ID = "org.eclipse.jdt.java.ui.javaContent"; //$NON-NLS-1$
	static final String VIEWER_PROPERTY = "org.eclipse.jdt.ui.navigator.viewer"; //$NON-NLS-1$

	private IExtensionStateModel fStateModel;

	private IPropertyChangeListener fLayoutPropertyListener;

	private TreeViewer fViewer;
	private final Set<Object> fPendingFoldRefreshTargets= new LinkedHashSet<>();
	private TreePath[] fPendingExpandedPaths;
	private boolean fFoldRefreshScheduled;

	@Override
	public void init(ICommonContentExtensionSite commonContentExtensionSite) {
		IExtensionStateModel stateModel = commonContentExtensionSite
				.getExtensionStateModel();
		IMemento memento = commonContentExtensionSite.getMemento();

		fStateModel = stateModel;
		restoreState(memento);
		fLayoutPropertyListener = event -> {
			if (Values.IS_LAYOUT_FLAT.equals(event.getProperty())) {
				if (event.getNewValue() != null) {
					boolean newValue1 = ((Boolean) event.getNewValue()) ? true : false;
					setIsFlatLayout(newValue1);
				}
			} else if (Values.IS_LIBRARIES_NODE_SHOWN.equals(event.getProperty())) {
				if (event.getNewValue() != null) {
					boolean newValue2 = ((Boolean) event.getNewValue());
					setShowLibrariesNode(newValue2);
				}
			}
		};
		fStateModel.addPropertyChangeListener(fLayoutPropertyListener);

		setIsFlatLayout(fStateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT));
		setShowLibrariesNode(fStateModel.getBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN));

		IPreferenceStore store = PreferenceConstants.getPreferenceStore();
		boolean showCUChildren = store
				.getBoolean(PreferenceConstants.SHOW_CU_CHILDREN);
		setProvideMembers(showCUChildren);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fStateModel.getProperty(VIEWER_PROPERTY) == fViewer) {
			fStateModel.setProperty(VIEWER_PROPERTY, null);
		}
		fStateModel.removePropertyChangeListener(fLayoutPropertyListener);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer= (TreeViewer)viewer;
		fStateModel.setProperty(VIEWER_PROPERTY, fViewer);
		super.inputChanged(viewer, oldInput, findInputElement(newInput));
	}

	@Override
	public Object getParent(Object element) {
		Object parent= super.getParent(element);
		if (parent instanceof IJavaModel) {
			return ((IJavaModel)parent).getWorkspace().getRoot();
		}
		if (parent instanceof IJavaProject) {
			return ((IJavaProject)parent).getProject();
		}
		return parent;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			IWorkspaceRoot root = (IWorkspaceRoot) inputElement;
			return filterResourceProjects(root.getProjects());
		} else if (inputElement instanceof IJavaModel) {
			return filterResourceProjects(((IJavaModel) inputElement).getWorkspace().getRoot().getProjects());
		}
		if (inputElement instanceof IProject) {
			return super.getElements(JavaCore.create((IProject)inputElement));
		}
		return super.getElements(inputElement);
	}

	private static IProject[] filterResourceProjects(IProject[] projects) {
		List<IProject> filteredProjects= new ArrayList<>(projects.length);
		for (IProject project : projects) {
			if (!project.isOpen() || isJavaProject(project))
				filteredProjects.add(project);
		}
		return filteredProjects.toArray(new IProject[filteredProjects.size()]);
	}

	private static boolean isJavaProject(IProject project) {
		try {
			return project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof FoldedResourceFolder folded) {
			return getFoldedResourceFolderChildren(folded).length > 0;
		}
		if (element instanceof IProject) {
			return ((IProject) element).isAccessible();
		}
		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof FoldedResourceFolder folded) {
			return getFoldedResourceFolderChildren(folded);
		}
		if (parentElement instanceof IWorkspaceRoot) {
			IWorkspaceRoot root = (IWorkspaceRoot) parentElement;
			return filterResourceProjects(root.getProjects());
		}
		if (parentElement instanceof IProject) {
			return super.getChildren(JavaCore.create((IProject)parentElement));
		}
		Object[] children= super.getChildren(parentElement);
		if (parentElement instanceof IJavaElement) {
			return wrapFoldedFolders(parentElement, children);
		}
		if (parentElement instanceof IContainer && isResourceFolderFoldingEnabled()) {
			return Arrays.stream(children).filter(child -> !(child instanceof IFolder)).toArray();
		}
		return children;
	}

	private Object[] wrapFoldedFolders(Object parent, Object[] children) {
		if (!isResourceFolderFoldingEnabled()) {
			return children;
		}
		Object[] wrapped= children.clone();
		for (int i= 0; i < wrapped.length; i++) {
			if (wrapped[i] instanceof IFolder folder) {
				try {
					wrapped[i]= createFoldedFolder(parent, getFoldedResourceFolder(folder));
				} catch (CoreException e) {
					// leave the original folder unfolded
				}
			}
		}
		return wrapped;
	}

	private Object findInputElement(Object newInput) {
		if (newInput instanceof IWorkspaceRoot) {
			return JavaCore.create((IWorkspaceRoot) newInput);
		}
		return newInput;
	}

	@Override
	public void restoreState(IMemento memento) {

	}

	@Override
	public void saveState(IMemento memento) {

	}

	@Override
	public void getPipelinedChildren(Object parent, Set currentChildren) {
		customize(parent, getChildren(parent), currentChildren);
		foldPipelinedResourceFolders(parent, currentChildren);
	}

	@Override
	public void getPipelinedElements(Object input, Set currentElements) {
		customize(input, getElements(input), currentElements);
		foldPipelinedResourceFolders(input, currentElements);
	}

	@Override
	public Object getPipelinedParent(Object object, Object suggestedParent) {
		if (object instanceof FoldedResourceFolder folded) {
			return folded.getParent();
		}
		if (suggestedParent instanceof FoldedResourceFolder) {
			return suggestedParent;
		}
		Object parent= getParent(object);
		IFolder parentFolder= parent instanceof IFolder folder ? folder : null;
		FoldedResourceFolder foldedParent= createFoldedPipelinedParent(parentFolder);
		return foldedParent == null ? parent : foldedParent;
	}

	private FoldedResourceFolder createFoldedPipelinedParent(IFolder leafFolder) {
		if (leafFolder == null || !isResourceFolderFoldingEnabled()) {
			return null;
		}
		IFolder firstFolder= leafFolder;
		IContainer visibleParent= leafFolder.getParent();
		try {
			while (visibleParent instanceof IFolder parentFolder && !isPackageFragmentRoot(parentFolder)
					&& firstFolder.equals(getSingleVisibleResourceFolderChild(parentFolder, this::isVisible))) {
				firstFolder= parentFolder;
				visibleParent= parentFolder.getParent();
			}
		} catch (CoreException e) {
			return null;
		}
		return firstFolder.equals(leafFolder) ? null
				: new FoldedResourceFolder(visibleParent, firstFolder, leafFolder);
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {

		Object originalParent= addModification.getParent();
		Object parent= originalParent;
		boolean resourceShapeChanged= containsResource(addModification.getChildren());

		if (parent instanceof IJavaProject) {
			addModification.setParent(((IJavaProject)parent).getProject());
		}

		if (parent instanceof IWorkspaceRoot) {
			deconvertJavaProjects(addModification);
		}

		convertToJavaElements(addModification);
		foldPipelinedResourceFolders(addModification.getParent(), addModification.getChildren());
		if (resourceShapeChanged && isResourceFolderFoldingEnabled()) {
			scheduleFoldRefresh(originalParent);
			if (!originalParent.equals(addModification.getParent())) {
				scheduleFoldRefresh(addModification.getParent());
			}
		}
		return addModification;
	}

	/**
	 * Refreshes the smallest stable parent after an incremental addition changes a
	 * folder into a folded chain. The resource content provider may already have
	 * shown the first folder; refreshing replaces that stale node instead of adding
	 * the folded occurrence beside it.
	 */
	protected void scheduleFoldRefresh(Object parent) {
		TreeViewer viewer= fViewer;
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		Object refreshTarget= getFoldRefreshTarget(parent);
		if (refreshTarget == null) {
			refreshTarget= viewer.getInput();
		}
		if (refreshTarget == null) {
			return;
		}
		synchronized (fPendingFoldRefreshTargets) {
			fPendingFoldRefreshTargets.add(refreshTarget);
			if (fFoldRefreshScheduled) {
				return;
			}
			fPendingExpandedPaths= viewer.getExpandedTreePaths();
			fFoldRefreshScheduled= true;
		}
		viewer.getControl().getDisplay().asyncExec(this::runPendingFoldRefreshes);
	}

	private Object getFoldRefreshTarget(Object parent) {
		Object refreshTarget= parent;
		if (parent instanceof IFolder folder) {
			FoldedResourceFolder foldedParent= createFoldedPipelinedParent(folder);
			if (foldedParent != null) {
				refreshTarget= foldedParent.getParent();
			}
		}
		return refreshTarget;
	}

	private void runPendingFoldRefreshes() {
		Set<Object> refreshTargets;
		TreePath[] expandedPaths;
		synchronized (fPendingFoldRefreshTargets) {
			refreshTargets= Set.copyOf(fPendingFoldRefreshTargets);
			expandedPaths= fPendingExpandedPaths;
			fPendingFoldRefreshTargets.clear();
			fPendingExpandedPaths= null;
			fFoldRefreshScheduled= false;
		}
		TreeViewer viewer= fViewer;
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		for (Object target : refreshTargets) {
			viewer.refresh(target, true);
		}
		viewer.getControl().getDisplay().asyncExec(() -> restoreExpandedPaths(viewer, expandedPaths));
	}

	private static void restoreExpandedPaths(TreeViewer viewer, TreePath[] expandedPaths) {
		if (!viewer.getControl().isDisposed() && expandedPaths != null) {
			viewer.setExpandedTreePaths(expandedPaths);
		}
	}

	@Override
	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification removeModification) {
		Object originalParent= removeModification.getParent();
		boolean resourceShapeChanged= containsResource(removeModification.getChildren());
		IJavaElement javaParent= originalParent instanceof IContainer container ? convert(container) : null;
		deconvertJavaProjects(removeModification);
		convertToJavaElements(removeModification.getChildren());
		if (resourceShapeChanged && isResourceFolderFoldingEnabled()) {
			scheduleFoldRefresh(originalParent);
			if (javaParent != null && !originalParent.equals(javaParent)) {
				scheduleFoldRefresh(javaParent);
			}
		}
		return removeModification;
	}

	private static boolean containsResource(Set<?> children) {
		return children.stream().anyMatch(IResource.class::isInstance);
	}

	private void deconvertJavaProjects(PipelinedShapeModification modification) {
		Set<IProject> convertedChildren = new LinkedHashSet<>();
		for (Iterator<IAdaptable> iterator = modification.getChildren().iterator(); iterator.hasNext();) {
			Object added = iterator.next();
			if(added instanceof IJavaProject) {
				iterator.remove();
				convertedChildren.add(((IJavaProject)added).getProject());
			}
		}
		modification.getChildren().addAll(convertedChildren);
	}

	/**
	 * Converts the shape modification to use Java elements.
	 *
	 *
	 * @param modification
	 *            the shape modification to convert
	 * @return returns true if the conversion took place
	 */
	private boolean convertToJavaElements(PipelinedShapeModification modification) {
		Object parent = modification.getParent();
		// As of 3.3, we no longer re-parent additions to IProject.
		if (parent instanceof IContainer) {
			IJavaElement element = convert((IContainer) parent);
			if (element != null) {
				// we don't convert the root
				if( !(element instanceof IJavaModel) && !(element instanceof IJavaProject))
					modification.setParent(element);
				return convertToJavaElements(modification.getChildren());

			}
		}
		return false;
	}

	private static IJavaElement convert(IResource resource) {
		IJavaProject javaProject= JavaCore.create(resource.getProject());
		if (javaProject == null) {
			return null;
		}
		IJavaElement javaElement= JavaCore.create(resource, javaProject);
		if (javaElement == null || !javaElement.exists()) {
			return null;
		}
		return javaElement;
	}

	/**
	 * Converts the shape modification to use Java elements.
	 *
	 *
	 * @param currentChildren
	 *            The set of current children that would be contributed or refreshed in the viewer.
	 * @return returns true if the conversion took place
	 */
	private boolean convertToJavaElements(Set<Object> currentChildren) {

		LinkedHashSet<Object> convertedChildren = new LinkedHashSet<>();
		for (Iterator<Object> childrenItr = currentChildren.iterator(); childrenItr
				.hasNext();) {
			Object child = childrenItr.next();
			// only convert IFolders and IFiles
			if (child instanceof IFolder || child instanceof IFile) {
				IJavaElement newChild = convert((IResource) child);
				if (newChild != null) {
					IJavaProject javaProject= newChild.getJavaProject();
					if (javaProject != null && javaProject.isOnClasspath(newChild)) {
						childrenItr.remove();
						convertedChildren.add(newChild);
					}
				}
			} else if (child instanceof IJavaProject) {
				childrenItr.remove();
				convertedChildren.add( ((IJavaProject)child).getProject());
			}
		}
		if (!convertedChildren.isEmpty()) {
			currentChildren.addAll(convertedChildren);
			return true;
		}
		return false;

	}

	/**
	 * Adapted from the Common Navigator Content Provider
	 *
	 * @param javaElements the java elements
	 * @param proposedChildren the proposed children
	 */
	private void customize(Object parent, Object[] javaElements, Set<Object> proposedChildren) {
		if (parent instanceof IContainer) {
			removePhysicalPackageFolders(javaElements, proposedChildren);
		}
		List<?> elementList= Arrays.asList(javaElements);
		for (Object element : proposedChildren) {
			IResource resource= null;
			if (element instanceof IResource) {
				resource= (IResource)element;
			} else if (element instanceof IAdaptable) {
				resource= ((IAdaptable)element).getAdapter(IResource.class);
			}
			if (resource != null) {
				int i= elementList.indexOf(resource);
				if (i >= 0) {
					javaElements[i]= null;
				}
			}
		}
		for (Object element : javaElements) {
			if (element instanceof IJavaElement) {
				IJavaElement cElement= (IJavaElement)element;
				IResource resource= cElement.getResource();
				if (resource != null) {
					proposedChildren.remove(resource);
				}
				proposedChildren.add(element);
			} else if (element instanceof FoldedResourceFolder folded) {
				boolean represented= proposedChildren.remove(folded.getFirstFolder());
				represented|= proposedChildren.remove(folded.getFolder());
				if (represented) {
					proposedChildren.add(folded);
				}
			} else if (element instanceof IFolder && parent instanceof IContainer) {
				// Generic resource content owns raw folders in the Project Explorer.
			} else if (element != null) {
				proposedChildren.add(element);
			}
		}
	}

	private boolean foldPipelinedResourceFolders(Object parent, Set<Object> children) {
		if (!(parent instanceof IContainer) || !isResourceFolderFoldingEnabled()) {
			return false;
		}
		return foldResourceFolderChildren(parent, children);
	}

	private boolean foldResourceFolderChildren(Object visibleParent, Set<Object> children) {
		boolean changed= false;
		for (Object child : List.copyOf(children)) {
			if (child instanceof IFolder folder) {
				try {
					IFolder folded= getFoldedResourceFolder(folder);
					if (!folder.equals(folded)) {
						children.remove(folder);
						children.add(new FoldedResourceFolder(visibleParent, folder, folded));
						changed= true;
					}
				} catch (CoreException e) {
					// leave the original folder unfolded
				}
			}
		}
		return changed;
	}

	private Object[] getFoldedResourceFolderChildren(FoldedResourceFolder parent) {
		try {
			IFolder folder= parent.getFolder();
			Set<Object> children= new LinkedHashSet<>(Arrays.asList(folder.members()));
			customize(folder, super.getChildren(folder), children);
			if (isResourceFolderFoldingEnabled()) {
				foldResourceFolderChildren(parent, children);
			}
			return children.toArray();
		} catch (CoreException e) {
			return new Object[0];
		}
	}

	private Object createFoldedFolder(Object parent, IFolder folder) {
		IResource parentResource= null;
		if (parent instanceof IJavaElement javaElement) {
			parentResource= javaElement.getResource();
		} else if (parent instanceof IResource resource) {
			parentResource= resource;
		}
		if (!(parentResource instanceof IContainer container)) {
			return folder;
		}
		IFolder first= folder;
		while (first.getParent() instanceof IFolder ancestor && !ancestor.equals(container)) {
			first= ancestor;
		}
		if (!first.getParent().equals(container)) {
			return folder;
		}
		return first.equals(folder) ? folder : new FoldedResourceFolder(parent, first, folder);
	}

	private static void removePhysicalPackageFolders(Object[] javaElements, Set<Object> proposedChildren) {
		for (Object child : List.copyOf(proposedChildren)) {
			if (child instanceof IFolder folder && isRepresentedByPackage(javaElements, folder)) {
				proposedChildren.remove(folder);
			}
		}
	}

	private static boolean isRepresentedByPackage(Object[] javaElements, IFolder folder) {
		for (Object element : javaElements) {
			if (element instanceof IPackageFragment fragment && fragment.getResource() instanceof IResource resource
					&& folder.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	private IFolder getFoldedResourceFolder(IFolder folder) throws CoreException {
		return getFoldedResourceFolder(folder, this::isVisible);
	}

	static IFolder getFoldedResourceFolder(IFolder folder, BiPredicate<Object, Object> visibility) throws CoreException {
		IFolder child;
		while (!isPackageFragmentRoot(folder)
				&& (child= getSingleVisibleResourceFolderChild(folder, visibility)) != null) {
			folder= child;
		}
		return folder;
	}

	private static boolean isPackageFragmentRoot(IFolder folder) {
		IJavaElement javaElement= JavaCore.create(folder);
		return javaElement instanceof IPackageFragmentRoot && javaElement.exists();
	}

	private static IFolder getSingleVisibleResourceFolderChild(IFolder folder,
			BiPredicate<Object, Object> visibility) throws CoreException {
		IFolder result= null;
		for (IResource child : folder.members()) {
			if (visibility.test(folder, child)) {
				if (!(child instanceof IFolder childFolder) || result != null) {
					return null;
				}
				result= childFolder;
			}
		}
		return result;
	}

	@Override
	protected boolean isVisible(Object parent, Object child) {
		if (fViewer == null) {
			return true;
		}
		for (ViewerFilter filter : fViewer.getFilters()) {
			if (!(filter instanceof FoldedResourceFolderFilter) && !filter.select(fViewer, parent, child)) {
				return false;
			}
		}
		return true;
	}



	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
		return convertToJavaElements(refreshSynchronization.getRefreshTargets());

	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
		return convertToJavaElements(updateSynchronization.getRefreshTargets());
	}

	@Override
	protected void postAdd(final Object parent, final Object element, Collection<Runnable> runnables) {
		if (parent instanceof IJavaModel)
			super.postAdd(((IJavaModel) parent).getWorkspace().getRoot(), element, runnables);
		else if (parent instanceof IJavaProject)
			super.postAdd( ((IJavaProject)parent).getProject(), element, runnables);
		else
			super.postAdd(parent, element, runnables);
	}


	@Override
	protected void postRefresh(final List<Object> toRefresh, final boolean updateLabels, Collection<Runnable> runnables) {
		int size= toRefresh.size();
		for (int i= 0; i < size; i++) {
			Object element= toRefresh.get(i);
			if (element instanceof IJavaProject) {
				toRefresh.set(i, ((IJavaProject) element).getProject());
			}
		}
		for (Iterator<Object> iter = toRefresh.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof IJavaModel) {
				iter.remove();
				toRefresh.add(((IJavaModel)element).getWorkspace().getRoot());
				super.postRefresh(toRefresh, updateLabels, runnables);
				return;
			}
		}
		super.postRefresh(toRefresh, updateLabels, runnables);
	}

}
