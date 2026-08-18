package app.cash.paparazzi.agent

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import java.util.concurrent.ConcurrentHashMap

/**
 * Strips the `final` modifier from static fields of targeted classes as they are loaded, so they
 * can later be written with plain reflection instead of `sun.misc.Unsafe`, whose memory-access
 * methods are terminally deprecated (JEP 471) and disallowed by default in newer JDKs.
 *
 * Targets must be registered before the class is loaded: the JVM does not allow changing field
 * modifiers through retransformation of an already-loaded class. [PaparazziAgent] covers this
 * from premain when the Gradle plugin's -javaagent is present; this attach-based path is the
 * fallback for JVMs launched without it.
 */
internal object FinalFieldStripper {
  private val targetInternalNames = ConcurrentHashMap.newKeySet<String>()

  @Volatile
  private var installed = false

  /** Registers [className] and its nested classes for stripping. */
  fun stripFinalFromStaticFields(className: String) {
    targetInternalNames += className.replace('.', '/')

    if (!installed) {
      synchronized(this) {
        if (!installed) {
          AgentInstaller.install().addTransformer(
            object : ClassFileTransformer {
              override fun transform(
                loader: ClassLoader?,
                internalName: String?,
                classBeingRedefined: Class<*>?,
                protectionDomain: ProtectionDomain?,
                classfileBuffer: ByteArray
              ): ByteArray? {
                if (internalName == null || !isTarget(internalName)) return null
                return try {
                  stripFinal(classfileBuffer)
                } catch (e: Throwable) {
                  // Returning null keeps the original bytes; the eventual reflective write will
                  // fail with a clearer error than an exception swallowed by the JVM here.
                  null
                }
              }
            }
          )
          installed = true
        }
      }
    }
  }

  private fun isTarget(internalName: String): Boolean =
    targetInternalNames.any { internalName == it || internalName.startsWith("$it\$") }

  private fun stripFinal(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    reader.accept(
      object : ClassVisitor(Opcodes.ASM9, writer) {
        override fun visitField(
          access: Int,
          name: String,
          descriptor: String,
          signature: String?,
          value: Any?
        ): FieldVisitor {
          val isStatic = access and Opcodes.ACC_STATIC != 0
          val newAccess = if (isStatic) access and Opcodes.ACC_FINAL.inv() else access
          return super.visitField(newAccess, name, descriptor, signature, value)
        }
      },
      0
    )
    return writer.toByteArray()
  }
}
