# Zeilennummern in Stack Traces erhalten
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data-Model-Klassen für Firestore-Deserialisierung erhalten
-keep class de.familienwecker.famwake.model.** { *; }

# Firebase SDK mitgelieferte Rules ergänzen (Consumer-Rules vorhanden, aber zur Sicherheit)
# TODO: Diese Regeln sind zu breit. Firebase BOM bringt eigene Consumer ProGuard Rules mit.
#       Nach gründlichem Testen (R8 full mode) entfernen und prüfen, ob Crashlytics-Stacktraces
#       und Firestore-Deserialisierung weiterhin funktionieren.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }