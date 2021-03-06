package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;

//185
public class InvokeinterfaceInst extends OpcodeInst {

	public InvokeinterfaceInst(int _form) {
		super(_form, 0, 0);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		int callindex = getu16bitpara(ci, index);
		ret.append(getmethodinfo(ci, callindex, constp));
		return ret.toString();
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		String traceclass = info.getvalue("callclass");
		String tracemethod = info.getvalue("callname");
		// defs
		int argcnt = OpcodeInst.getArgNumByDesc(info.getvalue("calltype"));

		// An extra argument: caller:object->callee:this
		argcnt++;
		// collect arguments
		for (int i = 0; i < argcnt; i++) {
			Node node = graph.getRuntimeStack().pop();
			usenodes.add(node);
		}

		// if not traced
		if (!graph.isTraced(traceclass)) {
			defnode = graph.addNewStackNode(stmt);
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
			return;
		}

		// switch stack frame
		graph.pushStackFrame(traceclass, tracemethod);

		// static arguments starts with 0
		int paravarindex = 0;
		// non-static
		// paravarindex = 1;
		for (int i = 0; i < argcnt; i++) {
			List<Node> adduse = new ArrayList<>();
			Node curArgument = usenodes.get(argcnt - i - 1);
			adduse.add(curArgument);

			Node defnode = graph.addNewVarNode(paravarindex, stmt, traceclass, tracemethod);

			graph.buildFactor(defnode, prednodes, adduse, null, stmt);

			paravarindex += curArgument.getSize();
		}
	}
}
