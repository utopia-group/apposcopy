/* 
 receiver(r)
 action(r) = {PACKAGE_ADDED, PACKAGE_REMOVED}
 priority(r) > 1000
 calls(r) = {'<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'}

 receiver(t)
 action(t) = {BOOT_COMPLETED}
 priority(t) > 1000

 service(s)
 calls(s) = {'<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'}
 launch(t,s)
 flows(s) = {$getDeviceId->!INTERNET, $ENC/DEC->!INTERNET, $getSubscriberId->!INTERNET}
 */

  select tmp3.servId from
 (select tmp2.servId as servId from 
 (select serv.id as servId from
 (select recv1.iccg_id as iccgId from node as recv1 
          inner join intentFilter as ift1 on recv1.id=ift1.node_id
          inner join callerComp as cc1 on recv1.id=cc1.node_id
    where  recv1.iccg_id=? and
           ift1.iccg_id=? and
           cc1.iccg_id=? and
           recv1.type='receiver' and
           (ift1.name like '%PACKAGE_ADDED%') and 
           (ift1.name like '%PACKAGE_REMOVED%') and 
           (ift1.priority>1000) and
            cc1.callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'
   ) as tmp1 
          inner join node as recv2 on recv2.iccg_id=tmp1.iccgId
          inner join intentFilter as ift2 on ift2.node_id=recv2.id
          inner join edge e1 on e1.src_node_id=recv2.id
          inner join node as serv on serv.id=e1.tgt_node_id
    where recv2.type='receiver' and 
          e1.iccg_id=? and
          serv.iccg_id=? and
          recv2.iccg_id=? and
          ift2.iccg_id=? and
          (ift2.priority>1000) and
          (ift2.name like '%BOOT_COMPLETED%') and
          serv.type='service'
    ) as tmp2
          inner join flow as f1 on f1.src_node_id=tmp2.servId
          inner join callerComp as cc2 on tmp2.servId=cc2.node_id
    where   f1.src_node_id=f1.sink_node_id and
            f1.iccg_id=? and
            cc2.iccg_id=? and
            f1.source='$getDeviceId' and
            f1.sink='!INTERNET' and
            cc2.callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'
    ) as tmp3
          inner join flow as f2 on f2.src_node_id=tmp3.servId
          inner join flow as f3 on f3.src_node_id=tmp3.servId
    where  
            f2.src_node_id=f2.sink_node_id  and 
            f3.src_node_id=f3.sink_node_id and
            f2.iccg_id=? and
            f3.iccg_id=? and
            f2.source='$ENC/DEC' and
            f2.sink='!INTERNET' and
            f3.source='$getSubscriberId'  and
            f3.sink='!INTERNET'   
