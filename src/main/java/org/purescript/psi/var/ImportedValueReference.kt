package org.purescript.psi.`var`

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import org.purescript.psi.ModuleReference

class ImportedValueReference(element: PSVar) : PsiReferenceBase.Poly<PSVar>(
    element,
    TextRange.allOf(element.text.trim()),
    false
) {

    override fun getVariants(): Array<PsiNamedElement> {
        val currentModule = myElement.module
        return currentModule.importedValueDeclarations.toList().toTypedArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val name = myElement.text.trim()
        val module = myElement.module
        return createResults(
            module
                .importDeclarations
                .asSequence()
                .filter { it.isNotHidingName(name) }
                .map { ModuleReference(it).resolve() }
                .filterNotNull()
                .map { it.exportedValueDeclarationsByName[name] }
                .filterNotNull()
                .flatMap { it.asSequence() }
                .filterNotNull()
                .toList()
        )
    }

}