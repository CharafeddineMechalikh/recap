/**
 * 
 */
package eu.recap.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.CloudSimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import eu.recap.sim.helpers.Log;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventInfo;

import eu.recap.sim.cloudsim.RecapDatacenterBroker;
import eu.recap.sim.cloudsim.RecapVmAllocationPolicy;
import eu.recap.sim.cloudsim.cloudlet.RecapCloudlet;
import eu.recap.sim.cloudsim.cloudlet.IRecapCloudlet;
import eu.recap.sim.cloudsim.host.IRecapHost;
import eu.recap.sim.cloudsim.host.RecapHost;
import eu.recap.sim.cloudsim.vm.IRecapVe;
import eu.recap.sim.cloudsim.vm.RecapVe;
import eu.recap.sim.experiments.ExperimentHelpers;
import eu.recap.sim.helpers.ModelHelpers;
import eu.recap.sim.helpers.RecapCloudletsTableBuilder;
import eu.recap.sim.models.ApplicationModel.Application;

import eu.recap.sim.models.ApplicationModel.ApplicationLandscape;
import eu.recap.sim.models.WorkloadModel.Device;
import eu.recap.sim.models.WorkloadModel.Request;
import eu.recap.sim.models.WorkloadModel.Workload;
import eu.recap.sim.models.ApplicationModel.Application.Component;
import eu.recap.sim.models.ApplicationModel.Application.Component.Api;
import eu.recap.sim.models.ExperimentModel.Experiment;
import eu.recap.sim.models.InfrastructureModel.Infrastructure;
import eu.recap.sim.models.InfrastructureModel.Link;
import eu.recap.sim.models.InfrastructureModel.Node;
import eu.recap.sim.models.InfrastructureModel.Node.CPU;
import eu.recap.sim.models.InfrastructureModel.Node.Core;
import eu.recap.sim.models.InfrastructureModel.Node.Memory;
import eu.recap.sim.models.InfrastructureModel.Node.Storage;
import eu.recap.sim.models.InfrastructureModel.ResourceSite;

/**
 * The general class where RECAP simulation is started using RECAP based models
 * and configurations
 * 
 * @author Sergej Svorobej
 *
 */
public class RecapSim implements IRecapSim {
	private final CloudSim simulation;
	private List<IRecapVe> veList;
	private List<IRecapCloudlet> cloudletList;
	private List<IRecapCloudlet> onTheFlycloudletList;
	DatacenterBroker broker0;
	private List<Datacenter> datacenterList;
	private List<IRecapHost> hostList;
	private HashMap<Long, Boolean> finishedCloudlets;
					//linkID  list of cloudlets that are still being transferred
	private HashMap<String, List<IRecapCloudlet>> activeLinkCloudlets;
	private Infrastructure rim; 
	private ApplicationLandscape ram; 
	private Workload rwm;
	private Experiment config;

