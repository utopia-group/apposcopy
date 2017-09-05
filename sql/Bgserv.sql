/* 
 receiver(r)
 priority(r)>1000
 action(r) = {BOOT_COMPLETED}
 calls(_) = {'<android.content.BroadcastReceiver: void abortBroadcast()>'}

 service(s)
 flows(s) = {$getDeviceId->!File, $File->!WebView, $SDK->!File, $getLine1Number->!File}
 launch(r,s)
 */

  select distinct tmp2.servId from 
 ( select distinct tmp1.servId as servId from 
 (select serv.id as servId, serv.iccg_id from node as recv
    inner join intentFilter as ift on ift.node_id=recv.id
    inner join edge as e on e.src_node_id=recv.id
    inner join node as serv on serv.id=e.tgt_node_id
    where  recv.iccg_id=? and
           recv.type='receiver' and
           ift.iccg_id=? and
           e.iccg_id=? and
           serv.iccg_id=? and
           ift.name='android.intent.action.BOOT_COMPLETED' and 
           serv.type='service'
 ) as tmp1 
    inner join callerComp as cc on cc.iccg_id=?
    inner join flow as f1 on f1.src_node_id=tmp1.servId
    inner join flow as f2 on f2.src_node_id=tmp1.servId
    where   f1.src_node_id=f1.sink_node_id and
            f2.src_node_id=f2.sink_node_id  and 
            f1.iccg_id=? and
            f2.iccg_id=? and
            f1.source='$getDeviceId' and
            f1.sink='!File' and
            f2.source='$getLine1Number' and
            f2.sink='!File' and
            cc.callee='<android.content.BroadcastReceiver: void abortBroadcast()>'
  ) as tmp2 
    inner join flow as f3 on f3.src_node_id=tmp2.servId
    inner join flow as f4 on f4.src_node_id=tmp2.servId
    where   f3.src_node_id=f3.sink_node_id and
            f3.iccg_id=? and
            f4.iccg_id=? and
            f3.source='$SDK'  and
            f3.sink='!File'  and 
            f4.source='$File'  and
            f4.sink='!WebView'   

  UNION

 select distinct tmp3.servId as servId from
 (select serv.id as servId, serv.iccg_id from node as recv
    inner join intentFilter as ift on ift.node_id=recv.id
    inner join edge as e on e.src_node_id=recv.id
    inner join node as serv on serv.id=e.tgt_node_id
    where  recv.iccg_id=? and
           e.iccg_id=? and
           serv.iccg_id=? and
           ift.iccg_id=? and
           recv.type='receiver' and
           ift.name='android.intent.action.BOOT_COMPLETED' and
           serv.type='service'
 ) as tmp3
    inner join callerComp as cc on cc.iccg_id=?
    inner join callerComp as cc2 on cc2.iccg_id=?
    inner join flow as f1 on f1.src_node_id=tmp3.servId
    inner join flow as f2 on f2.src_node_id=tmp3.servId
    where   f1.src_node_id=f1.sink_node_id and
            f2.src_node_id=f2.sink_node_id  and
            f1.iccg_id=? and
            f2.iccg_id=? and
            cc.iccg_id=? and
            cc2.iccg_id=? and
            f1.source='$getDeviceId' and
            f1.sink='!File' and
            f2.source='$SDK' and
            f2.sink='!File' and
            cc.callee='<android.content.BroadcastReceiver: void abortBroadcast()>' and
            cc2.callee='<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>'


