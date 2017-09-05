/*
 receiver(r)
 action(r) = {BOOT_COMPLETED, SMS_RECEIVED, UMS_CONNECTED, PICK_WIFI_WORK}
 priority(r) > 1000
 calls(s) = {'<android.content.BroadcastReceiver: void abortBroadcast()>'}
 flows(r) = {$getDeviceId->!INTERNET, $MODEL->!INTERNET,$MANUFACTURER->!INTERNET, $getSubscriberId->!INTERNET}
 */

select tmp2.recvId from
(
select tmp.id as recvId, cc.callee as ce from 
(
select * from node as recv,  intentFilter as ift where recv.iccg_id=? and 
                                                       recv.id=ift.node_id and 
                                                       ift.priority>1000 and 
                                                       (ift.name like '%BOOT_COMPLETED%') and 
                                                       (ift.name like '%SMS_RECEIVED%') and 
                                                       (ift.name like '%UMS_CONNECTED%') and 
                                                       (ift.name like '%PICK_WIFI_WORK%') and 
                                                       recv.type='receiver'
) as tmp left join callerComp as cc on cc.node_id=tmp.id
) as tmp2, flow as f1, flow as f2, flow as f3, flow as f4 where tmp2.ce='<android.content.BroadcastReceiver: void abortBroadcast()>' and 
                                                                tmp2.recvId=f1.src_node_id and 
                                                                tmp2.recvId=f2.src_node_id and 
                                                                tmp2.recvId=f3.src_node_id and 
                                                                tmp2.recvId=f4.src_node_id and 
                                                                f1.iccg_id=? and
                                                                f2.iccg_id=? and
                                                                f3.iccg_id=? and
                                                                f4.iccg_id=? and
                                                                f1.source='$getDeviceId' and 
                                                                f1.sink='!INTERNET' and  
                                                                f2.source='$MANUFACTURER' and 
                                                                f2.sink='!INTERNET' and  
                                                                f3.source='$getSubscriberId' and 
                                                                f3.sink='!INTERNET' and  
                                                                f4.source='$MODEL' and 
                                                                f4.sink='!INTERNET'

