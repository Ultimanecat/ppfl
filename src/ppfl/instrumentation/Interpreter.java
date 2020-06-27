package ppfl.instrumentation;

import javassist.bytecode.Mnemonic;
import ppfl.instrumentation.opcode.IconstInst;
import ppfl.instrumentation.opcode.LcmpInst;
import ppfl.instrumentation.opcode.FcmpInst;
import ppfl.instrumentation.opcode.DcmpInst;
import ppfl.instrumentation.opcode.IfInst;
import ppfl.instrumentation.opcode.If_icmpInst;
import ppfl.instrumentation.opcode.If_acmpInst;
import ppfl.instrumentation.opcode.GotoInst;
import ppfl.instrumentation.opcode.JsrInst;
import ppfl.instrumentation.opcode.RetInst;
import ppfl.instrumentation.opcode.OpcodeInst;
import ppfl.instrumentation.opcode.OpcodeInst.paratype;

/*bytecode reference:
https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5*/
public class Interpreter {
	static OpcodeInst map[] = new OpcodeInst[256];
	// loads
	static int[] loadvar_para = { 21, 22, 23, 24, 25 };// load 1 var, index at byte(pos + 1)
	static int[] loadvar_opcode = { 26, 42 };// load 1 var, index based on (opcode-base_opcode)
	static int[] loadvar_opcode_cnt = { 3, 3 };// the length of loadvar_opcode
	static int[] loadconst_para = { 16 };// load 1 const,value at byte(pos + 1)
	static int[] loadconst_para2 = { 17 };// load 2 const value = [pos+1]<<8 | [pos+2]
	static int[] loadconst_pool = { 18, 19, 20 };// TODO constant pool
	static int[] loadconst_opcode = { 2, 9, 11, 14, 30, 34, 38 };// load 1 const, index based on (opcode-base_opcode)
	static int[] loadconst_opcode_cnt = { 6, 1, 2, 1, 3, 3, 3 };// the length of loadconst_opcode
	static int[] getstatic = { 178 };
	static int[] getfield = { 180 };
	// stores
	static int[] storevar_para = { 54, 55, 56, 57, 58 };// store(pop) 1 value at localvar, index at byte(pos + 1)
	static int[] storevar_opcode = { 59, 63, 67, 71, 75 };// store 1 value at localvar, index based on opcode
	static int[] storevar_opcode_cnt = { 3, 3, 3, 3, 3 };// length
	static int[] putstatic = { 179 };
	static int[] putfield = { 181 };
	// pops
	static int[] pop1 = { 87 };// pop 1
	static int[] pop2 = { 88 };// pop 2
	// branching
	static int[] branch_pop1 = { 153, 154, 155, 156, 157, 158, 198, 199 };// branching that pops 1
	static int[] branch_pop2 = { 159, 160, 161, 162, 163, 164, 165, 166 };// pops 2
	// controls
	static int[] neglect = { 167, 169, 200 };// do nothing to the stack
	static int[] ret0 = { 177 };// return void. pop all things in current frame.
	static int[] ret1 = { 172, 173, 174, 175, 176 };// return 1 value. push 1 after pop all
	static int[] jsr = { 168, 201 };// jsr(push 1) ..finally clausef
	// invokes
	static int[] invokes = { 182, 183, 184, 185, 186 };// invokes.
	// arith
	// pop2 push1 arith
	static int[] pop2push1 = { 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113,
			114, 115, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 148, 149, 150, 151, 152 };

	static int[] mods = { 112, 113, 114, 115 };// TODO mods. Where 112-113 NatMod,114-115 RealMod
	static int[] pop1push1 = { 116, 117, 118, 119, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146,
			147 };// pop1 push1 (negate\cast)
	static int[] para1const1 = { 132 };// special(iinc)
	// array operations
	static int[] arrayloads = { 46, 47, 48, 49, 50, 51, 52, 53 };// pop2(arrayref,index) push1 load element value from
																	// // array
	static int[] arraystores = { 79, 80, 81, 82, 83, 84, 85, 86 };// pop3 (arrayref,index,value)
	static int[] newarrays = { 188, 189 };// TODO
	static int[] multinewarray = { 197 };// TODO
	// stack manipulations (Not Implemented)
	static int[] dups = { 89, 90, 91, 92, 93, 94 };
	static int[] swaps = { 95 };

	static void printopcodes() {
		init();
		for (int i = 0; i < Mnemonic.OPCODE.length; i++) {
			if (map[i] == null) {
				System.out.print("Not implemented ");
				System.out.println(i + " " + Mnemonic.OPCODE[i]);
			} else {
				// if(map[i].pushnum != 1)continue;
				System.out.print(i + " " + Mnemonic.OPCODE[i] + " ");
				System.out.println(map[i].getinst(null, 0, null));
			}
		}
	}

	public static void main(String args[]) {
		printopcodes();
	}

