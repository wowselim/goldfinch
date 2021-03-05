package co.selim.goldfinch.codegen

import co.selim.goldfinch.annotation.GenerateProperties
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.KmClassifier
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

@KotlinPoetMetadataPreview
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("kapt.kotlin.generated")
@SupportedAnnotationTypes("co.selim.goldfinch.annotation.GenerateProperties")
class ServiceProcessor : AbstractProcessor() {
  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val elements = roundEnv.getElementsAnnotatedWith(GenerateProperties::class.java)
    if (elements.isEmpty()) return false

    val typeElements = ElementFilter.typesIn(elements)

    typeElements.forEach { typeElement ->
      val metadataClass = typeElement.toImmutableKmClass()
      val propertiesWithTypes = metadataClass.properties
        .sortedBy(typeElement.enclosedElements)
        .filter { it.returnType.classifier is KmClassifier.Class }
        .map { property ->
          val classifier = property.returnType.classifier as KmClassifier.Class
          val typeParameters = property.returnType.extractFullType()
          val type = classifier.toClassName().safelyParameterizedBy(typeParameters)
          property.name to type
        }.toMap()

      val annotatedClass = metadataClass.name.replace('/', '.')
      generatePropertyExtension(
        ClassName.bestGuess(annotatedClass),
        propertiesWithTypes
      ).writeTo(processingEnv.filer)
    }

    return true
  }

  // recursively extracts the full type of a property e.g. List -> List<List<String>>
  private fun ImmutableKmType.extractFullType(): List<TypeName> {
    return arguments.mapNotNull { typeProjection ->
      val params = typeProjection.type?.extractFullType()

      when (val classifier = typeProjection.type?.classifier) {
        is KmClassifier.Class -> classifier.toClassName().safelyParameterizedBy(params)
        is KmClassifier.TypeParameter,
        is KmClassifier.TypeAlias,
        null -> null
      }
    }
  }

  private fun ClassName.safelyParameterizedBy(typeNames: List<TypeName>?): TypeName {
    return if (typeNames.isNullOrEmpty()) {
      this
    } else {
      this.parameterizedBy(typeNames)
    }
  }

  // this hack can be removed after https://youtrack.jetbrains.com/issue/KT-20980 has been fixed
  private fun List<ImmutableKmProperty>.sortedBy(list: List<Element>): List<ImmutableKmProperty> {
    return sortedBy { property ->
      list.indexOfFirst { it.simpleName.toString() == property.name }
    }
  }

  private fun KmClassifier.Class.toClassName(): ClassName {
    val packageName = name.substringBeforeLast('/').replace('/', '.')
    val simpleName = name.substringAfterLast('/')
    return ClassName(packageName, simpleName)
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(GenerateProperties::class.java.name)
  }

  private fun generatePropertyExtension(
    receiver: ClassName,
    properties: Map<String, TypeName>,
  ): FileSpec {
    val sealedClassName = ClassName(receiver.packageName, "${receiver.simpleName}Property")
    val sealedClass = generateSealedClass(sealedClassName)
    return generateFile(receiver)
      .addType(sealedClass)
      .apply {
        properties.forEach { (propertyName, propertyType) ->
          addType(generatePropertyContainer(sealedClassName, propertyName, propertyType))
        }
      }
      .addProperty(generatePropertyMapper(sealedClassName, receiver, properties))
      .build()
  }
}
