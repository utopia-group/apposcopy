package com.apposcopy.analyses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import shord.analyses.DomM;
import shord.program.Program;
import shord.project.ClassicProject;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import stamp.analyses.DomL;
import stamp.analyses.SootUtils;
import chord.project.Chord;

/**
 * @author Yu Feng
 **/
@Chord(name = "srcsink-java", consumes = { "M", "Z" }, produces = { "DeviceId",
		"SubId", "Internet", "EncSrc", "EncSink", "MODEL", "BRAND", "SDK",
		"Manufact", "Product", "LineNumber", "SmsContent", "SimSerial",
		"FileSrc", "FileSink", "WebView", "Exec", "InstallPackage" }, namesOfTypes = { "L" }, types = { DomL.class }, namesOfSigns = {
		"DeviceId", "SubId", "Internet", "EncSrc", "EncSink", "MODEL", "BRAND",
		"SDK", "Manufact", "Product", "LineNumber", "SmsContent", "SimSerial",
		"FileSrc", "FileSink", "WebView", "Exec", "InstallPackage", "AppSrcSink" }, signs = {
		"L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0",
		"L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0", "L0:L0",
		"L0:L0", "L0:L0", "L0:L0" })
public class SrcSinkBuilder extends JavaAnalysis {
	private ProgramRel relArgArgTransfer;
	private ProgramRel relArgRetTransfer;
	private ProgramRel relArgArgFlow;

	private ProgramRel relInLabelArg;
	private ProgramRel relInLabelRet;
	private ProgramRel relOutLabelArg;
	private ProgramRel relOutLabelRet;

	public void run() {
		List<String> srcLabels = new ArrayList<String>();
		List<String> sinkLabels = new ArrayList<String>();
		List<String> worklist = new LinkedList<String>();
		// fill ArgArgTransfer, ArgRetTransfer, ArgArgFlow
		process(srcLabels, sinkLabels, worklist);

		// by yufeng.
		sigLabels(srcLabels, sinkLabels);
	}

	private void sigLabels(List<String> srcLabels, List<String> sinkLabels) {
		ProgramRel relAppSrcSink = (ProgramRel) ClassicProject.g().getTrgt("AppSrcSink");
		ProgramRel relDeviceId = (ProgramRel) ClassicProject.g().getTrgt(
				"DeviceId");
		ProgramRel relSubId = (ProgramRel) ClassicProject.g().getTrgt("SubId");
		ProgramRel relInternet = (ProgramRel) ClassicProject.g().getTrgt(
				"Internet");
		ProgramRel relEncSrc = (ProgramRel) ClassicProject.g()
				.getTrgt("EncSrc");
		ProgramRel relEncSink = (ProgramRel) ClassicProject.g().getTrgt(
				"EncSink");
		ProgramRel relModel = (ProgramRel) ClassicProject.g().getTrgt("MODEL");
		ProgramRel relBrand = (ProgramRel) ClassicProject.g().getTrgt("BRAND");
		ProgramRel relSdk = (ProgramRel) ClassicProject.g().getTrgt("SDK");
		ProgramRel relManufact = (ProgramRel) ClassicProject.g().getTrgt(
				"Manufact");
		ProgramRel relProduct = (ProgramRel) ClassicProject.g().getTrgt(
				"Product");
		ProgramRel relLineNumber = (ProgramRel) ClassicProject.g().getTrgt(
				"LineNumber");
		ProgramRel relSmsContent = (ProgramRel) ClassicProject.g().getTrgt(
				"SmsContent");
		ProgramRel relSimSerial = (ProgramRel) ClassicProject.g().getTrgt(
				"SimSerial");
		ProgramRel relFileSrc = (ProgramRel) ClassicProject.g().getTrgt(
				"FileSrc");
		ProgramRel relFileSink = (ProgramRel) ClassicProject.g().getTrgt(
				"FileSink");
		ProgramRel relWebView = (ProgramRel) ClassicProject.g().getTrgt(
				"WebView");
		ProgramRel relExec = (ProgramRel) ClassicProject.g().getTrgt("Exec");
		ProgramRel relInstallPkg = (ProgramRel) ClassicProject.g().getTrgt(
				"InstallPackage");

		relAppSrcSink.zero();
		relDeviceId.zero();
		relSubId.zero();
		relInternet.zero();
		relEncSrc.zero();
		relEncSink.zero();
		relModel.zero();
		relBrand.zero();
		relSdk.zero();
		relManufact.zero();
		relProduct.zero();
		relLineNumber.zero();
		relSmsContent.zero();
		relSimSerial.zero();
		relFileSrc.zero();
		relFileSink.zero();
		relWebView.zero();
		relExec.zero();
		relInstallPkg.zero();

		for (String l : srcLabels) {
			if (l.equals("$getDeviceId")) 
				relDeviceId.add(l);

			if (l.equals("$getSubscriberId")) 
				relSubId.add(l);

			if (l.equals("$ENC/DEC")) 
				relEncSrc.add(l);

			if (l.equals("$MODEL"))
				relModel.add(l);

			if (l.equals("$BRAND"))
				relBrand.add(l);

			if (l.equals("$SDK"))
				relSdk.add(l);

			if (l.equals("$MANUFACTURER"))
				relManufact.add(l);

			if (l.equals("$PRODUCT"))
				relProduct.add(l);

			if (l.equals("$getLine1Number"))
				relLineNumber.add(l);

			if (l.equals("$content://sms"))
				relSmsContent.add(l);

			if (l.equals("$getSimSerialNumber"))
				relSimSerial.add(l);

			if (l.equals("$File"))
				relFileSrc.add(l);

			if (l.equals("$InstalledPackages"))
				relInstallPkg.add(l);

            if(l.equals("$getDeviceId") || l.equals("$getSubscriberId") || l.equals("$ENC/DEC") 
               || l.equals("$MODEL") || l.equals("$BRAND") || l.equals("$SDK") || l.equals("$MANUFACTURER") 
               || l.equals("$PRODUCT") || l.equals("$getLine1Number") || l.equals("$content://sms") 
               || l.equals("$getSimSerialNumber") || l.equals("$File") || l.equals("$InstalledPackages") 
               || l.equals("$MyDate") || l.equals("$MyTime") || l.equals("$NetworkOperator")
               || l.equals("$SMS") || l.equals("$SimCountryIso") || l.equals("$SimOperator") || l.equals("$SimOperatorName") || l.contains("com.km") || l.equals("$ExternalStorage"))
               relAppSrcSink.add(l);

		}
		for (String l : sinkLabels) {
			if (l.equals("!INTERNET"))
				relInternet.add(l);
			if (l.equals("!ENC/DEC"))
				relEncSink.add(l);
			if (l.equals("!FILE"))
				relFileSink.add(l);
			if (l.equals("!WebView"))
				relWebView.add(l);
			if (l.equals("!PROCESS.OutputStream"))
				relExec.add(l);

            if(l.equals("!INTERNET") || l.equals("!ENC/DEC") || l.equals("!FILE") 
                || l.equals("!WebView") || l.equals("!PROCESS.OutputStream") || l.equals("!sendTextMessage") || l.contains("!createSubprocess")) 
               relAppSrcSink.add(l);
		}

        relAppSrcSink.save();
		relDeviceId.save();
		relSubId.save();
		relInternet.save();
		relEncSrc.save();
		relEncSink.save();
		relModel.save();
		relBrand.save();
		relSdk.save();
		relProduct.save();
		relManufact.save();
		relLineNumber.save();
		relSmsContent.save();
		relSimSerial.save();
		relFileSrc.save();
		relFileSink.save();
		relWebView.save();
		relExec.save();
		relInstallPkg.save();
	}

