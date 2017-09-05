 /*
   receiver(r)
   service(s)
   calls(s) = {'<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'}
   flows(s) = {$getDeviceId->!INTERNET, $getLine1Number->!INTERNET, $getSubscriberId->!INTERNET}
   {$ENC/DEC -> !INTERNET} is not a subset of flows(_) 
   launch(r,s)
  */

 select tmp3.servId, tmp4.cnt from
 (select distinct tmp2.servId from 
 (select distinct tmp1.servId as servId from  
 (select serv.id as servId from node as recv
        inner join intentFilter as ift on ift.node_id=recv.id
        inner join edge as e on e.src_node_id=recv.id
        inner join node as serv on serv.id=e.tgt_node_id
        inner join callerComp as cc on cc.node_id=serv.id
  where  recv.iccg_id=? and
         e.iccg_id=? and
         cc.iccg_id=? and
         serv.iccg_id=? and
         ift.iccg_id=? and
         recv.type='receiver' and
         serv.type='service' and
         ift.name like '%BOOT_COMPLETED%' and
         cc.callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'
 ) as tmp1
    inner join flow as f1 on f1.src_node_id=tmp1.servId
    inner join flow as f2 on f2.src_node_id=tmp1.servId
    where   f1.src_node_id=f1.sink_node_id and
            f2.src_node_id=f2.sink_node_id  and 
            f1.iccg_id=? and
            f2.iccg_id=? and
            f1.source='$getDeviceId' and
            f1.sink='!INTERNET' and
            f2.source='$getLine1Number' and
            f2.sink='!INTERNET' 
    ) as tmp2
    inner join flow as f3 on f3.src_node_id=tmp2.servId
    inner join flow as f4 on f4.src_node_id=tmp2.servId
    where   f3.src_node_id=f3.sink_node_id and
            f4.src_node_id=f4.sink_node_id and
            f3.iccg_id=? and
            f4.iccg_id=? and
            f3.source='$getSubscriberId'  and
            f3.sink='!INTERNET'  and 
            f4.source='$getSimSerialNumber'  and
            f4.sink='!INTERNET' 
 ) as tmp3, 
    ( select count(*) as cnt from flow as f5 where 
                f5.iccg_id=? and f5.sink_node_id=f5.src_node_id and 
                f5.source='$ENC/DEC' and f5.sink='!INTERNET') as tmp4
                where tmp4.cnt=0

