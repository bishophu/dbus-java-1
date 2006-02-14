package org.freedesktop.dbus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.freedesktop.DBus.Introspectable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** 
 * Converts a DBus XML file into Java interface definitions.
 */
public class CreateInterface
{
   public String comment = "";

   public CreateInterface(PrintStreamFactory factory)
   {
      this.factory = factory;
   }
   String parseReturns(Vector<Element> out, Set<String> imports, Map<String,Integer> tuples, Map<String, Integer> structs) throws DBusException
   {
      String[] names = new String[] { "Pair", "Triplet", "Quad", "Quintuple", "Sextuple", "Septuple" };
      String sig = "";
      String name = null;
      switch (out.size()) {
         case 0:
            sig += "void ";
            break;
         case 1:
            sig += DBusConnection.getJavaType(out.get(0).getAttribute("type"), imports, structs, false, false)+" ";
            break;
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
            name = names[out.size() - 2];
         default:
            if (null == name) 
               name = "NTuple"+out.size();

            tuples.put(name, out.size());
            sig += name + "<";
            for (Element arg: out)
               sig += DBusConnection.getJavaType(arg.getAttribute("type"), imports, structs, true, false)+", ";
            sig = sig.replaceAll(", $","> ");
            break;
      }
      return sig;
   }
   String parseMethod(Element meth, Set<String> imports, Map<String,Integer> tuples, Map<String, Integer> structs) throws DBusException
   {
      Vector<Element> in = new Vector<Element>();
      Vector<Element> out = new Vector<Element>();
      if (null == meth.getAttribute("name") ||
            "".equals(meth.getAttribute("name"))) {
         System.err.println("ERROR: Interface name was blank, failed");
         System.exit(1);
      }

      for (Node a: new IterableNodeList(meth.getChildNodes())) {

         if (Node.ELEMENT_NODE != a.getNodeType()) continue;

         checkNode(a, "arg", "annotation");

         if ("arg".equals(a.getNodeName())) {
            Element arg = (Element) a;

            if ("in".equals(arg.getAttribute("direction")))
               in.add(arg);
            else
               out.add(arg);
         }
         else if ("annotation".equals(meth.getNodeName()))
            System.err.println("WARNING: Ignoring annotation");
      }

      String sig = "";
      comment = "";
      sig += parseReturns(out, imports, tuples, structs);

      sig += meth.getAttribute("name")+"(";

      char defaultname = 'a';
      String params = "";
      for (Element arg: in) {
         String type = DBusConnection.getJavaType(arg.getAttribute("type"), imports, structs, false, false);
         String name = arg.getAttribute("name");
         if (null == name || "".equals(name)) name = ""+(defaultname++);
         params += type+" "+name+", ";         
      }
      return ("".equals(comment) ? "" : "   /**\n" + comment + "   */\n")
         + "  public " + sig + params.replaceAll("..$", "")+");";
   }
   String parseSignal(Element signal, Set<String> imports, Map<String, Integer> structs) throws DBusException
   {
      Map<String, String> params = new HashMap<String, String>();
      char defaultname = 'a';
      imports.add("org.freedesktop.dbus.DBusSignal");
      imports.add("org.freedesktop.dbus.DBusException");
      for (Node a: new IterableNodeList(signal.getChildNodes())) {

         if (Node.ELEMENT_NODE != a.getNodeType()) continue;

         checkNode(a, "arg", "annotation");

         if ("annotation".equals(a.getNodeName()))
            System.err.println("WARNING: Ignoring annotation");
         else {
            Element arg = (Element) a;
            String type = DBusConnection.getJavaType(arg.getAttribute("type"), imports, structs, false, false);
            String name = arg.getAttribute("name");
            if (null == name || "".equals(name)) name = ""+(defaultname++);
            params.put(name, type);
         }
      }

      String out = "";
      char t = 'A';
      out += "   public static class "+signal.getAttribute("name");
      if (params.size() > 0) {
         out += '<';
         for (String name: params.keySet())
            out += (t++)+" extends "+params.get(name)+",";
         out = out.replaceAll(",$", ">");
      }

      out += " extends DBusSignal\n   {\n";
      t = 'A';
      for (String name: params.keySet())
         out += "      public final "+(t++)+" "+name+";\n";
      out += "      public "+signal.getAttribute("name")+"(String path";
      t = 'A';
      for (String name: params.keySet())
         out += ", "+(t++)+" "+name;
      out += ") throws DBusException\n      {\n         super(path";
      for (String name: params.keySet())
         out += ", "+name;
      out += ");\n";
      for (String name: params.keySet())
         out += "         this."+name+" = "+name+";\n";
      out += "      }\n";

      out += "   }\n";
      return out;
   }

