package adc.sample;

import adc.ADCContext;
import adc.ADCListener;
import adc.ADCObserver;

import java.io.FileOutputStream;

import oracle.generic.ws.client.ClientFacade;
import oracle.generic.ws.client.ServerListenerAdapter;
import oracle.generic.ws.client.ServerListenerInterface;

public class WebSocketFeeder
{
  private final static boolean DEBUG = false;
  private ADCObserver.MCP3008_input_channels channel = null;
  private boolean keepWorking = true;
  private ClientFacade webSocketClient = null;
  
  public WebSocketFeeder(int ch) throws Exception
  {
    channel = findChannel(ch);
    String wsUri = System.getProperty("ws.uri", "ws://localhost:9876/"); 
    
    initWebSocketConnection(wsUri);
    final ADCObserver obs = new ADCObserver(channel); // Note: We could instantiate more than one observer (on several channels).
    ADCContext.getInstance().addListener(new ADCListener()
       {
         @Override
         public void valueUpdated(ADCObserver.MCP3008_input_channels inputChannel, int newValue) 
         {
           if (inputChannel.equals(channel))
           {
             int volume = (int)(newValue / 10.23); // [0, 1023] ~ [0x0000, 0x03FF] ~ [0&0, 0&1111111111]
             if (DEBUG)
               System.out.println("readAdc:" + Integer.toString(newValue) + 
                                               " (0x" + lpad(Integer.toString(newValue, 16).toUpperCase(), "0", 2) + 
                                               ", 0&" + lpad(Integer.toString(newValue, 2), "0", 8) + ")"); 
             System.out.println("Volume:" + volume + "% (" + newValue + ")");
             webSocketClient.send(Integer.toString(volume));
           }
         }
       });
    obs.start();         
    
    Runtime.getRuntime().addShutdownHook(new Thread()
       {
         public void run()
         {
           if (obs != null)
             obs.stop();
           keepWorking = false;
           webSocketClient.bye();
         }
       });    
  }
  
  private void initWebSocketConnection(String serverURI)
  {
    String[] targetedTransports = new String[] {"WebSocket", 
                                                "XMLHttpRequest"};
    ServerListenerInterface serverListener = new ServerListenerAdapter()
    {
      @Override
      public void onMessage(String mess)
      {
    //    System.out.println("    . Text message :[" + mess + "]");
    //    JSONObject json = new JSONObject(mess);
    //    System.out.println("    . Mess content:[" + ((JSONObject)json.get("data")).get("text") + "]");
      }

      @Override
      public void onMessage(byte[] bb)
      {
        System.out.println("    . Message for you (ByteBuffer) ...");
        System.out.println("Length:" + bb.length);
        try
        {
          FileOutputStream fos = new FileOutputStream("binary.xxx");
          for (int i=0; i<bb.length; i++)
            fos.write(bb[i]);
          fos.close();
          System.out.println("... was written in binary.xxx");
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
      }

      @Override
      public void onConnect()
      {
        System.out.println("    .You're in!");
        keepWorking = true;
      }

      @Override
      public void onClose()
      {
        System.out.println("    .Connection has been closed...");
        keepWorking = false;
      }

      @Override
      public void onError(String error)
      {
        System.out.println("    .Oops! error [" + error + "]");
        keepWorking = false; // Careful with that one..., in case of a fallback, use the value returned by the init method.
      }

      @Override
      public void setStatus(String status)
      {
        System.out.println("    .Your status is now [" + status + "]");
      }
      
      @Override
      public void onPong(String s)
      {
        if (DEBUG)
          System.out.println("WS Pong");
      }
      
      @Override
      public void onPing(String s)
      {
        if (DEBUG)
          System.out.println("WS Ping");
      }
      
      @Override
      public void onHandShakeSentAsClient()
      {
        System.out.println("WS-HS sent as client");
      }
      
      @Override
      public void onHandShakeReceivedAsServer()
      {
        if (DEBUG)
          System.out.println("WS-HS received as server");
      }
      
      @Override
      public void onHandShakeReceivedAsClient()
      {
        if (DEBUG)
          System.out.println("WS-HS received as client");
      }      
    };
    
    try
    {
      webSocketClient = new ClientFacade(serverURI, 
                                targetedTransports, 
                                serverListener);
      keepWorking = webSocketClient.init();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }    
  }
  
  public static void main(String[] args) throws Exception
  {
    int channel = 0;
    if (args.length > 0)
      channel = Integer.parseInt(args[0]);
    new WebSocketFeeder(channel);
  }

  private static ADCObserver.MCP3008_input_channels findChannel(int ch) throws IllegalArgumentException
  {
    ADCObserver.MCP3008_input_channels channel = null;
    switch (ch)
    {
      case 0:
        channel = ADCObserver.MCP3008_input_channels.CH0;
        break;
      case 1:
        channel = ADCObserver.MCP3008_input_channels.CH1;
        break;
      case 2:
        channel = ADCObserver.MCP3008_input_channels.CH2;
        break;
      case 3:
        channel = ADCObserver.MCP3008_input_channels.CH3;
        break;
      case 4:
        channel = ADCObserver.MCP3008_input_channels.CH4;
        break;
      case 5:
        channel = ADCObserver.MCP3008_input_channels.CH5;
        break;
      case 6:
        channel = ADCObserver.MCP3008_input_channels.CH6;
        break;
      case 7:
        channel = ADCObserver.MCP3008_input_channels.CH7;
        break;
      default:
        throw new IllegalArgumentException("No channel " + Integer.toString(ch));
    }
    return channel;
  }
  
  private static String lpad(String str, String with, int len)
  {
    String s = str;
    while (s.length() < len)
      s = with + s;
    return s;
  }
}

