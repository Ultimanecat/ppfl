package ppfl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ppfl.instrumentation.TraceDomain;

public class TraceChunk {

  private class MatchedPoint {
    public int id;
    // 0=normal return 1=catched 2=no match
    public int type;

    public MatchedPoint(int id, int type) {
      this.id = id;
      this.type = type;
    }
  }

  public List<String> traces;
  public boolean testpass;
  String fullname;
  boolean pruned = false;
  public List<ParseInfo> parsedTraces = new ArrayList<>();

  public TraceChunk(String fullname) {
    traces = new ArrayList<>();
    this.fullname = fullname;
  }

  public String getTestName() {
    return fullname.split("::")[1];
  }

  public void add(String s) {
    this.traces.add(s);
  }

  public void addSetUp(List<String> s) {
    this.traces.addAll(s);
  }

  public void pruneInit(Set<TraceDomain> TracedDomain) {
    for (int i = 0; i < traces.size(); i++) {
      ParseInfo parsed = null;
      try {
        parsed = new ParseInfo(traces.get(i));
      } catch (Exception e) {
        System.out.println(this.fullname + " " + i);
        System.out.println(traces.get(i));
        throw (e);
      }
      if (parsed.form == 179)// putstatic
        parsedTraces.add(parsed);
    }
    this.traces.clear();
  }

  public void prune(Set<TraceDomain> TracedDomain) {
    // List<Integer> toadd = new ArrayList<>();
    for (int i = 0; i < traces.size(); i++) {
      ParseInfo parsed = null;
      try {
        parsed = new ParseInfo(traces.get(i));
      } catch (Exception e) {
        System.out.println(this.fullname + " " + i);
        System.out.println(traces.get(i));
        throw (e);
      }
      parsedTraces.add(parsed);
      // // remove normal return msg.
      // if (parsed.isReturnMsg) {
      // continue;
      // }
      // parsedTraces.add(parsed);
      // // toadd.add(i);
      // if (parsed.isUntracedInvoke(tracedClass)) {
      // MatchedPoint mPoint = matchLastReturnOrCatch(i, parsed, tracedClass);
      // if (mPoint.type == 0) {
      // i = mPoint.id;
      // } else if (mPoint.type == 1) {
      // ParseInfo matchedCatch = new ParseInfo(traces.get(mPoint.id));
      // parsedTraces.add(matchedCatch);
      // parsed.setException();

      // System.out.println("Catch-clause Matched, " + i + " " + mPoint.id);
      // matchedCatch.debugprint();
      // i = mPoint.id;
      // } else if (mPoint.type == 2) {
      // parsed.setNoMatch();
      // System.out.println("No Match, " + i);
      // parsed.debugprint();
      // }
      // }
    }
    this.traces.clear();
  }

  private MatchedPoint matchLastReturnOrCatch(int pos, ParseInfo toMatch, Set<String> tracedClass) {
    int onlyret = matchLastReturnPoint(pos, toMatch, tracedClass, false);
    if (onlyret != -1) {
      return new MatchedPoint(onlyret, 0);
    }
    int catched = matchLastReturnPoint(pos, toMatch, tracedClass, true);
    if (catched != -1) {
      toMatch.setException();
      return new MatchedPoint(catched, 1);
    } else {
      toMatch.setNoMatch();
      return new MatchedPoint(-1, 2);
    }

  }

  private int matchLastReturnPoint(int pos, ParseInfo toMatch, Set<String> tracedClass, boolean matchCatch) {
    for (int i = pos + 1; i < traces.size(); i++) {
      ParseInfo cur = new ParseInfo(traces.get(i));
      if (toMatch.matchReturn(cur, matchCatch)) {
        // System.out.println(pos + " " + i);
        return i;
      }
      if (cur.isUntracedInvoke(tracedClass)) {
        int j = matchLastReturnPoint(i, cur, tracedClass, matchCatch);
        if (j == -1) {
          // System.out.println(traces.get(i));
          return -1;
        }
        i = j;
      }
    }
    return -1;
  }

}
