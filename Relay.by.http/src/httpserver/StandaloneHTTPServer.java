package httpserver;


import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import relay.RelayManager;

/**
 * Dedicated HTTP Server.
 * This is NOT J2EE Compliant, not even CGI.
 *
 * Runs the communication between an HTTP client and the
 * features of the Data server to be displayed remotely.
 * 
 * Comes with a speech option using "freeTTS" (see http://freetts.sourceforge.net/docs/index.php)
 */
public class StandaloneHTTPServer
{
  private static boolean verbose = false;
  private static boolean speak   = false;
  private static RelayManager rm;
  private VoiceManager voiceManager = VoiceManager.getInstance();
  private static Voice voice;
  private final static String VOICENAME = "kevin16";

  public StandaloneHTTPServer() {}
  
  public StandaloneHTTPServer(String[] prms)
  {
    try { rm = new RelayManager(); }
    catch (Exception ex)
    {
      System.err.println("You're not on the PI, hey?");
      ex.printStackTrace();
    }
    // Bind the server
    String machineName = "localhost";
    String port        = "9999";

    machineName = System.getProperty("http.host", machineName);
    port        = System.getProperty("http.port", port);
    
    System.out.println("HTTP Host:" + machineName);
    System.out.println("HTTP Port:" + port);
    
    System.out.println("\nOptions are -verbose=y|[n], -speak=y|[n]");
    if (prms != null && prms.length > 0)
    {
      for (int i=0; i<prms.length; i++)
      {
//      System.out.println("Parameter[" + i + "]=" + prms[i]);
        if (prms[i].startsWith("-verbose="))
        {
          verbose = prms[i].substring("-verbose=".length()).equals("y");
        }
        else if (prms[i].startsWith("-speak="))
        {
          speak = prms[i].substring("-speak=".length()).equals("y");
        }
//      System.out.println("verbose=" + verbose);
      }
    }
    
    int _port = 0;
    try
    {
      _port = Integer.parseInt(port);
    }
    catch (NumberFormatException nfe)
    {
      throw nfe;
    }

    if (speak)
    {
      voice = voiceManager.getVoice(VOICENAME);
      voice.allocate();
    }
    if (verbose)
      System.out.println("Server running from [" + System.getProperty("user.dir") + "]");
    // Infinite loop
    try
    {
      ServerSocket ss = new ServerSocket(_port);
      while (true)
      {
        Socket client = ss.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter   out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
        
        String line;
        while ((line = in.readLine()) != null)
        {
          if (line.length() == 0)
            break;
          else if (line.startsWith("POST /exit") || line.startsWith("GET /exit"))
          {
            System.out.println("Received an exit signal");
            System.exit(0);
          }
          else if (line.startsWith("POST /") || line.startsWith("GET /"))
          {
            manageRequest(line, out);
          }
          if (verbose)
            System.out.println("Read:[" + line + "]");
        }
//      out.println(generateContent());

        out.flush();
        out.close();
        in.close();
        client.close();
      }
    }
    catch (Exception e)
    {
      System.err.println(e.toString());
      e.printStackTrace();
    }
  }
  
  private void manageRequest(String request, PrintWriter out)
  {
    out.println(generateContent(request));
  }
 
  private String generateContent(String request)
  {
    String str = ""; // "Content-Type: text/xml\r\n\r\n";
//  System.out.println("Managing request [" + request + "]");
    String[] elements = request.split(" ");
    if (elements[0].equals("GET"))
    {
      String[] parts = elements[1].split("\\?");
      if (parts.length != 2)
      {
        String fileName = parts[0];
        File data = new File("." + fileName);
        if (data.exists())
        {
          try
          {
            BufferedReader br = new BufferedReader(new FileReader(data));
            String line = "";
            while (line != null)
            {
              line = br.readLine();
              if (line != null)
                str += (line + "\n");
            }
            br.close();
          }
          catch (Exception ex)
          {
            ex.printStackTrace();
          }
        }
        else
          str = "- There is no parameter is this query -";
      }
      else
      {
        if (request.startsWith("GET /relay-access"))
        {
          System.out.println("--> " + request);
          String dev = "";
          String status = "";
          String[] params = parts[1].split("&");
          for (String nv : params)
          {
            String[] nvPair = nv.split("=");
  //        System.out.println(nvPair[0] + " = " + nvPair[1]);
            if (nvPair[0].equals("dev"))
              dev = nvPair[1];
            else if (nvPair[0].equals("status"))
              status = nvPair[1];
          }
          System.out.println("Setting [" + dev + "] to [" + status + "]");
          if (("01".equals(dev) || "02".equals(dev)) &&
              ("on".equals(status) || "off".equals(status)))
          {
            if (speak)
              voice.speak("Turning light " + ("01".equals(dev)?"one":"two") + ", " + status + "!");
            try { rm.set(dev, status); }
            catch (Exception ex)
            {
              System.err.println(ex.toString());
            }
            str = "200 OK\r\n"; 
          }
          else
          {
            System.out.println("Unknown dev/status [" + dev + "/" + status + "]");
          }
        }
      }
    }
    else
      str = "- Not managed -";
    
    return str;
  }
  
  public static void shutdown()
  {
    rm.shutdown();
    rm.set("01", "off");
    rm.set("02", "off");
    if (speak)
      voice.deallocate();
  }

  /**
   * 
   * @param args see usage
   */
  public static void main(String[] args)
  {
    System.out.println("Starting tiny dedicated server");
    System.out.println("Use [Ctrl] + [C] to stop it, or POST the following request:");
    System.out.println("http://localhost:" + System.getProperty("http.port", "9999") + "/exit");
    System.out.println("Data are available at:");
    System.out.println("http://localhost:" + System.getProperty("http.port", "9999"));
    System.out.println("----------------------------------");
    if (isHelpRequired(args))
    {
      System.out.println("Usage is: java " + new StandaloneHTTPServer().getClass().getName() + " prms");
      System.out.println("\twhere prms can be:");
      System.out.println("\t-?\tDisplay this message");
      System.out.println("\t-verbose=[y|n] - default is n");
      System.out.println("The following variables can be defined in the command line (before the class name):");
      System.out.println("\t-Dhttp.port=[port number]\tThe HTTP port to listen to, 9999 by default");
      System.out.println("\t-Dhttp.host=[hostname]   \tThe HTTP host to bind, localhost by default");
      System.out.println("Example:");
      System.out.println("java -Dhttp.port=6789 -Dhttp.host=localhost " + new StandaloneHTTPServer().getClass().getName());
      System.exit(0);
    }
    Runtime.getRuntime().addShutdownHook(new Thread()
     {
       public void run()
       {
         System.out.println("\nShutting down nicely...");
         shutdown();
       }
     });
    new StandaloneHTTPServer(args);
  }
  
  private static boolean isHelpRequired(String[] args)
  {
    boolean ret = false;
    if (args != null)
    {
      for (int i=0; i<args.length; i++)
      {
        if (args[i].toUpperCase().equals("-H") ||
            args[i].toUpperCase().equals("-HELP") || 
            args[i].toUpperCase().equals("HELP") || 
            args[i].equals("?") || 
            args[i].equals("-?"))
        {
          ret = true;
          break;
        }
      }
    }
    return ret;
  }
}