# Jangan hapus model Firebase
-keepclassmembers class com.example.imaginate.models.** {
    *;
}

# Jangan hapus class Firebase
-keep class com.google.firebase.** { *; }

# Jangan hapus anotasi Firebase
-keepattributes Signature
-keepattributes *Annotation*

# Jangan hapus class Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Jangan hapus class ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Jangan hapus Glide Model
-keep class com.bumptech.glide.** { *; }

# Pastikan class yang digunakan tetap ada
-keepnames class com.example.imaginate.models.** { *; }
