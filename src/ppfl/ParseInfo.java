package ppfl;

import java.util.HashMap;
import java.util.Map;

public class ParseInfo {

	Map<String, String> tracemap;
	public String traceclass;
	public String tracemethod;
	public int linenumber;
	public int byteindex;
	public int form;
	public String opcode;

	public ParseInfo() {

	}

	public ParseInfo(String trace) {
		tracemap = new HashMap<String, String>();

		String[] split = trace.split(",");
		for (String instinfo : split) {
			String[] splitinstinfo = instinfo.split("=");
			String infotype = splitinstinfo[0];
			String infovalue = splitinstinfo[1];
			tracemap.put(infotype, infovalue);
		}
		String[] lineinfos = this.getvalue("lineinfo").split("#");
		this.traceclass = lineinfos[0];
		this.tracemethod = lineinfos[1];
		this.linenumber = Integer.valueOf(lineinfos[2]);
		this.byteindex = Integer.valueOf(lineinfos[3]);
		String[] opcodeinfos = this.getvalue("opcode").split("()");
		this.form = Integer.valueOf(opcodeinfos[0]);
		this.opcode = opcodeinfos[1];
	}

	public String getvalue(String stype) {
		return this.tracemap.get(stype);
	}

	public int getintvalue(String stype) {
		return Integer.valueOf(this.tracemap.get(stype));
	}

}