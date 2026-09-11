import os, re, sys
import tree_sitter_java as tsjava, tree_sitter as ts

ROOT = sys.argv[1]
LANG = ts.Language(tsjava.language())
PARSER = ts.Parser(LANG)
problems = []

raw = {}
for dirpath, dirnames, filenames in os.walk(ROOT):
    for fn in filenames:
        if fn.endswith('.java'):
            p = os.path.join(dirpath, fn)
            raw[p] = open(p, 'rb').read()

def txt(b, node):
    return b[node.start_byte:node.end_byte].decode('utf-8', 'replace')

trees = {}
for p, b in raw.items():
    t = PARSER.parse(b)
    trees[p] = t
    for n in (x for x in _walk(t.root_node, {'ERROR', 'MISSING'})) if False else []:
        pass

def walk(node, types):
    stack = [node]
    while stack:
        n = stack.pop()
        if n.type in types: yield n
        stack.extend(n.children)

for p, b in raw.items():
    for n in walk(trees[p].root_node, {'ERROR', 'MISSING'}):
        line = b[:n.start_byte].count(b'\n') + 1
        frag = b.splitlines()[min(line - 1, len(b.splitlines()) - 1)].strip().decode()[:90]
        problems.append((p, line, f"syntax: {n.type} near `{frag}`"))

PKG_RE = re.compile(rb'^\s*package\s+([\w.]+)\s*;', re.M)
own = {}
decls = {}
for p, b in raw.items():
    m = PKG_RE.search(b)
    pkg = m.group(1).decode() if m else ''
    for cls in walk(trees[p].root_node, {'class_declaration', 'interface_declaration', 'enum_declaration', 'record_declaration', 'annotation_type_declaration'}):
        nm = cls.child_by_field_name('name')
        if nm is None: continue
        name = txt(b, nm)
        q = f"{pkg}.{name}" if pkg else name
        if name in own and own[name] != q:
            problems.append((p, 0, f"duplicate type name {name}"))
        own[name] = q
        # superclass (own types only) for inherited-member resolution
        sup = None
        sc = cls.child_by_field_name('superclass')
        if sc is not None:
            for c in sc.children:
                if c.type in ('type_identifier', 'generic_type'):
                    sup = txt(b, c if c.type == 'type_identifier' else c.children[0])
        body = cls.child_by_field_name('body') or next((c for c in cls.children if c.type in ('enum_body', 'annotation_type_body', 'class_body')), None)
        ctors, methods, fields = [], {}, {}
        body_children = list(body.children) if body else []
        # enum members sit under an extra enum_body_declarations wrapper
        body_children = [c for bnode in body_children for c in (bnode.children if bnode.type == 'enum_body_declarations' else [bnode])]
        if body:
            for mem in body_children:
                if mem.type == 'constructor_declaration':
                    params = mem.child_by_field_name('parameters')
                    n_args = 0; vararg = False
                    if params:
                        ps = [c for c in params.children if c.type in ('formal_parameter', 'spread_parameter')]
                        n_args = len(ps)
                        vararg = any(c.type == 'spread_parameter' for c in ps)
                    ctors.append(('*',) if vararg else (n_args,))
                elif mem.type in ('method_declaration', 'abstract_method_declaration'):
                    nn = mem.child_by_field_name('name')
                    params = mem.child_by_field_name('parameters')
                    vararg = False; n_args = 0
                    if params:
                        ps = [c for c in params.children if c.type in ('formal_parameter', 'spread_parameter')]
                        n_args = len(ps)
                        vararg = bool(ps) and params.children and any(
                            (c.children and any(g.type == 'marker_annotation' or (g.type == 'dimensions' and '...' in txt(b, g)) for g in c.children)) or '...' in txt(b, c)
                            for c in params.children if c.type in ('formal_parameter', 'spread_parameter'))
                    methods.setdefault(txt(b, nn), set()).add(n_args if not vararg else '*')
                elif mem.type in ('field_declaration', 'constant_declaration'):
                    t = mem.child_by_field_name('type')
                    tname = re.sub(r'<.*', '', txt(b, t)).split('.')[-1] if t is not None else None
                    for d in mem.children:
                        if d.type == 'variable_declarator':
                            nnn = d.child_by_field_name('name')
                            if nnn is not None: fields[txt(b, nnn)] = tname
                elif mem.type == 'record_declaration':
                    pass
        d = decls.setdefault(q, {'ctors': [], 'methods': {}, 'fields': {}, 'super': None})
        d['ctors'].extend(c[0] if isinstance(c, tuple) else c for c in ctors)
        d['super'] = sup
        for k, v in methods.items(): d['methods'].setdefault(k, set()).update(v)
        d['fields'].update(fields)

def methods_of(q, seen=None):
    """Own + inherited (within our package) method name -> set of arities."""
    out = {}
    seen = seen or set()
    d = decls.get(q)
    if not d or q in seen: return out
    seen.add(q)
    out.update({k: set(v) for k, v in d['methods'].items()})
    if d['super'] and d['super'] in own:
        for k, v in methods_of(own[d['super']], seen).items():
            out.setdefault(k, set()).update(v)
    return out

