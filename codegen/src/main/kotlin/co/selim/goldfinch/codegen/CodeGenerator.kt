package co.selim.goldfinch.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun generateFile(
  receiver: ClassName
): FileSpec.Builder {
  return FileSpec.builder(
    packageName = receiver.packageName,
    fileName = "${receiver.simpleName.capitalize()}Properties"
  )
}

fun generateSealedClass(
  name: ClassName,
  visibilityModifier: KModifier,
): TypeSpec {
  return TypeSpec.classBuilder(name)
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

  return TypeSpec.classBuilder("${propertyName.capitalize()}Property")
    .addModifiers(KModifier.DATA)
    .addModifiers(visibilityModifier)
    .primaryConstructor(constructor)
    .addProperty(propertySpec)
    .superclass(sealedType)
    .build()
}

internal fun generatePropertyMapper(
  sealedType: ClassName,
  receiver: ClassName,
  properties: Map<String, TypeName>,
  visibilityModifier: KModifier,
): PropertySpec {
  val propertyMappings = properties.map { (propertyName, _) ->
    "${propertyName.capitalize()}Property($propertyName),"
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
