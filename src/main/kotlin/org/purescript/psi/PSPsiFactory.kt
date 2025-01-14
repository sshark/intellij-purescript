package org.purescript.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.intellij.lang.annotations.Language
import org.purescript.PSLanguage
import org.purescript.ide.formatting.*
import org.purescript.psi.imports.*
import org.purescript.psi.name.PSIdentifier
import org.purescript.psi.name.PSModuleName


/**
 * This should be [com.intellij.psi.util.findDescendantOfType]
 * but is currently missing from the EAP build
 *
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 */
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

/**
 * This should be [com.intellij.psi.util.findDescendantOfType]
 * but is currently missing from the EAP build
 *
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 */
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): T? {
    var result: T? = null
    this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T && predicate(element)) {
                result = element
                stopWalking()
                return
            }

            if (canGoInside(element)) {
                super.visitElement(element)
            }
        }
    })
    return result
}

@Suppress("PSUnresolvedReference")
class PSPsiFactory(private val project: Project) {

    fun createModuleName(name: String): PSModuleName? =
        createFromText("module $name where")

    fun createImportDeclaration(importDeclaration: ImportDeclaration): PSImportDeclaration? =
        createImportDeclaration(
            importDeclaration.moduleName,
            importDeclaration.hiding,
            importDeclaration
                .importedItems
                .sortedBy { it.name }
                .sortedBy { when(it) {
                    is ImportedClass -> 1
                    is ImportedKind -> 2
                    is ImportedType -> 3
                    is ImportedData -> 4
                    is ImportedValue -> 5
                    is ImportedOperator -> 6
                } }
                .map { createImportedItem(it) ?: return null }
            ,
            importDeclaration.alias
        )

    fun createImportedItem(importedItem: ImportedItem): PSImportedItem? {
        return when (importedItem) {
            is ImportedClass -> createImportedClass(importedItem.name)
            is ImportedData -> createImportedData(
                importedItem.name,
                importedItem.doubleDot,
                importedItem.dataMembers
            )
            is ImportedKind -> createImportedKind(importedItem.name)
            is ImportedOperator -> createImportedOperator(importedItem.name)
            is ImportedType -> createImportedType(importedItem.name)
            is ImportedValue -> createImportedValue(importedItem.name)
        }
    }

    fun createImportDeclaration(
        moduleName: String,
        hiding: Boolean = false,
        importedItems: Collection<PSImportedItem> = emptyList(),
        alias: String? = null
    ): PSImportDeclaration? =
        createFromText(
            buildString {
                appendLine("module Foo where")
                append("import $moduleName")
                if (importedItems.isNotEmpty()) {
                    if (hiding) {
                        append(" hiding")
                    }
                    append(importedItems.joinToString(prefix = " (", postfix = ")") { it.text })
                }
                if (alias != null) {
                    append(" as $alias")
                }
            }
        )

    private fun createImportedClass(name: String): PSImportedClass? =
        createFromText(
            """
                module Foo where
                import Bar (class $name)
            """.trimIndent()
        )

    private fun createImportedKind(name: String): PSImportedKind? =
        createFromText(
            """
                module Foo where
                import Bar (kind $name)
            """.trimIndent()
        )

    private fun createImportedOperator(name: String): PSImportedOperator? =
        createFromText(
            """
                module Foo where
                import Bar (($name))
            """.trimIndent()
        )

    private fun createImportedType(name: String): PSImportedType? =
        createFromText(
            """
                module Foo where
                import Bar (type ($name))
            """.trimIndent()
        )

    fun createImportedData(
        name: String,
        doubleDot: Boolean = false,
        importedDataMembers: Collection<String> = emptyList()
    ): PSImportedData? =
        createFromText(
            buildString {
                appendLine("module Foo where")
                append("import Bar (")
                append(name)
                if (doubleDot) {
                    append("(..)")
                } else if (importedDataMembers.isNotEmpty()) {
                    append(importedDataMembers.joinToString(prefix = "(", postfix = ")"))
                }
                append(")")
            }
        )

    fun createImportedValue(name: String): PSImportedValue? =
        createFromText(
            """
                module Foo where
                import Bar ($name)
            """.trimIndent()
        )

    fun createNewLine(): PsiElement =
        PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n")

    private inline fun <reified T : PsiElement> createFromText(@Language("Purescript") code: String): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText(PSLanguage.INSTANCE, code)
            .findDescendantOfType()

    fun createIdentifier(name: String): PSIdentifier? {
        return createFromText(
            """
            |module Main where
            |$name = 1
        """.trimMargin()
        )
    }

    fun createParenthesis(around: String): PSParens? {
        return createFromText(
            """
            |module Main where
            |x = ($around)
        """.trimMargin()
        )
    }
}
