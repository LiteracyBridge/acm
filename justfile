set positional-arguments := true

default:
    @just --choose

build_android:
    cd TalkingBookApp && ./gradlew assemble

release_android:
    #!/usr/bin/env bash
    set -euxo pipefail

    cd ./TalkingBookApp

    # Reads the value of a property from a properties file.
    # $1 - Key name, matched at beginning of line.
    function prop() {
        grep "^${1}" version.properties | cut -d'=' -f2
    }

    versionCode=$(prop 'versionCode')
    versionName=$(prop 'versionName')
    tagName="v$versionCode_android"

    # Stage & commit the version properties file, and tag the release
    git add version.properties
    git commit --message "Bump version to $versionCode"
    git tag --annotate "$tagName" --message "v/android/$versionCode_$versionName"
    git push --tags

    # Create a release on GitHub
    gh release create $tagName --title "v/android/$versionCode_$versionName" --notes-from-tag --latest 'app/release/app-release.apk#android-talking-book-loader-$versionName_versionCode.apk'
