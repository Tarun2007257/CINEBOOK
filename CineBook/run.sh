#!/bin/bash
# Compiles and runs CineBook. Usage:  ./run.sh
cd "$(dirname "$0")"

JAR="lib/sqlite-jdbc-3.36.0.3.jar"
URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar"

if ! command -v javac >/dev/null 2>&1; then
  echo "Java JDK not found. Install it first (see README.md, Step 1)."
  exit 1
fi

mkdir -p lib out
if [ ! -f "$JAR" ]; then
  echo "Downloading SQLite JDBC driver (one time only)..."
  curl -L -o "$JAR" "$URL" || { echo "Download failed. Check your internet."; exit 1; }
fi

javac -cp "$JAR" -d out src/*.java || exit 1
java -cp "out:$JAR" CineApp
