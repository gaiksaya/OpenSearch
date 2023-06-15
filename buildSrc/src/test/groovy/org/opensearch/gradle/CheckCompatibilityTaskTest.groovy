/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.gradle


import org.gradle.testkit.runner.GradleRunner
import org.opensearch.gradle.test.GradleUnitTestCase
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

class CheckCompatibilityTaskTest extends GradleUnitTestCase {

    void testGetRepoURL() {}


    private Project createProject() {
        Project project = ProjectBuilder.builder().build();
        return project;
    }

    private ConcatFilesTask createTask(Project project) {
        return project.getTasks().register("checkCompatibility", CheckCompatibilityTask.class);
    }
}
