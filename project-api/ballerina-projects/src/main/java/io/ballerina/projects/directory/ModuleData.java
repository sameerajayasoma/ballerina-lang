/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.projects.directory;

import java.nio.file.Path;
import java.util.List;

/**
 * {@code ModuleFileData} represents a Ballerina module directory.
 *
 * @since 2.0.0
 */
public class ModuleData {
    // Just the module name without the package name
    private final Path moduleDirPath;
    private final List<DocumentData> srcDocs;
    private final List<DocumentData> testSrcDocs;
    private byte[] birBytes = new byte[0];
    // TODO do we need to maintain resources and test resources

    private ModuleData(Path moduleDirPath,
                       List<DocumentData> srcDocs,
                       List<DocumentData> testSrcDocs) {
        this.moduleDirPath = moduleDirPath;
        this.srcDocs = srcDocs;
        this.testSrcDocs = testSrcDocs;
    }

    public static ModuleData from(Path path,
                                  List<DocumentData> srcDocuments,
                                  List<DocumentData> testSrcDocuments) {
        return new ModuleData(path, srcDocuments, testSrcDocuments);
    }

    // Temp code
    public static ModuleData from(Path path,
                                  List<DocumentData> srcDocuments,
                                  List<DocumentData> testSrcDocuments, byte[] birBytes) {
        final ModuleData moduleData = new ModuleData(path, srcDocuments, testSrcDocuments);
        moduleData.birBytes = birBytes;
        return moduleData;
    }

    public byte[] birBytes() {
        return birBytes;
    }

    public Path moduleDirectoryPath() {
        return moduleDirPath;
    }

    public List<DocumentData> sourceDocs() {
        return srcDocs;
    }

    public List<DocumentData> testSourceDocs() {
        return testSrcDocs;
    }
}
