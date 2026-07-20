# Zeilennummern in Stack Traces erhalten
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data-Model-Klassen für Firestore-Deserialisierung erhalten
-keep class de.familienwecker.famwake.model.** { *; }

# Firebase SDK and GMS use automatic consumer ProGuard rules, no need to manually keep all classes.

# Lottie
-keep class com.airbnb.lottie.** { *; }