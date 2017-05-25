package net.kenro.ji.jin.purescript.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import net.kenro.ji.jin.purescript.file.PSFile;
import net.kenro.ji.jin.purescript.file.PSFileStubType;
import net.kenro.ji.jin.purescript.lexer.PSLexer;
import net.kenro.ji.jin.purescript.psi.PSTokens;
import net.kenro.ji.jin.purescript.psi.cst.PSASTWrapperElement;
import org.jetbrains.annotations.NotNull;

public class PSParserDefinition implements ParserDefinition, PSTokens {
    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new PSLexer();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new PureParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return PSFileStubType.INSTANCE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return kStrings;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return new PSASTWrapperElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new PSFile(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
