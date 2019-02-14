-injars 'C:\Repositories\SplintGUI\out\artifacts\Splint\splint.jar'
-outjars bin\splint.jar

-libraryjars 'C:\Program Files\Java\jre1.8.0_191\lib\rt.jar'
-libraryjars 'C:\Program Files\Java\jre1.8.0_191\lib\ext\jfxrt.jar'
-libraryjars 'C:\Repositories\SplintGUI\out\artifacts\Splint\lib\args4j-2.0.16.jar'
-libraryjars 'C:\Repositories\SplintGUI\out\artifacts\Splint\lib\java-json.jar'
-libraryjars 'C:\Repositories\SplintGUI\out\artifacts\Splint\lib\sqlite-jdbc-3.23.1.jar'
-libraryjars 'C:\Repositories\SplintGUI\out\artifacts\Splint\lib\zip4j_1.3.2.jar'

-dontshrink
-dontoptimize
-flattenpackagehierarchy ''
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,LocalVariable*Table,*Annotation*,Synthetic,EnclosingMethod
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF



# Keep - Applications. Keep all application classes, along with their 'main'
# methods.
-keepclasseswithmembers public class com.cynobit.splint.Main {
    public static void main(java.lang.String[]);
}

-keepclassmembers,allowshrinking class * {
    @javafx.fxml.FXML
    <fields>;
    @javafx.fxml.FXML
    <methods>;
}
