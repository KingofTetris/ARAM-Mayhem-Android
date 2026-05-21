# ============================================================
# ARAM Mayhem Assistant - ProGuard Rules
# ============================================================

# ---------- Generic ----------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ---------- Retrofit ----------
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# ---------- OkHttp ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ---------- Gson ----------
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# ---------- core-common ----------
-keep class com.aram.mayhem.common.Result { *; }
-keep class com.aram.mayhem.common.Tier { *; }
-keep class com.aram.mayhem.common.Constants { *; }

# ---------- core-network DTO ----------
-keep class com.aram.mayhem.network.dto.** { *; }
-keep class com.aram.mayhem.network.api.** { *; }

# ---------- core-data Room ----------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.aram.mayhem.data.local.entity.** { *; }
-keep class com.aram.mayhem.data.local.converter.** { *; }
-keep class com.aram.mayhem.data.local.dao.** { *; }
-keep class com.aram.mayhem.data.local.AppDatabase { *; }
-keep class com.aram.mayhem.data.local.AppDatabaseMigrations { *; }

# ---------- core-ui UiModel ----------
-keep class com.aram.mayhem.ui.model.** { *; }

# ---------- Hilt / Dagger ----------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ---------- Navigation ----------
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.fragment.app.Fragment { *; }
-keepclassmembers class * {
    public static final ** CREATOR;
}

# ---------- AndroidX / Lifecycle ----------
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @androidx.lifecycle.MutableLiveData *;
    @androidx.lifecycle.LiveData *;
}

# ---------- Timber ----------
-dontwarn timber.log.**
-keep class timber.log.** { *; }

# ---------- Serializable / Parcelable ----------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---------- Enum ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Native methods ----------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- View binding ----------
-keep class * implements androidx.viewbinding.ViewBinding {
    *** bind(android.view.View);
    *** inflate(android.view.LayoutInflater);
}

# ---------- Material Components ----------
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ---------- AndroidX AppCompat ----------
-keep class androidx.appcompat.** { *; }
-keep class androidx.core.** { *; }
-dontwarn androidx.appcompat.**

# ---------- ConstraintLayout ----------
-keep class androidx.constraintlayout.** { *; }
-dontwarn androidx.constraintlayout.**

# ---------- RecyclerView ----------
-keep class androidx.recyclerview.** { *; }
-dontwarn androidx.recyclerview.**

# ---------- ViewPager2 ----------
-keep class androidx.viewpager2.** { *; }
-dontwarn androidx.viewpager2.**

# ---------- SwipeRefreshLayout ----------
-keep class androidx.swiperefreshlayout.** { *; }
-dontwarn androidx.swiperefreshlayout.**

# ---------- CardView ----------
-keep class androidx.cardview.** { *; }
-dontwarn androidx.cardview.**

# ---------- CoordinatorLayout ----------
-keep class androidx.coordinatorlayout.** { *; }
-dontwarn androidx.coordinatorlayout.**
