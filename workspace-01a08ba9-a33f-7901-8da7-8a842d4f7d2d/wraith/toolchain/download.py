#!/usr/bin/env python3
"""Download Minecraft 1.21.11 client + libraries + fabric deps + mappings + remapper toolchain."""
import json, os, sys, urllib.request, concurrent.futures, shutil

BASE = "/home/user/wraith/toolchain"
MC_VERSION = "1.21.11"
dirs = ["mc", "mc/libs", "deps", "remapped", "mappings"]
for d in dirs:
    os.makedirs(f"{BASE}/{d}", exist_ok=True)

def get(url, dest, timeout=120):
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        return f"CACHED {dest}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=timeout) as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)
    return f"OK {os.path.basename(dest)} ({os.path.getsize(dest)//1024}KB)"

def urlopen_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)

# 1) Minecraft version json
print("== Minecraft", MC_VERSION)
vjson = urlopen_json(f"https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
entry = next(v for v in vjson["versions"] if v["id"] == MC_VERSION)
vj = urlopen_json(entry["url"])
with open(f"{BASE}/mc/version.json", "w") as f:
    json.dump(vj, f)

tasks = []
cj = vj["downloads"]["client"]["url"]
tasks.append((cj, f"{BASE}/mc/minecraft-{MC_VERSION}-client.jar"))

for lib in vj.get("libraries", []):
    dl = lib.get("downloads", {})
    art = dl.get("artifact")
    if art and "url" in art:
        name = art["path"].split("/")[-1]
        tasks.append((art["url"], f"{BASE}/mc/libs/{name}"))

print(f"Downloading {len(tasks)} minecraft files...")
def do(t):
    try:
        return get(t[0], t[1])
    except Exception as e:
        return f"FAIL {t[1]}: {e}"
with concurrent.futures.ThreadPoolExecutor(8) as ex:
    for r in ex.map(do, tasks):
        print(" ", r)

# 2) Fabric deps
print("== Fabric")
fab_tasks = [
    ("https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar", f"{BASE}/deps/fabric-loader-0.19.5.jar"),
    ("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.141.6%2B1.21.11/fabric-api-0.141.6%2B1.21.11.jar", f"{BASE}/deps/fabric-api-0.141.6+1.21.11.jar"),
    ("https://maven.fabricmc.net/net/fabricmc/yarn/1.21.11%2Bbuild.6/yarn-1.21.11%2Bbuild.6-v2.jar", f"{BASE}/mappings/yarn-1.21.11+build.6-v2.jar"),
    ("https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.11/intermediary-1.21.11-v2.jar", f"{BASE}/mappings/intermediary-1.21.11-v2.jar"),
    ("https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.14.0/tiny-remapper-0.14.0.jar", f"{BASE}/deps/tiny-remapper-0.14.0.jar"),
    ("https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.14.0/tiny-remapper-0.14.0.pom", f"{BASE}/deps/tiny-remapper.pom"),
]
for u, d in fab_tasks:
    print(" ", get(u, d))

# 3) Mixin API (compile-time)
print("== Mixin")
mx_tasks = [
    ("https://repo1.maven.org/maven2/org/spongepowered/mixin/0.8.5/mixin-0.8.5.jar", f"{BASE}/deps/mixin-0.8.5.jar"),
    ("https://repo1.maven.org/maven2/com/google/guava/guava/33.4.0-jre/guava-33.4.0-jre.jar", f"{BASE}/deps/guava-33.4.0-jre.jar"),
]
for u, d in mx_tasks:
    print(" ", get(u, d))

print("DONE")
