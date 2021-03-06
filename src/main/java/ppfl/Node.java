package ppfl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node {

	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");
	protected static Logger printLogger = debugLogger;

	protected boolean obs;// obs = 1 means observed as a given value, which excludes this node from
	// inference procedure.
	protected boolean obsvalue;
	private boolean tempvalue;// for inference(sampling)
	protected boolean isStmt;
	private double p;// inferred chance to be correct
	protected String name;
	private double impT;
	private double impF;// importance for True and False in importance sampling.
	private String testname;
	private List<Edge> edges;
	private double epsilon = 1e-8;
	private Edge degde;
	protected boolean reduced;// should be reduced in the slice if val is true

	private int stacksize; // for double:2. Other type:1.
	StmtNode stmt;

	// Heap
	private boolean isHeapObject = false;
	private int address = 0;

	public Node(String name) {
		this.obs = false;
		this.p = 0.5;
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
		edges = new ArrayList<>();
		stmt = null;
		reduced = true;
		stacksize = 1;
	}

	public Node(String name, String testname, StmtNode stmt) {
		this.testname = testname;
		this.obs = false;
		this.p = 0.5;
		this.name = name;
		isStmt = false;
		tempvalue = true;// TODO init by statistics
		edges = new ArrayList<>();
		this.stmt = stmt;
		reduced = true;
		stacksize = 1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Node other = (Node) o;
		return this.name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	public int getSize() {
		return this.stacksize;
	}

	public void setSize(int s) {
		if (s < 1) {
			throw new IllegalArgumentException("Invalid stack size: " + s);
		}
		this.stacksize = s;
	}

	public boolean isHeapObject() {
		return this.isHeapObject;
	}

	public void setHeapObject() {
		this.isHeapObject = true;
	}

	public void setAddress(int add) {
		this.address = add;
	}

	public int getAddress() {
		if (!isHeapObject)
			return 0;
		return address;
	}

	public String getName() {
		return this.testname + "#" + this.name;
	}

	// not override ok?
	public String getStmtName() {
		String[] lineinfos = this.stmt.getName().split(":");
		String stmtname = lineinfos[0] + "#" + lineinfos[1];
		return stmtname;
	}

	public String getPrintName() {
		return this.testname + "#" + this.name + "@" + this.stmt.getName();
	}

	public void observe(boolean obsvalue) {
		obs = true;
		this.obsvalue = obsvalue;
	}

	public boolean getobs() {
		return obs;
	}

	public void setdedge(Edge e) {
		degde = e;
	}

	public Edge getdedge() {
		return degde;
	}

	// reduced should be false when the node is in the front slice of a obs node
	public void setreduced() {
		reduced = false;
	}

	public boolean getreduced() {
		return reduced;
	}

	public void setTemp(boolean t) {
		tempvalue = obs ? obsvalue : t;
	}

	public boolean getCurrentValue() {
		return obs ? obsvalue : tempvalue;
	}

	public void init() {
		if (!obs) {
			impT = 0;
			impF = 0;
		}
	}

	public void addimp(double imp) {
		if (!obs) {
			if (tempvalue)
				impT += imp;
			else
				impF += imp;
		}
	}

	public double getprob() {
		return impT / (impT + impF);
	}

	public boolean isStmt() {
		return this.isStmt;
	}

	public void add_edge(Edge edge) {
		edges.add(edge);
	}

	public boolean send_message() {
		if (obs) {
			double val = obsvalue ? 1.0 : 0.0;
			for (Edge n : edges) {
				n.set_ntof(val);
			}
			double delta = val - this.p;
			if (delta < 0)
				delta = -delta;
			this.p = val;
			return delta > epsilon;
		}

		double ratio = 1;
		int countnan= 0;
		for (Edge n : edges) {
			double tmpratio = n.get_fton() / (1 - n.get_fton());
			if(Double.isInfinite(tmpratio))
				countnan++;
			else if (tmpratio == 0.0)
				countnan--;
			ratio = ratio * n.get_fton() / (1 - n.get_fton());
		}
		double result = ratio / (ratio + 1);
		if(Double.isNaN(result))
		{
			if(countnan>0)
				result = 1;
			else if (countnan<0)
				result = 0;
			else{
				//System.out.println("0.5 error here");
				result = 0.5;
			}
		}
		double delta = result - this.p;
		if (delta < 0)
			delta = -delta;
		this.p = result;

		for (Edge n : edges) {
			double b = (1 - n.get_fton());
			double a = n.get_fton();
			// double tv1 = b/(b+a/result);
			double tv1;
			if(Double.isNaN(ratio)||Double.isInfinite(ratio))
			{
				if(countnan>0)
					tv1 = 1;
				else if (countnan<0)
					tv1 = 0;
				else{
					tv1 = 1-b;
				}
			}
			else if (Double.isNaN(a / ratio)||Double.isInfinite(a/ratio))
				tv1 = 0;
			else
				tv1 = b / (b + a / ratio);
			// if(Double.isNaN(tv1))
			// 	System.out.println("here is bug nan + a/ratio = "+ a/ratio+ ", b = "+ b+ ", ratio = " + ratio);
			n.set_ntof(tv1);
		}
		return delta > epsilon;
	}

	public double bp_getprob() {
		return p;
	}

	public static void setLogger(Logger lgr) {
		printLogger = lgr;
	}

	public void print(Logger lgr, String prefix) {
		if (this.obs) {
			lgr.info("{}{} observed = {}", prefix, this.getPrintName(), this.obsvalue);
		} else {
			lgr.info("{}{}", prefix, this.getPrintName());
		}
	}

	public void print(String prefix) {
		if (this.obs) {
			printLogger.info("{}{} observed = {}", prefix, this.getPrintName(), this.obsvalue);
		} else {
			printLogger.info("{}{}", prefix, this.getPrintName());
		}
	}

	public void print(Logger lgr) {
		if (this.obs) {
			lgr.info("{} observed = {}", this.getPrintName(), this.obsvalue);
		} else {
			lgr.info(this.getPrintName());
		}
	}

	public void print() {
		if (this.obs) {
			printLogger.info("{} observed = {}", this.getPrintName(), this.obsvalue);
		} else {
			printLogger.info(this.getPrintName());
		}
	}

	public void printprob() {
		if (this.obs) {
			printLogger.info("{}obs prob = {}", this.getPrintName(), (this.obsvalue ? 1.0 : 0.0));
		} else
			printLogger.info("{} prob = {}", this.getPrintName(), getprob());
	}

	public void bpPrintProb() {
		printLogger.info("{} prob_bp = {}", this.getPrintName(), bp_getprob());
	}

	public void bpPrintProb(Logger lgr) {
		lgr.info("{} prob_bp = {}", this.getPrintName(), bp_getprob());
	}

}
