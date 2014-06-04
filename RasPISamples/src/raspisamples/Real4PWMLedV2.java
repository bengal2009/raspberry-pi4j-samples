package raspisamples;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import raspisamples.pwm.PWMPin;

public class Real4PWMLedV2
{
  private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

  public static String userInput(String prompt)
  {
    String retString = "";
    System.err.print(prompt);
    try
    {
      retString = stdin.readLine();
    }
    catch(Exception e)
    {
      System.out.println(e);
      String s;
      try
      {
        s = userInput("<Oooch/>");
      }
      catch(Exception exception) 
      {
        exception.printStackTrace();
      }
    }
    return retString;
  }

  public static void main(String[] args)
    throws InterruptedException
  {
    final GpioController gpio = GpioFactory.getInstance();

    final PWMPin pin00 = new PWMPin(RaspiPin.GPIO_00, "LED-One",   PinState.HIGH);
    final PWMPin pin01 = new PWMPin(RaspiPin.GPIO_01, "LED-Two",   PinState.HIGH);
    final PWMPin pin02 = new PWMPin(RaspiPin.GPIO_02, "LED-Three", PinState.HIGH);
    final PWMPin pin03 = new PWMPin(RaspiPin.GPIO_03, "LED-Four",  PinState.HIGH);
    System.out.println("Ready...");

    Thread.sleep(1000);

    pin00.emitPWM(0);
    pin01.emitPWM(0);
    pin02.emitPWM(0);
    pin03.emitPWM(0);

    boolean go = true;
    while (go)
    {
      Thread one = new Thread()
        {
          public void run()
          {
            for (int vol=0; vol<100; vol++)
            {
              pin00.adjustPWMVolume(vol);
              try { Thread.sleep(10); } catch (Exception ex) {}
            }
            for (int vol=100; vol>=0; vol--)
            {
              pin00.adjustPWMVolume(vol);
              try { Thread.sleep(10); } catch (Exception ex) {}
            }
            System.out.println("Thread One finishing");
          }
        };
      Thread two = new Thread()
        {
          public void run()
          {
            for (int vol=100; vol>0; vol--)
            {
              pin01.adjustPWMVolume(vol);
              try { Thread.sleep(10); } catch (Exception ex) {}
            }
            for (int vol=0; vol<=100; vol++)
            {
              pin01.adjustPWMVolume(vol);
              try { Thread.sleep(10); } catch (Exception ex) {}
            }
            System.out.println("Thread Two finishing");
          }
        };
      Thread three = new Thread()
        {
          public void run()
          {
            for (int vol=0; vol<100; vol++)
            {
              pin02.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            for (int vol=100; vol>=0; vol--)
            {
              pin02.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            for (int vol=0; vol<100; vol++)
            {
              pin02.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            for (int vol=100; vol>=0; vol--)
            {
              pin02.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            System.out.println("Thread Three finishing");
          }
        };
      Thread four = new Thread()
        {
          public void run()
          {
            for (int vol=100; vol>0; vol--)
            {
              pin03.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            for (int vol=0; vol<=100; vol++)
            {
              pin03.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            for (int vol=100; vol>0; vol--)
            {
              pin03.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            for (int vol=0; vol<=100; vol++)
            {
              pin03.adjustPWMVolume(vol);
              try { Thread.sleep(5); } catch (Exception ex) {}
            }
            System.out.println("Thread Four finishing");
          }
        };
          
      one.start();
      two.start();
      three.start();
      four.start();
    
      String usr = userInput("Again y|n ? > ");
      if (!"Y".equalsIgnoreCase(usr))
        go = false;
    }

    pin00.stopPWM();
    pin01.stopPWM();
    pin02.stopPWM();
    pin03.stopPWM();
    
    Thread.sleep(1000);
    // Last blink
    System.out.println("Bye-bye");
    pin00.low();
    Thread.sleep(500);
    pin00.high();
    Thread.sleep(500);
    pin00.low();
    
    gpio.shutdown();
  }
}