	static void init() {
		// construct inst map based on different opcode types.
		// nop
		map[0] = new OpcodeInst(0, 0, 0);
		// aconst_null
		map[1] = new OpcodeInst(1, 1, 0);
		map[1].setPush(paratype.CONST, "NULL");
		// loads
		for (int id : loadvar_para) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPara(0, paratype.PARAVAR);
			map[id].setPush(paratype.PARAVAR, null);
		}
		for (int id : loadconst_para) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPara(0, paratype.PARACONST);
			map[id].setPush(paratype.PARACONST, null);
		}
		for (int id : loadconst_para2) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPara(0, paratype.PARACONST);
			map[id].setPara(1, paratype.PARACONST);
			map[id].setPush(paratype.PARACONST, null);
		}
		assert (loadvar_opcode.length == loadvar_opcode_cnt.length);
		for (int i = 0; i < loadvar_opcode.length; i++) {
			for (int j = 0; j <= loadvar_opcode_cnt[i]; j++) {
				int id = loadvar_opcode[i] + j;
				map[id] = new OpcodeInst(id, 1, 0);
				map[id].setPush(paratype.VAR, String.valueOf(j));
			}
		}
		assert (loadconst_opcode.length == loadconst_opcode_cnt.length);
		for (int i = 0; i < loadconst_opcode.length; i++) {
			for (int j = 0; j <= loadconst_opcode_cnt[i]; j++) {
				int id = loadconst_opcode[i] + j;
				map[id] = new OpcodeInst(id, 1, 0);
				int bias = i == 0 ? j - 1 : j;// iconst_<n> started from m1
				map[id].setPush(paratype.CONST, String.valueOf(bias));
			}
		}
		// register IconstInst here
		for (int i = 2; i <= 8; i++) {
			map[i] = new IconstInst(i, i - 3);
		}

		for (int id : loadconst_pool) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPush(paratype.POOL, null);
			map[id].setPara(0, paratype.POOL);
		}
		// stores
		for (int id : storevar_para) {
			map[id] = new OpcodeInst(id, 0, 1);
			map[id].setStore(paratype.PARAVAR, null);
		}
		assert (storevar_opcode.length == storevar_opcode_cnt.length);
		for (int i = 0; i < storevar_opcode.length; i++) {
			for (int j = 0; j <= storevar_opcode_cnt[i]; j++) {
				int id = storevar_opcode[i] + j;
				map[id] = new OpcodeInst(id, 0, 1);
				map[id].setStore(paratype.VAR, String.valueOf(j));
			}
		}
		// pops
		for (int id : pop1) {
			map[id] = new OpcodeInst(id, 0, 1);
		}
		for (int id : pop2) {
			map[id] = new OpcodeInst(id, 0, 2);
		}
		// branching TODO merge defs from both branch
		for (int id : branch_pop1) {
			map[id] = new OpcodeInst(id, 0, 1);
		}
		for (int id : branch_pop2) {
			map[id] = new OpcodeInst(id, 0, 2);
		}
		// controls
		for (int id : neglect) {
			map[id] = new OpcodeInst(id, 0, 0);
		}
		for (int id : ret0) {
			map[id] = new OpcodeInst(id, 0, -1);
		}
		for (int id : ret1) {// return value will be printed after called.
			map[id] = new OpcodeInst(id, 0, -1);
		}
		for (int id : jsr) {
			map[id] = new OpcodeInst(id, 1, 0);// TODO
		}
		// invokes
		for (int id : invokes) {
			map[id] = new OpcodeInst(id, 0, 0);
			map[id].setinvoke();
			// TODO set invoke
		}
		// arith
		for (int id : pop2push1) {
			map[id] = new OpcodeInst(id, 1, 2);
		}
		for (int id : pop1push1) {
			map[id] = new OpcodeInst(id, 1, 1);
		}
		for (int id : para1const1) {
			map[id] = new OpcodeInst(id, 0, 0);
			map[id].setPara(0, paratype.PARAVAR);
			map[id].setPara(1, paratype.PARACONST);
		}
		// array operation
		for (int id : arrayloads) {// TODO different this from pop2push1 ariths if needed.
			map[id] = new OpcodeInst(id, 1, 2);
		}
		for (int id : arraystores) {// TODO track arrayref in stack
			map[id] = new OpcodeInst(id, 0, 3);
		}
		// static & field get\put
		for (int id : getstatic) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPush(paratype.STATIC, null);
			map[id].setPara(0, paratype.STATIC);
			map[id].setPara(1, paratype.STATIC);
		}
		for (int id : putstatic) {
			map[id] = new OpcodeInst(id, 0, 1);
			map[id].setStore(paratype.STATIC, null);
			map[id].setPara(0, paratype.STATIC);
			map[id].setPara(1, paratype.STATIC);
		}
		for (int id : getfield) {
			map[id] = new OpcodeInst(id, 1, 1);
			map[id].setPush(paratype.FIELD, null);
			map[id].setPara(0, paratype.FIELD);
			map[id].setPara(1, paratype.FIELD);
		}
		for (int id : putfield) {
			map[id] = new OpcodeInst(id, 0, 2);
			map[id].setStore(paratype.FIELD, null);
			map[id].setPara(0, paratype.FIELD);
			map[id].setPara(1, paratype.FIELD);
		}
		
		map[148] = new LcmpInst(148);
		for (int i = 149; i <= 150; i++) {
			map[i] = new FcmpInst(i);
		}
		for (int i = 151; i <= 152; i++) {
			map[i] = new DcmpInst(i);
		}
		for (int i = 153; i <= 158; i++) {
			map[i] = new IfInst(i);
		}
		for (int i = 159; i <= 164; i++) {
			map[i] = new If_icmpInst(i);
		}
		for (int i = 165; i <= 166; i++) {
			map[i] = new If_acmpInst(i);
		}
		map[167] = new GotoInst(167);
		map[168] = new JsrInst(168);
		map[169] = new RetInst(169);
	}
}
