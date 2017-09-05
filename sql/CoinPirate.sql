/* 
 receiver(r)
 priority(r)>1000
 action(r) = {SMS_SENT, SME_RECEIVED}
 calls(r) = {'<android.content.BroadcastReceiver: void abortBroadcast()>'}

 receiver(t)
 service(s)
 calls(s) = {'<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>'}
 flows(s) = {$getDeviceId->!INTERNET, $MODEL->!INTERNET, $SDK->!INTERNET, $getSubscriberId->!INTERNET}
 launch(r,s)
 launch(s,t)
 */

  select tmp4.servId from 
 (select tmp3.servId as servId from  
 (select distinct tmp2.servId as servId from 
 ( select serv.id as servId from 
 (select smsRecv.id as smsId from node as smsRecv
          inner join intentFilter as ift on smsRecv.id=ift.node_id
          inner join callerComp as cc1 on smsRecv.id=cc1.node_id
  where smsRecv.type='receiver' and
                 smsRecv.iccg_id=? and
                 ift.iccg_id=? and
                 cc1.iccg_id=? and
                 (ift.priority > 1000) and
                 (ift.name like '%SMS_SENT%') and
                 (ift.name like '%SMS_RECEIVED%') and
                  cc1.callee='<android.content.BroadcastReceiver: void abortBroadcast()>'
    ) as tmp1
           inner join edge as e1 on tmp1.smsId=e1.src_node_id
           inner join node as serv on e1.tgt_node_id=serv.id
           inner join edge as e2 on e2.src_node_id=serv.id
           inner join node as alarmRecv on alarmRecv.id=e2.tgt_node_id
     where alarmRecv.type='receiver' and
           e1.iccg_id=? and
           e2.iccg_id=? and
           serv.iccg_id=? and
           alarmRecv.iccg_id=? and
           serv.type='service' 
    ) as tmp2
            inner join callerComp as cc2 on tmp2.servId
            inner join flow as f1 on f1.src_node_id=tmp2.servId
       where  f1.src_node_id=f1.sink_node_id and
              f1.iccg_id=? and
              cc2.iccg_id=? and
              f1.source='$getDeviceId' and
              f1.sink='!INTERNET' and
              cc2.callee='<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>'
   ) as tmp3 
            inner join flow as f3 on f3.src_node_id=tmp3.servId
            inner join flow as f4 on f4.src_node_id=tmp3.servId
       where  f3.src_node_id=f3.sink_node_id and
              f4.src_node_id=f4.sink_node_id  and 
              f3.iccg_id=? and
              f4.iccg_id=? and
              f3.source='$SDK' and
              f3.sink='!INTERNET' and
              f4.source='$MODEL'  and
              f4.sink='!INTERNET' 
   ) as tmp4
            inner join flow as f2 on f2.src_node_id=tmp4.servId
        where 
               f2.src_node_id=f2.sink_node_id  and 
               f2.iccg_id=? and
               f2.source='$getSubscriberId'  and
               f2.sink='!INTERNET'  
