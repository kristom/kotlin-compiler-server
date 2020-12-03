package indexation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import common.model.ImportInfo
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.io.File

internal fun createJsonWithIndexesJS(directory: String, outputPath: String) {
  val kotlinEnvironmentConfiguration = KotlinEnvironmentConfiguration(librariesFiles(directory))
  val kotlinEnvironment = kotlinEnvironmentConfiguration.kotlinEnvironment()
  val imports = getAllVariants(kotlinEnvironment)
  File(outputPath).writeText(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(imports))
}

fun librariesFiles(name: String) = LibrariesFile(
  File(name),
  File("$name-js")
)

fun getAllVariants(kotlinEnvironment: KotlinEnvironment): List<ImportInfo> {
  val imports = mutableListOf<ImportInfo>()
  kotlinEnvironment.environment {  coreEnvironment ->
    val project = coreEnvironment.project
    val configuration = JsConfig(
      project,
      kotlinEnvironment.jsConfiguration,
      kotlinEnvironment.JS_METADATA_CACHE,
      kotlinEnvironment.JS_LIBRARIES.toSet()
    )

    configuration.moduleDescriptors.forEach { moduleDescriptor ->
      val packages = moduleDescriptor.allPackages()
      packages.forEach { fqName ->
        val packageViewDescriptor = moduleDescriptor.getPackage(fqName)
        val descriptors = packageViewDescriptor.memberScope
          .getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)
        imports.addAll(descriptors.mapNotNull { it.toImportInfo() })
      }
    }
  }
  return imports.distinct()
}
