package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//189
public class AnewArrayInst extends OpcodeInst {

	public AnewArrayInst(int _form) {
		super(_form, 1, 1);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}
}
