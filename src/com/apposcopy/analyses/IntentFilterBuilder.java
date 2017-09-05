/*
 * Copyright (C) 2017 The Apposcopy and Astroid Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apposcopy.analyses;

import shord.analyses.DomComp;
import shord.analyses.DomF;
import shord.analyses.DomH;
import shord.analyses.DomI;
import shord.analyses.DomM;
import shord.analyses.DomS;
import shord.analyses.DomT;
import shord.analyses.DomU;
import shord.analyses.DomV;
import shord.analyses.DomZ;
import shord.project.ClassicProject;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import chord.project.Chord;

import com.apposcopy.model.XmlNode;

@Chord(name="intentFilter-java", 
	   produces={
	             "Service", "Receiver", "BootCompleted", "CompIntentAction", "Priority",
                 "Activity", "SmsReceived", "PickWifi", "UmsConnected", "ConnectChg", "BatteryLow", "PhoneState", 
                 "SmsSent", "NewOutCall", "SigStr", "PkgAdd", "PkgChg", "PkgRemove", "PkgReplace", "PkgInstall", "InstallAPK"},

       namesOfTypes = { "M", "Z", "I", "H", "V", "T", "F", "U", "S", "COMP" },
       types = { DomM.class, DomZ.class, DomI.class, DomH.class, DomV.class, DomT.class, DomF.class, DomU.class, DomS.class, DomComp.class},
	   namesOfSigns = { 
                        "Service", "Receiver", "BootCompleted", "CompIntentAction", "Priority",
                        "Activity", "SmsReceived", "PickWifi", "UmsConnected", "ConnectChg", "BatteryLow", "PhoneState", 
                        "SmsSent", "NewOutCall", "SigStr", "PkgAdd", "PkgChg", "PkgRemove", "PkgReplace", "PkgInstall", "InstallAPK"},

	   signs = {
                 "COMP0:COMP0", "COMP0:COMP0", "S0:S0", "COMP0,S0:COMP0_S0", "COMP0,Z0:COMP0_Z0",
                 "COMP0:COMP0", "S0:S0","S0:S0","S0:S0","S0:S0","S0:S0","S0:S0",
                 "S0:S0","S0:S0","S0:S0","S0:S0","S0:S0","S0:S0","S0:S0","S0:S0","COMP0:COMP0"}
	   )
public class IntentFilterBuilder extends JavaAnalysis
{
	private ProgramRel relCompIntentAct;
	private ProgramRel relPriority;

	private DomZ domZ;

    //add a node to represent install apk
    static String gInstallAPK = "INSTALL_APK";
    static String boot = "android.intent.action.BOOT_COMPLETED";
    static String smsRecv = "android.provider.Telephony.SMS_RECEIVED";
    static String pickWifi = "android.net.wifi.PICK_WIFI_WORK";
    static String umsConn = "android.intent.action.UMS_CONNECTED";
    static String connChg = "android.net.conn.CONNECTIVITY_CHANGE";
    static String batLow = "android.intent.action.BATTERY_LOW";
    static String phoneState = "android.intent.action.PHONE_STATE";
    static String smsSent = "android.provider.Telephony.SMS_SENT";
    static String newCall = "android.intent.action.NEW_OUTGOING_CALL";
    static String sigStr = "android.intent.action.SIG_STR";
    static String pkgAdd = "android.intent.action.PACKAGE_ADDED";
    static String pkgChg = "android.intent.action.PACKAGE_CHANGED";
    static String pkgRemove = "android.intent.action.PACKAGE_REMOVED";
    static String pkgRep = "android.intent.action.PACKAGE_REPLACED";
    static String pkgInstall = "android.intent.action.PACKAGE_INSTALL";


	void openRels()
	{
		relCompIntentAct = (ProgramRel) ClassicProject.g().getTrgt("CompIntentAction");
        relCompIntentAct.zero();
		relPriority = (ProgramRel) ClassicProject.g().getTrgt("Priority");
        relPriority.zero();
    }
	
	void saveRels()
	{
        relCompIntentAct.save();
        relPriority.save(); 
	}

	void populateRelations()
	{
		domZ = (DomZ) ClassicProject.g().getTrgt("Z");
        for(Object node : SpecMethodBuilder.components.keySet()) {
            XmlNode xml = SpecMethodBuilder.components.get(node);
            for(String pri : xml.getFilterList()) {
                int priInt = Integer.parseInt(pri);
                domZ.add(priInt);
            }
        }
        domZ.save();
        ////////////////////
        openRels();
        
        for(Object node : SpecMethodBuilder.components.keySet()) {
            XmlNode xml = SpecMethodBuilder.components.get(node);
            String strNode = (String)node;

            /////////////ACTION
            if(xml.getActionList().contains(boot)) 
                relCompIntentAct.add(strNode, boot);
            if(xml.getActionList().contains(smsRecv)) 
                relCompIntentAct.add(strNode, smsRecv);
            if(xml.getActionList().contains(pickWifi)) 
                relCompIntentAct.add(strNode, pickWifi);
            if(xml.getActionList().contains(umsConn)) 
                relCompIntentAct.add(strNode, umsConn);
            if(xml.getActionList().contains(connChg)) 
                relCompIntentAct.add(strNode, connChg);
            if(xml.getActionList().contains(batLow)) 
                relCompIntentAct.add(strNode, batLow);
            if(xml.getActionList().contains(phoneState)) 
                relCompIntentAct.add(strNode, phoneState);
            if(xml.getActionList().contains(smsSent)) 
                relCompIntentAct.add(strNode, smsSent);
            if(xml.getActionList().contains(newCall)) 
                relCompIntentAct.add(strNode, newCall);
            if(xml.getActionList().contains(sigStr)) 
                relCompIntentAct.add(strNode, sigStr);
            if(xml.getActionList().contains(pkgAdd)) 
                relCompIntentAct.add(strNode, pkgAdd);
            if(xml.getActionList().contains(pkgChg)) 
                relCompIntentAct.add(strNode, pkgChg);
            if(xml.getActionList().contains(pkgRemove)) 
                relCompIntentAct.add(strNode, pkgRemove);
            if(xml.getActionList().contains(pkgRep)) 
                relCompIntentAct.add(strNode, pkgRep);
            if(xml.getActionList().contains(pkgInstall)) 
                relCompIntentAct.add(strNode, pkgInstall);
            ///////////PRIORITY
            for(String pri : xml.getFilterList()) {
                int priInt = Integer.parseInt(pri);
                relPriority.add(strNode, priInt);
            }
            //////////////////
        }
	}


	private void populateDomComp() 
	{
		DomComp domComp = (DomComp) ClassicProject.g().getTrgt("COMP");
		DomS domS = (DomS) ClassicProject.g().getTrgt("S");
        //additional rel by yu.
	    ProgramRel relService = (ProgramRel) ClassicProject.g().getTrgt("Service");
		ProgramRel relReceiver = (ProgramRel) ClassicProject.g().getTrgt("Receiver");
		ProgramRel relActivity = (ProgramRel) ClassicProject.g().getTrgt("Activity");
		ProgramRel relCompIntentAct = (ProgramRel) ClassicProject.g().getTrgt("CompIntentAction");
		ProgramRel relBootComplete = (ProgramRel) ClassicProject.g().getTrgt("BootCompleted");
		ProgramRel relSmsRecv = (ProgramRel) ClassicProject.g().getTrgt("SmsReceived");
		ProgramRel relPickWifi = (ProgramRel) ClassicProject.g().getTrgt("PickWifi");
		ProgramRel relUmsConn = (ProgramRel) ClassicProject.g().getTrgt("UmsConnected");
		ProgramRel relConnChg = (ProgramRel) ClassicProject.g().getTrgt("ConnectChg");
		ProgramRel relBatLow = (ProgramRel) ClassicProject.g().getTrgt("BatteryLow");
		ProgramRel relPhoneState = (ProgramRel) ClassicProject.g().getTrgt("PhoneState");
		ProgramRel relSmsSent = (ProgramRel) ClassicProject.g().getTrgt("SmsSent");
		ProgramRel relNewCall = (ProgramRel) ClassicProject.g().getTrgt("NewOutCall");
		ProgramRel relSigStr = (ProgramRel) ClassicProject.g().getTrgt("SigStr");
		ProgramRel relPkgAdd = (ProgramRel) ClassicProject.g().getTrgt("PkgAdd");
		ProgramRel relPkgChg = (ProgramRel) ClassicProject.g().getTrgt("PkgChg");
		ProgramRel relPkgRemove = (ProgramRel) ClassicProject.g().getTrgt("PkgRemove");
		ProgramRel relPkgRep = (ProgramRel) ClassicProject.g().getTrgt("PkgReplace");
		ProgramRel relPkgInstall = (ProgramRel) ClassicProject.g().getTrgt("PkgInstall");
		ProgramRel relCompAPK = (ProgramRel) ClassicProject.g().getTrgt("InstallAPK");

        domS.add(boot);
        domS.add(smsRecv);
        domS.add(pickWifi);
        domS.add(umsConn);
        domS.add(connChg);
        domS.add(batLow);
        domS.add(phoneState);
        domS.add(smsSent);
        domS.add(newCall);
        domS.add(sigStr);
        domS.add(pkgAdd);
        domS.add(pkgChg);
        domS.add(pkgRemove);
        domS.add(pkgRep);
        domS.add(pkgInstall);

        for(Object node : SpecMethodBuilder.components.keySet()) {
            domComp.add((String)node);
            domS.add((String)node);
        }

		domS.save();
        domComp.add(gInstallAPK);
        domComp.save();

        ////////////
        relCompAPK.zero();
        relCompAPK.add(gInstallAPK);
        relCompAPK.save();
        /////////////
	    relService.zero();
        relReceiver.zero();
        relActivity.zero();
        relCompIntentAct.zero();
        relBootComplete.zero();
        relSmsRecv.zero();
        relPickWifi.zero();
        relUmsConn.zero();
        relConnChg.zero();
        relBatLow.zero();
        relPhoneState.zero();
        relSmsSent.zero();
        relNewCall.zero();
        relSigStr.zero();
        relPkgAdd.zero();
        relPkgChg.zero();
        relPkgRemove.zero();
        relPkgRep.zero();
        relPkgInstall.zero();

        //////////////////////////////////
        relBootComplete.add(boot);
        relSmsRecv.add(smsRecv);
        relPickWifi.add(pickWifi);
        relUmsConn.add(umsConn);
        relConnChg.add(connChg);
        relBatLow.add(batLow);
        relPhoneState.add(phoneState);
        relSmsSent.add(smsSent);
        relNewCall.add(newCall);
        relSigStr.add(sigStr);
        relPkgAdd.add(pkgAdd);
        relPkgChg.add(pkgChg);
        relPkgRemove.add(pkgRemove);
        relPkgRep.add(pkgRep);
        relPkgInstall.add(pkgInstall);

        for(Object node : SpecMethodBuilder.components.keySet()) {
            System.out.println("Component: " + node);
            System.out.println("Value: " + SpecMethodBuilder.components.get(node));
            XmlNode xml = SpecMethodBuilder.components.get(node);
            System.out.println("------------------------------------");
            if("service".equals(xml.getType()))
		        relService.add(node);
            else if("receiver".equals(xml.getType()))
		        relReceiver.add(node);
            else 
                relActivity.add(node);
        }

        relService.save();
		relReceiver.save();
		relActivity.save();
		relBootComplete.save();
        relSmsRecv.save();
        relPickWifi.save();
        relUmsConn.save();
        relConnChg.save();
        relBatLow.save();
        relPhoneState.save();
        relSmsSent.save();
        relNewCall.save();
        relSigStr.save();
        relPkgAdd.save();
        relPkgChg.save();
        relPkgRemove.save();
        relPkgRep.save();
        relPkgInstall.save();
	}

	public void run()
	{
		openRels();
	    populateDomComp();
		populateRelations();
		saveRels();
	}
}
