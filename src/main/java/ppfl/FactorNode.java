package ppfl;

import java.util.ArrayList;
import java.util.List;

// import org.apfloat.Apfloat;
// import org.apfloat.ApfloatMath;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class FactorNode {

	protected static MyWriter debugLogger = WriterUtils.getWriter("Debugger");
	protected static MyWriter printLogger = WriterUtils.getWriter("GraphLogger");

	private List<Node> preds;
	private Node def;
	private Node stmt;
	private List<Node> uses;
	private List<String> ops;// TODO consider operators
	private static final String[] unkops = { "%", "<", "<=", ">", ">=", "==", "!=" };
	private double HIGH = 1; // can change to 1-1e-10
	private double VHIGH = 0.99999;
	private double MEDIUM = 0.5;
	private double LOW = 0; // can change to 1e-10
	private List<Double> tensor;
	// private Apfloat ap_HIGH = new Apfloat("1.0", 100);
	// private Apfloat ap_MEDIUM = new Apfloat("0.5", 100);
	// private Apfloat ap_LOW = new Apfloat("0.0", 100);
	// private List<Apfloat> ap_tensor;
	private boolean use_ap = false;
	private List<Node> allnodes;
	private List<Edge> alledges;
	private int nnodes;
	private double stmtvalue;

	private Edge dedge;
	private Edge sedge;
	private List<Edge> pedges;
	private List<Edge> uedges;

	public FactorNode() {
		this.stmt = null;
		this.def = null;
		this.dedge = null;
	}

	// factor with only a stmt node
	public FactorNode(Node stmt, Edge sedge, double value) {
		this.stmt = stmt;
		this.def = null;
		this.dedge = null;
		this.sedge = sedge;
		this.tensor = new ArrayList<>();
		// this.ap_tensor = new ArrayList<>();
		this.nnodes = 1;
		this.stmtvalue = value;
		this.alledges = new ArrayList<>();
		alledges.add(sedge);
		if(use_ap){
			// ap_tensor.add(new Apfloat("1.0", 100).subtract(new Apfloat(String.valueOf(value),100)));
			// ap_tensor.add(new Apfloat(String.valueOf(value),100));
		}
		else{
			tensor.add(1 - value);
			tensor.add(value);
		}
	}

	public FactorNode(Node def, Node stmt, List<Node> preds, List<Node> uses, List<String> ops, Edge dedge, Edge sedge,
			List<Edge> pedges, List<Edge> uedges) {
		this.preds = preds;
		this.stmt = stmt;
		this.def = def;
		this.uses = uses;
		this.ops = ops;
		this.dedge = dedge;
		this.sedge = sedge;
		this.pedges = pedges;
		this.uedges = uedges;
		this.tensor = new ArrayList<>();
		// this.ap_tensor = new ArrayList<>();
		this.allnodes = new ArrayList<>();
		allnodes.add(stmt);
		allnodes.add(def);
		allnodes.addAll(preds);
		allnodes.addAll(uses);
		this.alledges = new ArrayList<>();
		alledges.add(sedge);
		alledges.add(dedge);
		alledges.addAll(pedges);
		alledges.addAll(uedges);
		this.nnodes = allnodes.size();
		// if (use_ap)
		// 	ap_gettensor(allnodes, nnodes - 1);
		// else
			gettensor(allnodes, nnodes - 1);
	}

	public List<Node> getpunodes() {
		ArrayList<Node> ret = new ArrayList<>();
		ret.addAll(preds);
		ret.addAll(uses);
		return ret;
	}

	public Node getstmt() {
		return this.stmt;
	}

	private void gettensor(List<Node> allnodes, int cur) {
		if (cur < 0) {
			tensor.add(getProb());
			return;
		}
		allnodes.get(cur).setTemp(false);
		gettensor(allnodes, cur - 1);
		allnodes.get(cur).setTemp(true);
		gettensor(allnodes, cur - 1);
	}

	// private void ap_gettensor(List<Node> allnodes, int cur) {
	// 	if (cur < 0) {
	// 		ap_tensor.add(ap_getProb());
	// 		return;
	// 	}
	// 	allnodes.get(cur).setTemp(false);
	// 	ap_gettensor(allnodes, cur - 1);
	// 	allnodes.get(cur).setTemp(true);
	// 	ap_gettensor(allnodes, cur - 1);
	// }

	public void send_message() {
		if (!use_ap) {
			// used to save all the messages from the nodes
			List<Double> tmpvlist = new ArrayList<>();
			for (int i = 0; i < nnodes; i++) {
				tmpvlist.add(alledges.get(i).get_ntof());
			}
			// System.out.println("tmplist = "+tmpvlist);
			for (int j = 0; j < nnodes; j++) {
				double v0 = 0;
				double v1 = 0;
				int step = (1 << j);
				int vnum = (1 << nnodes);
				// transform a tensor of nnodes-dimension into a one-dimension vector(two
				// values)
				for (int k = 0; k < vnum; k += 2 * step) {
					for (int o = 0; o < step; o++) {
						int index0 = k + o;
						double tmp0 = tensor.get(index0);

						int index1 = k + o + step;
						double tmp1 = tensor.get(index1);
						// get the bit and times the Corresponding message
						for (int mm = 0; mm < nnodes; mm++) {
							int bit0 = index0 % 2;
							index0 /= 2;
							int bit1 = index1 % 2;
							index1 /= 2;

							if (mm == j)
								continue;

							if (bit0 == 0) {
								// double tmp00 = tmp0* (1 - tmpvlist.get(mm));
								// if(Double.isNaN(tmp00))
								// System.out.println("in 0 , tmp0 = "+tmp0+", val = "+(1 - tmpvlist.get(mm)));
								tmp0 *= (1 - tmpvlist.get(mm));

							} else {
								// double tmp01 = tmp0* tmpvlist.get(mm);
								// if(Double.isNaN(tmp01))
								// System.out.println("in 1 , tmp0 = "+tmp0+", val = "+tmpvlist.get(mm));
								tmp0 *= tmpvlist.get(mm);
							}

							if (bit1 == 0) {
								// double tmp10 = tmp1* (1 - tmpvlist.get(mm));
								// if(Double.isNaN(tmp10))
								// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+(1 - tmpvlist.get(mm)));
								tmp1 *= (1 - tmpvlist.get(mm));

							} else {
								// double tmp11 = tmp1*tmpvlist.get(mm);
								// if(Double.isNaN(tmp11))
								// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+tmpvlist.get(mm));
								tmp1 *= tmpvlist.get(mm);
							}
						}

						v0 += tmp0;
						v1 += tmp1;
					}
				}
				// if(v1 + v0 == 0.0){
				// System.out.println("one 0 detected");
				// alledges.get(j).set_fton(0.0);
				// }
				// else
				// if(Double.isNaN(v1 / (v1 + v0))){
				// System.out.println("find nan , v1 = "+v1+", v0 = "+v0);
				// }
				alledges.get(j).set_fton(v1 / (v1 + v0));
			}
		}
		else{
			// // used to save all the messages from the nodes
			// List<Apfloat> tmpvlist = new ArrayList<>();
			// for (int i = 0; i < nnodes; i++) {
			// 	tmpvlist.add(alledges.get(i).ap_get_ntof());
			// }
			// // System.out.println("tmplist = "+tmpvlist);
			// for (int j = 0; j < nnodes; j++) {
			// 	Apfloat v0 = new Apfloat("0.0", 100);
			// 	Apfloat v1 = new Apfloat("0.0", 100);
			// 	int step = (1 << j);
			// 	int vnum = (1 << nnodes);
			// 	// transform a tensor of nnodes-dimension into a one-dimension vector(two
			// 	// values)
			// 	for (int k = 0; k < vnum; k += 2 * step) {
			// 		for (int o = 0; o < step; o++) {
			// 			int index0 = k + o;
			// 			Apfloat tmp0 = ap_tensor.get(index0);

			// 			int index1 = k + o + step;
			// 			Apfloat tmp1 = ap_tensor.get(index1);
			// 			// get the bit and times the Corresponding message
			// 			for (int mm = 0; mm < nnodes; mm++) {
			// 				int bit0 = index0 % 2;
			// 				index0 /= 2;
			// 				int bit1 = index1 % 2;
			// 				index1 /= 2;

			// 				if (mm == j)
			// 					continue;

			// 				if (bit0 == 0) {
			// 					// double tmp00 = tmp0* (1 - tmpvlist.get(mm));
			// 					// if(Double.isNaN(tmp00))
			// 					// System.out.println("in 0 , tmp0 = "+tmp0+", val = "+(1 - tmpvlist.get(mm)));
			// 					// tmp0 *= (1 - tmpvlist.get(mm));
			// 					tmp0 = tmp0.multiply(new Apfloat("1.0", 100).subtract(tmpvlist.get(mm)));

			// 				} else {
			// 					// double tmp01 = tmp0* tmpvlist.get(mm);
			// 					// if(Double.isNaN(tmp01))
			// 					// System.out.println("in 1 , tmp0 = "+tmp0+", val = "+tmpvlist.get(mm));
			// 					// tmp0 *= tmpvlist.get(mm);
			// 					tmp0 = tmp0.multiply(tmpvlist.get(mm));
			// 				}

			// 				if (bit1 == 0) {
			// 					// double tmp10 = tmp1* (1 - tmpvlist.get(mm));
			// 					// if(Double.isNaN(tmp10))
			// 					// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+(1 - tmpvlist.get(mm)));
			// 					// tmp1 *= (1 - tmpvlist.get(mm));
			// 					tmp1 = tmp1.multiply(new Apfloat("1.0", 100).subtract(tmpvlist.get(mm)));

			// 				} else {
			// 					// double tmp11 = tmp1*tmpvlist.get(mm);
			// 					// if(Double.isNaN(tmp11))
			// 					// System.out.println("in 0 , tmp1 = "+tmp1+", val = "+tmpvlist.get(mm));
			// 					// tmp1 *= tmpvlist.get(mm);
			// 					tmp1 = tmp1.multiply(tmpvlist.get(mm));
			// 				}
			// 			}

			// 			v0 = v0.add(tmp0);
			// 			v1 = v1.add(tmp1);

			// 			// v0 += tmp0;
			// 			// v1 += tmp1;
			// 		}
			// 	}
			// 	// if(v1 + v0 == 0.0){
			// 	// System.out.println("one 0 detected");
			// 	// alledges.get(j).set_fton(0.0);
			// 	// }
			// 	// else
			// 	// if(Double.isNaN(v1 / (v1 + v0))){
			// 	// System.out.println("find nan , v1 = "+v1+", v0 = "+v0);
			// 	// }
			// 	// alledges.get(j).set_fton(v1 / (v1 + v0));
			// 	alledges.get(j).ap_set_fton(v1.divide(v1.add(v0)));
			// }
		}
	}



	public double getProb() {
		boolean hasUNKoperator = false;
		if (ops != null)
			for (String op : ops) {
				for (String unk : unkops) {
					if (op.contentEquals(unk))
						hasUNKoperator = true;
				}
			}
		// if(hasUNKoperator)return MEDIUM;
		// hasUNKoperator = false;
		boolean defv = def.getCurrentValue();
		boolean predv = true;
		boolean usev = true;
		boolean stmtv = stmt.getCurrentValue();
		if (preds != null)
			for (Node p : preds) {
				if (!p.getCurrentValue()) {
					predv = false;
					break;
				}
			}
		if (uses != null)
			for (Node u : uses) {
				if (!u.getCurrentValue()) {
					usev = false;
					break;
				}
			}
		boolean pu = predv && usev;
		if (stmtv) {// if the statement is written correctly.
			if (defv && pu)
				return HIGH;
			if (!defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return HIGH;
			}
			if (!defv && pu)
				return LOW;
			if (defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return LOW;
			}
		} else {
			if (defv && pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return LOW;
			}
			if (!defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return HIGH;
			}
			if (!defv && pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return HIGH;
			}
			if (defv && !pu) {
				if (hasUNKoperator)
					return MEDIUM;
				return LOW;
			}
			// if (!defv) {
			// if (hasUNKoperator)
			// return MEDIUM;
			// return HIGH;
			// }
			// // else: def = true
			// else if (pu) {
			// if (hasUNKoperator)
			// return MEDIUM;
			// return LOW;
			// }
			// // def = true stmt = false use = false
			// return LOW;// TODO should be medium when using certain ops.
		}
		return MEDIUM;
	}

	// public Apfloat ap_getProb() {
	// 	boolean hasUNKoperator = false;
	// 	if (ops != null)
	// 		for (String op : ops) {
	// 			for (String unk : unkops) {
	// 				if (op.contentEquals(unk))
	// 					hasUNKoperator = true;
	// 			}
	// 		}
	// 	// if(hasUNKoperator)return MEDIUM;
	// 	// hasUNKoperator = false;
	// 	boolean defv = def.getCurrentValue();
	// 	boolean predv = true;
	// 	boolean usev = true;
	// 	boolean stmtv = stmt.getCurrentValue();
	// 	if (preds != null)
	// 		for (Node p : preds) {
	// 			if (!p.getCurrentValue()) {
	// 				predv = false;
	// 				break;
	// 			}
	// 		}
	// 	if (uses != null)
	// 		for (Node u : uses) {
	// 			if (!u.getCurrentValue()) {
	// 				usev = false;
	// 				break;
	// 			}
	// 		}
	// 	boolean pu = predv && usev;
	// 	if (stmtv) {// if the statement is written correctly.
	// 		if (defv && pu)
	// 			return ap_HIGH;
	// 		if (!defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_HIGH;
	// 		}
	// 		if (!defv && pu)
	// 			return ap_LOW;
	// 		if (defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_LOW;
	// 		}
	// 	} else {
	// 		if (defv && pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_LOW;
	// 		}
	// 		if (!defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_HIGH;
	// 		}
	// 		if (!defv && pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_HIGH;
	// 		}
	// 		if (defv && !pu) {
	// 			if (hasUNKoperator)
	// 				return ap_MEDIUM;
	// 			return ap_LOW;
	// 		}
	// 		// if (!defv) {
	// 		// if (hasUNKoperator)
	// 		// return MEDIUM;
	// 		// return HIGH;
	// 		// }
	// 		// // else: def = true
	// 		// else if (pu) {
	// 		// if (hasUNKoperator)
	// 		// return MEDIUM;
	// 		// return LOW;
	// 		// }
	// 		// // def = true stmt = false use = false
	// 		// return LOW;// TODO should be medium when using certain ops.
	// 	}
	// 	return ap_MEDIUM;
	// }

	public void print(MyWriter lgr) {

		stmt.print(lgr, "Statement: ");
		if (def != null) {
			lgr.writeln("\tdef:");
			def.print(lgr, "\t\t");
		} else {
			lgr.writeln("\tstmtvalue = " + this.stmtvalue);
		}

		if (uses != null) {
			lgr.writeln("\tuses:");
			for (Node n : uses) {
				n.print(lgr, "\t\t");
			}
		}
		if (preds != null) {
			lgr.writeln("\tpreds:");
			for (Node n : preds) {
				n.print(lgr, "\t\t");
			}
		}
		if (ops != null) {
			lgr.writeln("\tops:");
			for (String eachop : ops) {
				lgr.writeln("\t\t{}", eachop);
			}
		}
	}

	public void print() {
		stmt.print("Statement: ");
		if (def != null) {
			debugLogger.writeln("\tdef:");
			def.print("\t\t");
		} else {
			debugLogger.writeln("\tstmtvalue = " + this.stmtvalue);
		}

		if (uses != null) {
			debugLogger.writeln("\tuses:");
			for (Node n : uses) {
				n.print("\t\t");
			}
		}
		if (preds != null) {
			debugLogger.writeln("\tpreds:");
			for (Node n : preds) {
				n.print("\t\t");
			}
		}
		if (ops != null) {
			debugLogger.writeln("\tops:");
			for (String eachop : ops) {
				debugLogger.writeln("\t\t{}", eachop);
			}
		}
	}
}
