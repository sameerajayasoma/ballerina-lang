/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.cli.task;

import io.ballerina.cli.utils.FileUtils;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.model.Target;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.projects.util.ProjectConstants.BLANG_COMPILED_JAR_EXT;
import static io.ballerina.projects.utils.ProjectConstants.USER_DIR;

/**
 * Task for creating the executable jar file.
 *
 * @since 2.0.0
 */
public class CreateExecutableTask implements Task {
    private final transient PrintStream out;
    private String output;

    public CreateExecutableTask(PrintStream out, String output) {
        this.out = out;
        this.output = output;
    }

    @Override
    public void execute(Project project) {
        this.out.println();
        this.out.println("Generating executables");

        Target target;
        Path currentDir = Paths.get(System.getProperty(USER_DIR));
        try {
            target = new Target(project.sourceRoot());
            if (project instanceof SingleFileProject) {
                Path outputPath;
                DocumentId documentId = project.currentPackage().getDefaultModule().documentIds().iterator().next();
                String documentName = project.currentPackage().getDefaultModule().document(documentId).name();
                String executableName = FileUtils.geFileNameWithoutExtension(Paths.get(documentName));
                if (executableName == null) {
                    throw new RuntimeException("unable to determine executable name");
                }
                if (output != null) {
                    if (!FileUtils.hasExtension(Paths.get(output))) {
                        outputPath = Paths.get(output).resolve(executableName + BLANG_COMPILED_JAR_EXT);
                    } else {
                        outputPath = Paths.get(output);
                    }
                } else {
                    outputPath = currentDir.resolve(executableName + BLANG_COMPILED_JAR_EXT);
                }
                target.setOutputPath(outputPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("unable to set executable path: " + e.getMessage());
        }

        Path executablePath = target.getExecutablePath(project.currentPackage()).toAbsolutePath().normalize();
        PackageCompilation packageCompilation = project.currentPackage().getCompilation();
        packageCompilation.emit(PackageCompilation.OutputType.EXEC, executablePath);

        // Print the path of the executable
        Path relativePathToExecutable = currentDir.relativize(executablePath);
        if (relativePathToExecutable.toString().contains("..") ||
                relativePathToExecutable.toString().contains("." + File.separator)) {
            this.out.println("\t" + executablePath.toString());
        } else {
            this.out.println("\t" + relativePathToExecutable.toString());
        }
    }
}
