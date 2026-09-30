/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.ui.actions.ILaunchable;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.navigator.CommonViewerComparator;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.NavigatorContentServiceFactory;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementComparator;

import org.eclipse.jdt.internal.ui.actions.SimpleSelectionProvider;
import org.eclipse.jdt.internal.ui.filters.NamePatternFilter;
import org.eclipse.jdt.internal.ui.navigator.FoldedResourceFolder;
import org.eclipse.jdt.internal.ui.navigator.FoldedResourceFolderFilter;
import org.eclipse.jdt.internal.ui.navigator.FoldedResourceRenameOperation;
import org.eclipse.jdt.internal.ui.navigator.FoldedResourceSelectionProvider;
import org.eclipse.jdt.internal.ui.navigator.JavaNavigatorActionProvider;
import org.eclipse.jdt.internal.ui.navigator.JavaNavigatorContentProvider;
import org.eclipse.jdt.internal.ui.navigator.JavaFileLinkHelper;
import org.eclipse.jdt.internal.ui.navigator.JavaNavigatorLabelProvider;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerLabelProvider;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * Tests for the PackageExplorerContentProvider.
 *
 * @since 2.1
 */
public class ContentProviderTests3{

	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	private IPackageFragmentRoot fRoot1;
	private IPackageFragment fPack1;
	private IPackageFragment fPack2;
	private IPackageFragment fPack4;
	private IPackageFragment fPack3;
	private IWorkspace fWorkspace;
	private IWorkbench fWorkbench;
	private MockPluginView fMyPart;

	private ITreeContentProvider fProvider;
	private IPackageFragmentRoot fArchiveFragmentRoot;
	private IPackageFragment fPackJunit;
	private IPackageFragment fPackJunitSamples;
	private IPackageFragment fPackJunitSamplesMoney;

	private IPackageFragment fPack6;
	private IPackageFragment fPackJunitExtentions;
	private IPackageFragment fPackJunitFramework;
	private IPackageFragment fPackJunitRunner;
	private IPackageFragment fPackJunitTextUi;
	private IPackageFragment fPackJunitUi;
	private IPackageFragment fPackJunitTests;

	private ICompilationUnit fCUIMoney;
	private ICompilationUnit fCUMoney;
	private ICompilationUnit fCUMoneyBag;
	private ICompilationUnit fCUMoneyTest;
	private ICompilationUnit fCU1;
	private ICompilationUnit fCU2;
	private ICompilationUnit fCU3;
	private IFile fFile1;
	private IFile fFile2;
	private IFolder dotSettings;

	private IWorkbenchPage page;
	private IPackageFragmentRoot jdk;
	private boolean fEnableAutoBuildAfterTesting;

	//---------Test for getChildren-------------------

	@Test
	public void testGetChildrenProjectWithSourceFolders() throws Exception{
		Object[] expectedChildren= new Object[]{fRoot1, fFile1, fFile2, dotSettings, jdk};
		Object[] children= fProvider.getChildren(fJProject2);
		assertTrue(compareArrays(children, expectedChildren), "Wrong children found for project with folding");//$NON-NLS-1$
	}


	@Test
	public void testGetChildrentMidLevelFragment() throws Exception{
		Object[] expectedChildren= new Object[]{fPack4, fPack6};
		Object[] children= fProvider.getChildren(fPack3);
		assertTrue(compareArrays(children, expectedChildren),"Wrong children found for PackageFragment with folding");//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragment() throws Exception{
		Object[] expectedChildren= new Object[]{};
		Object[] children= fProvider.getChildren(fPack1);
		assertTrue(compareArrays(children, expectedChildren),"Wrong children found for PackageFragment with folding");//$NON-NLS-1$

	}

	@Test
	public void testGetChildrenBottomLevelFragmentWithCU() throws Exception{
		Object[] expectedChildren= new Object[]{fCU1};
		Object[] children= fProvider.getChildren(fPack2);
		assertTrue(compareArrays(children, expectedChildren),"Wrong children found for PackageFragment with folding");	//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenBottomLevelFragmentInArchive() throws Exception{
		Object[] expectedChildren= new Object[]{fCUIMoney, fCUMoney, fCUMoneyBag, fCUMoneyTest};
		Object[] children= fProvider.getChildren(fPackJunitSamplesMoney);
		assertTrue(compareArrays(children, expectedChildren), "wrong children found for a bottom PackageFragment in PackageFragmentRoot Archive with folding");	//$NON-NLS-1$
	}

	@Test
	public void testGetChildrenSource() throws Exception{
		Object[] expectedChildren= new Object[]{fPack1,fPack2,fPack3, fRoot1.getPackageFragment("")};//$NON-NLS-1$
		Object[] children= fProvider.getChildren(fRoot1);
		assertTrue(compareArrays(children, expectedChildren), "Wrong children found for PackageFragmentRoot with folding");	//$NON-NLS-1$
	}

