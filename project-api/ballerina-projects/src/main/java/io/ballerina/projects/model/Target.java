/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.projects.model;

import io.ballerina.projects.Package;
import io.ballerina.projects.utils.ProjectConstants;
import io.ballerina.projects.utils.ProjectUtils;
import org.apache.commons.io.FileUtils;
import org.wso2.ballerinalang.compiler.util.ProjectDirConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents the target directory model.
 *
 * @since 2.0.0
 */
public class Target {
    private final Path targetPath;
    private Path outputPath = null;
    private Path cache;
    private Path jarCachePath;
    private Path baloCachePath;
    private Path birCachePath;
    private Path binPath;

    public Target(Path sourceRoot) throws IOException {
        this.targetPath = sourceRoot.resolve(ProjectConstants.TARGET_DIR_NAME);
        this.cache = this.targetPath.resolve(ProjectConstants.CACHES_DIR_NAME);
        this.baloCachePath = this.targetPath.resolve(ProjectConstants.TARGET_BALO_DIR_NAME);
        this.jarCachePath = this.cache.resolve(ProjectDirConstants.JAR_CACHE_DIR_NAME);
        this.birCachePath = this.cache.resolve(ProjectConstants.BIR_CACHE_DIR_NAME);
        this.binPath = this.targetPath.resolve(ProjectConstants.BIN_DIR_NAME);
        Files.createDirectories(this.targetPath);
    }

    /**
     * Returns the balo dir path.
     *
     * @return path of the balo file
     */
    public Path getBaloPath() throws IOException {
        Files.createDirectories(baloCachePath);
        return baloCachePath;
    }

    /**
     * Returns the jar-cache path.
     *
     * @return path of the executable
     */
    public Path getJarCachePath() throws IOException {
        Files.createDirectories(jarCachePath);
        return jarCachePath;
    }

    /**
     * Returns the path of the executable jar.
     *
     * @param pkg Package instance
     * @return the path of the executable
     */
    public Path getExecutablePath(Package pkg) {
        if (outputPath != null) {
            return outputPath;
        }
        try {
            return getBinPath().resolve(ProjectUtils.getExecutableName(pkg));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the bin directory path.
     *
     * @return bin path
     */
    public Path getBinPath() throws IOException {
        Files.createDirectories(binPath);
        return binPath;
    }

    /**
     * Returns the caches directory path.
     *
     * @return caches path
     */
    public Path cachesPath() {
        return this.targetPath.resolve(ProjectConstants.CACHES_DIR_NAME);
    }

    /**
     * Returns the bir-cache directory path.
     *
     * @return caches path
     */
    public Path getBirCachePath() throws IOException {
        Files.createDirectories(birCachePath);
        return birCachePath;
    }

    /**
     * Returns the path of the target directory.
     *
     * @return target path
     */
    public Path path() {
        return this.targetPath;
    }

    /**
     * Sets a custom path as the executable jar path.
     *
     * @param outputPath path to set for the executable jar
     * @throws IOException if directory creation fails
     */
    public void setOutputPath(Path outputPath) throws IOException {
        outputPath = outputPath.toAbsolutePath();
        if (Files.exists(outputPath)) {
            Files.delete(outputPath);
        }
        // create parent directories
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.outputPath = outputPath;
    }
    /**
     * Clean any files that created from the build.
     *
     */
    public void clean() throws IOException {
        // Remove from cache
        FileUtils.deleteDirectory(this.cache.toFile());
        // Remove any generated balo
        FileUtils.deleteDirectory(this.baloCachePath.toFile());
        FileUtils.deleteDirectory(this.binPath.toFile());
    }
}