	public RecapSim() {
		Log.printLine("Starting RecapSim...");
		this.simulation = new CloudSim();
		//simulation.addOnClockTickListener(this::createNewCloudlets);
		//simulation.addOnEventProcessingListener(listener)
		// InitiateLists
		this.veList = new ArrayList<>();
		this.cloudletList = new ArrayList<>();
		this.onTheFlycloudletList = new ArrayList<>();
		this.datacenterList = new ArrayList<>();
		this.hostList = new ArrayList<>();
		this.activeLinkCloudlets = new HashMap<String, List<IRecapCloudlet>>();
		//EventListener<EventInfo> listener
		//this.simulation.addOnClockTickListener(this::onSimTimeAdvanceListener);
		
		/*
		 * Creates a Broker accountable for submission of VMs and Cloudlets on
		 * behalf of a given cloud user (customer).
		 */
		//CHAGE
		broker0 = new RecapDatacenterBroker(simulation);
		//broker0 = new DatacenterBrokerSimple(simulation);
		// bugfix list
		finishedCloudlets = new HashMap<Long, Boolean>();

	}

	
	@Override
	public String StartSimulation(Experiment experiment) {
		long conversionStartTime = System.currentTimeMillis();
		System.out.println("Starting model conversion...");
		this.rim =experiment.getInfrastructure();
		this.ram =experiment.getApplicationLandscape();
		this.rwm=experiment.getWorkload();
		this.config=experiment;
		
		// Generate the simulation ID from the experiment name and the time
		// stamp
		String simulationID = config.getName() + "_" + (System.currentTimeMillis() / 1000L);

		/**
		 * Create Infrastructure
		 */

		// bw the Bandwidth (BW) capacity in Megabits/s
		// TO-DO: see how this needed for node
		//int bw = rim.getLinksList().get(0).getBandwith();
		int nodeBw = 10_000; //100 Mbps = 12.5 MB/s
		int veBw = 100;
		for (ResourceSite site : rim.getSitesList()) {
			// storing all the dc hosts temporary
			List<IRecapHost> siteHostList = new ArrayList<>();

			// generating hosts
			for (Node node : site.getNodesList()) {
				// get PEs
				List<Pe> pesList = new ArrayList<>(); // List of CPU cores
				for (CPU cpu : node.getProcessingUnitsList()) {
					for (Core core : cpu.getCpuCoresList()) {
						pesList.add(new PeSimple(cpu.getFrequency(), new PeProvisionerSimple()));
					}
				}

				// get memory
				int totalNodeMemory = 0;
				for (Memory memory : node.getMemoryUnitsList()) {
					totalNodeMemory = totalNodeMemory + memory.getCapacity();
				}

				// get memory
				int totalNodeStorage = 0;
				for (Storage storage : node.getStorageUnitsList()) {
					totalNodeStorage = totalNodeStorage + storage.getSize();
				}

				IRecapHost host = (IRecapHost) new RecapHost(totalNodeMemory, nodeBw, totalNodeStorage, pesList)
						.setRamProvisioner(new ResourceProvisionerSimple())
						.setBwProvisioner(new ResourceProvisionerSimple()).setVmScheduler(new VmSchedulerTimeShared());
				
				host.setRecapNodeId(node.getId());
				host.setRecapNodeName(node.getName());
				host.setRecapResourceSiteId(site.getId());
				host.setRecapResourceSiteName(site.getName());

				// add to site host list
				siteHostList.add(host);

			}

			
			Datacenter datacenter = new DatacenterSimple(simulation, siteHostList, new RecapVmAllocationPolicy());
			DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple(datacenter);
			datacenter.setName(site.getName());
			//TO-DO: extend datacentre class to mach with site IDs
			//datacenter.setId(1);
			
			// adding to lists
			this.datacenterList.add(datacenter);
			this.hostList.addAll(siteHostList);
		} // end of Infrastructure creation

		/**
		 * Create VEs and deploy applications
		 * 
		 */

		// assign application components to VEs in order
		for (Application application : ram.getApplicationsList()) {
			for (Component component : application.getComponentsList()) {

				/*
				 * Creates VEs
				 */
				IRecapHost host = ExperimentHelpers.GetHostByIdFromList(component.getDeployment().getNodeId(),
						this.hostList);
				
				//TO-DO: in future we will implement placement policy to take care of this
				if(host == null){
					Log.printLine(
							"Error: No component deployment found for component #"+component.getComponentName()+". Strict deployment for now");
					System.exit(1);
					
				}

				
				component.getDeployment().getNodeId();
				

				RecapVe ve = new RecapVe(veList.size(), broker0, host.getPeList().get(0).getCapacity(),
						component.getFlavour().getCores(), component.getFlavour().getMemory(), veBw,
						component.getFlavour().getStorage(), "xen", new CloudletSchedulerTimeShared());
				
				//setting bandwith to 0 because we dont calculate bandwith between VMs only between sites
				ve.setBw(veBw);
				ve.setHost(host);
				ve.setApplicationID(application.getApplicationId());
				ve.setApplicationComponentID(component.getComponentId());
				this.veList.add(ve);

				Log.printFormattedLine("ApplicationID:" + application.getApplicationId() + " Component:"
						+ component.getComponentId() + " Placed on VE:" + ve.getId());

			}

		}

		/*
		 * Create initial requests for the submitted applications
		 * 
		 */

		for (Device device : rwm.getDevicesList()) {
			for (Request request : device.getRequestsList()) {
				for (Application application : ram.getApplicationsList()) {
					// check that app matches
					if (application.getApplicationId().equals(request.getApplicationId())) {

						for (Component component : application.getComponentsList()) {
							// check that component matches
							if (component.getComponentId().equals(request.getComponentId())) {

								for (Api api : component.getApisList()) {
									// check that api matches
									if (api.getApiId().equals(request.getApiId())) {
										int mi = api.getMips();
										int io = api.getIops();
										long outputFileSize = api.getDataToTransfer();

										long cloudletDelay = request.getTime();
										long inputFileSize = request.getDataToTransfer();
										IRecapVe requestVe = getMatchingVeId(application.getApplicationId(),component.getComponentId());
										IRecapCloudlet cl = createCloudlet(
												requestVe,
												mi, inputFileSize, outputFileSize, io, cloudletDelay,
												application.getApplicationId(), component.getComponentId(),
												api.getApiId());

										// System.out.println(application.getApplicationId()+component.getComponentId()+api.getApiId());
										cloudletList.add(cl);
									}
								}
							}
						}
					}
				}
			}
		}

		broker0.submitVmList(veList);
		broker0.submitCloudletList(cloudletList);

		System.out.println("Model cloudsim model creation took: "+(System.currentTimeMillis()-conversionStartTime)+"ms");

		System.out.println("Simulation started...");
		long startSimTime = System.currentTimeMillis();
		
		/* Starts the simulation and waits all cloudlets to be executed. */
		simulation.start();
		System.out.println("Simulation took: "+(System.currentTimeMillis()-startSimTime)+"ms real time for "+simulation.clock()+"s simtime.");
		
		
		/*
		 * Prints results when the simulation is over (you can use your own code
		 * here to print what you want from this cloudlet list)
		 */
		
		List<RecapCloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
		new RecapCloudletsTableBuilder(finishedCloudlets).build();


		return simulationID;
	}

