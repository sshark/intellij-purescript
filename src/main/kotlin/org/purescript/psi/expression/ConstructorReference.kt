package org.purescript.psi.expression

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixProvider
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.parentOfType
import org.purescript.file.ExportedConstructorsIndex
import org.purescript.psi.PSPsiElement
import org.purescript.psi.PSPsiFactory
import org.purescript.psi.data.PSDataDeclaration
import org.purescript.psi.name.PSQualifiedProperName
import org.purescript.psi.newtype.PSNewTypeDeclaration

class ConstructorReference(
    element: PSPsiElement,
    val qualifiedProperName: PSQualifiedProperName
) :
    LocalQuickFixProvider,
    PsiReferenceBase<PSPsiElement>(
        element,
        qualifiedProperName.properName.textRangeInParent,
        false
    ) {

    override fun getVariants(): Array<Any> =
        candidates.toList().toTypedArray()

    override fun resolve(): PsiNamedElement? =
        candidates.firstOrNull { it.name == element.name }

    private val candidates: Sequence<PsiNamedElement>
        get() {
            val module = element.module ?: return emptySequence()
            val qualifyingName = qualifiedProperName.moduleName?.name
            return sequence {
                if (qualifyingName == null) {
                    yieldAll(module.newTypeConstructors)
                    yieldAll(module.dataConstructors)
                }
                val importDeclarations = module.importDeclarations
                    .filter { it.importAlias?.name == qualifyingName }
                yieldAll(importDeclarations.flatMap { it.importedNewTypeConstructors })
                yieldAll(importDeclarations.flatMap { it.importedDataConstructors })
            }
        }

    override fun getQuickFixes(): Array<LocalQuickFix> {
        val factory = PSPsiFactory(element.project)
        val quickFixes = mutableListOf<LocalQuickFix>()
        for ((moduleName, typeName) in importCandidates) {
            val importedData = factory.createImportedData(typeName, true)
                ?: return emptyArray()
            quickFixes += ImportQuickFix(moduleName, importedData)
        }
        return quickFixes.toTypedArray()
    }

    private val importCandidates: Set<Pair<String, String>>
        get() {
            val modules = ExportedConstructorsIndex
                .filesExportingConstructor(element.project, qualifiedProperName.name)
                .mapNotNull { it.module }

            val importCandidates = mutableSetOf<Pair<String, String>>()
            for (module in modules) {
                for (exportedDataConstructor in module.exportedDataConstructors) {
                    if (exportedDataConstructor.name == qualifiedProperName.name) {
                        val dataDeclaration = exportedDataConstructor.parentOfType<PSDataDeclaration>()
                            ?: continue
                        importCandidates += module.name to dataDeclaration.name
                    }
                }
                for (exportedNewTypeConstructor in module.exportedNewTypeConstructors) {
                    if (exportedNewTypeConstructor.name == qualifiedProperName.name) {
                        val newTypeDeclaration = exportedNewTypeConstructor.parentOfType<PSNewTypeDeclaration>()
                            ?: continue
                        importCandidates += module.name to newTypeDeclaration.name
                    }
                }
            }
            return importCandidates
        }

}
