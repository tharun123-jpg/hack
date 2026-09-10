#!/bin/bash
# Wraith client build pipeline (memory-light, no decompilation)
set -e
cd /home/user/wraith
export JAVA_HOME=/home/user/jdk/jdk-21.0.12.1+1
JAVAC="$JAVA_HOME/bin/javac"
JAR="$JAVA_HOME/bin/jar"
JAVA="$JAVA_HOME/bin/java"
TC=toolchain
TR_CP="$TC/deps/tiny-remapper-0.14.0.jar:$TC/deps/asm-9.9.1.jar:$TC/deps/asm-commons-9.9.1.jar:$TC/deps/asm-tree-9.9.1.jar:$TC/deps/asm-util-9.9.1.jar:$TC/deps/mapping-io-0.7.1.jar"

MC_LIBS=$(ls $TC/mc/libs/*.jar | tr '\n' ':')
FAB_MODULES=$(ls $TC/remapped/fabric-modules/*.jar | tr '\n' ':')
CP="$TC/remapped/minecraft-yarn.jar:$TC/remapped/fabric-api-yarn.jar:$TC/remapped/fabric-loader-yarn.jar:$TC/deps/mixin-0.8.5.jar:$MC_LIBS$FAB_MODULES"

rm -rf build/classes build/final build/tool
mkdir -p build/classes build/final build/tool

echo "=== [1/5] compile ==="
find src/main/java -name "*.java" > build/sources.txt
$JAVAC -proc:none -cp "$CP" -d build/classes --release 21 -encoding UTF-8 @build/sources.txt
echo "compile OK ($(find build/classes -name '*.class' | wc -l) classes)"

echo "=== [2/5] jar classes ==="
(cd build/classes && $JAR cf ../../build/classes.jar .)

echo "=== [3/5] remap yarn -> intermediary ==="
$JAVA -Xmx1g -cp "$TR_CP" net.fabricmc.tinyremapper.Main \
    build/classes.jar build/mod-classes.jar \
    $TC/mappings/extracted/yarn/mappings/mappings.tiny named intermediary \
    $TC/remapped/minecraft-yarn.jar > /dev/null

echo "=== [4/5] refmap ==="
$JAVAC -cp "$TC/deps/asm-9.9.1.jar:$TC/deps/asm-tree-9.9.1.jar:$TC/deps/mapping-io-0.7.1.jar" -d build/tool $TC/RefmapGen.java
$JAVA -Xmx512m -cp "build/tool:$TC/deps/asm-9.9.1.jar:$TC/deps/asm-commons-9.9.1.jar:$TC/deps/asm-tree-9.9.1.jar:$TC/deps/asm-util-9.9.1.jar:$TC/deps/mapping-io-0.7.1.jar" \
    RefmapGen build/classes $TC/mappings/extracted/yarn/mappings/mappings.tiny build/wraith.refmap.json

echo "=== [5/5] package ==="
cd build/final
$JAR xf ../mod-classes.jar
cp ../../src/main/resources/fabric.mod.json .
cp ../../src/main/resources/wraith.mixins.json .
cp ../wraith.refmap.json .
(cd /home/user/wraith && $JAR cf build/wraith-client.jar -C build/final .)
cd /home/user/wraith
ls -la build/wraith-client.jar
echo "BUILD DONE"
