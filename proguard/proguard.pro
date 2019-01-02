#
# This ProGuard configuration file illustrates how to process ProGuard itself.
# Configuration files for typical applications will be very similar.
# Usage:
#     java -jar proguard.jar @proguard.pro
#

# Specify the input jars, output jars, and library jars.
# We'll filter out the Ant and WTK classes, keeping everything else.

-injars  ../distrib/aofeinfc_temp.jar
-outjars ../distrib/aofei_nfc.jar


-libraryjars ../libs/;../android_lib/

# Write out an obfuscation mapping file, for de-obfuscating any stack traces
# later on, or for incremental obfuscation of extensions.

-printmapping proguard.map

# Allow methods with the same signature, except for the return type,
# to get the same obfuscation name.

#-overloadaggressively

-dontshrink 

# Allow classes and class members to be made public.

-allowaccessmodification

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}



# Keep names - Native method names. Keep all native class/method names.
-keepclasseswithmembers,allowshrinking class com.aofei.nfc.TagUtil {
    public <methods>;
}
-keepclasseswithmembers,allowshrinking class com.aofei.nfc.AuthenticationException {
    public <methods>;
}
-keepclasseswithmembers,allowshrinking class com.aofei.nfc.ThreeDES {
    public <methods>;
}
