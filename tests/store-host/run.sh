#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
: "${SQLITE_JDBC_JAR:?Set SQLITE_JDBC_JAR to a local SQLite JDBC jar}"
SCRATCH=${TMPDIR:-${XDG_CACHE_HOME:-$HOME/.cache}}/yongyaoji-store-host
mkdir -p "$SCRATCH/classes"
cd "$ROOT"
javac -cp "$SQLITE_JDBC_JAR" -d "$SCRATCH/classes" \
  tests/store-host/android/content/*.java tests/store-host/android/database/*.java \
  tests/store-host/android/database/sqlite/*.java \
  src/main/java/cn/medreminder/app/Core.java src/main/java/cn/medreminder/app/Store.java src/main/java/cn/medreminder/app/DoseStats.java \
  tests/CoreTest.java tests/DoseStatsTest.java tests/store-host/StoreHostTest.java
java -cp "$SCRATCH/classes:$SQLITE_JDBC_JAR" cn.medreminder.app.CoreTest
java -cp "$SCRATCH/classes:$SQLITE_JDBC_JAR" cn.medreminder.app.DoseStatsTest
java -Djava.io.tmpdir="$SCRATCH" -cp "$SCRATCH/classes:$SQLITE_JDBC_JAR" cn.medreminder.app.StoreHostTest
