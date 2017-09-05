/*receiver->service(leak data)->activity
 receiver(r)
 service(s)
 activity(a)
 launch(r,s)
 launch(s,a)
 action(r) = {BOOT_COMPLETED}
 flows(s) = {$getDeviceId->!INTERNET, $MODEL->!INTERNET, $BRAND->!INTERNET, $getLine1Number->!INTERNET, $ENC/DEC->!INTERNET}
 !flows(_) = {$ENC/DEC->!INTERNET}
 installAPK(_)
 */


select tmp4.serviceId, tmp5.cnt from
(
select distinct serviceId from
(
  select node_id as recv_id, tgtId as serviceId, tgt_node_id as activity_id from (
    /*receiver a will launch b*/
      select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e 
                                                    where ift.iccg_id=? and 
                                                          e.iccg_id=? and
                                                          (ift.name like '%BOOT_COMPLETE%') and 
                                                          e.src_node_id=ift.node_id)  as tmp,

        /*service  b that generates src-sink*/
          (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3, flow f4 where f1.iccg_id=? and 
                                                                                                       f2.iccg_id=? and 
                                                                                                       f3.iccg_id=? and 
                                                                                                       f4.iccg_id=? and 
                                                                                                       f1.src_node_id=f2.src_node_id and 
                                                                                                       f2.src_node_id=f3.src_node_id and 
                                                                                                       f3.src_node_id=f4.src_node_id and 
                                                                                                       f1.source='$getDeviceId' and 
                                                                                                       f1.sink='!INTERNET' and  
                                                                                                       f2.source='$getLine1Number' and 
                                                                                                       f2.sink='!INTERNET' and  
                                                                                                       f3.source='$MODEL' and 
                                                                                                       f3.sink='!INTERNET' and 
                                                                                                       f4.source='$BRAND' and f4.sink='!INTERNET' ) as tmp2,

            /*service b launches activity c. */
               node as nd) where node_id=nd.id and 
                                 nd.iccg_id=? and
                                 nd.type='receiver' and 
                                 serviceId=tgtId) as tmp3, edge as eg, node as activity where eg.src_node_id=tgtId and 
                                                                                              eg.iccg_id=? and
                                                                                              activity.iccg_id=? and
                                                                                              eg.tgt_node_id=activity.id and 
                                                                                              activity.type='activity'
                 /*install apk can be in other component. */
                 ) as tmp3, edge as eInstall, node as nInstall where 
                                                                eInstall.iccg_id=? and 
                                                                nInstall.iccg_id=? and 
                                                                eInstall.tgt_node_id=nInstall.id and 
                                                                nInstall.full_name='INSTALL_APK'
                 /*no encrypt c&c server for kungfu1.*/
                 ) as tmp4,
                 ( select count(*) as cnt from flow as f5 where 
                        f5.iccg_id=? and f5.sink_node_id=f5.src_node_id and f5.source='$ENC/DEC' and f5.sink='!INTERNET') as tmp5
                where tmp5.cnt=0

