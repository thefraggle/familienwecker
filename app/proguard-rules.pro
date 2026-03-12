# Zeilennummern in Stack Traces erhalten
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data-Model-Klassen für Firestore-Deserialisierung erhalten
-keep class de.familienwecker.famwake.model.** { *; }

# Firebase SDK mitgelieferte Rules ergänzen (Consumer-Rules vorhanden, aber zur Sicherheit)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }