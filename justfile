default:
    @just --choose

[doc("Builds the Android app.")]
build_android:
    cd TalkingBookApp && ./gradlew assembleRelease

[doc("Builds the Android app and creates a release on GitHub.")]
release_android: build_android
    #!/usr/bin/env bash
    set -euxo pipefail

    cd ./TalkingBookApp

    # Reads the value of a property from a properties file.
    # $1 - Key name, matched at beginning of line.
    function prop() {
        grep "^${1}" version.properties | cut -d'=' -f2
    }

    version="$(prop 'versionNumber')"
    versionCode="$(prop 'versionCode')"
    tagName="v/android/${version}_${versionCode}"
    apkFileName="android-talking-book-loader-${version}_${versionCode}.apk"

    # Rename the generated apk file
    mv app/build/outputs/apk/release/app-release.apk app/build/outputs/apk/release/"${apkFileName}"

    # Stage & commit the version properties file, and tag the release
    git add version.properties
    git commit --message "Bump version to $versionCode"
    git tag --annotate "$tagName" --message "v/android/${version}_${versionCode}"
    git push --tags

    # Create a release on GitHub
    gh release create $tagName --title "v/android/${version}_${versionCode}" --notes-from-tag --latest "app/build/outputs/apk/release/${apkFileName}"

[doc("Compiles ACM, TB Loader, CSM Compiler and CloudSync")]
build_acm:
    # Build csm compiler. Assumes 'CSMcompile' repo is cloned and in the parent folder
    cd ../CSMcompile && ./gradlew clean && ./gradlew jar

    # Build CloudSync & ctrl. Assumes 'S3Sync' repo is cloned and in the parent folder
    cd ../S3Sync/ctrl && ./gradlew clean && ./gradlew dist

    # Build acm
    cd acm && ./gradlew clean && ./gradlew dist

[doc("Creates ACM/TB Loader installer")]
build_acm_installer: build_acm
    cd installer && sh ./build_dist.sh && sh ./build_inst.sh

[doc("Uploads new ACM installer to S3 buckets")]
release_acm: build_acm_installer
    cd installer && sh ./build_s3.sh
