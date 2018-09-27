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
package org.ballerinalang.langserver.common.utils;

import com.google.common.collect.Lists;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.ballerinalang.langserver.common.UtilSymbolKeys;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.compiler.common.LSDocument;
import org.ballerinalang.langserver.compiler.common.modal.BallerinaPackage;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentManager;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.SymbolInfo;
import org.ballerinalang.langserver.completions.util.ItemResolverConstants;
import org.ballerinalang.langserver.completions.util.Priority;
import org.ballerinalang.langserver.completions.util.Snippet;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.types.FiniteType;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BOperatorSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BServiceSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFiniteType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BIntermediateCollectionType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BJSONType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BMapType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStructureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTupleType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BXMLType;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTupleDestructure;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import static org.ballerinalang.langserver.compiler.LSCompilerUtil.getUntitledFilePath;

/**
 * Common utils to be reuse in language server implementation.
 */
public class CommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static final String LINE_SEPARATOR_SPLIT = "\\r?\\n";

    public static final boolean LS_DEBUG_ENABLED;

    public static final String BALLERINA_HOME;

    static {
        String debugLogStr = System.getProperty("ballerina.debugLog");
        LS_DEBUG_ENABLED =  debugLogStr != null && Boolean.parseBoolean(debugLogStr);
        BALLERINA_HOME = System.getProperty("ballerina.home");
    }

    private CommonUtil() {
    }

    /**
     * Get the package URI to the given package name.
     *
     * @param pkgName        Name of the package that need the URI for
     * @param currentPkgPath String URI of the current package
     * @param currentPkgName Name of the current package
     * @return String URI for the given path.
     */
    public static String getPackageURI(String pkgName, String currentPkgPath, String currentPkgName) {
        String newPackagePath;
        // If current package path is not null and current package is not default package continue,
        // else new package path is same as the current package path.
        if (currentPkgPath != null && !currentPkgName.equals(".")) {
            int indexOfCurrentPkgName = currentPkgPath.lastIndexOf(currentPkgName);
            if (indexOfCurrentPkgName >= 0) {
                newPackagePath = currentPkgPath.substring(0, indexOfCurrentPkgName);
            } else {
                newPackagePath = currentPkgPath;
            }

            if (pkgName.equals(".")) {
                newPackagePath = Paths.get(newPackagePath).toString();
            } else {
                newPackagePath = Paths.get(newPackagePath, pkgName).toString();
            }
        } else {
            newPackagePath = currentPkgPath;
        }
        return newPackagePath;
    }

    /**
     * Calculate the user defined type position.
     *
     * @param position position of the node
     * @param name     name of the user defined type
     * @param pkgAlias package alias name of the user defined type
     */
    public static void calculateEndColumnOfGivenName(DiagnosticPos position, String name, String pkgAlias) {
        position.eCol = position.sCol + name.length() + (!pkgAlias.isEmpty() ? (pkgAlias + ":").length() : 0);
    }

    /**
     * Convert the diagnostic position to a zero based positioning diagnostic position.
     *
     * @param diagnosticPos - diagnostic position to be cloned
     * @return {@link DiagnosticPos} converted diagnostic position
     */
    public static DiagnosticPos toZeroBasedPosition(DiagnosticPos diagnosticPos) {
        int startLine = diagnosticPos.getStartLine() - 1;
        int endLine = diagnosticPos.getEndLine() - 1;
        int startColumn = diagnosticPos.getStartColumn() - 1;
        int endColumn = diagnosticPos.getEndColumn() - 1;
        return new DiagnosticPos(diagnosticPos.getSource(), startLine, endLine, startColumn, endColumn);
    }

    /**
     * Replace and returns a diagnostic position with a new position.
     *
     * @param oldPos old position
     * @param newPos new position
     */
    public static void replacePosition(DiagnosticPos oldPos, DiagnosticPos newPos) {
        oldPos.sLine = newPos.sLine;
        oldPos.eLine = newPos.eLine;
        oldPos.sCol = newPos.sCol;
        oldPos.eCol = newPos.eCol;
    }

    /**
     * Get the previous default token from the given start index.
     *
     * @param tokenStream Token Stream
     * @param startIndex  Start token index
     * @return {@link Token}    Previous default token
     */
    public static Token getPreviousDefaultToken(TokenStream tokenStream, int startIndex) {
        return getDefaultTokenToLeftOrRight(tokenStream, startIndex, -1);
    }

    /**
     * Get the next default token from the given start index.
     *
     * @param tokenStream Token Stream
     * @param startIndex  Start token index
     * @return {@link Token}    Previous default token
     */
    public static Token getNextDefaultToken(TokenStream tokenStream, int startIndex) {
        return getDefaultTokenToLeftOrRight(tokenStream, startIndex, 1);
    }

    /**
     * Get n number of default tokens from a given start index.
     * @param tokenStream       Token Stream
     * @param n                 number of tokens to extract
     * @param startIndex        Start token index
     * @return {@link List}     List of tokens extracted
     */
    public static List<Token> getNDefaultTokensToLeft(TokenStream tokenStream, int n, int startIndex) {
        List<Token> tokens = new ArrayList<>();
        Token t;
        while (n > 0) {
            t = getDefaultTokenToLeftOrRight(tokenStream, startIndex, -1);
            if (t == null) {
                return new ArrayList<>();
            }
            tokens.add(t);
            n--;
            startIndex = t.getTokenIndex();
        }
        
        return Lists.reverse(tokens);
    }

    /**
     * Get the Nth Default token to the left of current token index.
     *
     * @param tokenStream Token Stream to traverse
     * @param startIndex  Start position of the token stream
     * @param offset      Number of tokens to traverse left
     * @return {@link Token}    Nth Token
     */
    public static Token getNthDefaultTokensToLeft(TokenStream tokenStream, int startIndex, int offset) {
        Token token = null;
        int indexCounter = startIndex;
        for (int i = 0; i < offset; i++) {
            token = getPreviousDefaultToken(tokenStream, indexCounter);
            indexCounter = token.getTokenIndex();
        }

        return token;
    }

    /**
     * Get the current token index from the token stream.
     *
     * @param context               LSServiceOperationContext
     * @return {@link Integer}      token index
     */
    public static int getCurrentTokenFromTokenStream(LSContext context) {
        TokenStream tokenStream = context.get(CompletionKeys.TOKEN_STREAM_KEY);
        Position position = context.get(DocumentServiceKeys.POSITION_KEY).getPosition();
        Token lastToken = null;
        int line = position.getLine();
        int col = position.getCharacter();
        int tokenLine;
        int tokenCol;
        int index = 0;
        
        if (tokenStream == null) {
            return -1;
        }
        
        while (true) {
            Token token = tokenStream.get(index);
            tokenLine = token.getLine() - 1;
            tokenCol = token.getCharPositionInLine();
            if (tokenLine > line || (tokenLine == line && tokenCol >= col)) {
                break;
            }
            index++;
            lastToken = token;
        }
        
        return lastToken == null ? -1 : lastToken.getTokenIndex();
    }

    /**
     * Pop n number of Elements from the stack and return as a List.
     * 
     * Note: If n is greater than stack, then all the elements of list will be returned
     * 
     * @param itemStack         Item Stack to pop elements from     
     * @param n                 number of elements to pop
     * @param <T>               Type of the Elements
     * @return {@link List}     List of popped Items
     */
    public static  <T> List<T> popNFromStack(Stack<T> itemStack, int n) {
        List<T> poppedList = new ArrayList<>(itemStack);
        if (n > poppedList.size()) {
            return poppedList;
        }
        
        return poppedList.subList(poppedList.size() - n, poppedList.size());
    }

    private static Token getDefaultTokenToLeftOrRight(TokenStream tokenStream, int startIndex, int direction) {
        Token token = null;
        while (true) {
            startIndex += direction;
            if (startIndex < 0 || startIndex == tokenStream.size()) {
                break;
            }
            token = tokenStream.get(startIndex);
            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                break;
            }
        }
        return token;
    }

    /**
     * Get the top level node type in the line.
     *
     * @param identifier    Document Identifier
     * @param startPosition Start position
     * @param docManager    Workspace document manager
     * @return {@link String}   Top level node type
     */
    public static String topLevelNodeTypeInLine(TextDocumentIdentifier identifier, Position startPosition,
                                                WorkspaceDocumentManager docManager) {
        List<String> topLevelKeywords = Arrays.asList("function", "service", "resource", "endpoint", "object",
                "record");
        LSDocument document = new LSDocument(identifier.getUri());

        try {
            Path filePath = document.getPath();
            Path compilationPath = getUntitledFilePath(filePath.toString()).orElse(filePath);
            String fileContent = docManager.getFileContent(compilationPath);
            String[] splitedFileContent = fileContent.split(LINE_SEPARATOR_SPLIT);
            if ((splitedFileContent.length - 1) >= startPosition.getLine()) {
                String lineContent = splitedFileContent[startPosition.getLine()];
                List<String> alphaNumericTokens = new ArrayList<>(Arrays.asList(lineContent.split("[^\\w']+")));

                for (String topLevelKeyword : topLevelKeywords) {
                    if (alphaNumericTokens.contains(topLevelKeyword)) {
                        return topLevelKeyword;
                    }
                }
            }
            return null;
        } catch (WorkspaceDocumentException e) {
            logger.error("Error occurred while reading content of file: " + document.toString());
            return null;
        }
    }

    /**
     * Get current package by given file name.
     *
     * @param packages list of packages to be searched
     * @param fileUri  string file URI
     * @return {@link BLangPackage} current package
     */
    public static BLangPackage getCurrentPackageByFileName(List<BLangPackage> packages, String fileUri) {
        Path filePath = new LSDocument(fileUri).getPath();
        Path fileNamePath = filePath.getFileName();
        BLangPackage currentPackage = null;
        try {
            found:
            for (BLangPackage bLangPackage : packages) {
                for (BLangCompilationUnit compilationUnit : bLangPackage.getCompilationUnits()) {
                    if (compilationUnit.name.equals(fileNamePath.getFileName().toString())) {
                        currentPackage = bLangPackage;
                        break found;
                    }
                }
            }
        } catch (NullPointerException e) {
            currentPackage = packages.get(0);
        }
        return currentPackage;
    }

    /**
     * Get the Annotation completion Item.
     *
     * @param packageID  Package Id
     * @param annotationSymbol BLang annotation to extract the completion Item
     * @return {@link CompletionItem}   Completion item for the annotation
     */
    public static CompletionItem getAnnotationCompletionItem(PackageID packageID, BAnnotationSymbol annotationSymbol) {
        String label = getAnnotationLabel(packageID, annotationSymbol);
        String insertText = getAnnotationInsertText(packageID, annotationSymbol);
        CompletionItem annotationItem = new CompletionItem();
        annotationItem.setLabel(label);
        annotationItem.setInsertText(insertText);
        annotationItem.setInsertTextFormat(InsertTextFormat.Snippet);
        annotationItem.setDetail(ItemResolverConstants.ANNOTATION_TYPE);

        return annotationItem;
    }

    /**
     * Get the annotation Insert text.
     *
     * @param packageID  Package ID
     * @param annotationSymbol Annotation to get the insert text
     * @return {@link String}   Insert text
     */
    private static String getAnnotationInsertText(PackageID packageID, BAnnotationSymbol annotationSymbol) {
        String pkgAlias = CommonUtil.getLastItem(packageID.getNameComps()).getValue();
        StringBuilder annotationStart = new StringBuilder();
        if (!packageID.getName().getValue().equals(Names.BUILTIN_PACKAGE.getValue())) {
            annotationStart.append(pkgAlias).append(UtilSymbolKeys.PKG_DELIMITER_KEYWORD);
        }
        if (annotationSymbol.attachedType != null) {
            annotationStart.append(annotationSymbol.getName().getValue()).append(" ")
                    .append(UtilSymbolKeys.OPEN_BRACE_KEY).append(LINE_SEPARATOR)
                    .append("\t").append("${1}").append(LINE_SEPARATOR)
                    .append(UtilSymbolKeys.CLOSE_BRACE_KEY);
        } else {
            annotationStart.append(annotationSymbol.getName().getValue());
        }
        
        return annotationStart.toString();
    }

    /**
     * Get the completion Label for the annotation.
     *
     * @param packageID  Package ID
     * @param annotation BLang annotation
     * @return {@link String}          Label string
     */
    private static String getAnnotationLabel(PackageID packageID, BAnnotationSymbol annotation) {
        String pkgComponent = "";
        if (!packageID.getName().getValue().equals(Names.BUILTIN_PACKAGE.getValue())) {
            pkgComponent = CommonUtil.getLastItem(packageID.getNameComps()).getValue()
                    + UtilSymbolKeys.PKG_DELIMITER_KEYWORD;
        }

        return pkgComponent + annotation.getName().getValue();
    }

    /**
     * Get the default value for the given BType.
     *
     * @param bType BType to get the default value
     * @return {@link String}   Default value as a String
     */
    public static String getDefaultValueForType(BType bType) {
        String typeString;
        if (bType == null) {
            return "()";
        }
        switch (bType.getKind()) {
            case INT:
                typeString = Integer.toString(0);
                break;
            case FLOAT:
                typeString = Float.toString(0);
                break;
            case STRING:
                typeString = "\"\"";
                break;
            case BOOLEAN:
                typeString = Boolean.toString(false);
                break;
            case ARRAY:
            case BLOB:
                typeString = "[]";
                break;
            case RECORD:
                typeString = "{}";
                break;
            case FINITE:
                List<String> types = new ArrayList<>();
                ((FiniteType) bType).getValueSpace().forEach(typeEntry -> types.add(typeEntry.toString()));
                types.sort(Comparator.naturalOrder());
                typeString = String.join("|", types);
                break;
            case UNION:
                String[] typeNameComps = bType.toString().split(UtilSymbolKeys.PKG_DELIMITER_KEYWORD);
                typeString = typeNameComps[typeNameComps.length - 1];
                break;
            case STREAM:
            default:
                typeString = "()";
                break;
        }
        return typeString;
    }

    /**
     * Check whether a given symbol is an endpoint object or not.
     * @param bSymbol           BSymbol to evaluate
     * @return {@link Boolean}  Symbol evaluation status
     */
    public static boolean isEndpointObject(BSymbol bSymbol) {
        if (SymbolKind.OBJECT.equals(bSymbol.kind)) {
            List<BAttachedFunction> attachedFunctions = ((BObjectTypeSymbol) bSymbol).attachedFuncs;
            for (BAttachedFunction attachedFunction : attachedFunctions) {
                if (attachedFunction.funcName.getValue().equals(UtilSymbolKeys.EP_OBJECT_IDENTIFIER)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check whether the packages list contains a given package.
     * @param pkg               Package to check
     * @param pkgList           List of packages to check against
     * @return {@link Boolean}  Check status of the package
     */
    public static boolean listContainsPackage(String pkg, List<BallerinaPackage> pkgList) {
        return pkgList.stream().anyMatch(ballerinaPackage -> ballerinaPackage.getFullPackageNameAlias().equals(pkg));
    }

    /**
     * Get completion items list for struct fields.
     * 
     * @param structFields      List of struct fields
     * @return {@link List}     List of completion items for the struct fields
     */
    public static List<CompletionItem> getStructFieldCompletionItems(List<BField> structFields) {
        List<CompletionItem> completionItems = new ArrayList<>();
        structFields.forEach(bStructField -> {
            StringBuilder insertText = new StringBuilder(bStructField.getName().getValue() + ": ");
            if (bStructField.getType() instanceof BStructureType) {
                insertText.append("{").append(LINE_SEPARATOR).append("\t${1}").append(LINE_SEPARATOR).append("}");
            } else {
                insertText.append("${1:").append(getDefaultValueForType(bStructField.getType())).append("}");
            }
            CompletionItem fieldItem = new CompletionItem();
            fieldItem.setInsertText(insertText.toString());
            fieldItem.setInsertTextFormat(InsertTextFormat.Snippet);
            fieldItem.setLabel(bStructField.getName().getValue());
            fieldItem.setDetail(ItemResolverConstants.FIELD_TYPE);
            fieldItem.setSortText(Priority.PRIORITY120.toString());
            completionItems.add(fieldItem);
        });

        return completionItems;
    }

    /**
     * Get the completion item to fill all the struct fields.
     * @param fields                    List of struct fields
     * @return {@link CompletionItem}   Completion Item to fill all the options
     */
    public static CompletionItem getFillAllStructFieldsItem(List<BField> fields) {
        List<String> fieldEntries = new ArrayList<>();

        fields.forEach(bStructField -> {
            String defaultFieldEntry = bStructField.getName().getValue()
                    + UtilSymbolKeys.PKG_DELIMITER_KEYWORD + " " + getDefaultValueForType(bStructField.getType());
            fieldEntries.add(defaultFieldEntry);
        });

        String insertText = String.join(("," + LINE_SEPARATOR), fieldEntries);
        String label = "Add All Attributes";

        CompletionItem completionItem = new CompletionItem();
        completionItem.setLabel(label);
        completionItem.setInsertText(insertText);
        completionItem.setDetail(ItemResolverConstants.NONE);
        completionItem.setSortText(Priority.PRIORITY110.toString());

        return completionItem;
    }

    /**
     * Get the BType name as string.
     * @param bType             BType to get the name
     * @param ctx               LS Operation Context
     * @return {@link String}   BType Name as String
     */
    public static String getBTypeName(BType bType, LSContext ctx) {
        PackageID pkgId = bType.tsymbol.pkgID;
        PackageID currentPkgId = ctx.get(DocumentServiceKeys.CURRENT_BLANG_PACKAGE_CONTEXT_KEY).packageID;
        String[] nameComponents = bType.toString().split(":");
        if (pkgId.toString().equals(currentPkgId.toString()) || pkgId.getName().getValue().equals("builtin")) {
            return nameComponents[nameComponents.length - 1];
        } else {
            return pkgId.getName().getValue() + UtilSymbolKeys.PKG_DELIMITER_KEYWORD
                    + nameComponents[nameComponents.length - 1];
        }
    }
    
    public static <T> T getLastItem(List<T> list) {
        return list.get(list.size() - 1);
    }

    static void populateIterableOperations(SymbolInfo variable, List<SymbolInfo> symbolInfoList, LSContext context) {
        BType bType = variable.getScopeEntry().symbol.getType();
        
        if (iterableType(bType)) {
            SymbolInfo itrForEach = getIterableOpSymbolInfo(Snippet.ITR_FOREACH, bType,
                    ItemResolverConstants.ITR_FOREACH_LABEL, context);
            SymbolInfo itrMap = getIterableOpSymbolInfo(Snippet.ITR_MAP, bType,
                    ItemResolverConstants.ITR_MAP_LABEL, context);
            SymbolInfo itrFilter = getIterableOpSymbolInfo(Snippet.ITR_FILTER, bType,
                    ItemResolverConstants.ITR_FILTER_LABEL, context);
            SymbolInfo itrCount = getIterableOpSymbolInfo(Snippet.ITR_COUNT, bType,
                    ItemResolverConstants.ITR_COUNT_LABEL, context);
            symbolInfoList.addAll(Arrays.asList(itrForEach, itrMap, itrFilter, itrCount));

            if (aggregateFunctionsAllowed(bType)) {
                SymbolInfo itrMin = getIterableOpSymbolInfo(Snippet.ITR_MIN, bType,
                        ItemResolverConstants.ITR_MIN_LABEL, context);
                SymbolInfo itrMax = getIterableOpSymbolInfo(Snippet.ITR_MAX, bType,
                        ItemResolverConstants.ITR_MAX_LABEL, context);
                SymbolInfo itrAvg = getIterableOpSymbolInfo(Snippet.ITR_AVERAGE, bType,
                        ItemResolverConstants.ITR_AVERAGE_LABEL, context);
                SymbolInfo itrSum = getIterableOpSymbolInfo(Snippet.ITR_SUM, bType,
                        ItemResolverConstants.ITR_SUM_LABEL, context);
                symbolInfoList.addAll(Arrays.asList(itrMin, itrMax, itrAvg, itrSum));
            }

            // TODO: Add support for Table and Tuple collection
        }
    }

    /**
     * Check whether the symbol is a valid invokable symbol.
     *
     * @param symbol            Symbol to be evaluated
     * @return {@link Boolean}  valid status
     */
    public static boolean isValidInvokableSymbol(BSymbol symbol) {
        if (!(symbol instanceof BInvokableSymbol)) {
            return false;
        }

        BInvokableSymbol bInvokableSymbol = (BInvokableSymbol) symbol;
        return ((bInvokableSymbol.kind == null
                && (SymbolKind.RECORD.equals(bInvokableSymbol.owner.kind)
                || SymbolKind.FUNCTION.equals(bInvokableSymbol.owner.kind)))
                || SymbolKind.FUNCTION.equals(bInvokableSymbol.kind));
    }

    static boolean isInvalidSymbol(BSymbol symbol) {
        return ("_".equals(symbol.name.getValue())
                || "runtime".equals(symbol.getName().getValue())
                || "transactions".equals(symbol.getName().getValue())
                || symbol instanceof BAnnotationSymbol
                || symbol instanceof BServiceSymbol
                || symbol instanceof BOperatorSymbol
                || symbolContainsInvalidChars(symbol));
    }

    // Private Methods
    
    private static SymbolInfo getIterableOpSymbolInfo(Snippet operation, @Nullable BType bType, String label,
                                                      LSContext context) {
        boolean isSnippet = context.get(CompletionKeys.CLIENT_CAPABILITIES_KEY).getCompletionItem().getSnippetSupport();
        String lambdaSignature = "";
        SymbolInfo.IterableOperationSignature signature;
        SymbolInfo iterableOperation = new SymbolInfo();
        switch (operation) {
            case ITR_FOREACH: {
                String params = getIterableOpLambdaParam(bType, context);
                lambdaSignature = operation.getBlock()
                        .getString(isSnippet)
                        .replace(UtilSymbolKeys.ITR_OP_LAMBDA_PARAM_REPLACE_TOKEN, params);
                break;
            }
            case ITR_MAP: {
                String params = getIterableOpLambdaParam(bType, context);
                lambdaSignature = operation.getBlock()
                        .getString(isSnippet)
                        .replace(UtilSymbolKeys.ITR_OP_LAMBDA_PARAM_REPLACE_TOKEN, params);
                break;
            }
            case ITR_FILTER: {
                String params = getIterableOpLambdaParam(bType, context);
                lambdaSignature = operation.getBlock()
                        .getString(isSnippet)
                        .replace(UtilSymbolKeys.ITR_OP_LAMBDA_PARAM_REPLACE_TOKEN, params);
                break;
            }
            case ITR_COUNT:
            case ITR_MIN:
            case ITR_MAX:
            case ITR_AVERAGE:
            case ITR_SUM:
                lambdaSignature = operation.getBlock().getString(isSnippet);
                break;
            default: {
                // Do Nothing
                break;
            }
                
        }

        signature = new SymbolInfo.IterableOperationSignature(label, lambdaSignature);
        iterableOperation.setIterableOperation(true);
        iterableOperation.setIterableOperationSignature(signature);
        return iterableOperation;
    }

    private static String getIterableOpLambdaParam(BType bType, LSContext context) {
        String params = "";
        boolean isSnippet = context.get(CompletionKeys.CLIENT_CAPABILITIES_KEY).getCompletionItem().getSnippetSupport();
        if (bType instanceof BMapType) {
            params = Snippet.ITR_ON_MAP_PARAMS.getBlock().getString(isSnippet);
        } else if (bType instanceof BArrayType) {
            params = ((BArrayType) bType).eType.toString() + " v";
        } else if (bType instanceof BJSONType) {
            params = Snippet.ITR_ON_JSON_PARAMS.getBlock().getString(isSnippet);
        } else if (bType instanceof BXMLType) {
            params = Snippet.ITR_ON_XML_PARAMS.getBlock().getString(isSnippet);
        }

        return params;
    }
    
    private static boolean iterableType(BType bType) {
        return bType instanceof BArrayType || bType instanceof BMapType || bType instanceof BJSONType
                || bType instanceof BXMLType || bType instanceof BTableType
                || bType instanceof BIntermediateCollectionType;
    }
    
    private static boolean aggregateFunctionsAllowed(BType bType) {
        return bType instanceof BArrayType && (((BArrayType) bType).eType.toString().equals("int")
                || ((BArrayType) bType).eType.toString().equals("float"));
    }

    private static boolean symbolContainsInvalidChars(BSymbol bSymbol) {
        return bSymbol.getName().getValue().contains(UtilSymbolKeys.LT_SYMBOL_KEY)
                || bSymbol.getName().getValue().contains(UtilSymbolKeys.GT_SYMBOL_KEY)
                || bSymbol.getName().getValue().contains(UtilSymbolKeys.DOLLAR_SYMBOL_KEY)
                || bSymbol.getName().getValue().equals("main")
                || bSymbol.getName().getValue().endsWith(".new");
    }

    ///////////////////////////////
    /////      Predicates     /////
    ///////////////////////////////

    /**
     * Predicate to check for the invalid symbols.
     *
     * @return {@link Predicate}    Predicate for the check
     */
    public static Predicate<SymbolInfo> invalidSymbolsPredicate() {
        return symbolInfo -> !symbolInfo.isIterableOperation()
                && symbolInfo.getScopeEntry() != null
                && isInvalidSymbol(symbolInfo.getScopeEntry().symbol);
    }

    /**
     * Generate variable code.
     *
     * @param variableName          variable name
     * @param variableType          variable type
     * @return {@link String}       generated function signature
     */
    public static String createVariableDeclaration(String variableName, String variableType) {
        return variableType  + " " + variableName + " = ";
    }

    /**
     * Generates a random name.
     *
     * @param value index of the argument
     * @param argNames argument set
     * @return random argument name
     */
    public static String generateName(int value, Set<String> argNames) {
        StringBuilder result = new StringBuilder();
        int index = value;
        while (--index >= 0) {
            result.insert(0, (char) ('a' + index % 26));
            index /= 26;
        }
        while (argNames.contains(result.toString())) {
            result = new StringBuilder(generateName(++value, argNames));
        }
        return result.toString();
    }

    /**
     * Inner class for generating function code.
     */
    public static class FunctionGenerator {

        /**
         * Generate function code.
         *
         * @param name                  function name
         * @param args                  Function arguments                             
         * @param returnType            return type
         * @param returnDefaultValue    default return value
         * @return {@link String}       generated function signature
         */
        public static String createFunction(String name, String args, String returnType, String returnDefaultValue) {
            String funcBody = CommonUtil.LINE_SEPARATOR;
            String funcReturnSignature = "";
            if (returnType != null) {
                funcBody = returnDefaultValue + funcBody;
                funcReturnSignature = " returns " + returnType + " ";
            }
            return CommonUtil.LINE_SEPARATOR + CommonUtil.LINE_SEPARATOR + "function " + name + "(" + args + ")"
                    + funcReturnSignature + "{" + CommonUtil.LINE_SEPARATOR + funcBody + "}"
                    + CommonUtil.LINE_SEPARATOR;
        }

        /**
         * Get the default function return statement.
         *
         * @param bLangNode         BLangNode to evaluate
         * @param returnStatement   return statement to modify
         * @return {@link String}   Default return statement
         */
        public static String getFuncReturnDefaultStatement(BLangNode bLangNode, String returnStatement) {
            if (bLangNode.type == null && bLangNode instanceof BLangTupleDestructure) {
                // Check for tuple assignment eg. (int, int)
                List<String> list = new ArrayList<>();
                for (BLangExpression bLangExpression : ((BLangTupleDestructure) bLangNode).varRefs) {
                    if (bLangExpression.type != null) {
                        list.add(getFuncReturnDefaultStatement(bLangExpression.type, "{%1}"));
                    }
                }
                return returnStatement.replace("{%1}", "(" + String.join(", ", list) + ")");
            } else if (bLangNode instanceof BLangLiteral) {
                return returnStatement.replace("{%1}", ((BLangLiteral) bLangNode).getValue().toString());
            } else if (bLangNode instanceof BLangAssignment) {
                return returnStatement.replace("{%1}", "0");
            }
            return (bLangNode.type != null)
                    ? getFuncReturnDefaultStatement(bLangNode.type, returnStatement)
                    : null;
        }

        private static String getFuncReturnDefaultStatement(BType bType, String returnStatement) {
            if (bType.tsymbol == null && bType instanceof BArrayType) {
                return returnStatement.replace("{%1}", "[" +
                        getFuncReturnDefaultStatement(((BArrayType) bType).eType.tsymbol, "") + "]");
            } else if (bType instanceof BFiniteType) {
                // Check for finite set assignment
                BFiniteType bFiniteType = (BFiniteType) bType;
                Set<BLangExpression> valueSpace = bFiniteType.valueSpace;
                if (!valueSpace.isEmpty()) {
                    return getFuncReturnDefaultStatement(valueSpace.stream().findFirst().get(), returnStatement);
                }
            } else if (bType instanceof BMapType && ((BMapType) bType).constraint != null) {
                // Check for constrained map assignment eg. map<Student>
                BType constraintType = ((BMapType) bType).constraint;
                String name = constraintType.tsymbol.name.getValue();
                String mapName = name.toLowerCase(Locale.ROOT) + "Map";
                String mapDef = "map<" + name + "> " + mapName + " = "
                        + "{key: " + getFuncReturnDefaultStatement(constraintType, "{%1}") + "};"
                        + CommonUtil.LINE_SEPARATOR;
                return returnStatement
                        .replace("return", mapDef + "    return")
                        .replace("{%1}", mapName);
            } else if (bType instanceof BUnionType) {
                BUnionType bUnionType = (BUnionType) bType;
                Set<BType> memberTypes = bUnionType.memberTypes;
                if (!memberTypes.isEmpty()) {
                    return getFuncReturnDefaultStatement(memberTypes.stream().findFirst().get(), returnStatement);
                }
            } else if (bType instanceof BObjectType && ((BObjectType) bType).tsymbol instanceof BObjectTypeSymbol) {
                BObjectTypeSymbol bStructSymbol = (BObjectTypeSymbol) ((BObjectType) bType).tsymbol;
                List<String> list = new ArrayList<>();
                for (BVarSymbol param : bStructSymbol.initializerFunc.symbol.params) {
                    list.add(getFuncReturnDefaultStatement(param.type.tsymbol, "{%1}"));
                }
                return returnStatement.replace("{%1}", "new " + bStructSymbol.name.getValue()
                        + "(" + String.join(", ", list) + ")");
            }
            return (bType.tsymbol != null) ? getFuncReturnDefaultStatement(bType.tsymbol, returnStatement) :
                    returnStatement.replace("{%1}", "()");
        }

        private static String getFuncReturnDefaultStatement(BTypeSymbol tSymbol, String returnStatement) {
            String result;
            switch (tSymbol.name.getValue()) {
                case "int":
                case "any":
                    result = "0";
                    break;
                case "string":
                    result = "\"\"";
                    break;
                case "float":
                    result = "0.0";
                    break;
                case "json":
                    result = "{}";
                    break;
                case "map":
                    result = "<map>{}";
                    break;
                case "boolean":
                    result = "false";
                    break;
                case "xml":
                    result = "xml ` `";
                    break;
                case "blob":
                    result = "[]";
                    break;
                default:
                    result = "()";
                    break;
            }
            return returnStatement.replace("{%1}", result);
        }

        /**
         * Returns signature of the return type.
         *
         * @param bLangNode {@link BLangNode}
         * @return return type signature
         */
        public static String getFuncReturnSignature(BLangNode bLangNode) {
            if (bLangNode.type == null && bLangNode instanceof BLangTupleDestructure) {
                // Check for tuple assignment eg. (int, int)
                List<String> list = new ArrayList<>();
                for (BLangExpression bLangExpression : ((BLangTupleDestructure) bLangNode).varRefs) {
                    if (bLangExpression.type != null) {
                        list.add(getFuncReturnSignature(bLangExpression.type));
                    }
                }
                return "(" + String.join(", ", list) + ")";
            } else if (bLangNode instanceof BLangAssignment) {
                if (((BLangAssignment) bLangNode).declaredWithVar) {
                    return "any";
                }
            }
            return (bLangNode.type != null) ? getFuncReturnSignature(bLangNode.type) : null;
        }

        /**
         * Returns signature of the return type.
         *
         * @param bType {@link BType}
         * @return return type signature
         */
        public static String getFuncReturnSignature(BType bType) {
            if (bType.tsymbol == null && bType instanceof BArrayType) {
                // Check for array assignment eg.  int[]
                return getFuncReturnSignature(((BArrayType) bType).eType.tsymbol) + "[]";
            } else if (bType instanceof BMapType && ((BMapType) bType).constraint != null) {
                // Check for constrained map assignment eg. map<Student>
                BTypeSymbol tSymbol = ((BMapType) bType).constraint.tsymbol;
                if (tSymbol != null) {
                    return "map<" + getFuncReturnSignature(tSymbol) + ">";
                }
            } else if (bType instanceof BUnionType) {
                // Check for union type assignment eg. int | string
                List<String> list = new ArrayList<>();
                for (BType memberType : ((BUnionType) bType).memberTypes) {
                    list.add(getFuncReturnSignature(memberType));
                }
                return "(" + String.join("|", list) + ")";
            } else if (bType instanceof BTupleType) {
                // Check for tuple type assignment eg. int, string
                List<String> list = new ArrayList<>();
                for (BType memberType : ((BTupleType) bType).tupleTypes) {
                    list.add(getFuncReturnSignature(memberType));
                }
                return "(" + String.join(",", list) + ")";
            }
            return (bType.tsymbol != null) ? getFuncReturnSignature(bType.tsymbol) : "any";
        }

        private static String getFuncReturnSignature(BTypeSymbol tSymbol) {
            if (tSymbol != null) {
                return tSymbol.name.getValue();
            }
            return "any";
        }

        public static List<String> getFuncArguments(BLangNode parent) {
            List<String> list = new ArrayList<>();
            if (parent instanceof BLangInvocation) {
                BLangInvocation bLangInvocation = (BLangInvocation) parent;
                if (bLangInvocation.argExprs.isEmpty()) {
                    return null;
                }
                int argCounter = 1;
                Set<String> argNames = new HashSet<>();
                for (BLangExpression bLangExpression : bLangInvocation.argExprs) {
                    if (bLangExpression instanceof BLangSimpleVarRef) {
                        BLangSimpleVarRef simpleVarRef = (BLangSimpleVarRef) bLangExpression;
                        String varName = simpleVarRef.variableName.value;
                        String argType = lookupVariableReturnType(varName, parent);
                        list.add(argType + " " + varName);
                        argNames.add(varName);
                    } else if (bLangExpression instanceof BLangInvocation) {
                        BLangInvocation invocation = (BLangInvocation) bLangExpression;
                        String functionName = invocation.name.value;
                        String argType = lookupFunctionReturnType(functionName, parent);
                        String argName = generateName(argCounter++, argNames);
                        list.add(argType + " " + argName);
                        argNames.add(argName);
                    } else {
                        String argName = generateName(argCounter++, argNames);
                        list.add("any " + argName);
                        argNames.add(argName);
                    }
                }
            }
            return (!list.isEmpty()) ? list : null;
        }

        public static List<String> getFuncArguments(BInvokableSymbol bInvokableSymbol) {
            List<String> list = new ArrayList<>();
            if (bInvokableSymbol.type instanceof BInvokableType) {
                BInvokableType bInvokableType = (BInvokableType) bInvokableSymbol.type;
                if (bInvokableType.paramTypes.isEmpty()) {
                    return list;
                }
                int argCounter = 1;
                Set<String> argNames = new HashSet<>();
                for (BType bType : bInvokableType.getParameterTypes()) {
                    String argName = generateName(argCounter++, argNames);
                    String argType = getFuncReturnSignature(bType);
                    list.add(argType + " " + argName);
                    argNames.add(argName);
                }
            }
            return (!list.isEmpty()) ? list : new ArrayList<>();
        }

        private static String lookupVariableReturnType(String variableName, BLangNode parent) {
            if (parent instanceof BLangBlockStmt) {
                BLangBlockStmt blockStmt = (BLangBlockStmt) parent;
                Scope scope = blockStmt.scope;
                if (scope != null) {
                    for (Map.Entry<Name, Scope.ScopeEntry> entry : scope.entries.entrySet()) {
                        String key = entry.getKey().getValue();
                        BSymbol symbol = entry.getValue().symbol;
                        if (variableName.equals(key) && symbol instanceof BVarSymbol) {
                            return getFuncReturnSignature(symbol.type);
                        }
                    }
                }
            }
            return (parent != null && parent.parent != null)
                    ? lookupVariableReturnType(variableName, parent.parent)
                    : "any";
        }

        private static String lookupFunctionReturnType(String functionName, BLangNode parent) {
            if (parent instanceof BLangPackage) {
                BLangPackage blockStmt = (BLangPackage) parent;
                List<BLangFunction> functions = blockStmt.functions;
                for (BLangFunction function : functions) {
                    if (functionName.equals(function.name.getValue())) {
                        return getFuncReturnSignature(function.returnTypeNode);
                    }
                }
            }
            return (parent != null && parent.parent != null)
                    ? lookupFunctionReturnType(functionName, parent.parent) : "any";
        }
    }
}
