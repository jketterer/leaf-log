#!/bin/sh
set -e

root_dir=$CI_WORKSPACE_PATH
repo_dir=$CI_PRIMARY_REPOSITORY_PATH
jdk_dir="${CI_DERIVED_DATA_PATH}/JDK"
gradle_dir="${repo_dir}"
cache_dir="${CI_DERIVED_DATA_PATH}/.gradle"

# Check if we stored gradle caches in DerivedData.
recover_cache_files() {
    echo "\nRecover cache files"
    if [ ! -d "$cache_dir" ]; then
        echo " - No valid caches found, skipping"
        return 0
    fi
    echo " - Copying gradle cache to ${gradle_dir}"
    rm -rf "${gradle_dir}/.gradle"
    cp -r "$cache_dir" "$gradle_dir"
    return 0
}

# Install JDK 17 via Homebrew and link it to the path the Xcode build phase expects.
# The "Compile Kotlin Framework" build phase exports JAVA_HOME=${CI_DERIVED_DATA_PATH}/JDK
# when that directory exists, so we symlink the Homebrew-installed JDK there.
install_jdk_if_needed() {
    echo "\nInstall JDK if needed"

    if [ -d "$jdk_dir" ]; then
        echo " - Found a valid JDK installation, skipping install"
        return 0
    fi

    echo " - No valid JDK installation found, installing via Homebrew formula..."
    # Use the formula (not --cask) — cask installs to /usr/local/Caskroom which requires
    # sudo and fails in non-interactive CI. The formula installs to the Homebrew prefix
    # without elevated permissions.
    brew install openjdk@17

    echo " - Symlinking JDK to ${jdk_dir}"
    mkdir -p "$(dirname "$jdk_dir")"
    ln -sf "$(brew --prefix openjdk@17)" "$jdk_dir"

    echo " - JAVA_HOME will be set to ${jdk_dir}"
    return 0
}

recover_cache_files
install_jdk_if_needed
