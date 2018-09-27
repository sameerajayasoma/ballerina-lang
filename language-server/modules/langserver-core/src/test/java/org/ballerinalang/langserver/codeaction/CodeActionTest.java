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
package org.ballerinalang.langserver.codeaction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ballerinalang.compiler.CompilerPhase;
import org.ballerinalang.langserver.common.constants.CommandConstants;
import org.ballerinalang.langserver.compiler.LSCompiler;
import org.ballerinalang.langserver.compiler.common.modal.BallerinaFile;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentManagerImpl;
import org.ballerinalang.langserver.completion.util.FileUtils;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Cases for CodeActions.
 * 
 * @since 0.982.0
 */
public class CodeActionTest {
    private Endpoint serviceEndpoint;

    private Gson gson = new Gson();

    private JsonParser parser = new JsonParser();

    private Path sourcesPath = new File(getClass().getClassLoader().getResource("codeaction").getFile()).toPath();
    
    @BeforeClass
    public void init() throws Exception {
        this.serviceEndpoint = TestUtil.initializeLanguageSever();
    }
    
    @Test(dataProvider = "codeaction-no-diagnostics-data-provider")
    public void testCodeActionWithoutDiagnostics(String config, String source) {
        String configJsonPath = "codeaction" + File.separator + config;
        Path sourcePath = sourcesPath.resolve("source").resolve(source);
        JsonObject configJsonObject = FileUtils.fileContentAsObject(configJsonPath);
        JsonObject expected = configJsonObject.get("expected").getAsJsonObject();
        int numberOfCommands = expected.get("size").getAsInt();
        JsonObject documentThis = expected.getAsJsonObject("actions").getAsJsonObject("documentThis");
        CodeActionContext codeActionContext = new CodeActionContext();
        Range range = gson.fromJson(configJsonObject.get("range"), Range.class);
        
        JsonObject responseJson = parser.parse(TestUtil
                .getCodeActionResponse(this.serviceEndpoint, sourcePath.toString(), range, codeActionContext))
                .getAsJsonObject();
        JsonArray result = responseJson.getAsJsonArray("result");
        
        Assert.assertEquals(numberOfCommands, result.size());
        result.forEach(element -> {
            String title = element.getAsJsonObject().get("title").getAsString();
            String command = element.getAsJsonObject().get("command").getAsString();
            switch (command) {
                case CommandConstants.CMD_ADD_DOCUMENTATION:
                    Assert.assertEquals(title, "Document This");
                    JsonArray args = element.getAsJsonObject().get("arguments").getAsJsonArray();
                    JsonArray documentThisArr = documentThis.getAsJsonArray("arguments");
                    Assert.assertTrue(TestUtil.isArgumentsSubArray(args, documentThisArr));
                    break;
                case CommandConstants.CMD_ADD_ALL_DOC:
                    Assert.assertEquals(title, "Document All");
                    break;
                case CommandConstants.CMD_CREATE_CONSTRUCTOR:
                    Assert.assertEquals(title, "Create Constructor");
                    break;
                default:
                    Assert.fail("Invalid Command Found: [" + title + "]");
                    break;
            }
        });
    }
    