	@Test
	public void testFoldResourceFolders() throws Exception {
		IPackageFragmentRoot root= createResourceRoot();
		IFolder leaf= createFolderHierarchy((IFolder) root.getResource(), "com", "example", "application"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Object[] children= fProvider.getChildren(root);

		assertTrue(compareArrays(children, new Object[] { leaf }), "Wrong folded resource folder"); //$NON-NLS-1$
		assertEquals(root, fProvider.getParent(leaf), "Wrong parent for folded resource folder"); //$NON-NLS-1$
		assertEquals("com/example/application", getLabel(leaf), "Wrong folded resource folder label"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testVisibleResourcePreventsFolderFolding() throws Exception {
		IPackageFragmentRoot root= createResourceRoot();
		IFolder middle= createFolderHierarchy((IFolder) root.getResource(), "com", "example"); //$NON-NLS-1$ //$NON-NLS-2$
		createFolderHierarchy(middle, "application"); //$NON-NLS-1$
		middle.getFile("config.properties").create(new ByteArrayInputStream(new byte[0]), true, null); //$NON-NLS-1$

		Object[] children= fProvider.getChildren(root);

		assertTrue(compareArrays(children, new Object[] { middle }), "Visible resource did not stop folder folding"); //$NON-NLS-1$
		assertEquals("com/example", getLabel(middle), "Wrong partially folded resource folder label"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFilteredResourceDoesNotPreventFolderFolding() throws Exception {
		IPackageFragmentRoot root= createResourceRoot();
		IFolder middle= createFolderHierarchy((IFolder) root.getResource(), "com", "example"); //$NON-NLS-1$ //$NON-NLS-2$
		IFolder leaf= createFolderHierarchy(middle, "application"); //$NON-NLS-1$
		middle.getFile(".DS_Store").create(new ByteArrayInputStream(new byte[0]), true, null); //$NON-NLS-1$
		NamePatternFilter filter= new NamePatternFilter();
		filter.setPatterns(new String[] { ".*" }); //$NON-NLS-1$
		fMyPart.getTreeViewer().addFilter(filter);

		Object[] children= fProvider.getChildren(root);

		assertTrue(compareArrays(children, new Object[] { leaf }), "Filtered resource prevented folder folding"); //$NON-NLS-1$
	}

	@Test
	public void testResourceFoldersAreNotFoldedWhenResourceFolderFoldingIsDisabled() throws Exception {
		IPackageFragmentRoot root= createResourceRoot();
		IFolder top= createFolderHierarchy((IFolder) root.getResource(), "com"); //$NON-NLS-1$
		createFolderHierarchy(top, "example", "application"); //$NON-NLS-1$ //$NON-NLS-2$
		fMyPart.setResourceFolderFolding(false);

		Object[] children= fProvider.getChildren(root);

		assertTrue(compareArrays(children, new Object[] { top }), "Resource folder was folded while folding was disabled"); //$NON-NLS-1$
	}

	@Test
	public void testFoldResourceFoldersInProjectExplorer() throws Exception {
		IFolder root= fJProject2.getProject().getFolder("projectExplorerResources"); //$NON-NLS-1$
		root.create(true, true, null);
		IFolder src= createFolderHierarchy(root, "src"); //$NON-NLS-1$
		IFolder main= createFolderHierarchy(src, "main"); //$NON-NLS-1$
		IFolder java= createFolderHierarchy(main, "java"); //$NON-NLS-1$
		IFolder eu= createFolderHierarchy(java, "eu"); //$NON-NLS-1$
		IFolder javaPackageLeaf= createFolderHierarchy(eu, "example", "application"); //$NON-NLS-1$ //$NON-NLS-2$
		IFile javaFile= javaPackageLeaf.getFile("Example.java"); //$NON-NLS-1$
		javaFile.create(new ByteArrayInputStream(
				"package eu.example.application; class Example {}".getBytes(StandardCharsets.UTF_8)), true, null); //$NON-NLS-1$
		IFolder resources= createFolderHierarchy(main, "resources"); //$NON-NLS-1$
		IFile configFile= resources.getFile("caffeine-jcache.conf"); //$NON-NLS-1$
		configFile.create(new ByteArrayInputStream(new byte[0]), true, null);
		IFolder db= createFolderHierarchy(resources, "db"); //$NON-NLS-1$
		IFolder migration= createFolderHierarchy(db, "migration"); //$NON-NLS-1$
		IFile migrationFile= migration.getFile("V1.sql"); //$NON-NLS-1$
		migrationFile.create(new ByteArrayInputStream(new byte[0]), true, null);
		IFolder webapp= createFolderHierarchy(main, "webapp"); //$NON-NLS-1$
		IFolder loose= createFolderHierarchy(root, "loose"); //$NON-NLS-1$
		IFolder nested= createFolderHierarchy(loose, "nested"); //$NON-NLS-1$
		IFolder leaf= createFolderHierarchy(nested, "leaf"); //$NON-NLS-1$
		leaf.getFile("content.txt").create(new ByteArrayInputStream(new byte[0]), true, null); //$NON-NLS-1$
		IFolder emptyFirst= createFolderHierarchy(root, "empty"); //$NON-NLS-1$
		createFolderHierarchy(emptyFirst, "leaf"); //$NON-NLS-1$
		IFolder alphaFirst= createFolderHierarchy(root, "alpha"); //$NON-NLS-1$
		IFolder alphaLeaf= createFolderHierarchy(alphaFirst, "zulu"); //$NON-NLS-1$
		alphaLeaf.getFile("content.txt").create(new ByteArrayInputStream(new byte[0]), true, null); //$NON-NLS-1$
		IFolder betaFirst= createFolderHierarchy(root, "bravo"); //$NON-NLS-1$
		IFolder betaLeaf= createFolderHierarchy(betaFirst, "alpha"); //$NON-NLS-1$
		betaLeaf.getFile("content.txt").create(new ByteArrayInputStream(new byte[0]), true, null); //$NON-NLS-1$
		JavaProjectHelper.addSourceContainer(fJProject2, java.getProjectRelativePath().toString());
		IPackageFragmentRoot resourceRoot= JavaProjectHelper.addSourceContainer(fJProject2,
				resources.getProjectRelativePath().toString(), new Path[0],
				new Path[] { new Path("**") }); //$NON-NLS-1$
		JavaNavigatorContentProvider provider= createJavaNavigatorContentProvider();
		try {
			fMyPart.getTreeViewer().setInput(root);
			provider.inputChanged(fMyPart.getTreeViewer(), null, root);
			fMyPart.getTreeViewer().expandAll();
			TreePath[] expandedPaths= fMyPart.getTreeViewer().getExpandedTreePaths();
			assertTrue(expandedPaths.length > 0, "Test viewer did not contain an expanded path"); //$NON-NLS-1$
			fMyPart.setCollapseOnRefresh(true);
			fMyPart.clear();
			fMyPart.getTreeViewer().getControl().getDisplay().asyncExec(fMyPart.getTreeViewer()::collapseAll);
			provider.interceptRemove(new PipelinedShapeModification(null,
					new LinkedHashSet<>(Set.of(db))));
			while (fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
				// Process the asynchronously scheduled fold refresh.
			}
			assertTrue(fMyPart.wasObjectRefreshed(root),
					"A shape change without a parent did not refresh the viewer input"); //$NON-NLS-1$
			assertEquals(List.of(expandedPaths), List.of(fMyPart.getTreeViewer().getExpandedTreePaths()),
					"Fold refresh did not preserve the expanded tree paths"); //$NON-NLS-1$
			fMyPart.setCollapseOnRefresh(false);

			Set<Object> additions= new LinkedHashSet<>(Set.of(loose));
			provider.interceptAdd(new PipelinedShapeModification(root, additions));
			assertEquals(1, additions.size(), "Incremental addition retained the unfolded resource folder"); //$NON-NLS-1$
			assertTrue(additions.iterator().next() instanceof FoldedResourceFolder,
					"Incremental addition did not fold the resource folder"); //$NON-NLS-1$
			assertEquals("loose/nested/leaf", additions.iterator().next().toString(), //$NON-NLS-1$
					"Incremental addition produced the wrong folded path"); //$NON-NLS-1$

			Set<Object> scheduledRefreshes= new LinkedHashSet<>();
			JavaNavigatorContentProvider trackingProvider= new JavaNavigatorContentProvider() {
				@Override
				protected boolean isResourceFolderFoldingEnabled() {
					return true;
				}

				@Override
				protected void scheduleFoldRefresh(Object parent) {
					scheduledRefreshes.add(parent);
				}
			};
			trackingProvider.interceptAdd(new PipelinedShapeModification(resources,
					new LinkedHashSet<>(Set.of(db))));
			assertEquals(Set.of(resources, resourceRoot), scheduledRefreshes,
					"Adding a resource did not refresh both physical and Java projections"); //$NON-NLS-1$
			scheduledRefreshes.clear();
			trackingProvider.interceptRemove(new PipelinedShapeModification(resources,
					new LinkedHashSet<>(Set.of(db))));
			assertEquals(Set.of(resources, resourceRoot), scheduledRefreshes,
					"Removing a resource did not refresh both physical and Java projections"); //$NON-NLS-1$

			Set<Object> children= new LinkedHashSet<>();
			children.add(src);

			provider.getPipelinedChildren(root, children);

			assertEquals(1, children.size(), "Folded folder did not replace its generic resource ancestor"); //$NON-NLS-1$
			FoldedResourceFolder foldedMain= (FoldedResourceFolder) children.iterator().next();
			assertEquals(main, foldedMain.getAdapter(IFolder.class), "Folded folder adapted to the wrong resource"); //$NON-NLS-1$
			assertEquals("src/main", foldedMain.toString(), "Wrong folded Project Explorer label"); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals(root, provider.getPipelinedParent(foldedMain, null), "Wrong folded Project Explorer parent"); //$NON-NLS-1$

			children= new LinkedHashSet<>(Set.of(provider.getChildren(foldedMain)));

			assertEquals(Set.of(java, resources, webapp), children, "Project Explorer children were folded inconsistently"); //$NON-NLS-1$

			children= new LinkedHashSet<>(Set.of(eu));
			provider.getPipelinedChildren(java, children);

			assertEquals(Set.of(JavaCore.create(javaPackageLeaf)), children,
					"Physical package hierarchy leaked beside its Java package"); //$NON-NLS-1$
			Object javaFileParent= provider.getParent(javaFile);
			assertTrue(javaFileParent instanceof IPackageFragment, "Java source did not have a package parent"); //$NON-NLS-1$
			assertEquals(javaFileParent, provider.getPipelinedParent(javaFile, javaPackageLeaf),
					"Link with Editor moved Java source to the physical resource projection"); //$NON-NLS-1$

			children= new LinkedHashSet<>(Set.of(db, configFile));
			provider.getPipelinedChildren(resources, children);

			FoldedResourceFolder physicalMigration= children.stream()
					.filter(FoldedResourceFolder.class::isInstance)
					.map(FoldedResourceFolder.class::cast)
					.findFirst()
					.orElseThrow();
			assertEquals(Set.of(physicalMigration, configFile), children, "Physical resource hierarchy was not folded"); //$NON-NLS-1$
			assertEquals(migration, physicalMigration.getAdapter(IFolder.class), "Physical fold did not adapt to its leaf folder"); //$NON-NLS-1$
			assertNull(physicalMigration.getAdapter(IContainer.class),
					"Physical fold leaked into generic container content extensions"); //$NON-NLS-1$
			assertEquals(migration, physicalMigration.getAdapter(IResource.class),
					"Resource actions did not receive the folded folder's leaf resource"); //$NON-NLS-1$
			assertEquals("db/migration", physicalMigration.toString(), "Wrong physical folded-folder label"); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals(resources, provider.getPipelinedParent(physicalMigration, null), "Wrong physical folded-folder parent"); //$NON-NLS-1$

			Set<Object> directChildren= new LinkedHashSet<>(Arrays.asList(provider.getChildren(resources)));
			assertEquals(Set.of(configFile), directChildren,
					"Java content contribution retained the unfolded resource folder"); //$NON-NLS-1$
			directChildren.addAll(children);
			assertEquals(Set.of(physicalMigration, configFile), directChildren,
					"First-class and pipelined content produced duplicate resource folders"); //$NON-NLS-1$

			ViewerFilter foldedFolderFilter= new FoldedResourceFolderFilter();
			Object[] mergedChildren= foldedFolderFilter.filter(fMyPart.getTreeViewer(),
					new TreePath(new Object[] { root, src, main, resources }), //
					new Object[] { physicalMigration, db, configFile });
			assertEquals(Set.of(physicalMigration, configFile), Set.of(mergedChildren),
					"A later content extension reintroduced the unfolded resource folder"); //$NON-NLS-1$

			assertEquals(physicalMigration, provider.getPipelinedParent(migrationFile, migration),
					"Link with Editor could not reconstruct the folded physical parent"); //$NON-NLS-1$
			assertEquals(new StructuredSelection(migrationFile),
					new JavaFileLinkHelper().findSelection(new FileEditorInput(migrationFile)),
					"Link with Editor did not select a non-Java resource"); //$NON-NLS-1$
			JavaElementComparator comparator= new JavaElementComparator();
			assertEquals(comparator.category(db), comparator.category(physicalMigration),
					"Folded resource folder was not sorted as a folder"); //$NON-NLS-1$
			assertTrue(comparator.compare(fMyPart.getTreeViewer(), physicalMigration, configFile) < 0,
					"Folded resource folder was sorted after a file"); //$NON-NLS-1$

			Set<Object> sortedFolds= new LinkedHashSet<>(Set.of(alphaFirst, betaFirst));
			provider.getPipelinedChildren(root, sortedFolds);
			FoldedResourceFolder alphaFold= sortedFolds.stream()
					.filter(FoldedResourceFolder.class::isInstance)
					.map(FoldedResourceFolder.class::cast)
					.filter(folder -> folder.toString().equals("alpha/zulu")) //$NON-NLS-1$
					.findFirst()
					.orElseThrow();
			FoldedResourceFolder betaFold= sortedFolds.stream()
					.filter(FoldedResourceFolder.class::isInstance)
					.map(FoldedResourceFolder.class::cast)
					.filter(folder -> folder.toString().equals("bravo/alpha")) //$NON-NLS-1$
					.findFirst()
					.orElseThrow();
			assertTrue(comparator.compare(fMyPart.getTreeViewer(), alphaFold, betaFold) < 0,
					"Folded folders were sorted by their leaf names instead of their full labels"); //$NON-NLS-1$

			Set<Object> emptyFoldChildren= new LinkedHashSet<>(Set.of(emptyFirst));
			provider.getPipelinedChildren(root, emptyFoldChildren);
			FoldedResourceFolder emptyFold= (FoldedResourceFolder) emptyFoldChildren.iterator().next();
			assertFalse(provider.hasChildren(emptyFold), "Empty folded folder reported children"); //$NON-NLS-1$
			assertEquals(0, provider.getChildren(emptyFold).length, "Empty folded folder returned children"); //$NON-NLS-1$
			INavigatorContentService contentService= NavigatorContentServiceFactory.INSTANCE
					.createContentService("org.eclipse.ui.navigator.ProjectExplorer"); //$NON-NLS-1$
			try {
				contentService.getActivationService().activateExtensions(new String[] {
						"org.eclipse.jdt.java.ui.javaContent", //$NON-NLS-1$
						"org.eclipse.ui.navigator.resources.nested.nestedProjectContentProvider" }, false); //$NON-NLS-1$
				assertTrue(Arrays.stream(contentService.getFilterService().getVisibleFilters(true)) //
						.anyMatch(FoldedResourceFolderFilter.class::isInstance),
						"Common Navigator did not activate the folded resource-folder filter"); //$NON-NLS-1$
				assertEquals("db/migration", new JavaNavigatorLabelProvider().getText(physicalMigration), //$NON-NLS-1$
						"Java content extension lost the folded path label"); //$NON-NLS-1$
				CommonViewerComparator commonComparator= new CommonViewerComparator();
				commonComparator.setContentService(contentService);
				TreePath parentPath= new TreePath(new Object[] { fJProject2.getProject() });
				assertTrue(commonComparator.compare(fMyPart.getTreeViewer(), parentPath,
						fJProject1.getProject(), physicalMigration) < 0,
						"Common Navigator sorted a folded folder before a nested project"); //$NON-NLS-1$
				assertTrue(commonComparator.compare(fMyPart.getTreeViewer(), parentPath, alphaFold, betaFold) < 0,
						"Common Navigator sorted folded folders by their leaf names"); //$NON-NLS-1$
			} finally {
				contentService.dispose();
			}
			assertEquals("db/migration", physicalMigration.getAdapter(IWorkbenchAdapter.class) //$NON-NLS-1$
					.getLabel(physicalMigration), "Workbench adapter lost the folded path label"); //$NON-NLS-1$
			assertTrue(Platform.getAdapterManager().hasAdapter(physicalMigration, ILaunchable.class.getName()),
					"Folded resource folder was not considered launchable"); //$NON-NLS-1$

			Object[] javaResourceChildren= provider.getChildren(resourceRoot);
			FoldedResourceFolder javaMigration= Arrays.stream(javaResourceChildren)
					.filter(FoldedResourceFolder.class::isInstance)
					.map(FoldedResourceFolder.class::cast)
					.findFirst()
					.orElseThrow();
			assertEquals(Set.of(javaMigration, configFile), Set.of(javaResourceChildren),
					"Resource folder was no longer folded in the Java Resources projection"); //$NON-NLS-1$
			assertEquals(migration, javaMigration.getAdapter(IFolder.class), "Java Resources fold did not adapt to its leaf folder"); //$NON-NLS-1$
			assertEquals("db/migration", javaMigration.toString(), "Wrong Java Resources folded-folder label"); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals(resourceRoot, provider.getPipelinedParent(javaMigration, null), "Wrong Java Resources folded-folder parent"); //$NON-NLS-1$
			assertFalse(physicalMigration.equals(javaMigration), "Physical and Java Resources folds shared their identity"); //$NON-NLS-1$
			Set<Object> combinedResourceRootChildren= new LinkedHashSet<>(Set.of(db));
			provider.getPipelinedChildren(resourceRoot, combinedResourceRootChildren);
			assertEquals(Set.of(javaMigration, configFile), combinedResourceRootChildren,
					"Generic and Java content providers produced duplicate folded resource folders"); //$NON-NLS-1$

			SimpleSelectionProvider viewerSelectionProvider= new SimpleSelectionProvider();
			viewerSelectionProvider.setSelection(new StructuredSelection(physicalMigration));
			FoldedResourceSelectionProvider actionSelectionProvider=
					new FoldedResourceSelectionProvider(viewerSelectionProvider);
			assertEquals(new StructuredSelection(db), actionSelectionProvider.getSelection(),
					"Actions did not receive the folded folder's first resource"); //$NON-NLS-1$
			assertEquals(new StructuredSelection(physicalMigration), viewerSelectionProvider.getSelection(),
					"Action selection conversion changed the viewer selection"); //$NON-NLS-1$

			AtomicReference<SelectionChangedEvent> translatedEvent= new AtomicReference<>();
			AtomicInteger eventCount= new AtomicInteger();
			ISelectionChangedListener listener= event -> {
				translatedEvent.set(event);
				eventCount.incrementAndGet();
			};
			actionSelectionProvider.addSelectionChangedListener(listener);
			viewerSelectionProvider.setSelection(new StructuredSelection(physicalMigration));
			assertSame(actionSelectionProvider, translatedEvent.get().getSelectionProvider(),
					"Translated event exposed the viewer selection provider"); //$NON-NLS-1$
			assertEquals(new StructuredSelection(db), translatedEvent.get().getSelection(),
					"Selection event did not contain the first resource"); //$NON-NLS-1$
			actionSelectionProvider.removeSelectionChangedListener(listener);
			viewerSelectionProvider.setSelection(new StructuredSelection(physicalMigration));
			assertEquals(1, eventCount.get(), "Removed action listener was still notified"); //$NON-NLS-1$
		} finally {
			provider.dispose();
		}
	}

	@Test
	public void testJavaNavigatorActionProviderAcceptsNullContext() {
		JavaNavigatorActionProvider actionProvider= new JavaNavigatorActionProvider();
		actionProvider.setContext(null);
	}

	@Test
	public void testRenameCompleteFoldedResourcePath() throws Exception {
		IFolder parent= fJProject2.getProject().getFolder("renameFold"); //$NON-NLS-1$
		parent.create(true, true, null);
		IFolder first= createFolderHierarchy(parent, "alpha"); //$NON-NLS-1$
		IFolder leaf= createFolderHierarchy(first, "bravo", "charlie"); //$NON-NLS-1$ //$NON-NLS-2$
		leaf.getFile("content.txt").create(new ByteArrayInputStream(new byte[0]), true, null); //$NON-NLS-1$
		Path oldPath= new Path("alpha/bravo/charlie"); //$NON-NLS-1$
		Path newPath= new Path("delta/echo/foxtrot"); //$NON-NLS-1$
		FoldedResourceRenameOperation operation=
				new FoldedResourceRenameOperation(parent, oldPath, newPath, "Rename folded folder"); //$NON-NLS-1$

		assertTrue(FoldedResourceRenameOperation.validate(parent, oldPath, newPath).isOK());
		assertTrue(operation.execute(new NullProgressMonitor(), null).isOK());
		assertFalse(parent.getFolder(oldPath).exists(), "Old folded path still existed after rename"); //$NON-NLS-1$
		assertTrue(parent.getFile(newPath.append("content.txt")).exists(), "Content was not moved with the folded path"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(operation.undo(new NullProgressMonitor(), null).isOK());
		assertTrue(parent.getFile(oldPath.append("content.txt")).exists(), "Undo did not restore the old folded path"); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(parent.getFolder(newPath).exists(), "Undo retained the renamed folded path"); //$NON-NLS-1$

		assertTrue(operation.redo(new NullProgressMonitor(), null).isOK());
		assertTrue(parent.getFile(newPath.append("content.txt")).exists(), "Redo did not restore the renamed folded path"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetChildrenArchive(){
		Object[] expectedChildren= new Object[]{fPackJunit, fArchiveFragmentRoot.getPackageFragment("")};//$NON-NLS-1$
		Object[] children= fProvider.getChildren(fArchiveFragmentRoot);
		assertTrue(compareArrays(children, expectedChildren), "Wrong child found for PackageFragmentRoot Archive with folding");//$NON-NLS-1$

	}

	//---------------Get Parent Tests-----------------------------

	@Test
	public void testGetParentArchive() throws Exception{
		Object parent= fProvider.getParent(fArchiveFragmentRoot);
		assertSame(parent, fJProject1, "Wrong parent found for PackageFragmentRoot Archive with folding");//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragmentInArchive() throws Exception{
		Object expectedParent= fPackJunit;
		Object parent= fProvider.getParent(fPackJunitSamples);
		assertEquals(expectedParent, parent, "Wrong parent found for a top level PackageFragment in an Archive with folding");	//$NON-NLS-1$
	}

	@Test
	public void testGetParentTopLevelFragment() throws Exception{
		Object expectedParent= fRoot1;
		Object parent= fProvider.getParent(fPack3);
		assertEquals(expectedParent, parent, "Wrong parent found for a top level PackageFragment with folding"); //$NON-NLS-1$
	}

	@Test
	public void testGetParentFoldedBottomFragment() throws Exception{
		Object expectedParent= fRoot1;
		Object parent= fProvider.getParent(fPack3);
		assertEquals(expectedParent, parent, "Wrong parent found for a top level PackageFragment with folding");//$NON-NLS-1$

	}

	@Test
	public void testGetParentMidLevelFragment() throws Exception{
		Object expectedParent= fPack3;
		Object parent= fProvider.getParent(fPack4);
		assertEquals(expectedParent, parent, "Wrong parent found for a NON top level PackageFragment with folding");//$NON-NLS-1$
	}

	@Test
	public void testDeleteBottomLevelFragmentFolding() throws Exception {

		//send a delta indicating fragment deleted
		IJavaElementDelta delta= TestDelta.createDelta(fPack4, IJavaElementDelta.REMOVED);
		sendEvent(delta);

		assertTrue(fMyPart.hasRefreshHappened(), "Refresh happened"); //$NON-NLS-1$
		assertTrue(fMyPart.wasObjectRefreshed(fRoot1), "Correct Refresh"); //$NON-NLS-1$
		assertEquals(1, fMyPart.getRefreshedObject().size(), "Single refresh"); //$NON-NLS-1$
	}


	protected void sendEvent(IJavaElementDelta delta) {
		IElementChangedListener listener= (IElementChangedListener) fProvider;
		listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));

		//force events from dispaly
		while(fMyPart.getTreeViewer().getControl().getDisplay().readAndDispatch()) {
		}
	}

	@Test
	public void testAddBottomLevelFragmentFolding() throws Exception {
		IPackageFragment test= fRoot1.createPackageFragment("test", true, null); //$NON-NLS-1$

		//send a delta indicating fragment deleted
		IJavaElementDelta delta= TestDelta.createDelta(test, IJavaElementDelta.ADDED);
		sendEvent(delta);

		assertFalse(fMyPart.hasAddHappened(), "No add happened"); //$NON-NLS-1$
		assertTrue(fMyPart.hasRefreshHappened(), "Refresh happened"); //$NON-NLS-1$
		assertTrue(fMyPart.wasObjectRefreshed(fRoot1), "Correct Refresh"); //$NON-NLS-1$
		assertEquals(1, fMyPart.getRefreshedObject().size(), "Single refresh"); //$NON-NLS-1$
	}

	@Test
	public void testAddTopLevelFoldedResourceRefreshesProject() throws Exception {
		IFolder first= fJProject2.getProject().getFolder("outputs"); //$NON-NLS-1$
		first.create(true, true, null);
		IFolder leaf= createFolderHierarchy(first, "result"); //$NON-NLS-1$
		assertTrue(leaf.exists());
		fMyPart.clear();

		IResourceDelta resourceDelta= createResourceDelta(first, IResourceDelta.ADDED);
		TestDelta projectDelta= new TestDelta(IJavaElementDelta.CHANGED, fJProject2);
		projectDelta.setResourceDeltas(new IResourceDelta[] { resourceDelta });
		TestDelta modelDelta= new TestDelta(IJavaElementDelta.CHANGED, fJProject2.getJavaModel());
		modelDelta.setAffectedChildren(new IJavaElementDelta[] { projectDelta });

		sendEvent(modelDelta);

		assertFalse(fMyPart.hasAddHappened(), "Foldable top-level folder was added without recomputing folding"); //$NON-NLS-1$
		assertTrue(fMyPart.wasObjectRefreshed(fJProject2), "Java project was not refreshed"); //$NON-NLS-1$
	}

	@Test
	public void testRemoveTopLevelFoldedResourceRefreshesProject() throws Exception {
		IFolder first= fJProject2.getProject().getFolder("outputs"); //$NON-NLS-1$
		first.create(true, true, null);
		createFolderHierarchy(first, "result"); //$NON-NLS-1$
		fMyPart.clear();

		TestDelta projectDelta= new TestDelta(IJavaElementDelta.CHANGED, fJProject2);
		projectDelta.setResourceDeltas(new IResourceDelta[] {
				createResourceDelta(first, IResourceDelta.REMOVED) });
		TestDelta modelDelta= new TestDelta(IJavaElementDelta.CHANGED, fJProject2.getJavaModel());
		modelDelta.setAffectedChildren(new IJavaElementDelta[] { projectDelta });

		sendEvent(modelDelta);

		assertTrue(fMyPart.wasObjectRefreshed(fJProject2), "Removing a folded folder did not refresh the Java project"); //$NON-NLS-1$
	}

	private IResourceDelta createResourceDelta(IResource resource, int kind) {
		return (IResourceDelta) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[] { IResourceDelta.class },
				(proxy, method, args) -> switch (method.getName()) {
					case "getKind" -> kind; //$NON-NLS-1$
					case "getResource" -> resource; //$NON-NLS-1$
					case "getAffectedChildren" -> new IResourceDelta[0]; //$NON-NLS-1$
					case "getFlags" -> 0; //$NON-NLS-1$
					default -> null;
				});
	}

	@Test
	public void testChangedTopLevelPackageFragmentFolding() throws Exception {
		//send a delta indicating fragment deleted
		IJavaElementDelta delta= TestDelta.createDelta(fPack3, IJavaElementDelta.CHANGED);
		sendEvent(delta);

		assertEquals(0, fMyPart.getRefreshedObject().size(), "No refreshs"); //$NON-NLS-1$
	}

	@Test
	public void testChangeBottomLevelPackageFragmentFolding() throws Exception {
		//send a delta indicating fragment deleted
		IJavaElementDelta delta= TestDelta.createDelta(fPack6, IJavaElementDelta.CHANGED);
		sendEvent(delta);

		assertEquals(0,fMyPart.getRefreshedObject().size(),  "No refreshs"); //$NON-NLS-1$
	}

	@Test
	public void testRemoveCUsFromPackageFragment() throws Exception{

		//send a delta indicating fragment deleted
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[] { fCU2, fCU3 }, fPack6, IJavaElementDelta.REMOVED);
		sendEvent(delta);

		// removing more than one CU now results in a refresh.
		assertEquals(1, fMyPart.getRefreshedObject().size(), "One refresh"); //$NON-NLS-1$
	}

	@Test
	public void testRemoveCUFromPackageFragment() throws Exception {

		//send a delta indicating fragment deleted
		IJavaElementDelta delta= TestDelta.createCUDelta(new ICompilationUnit[]{fCU2}, fPack6, IJavaElementDelta.REMOVED);
		sendEvent(delta);

		assertTrue(fMyPart.hasRemoveHappened(), "Remove happened"); //$NON-NLS-1$
		assertTrue(fMyPart.getRemovedObjects().contains(fCU2), "Correct refresh"); //$NON-NLS-1$
		assertEquals(0, fMyPart.getRefreshedObject().size(), "No refreshes"); //$NON-NLS-1$
	}


	/*
	 * @see TestCase#setUp()
	 */
	@BeforeEach
	public void setUp() throws Exception {

		fWorkspace= ResourcesPlugin.getWorkspace();
		assertNotNull(fWorkspace);
		IWorkspaceDescription workspaceDesc= fWorkspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(false);

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");//$NON-NLS-1$//$NON-NLS-2$
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");//$NON-NLS-1$//$NON-NLS-2$

		assertNotNull(fJProject1, "project1 null");//$NON-NLS-1$
		assertNotNull(fJProject2, "project2 null");//$NON-NLS-1$

		for (Object object : fJProject2.getNonJavaResources()) {
			if(object instanceof IFile){
				IFile file = (IFile) object;
				if(".classpath".equals(file.getName()))//$NON-NLS-1$
					fFile1= file;
				else if (".project".equals(file.getName()))//$NON-NLS-1$
					fFile2= file;
			} else if (object instanceof IFolder) {
				IFolder folder= (IFolder) object;
				if(".settings".equals(folder.getName())) {
					dotSettings= folder;
				}
			}
		}
		assertNotNull(fFile1);
		assertNotNull(fFile2);
		assertNotNull(dotSettings);

		//set up project #1 : External Jar and zip file
		jdk= JavaProjectHelper.addVariableRTJar(fJProject1, "JRE_LIB_TEST", null, null);//$NON-NLS-1$
		assertNotNull(jdk, "jdk not found");//$NON-NLS-1$

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);

		assertNotNull(junitSrcArchive, "junit src not found");//$NON-NLS-1$
		assertTrue(junitSrcArchive.exists(), "junit src not found");//$NON-NLS-1$

		fArchiveFragmentRoot= JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);//$NON-NLS-1$

		fPackJunit= fArchiveFragmentRoot.getPackageFragment("junit");//$NON-NLS-1$
		fPackJunitSamples= fArchiveFragmentRoot.getPackageFragment("junit.samples");//$NON-NLS-1$
		fPackJunitSamplesMoney= fArchiveFragmentRoot.getPackageFragment("junit.samples.money");//$NON-NLS-1$
		fPackJunitExtentions= fArchiveFragmentRoot.getPackageFragment("junit.extensions");//$NON-NLS-1$
		fPackJunitFramework= fArchiveFragmentRoot.getPackageFragment("junit.framework");//$NON-NLS-1$
		fPackJunitRunner= fArchiveFragmentRoot.getPackageFragment("junit.runner");//$NON-NLS-1$
		fPackJunitTests= fArchiveFragmentRoot.getPackageFragment("junit.tests");//$NON-NLS-1$
		fPackJunitTextUi= fArchiveFragmentRoot.getPackageFragment("junit.textui");//$NON-NLS-1$
		fPackJunitUi= fArchiveFragmentRoot.getPackageFragment("junit.ui");//$NON-NLS-1$

		assertNotNull(fPackJunit, "creating fPackJunit");//$NON-NLS-1$
		assertNotNull(fPackJunitSamples, "creating fPackJunitSamples");//$NON-NLS-1$
		assertNotNull(fPackJunitSamplesMoney,"creating fPackJunitSamplesMoney");//$NON-NLS-1$
		assertNotNull(fPackJunitExtentions, "");//$NON-NLS-1$
		assertNotNull(fPackJunitFramework,"");//$NON-NLS-1$
		assertNotNull(fPackJunitRunner,"");//$NON-NLS-1$
		assertNotNull(fPackJunitTests,"");//$NON-NLS-1$
		assertNotNull(fPackJunitTextUi,"");//$NON-NLS-1$
		assertNotNull(fPackJunitUi,"");//$NON-NLS-1$

		fCUIMoney= fPackJunitSamplesMoney.getCompilationUnit("IMoney.java");//$NON-NLS-1$
		fCUMoney= fPackJunitSamplesMoney.getCompilationUnit("Money.java");//$NON-NLS-1$
		fCUMoneyBag= fPackJunitSamplesMoney.getCompilationUnit("MoneyBag.java");//$NON-NLS-1$
		fCUMoneyTest= fPackJunitSamplesMoney.getCompilationUnit("MoneyTest.java");//$NON-NLS-1$

		File mylibJar= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		assertNotNull(mylibJar, "lib not found");//$NON-NLS-1$
		assertTrue(mylibJar.exists(), "lib not found");//$NON-NLS-1$
		JavaProjectHelper.addLibraryWithImport(fJProject1, Path.fromOSString(mylibJar.getPath()), null, null);

		//set up project #2: file system structure with in a source folder

		JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);//$NON-NLS-1$

		fRoot1= JavaProjectHelper.addSourceContainer(fJProject2, "src1");//$NON-NLS-1$
		fPack1= fRoot1.createPackageFragment("pack1", true, null);//$NON-NLS-1$
		fPack2= fRoot1.createPackageFragment("pack2", true, null);//$NON-NLS-1$
		fPack3= fRoot1.createPackageFragment("pack3",true,null);//$NON-NLS-1$
		fPack4= fRoot1.createPackageFragment("pack3.pack4", true,null);//$NON-NLS-1$
		fRoot1.createPackageFragment("pack3.pack5",true,null);//$NON-NLS-1$
		fPack6= fRoot1.createPackageFragment("pack3.pack5.pack6", true, null);//$NON-NLS-1$

		fCU1= fPack2.createCompilationUnit("Object.java", "", true, null);//$NON-NLS-1$//$NON-NLS-2$
		fCU2= fPack6.createCompilationUnit("Object.java","", true, null);//$NON-NLS-1$//$NON-NLS-2$
		fCU3= fPack6.createCompilationUnit("Jen.java","", true,null);//$NON-NLS-1$//$NON-NLS-2$

		//set up the mock view
		setUpMockView();
	}

	public void setUpMockView() throws Exception{


//		fWorkspace= ResourcesPlugin.getWorkspace();
//		assertNotNull(fWorkspace);

		fWorkbench= PlatformUI.getWorkbench();
		assertNotNull(fWorkbench);

		page= fWorkbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);

		//just testing to make sure my part can be created
		IViewPart myPart= new MockPluginView();
		assertNotNull(myPart);

		myPart= page.showView("org.eclipse.jdt.ui.tests.packageview.MockPluginView");//$NON-NLS-1$
		if (myPart instanceof MockPluginView) {
			fMyPart= (MockPluginView) myPart;
			//turn on folding
			fMyPart.setFolding(true);
			fMyPart.setResourceFolderFolding(true);
			fMyPart.setFlatLayout(false);
			// above call might cause a property change event being sent
			fMyPart.clear();
			fProvider= (ITreeContentProvider)fMyPart.getTreeViewer().getContentProvider();

		}else fail("Unable to get view");//$NON-NLS-1$

		assertNotNull(fProvider);
	}

