/*
 * Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.command;

import com.google.gson.internal.LinkedTreeMap;
import org.ballerinalang.langserver.common.UtilSymbolKeys;
import org.ballerinalang.langserver.common.constants.CommandConstants;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.CommonUtil.FunctionGenerator;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSCompiler;
import org.ballerinalang.langserver.compiler.LSServiceOperationContext;
import org.ballerinalang.langserver.compiler.common.LSCustomErrorStrategy;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentManager;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.tree.Node;
import org.ballerinalang.model.tree.TopLevelNode;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangEndpoint;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangResource;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.ballerinalang.langserver.common.utils.CommonUtil.createVariableDeclaration;
import static org.ballerinalang.langserver.compiler.LSCompilerUtil.getUntitledFilePath;

/**
 * Command Executor for Ballerina Workspace service execute command operation.
 *
 * @since v0.964.0
 */
public class CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    private static final String ARG_KEY = "argumentK";

    private static final String ARG_VALUE = "argumentV";

    private CommandExecutor() {
    }

    /**
     * Command Execution router.
     *
     * @param params  Parameters for the command
     * @param context Workspace service context
     * @return Result object
     */
    public static Object executeCommand(ExecuteCommandParams params, LSServiceOperationContext context) {
        Object result;
        try {
            switch (params.getCommand()) {
                case CommandConstants.CMD_IMPORT_PACKAGE:
                    result = executeImportPackage(context);
                    break;
                case CommandConstants.CMD_CREATE_FUNCTION:
                    result = executeCreateFunction(context);
                    break;
                case CommandConstants.CMD_CREATE_VARIABLE:
                    result = executeCreateVariable(context);
                    break;
                case CommandConstants.CMD_ADD_DOCUMENTATION:
                    result = executeAddDocumentation(context);
                    break;
                case CommandConstants.CMD_ADD_ALL_DOC:
                    result = executeAddAllDocumentation(context);
                    break;
                case CommandConstants.CMD_CREATE_CONSTRUCTOR:
                    result = executeCreateObjectConstructor(context);
                    break;
                default:
                    // Do Nothing
                    result = new Object();
                    break;
            }
        } catch (WorkspaceDocumentException | BallerinaCommandExecutionException e) {
            logger.error("Error occurred while executing command", e);
            result = new Object();
        }

        return result;
    }

    /**
     * Execute the command, import package.
     *
     * @param context Workspace service context
     */
    private static Object executeImportPackage(LSServiceOperationContext context) throws WorkspaceDocumentException {
        String documentUri = null;
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();

        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_DOC_URI)) {
                documentUri = (String) ((LinkedTreeMap) arg).get(ARG_VALUE);
                textDocumentIdentifier.setUri(documentUri);
                context.put(DocumentServiceKeys.FILE_URI_KEY, documentUri);
            } else if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_PKG_NAME)) {
                context.put(ExecuteCommandKeys.PKG_NAME_KEY, (String) ((LinkedTreeMap) arg).get(ARG_VALUE));
            }
        }

        WorkspaceDocumentManager documentManager = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY);
        if (documentUri != null && context.get(ExecuteCommandKeys.PKG_NAME_KEY) != null) {
            Path filePath = Paths.get(URI.create(documentUri));
            Path compilationPath = getUntitledFilePath(filePath.toString()).orElse(filePath);
            String fileContent = documentManager.getFileContent(compilationPath);
            String[] contentComponents = fileContent.split(CommonUtil.LINE_SEPARATOR_SPLIT);
            int totalLines = contentComponents.length;
            int lastNewLineCharIndex = Math.max(fileContent.lastIndexOf('\n'), fileContent.lastIndexOf('\r'));
            int lastCharCol = fileContent.substring(lastNewLineCharIndex + 1).length();
            LSCompiler lsCompiler = context.get(ExecuteCommandKeys.LS_COMPILER_KEY);
            BLangPackage bLangPackage = lsCompiler.getBLangPackage(context, documentManager, false,
                                                                   LSCustomErrorStrategy.class,
                                                                   false).getRight();
            context.put(DocumentServiceKeys.CURRENT_PACKAGE_NAME_KEY,
                        bLangPackage.symbol.getName().getValue());
            String pkgName = context.get(ExecuteCommandKeys.PKG_NAME_KEY);
            String currentFile = context.get(DocumentServiceKeys.FILE_NAME_KEY);
            DiagnosticPos pos;

            // Filter the imports except the runtime import
            List<BLangImportPackage> imports = bLangPackage.getImports().stream()
                    .filter(bLangImportPackage -> bLangImportPackage.getPosition().src.cUnitName.equals(currentFile))
                    .collect(Collectors.toList());

            if (!imports.isEmpty()) {
                BLangImportPackage lastImport = CommonUtil.getLastItem(imports);
                pos = lastImport.getPosition();
            } else {
                pos = null;
            }

            int endCol = pos == null ? -1 : pos.getEndColumn() - 1;
            int endLine = pos == null ? 0 : pos.getEndLine() - 1;

            String remainingTextToReplace;

            if (endCol != -1) {
                int contentLengthToReplaceStart = fileContent.substring(0,
                        fileContent.indexOf(contentComponents[endLine])).length() + endCol + 1;
                remainingTextToReplace = fileContent.substring(contentLengthToReplaceStart);
            } else {
                remainingTextToReplace = fileContent;
            }

            String editText = (pos != null ? "\r\n" : "") + "import " + pkgName + ";"
                    + (remainingTextToReplace.startsWith("\n") || remainingTextToReplace.startsWith("\r") ? "" : "\r\n")
                    + remainingTextToReplace;
            Range range = new Range(new Position(endLine, endCol + 1), new Position(totalLines + 1, lastCharCol));

            return applySingleTextEdit(editText, range, textDocumentIdentifier,
                    context.get(ExecuteCommandKeys.LANGUAGE_SERVER_KEY).getClient());
        }

        return new Object();
    }

    /**
     * Execute the command, create function.
     *
     * @param context Workspace service context
     */
    private static Object executeCreateFunction(LSServiceOperationContext context) throws WorkspaceDocumentException {
        String documentUri = null;
        String funcName = null;
        String returnType = null;
        String returnDefaultValue = null;
        String funcArgs = "";
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();

        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            String argKey = ((LinkedTreeMap) arg).get(ARG_KEY).toString();
            String argVal = ((LinkedTreeMap) arg).get(ARG_VALUE).toString();
            if (argKey.equals(CommandConstants.ARG_KEY_DOC_URI)) {
                documentUri = argVal;
                textDocumentIdentifier.setUri(documentUri);
                context.put(DocumentServiceKeys.FILE_URI_KEY, documentUri);
            } else if (argKey.equals(CommandConstants.ARG_KEY_FUNC_NAME)) {
                funcName = argVal;
            } else if (argKey.equals(CommandConstants.ARG_KEY_RETURN_TYPE)) {
                returnType = argVal;
            } else if (argKey.equals(CommandConstants.ARG_KEY_RETURN_DEFAULT_VAL)) {
                returnDefaultValue = argVal;
            } else if (argKey.equals(CommandConstants.ARG_KEY_FUNC_ARGS)) {
                funcArgs = argVal;
            }
        }

        if (documentUri == null || funcName == null) {
            return new Object();
        }

        WorkspaceDocumentManager documentManager = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY);

        Path filePath = Paths.get(URI.create(documentUri));
        Path compilationPath = getUntitledFilePath(filePath.toString()).orElse(filePath);
        String fileContent = documentManager.getFileContent(compilationPath);
        String[] contentComponents = fileContent.split("\\n|\\r\\n|\\r");
        int totalLines = contentComponents.length;
        int lastNewLineCharIndex = Math.max(fileContent.lastIndexOf('\n'), fileContent.lastIndexOf('\r'));
        int lastCharCol = fileContent.substring(lastNewLineCharIndex + 1).length();

        LSCompiler lsCompiler = context.get(ExecuteCommandKeys.LS_COMPILER_KEY);
        BLangPackage bLangPackage = lsCompiler.getBLangPackage(context, documentManager, false,
                                                               LSCustomErrorStrategy.class, false).getRight();
        if (bLangPackage == null) {
            return new Object();
        }

        String editText = FunctionGenerator.createFunction(funcName, funcArgs, returnType, returnDefaultValue);
        Range range = new Range(new Position(totalLines, lastCharCol + 1), new Position(totalLines + 3, lastCharCol));

        LanguageClient client = context.get(ExecuteCommandKeys.LANGUAGE_SERVER_KEY).getClient();
        return applySingleTextEdit(editText, range, textDocumentIdentifier, client);
    }

    /**
     * Execute the command, create variable.
     *
     * @param context Workspace service context
     */
    private static Object executeCreateVariable(LSServiceOperationContext context) throws WorkspaceDocumentException {
        String documentUri = null;
        String variableType = null;
        String variableName = null;
        int sLine = -1;
        int sCol = -1;
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();

        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            String argKey = ((LinkedTreeMap) arg).get(ARG_KEY).toString();
            String argVal = ((LinkedTreeMap) arg).get(ARG_VALUE).toString();
            switch (argKey) {
                case CommandConstants.ARG_KEY_DOC_URI:
                    documentUri = argVal;
                    textDocumentIdentifier.setUri(documentUri);
                    context.put(DocumentServiceKeys.FILE_URI_KEY, documentUri);
                    break;
                case CommandConstants.ARG_KEY_RETURN_TYPE:
                    variableType = argVal;
                    break;
                case CommandConstants.ARG_KEY_FUNC_LOCATION:
                    String[] split = argVal.split(",");
                    sLine = Integer.parseInt(split[0]);
                    sCol = Integer.parseInt(split[1]);
                    break;
                case CommandConstants.ARG_KEY_VAR_NAME:
                    variableName = argVal;
                    break;
                default:
                    //do nothing
            }
        }

        if (documentUri == null) {
            return new Object();
        }

        String editText = createVariableDeclaration(variableName, variableType);
        Position position = new Position(sLine - 1, sCol - 1);

        LanguageClient client = context.get(ExecuteCommandKeys.LANGUAGE_SERVER_KEY).getClient();
        return applySingleTextEdit(editText, new Range(position, position), textDocumentIdentifier, client);
    }

    /**
     * Execute the add documentation command.
     *
     * @param context Workspace service context
     */
    private static Object executeAddDocumentation(LSServiceOperationContext context) throws WorkspaceDocumentException {
        String topLevelNodeType = "";
        String documentUri = "";
        int line = 0;
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();
        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_DOC_URI)) {
                documentUri = (String) ((LinkedTreeMap) arg).get(ARG_VALUE);
                textDocumentIdentifier.setUri(documentUri);
                context.put(DocumentServiceKeys.FILE_URI_KEY, documentUri);
            } else if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_NODE_TYPE)) {
                topLevelNodeType = (String) ((LinkedTreeMap) arg).get(ARG_VALUE);
            } else if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_NODE_LINE)) {
                line = Integer.parseInt((String) ((LinkedTreeMap) arg).get(ARG_VALUE));
            }
        }
        LSCompiler lsCompiler = context.get(ExecuteCommandKeys.LS_COMPILER_KEY);
        WorkspaceDocumentManager documentManager = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY);
        BLangPackage bLangPackage = lsCompiler.getBLangPackage(context, documentManager,
                false, LSCustomErrorStrategy.class, false).getRight();
        context.put(DocumentServiceKeys.CURRENT_BLANG_PACKAGE_CONTEXT_KEY, bLangPackage);
        CommandUtil.DocAttachmentInfo docAttachmentInfo =
                getDocumentationEditForNodeByPosition(topLevelNodeType, bLangPackage, line);

        if (docAttachmentInfo != null) {
            Path filePath = Paths.get(URI.create(documentUri));
            Path compilationPath = getUntitledFilePath(filePath.toString()).orElse(filePath);
            String fileContent = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY).getFileContent(compilationPath);
            String[] contentComponents = fileContent.split(CommonUtil.LINE_SEPARATOR_SPLIT);
            int replaceEndCol = contentComponents[line].length();
            String textBeforeNode = String.join(CommonUtil.LINE_SEPARATOR,
                    Arrays.asList(Arrays.copyOfRange(contentComponents, 0, line)));
            String replaceText = String.join(CommonUtil.LINE_SEPARATOR,
                    Arrays.asList(textBeforeNode, docAttachmentInfo.getDocAttachment(), contentComponents[line]));
            Range range = new Range(new Position(0, 0), new Position(line, replaceEndCol));

            return applySingleTextEdit(replaceText, range, textDocumentIdentifier,
                    context.get(ExecuteCommandKeys.LANGUAGE_SERVER_KEY).getClient());
        }

        return new Object();
    }

    /**
     * Generate workspace edit for generating doc comments for all top level nodes and resources.
     *
     * @param context Workspace Service Context
     */
    private static Object executeAddAllDocumentation(LSServiceOperationContext context)
            throws WorkspaceDocumentException {
        String documentUri = "";
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();

        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_DOC_URI)) {
                documentUri = (String) ((LinkedTreeMap) arg).get(ARG_VALUE);
                textDocumentIdentifier.setUri(documentUri);
                context.put(DocumentServiceKeys.FILE_URI_KEY, documentUri);
            }
        }
        LSCompiler lsCompiler = context.get(ExecuteCommandKeys.LS_COMPILER_KEY);
        BLangPackage bLangPackage = lsCompiler.getBLangPackage(context,
                context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY), false, LSCustomErrorStrategy.class, false)
                .getRight();

        Path filePath = Paths.get(URI.create(documentUri));
        Path compilationPath = getUntitledFilePath(filePath.toString()).orElse(filePath);
        String fileContent = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY).getFileContent(compilationPath);
        String[] contentComponents = fileContent.split(CommonUtil.LINE_SEPARATOR_SPLIT);
        List<TextEdit> textEdits = new ArrayList<>();
        String fileName = context.get(DocumentServiceKeys.FILE_NAME_KEY);
        bLangPackage.topLevelNodes.stream()
                .filter(node -> node.getPosition().getSource().getCompilationUnitName().equals(fileName))
                .forEach(topLevelNode -> {
                    CommandUtil.DocAttachmentInfo docAttachmentInfo = getDocumentationEditForNode(topLevelNode);
                    if (docAttachmentInfo != null) {
                        textEdits.add(getTextEdit(docAttachmentInfo, contentComponents));
                    }
                    if (topLevelNode instanceof BLangService) {
                        ((BLangService) topLevelNode).getResources().forEach(bLangResource -> {
                            CommandUtil.DocAttachmentInfo resourceInfo = getDocumentationEditForNode(bLangResource);
                            if (resourceInfo != null) {
                                textEdits.add(getTextEdit(resourceInfo, contentComponents));
                            }
                        });
                    }
                });
        TextDocumentEdit textDocumentEdit = new TextDocumentEdit(textDocumentIdentifier, textEdits);
        return applyWorkspaceEdit(Collections.singletonList(textDocumentEdit),
                context.get(ExecuteCommandKeys.LANGUAGE_SERVER_KEY).getClient());
    }

    /**
     * Execute the create object constructor. Generates the snippet for the new constructor and hence the text edit.
     *
     * @param context                               LsServiceOperationContext instance for command execution
     * @return {@link Object}                       ApplyWorkspaceEditParams related to text edit
     * @throws WorkspaceDocumentException           Error while accessing the document manager
     * @throws BallerinaCommandExecutionException   Error while the command Execution
     */
    private static Object executeCreateObjectConstructor(LSServiceOperationContext context)
            throws WorkspaceDocumentException, BallerinaCommandExecutionException {
        String documentUri;
        int line = 0;
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();
        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_DOC_URI)) {
                documentUri = (String) ((LinkedTreeMap) arg).get(ARG_VALUE);
                textDocumentIdentifier.setUri(documentUri);
                context.put(DocumentServiceKeys.FILE_URI_KEY, documentUri);
            } else if (((LinkedTreeMap) arg).get(ARG_KEY).equals(CommandConstants.ARG_KEY_NODE_LINE)) {
                line = Integer.parseInt((String) ((LinkedTreeMap) arg).get(ARG_VALUE));
            }
        }
        LSCompiler lsCompiler = context.get(ExecuteCommandKeys.LS_COMPILER_KEY);
        WorkspaceDocumentManager documentManager = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY);
        BLangPackage bLangPackage = lsCompiler.getBLangPackage(context, documentManager,
                false, LSCustomErrorStrategy.class, false).getRight();
        context.put(DocumentServiceKeys.CURRENT_BLANG_PACKAGE_CONTEXT_KEY, bLangPackage);

        int finalLine = line;
        
        /*
        In the ideal situation Command execution exception should never throw. If thrown, create constructor command
        has been executed over a non object type node.
         */
        TopLevelNode objectNode = bLangPackage.topLevelNodes.stream()
                .filter(topLevelNode -> topLevelNode instanceof BLangTypeDefinition
                        && ((BLangTypeDefinition) topLevelNode).symbol.kind.equals(SymbolKind.OBJECT)
                        && topLevelNode.getPosition().getStartLine() - 1 == finalLine)
                .findAny().orElseThrow(() ->
                        new BallerinaCommandExecutionException("Error Executing Create Constructor Command"));
        List<BLangVariable> fields = ((BLangObjectTypeNode) ((BLangTypeDefinition) objectNode).typeNode).fields;
        
        DiagnosticPos zeroBasedIndex = CommonUtil.toZeroBasedPosition(CommonUtil.getLastItem(fields).getPosition());
        int lastFieldLine = zeroBasedIndex.getEndLine();
        int lastFieldOffset = zeroBasedIndex.getStartColumn();
        String constructorSnippet = CommandUtil.getObjectConstructorSnippet(fields, lastFieldOffset);
        Range range = new Range(new Position(lastFieldLine + 1, 0),
                new Position(lastFieldLine + 1, 0));

        return applySingleTextEdit(constructorSnippet, range, textDocumentIdentifier,
                context.get(ExecuteCommandKeys.LANGUAGE_SERVER_KEY).getClient());
    }

    /**
     * Get TextEdit from doc attachment info.
     *
     * @param attachmentInfo    Doc attachment info
     * @param contentComponents file content component
     * @return {@link TextEdit}     Text edit for attachment info
     */
    private static TextEdit getTextEdit(CommandUtil.DocAttachmentInfo attachmentInfo, String[] contentComponents) {
        int replaceFrom = attachmentInfo.getReplaceStartFrom();
        int replaceEndCol = contentComponents[attachmentInfo.getReplaceStartFrom()].length();
        Range range = new Range(new Position(replaceFrom, 0), new Position(replaceFrom, replaceEndCol));
        String replaceText = attachmentInfo.getDocAttachment()
                + System.lineSeparator() + contentComponents[replaceFrom];
        return new TextEdit(range, replaceText);
    }

    /**
     * Get Documentation edit for node at a given position.
     *
     * @param topLevelNodeType  top level node type
     * @param bLangPkg          BLang package
     * @param line              position to be compared with
     * @return Document attachment info
     */
    private static CommandUtil.DocAttachmentInfo getDocumentationEditForNodeByPosition(String topLevelNodeType, 
                                                                                       BLangPackage bLangPkg,
                                                                                       int line) {
        CommandUtil.DocAttachmentInfo docAttachmentInfo = null;
        switch (topLevelNodeType) {
            case UtilSymbolKeys.FUNCTION_KEYWORD_KEY:
                docAttachmentInfo = CommandUtil.getFunctionDocumentationByPosition(bLangPkg, line);
                break;
            case UtilSymbolKeys.ENDPOINT_KEYWORD_KEY:
                docAttachmentInfo = CommandUtil.getEndpointDocumentationByPosition(bLangPkg, line);
                break;
            case UtilSymbolKeys.SERVICE_KEYWORD_KEY:
                docAttachmentInfo = CommandUtil.getServiceDocumentationByPosition(bLangPkg, line);
                break;
            case UtilSymbolKeys.RECORD_KEYWORD_KEY:
            case UtilSymbolKeys.OBJECT_KEYWORD_KEY:
                docAttachmentInfo = CommandUtil.getTypeNodeDocumentationByPosition(bLangPkg, line);
                break;
            default:
                break;
        }

        return docAttachmentInfo;
    }

    /**
     * Get the documentation edit attachment info for a given particular node.
     *
     * @param node Node given
     * @return Doc Attachment Info
     */
    private static CommandUtil.DocAttachmentInfo getDocumentationEditForNode(Node node) {
        CommandUtil.DocAttachmentInfo docAttachmentInfo = null;
        int replaceFrom;
        switch (node.getKind()) {
            case FUNCTION:
                if (((BLangFunction) node).markdownDocumentationAttachment == null) {
                    replaceFrom = CommonUtil.toZeroBasedPosition(((BLangFunction) node).getPosition()).getStartLine();
                    docAttachmentInfo = CommandUtil.getFunctionNodeDocumentation((BLangFunction) node, replaceFrom);
                }
                break;
            case TYPE_DEFINITION:
                if (((BLangTypeDefinition) node).markdownDocumentationAttachment == null
                        && (((BLangTypeDefinition) node).typeNode instanceof BLangRecordTypeNode
                        || ((BLangTypeDefinition) node).typeNode instanceof BLangObjectTypeNode)) {
                    replaceFrom = CommonUtil
                            .toZeroBasedPosition(((BLangTypeDefinition) node).getPosition()).getStartLine();
                    docAttachmentInfo = CommandUtil
                            .getRecordOrObjectDocumentation((BLangTypeDefinition) node, replaceFrom);
                }
                break;
            case ENDPOINT:
                // TODO: Here we need to check for the doc attachments of the endpoint.
                replaceFrom = CommonUtil.toZeroBasedPosition(((BLangEndpoint) node).getPosition()).getStartLine();
                docAttachmentInfo = CommandUtil.getEndpointNodeDocumentation((BLangEndpoint) node, replaceFrom);
                break;
            case RESOURCE:
                if (((BLangResource) node).markdownDocumentationAttachment == null) {
                    BLangResource bLangResource = (BLangResource) node;
                    replaceFrom =
                            getReplaceFromForServiceOrResource(bLangResource, bLangResource.getAnnotationAttachments());
                    docAttachmentInfo = CommandUtil.getResourceNodeDocumentation(bLangResource, replaceFrom);
                }
                break;
            case SERVICE:
                if (((BLangService) node).markdownDocumentationAttachment == null) {
                    BLangService bLangService = (BLangService) node;
                    replaceFrom = getReplaceFromForServiceOrResource(bLangService,
                            bLangService.getAnnotationAttachments());
                    docAttachmentInfo = CommandUtil.getServiceNodeDocumentation(bLangService, replaceFrom);
                }
                break;
            default:
                break;
        }

        return docAttachmentInfo;
    }

    private static int getReplaceFromForServiceOrResource(BLangNode bLangNode,
                                                          List<BLangAnnotationAttachment> annotationAttachments) {
        if (!annotationAttachments.isEmpty()) {
            return CommonUtil.toZeroBasedPosition(CommonUtil.getLastItem(annotationAttachments)
                    .getPosition()).getEndLine() + 1;
        }

        return CommonUtil.toZeroBasedPosition(bLangNode.getPosition()).getStartLine();
    }

    private static ApplyWorkspaceEditParams applySingleTextEdit(String editText, Range range,
                                                                VersionedTextDocumentIdentifier identifier,
                                                                LanguageClient client) {
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        ApplyWorkspaceEditParams applyWorkspaceEditParams = new ApplyWorkspaceEditParams();
        TextEdit textEdit = new TextEdit(range, editText);
        TextDocumentEdit textDocumentEdit = new TextDocumentEdit(identifier,
                Collections.singletonList(textEdit));
        workspaceEdit.setDocumentChanges(Collections.singletonList(textDocumentEdit));
        applyWorkspaceEditParams.setEdit(workspaceEdit);
        if (client != null) {
            client.applyEdit(applyWorkspaceEditParams);
        }
        return applyWorkspaceEditParams;
    }

    private static Object applyWorkspaceEdit(List<TextDocumentEdit> textDocumentEdits, LanguageClient client) {
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        workspaceEdit.setDocumentChanges(textDocumentEdits);
        ApplyWorkspaceEditParams applyWorkspaceEditParams = new ApplyWorkspaceEditParams(workspaceEdit);
        if (client != null) {
            client.applyEdit(applyWorkspaceEditParams);
        }
        return applyWorkspaceEditParams;
    }
}
