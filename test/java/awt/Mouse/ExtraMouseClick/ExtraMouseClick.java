/*
 * Copyright 2005-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

 /* 
  test
  @bug 5039416 6404008
  @summary REGRESSION: Extra mouse click dispatched after press-drag- release sequence.
  @library ../../regtesthelpers
  @build Util
  @author  andrei.dmitriev area=awt.event
  @run applet ExtraMouseClick.html
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

//**
// Here are two values of smugde used in this test (2 and 5). They should be on
// different sides from value 4 (smudge distance on both toolkits).
// Note that this test may not fail easily. But it must always pass on
// patched workspace.
//**

public class ExtraMouseClick extends Applet
{
    Frame frame = new Frame("Extra Click After MouseDrag");
    final int TRIALS = 10;
    final int SMUDGE_WIDTH = 4;
    final int SMUDGE_HEIGHT = 4;
    Robot robot;
    Point fp; //frame's location on screen
    boolean dragged = false;
    boolean clicked = false;
    boolean pressed = false;
    boolean released = false;

    public void init() 
    {
        this.setLayout (new BorderLayout ());

        frame.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    System.out.println("MousePressed");
                    pressed = true;
                }
                
                public void mouseReleased(MouseEvent e) {
                    System.out.println("MouseReleased");
                    released = true;
                }
                
                public void mouseClicked(MouseEvent e) {
                    System.out.println("MouseClicked!!!!");
                    clicked = true;
                }
            });
        frame.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    System.out.println("MouseDragged--"+e);
                    dragged = true;
                }
                public void mouseMoved(MouseEvent e) {
                }
            });
        
    }//End  init()


    public void start ()
    {
        frame.setSize(480, 300);
        frame.setVisible(true);
        try{
            robot = new Robot();
        }catch(AWTException e){
            throw new RuntimeException(e);
        }

        Util.waitForIdle(robot);  //a time to show Frame

        fp = frame.getLocationOnScreen();

        for (int i = 0; i< TRIALS; i++){
            checkClicked();
            clearFlags();
        }        

        for (int i = 0; i< TRIALS; i++){
            oneDrag(2);
            clearFlags();
        }

        for (int i = 0; i< TRIALS; i++){
            oneDrag(5);
            clearFlags();
        }

        for (int i = 0; i< TRIALS; i++){
            oneDrag(70);
            clearFlags();
        }

        //Check that no Drag event occur in the SMUDGE area
        String sToolkit = Toolkit.getDefaultToolkit().getClass().getName();
        System.out.println("Toolkit == "+sToolkit);
        if ("sun.awt.windows.WToolkit".equals(sToolkit)){
            int dragWidth = ((Integer)Toolkit.getDefaultToolkit().
                             getDesktopProperty("win.drag.width")).intValue();
            int dragHeight = ((Integer)Toolkit.getDefaultToolkit().
                            getDesktopProperty("win.drag.height")).intValue();
            System.out.println("dragWidth=="+dragWidth+":: dragHeight=="+dragHeight);
            // DragWidth and dragHeight may be equal to 1. In that case the SMUDGE rectangle
            // narrowed into 1x1 pixel and we can't drag a mouse in it.
            // In that case we may skip following testcase but I'd prefer if we move mouse on 1 pixel only.
            // And that should pass as well.
            dragWidth = dragWidth > 1? dragWidth/2:1; 
            dragHeight = dragHeight > 1? dragHeight/2:1; 
            for (int i = 0; i< TRIALS; i++){
                smallWin32Drag(dragWidth, dragHeight);
                clearFlags();
            }
        }else{
            for (int i = 0; i< TRIALS; i++){
                smallDrag(SMUDGE_WIDTH - 1, SMUDGE_HEIGHT - 1); //on Motif and XAWT SMUDGE area is 4-pixels wide
                clearFlags();
            }
        }
        System.out.println("Test passed.");
    }// start()

    public void oneDrag(int pixels){
        robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 );
        //drag for a short distance
        robot.mousePress(InputEvent.BUTTON1_MASK );
        for (int i = 1; i<pixels;i++){
            robot.mouseMove(fp.x + frame.getWidth()/2 + i, fp.y + frame.getHeight()/2 );
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK );
        robot.delay(1000);
        if (dragged && clicked){
            throw new RuntimeException("Test failed. Clicked event follows by Dragged. Dragged = "+dragged +". Clicked = "+clicked + " : distance = "+pixels);
        }
    }

  public void smallDrag(int pixelsX, int pixelsY){
        // by the X-axis
        robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 );
        //drag for a short distance
        robot.mousePress(InputEvent.BUTTON1_MASK );
        for (int i = 1; i<pixelsX;i++){
            robot.mouseMove(fp.x + frame.getWidth()/2 + i, fp.y + frame.getHeight()/2 );
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK );
        robot.delay(1000);
        if (dragged){
            throw new RuntimeException("Test failed. Dragged event (by the X-axis) occured in SMUDGE area. Dragged = "+dragged +". Clicked = "+clicked);
        }

        // the same with Y-axis
        robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 );
        robot.mousePress(InputEvent.BUTTON1_MASK );
        for (int i = 1; i<pixelsY;i++){
            robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 + i );
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK );
        robot.delay(1000);
        if (dragged){
            throw new RuntimeException("Test failed. Dragged event (by the Y-axis) occured in SMUDGE area. Dragged = "+dragged +". Clicked = "+clicked);
        }

    }

    // The difference between X-system and Win32: on Win32 Dragged event start to be generated after any mouse drag.
    // On X-systems Dragged event first fired when mouse has left the SMUDGE area
    public void smallWin32Drag(int pixelsX, int pixelsY){
        // by the X-axis
        robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 );
        //drag for a short distance
        robot.mousePress(InputEvent.BUTTON1_MASK );
        System.out.println(" pixelsX = "+ pixelsX +" pixelsY = " +pixelsY);
        for (int i = 1; i<=pixelsX;i++){
            System.out.println("Moving a mouse by X");
            robot.mouseMove(fp.x + frame.getWidth()/2 + i, fp.y + frame.getHeight()/2 );
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK );
        robot.delay(1000);
        if (!dragged){
            throw new RuntimeException("Test failed. Dragged event (by the X-axis) didn't occur in the SMUDGE area. Dragged = "+dragged);
        }

        // the same with Y-axis
        robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 );
        robot.mousePress(InputEvent.BUTTON1_MASK );
        for (int i = 1; i<=pixelsY;i++){
            System.out.println("Moving a mouse by Y");
            robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 + i );
        }
        robot.mouseRelease(InputEvent.BUTTON1_MASK );
        robot.delay(1000);
        if (!dragged){
            throw new RuntimeException("Test failed. Dragged event (by the Y-axis) didn't occur in the SMUDGE area. Dragged = "+dragged);
        }
    }

    public void checkClicked(){
        robot.mouseMove(fp.x + frame.getWidth()/2, fp.y + frame.getHeight()/2 );
        robot.mousePress(InputEvent.BUTTON1_MASK );
        robot.delay(10);
        robot.mouseRelease(InputEvent.BUTTON1_MASK );
        robot.delay(1000);
        if (!clicked || !pressed || !released || dragged){
            throw new RuntimeException("Test failed. Some of Pressed/Released/Clicked events are missed or dragged occured. Pressed/Released/Clicked/Dragged = "+pressed + ":"+released+":"+clicked +":" +dragged);
        }
    }
    
    public void clearFlags(){
        clicked = false;
        pressed = false;
        released = false;
        dragged = false;
    }
}// class
