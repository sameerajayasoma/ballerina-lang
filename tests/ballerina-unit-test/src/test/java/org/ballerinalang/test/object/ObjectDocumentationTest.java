/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.object;

import org.ballerinalang.launcher.util.BAssertUtil;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotation;
import org.wso2.ballerinalang.compiler.tree.BLangMarkdownDocumentation;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;

/**
 * Test cases for user defined documentation attachment in ballerina.
 */
public class ObjectDocumentationTest {

    private static final String CARRIAGE_RETURN_CHAR = "\r";
    private static final String EMPTY_STRING = "";

    @BeforeClass
    public void setup() {
    }

    @Test(description = "Test doc annotation.")
    public void testDocAnnotation() {
        CompileResult compileResult = BCompileUtil.compile("test-src/object/object_annotation.bal");
        Assert.assertEquals(0, compileResult.getWarnCount());
        PackageNode packageNode = compileResult.getAST();
        BLangMarkdownDocumentation docNode = ((BLangTypeDefinition) packageNode.getTypeDefinitions()
                .get(0)).markdownDocumentationAttachment;
        Assert.assertNotNull(docNode);
        Assert.assertEquals(docNode.getDocumentation().replaceAll(CARRIAGE_RETURN_CHAR, EMPTY_STRING),
                "Documentation for Test annotation\n");
        Assert.assertEquals(docNode.getParameters().size(), 3);
        Assert.assertEquals(docNode.getParameters().get(0).getParameterName().getValue(), "a");
        Assert.assertEquals(docNode.getParameters().get(0).getParameterDocumentation().replaceAll(CARRIAGE_RETURN_CHAR,
                EMPTY_STRING), "annotation `field a` documentation");

        Assert.assertEquals(docNode.getParameters().get(1).getParameterName().getValue(), "b");
        Assert.assertEquals(docNode.getParameters().get(1).getParameterDocumentation().replaceAll(CARRIAGE_RETURN_CHAR,
                EMPTY_STRING), "annotation `field b` documentation");
        Assert.assertEquals(docNode.getParameters().get(2).getParameterName().getValue(), "c");
        Assert.assertEquals(docNode.getParameters().get(2).getParameterDocumentation().replaceAll(CARRIAGE_RETURN_CHAR,
                EMPTY_STRING), "annotation `field c` documentation");

        docNode = ((BLangAnnotation) packageNode.getAnnotations().get(0)).markdownDocumentationAttachment;
        Assert.assertNotNull(docNode);
    }

    @Test(description = "Test doc struct.")
    public void testDocStruct() {
        CompileResult compileResult = BCompileUtil.compile("test-src/object/object_doc_annotation.bal");
        Assert.assertEquals(0, compileResult.getWarnCount());
        PackageNode packageNode = compileResult.getAST();
        BLangMarkdownDocumentation dNode =
                ((BLangTypeDefinition) packageNode.getTypeDefinitions().get(0)).markdownDocumentationAttachment;
        Assert.assertNotNull(dNode);
        Assert.assertEquals(dNode.getDocumentation().replaceAll(CARRIAGE_RETURN_CHAR, EMPTY_STRING),
                "Documentation for Test struct\n");
        Assert.assertEquals(dNode.getParameters().size(), 3);
        Assert.assertEquals(dNode.getParameters().get(0).getParameterName().getValue(), "a");
        Assert.assertEquals(dNode.getParameters().get(0).getParameterDocumentation().replaceAll(CARRIAGE_RETURN_CHAR,
                EMPTY_STRING), "struct `field a` documentation");
        Assert.assertEquals(dNode.getParameters().get(1).getParameterName().getValue(), "b");
        Assert.assertEquals(dNode.getParameters().get(1).getParameterDocumentation().replaceAll(CARRIAGE_RETURN_CHAR,
                EMPTY_STRING), "struct `field b` documentation");
        Assert.assertEquals(dNode.getParameters().get(2).getParameterName().getValue(), "c");
        Assert.assertEquals(dNode.getParameters().get(2).getParameterDocumentation().replaceAll(CARRIAGE_RETURN_CHAR,
                EMPTY_STRING), "struct `field c` documentation");
    }

    @Test(description = "Test doc negative cases.")
    public void testDocumentationNegative() {
        CompileResult compileResult = BCompileUtil.compile("test-src/object/object_annotation_negative.bal");
        Assert.assertEquals(compileResult.getErrorCount(), 0, getErrorString(compileResult.getDiagnostics()));
        Assert.assertEquals(compileResult.getWarnCount(), 16);
        BAssertUtil.validateWarning(compileResult, 0, "field 'a' already documented", 6, 5);
        BAssertUtil.validateWarning(compileResult, 1, "no such documentable field 'c'", 8, 5);
        BAssertUtil.validateWarning(compileResult, 2, "undocumented field 'cd'", 12, 5);
        BAssertUtil.validateWarning(compileResult, 3, "no such documentable parameter 'testConst'", 19, 5);
        BAssertUtil.validateWarning(compileResult, 4, "field 'a' already documented", 25, 5);
        BAssertUtil.validateWarning(compileResult, 5, "no such documentable field 'c'", 27, 5);
        BAssertUtil.validateWarning(compileResult, 6, "undocumented field 'cdd'", 31, 5);
        BAssertUtil.validateWarning(compileResult, 7, "parameter 'accessMode' already documented", 39, 5);
        BAssertUtil.validateWarning(compileResult, 8, "no such documentable parameter 'successfuls'", 40, 5);
        BAssertUtil.validateWarning(compileResult, 9, "undocumented return parameter", 41, 1);
        BAssertUtil.validateWarning(compileResult, 10, "field 'url' already documented", 73, 5);
        BAssertUtil.validateWarning(compileResult, 11, "no such documentable field 'urls'", 74, 5);
        BAssertUtil.validateWarning(compileResult, 12, "no such documentable parameter 'conn'", 81, 5);
        BAssertUtil.validateWarning(compileResult, 13, "parameter 'req' already documented", 87, 9);
        BAssertUtil.validateWarning(compileResult, 14, "no such documentable parameter 'reqest'", 88, 9);
        BAssertUtil.validateWarning(compileResult, 15, "no such documentable parameter 'testConstd'", 99, 5);
    }

    private String getErrorString(Diagnostic[] diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic diagnostic : diagnostics) {
            sb.append(diagnostic).append("\n");
        }
        return sb.toString();
    }
}
