package ppfl.instrumentation.opcode;

import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

//165-166, type reference is object?
public class If_acmpInst extends OpcodeInst {

	public If_acmpInst(int _form) {
		super(_form, 0, 2);
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		return ret.toString();
	}

	// @Override
	// public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp,
	// CallBackIndex cbi)
	// throws BadBytecode {
	// int instpos = ci.insertGap(4);// the gap must be long enough for the
	// following instrumentation
	// ci.writeByte(184, instpos);// invokestatic
	// ci.write16bit(cbi.tsindex_object, instpos + 1);
	// }

}
