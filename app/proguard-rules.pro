# Keep Kotlinx Serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclassesWithStringConcatenationClassBody kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.galaxy.weatherwatch.**$$serializer { *** Companion; }
-keepclassmembers class com.galaxy.weatherwatch.** {
    *** Companion;
}
-keepclassesWithStringConcatenationClassBody com.galaxy.weatherwatch.** {
    *** Companion;
}

# Keep model data classes for serialization
-keep class com.galaxy.weatherwatch.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
