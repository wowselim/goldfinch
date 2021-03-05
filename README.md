# Goldfinch
[![](https://jitpack.io/v/wowselim/goldfinch.svg)](https://jitpack.io/#wowselim/goldfinch)

Goldfinch generates kotlin code that lets you iterate
over the properties of a class exhaustively.

This can be useful when implementing validation
or custom serialization. The compile-time safety when combined
with exhaustive `when`-statements guarantees that no
properties are missed.

## Getting started
Simply annotate a class with `@GenerateProperties` to
enable code generation:
```kotlin
@GenerateProperties
data class Person(val name: String, val dateOfBirth: LocalDate)
```
To iterate over the properties of a `Person` you can
use the generated extension property `Person#properties`
like so:
```kotlin
val person = Person("Selim", LocalDate.of(1970, 1, 1))
selim.properties
  .forEach { property ->
    val message = when (property) {
      is NameProperty -> "Name: ${property.name}"
      is DateOfBirthProperty -> {
        val currentYear = LocalDate.now()
        "Age: ${property.dateOfBirth.until(currentYear).years}"
      }
    }

    println(message)
  }
```

To see it in action, check out the `example` module.

## Adding it to your project
Add the
[JitPack repository](https://jitpack.io/#wowselim/goldfinch)
to your build script and include the following dependencies:

```groovy
implementation 'com.github.wowselim.goldfinch:goldfinch-annotation:<latestVersion>'
kapt 'com.github.wowselim.goldfinch:goldfinch-codegen:<latestVersion>'
```

The latest version can be found in the
[releases section](https://github.com/wowselim/goldfinch/releases/latest).
