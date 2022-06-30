package co.selim.goldfinch.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.*

internal fun generateFile(
  receiver: ClassName
): FileSpec.Builder {
  return FileSpec.builder(
    packageName = receiver.packageName,
    fileName = "${receiver.simpleName.cap()}Properties"
  )
}

fun generateSealedClass(
  name: ClassName,
  visibilityModifier: KModifier,
): TypeSpec {
  return TypeSpec.interfaceBuilder(name)
    .addModifiers(KModifier.SEALED)
    .addModifiers(visibilityModifier)
    .build()
}

fun generatePropertyContainer(
  sealedType: ClassName,
  propertyName: String,
  propertyType: TypeName,
  visibilityModifier: KModifier,
): TypeSpec {
  val constructor = FunSpec.constructorBuilder()
    .addParameter(propertyName, propertyType)
    .build()

  val propertySpec = PropertySpec.builder(propertyName, propertyType)
    .initializer(propertyName)
    .addModifiers(visibilityModifier)
    .build()

  return TypeSpec.classBuilder("${propertyName.cap()}Property")
    .addModifiers(KModifier.VALUE)
    .addAnnotation(JvmInline::class.asTypeName())
    .addModifiers(visibilityModifier)
    .primaryConstructor(constructor)
    .addProperty(propertySpec)
    .addSuperinterface(sealedType)
    .build()
}

internal fun generatePropertyMapper(
  sealedType: ClassName,
  receiver: ClassName,
  properties: Map<String, TypeName>,
  visibilityModifier: KModifier,
): PropertySpec {
  val propertyMappings = properties.map { (propertyName, _) ->
    "${propertyName.cap()}Property($propertyName),"
  }.joinToString("\n      ")

  val code = """
    return setOf(
      $propertyMappings
    )
  """.trimIndent()
  val getter = FunSpec.getterBuilder().addCode(code).build()

  return PropertySpec.builder("properties", Set::class.asTypeName().parameterizedBy(sealedType))
    .receiver(receiver)
    .getter(getter)
    .addModifiers(visibilityModifier)
    .build()
}

private fun String.cap(): String {
  return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
