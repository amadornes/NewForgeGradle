package net.minecraftforge.gradle.api.mapping;

/**
 * Represents a generic mapping entry. Do not use directly.
 *
 * @see Package
 * @see Class
 * @see Field
 * @see Method
 */
public class MappingEntry {

    public static Package forPackage(String name) {
        return new Package(name);
    }

    public static Class forClass(String name) {
        return new Class(name);
    }

    public static Field forField(String owner, String name) {
        return new Field(owner, name);
    }

    public static Field forFullyQualifiedField(String fqname) {
        int index = fqname.lastIndexOf('/');
        String owner = fqname.substring(0, index);
        String name = fqname.substring(index + 1);
        return forField(owner, name);
    }

    public static Method forMethod(String owner, String name, String descriptor) {
        return new Method(owner, name, descriptor);
    }

    public static Method forFullyQualifiedMethod(String fqname, String descriptor) {
        int index = fqname.lastIndexOf('/');
        String owner = fqname.substring(0, index);
        String name = fqname.substring(index + 1);
        return forMethod(owner, name, descriptor);
    }

    private final String name;

    private MappingEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Represents a package mapping entry.
     *
     * @see MappingEntry
     */
    public static final class Package extends MappingEntry {
        private Package(String name) {
            super(name);
        }
    }

    /**
     * Represents a class mapping entry.
     *
     * @see MappingEntry
     */
    public static final class Class extends MappingEntry {
        private Class(String name) {
            super(name);
        }
    }

    /**
     * Represents a field mapping entry.
     *
     * @see MappingEntry
     */
    public static final class Field extends MappingEntry {

        private final String owner;

        private Field(String owner, String name) {
            super(name);
            this.owner = owner;
        }

        public String getOwner() {
            return owner;
        }

    }

    /**
     * Represents a method mapping entry.
     *
     * @see MappingEntry
     */
    public static final class Method extends MappingEntry {

        private final String owner, desc;

        private Method(String owner, String name, String desc) {
            super(name);
            this.owner = owner;
            this.desc = desc;
        }

        public String getOwner() {
            return owner;
        }

        public String getDescriptor() {
            return desc;
        }

    }

}
