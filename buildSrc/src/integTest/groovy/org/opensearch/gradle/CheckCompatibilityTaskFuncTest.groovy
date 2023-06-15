/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.opensearch.gradle.fixtures.AbstractGradleFuncTest;

class CheckCompatibilityTaskFuncTest extends AbstractGradleFuncTest {

    def 'check success'() {
    given:
    buildFile << 'tasks.register(\'checkCompatibility\', CheckCompatibilityTask)'

    when:
    def result = gradleRunner("checkCompatibility").build()

    then:
    result.task('checkCompatibility').outcome == TaskOutcome.SUCCESS
    }
}