   void parseInterface(Element iface, PrintStream out, Map<String,Integer> tuples, Map<String, Integer> structs) throws DBusException
   {
      if (null == iface.getAttribute("name") ||
            "".equals(iface.getAttribute("name"))) {
         System.err.println("ERROR: Interface name was blank, failed");
         System.exit(1);
      }

      out.println("package "+iface.getAttribute("name").replaceAll("\\.[^.]*$","")+";");

      String methods = "";
      String signals = "";
      Set<String> imports = new TreeSet<String>();
      imports.add("org.freedesktop.dbus.DBusInterface");
      for (Node meth: new IterableNodeList(iface.getChildNodes())) {

         if (Node.ELEMENT_NODE != meth.getNodeType()) continue;

         checkNode(meth, "method", "signal", "property", "annotation");

         if ("method".equals(meth.getNodeName()))
            methods += parseMethod((Element) meth, imports, tuples, structs) + "\n";
         else if ("signal".equals(meth.getNodeName()))
            signals += parseSignal((Element) meth, imports, structs);
         else if ("property".equals(meth.getNodeName()))
            System.err.println("WARNING: Ignoring property");
         else if ("annotation".equals(meth.getNodeName()))
            System.err.println("WARNING: Ignoring annotation");

      }

      if (imports.size() > 0) 
         for (String i: imports)
            out.println("import "+i+";");

      out.print("public interface "+iface.getAttribute("name").replaceAll("^.*\\.([^.]*)$","$1"));
      out.println(" extends DBusInterface");
      out.println("{");
      out.println(signals);
      out.println(methods);
      out.println("}");
   }

   void createTuple(String name, int num, String pack, PrintStream out, String superclass) throws DBusException
   {
      out.println("package "+pack+";");
      out.println("import org.freedesktop.dbus."+superclass+";");
      out.println("/** Just a typed container class */");
      out.print("public final class "+name);
      String types = " <";
      for (char v = 'A'; v < 'A'+num; v++) 
         types += v + ",";
      out.print(types.replaceAll(",$","> "));
      out.println("extends "+superclass);
      out.println("{");

      char t = 'A';
      char n = 'a';
      for (int i = 0; i < num; i++,t++,n++)
         out.println("   public final "+t+" "+n+";");

      out.print("   public "+name+"(");
      String sig = "";
      t = 'A';
      n = 'a';
      for (int i = 0; i < num; i++,t++,n++)
         sig += t+" "+n+", ";
      out.println(sig.replaceAll(", $", ")"));
      out.println("   {");
      out.print("      super(");
      sig = "";
      for (char v = 'a'; v < 'a'+num; v++) 
         sig += v+", ";
      out.println(sig.replaceAll(", $", ");"));
      for (char v = 'a'; v < 'a'+num; v++) 
         out.println("      this."+v+" = "+v+";");
      out.println("   }");

      out.println("}");
   }
   void parseRoot(Element root) throws DBusException, IOException
   {
      for (Node iface: new IterableNodeList(root.getChildNodes())) {

         if (Node.ELEMENT_NODE != iface.getNodeType()) continue;

         checkNode(iface, "interface", "node");

         if ("interface".equals(iface.getNodeName())) {

            Map<String, Integer> tuples = new HashMap<String, Integer>();
            Map<String, Integer> structs = new HashMap<String, Integer>();
            String name = ((Element) iface).getAttribute("name");
            String file = name.replaceAll("\\.","/")+".java";
            String path = file.replaceAll("/[^/]*$", "");

            factory.init(file, path);
            parseInterface((Element) iface, 
                  factory.createPrintStream(file), tuples, structs);
            createTuples(tuples, "Tuple", name, path);
            createTuples(structs, "Struct", name, path);
         }
         else if ("node".equals(iface.getNodeName())) 
            parseRoot((Element) iface);
         else {
            System.err.println("ERROR: Unknown node: "+iface.getNodeName()+".");
            System.exit(1);
         }
      }
   }
   /**
    * @param typeMap
    * @param type
    * @param name
    * @param path
    * @throws DBusException
    * @throws IOException
    */
   private void createTuples(Map<String, Integer> typeMap, String type, String name, String path) throws DBusException, IOException
   {
      for (String tname: typeMap.keySet()) 
         createTuple(tname, typeMap.get(tname), name.replaceAll("\\.[^.]*$",""),
               factory.createPrintStream(path, tname), type);
   }

   public static abstract class PrintStreamFactory
   {

      public abstract void init(String file, String path);

      /**
       * @param path
       * @param tname
       * @return
       * @throws IOException
       */
      public PrintStream createPrintStream(String path, String tname) throws IOException
      {
         final String file = path+"/"+tname+".java";
         return createPrintStream(file);
      }

      /**
       * @param file
       * @return
       * @throws IOException
       */
      public abstract PrintStream createPrintStream(final String file) throws IOException;

   }
   static class ConsoleStreamFactory extends PrintStreamFactory
   {