	private IRecapVe getMatchingVeId(String applicationId, String componentId){
		
		// select matching VE where to send 1st
		// task
		IRecapVe veMatch = null;
		int matchingVEfound = 0;
		for (IRecapVe ve : veList) {
			if (ve.getApplicationID().equals(applicationId) && ve
					.getApplicationComponentID().equals(componentId)) {
				veMatch = ve;
				matchingVEfound++;
			}
		}

		// WARNING: when unable to match a VE
		// with the task
		if (matchingVEfound > 1) {
			Log.printLine(
					"Error: More than one VE found with the same application and component ID, stopping simulation");
			System.exit(1);
		} else if (matchingVEfound == 0) {
			Log.printLine(
					"Error: No VE found with the same application and component ID,stopping simulation");
			System.exit(1);
		}
		
		return veMatch;
	}
	
	private IRecapCloudlet createCloudlet(Vm vm, long mi, long inputFileSize, long outputFileSize, long io,
			double submissionDelay, String applicationId, String componentId, String apiId) {
		// final long length = 10000; //in Million Structions (MI)
		// final long fileSize = 300; //Size (in bytes) before execution
		// final long outputSize = 300; //Size (in bytes) after execution
		final int numberOfCpuCores = (int) vm.getNumberOfPes(); // cloudlet will
																// use all the
																// VM's CPU
																// cores

		// Defines how CPU, RAM and Bandwidth resources are used
		// Sets the same utilization model for all these resources.
		UtilizationModel utilization = new UtilizationModelFull();

		IRecapCloudlet recapCloudlet = (IRecapCloudlet) new RecapCloudlet(cloudletList.size(), mi, numberOfCpuCores)
				.setFileSize(inputFileSize).setOutputSize(outputFileSize).setUtilizationModel(utilization).setVm(vm)
				.addOnFinishListener(this::onCloudletFinishListener);

		recapCloudlet.setSubmissionDelay(submissionDelay);
		recapCloudlet.setApplicationId(applicationId);
		recapCloudlet.setApplicationComponentId(componentId);
		recapCloudlet.setApiId(apiId);

		return recapCloudlet;
	}
	
