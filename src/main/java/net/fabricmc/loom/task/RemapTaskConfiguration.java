/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2021 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.task;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.jvm.tasks.Jar;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.gradle.GradleUtils;
import net.fabricmc.loom.util.gradle.SourceSetHelper;
import net.fabricmc.loom.util.gradle.SyncTaskBuildService;

//We dont need remap task anymore
public abstract class RemapTaskConfiguration implements Runnable {
	public static final String REMAP_JAR_TASK_NAME = "remapJar";
	public static final String REMAP_SOURCES_JAR_TASK_NAME = "remapSourcesJar";

	@Inject
	protected abstract Project getProject();

	@Inject
	protected abstract TaskContainer getTasks();

	@Inject
	protected abstract ArtifactHandler getArtifacts();

	@Inject
	protected abstract ConfigurationContainer getConfigurations();

	public void run() {
		final LoomGradleExtension extension = LoomGradleExtension.get(getProject());
		SyncTaskBuildService.register(getProject());
        extension.getUnmappedModCollection().from(getTasks().getByName(JavaPlugin.JAR_TASK_NAME));
    }

	private void trySetupSourceRemapping() {
		final LoomGradleExtension extension = LoomGradleExtension.get(getProject());
		final String sourcesJarTaskName = SourceSetHelper.getMainSourceSet(getProject()).getSourcesJarTaskName();

		TaskProvider<RemapSourcesJarTask> remapSourcesTask = getTasks().register(REMAP_SOURCES_JAR_TASK_NAME, RemapSourcesJarTask.class, task -> {
			task.setDescription("Remaps the default sources jar to intermediary mappings.");
			task.setGroup(Constants.TaskGroup.FABRIC);

			final Task sourcesTask = getTasks().findByName(sourcesJarTaskName);

			if (sourcesTask == null) {
				getProject().getLogger().info("{} task was not found, not remapping sources", sourcesJarTaskName);
				task.setEnabled(false);
				return;
			}

			if (!(sourcesTask instanceof Jar sourcesJarTask)) {
				getProject().getLogger().info("{} task is not a Jar task, not remapping sources", sourcesJarTaskName);
				task.setEnabled(false);
				return;
			}

			sourcesJarTask.getArchiveClassifier().convention("dev-sources");
			sourcesJarTask.getDestinationDirectory().set(getProject().getLayout().getBuildDirectory().map(directory -> directory.dir("devlibs")));
			task.getArchiveClassifier().convention("sources");

			task.dependsOn(sourcesJarTask);
			task.getInputFile().convention(sourcesJarTask.getArchiveFile());
			task.getIncludesClientOnlyClasses().set(getProject().provider(extension::areEnvironmentSourceSetsSplit));
		});

		getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(remapSourcesTask));

		if (GradleUtils.getBooleanProperty(getProject(), "fabric.loom.disableRemappedVariants")) {
			return;
		}

		GradleUtils.afterSuccessfulEvaluation(getProject(), () -> {
			final Task sourcesTask = getTasks().findByName(sourcesJarTaskName);

			if (!(sourcesTask instanceof Jar sourcesJarTask)) {
				return;
			}

			if (getConfigurations().getNames().contains(JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME)) {
				getArtifacts().add(JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME, remapSourcesTask);

				// Remove the dev sources artifact
				Configuration configuration = getConfigurations().getByName(JavaPlugin.SOURCES_ELEMENTS_CONFIGURATION_NAME);
				configuration.getArtifacts().removeIf(a -> a.getFile().equals(sourcesJarTask.getArchiveFile().get().getAsFile()));
			} else {
				// Sources jar may not have been created with withSourcesJar
				getProject().getLogger().warn("Not publishing sources jar as it was not found. Use java.withSourcesJar() to fix.");
			}
		});
	}
}
