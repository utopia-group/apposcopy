package com.apposcopy.analyses;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import stamp.harnessgen.UTF8ToAnsiUtils;
import stamp.harnessgen.PersonalNamespaceContext;


import com.apposcopy.model.XmlNode;

/*
* reads AndroidManifest.xml to find out several info about the app
* @author Saswat Anand
*/
public class ParseManifest
{
	private String pkgName;

	void process(File manifestFile, Set<String> activities, Set<String> others)
	{
		try{
			File tmpFile = File.createTempFile("stamp_android_manifest", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{manifestFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			manifestFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(manifestFile);
			
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new PersonalNamespaceContext());
			
			//find package name
			Node node = (Node)
				xpath.evaluate("/manifest", document, XPathConstants.NODE);
			pkgName = node.getAttributes().getNamedItem("package").getNodeValue();
			
			findComponents(xpath, document, activities, "activity");

			for(String compType : new String[]{"service", "receiver"}){
				findComponents(xpath, document, others, compType);
			}
			
			node = (Node)
				xpath.evaluate("/manifest/application", document, XPathConstants.NODE);

			//backup agent
			Node backupAgent = node.getAttributes().getNamedItem("android:backupAgent");
			if(backupAgent != null)
				others.add(fixName(backupAgent.getNodeValue()));
			
			//application class
			Node application = node.getAttributes().getNamedItem("android:name");
			if(application != null)
				others.add(fixName(application.getNodeValue()));

		}catch(Exception e){
			throw new Error(e);
		}
	}

	private String fixName(String comp)
	{
		if(comp.startsWith("."))
			comp = pkgName + comp;
		else if(comp.indexOf('.') < 0)
			comp = pkgName + "." + comp;
		return comp;
	}


	private void findComponents(XPath xpath, Document document, Set<String> comps, String componentType) throws Exception
	{
		NodeList nodes = (NodeList)
			xpath.evaluate("/manifest/application/"+componentType, document, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			NamedNodeMap nnm = node.getAttributes();
			String name = null;
			for(int j = 0; j < nnm.getLength(); j++){
				Node n = nnm.item(j);
				if(n.getNodeName().equals("android:name")){
					name = n.getNodeValue();
					break;
				}
				//System.out.println(n.getNodeName() + " " + );
			}			
			assert name != null : node.getNodeName();
			comps.add(fixName(name));
		}
	}

	private void findNodes(XPath xpath, Document document, Map<String, XmlNode> comps, String componentType) throws Exception
	{
		NodeList nodes = (NodeList)
			xpath.evaluate("/manifest/application/"+componentType, document, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			NamedNodeMap nnm = node.getAttributes();
			String name = null;
			String permission = null;
            XmlNode comptNode = new XmlNode();
			for(int j = 0; j < nnm.getLength(); j++){
				Node n = nnm.item(j);
				if(n.getNodeName().equals("android:name")){
					name = n.getNodeValue();
                    comptNode.setName(name);
				}
            	if(n.getNodeName().equals("android:permission")){
					permission = n.getNodeValue();
                    comptNode.setPermission(permission);
				}
			}			
            comptNode.setType(componentType);
			assert name != null : node.getNodeName();

            //for intentfilter.
   /*         NodeList filters = node.getChildNodes();
            for(int k=0; k < filters.getLength(); k++){
                Node fnode = filters.item(k);
                NodeList actions = fnode.getChildNodes();
                for(int j=0; j < actions.getLength(); j++){
                    Node aNode = actions.item(j);
			        NamedNodeMap anm = aNode.getAttributes();
                    if(anm == null) continue;

                    for(int q = 0; q < anm.getLength(); q++){
                        Node v = anm.item(j);
                        if(v == null) continue;
                        if(v.getNodeName().equals("android:name")){
                            filter += v.getNodeValue();
                            System.out.println("myfilter...." + filter);
                        }
                    }

                }

            }

            comptNode.setIntentFilter(filter);*/
			comps.put(fixName(name), comptNode);
		}

	}

    public String getPkgName() {
        return pkgName;
    }

	public void extractComponents(File manifestFile, Map<String, XmlNode> activities)
	{
		try{
			File tmpFile = File.createTempFile("stamp_android_manifest", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{manifestFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			manifestFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(manifestFile);
			
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new PersonalNamespaceContext());
			
			//find package name
			Node node = (Node)
				xpath.evaluate("/manifest", document, XPathConstants.NODE);
			pkgName = node.getAttributes().getNamedItem("package").getNodeValue();
			
			findNodes(xpath, document, activities, "activity");
		    findNodes(xpath, document, activities, "service");
		    findNodes(xpath, document, activities, "receiver");

            ///add by yu
            NodeList mynodes = (NodeList)
                xpath.evaluate("/manifest/application/activity/intent-filter/action", document, XPathConstants.NODESET);
            for (int i = 0; i < mynodes.getLength(); i++) {
                Node mynode = mynodes.item(i);
                NamedNodeMap nnm = mynode.getAttributes();
                String name = null;
                for(int j = 0; j < nnm.getLength(); j++){
                    Node n = nnm.item(j);
                    if(n.getNodeName().equals("android:name")){
                        name = n.getNodeValue();
                        Node mainNode = mynode.getParentNode().getParentNode();
                        NamedNodeMap nnm1 = mainNode.getAttributes();
                        String actname = null;
                        for(int k = 0; k < nnm1.getLength(); k++){
                            Node n1 = nnm1.item(k);
                            if(n1.getNodeName().equals("android:name")) {
                                actname = n1.getNodeValue();
                                //if (activities.get(fixName(actname)) != null)
                                if ("android.intent.action.MAIN".equals(name))
                                    activities.get(fixName(actname)).setMain(true);

                                activities.get(fixName(actname)).addAction(name);
                                break;
                            }
                        }
                    }
                }			
            }
            //end

            //append intentfilter for receiver.
            NodeList fnodes = (NodeList)
                xpath.evaluate("/manifest/application/receiver/intent-filter/action", document, XPathConstants.NODESET);

            for (int i = 0; i < fnodes.getLength(); i++) {
                Node mynode = fnodes.item(i);
                NamedNodeMap nnm = mynode.getAttributes();
                String name = null;
                for(int j = 0; j < nnm.getLength(); j++){
                    Node n = nnm.item(j);
                    if(n.getNodeName().equals("android:name")){
                        name = n.getNodeValue();

                        //get component node.
                        Node mainNode = mynode.getParentNode().getParentNode();
                        NamedNodeMap nnm1 = mainNode.getAttributes();
                        String actname = null;
                        for(int k = 0; k < nnm1.getLength(); k++){
                            Node n1 = nnm1.item(k);
                            if(n1.getNodeName().equals("android:name")) {
                                actname = n1.getNodeValue();
                                if (activities.get(fixName(actname)) != null)
                                    activities.get(fixName(actname)).setIntentFilter(name);
                                //FIXME only one filter.
                                activities.get(fixName(actname)).addAction(name);
                                break;
                            }
                        }
                        //get priority from intent filter, if any.
                        Node filterNode = mynode.getParentNode();
                        NamedNodeMap filterMap = filterNode.getAttributes();
                        for(int k = 0; k < filterMap.getLength(); k++){
                            Node fn = filterMap.item(k);
                            if(fn.getNodeName().equals("android:priority")) {
                                String priority = fn.getNodeValue();
                                assert(actname != null);
                                activities.get(fixName(actname)).addFilter(priority);
                                break;
                            }
                        }

                    }
                    //System.out.println(n.getNodeName() + " " + );
                }			
            }


		}catch(Exception e){
			throw new Error(e);
		}
	}



	/*
	public static void main(String[] args) throws Exception
	{
		File androidManifestFile = new File(args[0]);
		String classPath = args[1];
		String androidJar = args[2];

		List<JarFile> jars = new ArrayList();
		for(String cp : classPath.split(":")){
			if(!(new File(cp).exists()))
				System.out.println("WARNING: "+cp +" does not exists!");
			else
				jars.add(new JarFile(cp));
		}

		App app = new App(androidManifestFile, classPath, androidJar);
		System.out.println(app);
		}*/
}
