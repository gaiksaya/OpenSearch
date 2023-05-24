/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.gradle

import groovy.json.JsonSlurper
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.BranchListOp
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

class CheckCompatibilityTask extends DefaultTask {

    @Input
    List repositoryUrls = project.hasProperty('repositoryUrls') ? project.property('repositoryUrls').split(',') : getRepoUrls()

    @Input
    String ref = project.hasProperty('ref') ? project.property('ref') : 'main'

    @Internal
    List failedComponents = []

    @Internal
    List gitFailedComponents = []

    @Internal
    List compatibleComponents = []

    @TaskAction
    void checkCompatibility() {
        println("Checking compatibility for: $repositoryUrls for $ref")
        repositoryUrls.parallelStream().forEach { repositoryUrl ->
            def tempDir = File.createTempDir()
            def gradleScript = getGradleExec()
            try {
                if (cloneAndCheckout(repositoryUrl, tempDir)) {
                    if (repositoryUrl.toString().endsWithAny('notifications','notifications.git')) {
                        tempDir = Paths.get(tempDir.getAbsolutePath(), 'notifications')
                    }
                    project.exec {
                        workingDir = tempDir
                        def stdout = new ByteArrayOutputStream()
                        commandLine gradleScript, 'assemble'
                        standardOutput stdout
                    }
                    compatibleComponents.add(repositoryUrl)
                } else {
                    println("Skipping compatibility check for $repositoryUrl")
                }
            } catch (ex) {
                failedComponents.add(repositoryUrl)
                logger.info("Gradle assemble failed for $repositoryUrl", ex)
            } finally {
                tempDir.deleteDir()
            }
        }
        if (!failedComponents.isEmpty()) {
            println("Incompatible components: $failedComponents")
            logger.info("Compatible components: $compatibleComponents")
        } else {
            println("Compatible components: $compatibleComponents")
        }
        println("Skipped components: $gitFailedComponents")
    }

    protected static List getRepoUrls() {
        def json = new JsonSlurper().parse('https://raw.githubusercontent.com/opensearch-project/opensearch-plugins/main/plugins/.meta'.toURL())
        def labels = json.projects.values()
        return labels as List
    }

    protected static String getGradleExec() {
        if(System.properties['os.name'].toLowerCase().contains('windows')){
            return 'gradlew.bat'
        }
        return './gradlew'
    }

    protected boolean cloneAndCheckout(repoUrl, directory){
        def grgit = Grgit.clone(dir: directory, uri: repoUrl)
        def remoteBranches = grgit.branch.list(mode: BranchListOp.Mode.REMOTE)
        String targetBranch = 'origin/' + ref
        if (remoteBranches.find { it.name == targetBranch } == null) {
            gitFailedComponents.add(repoUrl)
            logger.info("$ref does not exist for $repoUrl. Skipping the compatibility check!!")
            return false
        } else {
            logger.info("Checking out $targetBranch")
            grgit.checkout(branch: targetBranch)
            return true
        }
    }
}
