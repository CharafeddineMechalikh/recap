/**
 * 
 */
package eu.recap.sim.cloudsim.vm;

import java.util.Objects;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.vms.VmSimple;

/**
 * RECAP notion of VE component with a deployed application  
 * 
 * @author Sergej Svorobej
 *
 */
public class RecapVe extends VmSimple implements IRecapVe{
	String applicationId;
	String applicationComponentId;
	boolean isLoadbalancer = false;

	
	public RecapVe(int id, DatacenterBroker broker, long mipsCapacity, int numberOfPes, long ramCapacity,
			long bwCapacity, long size, String vmm, CloudletScheduler cloudletScheduler) {
		
		super(id,mipsCapacity,numberOfPes);
		super.setBroker(broker);
		super.setRam(ramCapacity);
		super.setBw(bwCapacity);
		super.setSize(size);
		super.setVmm(vmm);
		super.setCloudletScheduler(cloudletScheduler);
		
	}

	@Override
	public String getApplicationID() {
		
		return applicationId;
	}

	@Override
	public IRecapVe setApplicationID(String applicationId) {
		this.applicationId =  Objects.isNull(applicationId) ? "" : applicationId;
        
        return this;
		
	}

	@Override
	public String getApplicationComponentID() {
		
		return applicationComponentId;
	}

	@Override
	public IRecapVe setApplicationComponentID(String applicationComponentId) {
		this.applicationComponentId = Objects.isNull(applicationComponentId) ? "" : applicationComponentId;
		return this;
	}

	@Override
	public boolean isLoadbalancer() {
		return isLoadbalancer;
	}

}
