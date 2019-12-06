/**
 * 
 */
package eu.recap.tests;

import static org.junit.Assert.*;

import java.io.FileWriter;
import java.io.Writer;

import org.junit.Rule;
import org.junit.Test;

import com.google.protobuf.util.JsonFormat.Printer;

import eu.recap.sim.RecapSim;
import eu.recap.sim.experiments.ExperimentHelpers;
import eu.recap.sim.models.ApplicationModel.ApplicationLandscape;
import eu.recap.sim.models.WorkloadModel.Workload;
import eu.recap.sim.models.ExperimentModel.Experiment;
import eu.recap.sim.models.InfrastructureModel.Infrastructure;

/**
 * @author Sergej Svorobej
 *
 */
public class SimpleTest {


	@Test
	public void GeneralTest() {
		
		int nSites = 2; 
		int nNodesPerSite = 1;
		int nApps = 2;
		int nComponentsPerApp =1;
		//eNodeB-c, eNode-U, MME, SGW-U, PGW-U 
		int nDevices = 4;
		int nRequestsPerDevice =1;
		int requestQtyControlPlane = 1;
		int requestQtyUserPlane = 1;
		
		
		long startTime = System.currentTimeMillis();
		//Generate the models
		Infrastructure rim = ExperimentHelpers.GenerateInfrastructure("Test-Infrastructure", nSites, nNodesPerSite);
		ApplicationLandscape ram = ExperimentHelpers.GenerateApplication(nApps,nComponentsPerApp, rim);
		Workload rwm = ExperimentHelpers.GenerateDeviceBehavior(nDevices,nRequestsPerDevice,ram);
		Experiment experiment = ExperimentHelpers.GenerateConfiguration("Test-Config", 2,rim,ram,rwm);//duration only will work if simulation runs longer
		
		
		long stopTime = System.currentTimeMillis();
		
		System.out.println("Model generation took: "+(stopTime-startTime)+"ms");
					
		//run the example
		RecapSim recapExperiment = new RecapSim();
		
		try{
		    //serialise to Json
			String filenameJson = "experimentOutput.json";
		    Printer printer = com.google.protobuf.util.JsonFormat.printer().preservingProtoFieldNames();
		    Writer wr = new FileWriter(filenameJson);
		    wr.flush();
		    printer.appendTo(experiment, wr); 
		    wr.close();
		    
		    System.out.println("Written Json to file "+filenameJson);
		    
			String simulationId = recapExperiment.StartSimulation(experiment);	
			System.out.println("Simulation is:"+ recapExperiment.SimulationStatus(simulationId));
		}catch (Exception e){
			e.printStackTrace();
			fail("Inside Simulation error.");
			
		}
		
		
	}

}
