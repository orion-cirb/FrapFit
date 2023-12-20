import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class StackReg_Credits extends Dialog {
   private final long serialVersionUID = 1L;

   public Insets getInsets() {
      return new Insets(0, 20, 20, 20);
   }

   protected StackReg_Credits(Frame var1) {
      super(var1, "StackReg", true);
      this.setLayout(new BorderLayout(0, 20));
      Label var2 = new Label("");
      Panel var3 = new Panel();
      var3.setLayout(new FlowLayout(1));
      Button var4 = new Button("Done");
      var4.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent var1) {
            if (var1.getActionCommand().equals("Done")) {
               StackReg_Credits.this.dispose();
            }

         }
      });
      var3.add(var4);
      TextArea var5 = new TextArea(30, 56);
      var5.setEditable(false);
      var5.append("\n");
      var5.append(" This StackReg version is dated July 7, 2011\n");
      var5.append("\n");
      var5.append(" ###\n");
      var5.append("\n");
      var5.append(" This work is based on the following paper:\n");
      var5.append("\n");
      var5.append(" P. Thévenaz, U.E. Ruttimann, M. Unser\n");
      var5.append(" A Pyramid Approach to Subpixel Registration Based on Intensity\n");
      var5.append(" IEEE Transactions on Image Processing\n");
      var5.append(" vol. 7, no. 1, pp. 27-41, January 1998.\n");
      var5.append("\n");
      var5.append(" This paper is available on-line at\n");
      var5.append(" http://bigwww.epfl.ch/publications/thevenaz9801.html\n");
      var5.append("\n");
      var5.append(" Other relevant on-line publications are available at\n");
      var5.append(" http://bigwww.epfl.ch/publications/\n");
      var5.append("\n");
      var5.append(" Additional help available at\n");
      var5.append(" http://bigwww.epfl.ch/thevenaz/stackreg/\n");
      var5.append("\n");
      var5.append(" Ancillary TurboReg_ plugin available at\n");
      var5.append(" http://bigwww.epfl.ch/thevenaz/turboreg/\n");
      var5.append("\n");
      var5.append(" You'll be free to use this software for research purposes, but\n");
      var5.append(" you should not redistribute it without our consent. In addition,\n");
      var5.append(" we expect you to include a citation or acknowledgment whenever\n");
      var5.append(" you present or publish results that are based on it.\n");
      this.add("North", var2);
      this.add("Center", var5);
      this.add("South", var3);
      this.pack();
   }
}
