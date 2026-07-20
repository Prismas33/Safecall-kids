# AGP 9 migration notes

## Context

Google Play is showing a quality recommendation to update the Android Gradle Plugin
to version 9.0 or higher. The current project was on:

- Android Gradle Plugin: 8.12.1
- Gradle wrapper: 8.13
- Kotlin Android plugin: 1.9.10
- compileSdk: 35
- targetSdk: 35

The app already builds as an Android App Bundle and already has release shrinking
enabled with R8.

## Why migrate

- Removes the Play Console recommendation that specifically asks for Android Gradle
  Plugin 9.0 or higher.
- Enables optimized resource shrinking by default when `shrinkResources` is enabled.
- Moves Kotlin compilation to AGP built-in Kotlin support, avoiding the older
  `org.jetbrains.kotlin.android` plugin path.
- Keeps the release toolchain aligned with current Android build behavior.

## Risks and tradeoffs

- AGP 9 requires Gradle 9.1.0 or higher, so the Gradle wrapper must move from 8.13.
- AGP 9 changes the Android DSL implementation. Third-party Gradle plugins can fail
  if they depend on old AGP internals.
- AGP 9 enables built-in Kotlin by default. The old `org.jetbrains.kotlin.android`
  plugin must be removed or the build can fail.
- The old `android.kotlinOptions` block should be removed or migrated. This project
  can rely on `compileOptions.targetCompatibility = JavaVersion.VERSION_17`.
- Release behavior can change subtly because R8 defaults are newer. Billing,
  Crashlytics, call blocking, language switching and release signing must still be
  tested after the migration.
- AGP 9.1+ introduces extra R8 behavior changes. For this app, AGP 9.0.1 is the
  lowest-risk target because the Play recommendation only requires 9.0 or higher.

## Current blockers checked

- No Kotlin Multiplatform.
- No kapt.
- No custom Gradle plugin code in the repo.
- No APK density split DSL. The `bundle { density/abi/language }` DSL is for App
  Bundles and remains available in AGP 9.
- Release signing is still configured through the existing keystore properties.
- Package name and `applicationId` stay unchanged: `com.safecallkids.app`.

## Chosen migration

Use AGP 9.0.1 with Gradle 9.1.0.

Reason:

- It satisfies the Play Console recommendation.
- It is a smaller jump than AGP 9.1/9.2/9.3.
- It avoids adopting additional R8 behavior changes from later AGP 9 releases unless
  they are needed later.

## Files to change

- `build.gradle`
  - Update `com.android.application` from 8.12.1 to 9.0.1.
  - Remove `org.jetbrains.kotlin.android` from the top-level plugin declarations.

- `app/build.gradle`
  - Remove `id 'org.jetbrains.kotlin.android'`.
  - Remove the old `kotlinOptions` block.
  - Keep `compileOptions` on Java 17.
  - Keep release signing, R8, Crashlytics and Play Billing unchanged.

- `gradle/wrapper/gradle-wrapper.properties`
  - Update the wrapper distribution from Gradle 8.13 to Gradle 9.1.0.

- `gradle.properties`
  - Remove `android.r8.optimizedResourceShrinking=true`, because AGP 9 enables this
    path by default when resource shrinking is enabled.

## Validation plan

1. Run `.\gradlew.bat clean bundleRelease --console=plain`.
2. Confirm `app/build/outputs/bundle/release/app-release.aab` is generated.
3. Inspect the AAB for accidental package/application ID changes.
4. Copy the final AAB to the Desktop, replacing the previous file.

## Pending risk after migration

The Play Console can still show historical warnings for already uploaded releases.
Only a freshly uploaded AAB from the AGP 9 build can confirm which recommendations
are cleared.

## Migration result

- Build succeeded with `.\gradlew.bat clean bundleRelease --console=plain`.
- Generated AAB: `app/build/outputs/bundle/release/app-release.aab`.
- Copied AAB: `C:\Users\ventu\Desktop\SafeCall-release-5.0.1.aab`.
- AAB metadata confirms `androidGradlePluginVersion=9.0.1`.
- `jarsigner -verify` returned `jar verified`.
- The AndroidX `enableEdgeToEdge()` extension was replaced by a local
  `EdgeToEdgeConfigurer` that sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on
  Android 15 and above.

## Remaining Play Console checks

- Upload the new AAB to Play Console and wait for the release analysis to refresh.
- The AGP 9/R8 recommendation should clear because the AAB was produced by AGP
  9.0.1.
- The cutout warning should clear if it was caused by the previous AndroidX
  `enableEdgeToEdge()` path. If Play still flags a minified third-party symbol such
  as `B.k.q`, the next step is to inspect the newly uploaded bundle report from Play
  because the local source no longer references `SHORT_EDGES`.
