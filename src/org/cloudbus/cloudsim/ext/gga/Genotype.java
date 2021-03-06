package org.cloudbus.cloudsim.ext.gga;

import org.cloudbus.cloudsim.ext.gga.enums.PackingT;
import org.cloudbus.cloudsim.ext.utils.ScientificMethods;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Genotype {
	static private int idnum = 0;
	static private Random rnd = new Random();
	static private Problem problem;

	private int idTag; // unique number
	private double fitness; // current fitness
	private double[] gFitness; // current fitness
	private int nrOfObjects; // number of objects
	private int nrOfGroups; // number of groups
	private int nrOfPacks; // number of colors in problem
	private double allEleMutationProb; // chance group is deleted in mutation
	private PackingT packingUsed; // what algo to use for coloring nodes
	private int objects[]; // holds objects
	private int groups[]; // holds groups
	
	//增加统计利用率的部分
	private double[] uCpu;
	private double[] uRam;
	private double[] uDisk;
	private double[] uBw;
	private double[] uAvg;

	public Genotype() {
		this.objects = new int[Constants.MAXOBJECTS];
		this.groups = new int[Constants.MAXOBJECTS];
		this.packingUsed = PackingT.FIRSTFIT;
		this.gFitness = new double[Constants.MAXOBJECTS];
		
		uCpu = new double[Constants.MAXOBJECTS];
		uRam = new double[Constants.MAXOBJECTS];
		uDisk = new double[Constants.MAXOBJECTS];
		uBw = new double[Constants.MAXOBJECTS];
		uAvg = new double[Constants.MAXOBJECTS];
	}
	
	public Genotype(String geno) {
		this.objects = new int[Constants.MAXOBJECTS];
		this.groups = new int[Constants.MAXOBJECTS];
		this.packingUsed = PackingT.FIRSTFIT;
		this.gFitness = new double[Constants.MAXOBJECTS];
		
		String [] tmp = geno.split(":");
		String [] objs = tmp[0].trim().split(" ");
		String [] grps = tmp[1].trim().split(" ");
		
		nrOfObjects = objs.length;
		nrOfGroups = grps.length;
		
		for (int i=0; i < objs.length; i++) {
			objects[i] = Integer.parseInt(objs[i]);
			//System.out.print(i + ": " + objs[i] + "--");
		}
		
		for (int i=0; i < grps.length; i++) {
			groups[i] = Integer.parseInt(grps[i]);
			//System.out.print(i + ": " + grps[i] + "--");
		}
		
		//System.out.println("iii: " + this);
		        		
		//System.exit(0);
	}

	public void Initialize(int numberOfObjects,
			double allElemutationProbability, int kBinpacking,
			PackingT packingAlgorithm)
	// Initialize the geno by making a coloring using the
	// PackObject function, and starting at a random node.
	{
		int i, r;

		r = rnd.nextInt(99999);

		idTag = idnum;
		idnum++;
		allEleMutationProb = allElemutationProbability;

		packingUsed = packingAlgorithm;

		if (packingUsed == PackingT.UNDIFINED) {
			System.err.println("Error: No coloring algorithm defined");
			System.exit(2);
		}

		nrOfGroups = 0;
		nrOfPacks = kBinpacking;
		nrOfObjects = numberOfObjects;

		if (nrOfObjects > Constants.MAXOBJECTS) {
			System.err.println("Error: number of objects is larger than "
					+ Constants.MAXOBJECTS);
			System.exit(2);
		}

		for (i = 0; i < nrOfObjects; i++)
			objects[i] = Constants.UNCOLORED;
		for (i = 0; i < nrOfObjects; i++)
			PackObject((i + r) % nrOfObjects);

		Evaluate();
		
		//System.out.println("\n\nNetworkCost: " + problem.getTopology().getVolumeCostOfNetwork(objects) + "\n");

	}
	
	public void xfdInitialize(int numberOfObjects, PackingT packingAlgorithm) {
		int i, r;
		
		r = rnd.nextInt(99999);

		idTag = idnum;
		idnum++;
		allEleMutationProb = 0;

		packingUsed = packingAlgorithm;

		if (packingUsed == PackingT.UNDIFINED) {
			System.err.println("Error: No coloring algorithm defined");
			System.exit(2);
		}

		nrOfGroups = 0;
		// number of packs can be equal to number of objects
		nrOfPacks = numberOfObjects;
		nrOfObjects = numberOfObjects;

		if (nrOfObjects > Constants.MAXOBJECTS) {
			System.err.println("Error: number of objects is larger than "
					+ Constants.MAXOBJECTS);
			System.exit(2);
		}

		for (i = 0; i < nrOfObjects; i++)
			objects[i] = Constants.UNCOLORED;
		for (i = 0; i < nrOfObjects; i++)
			PackObject(i);

		Evaluate();
	}

	public void Evaluate() {
		/*
		double uRam = 0;
		double uCpu = 0;
		double uDisk = 0;
		double uBw = 0;
		double uAvg = 0;
		*/
		int cRam[];
		int cCpu[];
		int cDisk[];
		int cBw[];

		fitness = 0;
		
		//int n = GetBinsUsed();
		// group sequence is no longer continuous
		//int n = Constants.MAXOBJECTS;
		int n = this.nrOfObjects;
		cRam = new int[n];
		cCpu = new int[n];
		cBw = new int[n];
		cDisk = new int[n];
		for (int i=0; i < n; i++) {
			cBw[i] = 0;
			cCpu[i] = 0;
			cDisk[i] = 0;
			cRam[i] = 0;
			gFitness[i] = 0;
			
			// 每个箱子的利用率清零
			uBw[i] = 0;
			uCpu[i] = 0;
			uDisk[i] = 0;
			uRam[i] = 0;
			uAvg[i] = 0;
		}
		
		for (int j=0; j < nrOfObjects; j++) {
			int group = objects[j];
			cBw[group] += problem.getItemRequest(j, 0);
			cCpu[group] += problem.getItemRequest(j, 1);
			cDisk[group] += problem.getItemRequest(j, 2);
			cRam[group] += problem.getItemRequest(j, 3);
		}
		
		int nRam = problem.GetBinSize().Mem;
		int nCpu = problem.GetBinSize().Cpu;
		int nDisk = problem.GetBinSize().Disk;
		int nBw = problem.GetBinSize().Bandwidth;
		
		for (int i=0; i < n; i++) {
			// 加上本来已经预留的资源
			cBw[i] += (nBw - problem.GetBinSize(i).Bandwidth);
			cRam[i] += (nRam - problem.GetBinSize(i).Mem);
			cCpu[i] += (nCpu - problem.GetBinSize(i).Cpu);
			cDisk[i] += (nDisk - problem.GetBinSize(i).Disk);
			nDisk = problem.GetBinSize(n).Disk;
			nBw = problem.GetBinSize(n).Bandwidth;
			// 计算算子结果
			// 将计算结果保留到了uXXX数组里头
			uRam[i] = (double)cRam[i] / nRam;
			//System.out.println("uRam: " + uRam);
			uCpu[i] = (double)cCpu[i] / nCpu;
			uDisk[i] = (double)cDisk[i] / nDisk;
			uBw[i] = (double)cBw[i] / nBw;
			uAvg[i] = (uRam[i]+uCpu[i]+uDisk[i]+uBw[i]) / 4;
			
			//计算FF中单项分母
			double down = 0;
			down += Math.sqrt(Math.abs(uCpu[i]-uAvg[i]));
			down += Math.sqrt(Math.abs(uBw[i]-uAvg[i]));
			down += Math.sqrt(Math.abs(uDisk[i]-uAvg[i]));
			down += Math.sqrt(Math.abs(uRam[i]-uAvg[i]));
			
			//计算单项结果
			if (down != 0) {
				gFitness[i] = Math.sqrt(uAvg[i] / down);
				fitness += gFitness[i];
			}
		}
		
		//得到最后结果
		fitness /= GetBinsUsed();
		/*
		if (isGenoValid()) {
			System.out.println("valid");
		} else {
			System.out.println(this);
			System.out.println("not valid");
		}*/
		//System.err.println("fitdddd!!!ness: "+fitness);
	}

	private boolean isGenoValid() {
		//bins 
		int ob;
		boolean find = false;
		boolean [] gFind = new boolean[nrOfGroups];
		
		for (int j=0; j < nrOfGroups; j++) {
			gFind[j] = false;
		}
		
		for (int i=0; i < nrOfObjects; i++) {
			ob = objects[i];
			find = false;
			for (int j=0; j < nrOfGroups; j++) {
				if (ob == groups[j]) {
					find = true;
					gFind[j] = true;
					break;
				}					
			}
			if (!find) {
				System.out.println("Cannot find: " + ob + " at " + i);
				return false;
			}
		}
		
		for (int j=0; j < nrOfGroups; j++) {
			if (!gFind[j]) {
				System.out.println("GG Cannot find: " + j);
				//return false;
			}
		}
		
		return true;
	}

	public void Mutation()
	// Mutate a geno by eliminating some groups and reinserting
	// the objects using the PackObject function.
	{
		//System.out.println("\n\nBeforeMutation:");
		//this.Print();
		//System.out.println("----BeforeMutation\n\n");
		
		int i;
		Stack<Integer> savedObjects = new Stack<Integer>();			// objects form eliminated groups
		boolean eliminated [] = new boolean[Constants.MAXOBJECTS];	// marker-array for eliminated groups

		//cerr << "mutation " << idtag << endl;

		for (i = 0; i < nrOfGroups; i++)
				eliminated[i] = false;

		// pick colors for elimination
		for (i = 0; i < nrOfGroups; i++)
			if ((rnd.nextInt (99999) % 100) <= (allEleMutationProb * 100))
			{
				eliminated[groups[i]] = true;
				groups[i] = Constants.ELIMINATED;
			}

		// Pick all colors that were in an eliminated group and mark
		// them uncolored
		for (i = 0; i < nrOfObjects; i++)
		{
			if (eliminated[objects[i]])
			{
				objects[i] = Constants.UNCOLORED;
				savedObjects.push (i);
			}
		}

		//System.out.println("before compact");
		//this.Print();
		//System.out.println("before compact");
		// Remove holes in group part created by elimination
		CompactGroupSimple ();
		//System.out.println("\nafter compact");
		//this.Print();
		//System.out.println("after compact");

		// Reinsert uncolored objects with PackObject function
		while (!savedObjects.empty ()) {
			PackObject (savedObjects.pop ());
		}

		// Reevaluate geno
		Evaluate ();
		
		//System.out.println("\n\nAfterMutation:");
		//this.Print();
		//System.out.println("----AfterMutation\n\n");
	} // Mutation ()
	
	// 旧的CrossOver暂时废除
	/*
	public void Crossover(Genotype otherParent, Genotype child1, Genotype child2)
	// Do a crossover operation between the geno and another parent,
	// producing two children. Using the procedure described by
	// E.Falkenhauer and the PackObject function to reinsert objects.
	{
		int i, j;
		int p1cp1, p1cp2;				// crossover-points for parent P1
		int p2cp1, p2cp2;				// crossover-points for parent P2
		boolean eliminated1[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P1
		boolean eliminated2[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P2
		Stack<Integer> objects1 = new Stack<Integer>();				// holds objects from eliminated groups P1
		Stack<Integer> objects2 = new Stack<Integer>();				// holds objects from eliminated groups P2
	
		for (i = 0; i < nrOfGroups; i++)
		{
			eliminated1[i] = false;
			eliminated2[i] = false;
		}
	
		//cerr << "crossover " << idtag << " " << otherParent.idtag << " " << child1.idtag << " " << child2.idtag << endl;
	
		// Choose crossover points
		p1cp1 = rnd.nextInt (99999) % nrOfGroups;
		p1cp2 = rnd.nextInt (99999) % nrOfGroups;
		if (p1cp2 < p1cp1)
		{
			i = p1cp1;
			p1cp1 = p1cp2;
			p1cp2 = i;
		}
		p2cp1 = rnd.nextInt (99999) % otherParent.nrOfGroups;
		p2cp2 = rnd.nextInt (99999) % otherParent.nrOfGroups;
		if (p2cp2 < p2cp1)
		{
			i = p2cp1;
			p2cp1 = p2cp2;
			p2cp2 = i;
		}
		
		System.err.println("this.getCrossPoint: " + this.getCrossPoint());
	
		// Copy parents to children
		Copy (child1);
		otherParent.Copy (child2);
	
		// Mark all groups losing at least one object with ELIMINATED
		for (i = 0; i < nrOfObjects; i++)			// look at all objects
			for (j = p1cp1; j <= p1cp2; j++)		// walk through crossing-section
				if (objects[i] == groups[j])		// object is in injected group
				{
					eliminated2[child2.objects[i]] = true;		// mark group affected
					child2.objects[i] = groups[j] + child2.nrOfGroups;	// new color
				}
		for (i = 0; i < otherParent.nrOfObjects; i++)
			for (j = p2cp1; j <= p2cp2; j++)
				if (otherParent.objects[i] == otherParent.groups[j])
				{
					eliminated1[child1.objects[i]] = true;
					child1.objects[i] = otherParent.groups[j] + child1.nrOfGroups;
				}
	
		// Eliminate effected groups
		for (i = 0; i < child1.nrOfGroups; i++)
			if (eliminated1[child1.groups[i]])
					child1.groups[i] = Constants.ELIMINATED;
	
		for (i = 0; i < child2.nrOfGroups; i++)
			if (eliminated2[child2.groups[i]])
					child2.groups[i] = Constants.ELIMINATED;
	
		// Collect objects member of an eliminated group
		for (i = 0; i < child2.nrOfObjects; i++)
			if ((child2.objects[i] < child2.nrOfGroups) && (eliminated2[child2.objects[i]]))
			{
				child2.objects[i] = Constants.UNCOLORED;
				objects2.push (i);
			}
		for (i = 0; i < child1.nrOfObjects; i++)
			if ((child1.objects[i] < child1.nrOfGroups) && (eliminated1[child1.objects[i]]))
			{
				child1.objects[i] = Constants.UNCOLORED;
				objects1.push (i);
			}
	
		// Inject group-part from parents into children
		child2.InsertGroup (groups, p1cp1, p1cp2, p2cp1);
		child1.InsertGroup (otherParent.groups, p2cp1, p2cp2, p1cp1);
	
		// Remove holes in group-array created by the elimination process
		child2.CompactGroupPart ();
		child1.CompactGroupPart ();
	
		// Reinsert objects from eliminted groups
		while (!objects2.empty ())
			child2.PackObject (objects2.pop ());
		while (!objects1.empty ())
			child1.PackObject (objects1.pop ());
	
		// Compute fitness of children
		child2.Evaluate ();
		child1.Evaluate ();
	
	} // Crossover ()
	*/

	public boolean Crossover(Genotype otherParent, Genotype child1, Genotype child2)
	// 这个CrossOver是单基因位的交换
	{
		int p1cp, p2cp;		//两个父代的交汇点；
		int i;
		boolean eliminated1[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P1
		boolean eliminated2[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P2
		Stack<Integer> objects1 = new Stack<Integer>();				// holds objects from eliminated groups P1
		Stack<Integer> objects2 = new Stack<Integer>();				// holds objects from eliminated groups P2
		
		//TODO: DEBUG用
		//System.out.println("\n\nbefore:");
		//System.out.println("1: " + this);
		//System.out.println("2: " + otherParent);
		
		for (i = 0; i < nrOfGroups; i++)
		{
			eliminated1[i] = false;
			eliminated2[i] = false;
		}
		
		// Choose crossover point
		p1cp = getCrossPoint();
		p2cp = otherParent.getCrossPoint();
		
		//TODO: DEBUG用
		//System.out.println("Chosen: 1: " + groups[p1cp] + " 2: " + otherParent.groups[p2cp]);
		
		// Copy parents to children
		Copy (child1);
		otherParent.Copy (child2);
		
		// 在这里进行允许交叉的判断
		boolean inList = false;		// 对应伪码中的在List中；
		boolean allInList = true;	// 对应伪码里头的全部在List中

		// Mark all groups losing at least one object with ELIMINATED
		
		if (problem.isTaken(groups[p1cp])) {
			inList = true;
		}
		for (i = 0; i < nrOfObjects; i++)			// look at all objects
			if (objects[i] == groups[p1cp])			// object is in injected group
			{
				if (!problem.isTaken(child2.objects[i])) {
					// 这种情况说明不在箱子里头
					allInList = false;
				}
			}
		// 判断是否允许交叉操作，如果新插入不在List内，且其内容全部是List里头的
		if (!inList && allInList) 
			return false;
		
		// 对另一组进行判断操作
		
		inList = false;		// 对应伪码中的在List中；
		allInList = true;	// 对应伪码里头的全部在List中
		if (problem.isTaken(otherParent.groups[p2cp])) {
			inList = true;
		}
		for (i = 0; i < otherParent.nrOfObjects; i++)
			if (otherParent.objects[i] == otherParent.groups[p2cp])
			{
				if (!problem.isTaken(child1.objects[i])) {
					// 这种情况说明不在箱子里头
					allInList = false;
				}
			}
		// 判断是否允许交叉操作，如果新插入不在List内，且其内容全部是List里头的
		if (!inList && allInList) 
			return false;
		
		
		// 将bin_id冲突的item位直接拿出来
		for (i = 0; i < child2.nrOfObjects; i++)
			if (child2.objects[i] == groups[p1cp])
			{
				child2.objects[i] = Constants.UNCOLORED;
				objects2.push (i);
			}
		for (i = 0; i < child1.nrOfObjects; i++)
			if (child1.objects[i] == otherParent.groups[p2cp])
			{
				child1.objects[i] = Constants.UNCOLORED;
				objects1.push (i);
			}
		
		//TODO: DEBUG用
//		System.out.println("\n\nAfter First:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);
				
		for (i = 0; i < nrOfObjects; i++)			// look at all objects
			if (objects[i] == groups[p1cp])			// object is in injected group
			{				
				if (child2.objects[i] != Constants.UNCOLORED) eliminated2[child2.objects[i]] = true;		// mark group affected
				child2.objects[i] = groups[p1cp];	// 这里改动了，变成直接的
			}
		
		for (i = 0; i < otherParent.nrOfObjects; i++)
			if (otherParent.objects[i] == otherParent.groups[p2cp])
			{				
				if (child1.objects[i] != Constants.UNCOLORED) eliminated1[child1.objects[i]] = true;
				child1.objects[i] = otherParent.groups[p2cp];
			}
		
//		//TODO: DEBUG用
//		System.out.println("\n\nAfter Uncoloered:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);
		
		// 这里把bin_id冲突的标记为eliminated，为了下一步抹去group
		eliminated1[otherParent.groups[p2cp]] = true;
		eliminated2[groups[p1cp]] = true;
		
//		//TODO: DEBUG用
//		System.out.println("\n\nBefore EG:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);
		
		// Eliminate effected groups
		// 这里把bin_id冲突的group位抹去
		for (i = 0; i < child1.nrOfGroups; i++)
			if (eliminated1[child1.groups[i]])
					child1.groups[i] = Constants.ELIMINATED;

		for (i = 0; i < child2.nrOfGroups; i++)
			if (eliminated2[child2.groups[i]])
					child2.groups[i] = Constants.ELIMINATED;
		
		// 注意，我们不把和bin_id冲突的标记为eliminated，因为item位已经染色
		eliminated1[otherParent.groups[p2cp]] = false;
		eliminated2[groups[p1cp]] = false;
		
		//TODO: DEBUG用
//		System.out.println("\n\nAfter EG:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);

		//TODO: DEBUG用
//		System.out.println("\n\nBefore UO:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);
		
		// Collect objects member of an eliminated group
		// 这里不标记item位冲突的；
		for (i = 0; i < child2.nrOfObjects; i++)
			//!!!fuck!!! (child2.objects[i] < child2.nrOfGroups)
			// 这个判断条件不应该要了，object[i]的标记有可能和那个不同啊！！！
			if ((child2.objects[i] != Constants.UNCOLORED) && (eliminated2[child2.objects[i]]))
			{
				child2.objects[i] = Constants.UNCOLORED;
				objects2.push (i);
			}
		for (i = 0; i < child1.nrOfObjects; i++)
			//!!!fuck!!!
			if ((child1.objects[i] != Constants.UNCOLORED) && (eliminated1[child1.objects[i]]))
			{
				child1.objects[i] = Constants.UNCOLORED;
				objects1.push (i);
			}
		
		//TODO: DEBUG用
//		System.out.println("\n\nAfter UO:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);

		// Inject group-part from parents into children
		
		//TODO: DEBUG用
//		System.out.println("\n\nBefore inject:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);
		
		child2.InsertGroup (groups, p1cp, p2cp);
		child1.InsertGroup (otherParent.groups, p2cp, p1cp);
		
		//TODO: DEBUG用
//		System.out.println("\n\nAfter inject:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);
		
		

		// Remove holes in group-array created by the elimination process
		child2.CompactGroupSimple ();
		child1.CompactGroupSimple ();

		// Reinsert objects from eliminted groups
		while (!objects2.empty ())
			child2.PackObject (objects2.pop ());
		while (!objects1.empty ())
			child1.PackObject (objects1.pop ());
		
		//TODO: DEBUG用
//		System.out.println("\n\nAfter:");
//		System.out.println("1: " + child1);
//		System.out.println("2: " + child2);

		// Compute fitness of children
		child2.Evaluate ();
		child1.Evaluate ();
		
		return true;
		//System.out.println("\n\nAfterCrossOver:");
		//child1.Print();
		//child2.Print();
		//System.out.println("----AfterCrossOver\n\n");
	}
	
	public void CrossoverFit(Genotype otherParent, Genotype child1, Genotype child2)
	// Do a crossover operation between the geno and another parent,
	// producing two children. Using the procedure described by
	// E.Falkenhauer and the PackObject function to reinsert objects.
	{
		int i;
		int p1cp, p2cp;
		boolean eliminated1[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P1
		boolean eliminated2[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P2
		Stack<Integer> objects1 = new Stack<Integer>();				// holds objects from eliminated groups P1
		Stack<Integer> objects2 = new Stack<Integer>();				// holds objects from eliminated groups P2

		for (i = 0; i < nrOfGroups; i++)
		{
			eliminated1[i] = false;
			eliminated2[i] = false;
		}

		//cerr << "crossover " << idtag << " " << otherParent.idtag << " " << child1.idtag << " " << child2.idtag << endl;

		// Choose crossover points
		p1cp = getCrossPoint();
		p2cp = otherParent.getCrossPoint();
		
		// Copy parents to children
		Copy (child1);
		otherParent.Copy (child2);

		// Mark all groups losing at least one object with ELIMINATED
		for (i = 0; i < nrOfObjects; i++)			// look at all objects
			if (objects[i] == groups[p1cp])
			{
				eliminated2[child2.objects[i]] = true;		// mark group affected
				child2.objects[i] = groups[p1cp] + child2.nrOfGroups;	// new color
			}
		for (i = 0; i < otherParent.nrOfObjects; i++)
			if (otherParent.objects[i] == otherParent.groups[p2cp])
			{
				eliminated1[child1.objects[i]] = true;
				child1.objects[i] = otherParent.groups[p2cp] + child1.nrOfGroups;
			}

		// Eliminate effected groups
		for (i = 0; i < child1.nrOfGroups; i++)
			if (eliminated1[child1.groups[i]])
					child1.groups[i] = Constants.ELIMINATED;

		for (i = 0; i < child2.nrOfGroups; i++)
			if (eliminated2[child2.groups[i]])
					child2.groups[i] = Constants.ELIMINATED;

		// Collect objects member of an eliminated group
		for (i = 0; i < child2.nrOfObjects; i++)
			if ((child2.objects[i] < child2.nrOfGroups) && (eliminated2[child2.objects[i]]))
			{
				child2.objects[i] = Constants.UNCOLORED;
				objects2.push (i);
			}
		for (i = 0; i < child1.nrOfObjects; i++)
			if ((child1.objects[i] < child1.nrOfGroups) && (eliminated1[child1.objects[i]]))
			{
				child1.objects[i] = Constants.UNCOLORED;
				objects1.push (i);
			}

		// Inject group-part from parents into children
		child2.InsertGroup (groups, p1cp, p1cp, p2cp);
		child1.InsertGroup (otherParent.groups, p2cp, p2cp, p1cp);

		// Remove holes in group-array created by the elimination process
		child2.CompactGroupPart ();
		child1.CompactGroupPart ();

		// Reinsert objects from eliminted groups
		while (!objects2.empty ())
			child2.PackObject (objects2.pop ());
		while (!objects1.empty ())
			child1.PackObject (objects1.pop ());

		// Compute fitness of children
		child2.Evaluate ();
		child1.Evaluate ();

	} // CrossoverF ()
	
	// 旧的是MulCrossover
	public void CrossoverOld(Genotype otherParent, Genotype child1, Genotype child2)
	// Do a crossover operation between the geno and another parent,
	// producing two children. Using the procedure described by
	// E.Falkenhauer and the PackObject function to reinsert objects.
	{
		int i, j;
		int p1cp1, p1cp2;				// crossover-points for parent P1
		int p2cp1, p2cp2;				// crossover-points for parent P2
		boolean eliminated1[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P1
		boolean eliminated2[] = new boolean[Constants.MAXOBJECTS];	// marker-array for elimination process P2
		Stack<Integer> objects1 = new Stack<Integer>();				// holds objects from eliminated groups P1
		Stack<Integer> objects2 = new Stack<Integer>();				// holds objects from eliminated groups P2

		for (i = 0; i < nrOfGroups; i++)
		{
			eliminated1[i] = false;
			eliminated2[i] = false;
		}

		//cerr << "crossover " << idtag << " " << otherParent.idtag << " " << child1.idtag << " " << child2.idtag << endl;

		// Choose crossover points
		p1cp1 = rnd.nextInt (99999) % nrOfGroups;
		p1cp2 = rnd.nextInt (99999) % nrOfGroups;
		if (p1cp2 < p1cp1)
		{
			i = p1cp1;
			p1cp1 = p1cp2;
			p1cp2 = i;
		}
		p2cp1 = rnd.nextInt (99999) % otherParent.nrOfGroups;
		p2cp2 = rnd.nextInt (99999) % otherParent.nrOfGroups;
		if (p2cp2 < p2cp1)
		{
			i = p2cp1;
			p2cp1 = p2cp2;
			p2cp2 = i;
		}
		
		System.err.println("this.getCrossPoint: " + this.getCrossPoint());

		// Copy parents to children
		Copy (child1);
		otherParent.Copy (child2);

		// Mark all groups losing at least one object with ELIMINATED
		for (i = 0; i < nrOfObjects; i++)			// look at all objects
			for (j = p1cp1; j <= p1cp2; j++)		// walk through crossing-section
				if (objects[i] == groups[j])		// object is in injected group
				{
					eliminated2[child2.objects[i]] = true;		// mark group affected
					child2.objects[i] = groups[j] + child2.nrOfGroups;	// new color
				}
		for (i = 0; i < otherParent.nrOfObjects; i++)
			for (j = p2cp1; j <= p2cp2; j++)
				if (otherParent.objects[i] == otherParent.groups[j])
				{
					eliminated1[child1.objects[i]] = true;
					child1.objects[i] = otherParent.groups[j] + child1.nrOfGroups;
				}

		// Eliminate effected groups
		for (i = 0; i < child1.nrOfGroups; i++)
			if (eliminated1[child1.groups[i]])
					child1.groups[i] = Constants.ELIMINATED;

		for (i = 0; i < child2.nrOfGroups; i++)
			if (eliminated2[child2.groups[i]])
					child2.groups[i] = Constants.ELIMINATED;

		// Collect objects member of an eliminated group
		for (i = 0; i < child2.nrOfObjects; i++)
			if ((child2.objects[i] < child2.nrOfGroups) && (eliminated2[child2.objects[i]]))
			{
				child2.objects[i] = Constants.UNCOLORED;
				objects2.push (i);
			}
		for (i = 0; i < child1.nrOfObjects; i++)
			if ((child1.objects[i] < child1.nrOfGroups) && (eliminated1[child1.objects[i]]))
			{
				child1.objects[i] = Constants.UNCOLORED;
				objects1.push (i);
			}

		// Inject group-part from parents into children
		child2.InsertGroup (groups, p1cp1, p1cp2, p2cp1);
		child1.InsertGroup (otherParent.groups, p2cp1, p2cp2, p1cp1);

		// Remove holes in group-array created by the elimination process
		child2.CompactGroupPart ();
		child1.CompactGroupPart ();

		// Reinsert objects from eliminted groups
		while (!objects2.empty ())
			child2.PackObject (objects2.pop ());
		while (!objects1.empty ())
			child1.PackObject (objects1.pop ());

		// Compute fitness of children
		child2.Evaluate ();
		child1.Evaluate ();

	} // MulCrossover ()

	public void Print()
	// Print out the geno's genes and it's fitness.
	{
		System.out.println("Geno: " + this);
	}
	
	public String getStatics()
	// Print out the geno's genes and it's fitness.
	{
		String ret = "";
		ret += GetBinsUsed();
		ret += ", ";
		double tCpu = 0;
		double tRam = 0;
		double tDisk = 0;
		double tBw = 0;
		// tXX记录总的利用率
		// 这里输出每个bin的各维利用率
		for (int i=0; i < GetBinsUsed(); i++) {
			ret += "bin" + i + ", ";
			ret += uCpu[i];
			tCpu += uCpu[i];
			ret += ", ";
			ret += uRam[i];
			tRam += uRam[i];
			ret += ", ";
			ret += uDisk[i];
			tDisk += uDisk[i];
			ret += ", ";
			ret += uBw[i];
			tBw += uBw[i];
			ret += ", ";
		}
		
		// 最后输出总的利用率
		ret += tCpu;
		ret += ", ";
		ret += tRam;
		ret += ", ";
		ret += tDisk;
		ret += ", ";
		ret += tBw;
		
		ret += ", ";
		ret += this.GetFitness();
		
		return ret;
	}

	public int GetBinsUsed() {
		return nrOfGroups;
	}

	public double GetFitness() {
		return fitness;
	}

	public void Copy(Genotype child) 
	// Copy the geno to the supplied recipiant.
	{
		int i;

		for (i = 0; i < nrOfObjects; i++)
			child.objects[i] = objects[i];
		for (i = 0; i < nrOfGroups; i++)
			child.groups[i] = groups[i];

		child.nrOfObjects = nrOfObjects;
		child.nrOfGroups = nrOfGroups;
		child.fitness = fitness;

	}

	public int IsValid(int object)
	// Check if all objects in the geno do not violate a solution, and
	// check if there are no duplicate colors.
	{
		return 1;
	}

	public static Problem getProblem() {
		return problem;
	}

	public static void setProblem(Problem problem) {
		Genotype.problem = problem;
	}
	
	@Deprecated
	public int getAllocatedHost(int vm) {
		if (vm < nrOfObjects)
			return objects[vm];
		else {
			System.err.println("Err: Vm sequence exeeded");
			return -1;
		}
	}
	
	public int getAllocatedBin(int seq) {
		if (seq < nrOfObjects)
			return objects[seq];
		else {
			System.err.println("Err: Vm sequence exeeded");
			return -1;
		}
	}
	
	public void CompactFromOutSide() {
		this.CompactGroupPart();
	}
	
	public String toString() {
		int i;
		String str = new String("");

		str += ("(" + idTag + ") ");
		str += ("--" + nrOfGroups + "-- ");

		// Print out objects
		for (i = 0; i < nrOfObjects; i++)
			if (objects[i] == Constants.UNCOLORED)
				str += ("xx ");
			else
				str += (new DecimalFormat("00").format(objects[i])+ " ");

		str += (" : ");

		// Print out groups
		for (i = 0; i < nrOfGroups; i++)
			if (groups[i] == Constants.ELIMINATED)
				str += ("xx ");
			else
				str += (groups[i]+" ");

		str += (", ");

		// Print out fitness
		str += (" fitness: "+fitness);
		
		return str;		
	}
	
	public int[] getGroups() {
		return groups;
	}

	private int ViolatedConstraints (int object)
	// Calculate the number of constraints an object violates.
	{
		int violations = 0;
		boolean success = false;
		
		int group = objects[object];
		
		//Capacity size = problem.GetBinSize();
		// FUCK! This is not RIGHT!!!
		Capacity size = problem.GetBinSize(group);
		
		for (int i=0; i < nrOfObjects; i++) {
			if (objects[i] == group) {
				//将第i个item放到bin里头，size记录当前这个bin剩余容量
				success = problem.PutItem(size, i);
				//如果不能放入的话，返回>0的violations，表示这个防止方法不符合要求
				if (!success) return 1;
			}
		}
			
		return violations;
	} // ViolatedConstraints ()

	private void PackObject(int object)
	// Pack an object using the algoritm selected.
	{
		switch (packingUsed) {
		case FIRSTFIT:
			PackObject_FirstFit_Advanced(object);
			break;
		case FFD:
			PackObject_FirstFit_Simple(object);
			break;
		case BFD:
			PackObject_BestFit(object);
			break;
		// These two have to be implemented
		/*
		 * case smallfirst: PackObject_OrderedPacking (object); break; case
		 * largefirst: PackObject_OrderedPacking (object); break;
		 */
		default:
			System.err.println("Error: No coloring algorithm defined");
			System.exit(2);
		}

	} // PackObject ()

	private void PackObject_FirstFit_Advanced(int object)
	// Packs an object by using a first fit heurist, if no color
	// is available it creates a new group and uses this to color
	// the object with.
	{
		int i = 0;
		int bin = -1;
		boolean find = false;
		boolean[] binUsed = new boolean[Constants.MAXOBJECTS];
		
		// TODO: 这里可以考虑用哈希表存储已经访问过的
		for (int j = 0; j < Constants.MAXOBJECTS; j++) {
			binUsed[j] = false;
		}

		// First Fit Packing
		// 首先从已经被使用的箱子里头选择
		// 这里的i是group位号
		while (i < nrOfGroups) {
			bin = groups[i];
			binUsed[bin] = true;
			objects[object] = bin;
			if (ViolatedConstraints(object) > 0) {
				i++;
			} else {
				find = true;
				break;
			}
		}
		
		// 如果没有从已用箱子里头找到合适的，则进行第二轮
		// 第二轮优先使用已经占用的箱子
		i = 0;		//这里的i是binId;
		while (!find && i < nrOfObjects) {
			//如果箱子木有用，则试试
			if (!binUsed[i] && problem.isTaken(i)) {
				objects[object] = i;
				if (ViolatedConstraints(object) > 0) {
					find = false;
				} else {
					find = true;
					nrOfGroups++;
					groups[nrOfGroups - 1] = i;
				}
			} 
			i++;
		}
		
		// 最后，如果没有从已用和上轮预留箱子里头找到合适的
		// 则需要重新建一个箱子
		i = 0;
		while (!find && i < nrOfObjects) {
			//如果箱子木有用，则试试
			if (!binUsed[i] && !problem.isTaken(i)) {
				objects[object] = i;
				if (ViolatedConstraints(object) > 0) {
					find = false;
				} else {
					find = true;
					nrOfGroups++;
					groups[nrOfGroups - 1] = i;
				}
			} 
			i++;
		}
	} // PackObject_FirstFit_Advanced ()
	
	// 不区分不同的bin；
	private void PackObject_FirstFit_Simple(int object)
	// Packs an object by using a first fit heurist, if no color
	// is available it creates a new group and uses this to color
	// the object with.
	{
		 int i = 0;

         // First Fit Packing
         objects[object] = 0;
         while (ViolatedConstraints(object) > 0) {
                 i++;
                 objects[object] = i;
         }

         // New color used, update number of groups and
         // add a new color to the group-part
         if (i + 1 > nrOfGroups) {
                 nrOfGroups = i + 1;
                 groups[nrOfGroups - 1] = nrOfGroups - 1;
         }
	} // PackObject_FirstFit_Simple ()
	
	private void PackObject_BestFit(int object)
	// Packs an object by using a best fit heurist
	{
		 int best = 0;
		 int leastLeft = Integer.MAX_VALUE; 

         // First Fit Packing
         int left = 0;
         for (int i=0; i < nrOfObjects; i++) {
        	 objects[object] = i;
        	 left = tryBestFit(object);
        	 if (left >= 0 && left < leastLeft) {
        		 best = i;
        		 leastLeft = left;        		 
        	 } 
         }
         
         objects[object] = best;

         // New color used, update number of groups and
         // add a new color to the group-part
         if (best + 1 > nrOfGroups) {
                 nrOfGroups = best + 1;
                 groups[nrOfGroups - 1] = nrOfGroups - 1;
         }
	} // PackObject_FirstFit_Simple ()


	private int tryBestFit(int object) {
		int left = -1;
		boolean success = false;
		
		int group = objects[object];
		
		//Capacity size = problem.GetBinSize();
		// FUCK! This is not RIGHT!!!
		Capacity size = problem.GetBinSize(group);
		
		for (int i=0; i < nrOfObjects; i++) {
			if (objects[i] == group) {
				//将第i个item放到bin里头，size记录当前这个bin剩余容量
				success = problem.PutItem(size, i);
				//如果不能放入的话，返回>0的violations，表示这个防止方法不符合要求
				if (!success) return -1;
			}
		}
			
		return size.Cpu;
	}

	private void InsertGroup(int parentGroups[], int cp1, int cp2, int position)
	// Given the group-part and the crossing-points of another geno,
	// this method inserts the groups between these points on the
	// given position.
	{
		int i;

		// Make room for to-be-inserted-groups
		for (i = nrOfGroups; i > position; i--)
			groups[i + (cp2 - cp1)] = groups[i - 1];

		// Inject groups in the gene
		for (i = cp1; i <= cp2; i++)
			groups[i + position - cp1] = parentGroups[i] + nrOfGroups;

		// Update number of groups
		nrOfGroups = nrOfGroups + (cp2 - cp1) + 1;

	} // InsertGroup ()

	
	// 将另一个父辈cross point上头的插入到自己的position
	private void InsertGroup(int[] parentGroups, int cp, int position) {
		groups[this.nrOfGroups] = parentGroups[cp];
		nrOfGroups ++;
	}

	private void CompactGroupPart()
	// After elimination of groups from the group-part of the gene,
	// this method removes the holes created by elimination and
	// renumbers the remaining groups to create a numbering between
	// 0 and nrOfGroups - 1.
	{
		int i, j;
		boolean found; // used in finding each number 0...(nrfgroups-1)
		int max; // maximum number currently used in group-part

		// Remove eliminated groups
		i = 0;
		while (i < nrOfGroups)
			if (groups[i] == Constants.ELIMINATED) {
				for (j = i; j < nrOfGroups - 1; j++)
					groups[j] = groups[j + 1];
				nrOfGroups--;
			} else
				i++;

		// Renumber to get a nice permutation of 0,1,2,3... again
		i = 0;
		while (i < nrOfGroups) {
			j = 0;
			found = false;
			max = 0;
			// Look for the current (i) number
			while ((j < nrOfGroups) && (!found)) {
				if (groups[j] > groups[max])
					max = j;
				if (groups[j] == i)
					found = true;
				else
					j++;
			}

			// Number (i) not found, give new number to largest number
			if (!found) {
				for (j = 0; j < nrOfObjects; j++)
					if (objects[j] == groups[max])
						objects[j] = i;
				groups[max] = i;
			}
			i++;
		}
	} // CompactGroup ()
	
	private void CompactGroupSimple()
	// 这个过程去掉了传统的压缩操作，仅仅进行移动位的操作；
	{
		int i, j;

		// Remove eliminated groups
		i = 0;
		while (i < nrOfGroups)
			if (groups[i] == Constants.ELIMINATED) {
				for (j = i; j < nrOfGroups - 1; j++)
					groups[j] = groups[j + 1];
				nrOfGroups--;
			} else
				i++;
	} // CompactGroupSimple ()
	
	private int getCrossPoint() {
		List<RankSort> rankList = new ArrayList<RankSort>();
		
		for (int i=0; i < GetBinsUsed(); i++) {
			// TODO: RankSort的groupId部分，应该是序列号，而不是bin_id
			rankList.add(new RankSort(i, gFitness[groups[i]]));
		}
		
		// 进行排序
		Collections.sort(rankList);
		Collections.reverse(rankList);	//Bug Fix: 需要逆序		
		
		// 得到一个随机的rank
		double prob = rnd.nextDouble();
		int selectedRank = ScientificMethods.getRankByProb(prob, GetBinsUsed());
		
		return rankList.get(selectedRank).groupId;
	}
}

class RankSort implements Comparable<RankSort>{
	public int groupId;
	public double fitness;
	
	public RankSort(int groupId, double fitness) {
		this.groupId = groupId;
		this.fitness = fitness;
	}

	@Override
	public int compareTo(RankSort other) {
		if ((fitness - other.fitness) > 0)
			return 1;
		else if ((fitness - other.fitness) < 0)
			return -1;
		else return 0;
	}
}
