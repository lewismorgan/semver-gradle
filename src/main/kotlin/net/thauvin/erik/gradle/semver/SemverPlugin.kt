/*
 * SemverPlugin.kt
 *
 * Copyright (c) 2018, Erik C. Thauvin (erik@thauvin.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *   Neither the name of this project nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.thauvin.erik.gradle.semver

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class SemverPlugin : Plugin<Project> {
    private val simpleName = SemverPlugin::class.simpleName
    private var version = Version()
    private lateinit var config: SemverConfig

    companion object {
        fun saveProperties(config: SemverConfig, version: Version) {
            val propsFile = File(config.properties)
            SortedProperties().apply {
                propsFile.apply {
                    if (canRead() && isFile) {
                        FileInputStream(this).reader().use { load(it) }
                    }
                }

                put(config.majorKey, version.major)
                put(config.minorKey, version.minor)
                put(config.patchKey, version.patch)
                put(config.preReleaseKey, version.preRelease)
                put(config.buildMetaKey, version.buildMeta)
                if (version.buildMetaPrefix != Version.DEFAULT_BUILDMETA_PREFIX ||
                    containsKey(config.buildMetaPrefixKey))
                    put(config.buildMetaPrefixKey, version.buildMetaPrefix)
                if (version.preReleasePrefix != Version.DEFAULT_PRERELEASE_PREFIX ||
                    containsKey(config.preReleasePrefixKey))
                    put(config.preReleasePrefixKey, version.preReleasePrefix)
                if (version.separator != Version.DEFAULT_SEPARATOR || containsKey(config.separatorKey))
                    put(config.separatorKey, version.separator)

                propsFile.apply {
                    if (canWrite()) {
                        FileOutputStream(this).writer().use {
                            store(it, "Generated by the Semver Plugin for Gradle")
                        }
                    } else {
                        throw GradleException("Unable to write version to: `$absoluteFile`")
                    }
                }
            }
        }
    }

    override fun apply(project: Project) {
        if (GradleVersion.current() < GradleVersion.version("4.8.1")) {
            throw GradleException("The $simpleName plugin requires Gradle version 4.8.1 or greater.")
        }
        project.afterEvaluate(this::afterEvaluate)
        config = project.extensions.create("semver", SemverConfig::class.java)

        project.tasks.apply {
            create("incrementMajor", SemverIncrementTask::class.java, config, version, SemverConfig.DEFAULT_MAJOR_KEY)
            create("incrementMinor", SemverIncrementTask::class.java, config, version, SemverConfig.DEFAULT_MINOR_KEY)
            create("incrementPatch", SemverIncrementTask::class.java, config, version, SemverConfig.DEFAULT_PATCH_KEY)
            create("incrementBuildMeta", SemverIncrementBuildMetaTask::class.java, config, version)
        }
    }

    private fun afterEvaluate(project: Project) {
        val propsFile = File(config.properties)
        if (project.version != "unspecified") {
            project.logger.warn(
                "Please specify the version in ${propsFile.name} and remove it from ${project.buildFile.name}")
        }
        propsFile.apply {
            project.logger.info(
                "[$simpleName] Attempting to read properties from: `$absoluteFile`. [exists: ${exists()}, isFile: $isFile, canRead: ${canRead()}]")
            if (canRead() && isFile) {
                FileInputStream(this).reader().use { reader ->
                    Properties().apply {
                        load(reader)

                        version.major = getProperty(config.majorKey, Version.DEFAULT_MAJOR)
                        version.minor = getProperty(config.minorKey, Version.DEFAULT_MINOR)
                        version.patch = getProperty(config.patchKey, Version.DEFAULT_PATCH)
                        version.preRelease = getProperty(config.preReleaseKey, Version.DEFAULT_EMPTY)
                        version.preReleasePrefix =
                            getProperty(config.preReleasePrefixKey, Version.DEFAULT_PRERELEASE_PREFIX)
                        version.buildMeta = getProperty(config.buildMetaKey, Version.DEFAULT_EMPTY)
                        version.buildMetaPrefix =
                            getProperty(config.buildMetaPrefixKey, Version.DEFAULT_BUILDMETA_PREFIX)
                        version.separator = getProperty(config.separatorKey, Version.DEFAULT_SEPARATOR)

                        project.tasks.withType(SemverIncrementBuildMetaTask::class.java) {
                            buildMeta = version.buildMeta
                        }
                    }
                }
            } else if (exists()) {
                throw GradleException("Unable to read version from: `$absoluteFile`")
            }
            project.version = version.semver
            project.logger.info("[$simpleName] Project version set to: ${project.version}")
            saveProperties(config, version)
        }
    }
}
