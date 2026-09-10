import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Generates a mixin refmap mapping the @Inject target method names from
 * Yarn (named) to intermediary, for use by the Fabric loader at runtime.
 *
 * Usage: RefmapGen <classesDir> <yarnTiny> <outRefmapJson>
 */
public class RefmapGen {

    public static void main(String[] args) throws Exception {
        Path classesDir = Path.of(args[0]);
        String yarnTiny = args[1];
        Path out = Path.of(args[2]);

        // Load yarn mapping (src = intermediary, dst = named/yarn)
        MemoryMappingTree tree = new MemoryMappingTree();
        try (var reader = new InputStreamReader(new FileInputStream(yarnTiny))) {
            Tiny2FileReader.read(reader, tree);
        }
        int yarnId = tree.getNamespaceId("named");
        if (yarnId < 0) {
            throw new RuntimeException("namespace 'named' not found; namespaces=" + tree.getDstNamespaces());
        }

        Map<String, String> classRev = new HashMap<>();    // yarn class -> intermediary class
        Map<String, String> methodRev = new HashMap<>();   // yarnClass + desc + yarnName -> intermediary name
        Map<String, List<String[]>> nameRev = new HashMap<>(); // yarnClass + name -> [(desc, interName)]
        for (MappingTreeView.ClassMappingView cv : tree.getClasses()) {
            classRev.put(cv.getDstName(yarnId), cv.getSrcName());
            for (MappingTreeView.MethodMappingView mv : cv.getMethods()) {
                methodRev.put(cv.getDstName(yarnId) + "\0" + mv.getDstDesc(yarnId) + "\0" + mv.getDstName(yarnId),
                        mv.getSrcName());
                nameRev.computeIfAbsent(cv.getDstName(yarnId) + "\0" + mv.getDstName(yarnId),
                        k -> new ArrayList<>()).add(new String[]{mv.getDstDesc(yarnId), mv.getSrcName()});
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"mappings\": {\n");

        List<String> classFiles = new ArrayList<>();
        try (Stream<Path> s = Files.walk(classesDir)) {
            s.filter(p -> p.toString().endsWith(".class")).forEach(p -> classFiles.add(classesDir.relativize(p).toString()));
        }
        classFiles.sort(String::compareTo);

        boolean firstClass = true;
        int entries = 0;
        for (String rel : classFiles) {
            String internal = rel.replace('\\', '/');
            if (!internal.contains("mixin/")) continue;

            AtomicReference<String> targetRef = new AtomicReference<>();
            Map<String, String> injects = new LinkedHashMap<>(); // mixin method name -> "yarnMethod(desc)"

            ClassReader cr = new ClassReader(new FileInputStream(classesDir.resolve(rel).toString()));
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    AnnotationVisitor superAv = super.visitAnnotation(descriptor, visible);
                    if (descriptor.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                        return new AnnotationVisitor(Opcodes.ASM9, superAv) {
                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                if (name.equals("value")) {
                                    return new AnnotationVisitor(Opcodes.ASM9, super.visitArray(name)) {
                                        @Override
                                        public void visit(String aname, Object value) {
                                            if (value instanceof Type t) {
                                                targetRef.set(t.getClassName());
                                            } else if (value instanceof String s2) {
                                                targetRef.set(s2.replace('/', '.'));
                                            }
                                            super.visit(aname, value);
                                        }
                                    };
                                }
                                return super.visitArray(name);
                            }

                            @Override
                            public void visit(String name, Object value) {
                                if (name.equals("value")) {
                                    if (value instanceof Type t) targetRef.set(t.getClassName());
                                    else if (value instanceof String s2) targetRef.set(s2.replace('/', '.'));
                                }
                                super.visit(name, value);
                            }
                        };
                    }
                    return superAv;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String sig, String[] exceptions) {
                    final String methodName = name;
                    MethodVisitor superMv = super.visitMethod(access, name, descriptor, sig, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, superMv) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            AnnotationVisitor superAv = super.visitAnnotation(descriptor, visible);
                            if (descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                                    || descriptor.equals("Lorg/spongepowered/asm/mixin/injection/ModifyArg;")
                                    || descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")) {
                                return new AnnotationVisitor(Opcodes.ASM9, superAv) {
                                    @Override
                                    public AnnotationVisitor visitArray(String name) {
                                        if (name.equals("method")) {
                                            return new AnnotationVisitor(Opcodes.ASM9, super.visitArray(name)) {
                                                @Override
                                                public void visit(String aname, Object value) {
                                                    if (value instanceof String ms) {
                                                        injects.put(methodName, ms);
                                                    }
                                                    super.visit(aname, value);
                                                }
                                            };
                                        }
                                        return super.visitArray(name);
                                    }

                                    @Override
                                    public void visit(String aname, Object value) {
                                        if (aname.equals("method") && value instanceof String ms) {
                                            injects.put(methodName, ms);
                                        }
                                        super.visit(aname, value);
                                    }
                                };
                            }
                            return superAv;
                        }
                    };
                }
            }, 0);

            String target = targetRef.get();
            if (target == null || injects.isEmpty()) {
                if (internal.contains("Mixin")) System.err.println("[refmap] NOTE: no targets found in " + internal + " (target=" + target + ")");
                continue;
            }
            target = target.replace('.', '/'); // mapping keys use slashed form

            String className = internal.endsWith(".class") ? internal.substring(0, internal.length() - 6) : internal;
            if (!firstClass) sb.append(",\n");
            firstClass = false;
            sb.append("    \"").append(escape(className)).append("\": {\n");

            boolean firstM = true;
            for (Map.Entry<String, String> e : injects.entrySet()) {
                String ms = e.getValue();
                int p = ms.indexOf('(');
                String yarnName, desc, interName;
                if (p > 0) {
                    yarnName = ms.substring(0, p);
                    desc = ms.substring(p);
                    interName = methodRev.get(target + "\0" + desc + "\0" + yarnName);
                    if (interName == null) {
                        // name unchanged by mappings (identity) — e.g. methods Mojang named directly
                        interName = yarnName;
                    }
                } else {
                    yarnName = ms;
                    desc = null;
                    List<String[]> candidates = nameRev.get(target + "\0" + yarnName);
                    if (candidates == null || candidates.isEmpty()) {
                        System.err.println("[refmap] WARNING: no mapping at all for " + target + "." + yarnName);
                        interName = yarnName;
                    } else if (candidates.size() == 1) {
                        desc = candidates.get(0)[0];
                        interName = candidates.get(0)[1];
                    } else {
                        System.err.println("[refmap] WARNING: ambiguous method " + target + "." + yarnName + " (" + candidates.size() + " overloads); using first");
                        desc = candidates.get(0)[0];
                        interName = candidates.get(0)[1];
                    }
                }
                if (desc == null) desc = "";
                entries++;
                if (!firstM) sb.append(",\n");
                firstM = false;
                sb.append("      \"").append(escape(e.getKey())).append("\": { \"methodName\": \"")
                        .append(escape(interName)).append("\", \"desc\": \"").append(escape(desc)).append("\" }");
            }
            sb.append("\n    }");
        }

        sb.append("\n  }\n}\n");
        Files.writeString(out, sb.toString());
        System.out.println("[refmap] wrote " + out + " (" + entries + " entries)");
        if (entries == 0) throw new RuntimeException("refmap is empty!");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
