/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.langserver.hover;

import org.ballerinalang.langserver.compiler.LSContextManager;
import org.ballerinalang.langserver.completion.util.FileUtils;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test hover feature in language server.
 */
public class HoverProviderTest {
    private Path balPath = FileUtils.RES_DIR.resolve("hover").resolve("hover.bal");
    private Endpoint serviceEndpoint;

    @BeforeClass
    public void loadLangServer() throws IOException {
        serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, balPath);
    }
    
    @BeforeMethod
    public void clearPackageCache() {
        LSContextManager.getInstance().clearAllContexts();
    }

    @Test(description = "Test Hover for built in functions", dataProvider = "hoverBuiltinFuncPosition",
            enabled = false)
    public void hoverForBuiltInFunctionTest(Position position, String expectedFile)
            throws URISyntaxException, InterruptedException, IOException {
        Assert.assertEquals(TestUtil.getHoverResponse(balPath.toString(), position, serviceEndpoint),
                getExpectedValue(expectedFile),
                "Did not match the hover content for " + expectedFile + " and position line:" + position.getLine()
                        + " character:" + position.getCharacter());
    }

    @Test(description = "Test Hover for current package's functions",
            dataProvider = "hoverCurrentPackageFuncPosition")
    public void hoverForCurrentPackageFunctionTest(Position position, String expectedFile)
            throws InterruptedException, IOException {
        Assert.assertEquals(TestUtil.getHoverResponse(balPath.toString(), position, serviceEndpoint),
                getExpectedValue(expectedFile),
                "Did not match the hover content for " + expectedFile + " and position line:" + position.getLine()
                        + " character:" + position.getCharacter());
    }

    @Test(description = "Test Hover for current package's enums", dataProvider = "hoverCurrentPackageEnumPosition",
            enabled = false)
    public void hoverForCurrentPackageEnumTest(Position position, String expectedFile)
            throws InterruptedException, IOException {
        Assert.assertEquals(TestUtil.getHoverResponse(balPath.toString(), position, serviceEndpoint),
                getExpectedValue(expectedFile),
                "Did not match the hover content for " + expectedFile + " and position line:" + position.getLine()
                        + " character:" + position.getCharacter());
    }

    @Test(description = "Test Hover for current package's records",
            dataProvider = "hoverCurrentPackageRecordPosition")
    public void hoverForCurrentPackageRecordTest(Position position, String expectedFile)
            throws InterruptedException, IOException {
        Assert.assertEquals(TestUtil.getHoverResponse(balPath.toString(), position, serviceEndpoint),
                getExpectedValue(expectedFile),
                "Did not match the hover content for " + expectedFile + " and position line:" + position.getLine()
                        + " character:" + position.getCharacter());
    }

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.closeDocument(this.serviceEndpoint, balPath);
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
    }

    @DataProvider(name = "hoverBuiltinFuncPosition")
    public Object[][] getBuiltinFunctionPositions() {
        return new Object[][]{
                {new Position(45, 7), "builtin-function1.json"},
                {new Position(46, 19), "builtin-function2.json"}
        };
    }

    @DataProvider(name = "hoverCurrentPackageFuncPosition")
    public Object[][] getCurrentPackageFunctionPositions() {
        return new Object[][]{
                {new Position(45, 14), "currentPkg-function1.json"}
        };
    }

    @DataProvider(name = "hoverCurrentPackageEnumPosition")
    public Object[][] getCurrentPackageEnumPositions() {
        return new Object[][]{
                {new Position(31, 20), "currentPkg-enum.json"},
                {new Position(32, 7), "currentPkg-enum.json"},
                {new Position(32, 20), "currentPkg-enum.json"},
                {new Position(33, 12), "currentPkg-enum.json"},
                {new Position(34, 8), "currentPkg-enum.json"},
                {new Position(34, 14), "currentPkg-enum.json"}
        };
    }

    @DataProvider(name = "hoverCurrentPackageRecordPosition")
    public Object[][] getCurrentPackageStructPositions() {
        return new Object[][]{
                {new Position(46, 7), "currentPkg-record.json"},
                {new Position(51, 19), "currentPkg-record.json"},
                {new Position(52, 8), "currentPkg-record.json"}
        };
    }

    /**
     * Get the expected value from the expected file.
     *
     * @param expectedFile json file which contains expected content.
     * @return string content read from the json file.
     */
    private String getExpectedValue(String expectedFile) throws IOException {
        Path expectedFilePath = FileUtils.RES_DIR.resolve("hover").resolve("expected").resolve(expectedFile);
        byte[] expectedByte = Files.readAllBytes(expectedFilePath);
        return new String(expectedByte);
    }
}
