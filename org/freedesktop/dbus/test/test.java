package org.freedesktop.dbus.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusException;
import org.freedesktop.dbus.DBusExecutionException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;

import org.freedesktop.DBus.Error.UnknownObject;
import org.freedesktop.DBus.Error.ServiceUnknown;
import org.freedesktop.DBus.Peer;
import org.freedesktop.DBus.Introspectable;

class testclass implements TestRemoteInterface, TestRemoteInterface2, TestSignalInterface
{
   private DBusConnection conn;
   public testclass(DBusConnection conn)
   {
      this.conn = conn;
   }
   public void waitawhile()
   {
      System.out.println("Sleeping.");
      try {
         Thread.sleep(5000);
      } catch (InterruptedException Ie) {}
      System.out.println("Done sleeping.");
   }
   public <A> TestTuple<String, Integer, Boolean> show(A in)
   {
      System.out.println("Showing Stuff: "+in.getClass()+"("+in+")");
      if (!(in instanceof Integer) || ((Integer) in).intValue() != 234)
         test.fail("show received the wrong arguments");
      DBusCallInfo info = DBusConnection.getCallInfo();
      return new TestTuple<String, Integer, Boolean>(info.getSource(), 28165, true);
   }
   @SuppressWarnings("unchecked")
   public <T> T dostuff(TestStruct foo)
   {
      System.out.println("Doing Stuff "+foo);
      System.out.println(" -- ("+foo.a.getClass()+", "+foo.b.getClass()+", "+foo.c.getClass()+")");
      if (!(foo instanceof TestStruct) ||
            !(foo.a instanceof String) ||
            !(foo.b instanceof UInt32) ||
            !(foo.c instanceof Variant) ||
            !"bar".equals(foo.a) ||
            foo.b.intValue() != 52 ||
            !(foo.c.getValue() instanceof Boolean) ||
            ((Boolean) foo.c.getValue()).booleanValue() != true)
         test.fail("dostuff received the wrong arguments");
      return (T) foo.c.getValue();
   }
   /** Local classes MUST implement this to return false */
   public boolean isRemote() { return false; }
   /** The method we are exporting to the Bus. */
   public List<Integer> sampleArray(List<String> ss, Integer[] is, long[] ls)
   {
      System.out.println("Got an array:");
      for (String s: ss)
         System.out.println("--"+s);
      if (ss.size()!= 5 ||
            !"hi".equals(ss.get(0)) ||
            !"hello".equals(ss.get(1)) ||
            !"hej".equals(ss.get(2)) ||
            !"hey".equals(ss.get(3)) ||
            !"aloha".equals(ss.get(4)))
         test.fail("sampleArray, String array contents incorrect");
      System.out.println("Got an array:");
      for (Integer i: is)
         System.out.println("--"+i);
      if (is.length != 4 ||
            is[0].intValue() != 1 ||
            is[1].intValue() != 5 ||
            is[2].intValue() != 7 ||
            is[3].intValue() != 9)
         test.fail("sampleArray, Integer array contents incorrect");
      System.out.println("Got an array:");
      for (long l: ls)
         System.out.println("--"+l);
      if (ls.length != 4 ||
            ls[0] != 2 ||
            ls[1] != 6 ||
            ls[2] != 8 ||
            ls[3] != 12)
         test.fail("sampleArray, Integer array contents incorrect");
      Vector<Integer> v = new Vector<Integer>();
      v.add(-1);
      v.add(-5);
      v.add(-7);
      v.add(-12);
      v.add(-18);
      return v;
   }
   public String getName()
   {
      return "This Is A Name!!";
   }
   public boolean check()
   {
      System.out.println("Being checked");
      return false;
   }
   public <T> int frobnicate(List<Long> n, Map<String,Map<UInt16,Short>> m, T v)
   {
      if (null == n)
         test.fail("List was null");
      if (n.size() != 3)
         test.fail("List was wrong size (expected 3, actual "+n.size()+")");
      if (n.get(0) != 2L ||
          n.get(1) != 5L ||
          n.get(2) != 71L)
         test.fail("List has wrong contents");
      if (!(v instanceof Integer))
         test.fail("v not an Integer");
      if (((Integer) v) != 13)
         test.fail("v is incorrect");
      if (null == m)
         test.fail("Map was null");
      if (m.size() != 1)
         test.fail("Map was wrong size");
      if (!m.keySet().contains("stuff"))
         test.fail("Incorrect key");
      Map<UInt16,Short> mus = m.get("stuff");
      if (null == mus)
         test.fail("Sub-Map was null");
      if (mus.size() != 3)
         test.fail("Sub-Map was wrong size");
      if (!(new Short((short)5).equals(mus.get(new UInt16(4)))))
         test.fail("Sub-Map has wrong contents");
      if (!(new Short((short)6).equals(mus.get(new UInt16(5)))))
         test.fail("Sub-Map has wrong contents");
      if (!(new Short((short)7).equals(mus.get(new UInt16(6)))))
         test.fail("Sub-Map has wrong contents");
      return -5;
   }
   public DBusInterface getThis(DBusInterface t)
   {
      if (!t.equals(this))
         test.fail("Didn't get this properly");
      return this;
   }
   public void throwme() throws TestException
   {
      throw new TestException("test");
   }
   public void testSerializable(byte b, TestSerializable s, int i)
   {
      System.out.println("Recieving TestSerializable: "+s);
      if (  b != 12
         || i != 13
         || !(s.getInt() == 1)
         || !(s.getString().equals("woo"))
         || !(s.getVector().size() == 3)
         || !((Integer) s.getVector().get(0) == 1)
         || !((Integer) s.getVector().get(1) == 2)
         || !((Integer) s.getVector().get(2) == 3)    )
         test.fail("Error in recieving custom synchronisation");
   }
   public String recursionTest()
   {
      try {
         TestRemoteInterface tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.Test", "/Test", TestRemoteInterface.class);
         return tri.getName();
      } catch (DBusException DBe) {
         test.fail("Failed with error: "+DBe);
         return "";
      }
   }
   public int overload(String s)
   {
      return 1;
   }
   public int overload(byte b)
   {
      return 2;
   }
   public int overload()
   {
      DBusCallInfo info = DBusConnection.getCallInfo();
      if ("org.freedesktop.dbus.test.TestRemoteInterface2".equals(info.getInterface()))
         return 3;
      else if ("org.freedesktop.dbus.test.TestRemoteInterface".equals(info.getInterface()))
         return 4;
      else
         return -1;
   }
}

