/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.faces.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.faces.util.FacesLogger;

/**
 * <p>
 * Factory for dynamically generating PropertyEditor classes that extend
 * {@link ConverterPropertyEditorBase} and replace any references to the target
 * class from the template with a supplied target class.
 * </p>
 */
public class ConverterPropertyEditorFactory {

    private static final Logger LOGGER = FacesLogger.APPLICATION.getLogger();

    /**
     * <p>
     * Capture information extracted from a "template" PropertyEditor class, and
     * perform manipulation of the byte codes in order to generate the bytes for
     * a new PropertyEditor class.
     * </p>
     * <p>
     * The new class bytes are generated by identifying UTF8Info entries in the
     * constant pool of the template class, and replacing them with new UTF8
     * constants to define a new class. The constants to be replaced are those
     * for:
     * <ul>
     * <li>The name of the class itself
     * (com/sun/faces/application/ConverterPropertyEditorFor_XXXX).</li>
     * <li>The class name as a type reference
     * (Lcom/sun/faces/application/ConverterPropertyEditorFor_XXXX;).</li>
     * <li>The name of the <i>target class</i> that the editor will be
     * manipulating (java/util/Date in the current template).</li>
     * </ul>
     * </p>
     */
    private static class ClassTemplateInfo {
        /**
         * Capture details of the location of a UTF8Info entry in the constant
         * pool of the template class.
         */
        private static class Utf8InfoRef {
            
            /**
             * The position of the constant in the byte array that defines the
             * template class.
             */
            int index;
            /**
             * The number of bytes that the constant occupies in the byte array
             * that defines the template class.
             */
            int length;

            public Utf8InfoRef(int index, int length) {
                super();               
                this.index = index;
                this.length = length;
            }
        }

        /**
         * Capture details of a single substitution to be made in the template
         * class while generating the new class. Implements
         * {@link java.lang.Comparable} so that the replacements can be ordered
         * according to the order they appear in the source.
         */
        private static class Utf8InfoReplacement implements
            Comparable<Utf8InfoReplacement> {
            /**
             * The utf8 constant reference from the template source.
             */
            Utf8InfoRef ref;
            /**
             * The bytes to replace the constant with (must also be a valid utf8
             * constant pool entry).
             */
            byte[] replacement;

            public Utf8InfoReplacement(Utf8InfoRef ref, String replacement) {
                super();
                this.ref = ref;
                this.replacement = getUtf8InfoBytes(replacement);
            }

            /**
             * Order by the index position of the source UTF8Info reference.
             */
            public int compareTo(Utf8InfoReplacement rhs) {
                return ref.index - rhs.ref.index;
            }
        }

        // The source template class on which to base the definition of the new
        // PropertyEditor classes.
        private Class<? extends ConverterPropertyEditorBase> templateClass;
        // The bytes that define the source template class.
        private byte[] templateBytes;
        // The constant_pool_count from the template class bytecodes.
        private int constant_pool_count;
        // Reference to the class name utf8 constant
        private Utf8InfoRef classNameConstant;
        // Reference to the class name ref utf8 constant
        private Utf8InfoRef classNameRefConstant;
        // Reference to the target class name utf8 constant
        private Utf8InfoRef targetClassConstant;

        /**
         * Default constructor uses the {@link ConverterPropertyEditorFor_XXXX}
         * class as the source template.
         */
        public ClassTemplateInfo() {
            this(ConverterPropertyEditorFor_XXXX.class);
        }

