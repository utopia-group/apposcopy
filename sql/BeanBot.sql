/*
 receiver(r)
 service(s)
 service(t)
 service(q)
 priority(r) > 1000
 action(r) = {PHONE_STATE}
 calls(r) = {'<android.content.BroadcastReceiver: void abortBroadcast()>'}
 flows(s) = {$getLine1Number->!INTERNET, $getDeviceId->!INTERNET, $getSimSerialNumber->!INTERNET}
 launch(r,s)
 launch(s,t)
 launch(s,q)
 t != q
 */

select tmp2.servId from 
(select distinct tmp1.servId as servId from 
 (select serv.id as servId from node as recv 
    inner join intentFilter as ift on ift.node_id=recv.id
    inner join callerComp as cc on cc.node_id=recv.id
    inner join edge as e1 on e1.src_node_id=recv.id
    inner join node as serv on serv.id=e1.tgt_node_id
 where recv.iccg_id=? and
        recv.type='receiver' and
        e1.iccg_id=? and
        serv.iccg_id=? and
        cc.iccg_id=? and
        ift.iccg_id=? and
        ift.priority > 1000 and
        (ift.name like '%PHONE_STATE%') and
        cc.callee='<android.content.BroadcastReceiver: void abortBroadcast()>' and
        serv.type='service' 
 ) as tmp1
    inner join edge as e2 on e2.src_node_id=tmp1.servId 
    inner join edge as e3 on e3.src_node_id=tmp1.servId 
    inner join node as serv2 on serv2.id=e2.tgt_node_id 
    inner join node as serv3 on serv3.id=e3.tgt_node_id
 where serv2.type='service' and
       e2.iccg_id=? and
       e3.iccg_id=? and
       serv2.iccg_id=? and
       serv3.iccg_id=? and
       serv3.type='service' and
       serv2.id<>serv3.id 
 ) as tmp2
    inner join flow f1 on f1.src_node_id=tmp2.servId
    inner join flow f2 on f2.src_node_id=tmp2.servId
    inner join flow f3 on f3.src_node_id=tmp2.servId
 where f1.src_node_id=f1.sink_node_id and
       f2.src_node_id=f2.sink_node_id and
       f3.src_node_id=f3.sink_node_id and
       f1.iccg_id=? and
       f2.iccg_id=? and
       f3.iccg_id=? and
       f1.source='$getLine1Number' and
       f2.source='$getDeviceId' and
       f3.source='$getSimSerialNumber' and
       f1.sink="!INTERNET" and
       f2.sink="!INTERNET" and
       f3.sink="!INTERNET" 
