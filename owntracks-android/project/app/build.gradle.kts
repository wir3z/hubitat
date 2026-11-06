import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.hilt.android)
  // Kotlin is provided by AGP's built-in Kotlin support; the legacy-kapt plugin
  // drives data binding's annotation processing (everything else is on KSP).
  alias(libs.plugins.android.legacy.kapt)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.plugin.serialization)
  // Local plugin from buildSrc; embeds Espresso screenshots/logcats in the
  // connected-test report. Declared last so AGP's ApplicationExtension exists by
  // the time it is applied.
  id("owntracks.espresso-metadata-embedding")
}

val googleMapsAPIKey =
    System.getenv("GOOGLE_MAPS_API_KEY")
        ?: extra.get("google_maps_api_key")?.toString()
        ?: "PLACEHOLDER_API_KEY"

// Default server URL baked into the build, injected by CI from the
// HUBITAT_PRIVATE_URL CI/CD variable: an env var, else a -P property, else empty.
//
// findProperty, NOT extra.get as the maps key above uses. extra.get THROWS
// UnknownPropertyException when the property is absent rather than returning
// null, so the ?: fallback never runs. The maps key gets away with it only
// because google_maps_api_key is committed in gradle.properties and is therefore
// always present; ownTracksUrl has no such entry on purpose. Do not "restore
// symmetry" here - extra.get failed configuration outright in every job that
// passes no -P flags (format, test) and on the -pub build, which deliberately
// drops the secret flags.
//
// Empty is the upstream default and is deliberately the fallback - a build with
// nothing injected is the public build, and its URL field is simply blank for
// the user to fill in. So this must never carry a real URL as its default: the
// URL is a credential here, it embeds a per-deployment access token.
val ownTracksUrl = System.getenv("OWNTRACKS_URL") ?: findProperty("ownTracksUrl")?.toString() ?: ""

// Default HTTP Basic-auth password, injected by CI from HUBITAT_HTTP_PASSWORD.
// Property only, no env-var fallback: a bare-named CI/CD variable would outrank
// the -P flag, the trap ownTracksUrl above has to be defended against in CI.
// Empty is the upstream default, and is what a -pub build gets.
val ownTracksPassword = findProperty("ownTracksPassword")?.toString() ?: ""

val gmsImplementation: Configuration by configurations.creating

// CI passes the release tag through VERSION_NAME so a shipped build reports the
// version it was tagged as, instead of whatever was last hardcoded here. The
// literal remains the fallback for local builds and for CI runs with no tag
// (branch pipelines have nothing to read).
val versionNameValue = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "2.6.0"

fun generateVersionCode(versionName: String): Int {
  // Tolerates a tag-derived name like "v2.6.0", "2.6.0-rc1" or "2.5.9.1". Each
  // component is truncated at the first non-digit, because "0-rc1".toIntOrNull()
  // is null and would fall back to 0 - silently producing a LOWER code than the
  // previous release, which Android then refuses to install as an upgrade.
  //
  // The 4th component is deliberate, not padding: the release tag pattern allows
  // v1.2.3.4 and this project has shipped 2.5.9.1 and 2.5.9.10. patch * 1000
  // leaves exactly three digits free for it, so a build number slots in without
  // disturbing the ordering of the leading x.y.z.
  val parts = versionName.trimStart('v', 'V').split(".")
  fun part(i: Int) = parts.getOrNull(i)?.takeWhile(Char::isDigit)?.toIntOrNull() ?: 0
  return 400000000 + part(0) * 10000000 + part(1) * 100000 + part(2) * 1000 + part(3)
}

val generatedVersionCode = generateVersionCode(versionNameValue)
val envVersionCode = System.getenv("VERSION_CODE")?.toInt()
val packageVersionCode: Int =
    if (envVersionCode != null && envVersionCode > generatedVersionCode) {
      envVersionCode
    } else {
      generatedVersionCode
    }
val manuallySetVersion: Boolean = System.getenv("VERSION_CODE") != null
val enablePlayPublishing: Boolean = !System.getenv("ANDROID_PUBLISHER_CREDENTIALS").isNullOrBlank()

