# Class ini di-instansiasi oleh proses Shizuku berdasarkan nama class
-keep class com.udyneos.resteam.shizuku.ShellService { *; }
-keep class com.udyneos.resteam.shizuku.IShellService { *; }
# Stub AIDL adalah nested class; tanpa ini R8 mengganti nama/membuangnya dan bind Shizuku gagal diam-diam
-keep class com.udyneos.resteam.shizuku.IShellService$Stub { *; }
-keep class com.udyneos.resteam.shizuku.IShellService$Stub$Proxy { *; }
-keepclassmembers class com.udyneos.resteam.shizuku.ShellService { public <init>(...); }
-keep class com.udyneos.resteam.session.GameSessionService { *; }
