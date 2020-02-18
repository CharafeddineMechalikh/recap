/**
 * 
 */
package eu.recap.sim.experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import eu.recap.sim.models.WorkloadModel.Device;
import eu.recap.sim.models.LocationModel.Location;
import eu.recap.sim.models.WorkloadModel.Request;
import eu.recap.sim.models.ApplicationModel.VeFlavour;
import eu.recap.sim.models.WorkloadModel.Workload;
import eu.recap.sim.models.ExperimentModel.Experiment;
import eu.recap.sim.models.InfrastructureModel.Infrastructure;
import eu.recap.sim.models.InfrastructureModel.Link;
import eu.recap.sim.models.InfrastructureModel.Node;
import eu.recap.sim.models.InfrastructureModel.ResourceSite;
import eu.recap.sim.models.InfrastructureModel.ResourceSite.SiteLevel;
import eu.recap.sim.models.InfrastructureModel.Node.CPU;
import eu.recap.sim.models.InfrastructureModel.Node.Core;
import eu.recap.sim.models.InfrastructureModel.Node.Memory;
import eu.recap.sim.models.InfrastructureModel.Node.Storage;
import eu.recap.sim.cloudsim.host.IRecapHost;
import eu.recap.sim.models.ApplicationModel.Application;
import eu.recap.sim.models.ApplicationModel.Application.Component;
import eu.recap.sim.models.ApplicationModel.Application.Component.Builder;
import eu.recap.sim.models.ApplicationModel.ApplicationLandscape;
import eu.recap.sim.models.ApplicationModel.Deployment;

/**
 * Class provides static methods to generate dummy data and help 
 * experiments
 * 
 * @author Sergej Svorobej
 *
 */

public class ExperimentHelpers {

/**
 *  Get host from the list by id	
 * @param hostId
 * @param hostList
 * @return
 */
	public static IRecapHost GetHostByIdFromList(String recapNodeId, List<IRecapHost> hostList) {
		for (IRecapHost host:hostList){
			
			if(recapNodeId.equals("")){
				return null;
			}
			
			if(host.getRecapNodeId().equals(recapNodeId)){
				return host;
			}
		}
		//no host found
		return null;
	}
	
	
	
	/**
	 * Creates test Infrastructure model for tieto usecase
	 * 
	 * @param name
	 * @param numberOfSites
	 * @param numberOfNodesPerSite
	 * @return the populated Infrastructure model
	 */
	public static Infrastructure GenerateLinkNovateInfrastructure(String name, int numberOfSites, int numberOfNodesPerSite) {
		final int cpuFrequency = 3000; //MIPS or 2.6 GHz
		final int cpuCores = 80; 
		final int ram = 2048_000; // host memory (MEGABYTE)
		final int hdd = 1000000_000; // host storage (MEGABYTE)
		final int bw = 10_000; // in 10Gbit/s
		
		Infrastructure.Builder infrastructure = Infrastructure.newBuilder();
		infrastructure.setName(name);
		
		//only one link where all sites are connected
		Link.Builder link = Link.newBuilder();
		link.setId("0");
		link.setBandwith(bw);
		
		//create sites
	    for(int i=0; i<numberOfSites; i++){
            
	    	ResourceSite.Builder site = ResourceSite.newBuilder();
	    	site.setName("Site_"+i);
	    	site.setId(i+"");
	    	
	    	Location.Builder geolocation = Location.newBuilder();
	    	geolocation.setLatitude(i);
	    	geolocation.setLongitude(i);
	    	site.setLocation(geolocation.build());
	    	site.setHierarchyLevel(SiteLevel.Edge);
	    	
	    	//create nodes
	    	for(int j=0; j<numberOfNodesPerSite; j++){
	    		
	    		Node.Builder node = Node.newBuilder();
	    		node.setName("Node_"+i+"_"+j);
	    		node.setId(i+"_"+j);

	    		
	    		CPU.Builder cpu = CPU.newBuilder();
	    		cpu.setName("Xeon_"+i+"_"+j);
	    		cpu.setId(i+"_"+j);
	    		cpu.setMake("Intel");
	    		cpu.setRating("12345");
	    		cpu.setFrequency(cpuFrequency);
	    		//create cores
	    		for(int e=0; e<cpuCores; e++){
	    			Core.Builder core = Core.newBuilder();
	    			core.setId(i+"_"+j+"_"+e);
	    			cpu.addCpuCores(core.build());
	    		}
	    		
	    		
	    		Memory.Builder memory = Memory.newBuilder();
	    		memory.setId(i+"_"+j);
	    		memory.setCapacity(ram);
	    		
	    		Storage.Builder storage = Storage.newBuilder();
	    		storage.setId(i+"_"+j);
	    		storage.setSize(hdd);
	    		
	    		
	    		//add resources to node
	    		node.addProcessingUnits(cpu.build());
	    		node.addMemoryUnits(memory.build());
	    		node.addStorageUnits(storage.build());
	    		
	    		//add node to site
	    		site.addNodes(node.build());
	    	}
	    	ResourceSite builtSite = site.build();
	    	//add sites to infrastructure
	    	infrastructure.addSites(builtSite);
	    	
	    	//add sites to link by id
	    	link.addConnectedSites(builtSite);
	    	
	    }
		
	    infrastructure.addLinks(link.build());
		return infrastructure.build();

	}

	
	
