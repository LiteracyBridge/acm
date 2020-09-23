import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Yes, this is not a best practice, putting a class right up at the root.
 *
 * Helper to more easily invoke utilities from the command line.
 */
public class TB {

    private enum Util {
        acm(org.literacybridge.acm.gui.Application.class),
        tbloader(org.literacybridge.acm.tbloader.TBLoader.class, "loader", "tbl"),
        tbbuilder(org.literacybridge.acm.tbbuilder.TBBuilder.class, "builder"),
        acmcleaner(org.literacybridge.acm.tools.AcmCleaner.class, "clean", "cleaner");

        private Class clazz;
        private Set<String> aliases = new HashSet<>();
        Util(Class clazz, String... aliases) {
            this.clazz = clazz;
            this.aliases.add(this.toString());
            Collections.addAll(this.aliases, aliases);
        }

        /**
         * Is the given name our name, or an alias?
         * @param name to search
         * @return true if it matches this Util
         */
        private boolean matches(String name) {
            return aliases.contains(name);
        }

        /**
         * Invoke 'main' on the corresponding class, passing args.
         * @param args for main()
         * @throws Exception if any.
         */
        private void invoke(String[] args) throws Exception {
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (m.getName().equals("main")) {
                    m.invoke(null, new Object[] {args});
                    break;
                }
            }
        }

        /**
         * Find the Util that matches the name.
         * @param name to be found
         * @return matching Util, or null
         */
        static Util lookup(String name) {
            for (Util u: Util.values()) {
                if (u.matches(name))
                    return u;
            }
            return null;
        }

        static void print() {
            for (Util u: Util.values()) {
                System.out.printf("%s: %s\n", u.clazz.toString(), u.aliases.toString());
            }
        }
    }

    public static void run(String utilName, String[] args) throws Exception {
        // Known Util?
        Util util = Util.lookup(utilName);
        if (util != null) {
            util.invoke(args);
        } else {
            System.err.printf("Unknown Util: %s\n", utilName);
            Util.print();
        }
    }

    public static void main(String[] args) throws Exception {
        // First argument is the utility name.
        String utilName = args[0].toLowerCase();
        // Remainder are passed through.
        args = Arrays.copyOfRange(args, 1, args.length);
        
        run(utilName, args);
    }
}