	private void process(List<String> srcLabels, List<String> sinkLabels,
			List worklist) {
		relArgArgTransfer = (ProgramRel) ClassicProject.g().getTrgt(
				"ArgArgTransfer");
		relArgRetTransfer = (ProgramRel) ClassicProject.g().getTrgt(
				"ArgRetTransfer");
		relArgArgFlow = (ProgramRel) ClassicProject.g().getTrgt("ArgArgFlow");

		relArgArgTransfer.zero();
		relArgRetTransfer.zero();
		relArgArgFlow.zero();

		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		Scene scene = Program.g().scene();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					"stamp_annotations.txt")));
			String line = reader.readLine();
			while (line != null) {
				final String[] tokens = line.split(" ");
				String chordMethodSig = tokens[0];
				int atSymbolIndex = chordMethodSig.indexOf('@');
				String className = chordMethodSig.substring(atSymbolIndex + 1);
				if (scene.containsClass(className)) {
					SootClass klass = scene.getSootClass(className);
					String subsig = SootUtils.getSootSubsigFor(chordMethodSig
							.substring(0, atSymbolIndex));
					SootMethod meth = klass.getMethod(subsig);

					if (domM.indexOf(meth) >= 0) {
						String from = tokens[1];
						String to = tokens[2];

						boolean b1 = addLabel(from, srcLabels, sinkLabels);
						boolean b2 = addLabel(to, srcLabels, sinkLabels);

						char c = from.charAt(0);
						boolean src = (c == '$' || c == '!');
						boolean sink = to.charAt(0) == '!';
						if (b1 && b2) {
							System.out.println("Unsupported annotation type "
									+ line);
						} else if (b1 || b2) {
							worklist.add(meth);
							worklist.add(from);
							worklist.add(to);
						} else {
							addFlow(meth, from, to);
						}
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			throw new Error(e);
		}

		relArgArgTransfer.save();
		relArgRetTransfer.save();
		relArgArgFlow.save();
	}

	private boolean addLabel(String label, List<String> srcLabels,
			List<String> sinkLabels) {
		char c = label.charAt(0);
		if (c == '$') {
			srcLabels.add(label);
			return true;
		}
		if (c == '!') {
			sinkLabels.add(label);
			return true;
		}
		return false;
	}

	private void addFlow(SootMethod meth, String from, String to) // throws
																	// NumberFormatException
	{
		System.out.println("+++ " + meth + " " + from + " " + to);
		List<SootMethod> meths = SootUtils.overridingMethodsFor(meth);
		char from0 = from.charAt(0);
		if (from0 == '$' || from0 == '!') {
			if (to.equals("-1")) {
				for (SootMethod m : meths)
					relInLabelRet.add(from, m);
			} else {
				for (SootMethod m : meths)
					relInLabelArg.add(from, m, Integer.valueOf(to));
			}
		} else {
			Integer fromArgIndex = Integer.valueOf(from);
			char to0 = to.charAt(0);
			if (to0 == '!') {
				if (from.equals("-1")) {
					for (SootMethod m : meths)
						relOutLabelRet.add(to, m);
				} else {
					for (SootMethod m : meths)
						relOutLabelArg.add(to, m, fromArgIndex);
				}
			} else if (to0 == '?') {
				Integer toArgIndex = Integer.valueOf(to.substring(1));
				for (SootMethod m : meths)
					relArgArgFlow.add(m, fromArgIndex, toArgIndex);
			} else if (to.equals("-1")) {
				for (SootMethod m : meths)
					relArgRetTransfer.add(m, fromArgIndex);
			} else {
				Integer toArgIndex = Integer.valueOf(to);
				for (SootMethod m : meths)
					relArgArgTransfer.add(m, fromArgIndex, toArgIndex);
			}
		}
	}
}
