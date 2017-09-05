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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import shord.analyses.DomI;
import shord.analyses.DomM;
import shord.analyses.DomT;
import shord.program.Program;
import shord.project.ClassicProject;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.util.NumberedSet;
import chord.project.Chord;

import com.apposcopy.model.XmlNode;

/**
  * Search for implicit intent.
  **/

@Chord(name="specMeth-java",
       produces={ 
                  "abortBroadcast", "RuntimeExec", "sendTextMsg"},
       namesOfTypes = {"M", "T", "I"},
       types = { DomM.class, DomT.class, DomI.class},
       namesOfSigns = { 
                        "abortBroadcast", "RuntimeExec", "sendTextMsg"},
       signs = { 
                 "M0:M0", "M0:M0", "M0:M0"}
       )
public class SpecMethodBuilder extends JavaAnalysis
{

    private ProgramRel relAbort;
    private ProgramRel relExec; 
	private ProgramRel relSendMsg;

	public static NumberedSet stubMethods;

	public static final boolean ignoreStubs = false;

	public static Map<String, XmlNode> components = new HashMap<String, XmlNode>();

    public static String pkgName = ""; 

    void openRels()
    {
		relAbort = (ProgramRel) ClassicProject.g().getTrgt("abortBroadcast");
		relExec = (ProgramRel) ClassicProject.g().getTrgt("RuntimeExec");
		relSendMsg = (ProgramRel) ClassicProject.g().getTrgt("sendTextMsg");
	    relAbort.zero();
	    relExec.zero();
	    relSendMsg.zero();
    }

    void saveRels() 
    {
		relAbort.save();
		relExec.save();
		relSendMsg.save();
    }

    public SpecMethodBuilder()
    {
        String stampOutDir = System.getProperty("stamp.out.dir");
        //parse manifest.xml
        String manifestDir = stampOutDir + "/apktool-out";
        File manifestFile = new File(manifestDir, "AndroidManifest.xml");
        ParseManifest pmf = new ParseManifest();
        pmf.extractComponents(manifestFile, components);
	    pkgName = pmf.getPkgName();
        System.out.println("Current components: " + components);
    }


	protected void visit(SootClass klass)
	{
		Collection<SootMethod> methodsCopy = new ArrayList<SootMethod>(klass.getMethods());
		for(SootMethod method : methodsCopy)
		    visitMethod(method);
	}

    private void visitMethod(SootMethod method)
    {
        if(!method.isConcrete())
            return;

        String abortSig = "<android.content.BroadcastReceiver: void abortBroadcast()>";
        String execSig = "<java.lang.Runtime: java.lang.Process exec(java.lang.String)>";
        String sendMsgSig = "<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>";

        ///////////
        if(abortSig.equals(method.getSignature()))
            relAbort.add(method);
        if(execSig.equals(method.getSignature()))
            relExec.add(method);
        if(sendMsgSig.equals(method.getSignature()))
            relSendMsg.add(method);
        ////////////
    }

    public void run()
    {
        openRels();
        for(SootClass klass: Program.g().getClasses()){
            this.visit(klass);
        }
        saveRels();
    }


}
