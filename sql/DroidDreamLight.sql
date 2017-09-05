/* 
 receiver(r)
 service(s)
 launch(r,s)
 action(r) = {PHONE_STATE}
 flows(s) = {$getDeviceId->!INTERNET, $InstalledPackages->!INTERNET, $getSubscriberId->!INTERNET}
 */


select node_id,  full_name, tgtId from ( 
   (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.iccg_id=? and 
                                                                                e.iccg_id=? and
                                                                                (ift.name like '%PHONE_STATE%') and 
                                                                                e.src_node_id=ift. node_id)  as tmp,
   (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3 where f1.iccg_id=? and 
                                                                                       f2.iccg_id=? and 
                                                                                       f3.iccg_id=? and 
                                                                                       f1.src_node_id=f2.src_node_id and 
                                                                                       f2.src_node_id=f3.src_node_id and 
                                                                                       f1.source='$getDeviceId' and 
                                                                                       f1.sink='!INTERNET' and  
                                                                                       f2.source='$InstalledPackages' and 
                                                                                       f2.sink='!INTERNET' and  
                                                                                       f3.source='$getSubscriberId' and 
                                                                                       f3.sink='!INTERNET' ) as tmp2,

 node as nd) where node_id=nd.id and 
                   nd.iccg_id=? and
                   nd.type='receiver' and 
                   serviceId=tgtId