	private void onSimTimeAdvanceListener(EventInfo eventInfo) {
		System.out.println("Second: "+eventInfo.getTime());
		
	}
	

	
	private void onCloudletFinishListener(CloudletVmEventInfo eventInfo) {

		// 0. Bug workaround: check if it is a second execution of the listener
		// If the entry already here then we skip it
		if (this.finishedCloudlets.containsKey(eventInfo.getCloudlet().getId())) {

			// Log.printFormattedLine("\n#Bugfix#All following entries already
			// executed for CloudletId:"+eventInfo.getCloudlet().getId()+"\n");

		} else {
			IRecapCloudlet finishedRecapCloudlet = (IRecapCloudlet) eventInfo.getCloudlet();
			RecapVe currentVe = (RecapVe) eventInfo.getVm();

			Log.printFormattedLine("Finished ApplicationId:" + finishedRecapCloudlet.getApplicationId() + " ComponentId:"
					+ finishedRecapCloudlet.getApplicationComponentId() + " apiTaskId: " + finishedRecapCloudlet.getApiId());

			Log.printFormattedLine("\n#EventListener: Cloudlet %d finished running at Vm %d at time %.2f",
					finishedRecapCloudlet.getId(), currentVe.getId(), eventInfo.getTime());

			// 1. Check if the Request that has triggered the VM has next
			// cloudlet to execute
			Api currentApi = ModelHelpers.getApiTask(this.ram.getApplicationsList(), finishedRecapCloudlet.getApplicationId(),
					finishedRecapCloudlet.getApplicationComponentId(), finishedRecapCloudlet.getApiId());

			// check if we have a chain of application compoents attached
			if (!currentApi.getNextApiId().equals("") && !currentApi.getNextComponentId().equals("")) {
				Log.printFormattedLine("Found next component ID:" + currentApi.getNextComponentId());
				Api nextApi = ModelHelpers.getApiTask(this.ram.getApplicationsList(), finishedRecapCloudlet.getApplicationId(),
						currentApi.getNextComponentId(), currentApi.getNextApiId());
				
				
				// 2. Create cloudlet using api specs
				double delay = 0.0;
				IRecapVe targetVe = getMatchingVeId(finishedRecapCloudlet.getApplicationId(),currentApi.getNextComponentId());
				//2b. create cloudlet
				IRecapCloudlet newRecapCloudlet = createCloudlet(targetVe, nextApi.getMips(), nextApi.getDataToTransfer(),
						nextApi.getDataToTransfer(), nextApi.getIops(), delay, finishedRecapCloudlet.getApplicationId(),
						currentApi.getNextComponentId(), nextApi.getApiId());
				
				newRecapCloudlet.setBwUpdateTime(simulation.clock());
				
				//2a. calculate delay based on the connection
				//TO-DO: Update transfer remaining speeds when a cloudlet finished transferring through a link 
				//is cloudlet being sent between DC sites?
				if (targetVe.getHost().getDatacenter().getId()!=currentVe.getHost().getDatacenter().getId()){
					//if so calculate link BW demand
					Link link = ModelHelpers.getNetworkLink(rim, currentVe.getHost().getDatacenter().getName(), targetVe.getHost().getDatacenter().getName());
					int linkBw = link.getBandwith();
					
					//get current cloudlets on the link
					List<IRecapCloudlet> listActivecloudlets;
					if(activeLinkCloudlets.containsKey(link.getId())){
				
						listActivecloudlets = activeLinkCloudlets.get(link.getId());
						
						//clean cloudlets list that are being processed already. Cloudlets that are not in status instantiated are removed from the list
						for(IRecapCloudlet cl:listActivecloudlets){
							if(!cl.getStatus().equals(org.cloudbus.cloudsim.cloudlets.Cloudlet.Status.INSTANTIATED)){
								listActivecloudlets.remove(cl);
							}
						}
						listActivecloudlets.add(newRecapCloudlet);
						//update
						activeLinkCloudlets.put(link.getId(), listActivecloudlets);
						
					}else{
						//create list and add the cloudlet
						listActivecloudlets = new ArrayList<IRecapCloudlet>();
						listActivecloudlets.add(newRecapCloudlet);
						activeLinkCloudlets.put(link.getId(), listActivecloudlets);
					}
					
					//assume bandwidth divided equally
					double availableBandwithSliceForCloudlet = linkBw/listActivecloudlets.size();
					
					//bandwith speed is in Megabits per second where file size is in Bytes, so we convert Megabits to Bytes by multiplying by 125000
					//calculate delay         Megabits                         Bytes
					double  ByteperSecond = 125000*availableBandwithSliceForCloudlet;
					delay = ByteperSecond/newRecapCloudlet.getFileSize();
					newRecapCloudlet.setSubmissionDelay(delay);

					//Update the delay for the rest of cloudlets in the list based on more cloudlets in the link
					//check if more cloudlets in the list than the new one
					if(listActivecloudlets.size()>1){
						//calculate how much of data was already transferred in the previous time slice
						//update with new delays for the remainder of the data to be transferred
						for(IRecapCloudlet cl: listActivecloudlets){
							//all except the new one
							if(cl.getId()!=newRecapCloudlet.getId()){
								double timePassedInDataTransfer = simulation.clock() - cl.getBwUpdateTime();
								//calculate already how much was transferred
								double availableBandwithSliceBeforeNewVM = linkBw/(listActivecloudlets.size()-1);
								double transferredBytes = cl.getFileSize()- (timePassedInDataTransfer*(availableBandwithSliceBeforeNewVM*125000));
								//new delay with new slice byteper second
								double newDelay = ByteperSecond/ (cl.getFileSize() -transferredBytes);
								cl.setSubmissionDelay(newDelay);
								//set the bytes that were transferred in the past time and time when that was updated before the new time delay estimation
								cl.setTransferredBytes(transferredBytes);
								cl.setBwUpdateTime(simulation.clock());
 								
							}
						}
						
					}


					
					
				}

				// need to add cloudlet to the list to have a consistent ID
				cloudletList.add(newRecapCloudlet);
				onTheFlycloudletList.add(newRecapCloudlet);
				Log.printFormattedLine("Submitting Cloudlet ID: " + newRecapCloudlet.getId());
				this.broker0.submitCloudlet(newRecapCloudlet);
				//System.out.println("#Submittedcl"+newRecapCloudlet.getStatus());

			}
			//System.out.println("#FinishedCL: "+eventInfo.getCloudlet().getStatus());

			// add the key to the check list
			finishedCloudlets.put(eventInfo.getCloudlet().getId(), true);
			
			this.broker0.getCloudletWaitingList();

		}

	}
	
	@Override
	public eu.recap.sim.IRecapSim.SimulationStatus SimulationStatus(String simulationId) {

		return SimulationStatus.FINISHED;

	}

}
