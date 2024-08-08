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

    # Stage & commit the version properties file, and tag the release
    git add version.properties
    git commit --message "Bump version to $versionCode"
    git tag --annotate "$tagName" --message "v/android/${version}_${versionCode}"
    git push --tags

    # Create a release on GitHub
    gh release create $tagName --title "v/android/${version}_${versionCode}" --notes-from-tag --latest "app/build/outputs/apk/release/app-release.apk#android-talking-book-loader-${version}_${versionCode}.apk"