	/**
	 * Method to check if the device exists in the workload list 
	 * @param deviceId
	 * @param workload
	 * @return Device.Builder of device to create requests
	 */
	private static Device.Builder getDeviceIfDeviceExistsOrNew(String deviceId, Workload.Builder workload){
		
		//return device if it already exists
		for(Device.Builder device:workload.getDevicesBuilderList()){
			if (device.getDeviceId().equals(deviceId)){
				return device;
			}
		}
		//if does not exist create new one and return it 
		Device.Builder device = Device.newBuilder();
		device.setDeviceId(deviceId);
		device.setDeviceName(deviceId);
		
		return device;
	}
	
	/**
	 * Submit device location at the time of request and get the closest location of data centre where application
	 * is running 
	 * 
	 * @param latitude
	 * @param longitude
	 * @param rim
	 * @return
	 */
	private static String getApplicationIdByCoordinates(double latitude, double longitude, Infrastructure rim, ApplicationLandscape ram){
		double shortestDistance = -1;
		String closestApplicationId = "-1";
		
		
		for(ResourceSite site:rim.getSitesList()){
			double rimLatitude =  site.getLocation().getLatitude();
			double rimLongitude = site.getLocation().getLongitude();
			//root((latitude - rimLatitude)sq + (longitude-rimLongitude)sq)
			double distance = Math.sqrt((Math.pow(latitude - rimLatitude, 2) + Math.pow(longitude-rimLongitude, 2)));
			//set an initial shortest distance as first element
			if (shortestDistance<0){
				shortestDistance=distance;
			}
			//if new distance is shortest lookup application ID and set it alongside
			if(distance<=shortestDistance){
				shortestDistance = distance;
				
				//map application to the DC location
				for (Application application: ram.getApplicationsList()){
					//we assume each application is deployed entirely on the same DC
					if (site.getId().equals(application.getComponents(0).getDeployment().getSiteId())){
						closestApplicationId = application.getApplicationId();					
					}
					
				}
				
			}
			
			
			
		}
		
		return closestApplicationId;
	}
	
	/** returns difference between two dates in seconds
	 * @param startingDateTime
	 * @param currentDateTime
	 * @return
	 */
	public static int differenceInSeconds(LocalDateTime startingDateTime, LocalDateTime currentDateTime){
		
		int daysDifference = (currentDateTime.getDayOfMonth() - startingDateTime.getDayOfMonth())*86400;
		int hourDifference = (currentDateTime.getHour() - startingDateTime.getHour())*3600;
		int minutesDifference = (currentDateTime.getMinute() -startingDateTime.getMinute())*60;
		int secondDifference = currentDateTime.getSecond() - startingDateTime.getSecond();
		
		
		return daysDifference+hourDifference+minutesDifference+secondDifference;
	}
	