	private IPackageFragmentRoot createResourceRoot() throws Exception {
		return JavaProjectHelper.addSourceContainer(
				fJProject2,
				"src/main/resources", //$NON-NLS-1$
				new Path[0],
				new Path[] { new Path("**") }); //$NON-NLS-1$
	}

	private JavaNavigatorContentProvider createJavaNavigatorContentProvider() {
		IExtensionStateModel stateModel= (IExtensionStateModel) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[] { IExtensionStateModel.class },
				(proxy, method, args) -> method.getReturnType() == boolean.class ? false : null);
		ICommonContentExtensionSite site= (ICommonContentExtensionSite) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[] { ICommonContentExtensionSite.class },
				(proxy, method, args) -> "getExtensionStateModel".equals(method.getName()) ? stateModel : null); //$NON-NLS-1$
		JavaNavigatorContentProvider provider= new JavaNavigatorContentProvider() {
			@Override
			protected boolean isResourceFolderFoldingEnabled() {
				return true;
			}
		};
		provider.init(site);
		return provider;
	}

	private IFolder createFolderHierarchy(IFolder parent, String... segments) throws Exception {
		IFolder result= parent;
		for (String segment : segments) {
			result= result.getFolder(segment);
			result.create(true, true, null);
		}
		return result;
	}

	private String getLabel(IFolder folder) {
		PackageExplorerLabelProvider labelProvider= new PackageExplorerLabelProvider((PackageExplorerContentProvider) fProvider);
		try {
			return labelProvider.getText(folder);
		} finally {
			labelProvider.dispose();
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		fArchiveFragmentRoot.close();
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);
		page.hideView(fMyPart);

		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);

	}

	/**
	 * Method compareArrays. Both arrays must be of IPackageFragments or compare will fail.
	 * @return boolean
	 */
	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if(children.length!=expectedChildren.length)
			return false;
		for (Object child : children) {
			if (child instanceof IJavaElement) {
				IJavaElement el= (IJavaElement) child;
				if(!contains(el, expectedChildren))
					return false;
			} else if(child instanceof IResource){
				IResource res = (IResource) child;
				if(!contains(res, expectedChildren)){
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * Method contains.
	 * @return boolean
	 */
	private boolean contains(IResource res, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IResource) {
				IResource expres= (IResource) object;
				if(expres.equals(res))
					return true;
			}
		}
		return false;
	}

	/**
	 * Method contains.
	 * @return boolean
	 */
	private boolean contains(IJavaElement fragment, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IJavaElement) {
				IJavaElement expfrag= (IJavaElement) object;
				if(expfrag.equals(fragment))
					return true;
			}
		}
		return false;
	}

}
