# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keep public class * implements com.bumptech.glide.module.GlideModule

-keep class com.mopub.mobileads.** {*;}
-dontwarn com.facebook.ads.internal.**

# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keep class com.smartfoxitsolutions.lockup.ResetPasswordResponse {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusLoginResponse {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusLoginData {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusRecoveryResponse {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusResetResponse {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusSignUpResponse {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusSignUpData {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.LoyaltyBonusInitialPointResponse {*;}
-keep class com.smartfoxitsolutions.lockup.loyaltybonus.services.UserLoyaltyReportResponse {*;}