        /**
         * Construct a template info instance based on the supplied class.
         * 
         * @param templateClass
         *            is a "template" class (but not in the java generics sense)
         *            which must extend {@link ConverterPropertyEditorBase} and
         *            override the
         *            {@link ConverterPropertyEditorBase#getTargetClass} method.
         */
        public ClassTemplateInfo(
            Class<? extends ConverterPropertyEditorBase> templateClass) {
            this.templateClass = templateClass;
            try {
                ConverterPropertyEditorBase tc = templateClass.newInstance();
                Class<?> templateTargetClass = tc.getTargetClass();
                loadTemplateBytes();
                classNameConstant = findConstant(getVMClassName(templateClass));
                classNameRefConstant = findConstant(
                     new StringBuilder(64).append('L').append(getVMClassName(templateClass)).append(';').toString());
                targetClassConstant = findConstant(getVMClassName(templateTargetClass));
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                               "Unexected exception ClassTemplateInfo",
                               e);
                }
            }
        }

        /**
         * Check whether the <code>targetBytes</code> match the content of the
         * <code>templateBytes</code> at the given <code>index</code>.
         * 
         * @param targetBytes
         *            byte array to compare.
         * @param index
         *            the index into <code>templateBytes</code> at which to
         *            compare.
         * @return true if the bytes from <code>targetBytes</code> match the
         *         bytes from <code>templateBytes</code>.
         */
        private boolean matchAtIndex(byte[] targetBytes, int index) {
            if (index < 0 || index + targetBytes.length > templateBytes.length) {
                return false;
            }
            for (int i = 0; i < targetBytes.length; ++i) {
                if (targetBytes[i] != templateBytes[index + i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Find an instance of UTF8Info in the source class's constant pool
         * where the text matches the given argument.
         * 
         * @param text
         *            the text that the UTF8Info must contain.
         * @return A {@link Utf8InfoRef} instance refering to the matched
         *         constant pool entry, or <code>null</code> if no match was
         *         found.
         */
        private Utf8InfoRef findConstant(String text) {
            byte[] utf8InfoBytes = getUtf8InfoBytes(text);
            assert utf8InfoBytes[0] == 1;
            int off = 10;
            for (int i = 1; i < constant_pool_count
                && off < templateBytes.length; ++i) {
                if (matchAtIndex(utf8InfoBytes, off)) {
                    return new Utf8InfoRef(off, utf8InfoBytes.length);
                }
                switch (templateBytes[off]) {
                case 1:// CONSTANT_Utf8
                {
                    int len = (templateBytes[off + 1] & 0xff << 8)
                        + (templateBytes[off + 2] & 0xff);
                    off += 3 + len;
                    break;
                }
                case 7:// CONSTANT_Class
                case 8:// CONSTANT_String
                    off += 3;
                    break;
                case 3:// CONSTANT_Integer
                case 4:// CONSTANT_Float
                case 9:// CONSTANT_Fieldref
                case 10:// CONSTANT_Methodref
                case 11:// CONSTANT_InterfaceMethodref
                case 12:// CONSTANT_NameAndType
                    off += 5;
                    break;
                case 5:// CONSTANT_Long
                case 6:// CONSTANT_Double
                    off += 9;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Unrecognized class file constant pool tag "
                            + templateBytes[off]);
                }
            }
            return null;
        }

        /**
         * Obtain the bytes that define the given class by looking for the
         * ".class" resource and loading the binary data.
         * 
         * @throws IOException if an error occurs loading the binary data
         */
        private void loadTemplateBytes() throws IOException {
            String resourceName = '/'
                + templateClass.getName().replace('.', '/') + ".class";
            InputStream in = ConverterPropertyEditorFactory.class
                .getResourceAsStream(resourceName);
            if (in != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buff = new byte[1024];
                    int more;
                    while ((more = in.read(buff)) > 0) {
                        baos.write(buff, 0, more);
                    }
                    templateBytes = baos.toByteArray();
                    // The bytes should start with the CAFEBABE "magic" header
                    // for class files.
                    assert templateBytes.length > 9;
                    assert templateBytes[0] == (byte) 0xCA;
                    assert templateBytes[1] == (byte) 0xFE;
                    assert templateBytes[2] == (byte) 0xBA;
                    assert templateBytes[3] == (byte) 0xBE;
                    constant_pool_count = ((templateBytes[8] & 0xff) << 8)
                        + (templateBytes[9] & 0xff);
                } finally {
                    in.close();
                }
            }
        }

        /**
         * Generate a class name to use for the generated PropertyEditor class,
         * based on the full name of the target class. This is done by replacing
         * the "XXXX" in the template class name with a version of the target
         * class name.
         * 
         * @param targetClass
         *            The target class which the PropertyEditor will operate on.
         * @param vmFormat
         *            If true, the package name components will be '/'
         *            separated. Otherwise they will be '.' separated.
         * @return The full name to use for the generated PropertyEditor class.
         */
        public String generateClassNameFor(Class<?> targetClass,
            boolean vmFormat) {
            String name = targetClass.getName();
            if (targetClass.isArray()) {                            
                int idx = name.lastIndexOf('[');
                int bracketCount = idx + 1;
                int semiIdx = name.indexOf(';');
                if (semiIdx == -1) {
                    // primitive array
                    name = PRIM_MAP.get(name.charAt(idx + 1));
                } else {
                    // Object array
                    name = name.substring(idx + 2, semiIdx);
                }
                name += "Array" + bracketCount + 'd';                
            }
            Matcher m = UnderscorePattern.matcher(name);
            // Replace existing underscores with one extra underscore.
            name = m.replaceAll("$0_");
            // Replace existing dots with a single underscore.
            name = name.replace('.', '_');
            if (vmFormat) {
                return getVMClassName(templateClass).replace("XXXX", name);
            } else {
                return templateClass.getName().replace("XXXX", name);
            }
        }

        /**
         * Extract the original target class name from the generated
         * PropertyEditor class name. (This is the reverse of
         * {@link #generateClassNameFor}).
         * 
         * @param className
         *            name of the generated PropertyEditor class.
         * @return the target class name, or null if the given
         *         <code>className</code> was not a generated PropertyEditor
         *         name.
         */
        public String getTargetClassName(String className) {
            String prefix = templateClass.getName().replace("XXXX", "");
            if (className.startsWith(prefix)) {
                String name = className.substring(prefix.length());
                name = SingleUnderscorePattern.matcher(name)
                    .replaceAll("$1.$2");
                name = MultipleUnderscorePattern.matcher(name).replaceAll("$1");
                return name;
            }
            return null;
        }

        /**
         * Generate the bytes for a new class based on the
         * <code>templateBytes</code>, but with all the replacements in
         * <code>replacements</code> performed.
         * 
         * @param replacements one or more Utf8InfoReplacments
         * @return the bytes for the new class definition.
         */
        private byte[] replaceInTemplate(Utf8InfoReplacement... replacements) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Sort the replacements, and weed out any that have no source match
            TreeSet<Utf8InfoReplacement> sorted = new TreeSet<Utf8InfoReplacement>();
            for (Utf8InfoReplacement r : replacements) {
                if (r.ref != null && r.replacement != null) {
                    sorted.add(r);
                }
            }
            // Now create the output bytes by applying the remaining
            // replacements
            int from = 0;
            for (Utf8InfoReplacement r : sorted) {
                baos.write(templateBytes, from, r.ref.index - from);
                from = r.ref.index + r.ref.length;
                baos.write(r.replacement, 0, r.replacement.length);
            }
            baos.write(templateBytes, from, templateBytes.length - from);
            return baos.toByteArray();
        }

        /**
         * @return the bytes for a new class with the given name and target
         * class.
         * 
         * @param newClassName
         *            the binary name of the new class.
         * @param targetClassName
         *            the binary name of the PropertyEditor's target class.
         */
        public byte[] generateClassBytesFor(String newClassName,
            String targetClassName) {
            return replaceInTemplate(new Utf8InfoReplacement(classNameConstant,
                newClassName), new Utf8InfoReplacement(classNameRefConstant,
                 new StringBuilder(32).append('L').append(newClassName).append(';').toString()), new Utf8InfoReplacement(
                targetClassConstant, targetClassName));
        }
    }

    /**
     * <p>
     * A custom class loader for the definition of the generated classes. When
     * the generated class is loaded, it will need to be able to resolve both
     * the base class ({@link ConverterPropertyEditorBase}) which comes from
     * <code>myLoader</code> and the target class which comes from
     * <code>targetLoader</code>. This class loader defines only the
     * generated class, and delegates to the above two loaders for the rest.
     * </p>
     * <p>
     * The {@link ConverterPropertyEditorFactory} will keep a cache of these
     * class loaders (via weak references), one for each class loader that the
     * target classes come from. That way the target class loader (which is
     * likely to be a webapp specific loader) can be disposed of and replaced
     * when the webapp is removed or reinstalled.
     * </p>
     */
    private class DisposableClassLoader extends ClassLoader {
        // The class loader which loaded the target class.
        private ClassLoader targetLoader;
        // The class loader which loaded the base class
        private ClassLoader myLoader;

        public DisposableClassLoader(ClassLoader targetLoader) {
            super(targetLoader);
            this.targetLoader = targetLoader;
            this.myLoader = ConverterPropertyEditorBase.class.getClassLoader();
        }

        /**
         * Override class loading to enable possible delegation to the two class
         * loaders, rather than just to the parent.
         */
        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
            // First, check if the class has already been loaded
            Class c = findLoadedClass(name);
            // Otherwise check if myLoader is able to load it ...
            //noinspection ObjectEquality
            if ((c == null) && (myLoader != null) && (myLoader != targetLoader)) {
                try {
                    c = myLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Ignoring ClassNotFoundException, continuing with parent ClassLoader.", ignored);
                    }
                }
            }
            // Otherwise go ahead with the targetLoader and with the dynamic
            // class generation ...
            if (c == null) {
                c = super.loadClass(name, false);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        /**
         * If <code>super.loadClass</code> is unable to locate a class, it
         * will call this method to define it. If the <code>className</code>
         * is a generated PropertyEditor class name, then create the new class.
         * Otherwise call <code>super.findClass</code> which will throw a
         * {@link ClassNotFoundException}.
         */
        @Override
        protected Class<?> findClass(String className)
            throws ClassNotFoundException {
            String targetClassName = getTemplateInfo().getTargetClassName(
                className);
            if (targetClassName != null) {
                // Need to generate an appropriate PropertyEditor class for the
                // specified target class.
                byte[] classBytes = getTemplateInfo().generateClassBytesFor(
                    className.replace('.', '/'),
                    targetClassName.replace('.', '/'));
                Class editorClass = defineClass(className,
                                                classBytes,
                                                0,
                                                classBytes.length);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Defined editorClass " + editorClass);
                }
                return editorClass;
            }
            // This will just cause ClassNotFoundException to be thrown.
            return super.findClass(className);
        }
    }

    private static final Pattern UnderscorePattern = Pattern.compile("_+");
    private static final Pattern SingleUnderscorePattern = Pattern
        .compile("([^_])_([^_])");
    private static final Pattern MultipleUnderscorePattern = Pattern
        .compile("_(_+)");
    private static ConverterPropertyEditorFactory defaultInstance;
    // Template information extracted from the source template class.
    private ClassTemplateInfo templateInfo;
    // Cache of DisposableClassLoaders keyed on the class loader of the target.
    private Map<ClassLoader, WeakReference<DisposableClassLoader>> classLoaderCache;
    
    private static final Map<Character,String> PRIM_MAP =
          new HashMap<Character,String>(8, 1.0f);
    static {
        PRIM_MAP.put('B', "byte");
        PRIM_MAP.put('C', "char");
        PRIM_MAP.put('S', "short");
        PRIM_MAP.put('I', "int");
        PRIM_MAP.put('F', "float");
        PRIM_MAP.put('J', "long");
        PRIM_MAP.put('D', "double");
        PRIM_MAP.put('Z', "boolean");
    }

    /**
     * Create a <code>ConverterPropertyEditorFactory</code> that uses the
     * default template class ({@link ConverterPropertyEditorFor_XXXX}).
     */
    public ConverterPropertyEditorFactory() {
        // Use the default template class
        templateInfo = new ClassTemplateInfo();
    }

    /**
     * Create a <code>ConverterPropertyEditorFactory</code> that uses the
     * specified template class.
     * 
     * @param templateClass the template
     */
    public ConverterPropertyEditorFactory(
        Class<? extends ConverterPropertyEditorBase> templateClass) {
        templateInfo = new ClassTemplateInfo(templateClass);
    }

    /**
     * @return the single default instance of this class (created with the
     * default template class).
     */
    public static synchronized ConverterPropertyEditorFactory getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new ConverterPropertyEditorFactory();
        }
        return defaultInstance;
    }

    private ClassTemplateInfo getTemplateInfo() {
        return templateInfo;
    }

    /**
     * Return a PropertyEditor class appropriate for editing the given
     * <code>targetClass</code>. The new class will be defined from a
     * DisposableClassLoader.
     * 
     * @param targetClass
     *            the class of object that the returned property editor class
     *            will be editing.
     * @return the dynamically generated PropertyEditor class.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends ConverterPropertyEditorBase> definePropertyEditorClassFor(
        final Class<?> targetClass) {
        try {
            String className = getTemplateInfo().generateClassNameFor(
                targetClass, false);
            if (classLoaderCache == null) {
                // Use a WeakHashMap so as not to prevent the class loaders from
                // being garbage collected.
                //noinspection CollectionWithoutInitialCapacity
                classLoaderCache = new WeakHashMap<ClassLoader, WeakReference<DisposableClassLoader>>();
            }
            DisposableClassLoader loader;
            WeakReference<DisposableClassLoader> loaderRef = classLoaderCache
                .get(targetClass.getClassLoader());
            if (loaderRef == null || (loader = loaderRef.get()) == null) {
                loader = (DisposableClassLoader) AccessController.doPrivileged(
                      new PrivilegedAction() {
                          public Object run() {
                            return new DisposableClassLoader(targetClass.getClassLoader());    
                          }
                      });
                if (loader == null) {
                    return null;
                }
                classLoaderCache.put(targetClass.getClassLoader(),
                    new WeakReference(loader));
            }
            return (Class<? extends ConverterPropertyEditorBase>) loader
                .loadClass(className);
        } catch (ClassNotFoundException e) {
        	if (LOGGER.isLoggable(Level.WARNING)) {
	            LOGGER.log(Level.WARNING,
	                "definePropertyEditorClassFor: ClassNotFoundException: "
	                    + e.getMessage(), e);
        	}
        }
        return null;
    }

    /**
     * @param c
     *            the class to find the name of.
     * @return the binary name of the class as used by the VM ('/' instead of
     *         '.' as a package name separator).
     */
    private static String getVMClassName(Class<?> c) {
        return c.getName().replace('.', '/');
    }

    /**
     * Create a UTF8Info constant pool structure for the given text.
     * 
     * @param text
     *            the text to create the UTF8 constant from.
     * @return the bytes for the UTF8Info constant pool entry, including the
     *         tag, length, and utf8 content.
     */
    private static byte[] getUtf8InfoBytes(String text) {
        byte[] utf8;
        try {
            utf8 = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // The DM_DEFAULT_ENCODING warning is acceptable here
            // because we explicitly *want* to use the Java runtime's
            // default encoding.
            utf8 = text.getBytes();
        }
        byte[] info = new byte[utf8.length + 3];
        info[0] = 1;
        info[1] = (byte) ((utf8.length >> 8) & 0xff);
        info[2] = (byte) (utf8.length & 0xff);
        System.arraycopy(utf8, 0, info, 3, utf8.length);
        return info;
    }
}
