package readserialport;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;

/**
 * Just reads the GPS data.
 * No parsing, just raw data.
 */
public class GPSDataReader
{
  public static void main(String args[])
    throws InterruptedException, NumberFormatException
  {
    int br = Integer.parseInt(System.getProperty("baud.rate", "9600"));
    String port = System.getProperty("port.name", Serial.DEFAULT_COM_PORT);
    if (args.length > 0)
    {
      try
      {
        br = Integer.parseInt(args[0]);
      }
      catch (Exception ex)
      {
        System.err.println(ex.getMessage());
      }
    }
    
    System.out.println("Serial Communication.");
    System.out.println(" ... connect using settings: " + Integer.toString(br) +  ", N, 8, 1.");
    System.out.println(" ... data received on serial port should be displayed below.");

    // create an instance of the serial communications class
    final Serial serial = SerialFactory.createInstance();
    
    // create and register the serial data listener
    serial.addListener(new SerialDataListener()
    {
      @Override
      public void dataReceived(SerialDataEvent event)
      {
        // print out the data received to the console
        String data = event.getData();
        System.out.println("Got Data (" + data.length() + " byte(s))");
        if (data.startsWith("$"))
          System.out.println(data);
        else
        {
          String hexString = "";
          char[] ca = data.toCharArray();
          for (int i=0; i<ca.length; i++)
            hexString += (lpad(Integer.toHexString(ca[i]), "0", 2) + " ");
          System.out.println(hexString);
        }
      }
    });
        
    final Thread t = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread()
                                         {
                                           public void run()
                                           {
                                             System.out.println("\nShutting down...");
                                             try
                                             {
                                               if (serial.isOpen())
                                               {
                                                 serial.close();
                                                 System.out.println("Serial port closed");
                                               }
                                               synchronized (t)
                                               {
                                                 t.notify();
                                                 System.out.println("Thread notified");
                                               }
                                             }
                                             catch (Exception ex)
                                             {
                                               ex.printStackTrace();
                                             }
                                           }
                                         });
    try
    {
      System.out.println("Opening port [" + port + "]");
      boolean open = false;
      while (!open)
      {
        serial.open(port, br); 
        open = serial.isOpen();
        System.out.println("Port is " + (open ? "" : "NOT ") + "opened.");
        if (!open)
          try { Thread.sleep(500L); } catch (Exception ex) {}
      }
      synchronized (t) { t.wait(); }
      System.out.println("Bye...");
    }
    catch (InterruptedException ie)
    {
      ie.printStackTrace();
    }
  }
  
  private static String lpad(String str, String with, int len)
  {
    String s = str;
    while (s.length() < len)
      s = with + s;
    return s;
  }
}

