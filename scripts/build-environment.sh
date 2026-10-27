#!/usr/bin/env bash
# Shared by canonical build and targeted tests. Source after ROOT is set.
v8cs_build_environment() {
  local candidate
  if [[ -z "${JAVA_HOME_ARG:-}" ]]; then
    for candidate in "${JAVA_HOME:-}" "$HOME/tools/jdk-25.0.4.1+1" /opt/1C/1CE/components/axiom-jdk-full-25.0.4+5-x86_64; do
      [[ -x "$candidate/bin/javac" ]] || continue
      if "$candidate/bin/java" -version 2>&1 | grep -q 'version "25'; then
        JAVA_HOME_ARG="$candidate"
        break
      fi
    done
  fi
  [[ -x "${JAVA_HOME_ARG:-}/bin/javac" ]] || { echo 'Full JDK 25 required (JAVA_HOME or --java-home)' >&2; return 1; }
  "$JAVA_HOME_ARG/bin/java" -version 2>&1 | grep -q 'version "25' || { echo 'JDK must be Java 25' >&2; return 1; }
  export JAVA_HOME="$JAVA_HOME_ARG"
  export PATH="$JAVA_HOME/bin:$PATH"
  if [[ -n "${MAVEN_HOME_ARG:-}" ]]; then
    MVN="$MAVEN_HOME_ARG/bin/mvn"
  elif [[ -z "${MVN:-}" ]]; then
    if [[ -x "$HOME/tools/apache-maven-3.9.9/bin/mvn" ]]; then
      MVN="$HOME/tools/apache-maven-3.9.9/bin/mvn"
    else
      MVN="$(command -v mvn)"
    fi
  fi
  [[ -x "$MVN" ]] || { echo 'Maven 3.9.9 required' >&2; return 1; }
  "$MVN" --version 2>&1 | grep -q 'Apache Maven 3.9.9' || { echo 'Use pinned Maven 3.9.9 (Tycho/Resolver compatibility)' >&2; return 1; }
  EDT_P2_REPOSITORY="$(python3 "$ROOT/scripts/prepare-platform.py")" || return 1
  export EDT_P2_REPOSITORY
}
