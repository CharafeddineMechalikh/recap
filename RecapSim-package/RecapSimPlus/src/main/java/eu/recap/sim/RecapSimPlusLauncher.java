package eu.recap.sim;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import eu.recap.sim.helpers.Log;

import eu.recap.sim.cloudsim.cloudlet.RecapCloudlet;
import eu.recap.sim.experiments.ExperimentHelpers;
import eu.recap.sim.helpers.RecapAscii;
import eu.recap.sim.helpers.RecapCloudletsTableBuilder;
import eu.recap.sim.models.ApplicationModel.ApplicationLandscape;
import eu.recap.sim.models.WorkloadModel.Workload;
import eu.recap.sim.models.ExperimentModel.Experiment;
import eu.recap.sim.models.InfrastructureModel.Infrastructure;

/**
 * Main class that launches the simulation from command prompt
 * 
 * @author Sergej Svorobej
 *
 */
public class RecapSimPlusLauncher {

	/**
	 * Main class that starts the simulation 
	 * 
	 * 
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		//####
		//TO-DO: load files via file names
		Options modelsLoad = new Options();

		Option infrastructureModelFilename = new Option("i", "infrastructure",true, "Infrastructure model file path");
		infrastructureModelFilename.setRequired(true);
		modelsLoad.addOption(infrastructureModelFilename);
		//####
		
		HelpFormatter formatter = new HelpFormatter();
		
		Options test = new Options();
		test.addOption("h", "help",false, "This is help");
		Option testOpt = new Option("t", "test",false, "Test mode");
		test.addOption(testOpt);
		
		Option tsites = new Option("s", "sites",true, "Number of sites");
		tsites.setRequired(true);
		test.addOption(tsites);
		
		Option tnodes = new Option("n", "nodes",true, "Number of nodes per site");
		tnodes.setRequired(true);
		test.addOption(tnodes);
		
		Option tapps = new Option("a", "app",true, "Number of applications deployed");
		tapps.setRequired(true);
		test.addOption(tapps);
		
		Option tcomponent = new Option("c", "component",true, "Number of component per application");
		tcomponent.setRequired(true);
		test.addOption(tcomponent);
		
		Option tdevice = new Option("d", "device",true, "Number of devices/users");
		tdevice.setRequired(true);
		test.addOption(tdevice);
		
		Option trequests = new Option("r", "request",true, "Number of requests per device ");
		trequests.setRequired(true);
		test.addOption(trequests);
		
		Option tdelay = new Option("l", "delay",true, "Launch delay in seconds to start the monitors");
		tdelay.setRequired(true);
		test.addOption(tdelay);
		
		Option tlog = new Option("o", "log",true, "To enable log set 1 to disable set 0");
		tlog.setRequired(true);
		test.addOption(tlog);
		
		
	
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(test, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			new RecapAscii();
			formatter.printHelp("RecapSim Launcher", test);

			System.exit(1);
			return;
		}


		if (cmd.hasOption("help")) {
			
			new RecapAscii();
			formatter.printHelp("RecapSim Launcher", test);
		
		} if(cmd.hasOption("test")) {
 
			//generator inputs
			int nSites = Integer.parseInt(cmd.getOptionValue("s"));
			int nNodesPerSite =Integer.parseInt(cmd.getOptionValue("n"));
			int nApps= Integer.parseInt(cmd.getOptionValue("a"));
			int nComponentsPerApp=Integer.parseInt(cmd.getOptionValue("c"));
			int nDevices =Integer.parseInt(cmd.getOptionValue("d")); //users
			int nRequestsPerDevice=Integer.parseInt(cmd.getOptionValue("r"));
			
			//Load experiment from somewhere
			
			//Disable logs
			if(Integer.parseInt(cmd.getOptionValue("o"))==0){
				Log.disable();	
			}
			
			
			//delay to attach monitors
			TimeUnit.SECONDS.sleep(Integer.parseInt(cmd.getOptionValue("l")));
						
			System.out.println("Starting model generation"
					+"\n############"
					+"\nnSites:"+nSites
					+"\nnNodesPerSite:"+nNodesPerSite
					+"\nnApps:"+nApps
					+"\nnComponentsPerApp:"+nComponentsPerApp
					+"\nnDevices:"+nDevices
					+"\nnRequestsPerDevice:"+nRequestsPerDevice
					+"\n############"
					);
			
			long startTime = System.currentTimeMillis();
			//Generate the models
			Infrastructure rim = ExperimentHelpers.GenerateInfrastructure("Test-Infrastructure", nSites, nNodesPerSite);
			ApplicationLandscape ram = ExperimentHelpers.GenerateApplication(nApps,nComponentsPerApp, rim);
			Workload rwm = ExperimentHelpers.GenerateDeviceBehavior(nDevices,nRequestsPerDevice,ram);
			Experiment config = ExperimentHelpers.GenerateConfiguration("Test-Config", 2,rim,ram,rwm);//duration only will work if simulation runs longer
			
			long stopTime = System.currentTimeMillis();
			
			System.out.println("Model generation took: "+(stopTime-startTime)+"ms");
						
			//run the example
			RecapSim recapExperiment = new RecapSim();

			String simulationId = recapExperiment.StartSimulation(config);
			
			System.out.println("Simulation is:"+ recapExperiment.SimulationStatus(simulationId));
			
			/*
			 * Prints results when the simulation is over (you can use your own code
			 * here to print what you want from this cloudlet list)
			 */
			List<RecapCloudlet> finishedCloudlets = recapExperiment.broker0.getCloudletFinishedList();
			new RecapCloudletsTableBuilder(finishedCloudlets).build();
			
			
		}
		//TO-DO: Load experiments from files
		System.exit(1);

		System.exit(0);
				 

	}
	
//	//pass this listener to every cloudlet created to notify it when cloudlet is done
//    public static void onCloudletFinishListener(CloudletVmEventInfo eventInfo) {
//        Log.printFormattedLine(
//                "\n\t#EventListener: Cloudlet %d finished running at Vm %d at time %.2f\n",
//                eventInfo.getCloudlet().getId(), eventInfo.getVm().getId(), eventInfo.getTime());
//    }
	


}
