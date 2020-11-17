package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;

//132
public class IincInst extends OpcodeInst {

	boolean isIntInst = true;
	int loadedconst;

	public IincInst(int _form) {
		super(_form, 0, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		super.buildtrace(graph);

		int varindex = info.getintvalue("VAR");
		// int incconst = info.getintvalue("CONST");

		usenodes.add(graph.getLoadNodeAsUse(varindex));
		defnode = graph.addNewVarNode(varindex, stmt);

		// build factor.
		if (defnode != null) {
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		ret.append(",VAR=" + getpara(ci, index, 1));
		ret.append(",CONST=" + getpara(ci, index, 2));
		return ret.toString();
	}

}