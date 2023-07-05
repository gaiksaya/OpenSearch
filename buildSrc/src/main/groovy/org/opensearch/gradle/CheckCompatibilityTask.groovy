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
import org.gradle.internal.os.OperatingSystem
import org.yaml.snakeyaml.Yaml

import java.nio.file.Paths

class CheckCompatibilityTask extends DefaultTask {

    static final String REPO_URL = 'https://raw.githubusercontent.com/opensearch-project/opensearch-build/main/manifests/3.0.0/opensearch-3.0.0.yml'

    @Input
    List repositoryUrls = project.hasProperty('repositoryUrls') ? project.property('repositoryUrls').split(',') : getRepoUrlsFromManifest()

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
        logger.info("Checking compatibility for: $repositoryUrls for $ref")
        repositoryUrls.parallelStream().forEach { repositoryUrl ->
            def tempDir = File.createTempDir()
            try {
                if (cloneAndCheckout(repositoryUrl, tempDir)) {
                    if (repositoryUrl.toString().endsWithAny('notifications', 'notifications.git')) {
                        tempDir = Paths.get(tempDir.getAbsolutePath(), 'notifications')
                    }
                    project.exec {
                        workingDir = tempDir
                        def stdout = new ByteArrayOutputStream()
                        executable = (OperatingSystem.current().isWindows()) ? 'gradlew.bat' : './gradlew'
                        args 'assemble'
                        standardOutput stdout
                    }
                    compatibleComponents.add(repositoryUrl)
                } else {
                    logger.lifecycle("Skipping compatibility check for $repositoryUrl")
                }
            } catch (ex) {
                failedComponents.add(repositoryUrl)
                logger.info("Gradle assemble failed for $repositoryUrl", ex)
            } finally {
                tempDir.deleteDir()
            }
        }
        if (!failedComponents.isEmpty()) {
            logger.lifecycle("Incompatible components: $failedComponents")
            logger.info("Compatible components: $compatibleComponents")
        }
        if (!gitFailedComponents.isEmpty()) {
            logger.lifecycle("Components skipped due to git failures: $gitFailedComponents")
            logger.info("Compatible components: $compatibleComponents")
        }
        if (!compatibleComponents.isEmpty()) {
            logger.lifecycle("Compatible components: $compatibleComponents")
        }
    }

    protected static List getRepoUrlsFromManifest() {
        def url = new URL(REPO_URL)
        def yamlContent = url.text
        Yaml parser = new Yaml()
        def manifest = parser.load(yamlContent)
        List repoUrls = manifest.components.collect { it.repository }
        repoUrls.remove('https://github.com/opensearch-project/OpenSearch.git')
        return repoUrls
    }

    protected boolean cloneAndCheckout(repoUrl, directory) {
        try {
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
        } catch (ex) {
            logger.error('Exception occurred during GitHub operations', ex)
            gitFailedComponents.add(repoUrl)
            return false
        }
    }
}