android {
  compileSdk = 36
  namespace = "org.owntracks.android"

  defaultConfig {
    applicationId = "org.owntracks.android"
    minSdk = 24
    targetSdk = 36

    versionCode = packageVersionCode
    versionName = versionNameValue

    val localeCount = fileTree("src/main/res/").matching { include("**/strings.xml") }.files.size

    buildConfigField(
        "int",
        "TRANSLATION_COUNT",
        localeCount.toString(),
    )

    // Consumed by DefaultsProvider as the defaults for Preferences::url and
    // Preferences::password. buildConfigField emits the third argument verbatim
    // into generated Java, so it has to arrive already quoted and escaped.
    buildConfigField(
        "String",
        "DEFAULT_URL",
        "\"" + ownTracksUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"",
    )
    buildConfigField(
        "String",
        "DEFAULT_PASSWORD",
        "\"" + ownTracksPassword.replace("\\", "\\\\").replace("\"", "\\\"") + "\"",
    )

    testInstrumentationRunner = "org.owntracks.android.testutils.hilt.CustomTestRunner"

    testInstrumentationRunnerArguments.putAll(
        mapOf(
            "clearPackageData" to "false",
            "coverage" to "true",
            "disableAnalytics" to "true",
            "useTestStorageService" to "false",
        ),
    )
    javaCompileOptions {
      annotationProcessorOptions { arguments["room.schemaLocation"] = "$projectDir/schemas" }
    }
  }

  androidResources { generateLocaleConfig = true }

  if (!System.getenv("KEYSTORE_PASSPHRASE").isNullOrBlank()) {
    signingConfigs {
      register("release") {
        keyAlias = "upload"
        keyPassword = System.getenv("KEYSTORE_PASSPHRASE")
        storeFile = file("../owntracks.release.keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSPHRASE")
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
      }
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles.addAll(
          listOf(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              file("proguard-rules.pro"),
          ),
      )
      resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
      signingConfig = signingConfigs.findByName("release")
    }

    named("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      isPseudoLocalesEnabled = true
      proguardFiles.addAll(
          listOf(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              file("proguard-rules.pro"),
          ),
      )
      resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
      applicationIdSuffix = ".debug"
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }

  buildFeatures {
    buildConfig = true
    dataBinding = true
    viewBinding = true
    resValues = true
  }

  dataBinding { addKtx = true }

  packaging {
    resources.excludes.add("META-INF/*")
    jniLibs.useLegacyPackaging = false
  }

  lint {
    baseline = file("../../lint/lint-baseline.xml")
    lintConfig = file("../../lint/lint.xml")
    checkAllWarnings = true
    warningsAsErrors = false
    abortOnError = false
    disable.addAll(
        setOf(
            "TypographyFractions",
            "TypographyQuotes",
            "Typos",
        ),
    )
  }
  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true
    unitTests { isIncludeAndroidResources = true }
    managedDevices {
      val pixel2api27 =
          localDevices.create("pixel2api27") {
            device = "Pixel 2"
            apiLevel = 27
            systemImageSource = "aosp"
          }
      val pixel8api34 =
          localDevices.create("pixel8api34") {
            device = "Pixel 8"
            apiLevel = 34
            systemImageSource = "google-atd"
          }
      groups {
        create("phoneAtd") {
          targetDevices.add(pixel2api27)
          targetDevices.add(pixel8api34)
        }
      }
    }
  }

  tasks.withType<Test> {
    testLogging {
      events(
          TestLogEvent.SKIPPED,
          TestLogEvent.FAILED,
          TestLogEvent.STANDARD_OUT,
          TestLogEvent.STANDARD_ERROR,
      )
      setExceptionFormat("full")
      showStandardStreams = true

      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
    reports.junitXml.required.set(true)
    reports.html.required.set(true)
    outputs.upToDateWhen { false }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    isCoreLibraryDesugaringEnabled = true
  }

  flavorDimensions.add("locationProvider")
  productFlavors {
    create("gms") {
      dimension = "locationProvider"
      dependencies {
        gmsImplementation(libs.gms.play.services.maps)
        gmsImplementation(libs.play.services.location)
      }
    }
    create("oss") { dimension = "locationProvider" }
  }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

tasks.withType<Test> {
  systemProperties["junit.jupiter.execution.parallel.enabled"] = false
  systemProperties["junit.jupiter.execution.parallel.mode.default"] = "same_thread"
  systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
  maxParallelForks = 1
}

tasks.withType<JavaCompile>().configureEach { options.isFork = true }

tasks.matching { it.name.endsWith("AndroidTest") }.configureEach { outputs.upToDateWhen { false } }

dependencies {
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.androidx)
  implementation(libs.androidx.test.espresso.idling)

  implementation(libs.google.material)

  // Explicit dependency on conscrypt to give up-to-date TLS support on all devices
  implementation(libs.conscrypt)

  // Mapping
  implementation(libs.osmdroid)

  // Connectivity
  implementation(libs.paho.mqttclient)
  implementation(libs.okhttp)

  // Utility libraries
  implementation(libs.bundles.hilt)
  implementation(libs.square.tape2)
  implementation(libs.timber)
  implementation(libs.bundles.androidx.room)
  implementation(libs.bundles.objectbox.migration)
  implementation(libs.kotlin.datetime)
  implementation(libs.kotlinx.serialization.json)

  // Used directly for its lightweight XSalsa20/Poly1305 primitives (EncryptionProvider); not
  // registered as a JCA Security provider, so only those specific classes end up in the app.
  implementation(libs.bouncycastle)

  // Widget libraries
  implementation(libs.widgets.materialize) { artifact { type = "aar" } }

  // Preprocessors
  ksp(libs.bundles.ksp.hilt)
  ksp(libs.androidx.room.compiler)

  kspTest(libs.bundles.ksp.hilt)

  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.androidx.core.testing)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  androidTestImplementation(libs.bundles.androidx.test)

  // Hilt Android Testing
  androidTestImplementation(libs.hilt.android.testing)
  kspAndroidTest(libs.hilt.compiler)

  androidTestImplementation(libs.barista) { exclude("org.jetbrains.kotlin") }
  androidTestImplementation(libs.okhttp.mockwebserver)
  androidTestImplementation(libs.bundles.kmqtt)
  androidTestImplementation(libs.square.leakcanary)

  androidTestUtil(libs.bundles.androidx.test.util)

  coreLibraryDesugaring(libs.desugar)
}
