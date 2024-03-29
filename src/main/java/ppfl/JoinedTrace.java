package ppfl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ppfl.instrumentation.TraceDomain;

public class JoinedTrace {

  private Set<String> d4jMethodNames = new HashSet<>();
  private Set<String> d4jTriggerTestNames = new HashSet<>();
  private Set<TraceDomain> tracedDomain = new HashSet<>();
  private static final double MAX_FILE_LIMIT = 2e8;// threshold for maximum pass trace : 200M

  public List<TraceChunk> traceList = new ArrayList<>();
  public TraceChunk staticInits = new TraceChunk("<init>");

  private List<String> setUpTraces = null;

  public void sortChunk() {
    this.traceList.sort(new Comparator<TraceChunk>() {
      @Override
      public int compare(TraceChunk arg0, TraceChunk arg1) {
        if (arg0.testpass == arg1.testpass) {
          return arg0.parsedTraces.size() - arg1.parsedTraces.size();
        } else {
          return arg0.testpass ? 1 : -1;
        }
      }
    });
  }

  private void addTraceChunk(String fullname) {
    TraceChunk toadd = new TraceChunk(fullname);
    toadd.testpass = getD4jTestState(fullname);
    traceList.add(toadd);
    if (setUpTraces != null) {
      toadd.addSetUp(setUpTraces);
      setUpTraces = null;
    }
  }

  private void addSingleTrace(String trace) {
    if (setUpTraces != null) {
      setUpTraces.add(trace);
      return;
    }
    if (!traceList.isEmpty())
      traceList.get(traceList.size() - 1).add(trace);
  }

  private boolean isD4jTestMethod(String longname) {
    // String longname = className + "::" + methodName;
    // System.out.println(longname);
    return d4jMethodNames.contains(longname);
  }

  private boolean isSetUp(String longname) {
    return longname.endsWith("::setUp");
  }

  private void addSetUp(String t) {
    this.setUpTraces = new ArrayList<>();
    // this.setUpTraces.add(t);
  }

  private boolean getD4jTestState(String fullname) {
    assert (d4jMethodNames.contains(fullname));
    return !d4jTriggerTestNames.contains(fullname);
  }

  public JoinedTrace(Set<String> d4jMethodNames, Set<String> d4jTriggerTestNames, Set<TraceDomain> tracedDomain) {
    this.d4jMethodNames = d4jMethodNames;
    this.d4jTriggerTestNames = d4jTriggerTestNames;
    this.tracedDomain = tracedDomain;
  }

  public void parseSourceFolder(String path) {
    File folder = new File(path);
    File[] fs = folder.listFiles();
    for (File f : fs) {
      if (!f.getName().endsWith(".init.log"))
        continue;
      if (f.length() < MAX_FILE_LIMIT) {
        parseStaticFile(f);
      }
    }
    parseStaticInfo();
  }

  private void parseStaticFile(File f) {
    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
      String t = null;
      while ((t = reader.readLine()) != null) {
        if (t.isEmpty()) {
          continue;
        }
        staticInits.add(t);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void parseFolder(String path) {
    File folder = new File(path);
    File[] fs = folder.listFiles();
    for (File f : fs) {
      if (f.getName().equals("all.log"))
        continue;
      parseSepFile(f, f.getName());
    }
    parseToInfo();
  }

  private void parseSepFile(File f, String name) {
    String suffix = ".log";
    if (name.endsWith(suffix))
      name = name.substring(0, name.length() - suffix.length());
    int index = name.lastIndexOf('.');
    String fullname = name.substring(0, index) + "::" + name.substring(index + 1);
    if (getD4jTestState(fullname) && f.length() > MAX_FILE_LIMIT) {
      return;
    }
    // Lang-6
    // if (!fullname.endsWith("testRootEndpoints"))
    // return;
    this.addTraceChunk(fullname);
    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
      String delimiterPrefix = "###";
      String t = null;
      while ((t = reader.readLine()) != null) {
        if (t.isEmpty()) {
          continue;
        }
        if (t.startsWith(delimiterPrefix)) {
          // System.out.println(t);
          t = t.substring(delimiterPrefix.length());
          // if (isSetUp(t)) {
          // this.addSetUp(t);
          // }
          if (isD4jTestMethod(t)) {
            // this.addTraceChunk(t);
          }
          if (t.startsWith("RET@")) {
            this.addSingleTrace(t);
          }
        } else {
          this.addSingleTrace(t);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // this could be memory-unfriendly.
  // some pruning will be done here.
  public void parseFile(String tracefilename) {
    this.traceList = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(tracefilename))) {
      String t = null;
      String delimiterPrefix = "###";
      int i = 0;
      while ((t = reader.readLine()) != null) {
        // System.out.println(t.length());
        ++i;
        // if (t.endsWith("AggregateTrans")) {
        // System.out.println(i);
        // System.out.println(t);
        // }
        if (t.isEmpty()) {
          continue;
        }
        if (t.startsWith(delimiterPrefix)) {
          // System.out.println(t);
          t = t.substring(delimiterPrefix.length());
          // String[] splt = t.split("::");
          if (isSetUp(t)) {
            this.addSetUp(t);
          }
          if (isD4jTestMethod(t)) {
            this.addTraceChunk(t);
          }
          if (t.startsWith("RET@")) {
            this.addSingleTrace(t);
          }
        } else {
          this.addSingleTrace(t);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    parseToInfo();
  }

  private void parseStaticInfo() {
    try {
      this.staticInits.pruneInit(this.tracedDomain);
    } catch (Exception e) {
      System.err.println("prune at <init> failed");
    }
  }

  private void parseToInfo() {
    // prune
    Iterator<TraceChunk> it = this.traceList.iterator();
    while (it.hasNext()) {
      TraceChunk chunk = it.next();
      try {
        chunk.prune(this.tracedDomain);
      } catch (Exception e) {
        System.err.println("prune at " + chunk.fullname + " failed");
        it.remove();
        // this.traceList.remove(chunk);
      }
    }
  }
}