/**
 * Typed signal handler
 */
class signalhandler implements DBusSigHandler<TestSignalInterface.TestSignal>
{
   /** Handling a signal */
   public void handle(TestSignalInterface.TestSignal t)
   {
      System.out.println("SignalHandler 1 Running");
      System.out.println("string("+t.value+") int("+t.number+")");
      if (!"Bar".equals(t.value) || !(new UInt32(42)).equals(t.number))
         test.fail("Incorrect TestSignal parameters");
   }
}

/**
 * Untyped signal handler
 */
class arraysignalhandler implements DBusSigHandler
{
   /** Handling a signal */
   public void handle(DBusSignal s)
   {
      TestSignalInterface.TestArraySignal t = (TestSignalInterface.TestArraySignal) s;
      System.out.println("SignalHandler 2 Running");
      System.out.println("Got a test array signal with Parameters: ");
      for (String str: t.v.a)
         System.out.println("--"+str);
      System.out.println(t.v.b.getType());
      System.out.println(t.v.b.getValue());
      if (!(t.v.b.getValue() instanceof UInt64) ||
            567L != ((UInt64) t.v.b.getValue()).longValue() ||
            t.v.a.size() != 5 ||
            !"hi".equals(t.v.a.get(0)) ||
            !"hello".equals(t.v.a.get(1)) ||
            !"hej".equals(t.v.a.get(2)) ||
            !"hey".equals(t.v.a.get(3)) ||
            !"aloha".equals(t.v.a.get(4)))
         test.fail("Incorrect TestArraySignal parameters");
   }
}

/**
 * handler which should never be called
 */
class badarraysignalhandler implements DBusSigHandler
{
   /** Handling a signal */
   public void handle(DBusSignal s)
   {
      test.fail("This signal handler shouldn't be called");
   }
}

/**
 * This is a test program which sends and recieves a signal, implements, exports and calls a remote method.
 */