      @Override
         public
         void init(String file, String path)
         {
            System.out.println("/* File: "+file+" */");
         }

      @Override
         public
         PrintStream createPrintStream(String file) throws IOException
         {
            return System.out;
         }

      public PrintStream createPrintStream(String path, String tname) throws IOException
      {
         System.out.println("/* File: "+path+"/"+tname+".java */");
         return super.createPrintStream(path, tname);
      }

   }

   static class FileStreamFactory extends PrintStreamFactory
   {
      public void init(String file, String path)
      {
         new File(path).mkdirs();
      }


      /**
       * @param file
       * @return
       * @throws IOException
       */
      public PrintStream createPrintStream(final String file) throws IOException
      {
         return new PrintStream(new FileOutputStream(file));
      }

   }

   static void checkNode(Node n, String... names) 
   {
      String expected = "";
      for (String name: names) {
         if (name.equals(n.getNodeName())) return;
         expected += name + " or ";
      }
      System.err.println("ERROR: Expected "+expected.replaceAll("....$", "")+", got "+n.getNodeName()+", failed.");
      System.exit(1);
   }

   private final PrintStreamFactory factory;

   static class Config
   {
      int bus = DBusConnection.SESSION;
      String service = null;
      String object = null;
      File datafile = null;
      boolean printtree = false;
      boolean fileout = false;
   }

   static void printSyntax()
   {
      printSyntax(System.err);
   }
   static void printSyntax(PrintStream o)
   {
      o.println("Syntax: CreateInterface <options> [file | service object]");
      o.println("        Options: --system -y --session -s --create-files -f --help -h");
   }

   static Config parseParams(String[] args)
   {
      Config config = new Config();
      for (String p: args) {
         if ("--system".equals(p) || "-y".equals(p)) 
            config.bus = DBusConnection.SYSTEM;
         else if ("--session".equals(p) || "-s".equals(p)) 
            config.bus = DBusConnection.SESSION;
         else if ("--print-tree".equals(p) || "-p".equals(p)) 
            config.printtree = true;
         else if ("--help".equals(p) || "-h".equals(p)) {
            printSyntax(System.out);
            System.exit(0);
         } else if (p.startsWith("-")) {
            System.err.println("ERROR: Unknown option: "+p);
            printSyntax();
            System.exit(1);
         }
         else {
            if (null == config.service) config.service = p;
            else if (null == config.object) config.object = p;
            else {
               printSyntax();
               System.exit(1);
            }
         }
      }
      if (null == config.service) {
         printSyntax();
         System.exit(1);
      }
      else if (null == config.object) {
         config.datafile = new File(config.service);
         config.service = null;
      }
      return config;
   }

   public static void main(String[] args) throws Exception
   {
      Config config = parseParams(args);

      Reader introspectdata = null;

      if (null != config.service) try {
         DBusConnection conn = DBusConnection.getConnection(config.bus);
         Introspectable in = (Introspectable) conn.getRemoteObject(config.service, config.object, Introspectable.class);
         String id = in.Introspect();
         if (null == id) {
            System.err.println("ERROR: Failed to get introspection data");
            System.exit(1);
         }
         introspectdata = new StringReader(id);
         conn.disconnect();
      } catch (DBusException DBe) {
         System.err.println("ERROR: Failure in DBus Communications: "+DBe.getMessage());
         System.exit(1);
      } catch (DBusExecutionException DEe) {
         System.err.println("ERROR: Failure in DBus Communications: "+DEe.getMessage());
         System.exit(1);

      } else if (null != config.datafile) try {
         introspectdata = new InputStreamReader(new FileInputStream(config.datafile));
      } catch (FileNotFoundException FNFe) {
         System.err.println("ERROR: Could not find introspection file: "+FNFe.getMessage());
         System.exit(1);
      }
      try {
         PrintStreamFactory factory = config.fileout  ? new FileStreamFactory() : new ConsoleStreamFactory();
         CreateInterface createInterface = new CreateInterface(factory);
         createInterface.createInterface(introspectdata);
      } catch (DBusException DBe) {
         System.err.println("ERROR: "+DBe.getMessage());
         System.exit(1);
      }
   }
   /** Output the interface for the supplied xml reader
    * @param introspectdata The introspect data reader
    * @throws ParserConfigurationException If the xml parser could not be configured
    * @throws SAXException If a problem occurs reading the xml data
    * @throws IOException If an IO error occurs
    * @throws DBusException If the dbus related error occurs
    */
   public void createInterface(Reader introspectdata) throws ParserConfigurationException, SAXException, IOException, DBusException
   {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new InputSource(introspectdata));

      Element root = document.getDocumentElement();
      checkNode(root, "node");
      parseRoot(root);

   }
}


