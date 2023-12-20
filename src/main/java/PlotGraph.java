import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import java.awt.Color;
import java.awt.Font;

public class PlotGraph {
   Plot plot;
   double ymax;
   int nlines;
   String leg;
   double tmax;

   public PlotGraph(double var1) {
      PlotWindow.noGridLines = false;
      Object var3 = null;
      this.tmax = var1;
      this.nlines = -1;
      this.leg = "";
   }

   public void maxUpdate(double[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         if (var1[var2] > this.ymax) {
            this.ymax = var1[var2];
         }
      }

   }

   public void plotXY(String var1, double[] var2, double[] var3, String var4) {
      this.maxUpdate(var3);
      this.plot = new Plot("Intensities", "Time", var1, var2, var3);
      this.plot.setLineWidth(2);
      this.plot.setColor(Color.red);
      ++this.nlines;
      this.leg = var4;
      this.plot.changeFont(new Font("Helvetica", 0, 16));
      this.plot.setLimits(0.0D, this.tmax, 0.0D, this.ymax * 1.05D);
      this.plot.show();
   }

   public void addPoints(double[] var1, double[] var2, String var3) {
      this.maxUpdate(var2);
      if (this.nlines == 0) {
         this.plot.setColor(Color.green);
      }

      if (this.nlines == 1) {
         this.plot.setColor(Color.blue);
      }

      if (this.nlines == 2) {
         this.plot.setColor(Color.orange);
      }

      if (this.nlines == 3) {
         this.plot.setColor(Color.black);
      }

      if (this.nlines == 4) {
         this.plot.setColor(Color.cyan);
      }

      this.plot.addPoints(var1, var2, 2);
      ++this.nlines;
      this.plot.setLimits(0.0D, this.tmax, 0.0D, this.ymax * 1.05D);
      this.leg = this.leg + "\n" + var3;
      this.plot.show();
   }

   public void showPlot() {
      this.plot.setLegend(this.leg, 128);
      this.plot.show();
   }

   public void closePlot() {
      this.plot.show();
      ImagePlus var1 = this.plot.getImagePlus();
      var1.close();
   }

   public void savePlot(String var1) {
      this.plot.setLegend(this.leg, 128);
      this.plot.show();
      ImagePlus var2 = this.plot.getImagePlus();
      IJ.saveAs(var2, "png", var1);
      var2.close();
   }
}
