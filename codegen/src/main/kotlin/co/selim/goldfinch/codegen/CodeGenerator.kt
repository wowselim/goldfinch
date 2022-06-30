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

fun generateSealedInterface(
  name: ClassName,
  visibilityModifier: KModifier,
): TypeSpec.Builder {
  return TypeSpec.interfaceBuilder(name)
    .addModifiers(KModifier.SEALED)
    .addModifiers(visibilityModifier)
}

fun generatePropertyContainer(
  sealedType: ClassName,
  propertyName: String,
  propertyType: TypeName,
  visibilityModifier: KModifier,
  isNested: Boolean,
): TypeSpec {
  val constructor = FunSpec.constructorBuilder()
    .addParameter(propertyName, propertyType)
    .build()

  val propertySpec = PropertySpec.builder(propertyName, propertyType)
    .initializer(propertyName)
    .addModifiers(visibilityModifier)
    .build()

  val typeName = if (isNested) propertyName.cap() else "${propertyName.cap()}Property"

  return TypeSpec.classBuilder(typeName)
    .addModifiers(KModifier.VALUE)
    .addAnnotation(JvmInline::class.asTypeName())
    .apply { if (!isNested) addModifiers(visibilityModifier) }
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
  isNested: Boolean,
): PropertySpec {
  val propertyMappings = properties.map { (propertyName, _) ->
    if (isNested) {
      "${sealedType.simpleName}.${propertyName.cap()}($propertyName),"
    } else {
      "${propertyName.cap()}Property($propertyName),"
    }
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
