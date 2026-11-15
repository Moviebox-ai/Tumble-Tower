# ProGuard / R8 rules for Tumble Tower: Skyfall
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Firebase Firestore data models (with @DocumentId and @ServerTimestamp)
-keepclassmembers class com.example.data.remote.** {
    *;
}
-keep class com.example.data.remote.** { *; }

# Keep Room entities & DAOs
-keep class com.example.data.local.** { *; }

# Keep Credential Manager & Google Identity classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
