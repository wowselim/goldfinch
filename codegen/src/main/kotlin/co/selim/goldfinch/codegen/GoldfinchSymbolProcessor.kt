package co.selim.goldfinch.codegen

import co.selim.goldfinch.annotation.GenerateProperties
import co.selim.goldfinch.annotation.Level
import co.selim.goldfinch.annotation.Visibility
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

class GoldfinchSymbolProcessor(
  private val logger: KSPLogger,
  private val codeGenerator: CodeGenerator
) : SymbolProcessor {

  private fun ClassName.safelyParameterizedBy(typeNames: List<TypeName>?): TypeName {
    return if (typeNames.isNullOrEmpty()) {
      this
    } else {
      this.parameterizedBy(typeNames)
    }
  }

  private fun logError(msg: String, node: KSNode? = null): Nothing {
    logger.error(msg, node)
    error(msg)
  }

  private fun generatePropertyExtension(
    receiver: ClassName,
    properties: Map<String, TypeName>,
    visibilityModifier: KModifier,
    nestProperties: Boolean,
  ): FileSpec {
    val sealedInterfaceName = ClassName(receiver.packageName, "${receiver.simpleName}Property")
    val sealedInterface = generateSealedInterface(sealedInterfaceName, visibilityModifier)
    val propertySpecs = properties.map { (propertyName, propertyType) ->
      generatePropertyContainer(sealedInterfaceName, propertyName, propertyType, visibilityModifier, nestProperties)
    }
    return generateFile(receiver)
      .apply {
        if (nestProperties)
          addType(sealedInterface.addTypes(propertySpecs).build())
        else {
          addType(sealedInterface.build())
          propertySpecs.forEach(::addType)
        }
      }
      .addProperty(
        generatePropertyMapper(
          sealedInterfaceName,
          receiver,
          properties,
          visibilityModifier,
          nestProperties
        )
      )
      .build()
  }

  private fun KSType.getFullType(): TypeName {
    val typeParams = arguments.mapNotNull { argument ->
      val resolvedType = argument.type?.resolve()

      resolvedType?.getFullType()?.copy(nullable = resolvedType.isMarkedNullable)
    }

    val fullName = declaration.qualifiedName!!.asString()
    val simpleName = fullName.substringAfter(declaration.packageName.asString())
    return ClassName(declaration.packageName.asString(), simpleName)
      .safelyParameterizedBy(typeParams)
      .copy(nullable = isMarkedNullable)
  }

  @OptIn(KspExperimental::class)
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(GenerateProperties::class.java.name)
      .filterIsInstance(KSClassDeclaration::class.java)
      .forEach { ksClassDeclaration ->
        val annotatedClass = ksClassDeclaration.qualifiedName!!.asString()
        val properties = ksClassDeclaration.getAllProperties()
          .associate { ksPropertyDeclaration ->
            val propertyName = ksPropertyDeclaration.simpleName.getShortName()
            propertyName to ksPropertyDeclaration.type.resolve().getFullType()
          }

        val annotation = ksClassDeclaration.getAnnotationsByType(GenerateProperties::class).first()
        val visibilityModifier = annotation.visibility.toKModifier(ksClassDeclaration)
        val dependencies = Dependencies(true, ksClassDeclaration.containingFile!!)

        codeGenerator.createNewFile(
          dependencies,
          ksClassDeclaration.packageName.asString(),
          "${annotatedClass}Properties"
        )
          .bufferedWriter()
          .use { writer ->
            generatePropertyExtension(
              ClassName.bestGuess(annotatedClass),
              properties,
              visibilityModifier,
              annotation.level == Level.NESTED
            ).writeTo(writer)
          }
      }

    return emptyList()
  }


  private fun Visibility.toKModifier(classDeclaration: KSClassDeclaration): KModifier {
    return when (this) {
      Visibility.PUBLIC -> KModifier.PUBLIC
      Visibility.INTERNAL -> KModifier.INTERNAL
      Visibility.INHERIT -> classDeclaration.getVisibilityKModifier()
    }
  }

  private fun KSClassDeclaration.getVisibilityKModifier(): KModifier {
    return when (val visibility = this.getVisibility()) {
      com.google.devtools.ksp.symbol.Visibility.PUBLIC -> KModifier.PUBLIC
      com.google.devtools.ksp.symbol.Visibility.PROTECTED -> KModifier.PROTECTED
      com.google.devtools.ksp.symbol.Visibility.INTERNAL -> KModifier.INTERNAL
      com.google.devtools.ksp.symbol.Visibility.PRIVATE,
      com.google.devtools.ksp.symbol.Visibility.LOCAL,
      com.google.devtools.ksp.symbol.Visibility.JAVA_PACKAGE -> {
        val msg = "Visibility '$visibility' on class '${this.qualifiedName!!.asString()}' is not supported"
        logError(msg, this)
      }
    }
  }
}
