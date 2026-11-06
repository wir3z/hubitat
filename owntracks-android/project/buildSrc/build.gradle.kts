plugins { `kotlin-dsl` }

repositories {
  google()
  mavenCentral()
}

dependencies { implementation(libs.android.gradle.api) }

// `kotlin-dsl` pulls in java-gradle-plugin, whose jar task warns "No valid plugin
// descriptors were found in META-INF/gradle-plugins" unless every plugin is
// registered under an id. Registering here also lets app/build.gradle.kts apply
// the plugin from its plugins block instead of an out-of-band apply<T>() call.
gradlePlugin {
  plugins {
    register("espressoMetadataEmbedding") {
      // EspressoMetadataEmbeddingPlugin.kt has no package declaration.
      id = "owntracks.espresso-metadata-embedding"
      implementationClass = "EspressoMetadataEmbeddingPlugin"
    }
  }
}
