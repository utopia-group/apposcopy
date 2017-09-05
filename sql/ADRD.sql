 /* receiver -> receiver <-> service
   receiver(r)
   action(r) = {BOOT_COMPLETED}
   receiver(s)
   service(t)
   launch(r,s)
   launch(s,t)
   launch(t,s)
   flows(t) = {$getDeviceId->!ENC/DEC, $getSubscriberId->!ENC/DEC, $ENC/DEC->!INTERNET}
 */

select servId from
(
select servId from 
(
select e2.tgt_node_id as servId from 
/*recv1->recv2*/
(
select tmp.src_node_id as recv1Id, tmp.tgt_node_id as recv2Id from 
       (select * from edge e1, node recv1, node recv2 where e1.iccg_id=? and 
                                                            recv1.iccg_id=? and 
                                                            recv2.iccg_id=?  and 
                                                            recv1.type='receiver' and 
                                                            recv2.type='receiver' and 
                                                            e1.src_node_id=recv1.id and e1.tgt_node_id=recv2.id
        ) as tmp left join intentFilter as ift where ift.node_id = tmp.src_node_id and (ift.name like '%BOOT_COMPLETED%')
) as tmp2, edge as e2, edge as e3 where  e2.iccg_id=? and 
                                         e3.iccg_id=? and 
                                         e2.src_node_id=tmp2.recv2Id and 
                                         e3.tgt_node_id=tmp2.recv2Id and 
                                         e3.src_node_id=e2.tgt_node_id 
) as tmp3 left join node as servNode where (servNode.id=tmp3.servId and servNode.type='service') 
) as tmp4 inner join flow as f1 on f1.src_node_id=tmp4.servId
 inner join flow as f2 on f2.src_node_id=tmp4.servId
 inner join flow as f3 on f3.src_node_id=tmp4.servId
 where  f1.iccg_id=? and
        f2.iccg_id=? and
        f3.iccg_id=? and
        f1.source='$getDeviceId' and 
        f1.sink='!ENC/DEC' and  
        f2.source='$getSubscriberId' and 
        f2.sink='!ENC/DEC' and  
        f3.source='$ENC/DEC' and 
        f3.sink='!INTERNET' and 
        f1.src_node_id=f1.sink_node_id and 
        f2.src_node_id=f2.sink_node_id and 
        f3.src_node_id=f3.sink_node_id