IMP_RE = re.compile(rb'^\s*import\s+(?:static\s+)?(com\.wraith\.[\w.]+)\s*;', re.M)
for p, b in raw.items():
    for m in IMP_RE.finditer(b):
        simple = m.group(1).decode().split('.')[-1]
        if simple != '*' and simple not in own:
            problems.append((p, b[:m.start()].count(b'\n') + 1, f"unresolved own import: {m.group(1).decode()}"))

for p, b in raw.items():
    for n in walk(trees[p].root_node, {'object_creation_expression'}):
        t = n.child_by_field_name('type')
        if t is None or txt(b, t).split('.')[-1] not in own: continue
        name = txt(b, t).split('.')[-1].split('<')[0]
        q = own[name]
        args = n.child_by_field_name('arguments')
        arity = sum(1 for c in args.children if c.type not in (',', '(', ')')) if args is not None else 0
        options = decls[q]['ctors']
        if not options: continue
        if '*' not in options and arity not in options:
            problems.append((p, b[:n.start_byte].count(b'\n') + 1,
                             f"new {name}(...): declared ctors take {sorted(options)}, call passes {arity}"))

for p, b in raw.items():
    m = PKG_RE.search(b)
    pkg = m.group(1).decode() if m else ''
    for cls in walk(trees[p].root_node, {'class_declaration', 'enum_declaration', 'interface_declaration', 'annotation_type_declaration'}):
        nm = cls.child_by_field_name('name')
        if nm is None: continue
        q = f"{pkg}.{txt(b, nm)}"
        if q not in decls: continue
        me = methods_of(q)
        fields = decls[q]['fields']
        for n in walk(cls, {'method_invocation'}):
            obj = n.child_by_field_name('object')
            name_n = n.child_by_field_name('name')
            if name_n is None: continue
            mname = txt(b, name_n)
            args = n.child_by_field_name('arguments')
            arity = sum(1 for c in args.children if c.type not in (',', '(', ')')) if args is not None else 0
            target = None
            if obj is None or txt(b, obj) == 'this':
                target = q
            else:
                only = txt(b, obj)
                if re.fullmatch(r'\w+', only) and only in fields and fields[only] in own:
                    target = own[fields[only]]
            if target is None or target not in decls: continue
            opts = methods_of(target).get(mname)
            if opts is None:
                if mname not in ('equals', 'hashCode', 'toString', 'getClass', 'notify', 'notifyAll', 'wait', 'clone',
                                 'finalize', 'values', 'valueOf'):
                    problems.append((p, b[:n.start_byte].count(b'\n') + 1,
                                     f"{target.split('.')[-1]}.{mname}() is not declared in {target.split('.')[-1]} or its own superclasses"))
            elif '*' not in opts and arity not in opts:
                problems.append((p, b[:n.start_byte].count(b'\n') + 1,
                                 f"{mname}() accepts {sorted(o for o in opts if o != '*')} args, call passes {arity} (in {q.split('.')[-1]})"))

# ---- pass 6: @Override contract against our own supertypes ----
for p, b in raw.items():
    m = PKG_RE.search(b)
    pkg = m.group(1).decode() if m else ''
    for cls in walk(trees[p].root_node, {'class_declaration', 'enum_declaration'}):
        nm = cls.child_by_field_name('name')
        if nm is None: continue
        q = f"{pkg}.{txt(b, nm)}"
        if q not in decls: continue
        sup = decls[q]['super']
        if not sup or sup not in own: continue          # MC supertypes are unverifiable here
        sup_methods = methods_of(own[sup])
        body = cls.child_by_field_name('body') or next((c for c in cls.children if c.type == 'class_body'), None)
        abstract_in_sup = set()
        for mem in (body_children_of(decls, own[sup]) if False else []):
            pass
        for mem in (body.children if body else []):
            if mem.type != 'method_declaration': continue
            mods = mem.children[0] if mem.children and mem.children[0].type == 'modifiers' else None
            has_override = mods is not None and any('override' in txt(b, a) for a in mods.children if a.type == 'annotation')
            mn = mem.child_by_field_name('name')
            if mn is None: continue
            name = txt(b, mn)
            params = mem.child_by_field_name('parameters')
            arity = len([c for c in params.children if c.type in ('formal_parameter', 'spread_parameter')]) if params else 0
            if has_override:
                opts = sup_methods.get(name)
                if opts is not None and '*' not in opts and arity not in opts:
                    problems.append((p, b[:mem.start_byte].count(b'\n') + 1,
                                     f"@Override {name}/{arity} does not match {own[sup].split('.')[-1]}.{name}{sorted(opts)}"))
            else:
                # a method that shadows a supertype hook without @Override usually means a typo'd signature
                opts = sup_methods.get(name)
                if opts is not None and '*' not in opts and arity not in opts:
                    problems.append((p, b[:mem.start_byte].count(b'\n') + 1,
                                     f"{name}/{arity} clashes with {own[sup].split('.')[-1]}.{name}{sorted(opts)} and has no @Override"))

print(f"parsed {len(raw)} files")
seen = set(); out = []
for p, line, msg in sorted(problems, key=lambda x: (x[0], x[1])):
    k = (p, line, msg)
    if k in seen: continue
    seen.add(k); out.append(f"{os.path.relpath(p, ROOT)}:{line}: {msg}")
print("\n".join(out) if out else "NO PROBLEMS FOUND")
