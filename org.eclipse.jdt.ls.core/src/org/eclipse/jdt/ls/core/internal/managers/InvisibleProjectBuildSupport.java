/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.managers;

import org.codehaus.plexus.util.SelectorUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager.CHANGE_TYPE;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences.ReferencedLibraries;

/**
 * @author Fred Bricon
 *
 */
public class InvisibleProjectBuildSupport extends EclipseBuildSupport implements IBuildSupport {

	static final String LIB_FOLDER = "lib";

	public InvisibleProjectBuildSupport() {
	}

	@Override
	public boolean applies(IProject project) {
		return project != null && project.isAccessible() && !ProjectUtils.isVisibleProject(project);
	}

	@Override
	public boolean fileChanged(IResource resource, CHANGE_TYPE changeType, IProgressMonitor monitor) throws CoreException {
		if (resource == null || !applies(resource.getProject())) {
			return false;
		}
		refresh(resource, changeType, monitor);
		String resourcePath = resource.getLocation().toOSString();
		IProject project = resource.getProject();
		IPath projectFolder = ProjectUtils.getProjectRealFolder(project);
		ReferencedLibraries libraries = JavaLanguageServerPlugin.getPreferencesManager().getPreferences().getReferencedLibraries();
		for (String pattern: libraries.getExclude()) {
			if (matchPattern(projectFolder, pattern, resourcePath)) {
				return false; // skip if excluded
			}
		}
		for (String pattern: libraries.getInclude()) {
			if (matchPattern(projectFolder, pattern, resourcePath)) {
				UpdateClasspathJob.getInstance().updateClasspath(JavaCore.create(project), libraries);
				return false; // update if included in any pattern
			}
		}
		return false;
	}

	public boolean matchPattern(IPath base, String pattern, String path) {
		return SelectorUtils.matchPath(ProjectUtils.resolveGlobPath(base, pattern).toString(), path);
	}

}
