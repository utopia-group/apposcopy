/* 
 service->service
 service(r)
 service(s)
 calls(r) = {'<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'}
 flows(r) = {$getDeviceId->!INTERNET, $File->!EXEC, $getSubscriberId->!INTERNET}
 launch(r,s)
 */

select tmp1.servId from (
select serv1.id as servId from node as serv1 
 inner join edge as e on serv1.id=e.src_node_id
 inner join node as serv2 on serv2.id=e.tgt_node_id
 where serv1.iccg_id=? and 
       serv2.iccg_id=? and
       e.iccg_id=? and
       serv1.type='service' and 
       serv2.type='service'  
) as tmp1 
 inner join callerComp as cc on tmp1.servId=cc.node_id
 inner join flow as f1 on f1.src_node_id=tmp1.servId
 inner join flow as f2 on f2.src_node_id=tmp1.servId
 inner join flow as f3 on f3.src_node_id=tmp1.servId
 where  f1.src_node_id=f1.sink_node_id and
        f2.src_node_id=f2.sink_node_id and 
        f3.src_node_id=f3.sink_node_id and
        f1.iccg_id=? and
        f2.iccg_id=? and
        f3.iccg_id=? and
        cc.iccg_id=? and
        f1.source='$getDeviceId' and
        f1.sink="!INTERNET" and 
        f2.source='$getSubscriberId' and
        f2.sink="!INTERNET" and 
        f3.source='$File' and
        f3.sink="!EXEC"  and 
        cc.callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'