	/**
	 * Generates number of requests per device. Each request sent out at the time as the request number.
	 * Requests are sent always on the 1st component of each application in a round robin fashion. 
	 * 
	 * @param deviceQty
	 * @param requestQty
	 * @return
	 */
	public static Workload GenerateDeviceBehavior(int deviceQty, int requestQty, ApplicationLandscape ram) {

		List<String> appIds = new ArrayList<String>(ram.getApplicationsCount());
		
		for (Application app :ram.getApplicationsList()){
			
			appIds.add(app.getApplicationId());
			
		}
		
		int indexNmberOfApplications = appIds.size()-1;
		int indexNmberOfApplicationsCounter = 0;
		
		Workload.Builder workload = Workload.newBuilder();
		
		while (deviceQty != 0) {
			Device.Builder device = Device.newBuilder();
			device.setDeviceId(deviceQty + "");
			device.setDeviceId("Smartphone_" + deviceQty);
			int requestQtyCounter = requestQty;
			while (requestQtyCounter != 0) {
				Request.Builder request = Request.newBuilder();
				
				request.setApplicationId(appIds.get(indexNmberOfApplicationsCounter));
				//reset if we ran out of applications or advance counter
				if(indexNmberOfApplicationsCounter==indexNmberOfApplications){
					indexNmberOfApplicationsCounter =0;
				}else{
					indexNmberOfApplicationsCounter++;
				}
								
				request.setComponentId("1");
				request.setApiId("1");
				request.setTime(requestQtyCounter);
				request.setDataToTransfer(100);

				device.addRequests(request.build());
				requestQtyCounter--;
			}

			workload.addDevices(device.build());
			deviceQty--;
		}

		return workload.build();

	}

	private static void createNodes(int numberOfNodesPerSite, ResourceSite.Builder site){
	final int cpuFrequency = 2000;
	final int cpuCores = 8; 
	final int ram = 2048; // host memory (MEGABYTE)
	final int hdd = 1000000; // host storage (MEGABYTE)
	
	String i = site.getId();
	
	//create nodes
	for(int j=0; j<numberOfNodesPerSite; j++){
		
		Node.Builder node = Node.newBuilder();
		node.setName("Node_"+i+"_"+j);
		node.setId(i+"_"+j);
		
		CPU.Builder cpu = CPU.newBuilder();
		cpu.setName("Xeon_"+i+"_"+j);
		cpu.setId(i+"_"+j);
		cpu.setMake("Intel");
		cpu.setRating("12345");
		cpu.setFrequency(cpuFrequency);
		//create cores
		for(int e=0; e<cpuCores; e++){
			Core.Builder core = Core.newBuilder();
			core.setId(i+"_"+j+"_"+e);
			cpu.addCpuCores(core.build());
		}
		
		
		Memory.Builder memory = Memory.newBuilder();
		memory.setId(i+"_"+j);
		memory.setCapacity(ram);
		
		Storage.Builder storage = Storage.newBuilder();
		storage.setId(i+"_"+j);
		storage.setSize(hdd);
		
		
		//add resources to node
		node.addProcessingUnits(cpu.build());
		node.addMemoryUnits(memory.build());
		node.addStorageUnits(storage.build());
		
		//add node to site
		site.addNodes(node.build());
	}
	
}
	
	/**
	 * Creates test Infrastructure model
	 * 
	 * @param name
	 * @param numberOfSites
	 * @param numberOfNodesPerSite
	 * @return the populated Infrastructure model
	 */
	public static Infrastructure GenerateInfrastructure(String name, int numberOfSites, int numberOfNodesPerSite) {
		final int cpuFrequency = 2000;
		final int cpuCores = 24; 
		final int ram = 2048; // host memory (MEGABYTE)
		final int hdd = 1000000; // host storage (MEGABYTE)
		final int bw = 1000000; // in Megabits/s
		
		Infrastructure.Builder infrastructure = Infrastructure.newBuilder();
		infrastructure.setName(name);
		
		//only one link where all sites are connected
		Link.Builder link = Link.newBuilder();
		link.setId("0");
		link.setBandwith(bw);
		
		//create sites
	    for(int i=0; i<numberOfSites; i++){
            
	    	ResourceSite.Builder site = ResourceSite.newBuilder();
	    	site.setName("Site_"+i);
	    	site.setId(i+"");
	    	
	    	Location.Builder geolocation = Location.newBuilder();
	    	geolocation.setLatitude(i);
	    	geolocation.setLongitude(i);
	    	site.setLocation(geolocation.build());
	    	site.setHierarchyLevel(SiteLevel.Edge);
	    	
	    	//create nodes
	    	for(int j=0; j<numberOfNodesPerSite; j++){
	    		
	    		Node.Builder node = Node.newBuilder();
	    		node.setName("Node_"+i+"_"+j);
	    		node.setId(i+"_"+j);
	    		
	    		CPU.Builder cpu = CPU.newBuilder();
	    		cpu.setName("Xeon_"+i+"_"+j);
	    		cpu.setId(i+"_"+j);
	    		cpu.setMake("Intel");
	    		cpu.setRating("12345");
	    		cpu.setFrequency(cpuFrequency);
	    		//create cores
	    		for(int e=0; e<cpuCores; e++){
	    			Core.Builder core = Core.newBuilder();
	    			core.setId(i+"_"+j+"_"+e);
	    			cpu.addCpuCores(core.build());
	    		}
	    		
	    		
	    		Memory.Builder memory = Memory.newBuilder();
	    		memory.setId(i+"_"+j);
	    		memory.setCapacity(ram);
	    		
	    		Storage.Builder storage = Storage.newBuilder();
	    		storage.setId(i+"_"+j);
	    		storage.setSize(hdd);
	    		
	    		
	    		//add resources to node
	    		node.addProcessingUnits(cpu.build());
	    		node.addMemoryUnits(memory.build());
	    		node.addStorageUnits(storage.build());
	    		
	    		//add node to site
	    		site.addNodes(node.build());
	    	}
	    	ResourceSite builtSite = site.build();
	    	//add sites to infrastructure
	    	infrastructure.addSites(builtSite);
	    	
	    	//add sites to link by id
	    	link.addConnectedSites(builtSite);
	    	infrastructure.addLinks(link.build());
	    	
	    	
	    }
		

		return infrastructure.build();

	}

