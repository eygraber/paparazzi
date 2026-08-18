package app.cash.paparazzi

import java.lang.reflect.Field
import java.lang.reflect.Modifier

internal fun Class<*>.getFieldReflectively(fieldName: String): Field =
  try {
    this.getDeclaredField(fieldName).also { it.isAccessible = true }
  } catch (e: NoSuchFieldException) {
    throw RuntimeException("Field '$fieldName' was not found in class $name.")
  }

internal fun Field.setStaticValue(value: Any) {
  try {
    this.isAccessible = true
    this.set(null, value)
  } catch (ex: IllegalAccessException) {
    if (Modifier.isFinal(modifiers)) {
      // PaparazziAgent/FinalFieldStripper can only strip modifiers at class load time
      throw RuntimeException(
        "Cannot set static final field '${declaringClass.name}.$name'; " +
          "its class was loaded before Paparazzi could remove the final modifier. " +
          "Is the test JVM running without the Paparazzi Gradle plugin's -javaagent?",
        ex
      )
    }
    throw RuntimeException(ex)
  } catch (ex: SecurityException) {
    throw RuntimeException(ex)
  } catch (ex: IllegalArgumentException) {
    throw RuntimeException(ex)
  }
}
