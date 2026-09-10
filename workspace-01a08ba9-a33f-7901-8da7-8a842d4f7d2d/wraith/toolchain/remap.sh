#!/bin/bash
# Remap jars: official/obfuscated -> intermediary -> yarn (two passes)
set -e
cd /home/user/wraith/toolchain
JAVA=/home/user/jdk/jdk-21.0.12.1+1/bin/java
TR="deps/tiny-remapper-0.14.0.jar:deps/asm-9.9.1.jar:deps/asm-commons-9.9.1.jar:deps/asm-tree-9.9.1.jar:deps/asm-util-9.9.1.jar:deps/mapping-io-0.7.1.jar"
INTER=mappings/extracted/intermediary/mappings/mappings.tiny
YARN=mappings/extracted/yarn/mappings/mappings.tiny

remap2() {
  local in="$1" out="$2" heap="$3"
  local tmp="${out%.jar}-inter.jar"
  echo ">>> $in -> intermediary -> $out"
  $JAVA -Xmx${heap:-768m} -cp "$TR" net.fabricmc.tinyremapper.Main "$in" "$tmp" "$INTER" official intermediary > /dev/null 2>&1
  $JAVA -Xmx${heap:-768m} -cp "$TR" net.fabricmc.tinyremapper.Main "$tmp" "$out" "$YARN" intermediary named > /dev/null 2>&1
  rm -f "$tmp"
  echo "<<< done: $out ($(du -h "$out" | cut -f1))"
}

case "$1" in
  mc)      remap2 mc/minecraft-1.21.11-client.jar remapped/minecraft-yarn.jar 1g ;;
  api)     remap2 deps/fabric-api-0.141.6+1.21.11.jar remapped/fabric-api-yarn.jar 768m ;;
  loader)  remap2 deps/fabric-loader-0.19.5.jar remapped/fabric-loader-yarn.jar 768m ;;
  *) echo "usage: $0 mc|api|loader" ;;
esac
