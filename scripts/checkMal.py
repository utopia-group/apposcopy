import sqlite3 as lite
from subprocess import PIPE, Popen
import sys

db = "/home/yufeng/research/shord/iccg_scheme.sqlite"
appLoc = "/home/yufeng/research/exp/VirusShare/"

con = None

try:
    con = lite.connect(db)
    
    cur = con.cursor()    
    cur.execute('SELECT * from iccg')
    
    rows = cur.fetchall()

    for row in rows:
    #for each apk, check whether it matches our query.
        apkId = str(row[0])
        apkName = row[1] 
        # query for DroidDreamLight.

        #dreamQuery = "select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.iccg_id="+apkId+" and (ift.name like '%PHONE_STATE%') and e.src_node_id=ift. node_id)  as tmp,  (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3 where f1.iccg_id="+apkId+" and f2.iccg_id="+apkId+" and f3.iccg_id="+apkId+" and f1.src_node_id=f2.src_node_id and f2.src_node_id=f3.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$InstalledPackages' and f2.sink='!INTERNET' and  f3.source='$getSubscriberId' and f3.sink='!INTERNET' ) as tmp2,  node as nd) where node_id=nd.id and nd.type='receiver' and serviceId=tgtId"

        querySet = {
            'DroidDreamLight' : "select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.iccg_id="+apkId+" and (ift.name like '%PHONE_STATE%') and e.src_node_id=ift. node_id)  as tmp,  (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3 where f1.iccg_id="+apkId+" and f2.iccg_id="+apkId+" and f3.iccg_id="+apkId+" and f1.src_node_id=f2.src_node_id and f2.src_node_id=f3.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$InstalledPackages' and f2.sink='!INTERNET' and  f3.source='$getSubscriberId' and f3.sink='!INTERNET' ) as tmp2,  node as nd) where node_id=nd.id and nd.type='receiver' and serviceId=tgtId"
            ,

            'GoldDream' : "select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.iccg_id="+apkId+" and ( ift.name like '%PHONE_STATE%' and ift.name like '%BOOT_COMPLETED%' and ift.name like '%SMS_RECEIVED%' and ift.name like '%NEW_OUTGOING_CALL%') and e.src_node_id=ift. node_id)  as tmp, (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3 where f1.iccg_id="+apkId+" and f2.iccg_id="+apkId+" and f3.iccg_id="+apkId+" and f1.src_node_id=f2.src_node_id and f2.src_node_id=f3.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$getSimSerialNumber' and f2.sink='!INTERNET' and  f3.source='$getSubscriberId' and f3.sink='!INTERNET' )  as tmp2, node as nd) where node_id=nd.id and nd.type='receiver' and serviceId=tgtId"
            ,
            'Geinimi' : "select  node_id as recv_id, tgtId as serviceId, tgt_node_id as activity_id from (select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.iccg_id="+apkId+" and (ift.name like '%BOOT_COMPLETE%') and e.src_node_id=ift. node_id)  as tmp, (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3, flow f4 where f1.iccg_id="+apkId+" and f2.iccg_id="+apkId+" and f3.iccg_id="+apkId+" and f4.iccg_id="+apkId+" and f1.src_node_id=f2.src_node_id and f2.src_node_id=f3.src_node_id and f3.src_node_id=f4.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$getLine1Number' and f2.sink='!INTERNET' and  f3.source='$MODEL' and f3.sink='!INTERNET' and f4.source='$BRAND' and f4.sink='!INTERNET' ) as tmp2, node as nd) where node_id=nd.id and nd.type='receiver' and serviceId=tgtId) as tmp3, edge as eg, node as self where eg. src_node_id=tgtId and eg.tgt_node_id=tgtId and self.type='service' and self.id=tgtId"
            ,
            'Pjapps' : "select tmp4.servId from(select serv.id as servId from ( select tmp2.recvId as recvId from ( select tmp.id as recvId, cc.callee as ce from ( select * from node as recv,  intentFilter as ift where recv.iccg_id="+apkId+" and recv.id=ift.node_id and ift.priority>1000  and (ift.name like '%SMS_RECEIVED%') and recv.type='receiver' ) as tmp left join callerComp as cc on cc.node_id=tmp.id ) as tmp2 where tmp2.ce='void abortBroadcast()' ) as tmp3, node as recv2, node as serv, edge as e where e.src_node_id=recv2.id and recv2.type='receiver' and e.tgt_node_id=serv.id and serv.type='service' and serv.iccg_id="+apkId+" and recv2.iccg_id="+apkId+" ) as tmp4, flow as f1, flow as f2, flow as f3, flow as f4 where tmp4.servId=f1.src_node_id and tmp4.servId=f2.src_node_id and tmp4.servId=f3.src_node_id and tmp4.servId=f4.src_node_id  and f1.source='$getSimSerialNumber' and f1.sink='!INTERNET' and f2.source='$getDeviceId' and f2.sink='!INTERNET'and  f3.source='$getSubscriberId' and f3.sink='!INTERNET' and  f4.source='$getLine1Number' and f4.sink='!INTERNET'"


            ,
            'DroidKungFu' : "select node_id as recv_id, tgtId as serviceId, tgt_node_id as activity_id from ( select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.iccg_id="+apkId+" and (ift.name like '%BOOT_COMPLETE%') and e.src_node_id=ift. node_id)  as tmp, (SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2, flow f3, flow f4 where f1.iccg_id="+apkId+" and f2.iccg_id="+apkId+" and f3.iccg_id="+apkId+" and f4.iccg_id="+apkId+" and f1.src_node_id=f2.src_node_id and f2.src_node_id=f3.src_node_id and f3.src_node_id=f4.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$getLine1Number' and f2.sink='!INTERNET' and  f3.source='$MODEL' and f3.sink='!INTERNET' and f4.source='$BRAND' and f4.sink='!INTERNET' ) as tmp2, node as nd) where node_id=nd.id and nd.type='receiver' and serviceId=tgtId) as tmp3, edge as eg, node as activity where eg. src_node_id=tgtId and eg.tgt_node_id=activity.id and activity.type='activity'"

            ,
            'BaseBridge' : "select distinct tmp3.servId from ( select e1.tgt_node_id as servId from ( select tmp.id as recvId, cc.callee as ce from ( select * from node as recv,  intentFilter as ift where recv.iccg_id="+apkId+" and recv.id=ift.node_id and ift.priority>1000 and (ift.name like '%BOOT_COMPLETED%') and (ift.name like '%SMS_RECEIVED%') and (ift.name like '%CONNECTIVITY_CHANGE%') and (ift.name like '%BATTERY_LOW%') and recv.type='receiver') as tmp left join callerComp as cc on cc.node_id=tmp.id ) as tmp2, edge as e1, node as serv where tmp2.ce='void abortBroadcast()' and e1.src_node_id=tmp2.recvId and serv.id=e1.tgt_node_id and serv.type='service') as tmp3, flow as f1, flow as f2, flow as f3, flow as f4 where tmp3.servId=f1.src_node_id and tmp3.servId=f2.src_node_id and tmp3.servId=f3.src_node_id and tmp3.servId=f4.src_node_id and f1.source='$content://sms' and f1.sink='!INTERNET' and  f2.source='$PRODUCT' and f2.sink='!INTERNET' and  f3.source='$getSubscriberId' and f3.sink='!INTERNET' and  f4.source='$MODEL' and f4.sink='!INTERNET'"
            ,
            #'ADRD' : "select distinct servId from (select servId from (select e2.tgt_node_id as servId from (select tmp.src_node_id as recv1Id, tmp.tgt_node_id as recv2Id from (select * from edge e1, node recv1, node recv2 where e1.iccg_id="+apkId+" and recv1.iccg_id="+apkId+" and recv2.iccg_id="+apkId+"  and recv1.type='receiver' and recv2.type='receiver' and e1.src_node_id=recv1.id and e1.tgt_node_id=recv2.id) as tmp left join intentFilter as ift where ift.node_id = tmp.src_node_id and (ift.name like '%BOOT_COMPLETED%') ) as tmp2, edge as e2, edge as e3 where  e2.iccg_id="+apkId+" and e3.iccg_id="+apkId+" and e2.src_node_id=tmp2.recv2Id and e3.tgt_node_id=tmp2.recv2Id and e3.src_node_id=e2.tgt_node_id ) as tmp3 left join node as servNode where (servNode.id=tmp3.servId and servNode.type='service') ) as tmp4, flow as f1, flow as f2, flow as f3 where f1.src_node_id=tmp4.servId and f2.src_node_id=tmp4.servId and f3.src_node_id=tmp4.servId and  f3.sink_node_id=tmp4.servId and f1.sink_node_id=tmp4.servId and f1.source='$getDeviceId' and f1.sink='!INTERNET' and f2.source='$content://sms//inbox' and f2.sink='!INTERNET' and f3.source='$getSubscriberId' and f3.sink='!INTERNET'"
            #,

            'ADRD2' : "select servId from (select servId from (select e2.tgt_node_id as servId from (select tmp.src_node_id as recv1Id, tmp.tgt_node_id as recv2Id from (select * from edge e1, node recv1, node recv2 where e1.iccg_id="+ apkId + " and recv1.iccg_id="+ apkId + " and recv2.iccg_id="+ apkId +  " and recv1.type='receiver' and recv2.type='receiver' and e1.src_node_id=recv1.id and e1.tgt_node_id=recv2.id) as tmp left join intentFilter as ift where ift.node_id = tmp.src_node_id and (ift.name like '%BOOT_COMPLETED%')) as tmp2, edge as e2, edge as e3 where  e2.iccg_id="+ apkId + " and e3.iccg_id="+ apkId + " and e2.src_node_id=tmp2.recv2Id and e3.tgt_node_id=tmp2.recv2Id and e3.src_node_id=e2.tgt_node_id ) as tmp3 left join node as servNode where (servNode.id=tmp3.servId and servNode.type='service') ) as tmp4 inner join flow as f1 on f1.src_node_id=tmp4.servId inner join flow as f2 on f2.src_node_id=tmp4.servId inner join flow as f3 on f3.src_node_id=tmp4.servId where  f1.iccg_id="+ apkId +" and f2.iccg_id="+ apkId +" and f3.iccg_id="+ apkId +" and f1.source='$getDeviceId' and f1.sink='!ENC/DEC' and  f2.source='$getSubscriberId' and f2.sink='!ENC/DEC' and f3.source='$ENC/DEC' and f3.sink='!INTERNET' and f1.src_node_id=f1.sink_node_id and f2.src_node_id=f2.sink_node_id and f3.src_node_id=f3.sink_node_id" 
            ,
            'AnserverBot' : "select tmp2.recvId from (select tmp.id as recvId, cc.callee as ce from (select * from node as recv,  intentFilter as ift where recv.iccg_id="+apkId+" and recv.id=ift.node_id and ift.priority>1000 and (ift.name like '%BOOT_COMPLETED%') and (ift.name like '%SMS_RECEIVED%') and (ift.name like '%UMS_CONNECTED%') and (ift.name like '%PICK_WIFI_WORK%') and recv.type='receiver' ) as tmp left join callerComp as cc on cc.node_id=tmp.id ) as tmp2, flow as f1, flow as f2, flow as f3, flow as f4 where tmp2.ce='void abortBroadcast()' and tmp2.recvId=f1.src_node_id and tmp2.recvId=f2.src_node_id and tmp2.recvId=f3.src_node_id and tmp2.recvId=f4.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$MANUFACTURER' and f2.sink='!INTERNET' and  f3.source='$getSubscriberId' and f3.sink='!INTERNET' and  f4.source='$MODEL' and f4.sink='!INTERNET'"
            ,
            'KMin' : "select node_id,  full_name, tgtId from ( (select node_id, e.tgt_node_id as tgtId from intentFilter ift,  edge e where ift.node_id=node_id and (ift.name like '%BOOT_COMPLETED%' ) and e.src_node_id=ift. node_id)  as tmp, ( SELECT  distinct f1.src_node_id as serviceId  FROM flow f1, flow f2 where f1.iccg_id="+apkId+" and f2.iccg_id="+apkId+" and f1.src_node_id=f2.src_node_id and f1.source='$getDeviceId' and f1.sink='!INTERNET' and  f2.source='$getSubscriberId' and f2.sink='!INTERNET' ) as tmp2, node as nd ) where node_id=nd.id and nd.type='receiver' and serviceId=tgtId"


                    }

        #for each app, do all the query.
        matchFamily = ''
        for key in querySet:
            currentQuery = querySet[key]

            cur.execute(currentQuery)
            matchs = cur.fetchall()
            if len(matchs) > 0 :
                matchFamily = ' ' + key

        grep = "find " + appLoc + " -iname " + apkName
        output, error = Popen(
            grep.split(" "), stdout=PIPE, stderr=PIPE).communicate()

        if matchFamily=='':
            matchFamily = 'unknown'

        print apkName + ' belongs to those families: ' + matchFamily + "  Original: " + output
        
    
except lite.Error, e:
    
    print "Error %s:" % e.args[0]
    sys.exit(1)
    
finally:
    
    if con:
        con.close()