public class test
{
   public static void fail(String message)
   {
      System.err.println("Test Failed: "+message);
      if (null != conn) conn.disconnect();
      System.exit(1);
   }
   static DBusConnection conn = null;
   @SuppressWarnings("unchecked")
   public static void main(String[] args) 
   { try {
      System.out.println("Creating Connection");
      conn = DBusConnection.getConnection(DBusConnection.SESSION);
      
      System.out.println("Registering Name");
      conn.registerService("foo.bar.Test");
      
      System.out.print("Listening for signals...");
      try {
         /** This registers an instance of the test class as the signal handler for the TestSignal class. */
         conn.addSigHandler(TestSignalInterface.TestSignal.class, new signalhandler());
         conn.addSigHandler(TestSignalInterface.TestSignal.class, new signalhandler());
         conn.addSigHandler(TestSignalInterface.TestArraySignal.class, new arraysignalhandler());
         badarraysignalhandler bash = new badarraysignalhandler();
         conn.addSigHandler(TestSignalInterface.TestSignal.class, bash);
         conn.removeSigHandler(TestSignalInterface.TestSignal.class, bash);
         System.out.println("done");
      } catch (DBusException DBe) {
         test.fail("Failed to add handlers");
      }
      
      System.out.println("Listening for Method Calls");
      testclass tclass = new testclass(conn);
      /** This exports an instance of the test class as the object /Test. */
      conn.exportObject("/Test", tclass);
      
      System.out.println("Sending Signal");
      /** This creates an instance of the Test Signal, with the given object path, signal name and parameters, and broadcasts in on the Bus. */
      conn.sendSignal(new TestSignalInterface.TestSignal("/foo/bar/com/Wibble", "Bar", new UInt32(42)));
      
      System.out.println("Getting our introspection data");
      /** This gets a remote object matching our service name and exported object path. */
      Introspectable intro = (Introspectable) conn.getRemoteObject("foo.bar.Test", "/", Introspectable.class);
      /** Get introspection data */
      String data = intro.Introspect();
      if (null == data || !data.startsWith("<!DOCTYPE"))
         fail("Introspection data invalid");
      System.out.println("Got Introspection Data: \n"+data);
      
      System.out.println("Pinging ourselves");
      /** This gets a remote object matching our service name and exported object path. */
      Peer peer = (Peer) conn.getRemoteObject("foo.bar.Test", "/Test", Peer.class);
      /** Call ping. */
      for (int i = 0; i < 10; i++) {
         long then = System.currentTimeMillis();
         peer.Ping();
         long now = System.currentTimeMillis();
         System.out.println("Ping returned in "+(now-then)+"ms.");
      }
      
      System.out.println("Calling Method0/1");
      /** This gets a remote object matching our service name and exported object path. */
      TestRemoteInterface tri = (TestRemoteInterface) conn.getPeerRemoteObject("foo.bar.Test", "/Test", TestRemoteInterface.class);
      System.out.println("Got Remote Object: "+tri);
      /** Call the remote object and get a response. */
      String rname = tri.getName();
      System.out.println("Got Remote Name: "+rname);
      if (!"This Is A Name!!".equals(rname))
         fail("getName return value incorrect");
      System.out.println("sending it to sleep");
      tri.waitawhile();
      System.out.println("frobnicating");
      List<Long> ls = new Vector<Long>();
      ls.add(2L);
      ls.add(5L);
      ls.add(71L);
      Map<UInt16,Short> mus = new HashMap<UInt16,Short>();
      mus.put(new UInt16(4), (short) 5);
      mus.put(new UInt16(5), (short) 6);
      mus.put(new UInt16(6), (short) 7);
      Map<String,Map<UInt16,Short>> msmus = new HashMap<String,Map<UInt16,Short>>();
      msmus.put("stuff", mus);
      int rint = tri.frobnicate(ls, msmus, 13);
      if (-5 != rint)
         fail("frobnicate return value incorrect");
 
      /** call something that throws */
      try {
         System.out.println("Throwing stuff");
         tri.throwme();
         test.fail("Method Execution should have failed");
      } catch (TestException Te) {
         System.out.println("Remote Method Failed with: "+Te.getClass().getName()+" "+Te.getMessage());
         if (!Te.getMessage().equals("test"))
            test.fail("Error message was not correct");
      }
     
      /** Try and call an invalid remote object */
      try {
         System.out.println("Calling Method2");
         tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.NotATest", "/Moofle", TestRemoteInterface.class);
         System.out.println("Got Remote Name: "+tri.getName());
         test.fail("Method Execution should have failed");
      } catch (ServiceUnknown SU) {
         System.out.println("Remote Method Failed with: "+SU.getClass().getName()+" "+SU.getMessage());
         if (!SU.getMessage().equals("The name foo.bar.NotATest was not provided by any .service files"))
            test.fail("Error message was not correct");
      }
      
      /** Try and call an invalid remote object */
      try {
         System.out.println("Calling Method3");
         tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.Test", "/Moofle", TestRemoteInterface.class);
         System.out.println("Got Remote Name: "+tri.getName());
         test.fail("Method Execution should have failed");
      } catch (UnknownObject UO) {
         System.out.println("Remote Method Failed with: "+UO.getClass().getName()+" "+UO.getMessage());
         if (!UO.getMessage().equals("/Moofle is not an object provided by this service."))
            test.fail("Error message was not correct");
      }

      System.out.println("Calling Method4/5/6/7");
      /** This gets a remote object matching our service name and exported object path. */
      TestRemoteInterface2 tri2 = (TestRemoteInterface2) conn.getRemoteObject("foo.bar.Test", "/Test", TestRemoteInterface2.class);
      /** Call the remote object and get a response. */
      TestTuple<String, Integer, Boolean> rv = tri2.show(234);
      System.out.println("Show Response = "+rv);
      if (!":1.0".equals(rv.a) ||
            28165 != rv.b.intValue() ||
            true != rv.c.booleanValue())
         fail("show return value incorrect");

      
      System.out.println("Doing stuff asynchronously");
      DBusAsyncReply<Boolean> stuffreply = (DBusAsyncReply<Boolean>) conn.callMethodAsync(tri2, "dostuff", new TestStruct("bar", new UInt32(52), new Variant<Boolean>(new Boolean(true))));

      System.out.println("Checking bools");
      if (tri2.check()) fail("bools are broken");
         
      List<String> l = new Vector<String>();
      l.add("hi");
      l.add("hello");
      l.add("hej");
      l.add("hey");
      l.add("aloha");
      System.out.println("Sampling Arrays:");
      List<Integer> is = tri2.sampleArray(l, new Integer[] { 1, 5, 7, 9 }, new long[] { 2, 6, 8, 12 });
      System.out.println("sampleArray returned an array:");
      for (Integer i: is)
         System.out.println("--"+i);
      if (is.size() != 5 ||
            is.get(0).intValue() != -1 ||
            is.get(1).intValue() != -5 ||
            is.get(2).intValue() != -7 ||
            is.get(3).intValue() != -12 ||
            is.get(4).intValue() != -18)
         fail("sampleArray return value incorrect");

      System.out.println("Get This");
      if (!tclass.equals(tri2.getThis(tri2)))
         fail("Didn't get the correct this");
      
      Boolean b = stuffreply.getReply();
      System.out.println("Do stuff replied "+b);
      if (true != b.booleanValue())
         fail("dostuff return value incorrect");
      
      System.out.print("Sending Array Signal...");
      /** This creates an instance of the Test Signal, with the given object path, signal name and parameters, and broadcasts in on the Bus. */
      conn.sendSignal(new TestSignalInterface.TestArraySignal("/foo/bar/com/Wibble", new TestStruct2(l, new Variant<UInt64>(new UInt64(567)))));
      
      System.out.println("done");

      System.out.print("testing custom serialization...");
      Vector<Integer> v = new Vector<Integer>();
      v.add(1);
      v.add(2);
      v.add(3);
      TestSerializable<String> s = new TestSerializable<String>(1, "woo", v);
      tri2.testSerializable((byte) 12, s, 13);
      
      System.out.println("done");
      
      System.out.print("testing recursion...");
      
      if (!"This Is A Name!!".equals(tri2.recursionTest())) fail("recursion test failed");
      
      System.out.println("done");

      System.out.print("testing method overloading...");
      tri = (TestRemoteInterface) conn.getRemoteObject("foo.bar.Test", "/Test", TestRemoteInterface.class);
      if (1 != tri2.overload("foo")) test.fail("wrong overloaded method called");
      if (2 != tri2.overload((byte) 0)) test.fail("wrong overloaded method called");
      if (3 != tri2.overload()) test.fail("wrong overloaded method called");
      if (4 != tri.overload()) test.fail("wrong overloaded method called");
      System.out.println("done");

      /** Pause while we wait for the DBus messages to go back and forth. */
      Thread.sleep(1000);

      System.out.println("Checking for outstanding errors");
      DBusExecutionException DBEe = conn.getError();
      if (null != DBEe) throw DBEe;
    
      System.out.println("Disconnecting");
      /** Disconnect from the bus. */
      conn.disconnect();
      conn = null;
   } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected Exception Occurred");
   }}
}
