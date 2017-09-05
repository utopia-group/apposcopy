/*
 receiver(r)
 service(s)
 activity(a)
 launch(a,s)
 action(r) = {PACKAGE_REMOVE, PACKAGE_ADDED, PACKAGE_CHANGED, PACKAGE_INSTALL, PACKAGE_REPLACED}
 calls(r) = {'<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'}
 flows(s) = {$getDeviceId->!ENC/DEC, $SDK->!ENC/DEC, $MODEL->!ENC/DEC}
  */


select distinct actId from 
 (select act.id as actId from 
 (select recv.iccg_id as iccg_id from node as recv 
       inner join intentFilter as ift on recv.id=ift.node_id
  where    recv.iccg_id=? and
           ift.iccg_id=? and
           recv.type='receiver' and
           (ift.name like '%PACKAGE_REMOVE%') and
           (ift.name like '%PACKAGE_ADDED%') and
           (ift.name like '%PACKAGE_CHANGED%') and
           (ift.name like '%PACKAGE_INSTALL%') and
           (ift.name like '%PACKAGE_REPLACED%')
   ) as tmp1
         inner join node as act on act.iccg_id=tmp1.iccg_id
         inner join edge as e on e.src_node_id=act.id
         inner join node as serv on e.tgt_node_id=serv.id
         inner join callerComp as cc on cc.node_id=serv.id
    where act.type='activity' and
          e.iccg_id=? and
          act.iccg_id=? and
          serv.iccg_id=? and
          cc.iccg_id=? and
          serv.type='service' and
          cc.callee='<java.lang.Runtime: java.lang.Process exec(java.lang.String)>'
   ) as tmp2 
         inner join flow as f1 on f1.src_node_id=tmp2.actId
         inner join flow as f2 on f1.src_node_id=tmp2.actId
         inner join flow as f3 on f1.src_node_id=tmp2.actId
    where f1.src_node_id=f1.sink_node_id and
          f2.src_node_id=f2.sink_node_id  and 
          f3.src_node_id=f3.sink_node_id  and 
          f1.iccg_id=? and
          f2.iccg_id=? and
          f3.iccg_id=? and
          f1.source='$getDeviceId' and
          f1.sink='!ENC/DEC'  and
          f2.source='$SDK' and
          f2.sink='!ENC/DEC' and
          f3.source='$MODEL' and
          f3.sink='!ENC/DEC'