	/**
	 * Generates test configuration object
	 * 
	 * @param name
	 * @param duration
	 * @param rwm 
	 * @param ram 
	 * @param rim 
	 * @return
	 */
	public static Experiment GenerateConfiguration(String name, double duration, Infrastructure rim, ApplicationLandscape ram, Workload rwm) {
		Experiment.Builder configuration = Experiment.newBuilder();
		configuration.setName(name);
		configuration.setDuration(duration);
		configuration.setApplicationLandscape(ram);
		configuration.setInfrastructure(rim);
		configuration.setWorkload(rwm);

		return configuration.build();

	}
	
	/**
	 * Creates Application model, with components with one API each pointing
	 * through all API in a row
	 * 
	 * @param applicationQty
	 * @param componentQty
	 * @param rim the infrastructure model to source nodes for component deployment
	 * @return
	 */
	public static ApplicationLandscape GenerateApplication(int applicationQty, int componentQty, Infrastructure rim) {
		
		List<String> nodeIds = new ArrayList<String>();
		
		for (ResourceSite site:rim.getSitesList()){
			for (Node node: site.getNodesList()){
				nodeIds.add(node.getId());
				
			}
			
		}
		
		int indexNmberOfNodes = nodeIds.size()-1;
		int indexNmberOfNodesCounter=0;

		ApplicationLandscape.Builder applicationList = ApplicationLandscape.newBuilder();
		
		int appCounter=1;
		while(applicationQty!=appCounter-1){
			
			Application.Builder application = Application.newBuilder();
			application.setApplicationId(appCounter+"");
			application.setApplicationName(appCounter+"");
			
			int componentCounter = 1;
			while(componentQty!=componentCounter-1){
				
				Component.Builder applicationComponent = Component.newBuilder();	
				applicationComponent.setComponentName(componentCounter+"");
				applicationComponent.setComponentId(componentCounter+"");
				applicationComponent.setIsLoadbalanced(false);
				
				//deploy on consecutive nodes
				Deployment.Builder deployment = Deployment.newBuilder();
				deployment.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
				//reset or advance counter
				if(indexNmberOfNodesCounter==indexNmberOfNodes){
					indexNmberOfNodesCounter =0;
				}else{
					indexNmberOfNodesCounter++;
				}
				
				applicationComponent.setDeployment(deployment.build());
				
				Component.Api.Builder api = Component.Api.newBuilder();
				api.setApiId("1");
				api.setApiName("Component"+componentCounter+"");
				api.setMips(1000);
				api.setIops(1000);
				api.setDataToTransfer(100);
				
				//check if last component in a row and forward reference to the next component and API IDs
				if(componentQty!=componentCounter){
					api.addNextComponentId((componentCounter+1)+"");
					// api always 1 for now
					api.addNextApiId("1");
				}
				
				
				applicationComponent.addApis(api.build());
				
				//create flavour
				VeFlavour.Builder veFlavour = VeFlavour.newBuilder();
				
				veFlavour.setCores(2);
				veFlavour.setMemory(512);
				veFlavour.setStorage(2000);
				
				applicationComponent.setFlavour(veFlavour.build());
				
				application.addComponents(applicationComponent.build());
				componentCounter++;
				
			}
			
			applicationList.addApplications(application.build());
			
			appCounter++;
		}
		
		
		return applicationList.build();

	}

}
