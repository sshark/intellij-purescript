package org.purescript.psi.imports

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiNamedElement
import org.purescript.psi.*
import org.purescript.psi.data.PSDataDeclaration
import kotlin.reflect.KProperty1

/**
 * An import declaration, as found near the top a module.
 *
 * E.g.
 * ```
 * import Foo.Bar hiding (a, b, c) as FB
 * ```
 */
class PSImportDeclarationImpl(node: ASTNode) : PSPsiElement(node) {

    /**
     * The identifier specifying module being imported, e.g.
     *
     * `Foo.Bar` in
     * ```
     * import Foo.Bar as Bar
     * ```
     */
    val importName get() = findChildByClass(PSProperName::class.java)

    /**
     * The import list of this import declaration.
     * It being null implies that all items are imported.
     */
    val importList: PSImportList?
        get() =
            findChildByClass(PSImportList::class.java)

    /**
     * @return the import alias of this import declaration,
     * if it has one.
     */
    val importAlias: PSImportAlias?
        get() =
            findChildByClass(PSImportAlias::class.java)

    /**
     * Either the name of the [PSImportAlias], if it exists,
     * or the name of the module this declaration is importing from.
     */
    override fun getName() =
        importAlias?.name ?: importName?.name

    /** the names that are exposed or hidden
     *
     * `import Lib (namedImports)`
     * */
    val namedImports: List<String>
        get() =
            importList
                ?.importedItems
                ?.map { it.name }
                ?: emptyList()

    /** is the import statement a hiding
     *
     * `import Lib hiding (x)`
     * */
    val isHiding: Boolean
        get() =
            importList?.isHiding ?: false

    /**
     * @return a reference to the [PSModule] that this declaration is
     * importing from
     */
    override fun getReference(): ModuleReference =
        ModuleReference(this)

    /**
     * Helper method for retrieving various types of imported declarations.
     *
     * @param exportedDeclarationProperty The property for the exported declarations of the wanted type in the module
     * @param importedItemClass The class of the [PSImportedItem] to use when filtering the results
     * @return the [Declaration] elements that this declaration imports
     */
    private fun <Declaration : PsiNamedElement> getImportedDeclarations(
        exportedDeclarationProperty: KProperty1<PSModule, List<Declaration>>,
        importedItemClass: Class<out PSImportedItem>
    ): List<Declaration> {
        val importedModule = importedModule ?: return emptyList()
        val exportedDeclarations = exportedDeclarationProperty.get(importedModule)

        val importedItems = importList?.importedItems
            ?: return exportedDeclarations

        val importedNames = importedItems.filterIsInstance(importedItemClass)
            .map { it.name }
            .toSet()

        return if (isHiding) {
            exportedDeclarations.filter { it.name !in importedNames }
        } else {
            exportedDeclarations.filter { it.name in importedNames }
        }
    }

    /**
     * @return the [PSModule] that this declaration is importing from
     */
    val importedModule get(): PSModule? = reference.resolve()

    /**
     * @return the [PSValueDeclaration] elements imported by this declaration
     */
    val importedValueDeclarations: List<PSValueDeclaration>
        get() = getImportedDeclarations(
            PSModule::exportedValueDeclarations,
            PSImportedValue::class.java
        )

    /**
     * @return the [PSForeignValueDeclaration] elements imported by this declaration
     */
    val importedForeignValueDeclarations: List<PSForeignValueDeclaration>
        get() = getImportedDeclarations(
            PSModule::exportedForeignValueDeclarations,
            PSImportedValue::class.java
        )

    /**
     * @return the [PSNewTypeDeclarationImpl] elements imported by this declaration
     */
    val importedNewTypeDeclarations: List<PSNewTypeDeclarationImpl>
        get() = getImportedDeclarations(
            PSModule::exportedNewTypeDeclarations,
            PSImportedData::class.java
        )

    /**
     * @return the [PSDataDeclaration] elements imported by this declaration
     */
    val importedDataDeclarations: List<PSDataDeclaration>
        get() = getImportedDeclarations(
            PSModule::exportedDataDeclarations,
            PSImportedData::class.java
        )
}