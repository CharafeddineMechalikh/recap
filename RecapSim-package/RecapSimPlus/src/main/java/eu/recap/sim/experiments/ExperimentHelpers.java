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
 * Class provides static methods to generate dummy data and help compose
 * experiments
 * 
 * @author Sergej Svorobej
 *
 */
/**
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
	 * Generates number of requests per device. Each request sent out at the time specified in the csv file.
	 * Requests are sent always on the 1st component of each application in a round robin fashion 
	 * 
	 * @param deviceQty
	 * @param requestQty
	 * @return
	 */
	public static Workload GenerateTietoDeviceBehavior(int deviceQty, int requestQtyControlPlane, int requestQtyUserPlane, ApplicationLandscape ram) {

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
			
			//generate control plane requests
			int requestQtyCounter = requestQtyControlPlane;
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
			
			//generate User Plane requests
			requestQtyCounter = requestQtyUserPlane;
			while (requestQtyCounter != 0) {
				Request.Builder request = Request.newBuilder();
				
				request.setApplicationId(appIds.get(indexNmberOfApplicationsCounter));
				//reset if we ran out of applications or advance counter
				if(indexNmberOfApplicationsCounter==indexNmberOfApplications){
					indexNmberOfApplicationsCounter =0;
				}else{
					indexNmberOfApplicationsCounter++;
				}
								
				request.setComponentId("2");
				request.setApiId("6");
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
	 * Generates number of requests per device. Each request sent out at the time specified in the CSV file.
	 * Request is sent in the closest deployed application  
	 * Requests are sent always on the 1st component of each application in a round robin fashion. 
	 * 
	 * @param Infrastructure rim
	 * @param ApplicationLandscape ram
	 * @return
	 */
	public static Workload GenerateTietoDemoDeviceBehaviorTietoModel(Infrastructure rim, ApplicationLandscape ram, String csvFilePath, int lineLimit) {
		//<device_id><time><long><lat>
		//create workload object where devices will be added
		Workload.Builder workload = Workload.newBuilder();
		
		//load csv from file
		String SPLIT_CHAR = ";";
		
        Scanner scanner;
        LocalDateTime startingDateTime = null;
        boolean isfirstLine =true;
        int lineCounter =0;
		try {
			scanner = new Scanner(new File(csvFilePath));
	        while (scanner.hasNext()) {
	            String[] line = scanner.nextLine().split(SPLIT_CHAR);
	            //String deviceId = line[1];
	            String deviceId = "1";
	            String dateCsv = line[0];
	            String latitude = line[1];
	            String longitude = line[2];
	            
	            //parse time into seconds from 2018-07-05-07:00:30
	            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss", Locale.ENGLISH);
	            LocalDateTime dateTime = LocalDateTime.parse(dateCsv,formatter);
	            //taking a reference date time value to create countdown from here
	            if(isfirstLine){
	            	startingDateTime = dateTime;
	            }
	            int seconds= differenceInSeconds(startingDateTime,dateTime);
	            String time = seconds+"";
	            //get device from the list orCreate device if doesn't exist
	            Device.Builder device = Device.newBuilder();
	            device.setDeviceId(deviceId);
	            //getDeviceIfDeviceExistsOrNew(deviceId,workload);
	            
				//create location for device
	            Location.Builder location = Location.newBuilder();
	            location.setTime(seconds);
	            location.setLatitude(Double.parseDouble(latitude));
	            location.setLongitude(Double.parseDouble(longitude));
	            device.addLocations(location.build());
	            //based on location of device identify location of the data centre and application it is running on
	            String closestAppId = getApplicationIdByCoordinates(Double.parseDouble(latitude), Double.parseDouble(longitude), rim,ram);
	            //generate control plane requests
	            Request.Builder controlPlaneRequest = Request.newBuilder();
	            controlPlaneRequest.setApplicationId(closestAppId);
	            controlPlaneRequest.setComponentId("1");
	            controlPlaneRequest.setApiId("1");
	            controlPlaneRequest.setTime(Long.parseLong(time));
	            controlPlaneRequest.setDataToTransfer(100);

				device.addRequests(controlPlaneRequest.build());

				//generate User Plane requests
				Request.Builder userPlaneRequest = Request.newBuilder();
				userPlaneRequest.setApplicationId(closestAppId);
				userPlaneRequest.setComponentId("2");
				userPlaneRequest.setApiId("6");
				userPlaneRequest.setTime(Long.parseLong(time));
				userPlaneRequest.setDataToTransfer(100);

				device.addRequests(userPlaneRequest.build());
				workload.addDevices(device.build());
				isfirstLine=false;
				if(lineCounter==lineLimit){
					break;
				}
				lineCounter++;
	        }
	        scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}

		return workload.build();

	}
	
	
	/**
	 * Generates number of requests per device. Each request sent out at the time specified in the CSV file.
	 * Request is sent in the closest deployed application  
	 * Requests are sent always on the 1st component of each application in a round robin fashion. 
	 * 
	 * @param Infrastructure rim
	 * @param ApplicationLandscape ram
	 * @return
	 */
	public static Workload GenerateTietoDemoDeviceBehaviorUmeaModel(Infrastructure rim, ApplicationLandscape ram, String csvFilePath) {
		//<device_id><time><long><lat>
		//create workload object where devices will be added
		Workload.Builder workload = Workload.newBuilder();
		
		//load csv from file
		String SPLIT_CHAR = ";";
		
        Scanner scanner;
        String startingDateTime = null;
        boolean isfirstLine =true;
        int lineCounter =0, lineLimit=-1;
		try {
			scanner = new Scanner(new File(csvFilePath));
	        while (scanner.hasNext()) {
	        	//id;timestamp;latitude;longitude
	        	//1;0;63.82420907620303;20.246098189729636
	            String[] line = scanner.nextLine().split(SPLIT_CHAR);
	            String deviceId = line[0];
	            String timestampCsv = line[1];
	            String latitude = line[2];
	            String longitude = line[3];
	            

	            //taking a reference date time value to create countdown from here
	            if(isfirstLine){
	            	startingDateTime = timestampCsv;
	            }
	            int seconds= Integer.parseInt(timestampCsv)-Integer.parseInt(startingDateTime);
	            String time = seconds+"";
	            //get device from the list orCreate device if doesn't exist
	            Device.Builder device = getDeviceIfDeviceExistsOrNew(deviceId,workload);
	            
				//create location for device
	            Location.Builder location = Location.newBuilder();
	            location.setTime(seconds);
	            location.setLatitude(Double.parseDouble(latitude));
	            location.setLongitude(Double.parseDouble(longitude));
	            device.addLocations(location.build());
	            //based on location of device identify location of the data centre and application it is running on
	            String closestAppId = getApplicationIdByCoordinates(Double.parseDouble(latitude), Double.parseDouble(longitude),  rim,ram);
	            //generate control plane requests
	            Request.Builder controlPlaneRequest = Request.newBuilder();
	            controlPlaneRequest.setApplicationId(closestAppId);
	            controlPlaneRequest.setComponentId("1");
	            controlPlaneRequest.setApiId("1");
	            controlPlaneRequest.setTime(Long.parseLong(time));
	            controlPlaneRequest.setDataToTransfer(100);

				device.addRequests(controlPlaneRequest.build());

				//generate User Plane requests
				Request.Builder userPlaneRequest = Request.newBuilder();
				userPlaneRequest.setApplicationId(closestAppId);
				userPlaneRequest.setComponentId("2");
				userPlaneRequest.setApiId("6");
				userPlaneRequest.setTime(Long.parseLong(time));
				userPlaneRequest.setDataToTransfer(100);

				device.addRequests(userPlaneRequest.build());
				workload.addDevices(device.build());
				isfirstLine=false;
				if(lineCounter==lineLimit){
					break;
				}
				lineCounter++;
	        }
	        scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}

		return workload.build();

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

	/**
	 * Creates test Infrastructure model for tieto usecase
	 * 
	 * @param name
	 * @param numberOfSites
	 * @param numberOfNodesPerSite
	 * @return the populated Infrastructure model
	 */
	public static Infrastructure GenerateTietoInfrastructure(String name, int numberOfSites, int numberOfNodesPerSite) {
		final int cpuFrequency = 2000;
		final int cpuCores = 8; 
		final int ram = 2048; // host memory (MEGABYTE)
		final int hdd = 1000000; // host storage (MEGABYTE)
		final int bw = 10000; // in Megabits/s
		
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
	 * Creates test Infrastructure model for tieto usecase for the Demo
	 * The infrastructure consists 7 Edge and one Core node representing Umea city
	 * 
	 * @param name
	 * @param numberOfSites
	 * @param numberOfNodesPerSite
	 * @return the populated Infrastructure model
	 */
	public static Infrastructure GenerateTietoDemoInfrastructure(String name, int numberOfNodesPerSite) {
		final int bw = 10000; // in Megabits/s
		
		Infrastructure.Builder infrastructure = Infrastructure.newBuilder();
		infrastructure.setName(name);
		
		//only one link where all sites are connected
		Link.Builder link = Link.newBuilder();
		link.setId("0");
		link.setBandwith(bw);
		
		//create sites
		//[1] 63.839307, 20.160540
    	ResourceSite.Builder site_1 = ResourceSite.newBuilder();
    	site_1.setName("Edge_Site_1");
    	site_1.setId("1");
    	
    	Location.Builder geolocation_1 = Location.newBuilder();
    	//geolocation_1.setLatitude(63.839307);
    	//geolocation_1.setLongitude(20.160540);
    	geolocation_1.setLatitude(100);
    	geolocation_1.setLongitude(400);
    	site_1.setLocation(geolocation_1.build());
    	site_1.setHierarchyLevel(SiteLevel.Edge);
    	
    	createNodes(numberOfNodesPerSite,site_1);
    	
	    ResourceSite builtSite_1 = site_1.build();
	    infrastructure.addSites(builtSite_1);
	    link.addConnectedSites(builtSite_1);
	    
		
    	//[2] 63.834893, 20.260456
    	ResourceSite.Builder site_2 = ResourceSite.newBuilder();
    	site_2.setName("Edge_Site_2");
    	site_2.setId("2");
    	
    	Location.Builder geolocation_2 = Location.newBuilder();
    	geolocation_2.setLatitude(63.834893);
    	geolocation_2.setLongitude(20.260456);
    	geolocation_2.setLatitude(600);
    	geolocation_2.setLongitude(424);
    	site_2.setLocation(geolocation_2.build());
    	site_2.setHierarchyLevel(SiteLevel.Edge);
		
    	createNodes(numberOfNodesPerSite,site_2);
    	
	    ResourceSite builtSite_2 = site_2.build();
	    infrastructure.addSites(builtSite_2);
	    link.addConnectedSites(builtSite_2);
    	
    	//[3]63.846289, 20.295831
    	ResourceSite.Builder site_3 = ResourceSite.newBuilder();
    	site_3.setName("Edge_Site_3");
    	site_3.setId("3");
    	
    	Location.Builder geolocation_3 = Location.newBuilder();
    	geolocation_3.setLatitude(63.846289);
    	geolocation_3.setLongitude(20.295831);
    	geolocation_3.setLatitude(760);
    	geolocation_3.setLongitude(284);
    	site_3.setLocation(geolocation_3.build());
    	site_3.setHierarchyLevel(SiteLevel.Edge);
    	
    	createNodes(numberOfNodesPerSite,site_3);
    	
	    ResourceSite builtSite_3 = site_3.build();
	    infrastructure.addSites(builtSite_3);
	    link.addConnectedSites(builtSite_3);
		
    	//[4]63.843638, 20.330647
    	ResourceSite.Builder site_4 = ResourceSite.newBuilder();
    	site_4.setName("Edge_Site_4");
    	site_4.setId("4");
    	
    	Location.Builder geolocation_4 = Location.newBuilder();
    	geolocation_4.setLatitude(63.843638);
    	geolocation_4.setLongitude(20.330647);
    	geolocation_4.setLatitude(925);
    	geolocation_4.setLongitude(235);
    	site_4.setLocation(geolocation_4.build());
    	site_4.setHierarchyLevel(SiteLevel.Edge);
    	
    	createNodes(numberOfNodesPerSite,site_4);
    	
	    ResourceSite builtSite_4 = site_4.build();
	    infrastructure.addSites(builtSite_4);
	    link.addConnectedSites(builtSite_4);
	    
		//[5]63.819331, 20.302661
    	ResourceSite.Builder site_5 = ResourceSite.newBuilder();
    	site_5.setName("Edge_Site_5");
    	site_5.setId("5");
    	
    	Location.Builder geolocation_5 = Location.newBuilder();
    	geolocation_5.setLatitude(63.819331);
    	geolocation_5.setLongitude(20.302661);
    	geolocation_5.setLatitude(823);
    	geolocation_5.setLongitude(532);
    	site_5.setLocation(geolocation_5.build());
    	site_5.setHierarchyLevel(SiteLevel.Edge);
    	
    	createNodes(numberOfNodesPerSite,site_5);
    	
	    ResourceSite builtSite_5 = site_5.build();
	    infrastructure.addSites(builtSite_5);
	    link.addConnectedSites(builtSite_5);
    	
		//[6]63.803858, 20.320135
    	ResourceSite.Builder site_6 = ResourceSite.newBuilder();
    	site_6.setName("Edge_Site_6");
    	site_6.setId("6");
    	
    	Location.Builder geolocation_6 = Location.newBuilder();
    	geolocation_6.setLatitude(63.803858);
    	geolocation_6.setLongitude(20.320135);
    	geolocation_6.setLatitude(862);
    	geolocation_6.setLongitude(860);
    	site_6.setLocation(geolocation_6.build());
    	site_6.setHierarchyLevel(SiteLevel.Edge);
    	
    	createNodes(numberOfNodesPerSite,site_6);
    	
	    ResourceSite builtSite_6 = site_6.build();
	    infrastructure.addSites(builtSite_6);
	    link.addConnectedSites(builtSite_6);
    	
    	//[7]63.809166, 20.253786
    	ResourceSite.Builder site_7 = ResourceSite.newBuilder();
    	site_7.setName("Edge_Site_7");
    	site_7.setId("7");
    	
    	Location.Builder geolocation_7 = Location.newBuilder();
    	geolocation_7.setLatitude(63.809166);
    	geolocation_7.setLongitude(20.253786);
    	geolocation_7.setLatitude(478);
    	geolocation_7.setLongitude(739);
    	site_7.setLocation(geolocation_7.build());
    	site_7.setHierarchyLevel(SiteLevel.Edge);
    	
    	createNodes(numberOfNodesPerSite,site_7);
    	
	    ResourceSite builtSite_7 = site_7.build();
	    infrastructure.addSites(builtSite_7);
	    link.addConnectedSites(builtSite_7);
    	
		//[8] Core : 63.826784, 20.265579
    	ResourceSite.Builder site_8 = ResourceSite.newBuilder();
    	site_8.setName("Core_Site_8");
    	site_8.setId("8");
    	
    	Location.Builder geolocation_8 = Location.newBuilder();
    	geolocation_8.setLatitude(63.826784);
    	geolocation_8.setLongitude(20.265579);
    	geolocation_8.setLatitude(625);
    	geolocation_8.setLongitude(533);
    	site_8.setLocation(geolocation_8.build());
    	site_8.setHierarchyLevel(SiteLevel.Core);
    	
    	createNodes(numberOfNodesPerSite,site_8);
    	
	    ResourceSite builtSite_8 = site_8.build();
	    infrastructure.addSites(builtSite_8);
	    link.addConnectedSites(builtSite_8);

    	//Build Infrastructure model
    	infrastructure.addLinks(link.build());
	    return infrastructure.build();

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
	public static ApplicationLandscape GenerateTietoApplication(int applicationQty, Infrastructure rim) {
		//All VMs are the same
		int vmCores = 2;
		int vmMemory = 512;
		int vmStorage = 2000;
		
				
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
			
			//Building Tieto application components
			//#####
			Component.Builder eNodeB_C = Component.newBuilder();
			eNodeB_C.setComponentName("eNodeB-C");
			eNodeB_C.setComponentId("1");
			eNodeB_C.setIsLoadbalanced(false);
			
			//deploy on consecutive nodes
			Deployment.Builder deployment_eNodeB_C = Deployment.newBuilder();
			deployment_eNodeB_C.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
			//reset or advance counter
			if(indexNmberOfNodesCounter==indexNmberOfNodes){
				indexNmberOfNodesCounter =0;
			}else{
				indexNmberOfNodesCounter++;
			}
			
			eNodeB_C.setDeployment(deployment_eNodeB_C.build());
			
			//create APIs
			Component.Api.Builder api_controlPlane = Component.Api.newBuilder();
			api_controlPlane.setApiId("1");
			api_controlPlane.setApiName(eNodeB_C.getComponentName()+"_Control_Plane_Action");
			//resource consumption
			api_controlPlane.setMips(1000);
			api_controlPlane.setIops(1000);
			api_controlPlane.setDataToTransfer(100);
			//connect to next api
			api_controlPlane.setNextComponentId("2");
			api_controlPlane.setNextApiId("2");
			
			eNodeB_C.addApis(api_controlPlane.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_controlPlane = VeFlavour.newBuilder();
			veFlavour_controlPlane.setCores(vmCores);
			veFlavour_controlPlane.setMemory(vmMemory);
			veFlavour_controlPlane.setStorage(vmStorage);
			
			eNodeB_C.setFlavour(veFlavour_controlPlane.build());
			
			application.addComponents(eNodeB_C.build());

			
			
			//####
			Component.Builder eNode_U = Component.newBuilder();
			eNode_U.setComponentName("eNode-U");
			eNode_U.setComponentId("2");
			eNode_U.setIsLoadbalanced(false);
			
			//deploy on consecutive nodes
			Deployment.Builder deployment_eNode_U = Deployment.newBuilder();
			deployment_eNode_U.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
			//reset or advance counter
			if(indexNmberOfNodesCounter==indexNmberOfNodes){
				indexNmberOfNodesCounter =0;
			}else{
				indexNmberOfNodesCounter++;
			}
			
			eNode_U.setDeployment(deployment_eNode_U.build());
			
			//create APIs
			Component.Api.Builder api_create_bearer = Component.Api.newBuilder();
			api_create_bearer.setApiId("2");
			api_create_bearer.setApiName(eNode_U.getComponentName()+"_Create_Bearer");
			//resource consumption
			api_create_bearer.setMips(1000);
			api_create_bearer.setIops(1000);
			api_create_bearer.setDataToTransfer(100);
			//connect to next api
			api_create_bearer.setNextComponentId("3");
			api_create_bearer.setNextApiId("3");
			
			eNode_U.addApis(api_create_bearer.build());
			
			Component.Api.Builder user_plane_action = Component.Api.newBuilder();
			user_plane_action.setApiId("6");
			user_plane_action.setApiName(eNode_U.getComponentName()+"_User_Plane_Action");
			//resource consumption
			user_plane_action.setMips(1000);
			user_plane_action.setIops(1000);
			user_plane_action.setDataToTransfer(100);
			//connect to next api
			user_plane_action.setNextComponentId("4");
			user_plane_action.setNextApiId("7");
			
			eNode_U.addApis(user_plane_action.build());
			
			
			//create flavour
			VeFlavour.Builder veFlavour_create_bearer = VeFlavour.newBuilder();
			veFlavour_create_bearer.setCores(vmCores);
			veFlavour_create_bearer.setMemory(vmMemory);
			veFlavour_create_bearer.setStorage(vmStorage);
			
			eNode_U.setFlavour(veFlavour_create_bearer.build());
			
			application.addComponents(eNode_U.build());
			
			
			//####
			Component.Builder MME = Component.newBuilder();
			MME.setComponentName("MME");
			MME.setComponentId("3");
			MME.setIsLoadbalanced(false);
			
			//deploy on consecutive nodes
			Deployment.Builder deployment_MME = Deployment.newBuilder();
			deployment_MME.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
			//reset or advance counter
			if(indexNmberOfNodesCounter==indexNmberOfNodes){
				indexNmberOfNodesCounter =0;
			}else{
				indexNmberOfNodesCounter++;
			}
			
			MME.setDeployment(deployment_MME.build());
			
			Component.Api.Builder authenticate = Component.Api.newBuilder();
			authenticate.setApiId("3");
			authenticate.setApiName(MME.getComponentName()+"_Authenticaten");
			//resource consumption
			authenticate.setMips(1000);
			authenticate.setIops(1000);
			authenticate.setDataToTransfer(100);
			//connect to next api
			authenticate.setNextComponentId("4");
			authenticate.setNextApiId("4");
			
			MME.addApis(authenticate.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_mme = VeFlavour.newBuilder();
			veFlavour_mme.setCores(vmCores);
			veFlavour_mme.setMemory(vmMemory);
			veFlavour_mme.setStorage(vmStorage);
			
			MME.setFlavour(veFlavour_mme.build());
			
			application.addComponents(MME.build());
			
			//####
			Component.Builder SGW_U = Component.newBuilder();
			SGW_U.setComponentName("SGW-U");
			SGW_U.setComponentId("4");
			SGW_U.setIsLoadbalanced(false);
			
			//deploy on consecutive nodes
			Deployment.Builder deployment_SGW_U = Deployment.newBuilder();
			deployment_SGW_U.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
			//reset or advance counter
			if(indexNmberOfNodesCounter==indexNmberOfNodes){
				indexNmberOfNodesCounter =0;
			}else{
				indexNmberOfNodesCounter++;
			}
			
			SGW_U.setDeployment(deployment_SGW_U.build());
			
			Component.Api.Builder relay1 = Component.Api.newBuilder();
			relay1.setApiId("4");
			relay1.setApiName(SGW_U.getComponentName()+"_Relay1");
			//resource consumption
			relay1.setMips(1000);
			relay1.setIops(1000);
			relay1.setDataToTransfer(100);
			//connect to next api
			relay1.setNextComponentId("5");
			relay1.setNextApiId("5");
			
			SGW_U.addApis(relay1.build());
			
			Component.Api.Builder relay2 = Component.Api.newBuilder();
			relay2.setApiId("7");
			relay2.setApiName(SGW_U.getComponentName()+"_Relay2");
			//resource consumption
			relay2.setMips(1000);
			relay2.setIops(1000);
			relay2.setDataToTransfer(100);
			//connect to next api
			relay2.setNextComponentId("5");
			relay2.setNextApiId("8");
			
			SGW_U.addApis(relay2.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_sgw_u = VeFlavour.newBuilder();
			veFlavour_sgw_u.setCores(vmCores);
			veFlavour_sgw_u.setMemory(vmMemory);
			veFlavour_sgw_u.setStorage(vmStorage);
			
			SGW_U.setFlavour(veFlavour_sgw_u.build());
			
			application.addComponents(SGW_U.build());
			
			//###
			Component.Builder PGW_U = Component.newBuilder();
			PGW_U.setComponentName("PGW-U");
			PGW_U.setComponentId("5");
			PGW_U.setIsLoadbalanced(false);
			
			//deploy on consecutive nodes
			Deployment.Builder deployment_PGW_U = Deployment.newBuilder();
			deployment_PGW_U.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
			//reset or advance counter
			if(indexNmberOfNodesCounter==indexNmberOfNodes){
				indexNmberOfNodesCounter =0;
			}else{
				indexNmberOfNodesCounter++;
			}
			
			PGW_U.setDeployment(deployment_PGW_U.build());
			
			Component.Api.Builder create_session = Component.Api.newBuilder();
			create_session.setApiId("5");
			create_session.setApiName(SGW_U.getComponentName()+"_Create_Session");
			//resource consumption
			create_session.setMips(1000);
			create_session.setIops(1000);
			create_session.setDataToTransfer(100);
			
			PGW_U.addApis(create_session.build());
			
			Component.Api.Builder relay3 = Component.Api.newBuilder();
			relay3.setApiId("8");
			relay3.setApiName(SGW_U.getComponentName()+"_Relay3");
			//resource consumption
			relay3.setMips(1000);
			relay3.setIops(1000);
			relay3.setDataToTransfer(100);
			
			PGW_U.addApis(relay3.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_pgw_u = VeFlavour.newBuilder();
			veFlavour_pgw_u.setCores(vmCores);
			veFlavour_pgw_u.setMemory(vmMemory);
			veFlavour_pgw_u.setStorage(vmStorage);
			
			PGW_U.setFlavour(veFlavour_pgw_u.build());
			
			application.addComponents(PGW_U.build());
			
			applicationList.addApplications(application.build());
			
			appCounter++;
		}
		
		
		return applicationList.build();

	}


	/**####DEMO####
	 * Creates Application model, with components with one API each pointing
	 * through all API in a row. Each edge location has one NFV application running
	 * 
	 * @param applicationQty
	 * @param componentQty
	 * @param rim the infrastructure model to source nodes for component deployment
	 * @return
	 */
	public static ApplicationLandscape GenerateTietoDemoApplication(Infrastructure rim) {
		//All VMs are the same
		int vmCores = 2;
		int vmMemory = 512;
		int vmStorage = 2000;
		
		int applicationQty =rim.getSitesCount();
		
		ApplicationLandscape.Builder applicationList = ApplicationLandscape.newBuilder();
		
		int appCounter=1;
		while(applicationQty!=appCounter-1){
			
			Application.Builder application = Application.newBuilder();
			application.setApplicationId(appCounter+"");
			application.setApplicationName(appCounter+"");
			
			//Building Tieto application components
			//#####
			Component.Builder eNodeB_C = Component.newBuilder();
			eNodeB_C.setComponentName("eNodeB-C");
			eNodeB_C.setComponentId("1");
			eNodeB_C.setIsLoadbalanced(false);
			
			
			//create APIs
			Component.Api.Builder api_controlPlane = Component.Api.newBuilder();
			api_controlPlane.setApiId("1");
			api_controlPlane.setApiName(eNodeB_C.getComponentName()+"_Control_Plane_Action");
			//resource consumption
			api_controlPlane.setMips(1000);
			api_controlPlane.setIops(1000);
			api_controlPlane.setDataToTransfer(100);
			//connect to next api
			api_controlPlane.setNextComponentId("2");
			api_controlPlane.setNextApiId("2");
			
			eNodeB_C.addApis(api_controlPlane.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_controlPlane = VeFlavour.newBuilder();
			veFlavour_controlPlane.setCores(vmCores);
			veFlavour_controlPlane.setMemory(vmMemory);
			veFlavour_controlPlane.setStorage(vmStorage);
			
			eNodeB_C.setFlavour(veFlavour_controlPlane.build());
			
			application.addComponents(eNodeB_C);

			
			//####
			Component.Builder eNode_U = Component.newBuilder();
			eNode_U.setComponentName("eNode-U");
			eNode_U.setComponentId("2");
			eNode_U.setIsLoadbalanced(false);
						
			//create APIs
			Component.Api.Builder api_create_bearer = Component.Api.newBuilder();
			api_create_bearer.setApiId("2");
			api_create_bearer.setApiName(eNode_U.getComponentName()+"_Create_Bearer");
			//resource consumption
			api_create_bearer.setMips(1000);
			api_create_bearer.setIops(1000);
			api_create_bearer.setDataToTransfer(100);
			//connect to next api
			api_create_bearer.setNextComponentId("3");
			api_create_bearer.setNextApiId("3");
			
			eNode_U.addApis(api_create_bearer.build());
			
			Component.Api.Builder user_plane_action = Component.Api.newBuilder();
			user_plane_action.setApiId("6");
			user_plane_action.setApiName(eNode_U.getComponentName()+"_User_Plane_Action");
			//resource consumption
			user_plane_action.setMips(1000);
			user_plane_action.setIops(1000);
			user_plane_action.setDataToTransfer(100);
			//connect to next api
			user_plane_action.setNextComponentId("4");
			user_plane_action.setNextApiId("7");
			
			eNode_U.addApis(user_plane_action.build());
			
			
			//create flavour
			VeFlavour.Builder veFlavour_create_bearer = VeFlavour.newBuilder();
			veFlavour_create_bearer.setCores(vmCores);
			veFlavour_create_bearer.setMemory(vmMemory);
			veFlavour_create_bearer.setStorage(vmStorage);
			
			eNode_U.setFlavour(veFlavour_create_bearer.build());
			
			application.addComponents(eNode_U);
			
			
			//####
			Component.Builder MME = Component.newBuilder();
			MME.setComponentName("MME");
			MME.setComponentId("3");
			MME.setIsLoadbalanced(false);
						
			Component.Api.Builder authenticate = Component.Api.newBuilder();
			authenticate.setApiId("3");
			authenticate.setApiName(MME.getComponentName()+"_Authenticaten");
			//resource consumption
			authenticate.setMips(1000);
			authenticate.setIops(1000);
			authenticate.setDataToTransfer(100);
			//connect to next api
			authenticate.setNextComponentId("4");
			authenticate.setNextApiId("4");
			
			MME.addApis(authenticate.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_mme = VeFlavour.newBuilder();
			veFlavour_mme.setCores(vmCores);
			veFlavour_mme.setMemory(vmMemory);
			veFlavour_mme.setStorage(vmStorage);
			
			MME.setFlavour(veFlavour_mme.build());
			
			application.addComponents(MME);
			
			//####
			Component.Builder SGW_U = Component.newBuilder();
			SGW_U.setComponentName("SGW-U");
			SGW_U.setComponentId("4");
			SGW_U.setIsLoadbalanced(false);
						
			Component.Api.Builder relay1 = Component.Api.newBuilder();
			relay1.setApiId("4");
			relay1.setApiName(SGW_U.getComponentName()+"_Relay1");
			//resource consumption
			relay1.setMips(1000);
			relay1.setIops(1000);
			relay1.setDataToTransfer(100);
			//connect to next api
			relay1.setNextComponentId("5");
			relay1.setNextApiId("5");
			
			SGW_U.addApis(relay1.build());
			
			Component.Api.Builder relay2 = Component.Api.newBuilder();
			relay2.setApiId("7");
			relay2.setApiName(SGW_U.getComponentName()+"_Relay2");
			//resource consumption
			relay2.setMips(1000);
			relay2.setIops(1000);
			relay2.setDataToTransfer(100);
			//connect to next api
			relay2.setNextComponentId("5");
			relay2.setNextApiId("8");
			
			SGW_U.addApis(relay2.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_sgw_u = VeFlavour.newBuilder();
			veFlavour_sgw_u.setCores(vmCores);
			veFlavour_sgw_u.setMemory(vmMemory);
			veFlavour_sgw_u.setStorage(vmStorage);
			
			SGW_U.setFlavour(veFlavour_sgw_u.build());
			
			application.addComponents(SGW_U);
			
			//###
			Component.Builder PGW_U = Component.newBuilder();
			PGW_U.setComponentName("PGW-U");
			PGW_U.setComponentId("5");
			PGW_U.setIsLoadbalanced(false);
					
			
			Component.Api.Builder create_session = Component.Api.newBuilder();
			create_session.setApiId("5");
			create_session.setApiName(SGW_U.getComponentName()+"_Create_Session");
			//resource consumption
			create_session.setMips(1000);
			create_session.setIops(1000);
			create_session.setDataToTransfer(100);
			
			PGW_U.addApis(create_session.build());
			
			Component.Api.Builder relay3 = Component.Api.newBuilder();
			relay3.setApiId("8");
			relay3.setApiName(SGW_U.getComponentName()+"_Relay3");
			//resource consumption
			relay3.setMips(1000);
			relay3.setIops(1000);
			relay3.setDataToTransfer(100);
			
			PGW_U.addApis(relay3.build());
			
			//create flavour
			VeFlavour.Builder veFlavour_pgw_u = VeFlavour.newBuilder();
			veFlavour_pgw_u.setCores(vmCores);
			veFlavour_pgw_u.setMemory(vmMemory);
			veFlavour_pgw_u.setStorage(vmStorage);
			
			PGW_U.setFlavour(veFlavour_pgw_u.build());
			
			application.addComponents(PGW_U);
			
			applicationList.addApplications(application);
			
			appCounter++;
		}
		

		//deploy one application per site
		int siteIndex =0;
		for ( Application.Builder  application:applicationList.getApplicationsBuilderList()){
			
			ResourceSite site = rim.getSites(siteIndex);
			
			int nodeNumber =site.getNodesCount();
			int nodeNumberCounter =0;
			for (Component.Builder component:application.getComponentsBuilderList()){
				
				//check if we ran out of nodes to deploy components on and start from the beginning
				if(nodeNumberCounter == nodeNumber){
					nodeNumberCounter=0;
				}
				
				//deploy components in round robin manner
				Deployment.Builder deployment = Deployment.newBuilder();
				deployment.setSiteId(site.getId());
				deployment.setNodeId(site.getNodesList().get(nodeNumberCounter).getId());
				component.setDeployment(deployment);
				component.build();
				
				nodeNumberCounter++;	
			}
			
			
			siteIndex++;
		}
		
//		for (ResourceSite site:rim.getSitesList()){
//			
//			//deploy on consecutive nodes
//			Deployment.Builder deployment_eNodeB_C = Deployment.newBuilder();
//			deployment_eNodeB_C.setNodeId(nodeIds.get(indexNmberOfNodesCounter));
//			//reset or advance counter
//			if(indexNmberOfNodesCounter==indexNmberOfNodes){
//				indexNmberOfNodesCounter =0;
//			}else{
//				indexNmberOfNodesCounter++;
//			}
//			
//			eNodeB_C.setDeployment(deployment_eNodeB_C.build());
//			
//			for (Node node: site.getNodesList()){
//				nodeIdss.add(node.getId());
//				
//			}
//			
//		}
		
		
		return applicationList.build();

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
					api.setNextComponentId((componentCounter+1)+"");
					// api always 1 for now
					api.setNextApiId("1");
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
