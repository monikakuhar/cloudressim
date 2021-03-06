package org.cloudbus.cloudsim.ext;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.*;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.ext.gga.Capacity;
import org.cloudbus.cloudsim.ext.utils.IOUtil;
import org.cloudbus.cloudsim.ext.utils.ScientificMethods;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class WorkLoad {
	private String workMode;
	private String loadFile;
	private int brokerId;
	private int vmsPerRound;
	private List<? extends Vm> vmList;
	private Host host;
	
	private double alfa = 0.2;
	private double beta = 0.2;
	
	public WorkLoad(String workMode, String loadFile, int brokerId, Host host, int vmsPerRound) {
		this.workMode = workMode;
		this.loadFile = loadFile;
		this.brokerId = brokerId;
		this.host = host;
		this.vmsPerRound = vmsPerRound;
		setVmList(new ArrayList<Vm>());
	}
	
	public void genWorkLoad() {
		if (workMode.equals(Constants.WORKLOAD_FROM_XML)) {
			try {
				setVmList(readWorkLoadFromXml(loadFile));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (workMode.equals(Constants.WORKLOAD_AUTO_GEN)){
			setVmList(autoGenVms(vmsPerRound));
			saveAsXML();
		} else {
			//TODO: 这里是默认参数
			setVmList(autoGenVms(vmsPerRound));
			saveAsXML();
		}		
	}
	
	public static void genManyWorkload() {
		int[] sizes = new int[5];
		sizes[0] = 100;
		sizes[1] = 300;
		sizes[2] = 500;
		sizes[3] = 800;
		sizes[4] = 1000;
		
		for (int i=0; i < sizes.length; i++) {
			genWorkload(sizes[i]);
		}
	}
	
	public static void genWorkload(int pSize) {
		int hMips = 2000;
    	int hSize = 1000000;
    	int hRam = 2000;
    	int hBw = 100;
    	
    	int mips;
    	int size;
    	int ram;
    	int bw;
    	
    	mips = 0;
    	size = 0;
    	ram = 0;
    	bw = 0;
    	
    	int cpuCnt = pSize / 4;
    	int memCnt = pSize / 4;
    	int bwCnt = pSize / 4;
    	int diskCnt = pSize / 4;
    	
    	double meanMips = 0.4 * hMips;
    	double deMips = 0.2 * hMips;
    	double meanSize = 0.4 * hSize; 
    	double deSize = 0.2 * hSize;
    	double meanRam = 0.4 * hRam; 
    	double deRam = 0.2 * hRam;
    	double meanBw = 0.4 * hBw; 
    	double deBw = 0.2 * hBw;
    	
    	Random rnd = new Random();
    	Random rnd1 = new Random();
    	Random rnd2 = new Random();
    	Random rnd3 = new Random();
    	Random rnd4 = new Random();
    	
    	List<Capacity> workload = new ArrayList<Capacity>();
    	
    	Capacity host = new Capacity();
    	host.Cpu = hMips;
    	host.Mem = hRam;
    	host.Disk = hSize;
    	host.Bandwidth = hBw;
    	
    	workload.add(host);
    	
    	int i=0;
    	while (i < pSize) {
    		// 递增i
    		i ++;    		
    		int choice = rnd.nextInt(100) % 4;
    		Capacity c = null;
    		double per = 0;
    		switch (choice) {
    		case 0:
    			cpuCnt --;
    			if (cpuCnt < 0) {
    				i--;
    				continue;
    			}
    			while (mips <= 0.1 || mips > hMips || per <= 0.1) {
        			mips = (int) ScientificMethods.normDistribution(rnd1, meanMips, deMips);
    				per = (double)mips / hMips;
    			}
    			
    			System.out.println("Perss" + per);
    			
        		while (size <= 0 || ((double)size / hSize) > per)
        			size = (int) (hSize * (rnd2.nextDouble()*0.15 + 0.1));
        		while (ram <= 0 || ((double)ram / hRam) > per)
        			ram = (int) (hRam * (rnd3.nextDouble()*0.15 + 0.1));
        		while (bw <= 0 || ((double)bw / hBw) > per)
        			bw = (int) (hBw * (rnd4.nextDouble()*0.15 + 0.1));
    			break;
    		case 1:
    			memCnt --;
    			if (memCnt < 0) {
    				i--;
    				continue;
    			}
    			while (ram <= 0.1 || ram > hRam || per <= 0.1) {
    				ram = (int) ScientificMethods.normDistribution(rnd3, meanRam, deRam);
    				per = (double)ram / hRam;
    			}
    			
    			System.out.println("Perss" + per);
    			
        		while (size <= 0 || ((double)size / hSize) > per)
        			size = (int) (hSize * (rnd2.nextDouble()*0.15 + 0.1));
        		while (mips <= 0 || ((double)mips / hMips) > per)
        			mips = (int) (hMips * (rnd1.nextDouble()*0.15 + 0.1));
        		while (bw <= 0 || ((double)bw / hBw) > per)
        			bw = (int) (hBw * (rnd4.nextDouble()*0.15 + 0.1));
    			break;
    		case 2:
    			diskCnt --;
    			if (diskCnt < 0) {
    				i--;
    				continue;
    			}
    			while (size <= 0.1 || size > hSize || per <= 0.1) {
    				size = (int) ScientificMethods.normDistribution(rnd2, meanSize, deSize);
    				per = (double)size / hSize;
    			}
    			
    			System.out.println("Perss" + per);
    			
    			while (ram <= 0 || ((double)ram / hRam) > per)
        			ram = (int) (hRam * (rnd3.nextDouble()*0.15 + 0.1));
        		while (mips <= 0 || ((double)mips / hMips) > per)
        			mips = (int) (hMips * (rnd1.nextDouble()*0.15 + 0.1));
        		while (bw <= 0 || ((double)bw / hBw) > per)
        			bw = (int) (hBw * (rnd4.nextDouble()*0.15 + 0.1));
    			break;
    		case 3:
    			bwCnt --;
    			if (bwCnt < 0) {
    				i--;
    				continue;
    			}
    			while (bw <= 0.1 || bw > hBw || per <= 0.1) {
    				bw = (int) ScientificMethods.normDistribution(rnd4, meanBw, deBw);
    				per = (double)bw / hBw;
    			}
    			
    			System.out.println("Perss" + per);
    			
    			while (ram <= 0 || ((double)ram / hRam) > per)
        			ram = (int) (hRam * (rnd3.nextDouble()*0.15 + 0.1));
    			while (mips <= 0 || ((double)mips / hMips) > per)
        			mips = (int) (hMips * (rnd1.nextDouble()*0.15 + 0.1));
        		while (size <= 0 || ((double)size / hSize) > per)
        			size = (int) (hSize * (rnd2.nextDouble()*0.15 + 0.1));
    			break;
    		}
    		c = new Capacity();
    		c.Cpu = mips;
    		c.Mem = ram;
    		c.Disk = size;
    		c.Bandwidth = bw;
    		
    		mips = 0;
        	size = 0;
        	ram = 0;
        	bw = 0;
    		
    		workload.add(c);
    	}
    	
    	try {
			IOUtil.saveAsXML(workload, (workload.size()-1) + "-old.sim");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		workload.remove(0);
		
		genDynamicWorkload(workload, new Capacity(host));
	}
	
	public void genNetwork(String fileName) {
		FileWriter file = null;
		Random comRnd = new Random();
		int[][] network = new int[300][300];
		int netLoad = -1;
		try {
			file =  new FileWriter(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i=0; i < vmsPerRound; i++) {
			for (int j=0; j < vmsPerRound; j++) {
				if (i == j) {
					netLoad = 0;
				} else if( i < j) {
					while (netLoad < 0 || netLoad > 8) {
						netLoad = (int) ScientificMethods.normDistribution(comRnd, 1, 2);
					}
				} else {
					netLoad = network[j][i];
				}
				network[i][j] = netLoad;
				// 重要，随机生成
				netLoad = -1;
			}
		}
		
		for (int i=0; i < vmsPerRound; i++) {
			for (int j=0; j < vmsPerRound; j++) {
				netLoad = network[i][j]; 
				String postfix = " ";
				if (j == (vmsPerRound - 1)) {
					postfix = "\n";
				} 
				try {
					file.write(Integer.toString(netLoad) + postfix);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Vm> List<T> autoGenVms(int amount) {
		List<Vm> vms = new ArrayList<Vm>();
		
		int hMips = host.getTotalMips();
    	long hSize = host.getStorage(); //image size (MB)
    	int hRam = host.getRam(); //vm memory (MB)
    	long hBw = host.getBw();
    	
    	double meanMips = alfa * hMips;
    	double deMips = beta * hMips;
    	double meanSize = alfa * hSize; 
    	double deSize = beta * hSize;
    	double meanRam = alfa * hRam; 
    	double deRam = beta * hRam;
    	double meanBw = alfa * hBw; 
    	double deBw = beta * hBw;
    	
    	Random rnd1 = new Random();
    	Random rnd2 = new Random(rnd1.nextLong());
    	Random rnd3 = new Random(rnd2.nextLong());
    	Random rnd4 = new Random(rnd3.nextLong());
		
    	int mips = 0;
    	long size = 0; //image size (MB)
    	int ram = 0; //vm memory (MB)
    	long bw = 0;
    	int pesNumber = 1; //number of cpus
    	int vmid = 0;
    	String vmm = "Xen"; //VMM name
    	
    	for (int i=0; i < amount; i++) {
    		while (mips <= 0 || mips > hMips)
    			mips = (int) ScientificMethods.normDistribution(rnd1, meanMips, deMips);
    		while (size <= 0 || size > hSize)
    			size = (long) ScientificMethods.normDistribution(rnd2, meanSize, deSize);
    		while (ram <= 0 || ram > hRam)
    			ram = (int) ScientificMethods.normDistribution(rnd3, meanRam, deRam);
    		while (bw <= 0 || bw > hBw)
    			bw = (long) ScientificMethods.normDistribution(rnd4, meanBw, deBw);
    		
    		Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			System.out.println("VM:: "+vm.getBw()+"**"+vm.getMips()+"**"+vm.getRam()+"**"+vm.getSize());
			vms.add(vm);
			vmid++;
			
			// 清零，下一次while可以进入循环
			mips = 0;
	    	size = 0;
	    	ram = 0;
	    	bw = 0;
    	}
    	
		return (List<T>) vms;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Vm> List<T> readWorkLoadFromXml(String xmlFile) {
		List<Capacity> workload;
		List<Vm> vms = new ArrayList<Vm>();
		
		try {
			workload = (List<Capacity>) IOUtil.loadFromXml(xmlFile);
		} catch (IOException e) {
			e.printStackTrace();
			workload = null;
		}
		
		// 将第一个也就是host的参数pop出来；
		Capacity ch = workload.remove(0);
		
		int mips = 0;
    	long size = 0; //image size (MB)
    	int ram = 0; //vm memory (MB)
    	long bw = 0;
    	int pesNumber = 1; //number of cpus
    	int vmid = 0;
    	String vmm = "Xen"; //VMM name
		for (int i=0; i < workload.size(); i++) {
			Capacity c = workload.get(i);
			mips = c.getCpu();
			size = c.getDisk();
			ram = c.getMem();
			bw = c.getBandwidth();
			
			Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			System.out.println("VM:: "+vm.getBw()+"**"+vm.getMips()+"**"+vm.getRam()+"**"+vm.getSize());
			vms.add(vm);
			vmid++;
		}
				
		//genDynamicWorkload(workload, ch);
		
		return (List<T>) vms;
	}
	
	private static void genDynamicWorkload(List<Capacity> workload, Capacity h) {
		Random rnd = new Random();
		List<Capacity> next = new ArrayList<Capacity>();
		next.add(h);
		Capacity c = new Capacity();
		System.out.println(h.Bandwidth + " " +h.Cpu + " " +h.Disk + " " +h.Mem +" WWW: " + workload.size());
		for (int i=0; i < workload.size(); i++) {
			do {
				c.Bandwidth = (int)((rnd.nextFloat()*1.5f + 0.5f) * workload.get(i).Bandwidth);
				System.out.println(i + "B CC: " + c.Bandwidth + "HH: " + h.Bandwidth);
			} while(c.Bandwidth >= h.Bandwidth);
			do {
				c.Cpu = (int)((rnd.nextFloat()*1.5f + 0.5f) * workload.get(i).Cpu);
				System.out.println(i + "C CC: " + c.Cpu + "HH: " + h.Cpu);
			} while(c.Cpu >= h.Cpu);
			do {
				c.Mem = (int)((rnd.nextFloat()*1.5f + 0.5f) * workload.get(i).Mem);
				System.out.println(i + "M CC: " + c.Mem + "HH: " + h.Mem);
			} while(c.Mem >= h.Mem);
			do {
				c.Disk = (int)((rnd.nextFloat()*1.5f + 0.5f) * workload.get(i).Disk);
				System.out.println(i + "D CC: " + c.Disk + "HH: " + h.Disk);
			} while(c.Disk >= h.Disk);
			
			next.add(new Capacity(c));
		}
		
		try {
			IOUtil.saveAsXML(next, workload.size() + "-new.sim");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	@SuppressWarnings("unchecked")
	private <T extends Vm> List<T> readWorkLoadFromXml(String xmlFile) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();   
		DocumentBuilder db = dbf.newDocumentBuilder();   
		// read from file
		Document doc = db.parse(new java.io.File(xmlFile));
		List<Vm> vms = new ArrayList<Vm>();
		System.out.println("haha");
		int mips = 0;
    	long size = 0; //image size (MB)
    	int ram = 0; //vm memory (MB)
    	long bw = 0;
    	int pesNumber = 1; //number of cpus
    	int vmid = 0;
    	String vmm = "Xen"; //VMM name
		
		NodeList nodeList = doc.getElementsByTagName("lease");
		for (int i=0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			System.out.println("fa " + node.getNodeName());
			Node lNode = node.getChildNodes().item(1);
			Node nodeSet = lNode.getChildNodes().item(1);
			pesNumber = Integer.parseInt(nodeSet.getAttributes().getNamedItem("numnodes").getNodeValue());
			int Ram = nodeSet.getChildNodes().getLength();
			NodeList ns = nodeSet.getChildNodes();
			for (int j=0; j < ns.getLength(); j++) {
				Node n = ns.item(j);
				if (n.getNodeName().equals("res")) {
					String type = n.getAttributes().getNamedItem("type").getNodeValue();
					if (type.equals("Bandwidth"))
						bw = Long.parseLong(n.getAttributes().getNamedItem("amount").getNodeValue());
					if (type.equals("Disk"))
						size = Long.parseLong(n.getAttributes().getNamedItem("amount").getNodeValue());
					if (type.equals("Memory"))
						ram = Integer.parseInt(n.getAttributes().getNamedItem("amount").getNodeValue());
					if (type.equals("CPU"))
						mips = Integer.parseInt(n.getAttributes().getNamedItem("amount").getNodeValue());
				}
			}
			Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			System.out.println("VM:: "+vm.getBw()+"**"+vm.getMips()+"**"+vm.getRam()+"**"+vm.getSize());
			vms.add(vm);
			vmid++;
		}
		
		return (List<T>) vms;
		
	}*/
	
	private void saveAsXML() {
		List<Capacity> workload = new ArrayList<Capacity>();
		
		Capacity c = new Capacity();
		c.setBandwidth((int)host.getBw());
		c.setDisk((int)host.getStorage());
		c.setMem(host.getRam());
		c.setCpu((int)host.getTotalMips());
		workload.add(c);
		
		for (int i=0; i < getVmList().size(); i++) {
			Vm vm = getVmList().get(i);
			c = new Capacity();
			c.setBandwidth((int)vm.getBw());
			c.setDisk((int)vm.getSize());
			c.setMem(vm.getRam());
			c.setCpu((int)vm.getMips());
			workload.add(c);
		}
		
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        java.util.Date date = new java.util.Date();
        String time = format.format(date);
		try {
			IOUtil.saveAsXML(workload, time + Constants.SIM_FILE_EXTENSION);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the vm list.
	 *
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmList() {
		return (List<T>) vmList;
	}

	/**
	 * Sets the vm list.
	 *
	 * @param vmList the new vm list
	 */
	protected <T extends Vm> void setVmList(List<T> vmList) {
		this.vmList = vmList;
	}

	public static void main(String[] args){
		genManyWorkload();
	}
}
