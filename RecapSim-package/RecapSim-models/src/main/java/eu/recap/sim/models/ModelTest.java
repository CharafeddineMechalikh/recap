/**
 * 
 */
package eu.recap.sim.models;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.google.protobuf.util.JsonFormat.Printer;

import eu.recap.sim.models.ApplicationModel.Application;
import eu.recap.sim.models.ApplicationModel.Application.Component;
import eu.recap.sim.models.Utilisation.Infrastructure;
import eu.recap.sim.models.Utilisation.Infrastructure.DataCentre;
import eu.recap.sim.models.Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.CoreUtil;


/**
 * @author Sergej Svorobej
 *
 */
public class ModelTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		UtilisationModelTest();
		ApplicationModelTest();
}
	
	public static void ApplicationModelTest() throws IOException {
		
		ApplicationModel.Application.Builder websiteApp = ApplicationModel.Application.newBuilder();
		websiteApp.setApplicationName("Website");
		websiteApp.setApplicationId("Web-1");
		
		
		ApplicationModel.Application.Component.Builder apacheFrontend = ApplicationModel.Application.Component.newBuilder();  
		apacheFrontend.setComponentName("Apache_v10");
		apacheFrontend.setComponentId("Apache_v10-1");
		apacheFrontend.setIsLoadbalanced(true);
		apacheFrontend.setComponentType(Component.ComponentTypes.WebService);
		
		ApplicationModel.Application.Component.Api.Builder getPage = ApplicationModel.Application.Component.Api.newBuilder();
		getPage.setApiName("get");
		getPage.setApiId("get-1");
		getPage.setMips(1_000);
		getPage.setIops(1_000);
		getPage.setDataToTransfer(10_000);
		getPage.setNextApiId("read-1");
		getPage.setNextComponentId("Mysql_v6-1");
		
		
		ApplicationModel.Application.Component.Api.Builder postFile = ApplicationModel.Application.Component.Api.newBuilder();
		postFile.setApiName("post");
		postFile.setApiId("post-1");
		postFile.setMips(1_000);
		postFile.setIops(10_000);
		postFile.setDataToTransfer(10_000);
		postFile.setNextComponentId("Mysql_v6-1");
		postFile.setNextApiId("write-1");
		
		// add above to the component repo
		//apacheFrontend.addApiRepository(getPage);
		//apacheFrontend.addApiRepository(postFile);
		
		
		ApplicationModel.Application.Component.Builder mysqlDb = ApplicationModel.Application.Component.newBuilder();  
		mysqlDb.setComponentName("Mysql_v6");
		mysqlDb.setComponentId("Mysql_v6-1");
		mysqlDb.setIsLoadbalanced(true);
		mysqlDb.setComponentType(Component.ComponentTypes.WebService);
		
		
		
		ApplicationModel.Application.Component.Api.Builder readQuery = ApplicationModel.Application.Component.Api.newBuilder();
		readQuery.setApiName("read");
		readQuery.setApiId("read-1");
		readQuery.setMips(1_000);
		readQuery.setIops(1_000);
		readQuery.setDataToTransfer(10_000);
		
		
		ApplicationModel.Application.Component.Api.Builder  writeQuery = ApplicationModel.Application.Component.Api.newBuilder();
		writeQuery.setApiName("write");
		writeQuery.setApiId("write-1");
		writeQuery.setMips(1_000);
		writeQuery.setIops(10_000);
		writeQuery.setDataToTransfer(10_000);

		// add above to the component repo
		//mysqlDb.addApiRepository(readQuery);
		//mysqlDb.addApiRepository(writeQuery);
		
		
		//add to the repo
		//websiteApp.addComponentRepository(mysqlDb);
		//websiteApp.addComponentRepository(apacheFrontend);
				
		


		String filename = "./webApp.sim";
		String filenameJson = "./appOutput.json";
		// Write the to disk.
	    FileOutputStream output = new FileOutputStream(filename);
	    Application appOutput = websiteApp.build();
	    appOutput.writeTo(output);
	    output.close();
	    System.out.println("Written binbary to file "+filename);
	    
	    //serialise to Json
	    Printer printer = com.google.protobuf.util.JsonFormat.printer().preservingProtoFieldNames();
	    Writer wr = new FileWriter("appOutput.json");
	    wr.flush();
	    printer.appendTo(appOutput, wr); 
	    wr.close();
	    
	    System.out.println("Written Json to file "+filenameJson);
		
		}
	
	
	public static void UtilisationModelTest() throws IOException {
	Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.CoreUtil.Builder core1util = Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.CoreUtil.newBuilder();
	core1util.setTime("10");
	core1util.setUtilisation(10);
	
	Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.Builder core1 = Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.newBuilder();
	core1.setName("core1");
	core1.setID("123");
	core1.addUtilisation(core1util);
	
	Utilisation.Infrastructure.DataCentre.Node.Cpu.Builder cpu = Utilisation.Infrastructure.DataCentre.Node.Cpu.newBuilder();
	cpu.setID("ID:123");
	cpu.setName("Name:node1");
	cpu.addCores(core1);
	
	
	Utilisation.Infrastructure.DataCentre.Node.Builder node = Utilisation.Infrastructure.DataCentre.Node.newBuilder();
	node.addCpu(cpu);
	
	
	Utilisation.Infrastructure.DataCentre.Builder dc = Utilisation.Infrastructure.DataCentre.newBuilder();
	dc.addNodes(node);
	dc.setName("dc1");
	
	Utilisation.Infrastructure.Builder infra = Utilisation.Infrastructure.newBuilder();
	infra.setExperimentId("experimentID:123");
	infra.addDataCentres(dc);

	String filename = "./nodeutil.sim";
	String filenameAdded = "./nodeutilAdd.sim";
	// Write the to disk.
    FileOutputStream output = new FileOutputStream(filename);
    Infrastructure ifraOutput = infra.build();
    ifraOutput.writeTo(output);
    output.close();
    System.out.println("Written to file "+filename);
    
    //Re-open file
    Utilisation.Infrastructure.Builder infraDisk = Utilisation.Infrastructure.newBuilder();
    infraDisk.mergeFrom(new FileInputStream(filenameAdded));
    System.out.println("Read from file "+filenameAdded+"\nName: "+infraDisk.getExperimentId());
    
    
    Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.CoreUtil.Builder core1utilt1 = Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.CoreUtil.newBuilder();
	core1utilt1.setTime("22");
	core1utilt1.setUtilisation(21);
	
	
	for (DataCentre.Builder dcDisk: infraDisk.getDataCentresBuilderList()){
		for (eu.recap.sim.models.Utilisation.Infrastructure.DataCentre.Node.Builder nodeDisk: dcDisk.getNodesBuilderList()){
			for ( eu.recap.sim.models.Utilisation.Infrastructure.DataCentre.Node.Cpu.Builder cpuDisk: nodeDisk.getCpuBuilderList()){
				for ( eu.recap.sim.models.Utilisation.Infrastructure.DataCentre.Node.Cpu.Core.Builder coreDisk: cpuDisk.getCoresBuilderList()){
					coreDisk.addUtilisation(core1utilt1);
					for (  CoreUtil utilDisk: coreDisk.getUtilisationList()){
					
						System.out.println("Time: "+utilDisk.getTime()+" Utilisation: "+utilDisk.getUtilisation());
					}
				}
				
			}
			
		}
		
	}
	
	// Write the to disk again.
	FileOutputStream outputCore = new FileOutputStream(filenameAdded);
	infraDisk.build().writeTo(outputCore);
	outputCore.close();
    //serialise to Json
    Printer printer = com.google.protobuf.util.JsonFormat.printer();
    Writer wr = new FileWriter("ifraOutput.json");
        
    printer.appendTo(infraDisk, wr); 
    wr.close();
    
    System.out.println("Written to file "+filenameAdded);
	
	}
	
	
}
