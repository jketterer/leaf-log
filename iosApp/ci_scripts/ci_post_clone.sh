#!/bin/sh
root_dir=$CI_WORKSPACE_PATH
repo_dir=$CI_PRIMARY_REPOSITORY_PATH
jdk_dir="${CI_DERIVED_DATA_PATH}/JDK"
gradle_dir="${repo_dir}"
cache_dir="${CI_DERIVED_DATA_PATH}/.gradle"
jdk_version="17.0.13"
jdk_build="11"
# Check if we stored gradle caches in DerivedData.
recover_cache_files() {

    echo "\nRecover cache files"
if [ ! -d $cache_dir ]; then
        echo " - No valid caches found, skipping"
        return 0
    fi
echo " - Copying gradle cache to ${gradle_dir}"
    rm -rf "${gradle_dir}/.gradle"
    cp -r $cache_dir $gradle_dir
return 0
}
# Install the JDK
install_jdk_if_needed() {
echo "\nInstall JDK if needed"
if [[ $(uname -m) == "arm64" ]]; then
        echo " - Detected M1"
        arch_type="aarch64"
    else
        echo " - Detected Intel"
        arch_type="x64"
    fi
# Location of version / arch detection file.
    detect_loc="${jdk_dir}/.${jdk_version}.${arch_type}"
if [ -f $detect_loc ]; then
        echo " - Found a valid JDK installation, skipping install"
        return 0
    fi
echo " - No valid JDK installation found, installing..."
tar_name="OpenJDK17U-jdk_${arch_type}_mac_hotspot_${jdk_version}_${jdk_build}.tar.gz"
# Download and un-tar JDK to our defined location.
    curl -OL "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-${jdk_version}%2B${jdk_build}/${tar_name}"
    tar xzf $tar_name -C $root_dir
# Move the JDK to our desired location.
    mkdir -p "$(dirname $jdk_dir)"
    rm -rf $jdk_dir
    mv "${root_dir}/jdk-${jdk_version}+${jdk_build}" $jdk_dir
# Some cleanup.
    rm $tar_name
# Add the detection file for subsequent builds.
    touch $detect_loc
echo " - Set JAVA_HOME to ${jdk_dir}"
return 0
}
recover_cache_files
install_jdk_if_needed