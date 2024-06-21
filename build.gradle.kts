buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    }
}

plugins {
    id("com.android.application").version("8.4.1").apply(false)
    id("com.android.library").version("8.4.1").apply(false)
    id("org.jetbrains.kotlin.android").version("1.8.21").apply(false)
}