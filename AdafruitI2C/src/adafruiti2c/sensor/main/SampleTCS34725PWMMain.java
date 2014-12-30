package adafruiti2c.sensor.main;

import adafruiti2c.sensor.AdafruitTCS34725;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import adafruiti2c.sensor.utils.PWMPin;

public class SampleTCS34725PWMMain
{
  private static boolean go = true;
  
  public static void main(String[] args) throws Exception
  {
    int colorThreshold = 4000;
    if (args.length > 0)
      try { colorThreshold = Integer.parseInt(args[0]); } catch (NumberFormatException nfe) { System.err.println(nfe.toString()); }
    final AdafruitTCS34725 sensor = new AdafruitTCS34725(AdafruitTCS34725.TCS34725_INTEGRATIONTIME_50MS, AdafruitTCS34725.TCS34725_GAIN_4X);
    // Setup output pins here for the 3 color led
    final GpioController gpio = GpioFactory.getInstance();

    final PWMPin greenPin = new PWMPin(RaspiPin.GPIO_00, "green", PinState.LOW);
    final PWMPin bluePin  = new PWMPin(RaspiPin.GPIO_01, "blue",  PinState.LOW);
    final PWMPin redPin   = new PWMPin(RaspiPin.GPIO_02, "red",   PinState.LOW);


    Thread.sleep(1000);

    greenPin.emitPWM(0);
    bluePin.emitPWM(0);
    redPin.emitPWM(0);

    Runtime.getRuntime().addShutdownHook(new Thread()
                                         {
                                           public void run()
                                           {
                                             go = false;
                                             redPin.emitPWM(0);
                                             greenPin.emitPWM(0);
                                             bluePin.emitPWM(0);

                                             gpio.shutdown();
                                             System.out.println("\nBye");
                                           }
                                         });  
    // Main loop
    while (go)
    {
      sensor.setInterrupt(false); // turn led on
      try { Thread.sleep(60); } catch (InterruptedException ie) {} // Takes 50ms to read, see above
      AdafruitTCS34725.TCSColor color = sensor.getRawData();
      sensor.setInterrupt(true); // turn led off
      int r = color.getR(),
          g = color.getG(),
          b = color.getB();
      int greenVol = 0,
          blueVol = 9,
          redVol = 0;
      // Display the color on the 3-color led accordingly
      System.out.println("Read color R:" + r + 
                                   " G:" + g +
                                   " B:" + b);
      // Send to 3-color led. The output is digital!! Not analog.
      if (r > colorThreshold || g > colorThreshold || b > colorThreshold)
      {
        // This calculation deserves improvements
        redVol   = Math.max(Math.min((int)((r - colorThreshold) / 100), 100), 0);
        greenVol = Math.max(Math.min((int)((g - colorThreshold) / 100), 100), 0);
        blueVol  = Math.max(Math.min((int)((b - colorThreshold) / 100), 100), 0);
        greenPin.adjustPWMVolume(greenVol);
        bluePin.adjustPWMVolume(blueVol);
        redPin.adjustPWMVolume(redVol);
        System.out.println("    writing (" + redVol + ", " + greenVol + ", " + blueVol + ")");
      }
      else
      {
        redPin.low();
        greenPin.low();
        bluePin.low();
      }
    }

    System.out.println("Exiting. Thanks.");
  }
}
