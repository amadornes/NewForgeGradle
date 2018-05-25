package net.minecraftforge.gradle.api;

/**
 * Represents a generic mapping entry. Do not use directly.
 *
 * @see Package
 * @see Class
 * @see Field
 * @see Method
 */
public class MappingEntry {

    protected String name;

    private MappingEntry(String name) {
        this.name = name;
    }

    private MappingEntry() {
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
        public Package(String name) {
            super(name);
        }
    }

    /**
     * Represents a class mapping entry.
     *
     * @see MappingEntry
     */
    public static final class Class extends MappingEntry {
        public Class(String name) {
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

        public Field(String owner, String name) {
            super(name);
            this.owner = owner;
        }

        public Field(String name) {
            String[] split = name.split("\\/");
            this.name = split[split.length - 1];
            this.owner = name.substring(0, name.length() - this.name.length() - 1);
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

        public Method(String owner, String name, String desc) {
            super(name);
            this.owner = owner;
            this.desc = desc;
        }

        public Method(String name, String desc) {
            String[] split = name.split("\\/");
            this.name = split[split.length - 1];
            this.owner = name.substring(0, name.length() - this.name.length() - 1);
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
