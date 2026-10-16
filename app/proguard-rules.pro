# =========================================================================
# ProGuard / R8 Configuration for PIC-k150-Programing
# =========================================================================

# 1. Preservar números de línea y atributos de depuración para Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Vistas personalizadas usadas en XML y layouts
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keep class com.diamon.managers.PicAnimationView { *; }
-keep class com.diamon.managers.ReselectableSpinner { *; }

# 3. Controlador USB Serial (comunicación con hardware PL2303, CH340, FTDI, CP2102)
-keep class com.hoho.android.usbserial.** { *; }
-dontwarn com.hoho.android.usbserial.**

# 4. Modelos de microcontroladores y estructuras de datos PIC
-keep class com.diamon.chip.** { *; }
-keep class com.diamon.datos.** { *; }
-keep class com.diamon.nucleo.** { *; }
-keep class com.diamon.protocolo.** { *; }
-keep class com.diamon.excepciones.** { *; }

# 5. Google Play Billing Library y gestor de compras
-keep class com.android.billingclient.api.** { *; }
-keep class com.diamon.billing.** { *; }

# 6. Firebase (Firestore, Messaging, Crashlytics) y Google Play Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# 7. AppCenter
-keep class com.microsoft.appcenter.** { *; }
-dontwarn com.microsoft.appcenter.**

# 8. Métodos nativos JNI / NDK
-keepclasseswithmembernames class * {
    native <methods>;
}