    @Test(dataProvider = "codeaction-diagnostics-data-provider")
    public void testCodeActionWithDiagnostics(String config, String source) throws IOException {
        String configJsonPath = "codeaction" + File.separator + config;
        Path sourcePath = sourcesPath.resolve("source").resolve(source);
        JsonObject configJsonObject = FileUtils.fileContentAsObject(configJsonPath);
        TestUtil.openDocument(serviceEndpoint, sourcePath);
        
        LSCompiler lsCompiler = new LSCompiler(WorkspaceDocumentManagerImpl.getInstance());
        BallerinaFile ballerinaFile = lsCompiler.compileFile(sourcePath, CompilerPhase.COMPILER_PLUGIN);
        List<Diagnostic> lsDiagnostics = new ArrayList<>();
        ballerinaFile.getDiagnostics().ifPresent(diagnostics -> lsDiagnostics.addAll(getLSDiagnostics(diagnostics)));
        CodeActionContext codeActionContext = new CodeActionContext(lsDiagnostics);
        Range range = gson.fromJson(configJsonObject.get("range"), Range.class);
        String res = TestUtil.getCodeActionResponse(serviceEndpoint, sourcePath.toString(), range, codeActionContext);
        TestUtil.closeDocument(this.serviceEndpoint, sourcePath);
        
        JsonObject expected = configJsonObject.get("expected").getAsJsonObject();
        String title = expected.get("title").toString();
        String command = expected.get("command").toString();
        JsonArray args = expected.get("arguments").getAsJsonArray();
        
        boolean codeActionFound = false;
        JsonObject responseJson = this.getResponseJson(res);
        for (JsonElement jsonElement : responseJson.getAsJsonArray("result")) {
            if (jsonElement.getAsJsonObject().get("title").toString().equals(title)
                    && jsonElement.getAsJsonObject().get("command").toString().equals(command)
                    && TestUtil.isArgumentsSubArray(jsonElement.getAsJsonObject().get("arguments").getAsJsonArray(),
                    args)) {
                codeActionFound = true;
                break;
            }
        }
        
        Assert.assertTrue(codeActionFound, "Cannot find expected Code Action for: " + title);
    }

    @DataProvider(name = "codeaction-no-diagnostics-data-provider")
    public Object[][] codeActionDataProvider() {
        return new Object[][] {
                {"singleDocGeneration.json", "singleDocGeneration.bal"},
                {"singleDocGeneration1.json", "singleDocGeneration.bal"},
                {"singleDocGeneration2.json", "singleDocGeneration.bal"},
                {"singleDocGeneration3.json", "singleDocGeneration.bal"},
                {"singleDocGeneration4.json", "singleDocGeneration.bal"},
        };
    }

    @DataProvider(name = "codeaction-diagnostics-data-provider")
    public Object[][] codeActionWithDiagnosticDataProvider() {
        return new Object[][] {
                {"undefinedPackageWithinFunction.json", "codeActionCommon.bal"},
                {"undefinedFunctionCodeAction.json", "createUndefinedFunction.bal"},
                {"undefinedFunctionCodeAction2.json", "createUndefinedFunction2.bal"},
                {"variableAssignmentRequiredCodeAction.json", "createVariable.bal"}
        };
    }

    @AfterClass
    public void cleanupLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
    }

    private JsonObject getResponseJson(String response) {
        JsonObject responseJson = parser.parse(response).getAsJsonObject();
        responseJson.remove("id");
        return responseJson;
    }
    
    private List<Diagnostic> getLSDiagnostics(List<org.ballerinalang.util.diagnostic.Diagnostic> ballerinaDiags) {
        List<Diagnostic> lsDiagnostics = new ArrayList<>();
        ballerinaDiags.forEach(diagnostic -> {
            Diagnostic lsDiagnostic = new Diagnostic();
            lsDiagnostic.setSeverity(DiagnosticSeverity.Error);
            lsDiagnostic.setMessage(diagnostic.getMessage());
            Range r = new Range();

            int startLine = diagnostic.getPosition().getStartLine() - 1; // LSP diagnostics range is 0 based
            int startChar = diagnostic.getPosition().getStartColumn() - 1;
            int endLine = diagnostic.getPosition().getEndLine() - 1;
            int endChar = diagnostic.getPosition().getEndColumn() - 1;

            if (endLine <= 0) {
                endLine = startLine;
            }

            if (endChar <= 0) {
                endChar = startChar + 1;
            }

            r.setStart(new Position(startLine, startChar));
            r.setEnd(new Position(endLine, endChar));
            lsDiagnostic.setRange(r);

            lsDiagnostics.add(lsDiagnostic);
        });
        
        return lsDiagnostics;
    }
}
