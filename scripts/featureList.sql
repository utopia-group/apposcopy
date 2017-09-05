 select F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 from 
 (/*F1*/
 select count(*) as F1 from callerComp where  iccg_id=? and callee='<android.content.BroadcastReceiver: void abortBroadcast()>'
 ),
 (/*F2*/
 select count(*) as F2  from edge as e 
            inner join node as n on e.tgt_node_id=n.id where 
                 n.full_name='INSTALL_APK' and n.iccg_id=?
 ),
 (/*F3*/
 select count(*)as F3 from callerComp where iccg_id=? and
           (callee='<android.telephony.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>' OR
            callee='<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>')
),
(/*F4*/
 select count(*) as F4 from callerComp where  iccg_id=? and
           (callee='<javax.crypto.Cipher: byte[] doFinal()>' OR
            callee='<javax.crypto.Cipher: byte[] doFinal(byte[])>' OR
            callee='<javax.crypto.Cipher: int doFinal(byte[],int)>' OR
            callee='<javax.crypto.Cipher: byte[] doFinal(byte[],int,int)>' OR
            callee='<javax.crypto.Cipher: int doFinal(byte[],int,int,byte[])>' OR
            callee='<javax.crypto.Cipher: byte[] doFinal(byte[])>' OR
            callee='<javax.crypto.Cipher: byte[] update(byte[])>' OR
            callee='<javax.crypto.Cipher: byte[] update(byte[],int,int)>' OR
            callee='<javax.crypto.Cipher: int update(byte[],int,int,byte[])>' OR
            callee='<javax.crypto.Cipher: int update(byte[],int,int,byte[],int)>' OR
            callee='<javax.crypto.Cipher: int update(java.nio.ByteBuffer,java.nio.ByteBuffer)>')
 ),
 (/*F5*/
 select  count(*) as F5 from intentFilter where priority > 0 and iccg_id=?
 ),
 (/*F6*/
 select count(*) as F6 from callerComp where iccg_id=? and
            (callee='<android.app.WallpaperManager: void setBitmap(android.graphics.Bitmap)>' OR
            callee='<android.app.WallpaperManager: void setResource(int)>' OR
            callee='<android.app.WallpaperManager: void setStream(java.io.InputStream)>' OR
            callee='<android.content.Context: void setWallpaper(java.io.InputStream)>' OR
            callee='<android.content.Context: void setWallpaper(android.graphics.Bitmap)>' )
 ), 
 (/*F7*/
 select  count(*) as F7 from callerComp where   iccg_id=? and
           (callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String)>' or
            callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String[])>' or
            callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String[],java.lang.String[])>' or
            callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String[],java.lang.String[],java.io.File)>' or
            callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String,java.lang.String[])>' or
            callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String,java.lang.String[],java.io.File)>')
 ),
 (/*F8*/
 select count(*) as F8 from callerComp where iccg_id=? and
           (callee='<java.lang.System: void loadLibrary(java.lang.String)>' OR
            callee='<java.lang.System: void load(java.lang.String)>' )
 ),
 (/*F9*/
 select count(*) as F9  from callerComp where iccg_id=? and
          ( callee='<android.content.Context: java.lang.String getPackageName()>'  OR
            callee='<android.content.Context: android.content.pm.PackageManager getPackageManager()>' )
 ),
 (/*F10*/
 select count(*) as F10 from callerComp where iccg_id=? and
          ( callee='<dalvik.system.DexClassLoader: void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.ClassLoader)>' OR
            callee='<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>')
 ),
  (/*F11*/
   select max(NumberOfFlows), type as F11, id as F12 from
   (select node.id, node.type, count(flow.id) as NumberOfFlows from (node inner join flow on flow.src_node_id=node.id) 
     where flow.iccg_id=? and 
               flow.src_node_id=flow.sink_node_id and 
               ( flow.sink='!INTERNET' or flow.sink='!File' or flow.sink='!FILE' or 
                 flow.sink='!EXEC' or flow.sink='!WebView' or flow.sink='!ENC/DEC' or flow.sink='!SOCKET' ) and 
               (source='$getDeviceId' or source='$getLine1Number' or source='$getSubscriberId' or
                source='$getSimSerialNumber' or source='$SDK' or source='$MODEL' or source='$BRAND' or
                source='$File' or source='$ENC/DEC' or source='$InstalledPackages' or
                source='$content://sms' or source='$RELEASE' or source='$PRODUCT' or
                source='MANUFACTURER')
    group by node.id  having  count(flow.id) >0)  
  )
