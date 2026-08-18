package app.cash.paparazzi.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Premain entry point, activated when the JVM starts with {@code -javaagent} pointing at the
 * paparazzi jar (the Gradle plugin adds this to test tasks). Strips the {@code final} modifier
 * from static fields of {@code android.os.Build} and its nested classes as they are loaded, so
 * they can later be written with plain reflection instead of {@code sun.misc.Unsafe}, whose
 * memory-access methods are terminally deprecated (JEP 471).
 *
 * Running at premain closes the load-order gap that {@link FinalFieldStripper} cannot: test code
 * may load {@code Build} before any Paparazzi class initializes (e.g. from a test class companion
 * object, or from an unrelated test that runs earlier in the same JVM).
 *
 * This class must only reference the JDK: at premain time in Gradle test workers the application
 * classpath (Kotlin, ASM) is not yet visible to the system class loader, which is also why the
 * class file is rewritten by hand instead of with ASM.
 */
public final class PaparazziAgent {
  private static final int ACC_STATIC = 0x0008;
  private static final int ACC_FINAL = 0x0010;

  private PaparazziAgent() {
  }

  public static void premain(@SuppressWarnings("unused") String args, Instrumentation instrumentation) {
    instrumentation.addTransformer(
      new ClassFileTransformer() {
        @Override
        public byte[] transform(
          ClassLoader loader,
          String internalName,
          Class<?> classBeingRedefined,
          ProtectionDomain protectionDomain,
          byte[] classfileBuffer
        ) {
          if (internalName == null) {
            return null;
          }
          if (!internalName.equals("android/os/Build") && !internalName.startsWith("android/os/Build$")) {
            return null;
          }
          try {
            return stripFinalFromStaticFields(classfileBuffer);
          } catch (RuntimeException e) {
            // Keep the original bytes; the eventual reflective write fails with a clearer error
            // than an exception swallowed by the JVM here.
            return null;
          }
        }
      }
    );
  }

  /**
   * Returns a copy of {@code classBytes} with {@code ACC_FINAL} cleared on every static field,
   * or null if no field needed changing. See JVMS §4.1 for the class file structure walked here.
   */
  private static byte[] stripFinalFromStaticFields(byte[] classBytes) {
    byte[] bytes = classBytes.clone();
    if (readU4(bytes, 0) != 0xCAFEBABEL) {
      return null;
    }
    int offset = 8; // magic + minor_version + major_version

    int constantPoolCount = readU2(bytes, offset);
    offset += 2;
    for (int i = 1; i < constantPoolCount; i++) {
      int tag = bytes[offset] & 0xFF;
      offset += 1;
      switch (tag) {
        case 1: // Utf8
          offset += 2 + readU2(bytes, offset);
          break;
        case 3: // Integer
        case 4: // Float
        case 9: // Fieldref
        case 10: // Methodref
        case 11: // InterfaceMethodref
        case 12: // NameAndType
        case 17: // Dynamic
        case 18: // InvokeDynamic
          offset += 4;
          break;
        case 5: // Long
        case 6: // Double
          offset += 8;
          i++; // occupies two constant pool slots
          break;
        case 7: // Class
        case 8: // String
        case 16: // MethodType
        case 19: // Module
        case 20: // Package
          offset += 2;
          break;
        case 15: // MethodHandle
          offset += 3;
          break;
        default:
          throw new IllegalStateException("Unknown constant pool tag " + tag);
      }
    }

    offset += 6; // access_flags + this_class + super_class
    offset += 2 + 2 * readU2(bytes, offset); // interfaces_count + interfaces

    int fieldsCount = readU2(bytes, offset);
    offset += 2;
    boolean changed = false;
    for (int i = 0; i < fieldsCount; i++) {
      int accessFlags = readU2(bytes, offset);
      if ((accessFlags & ACC_STATIC) != 0 && (accessFlags & ACC_FINAL) != 0) {
        writeU2(bytes, offset, accessFlags & ~ACC_FINAL);
        changed = true;
      }
      offset += 6; // access_flags + name_index + descriptor_index
      int attributesCount = readU2(bytes, offset);
      offset += 2;
      for (int j = 0; j < attributesCount; j++) {
        offset += 2; // attribute_name_index
        offset += 4 + (int) readU4(bytes, offset); // attribute_length + info
      }
    }

    return changed ? bytes : null;
  }

  private static int readU2(byte[] bytes, int offset) {
    return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
  }

  private static long readU4(byte[] bytes, int offset) {
    return ((long) (bytes[offset] & 0xFF) << 24) |
      ((bytes[offset + 1] & 0xFF) << 16) |
      ((bytes[offset + 2] & 0xFF) << 8) |
      (bytes[offset + 3] & 0xFF);
  }

  private static void writeU2(byte[] bytes, int offset, int value) {
    bytes[offset] = (byte) (value >>> 8);
    bytes[offset + 1] = (byte) value;
  }
}
