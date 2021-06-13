/**
 * Copyright © 2014-2016 Paolo Simonetto
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ocotillo.gui.quickview;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

import ocotillo.dygraph.DyGraph;
import ocotillo.dygraph.rendering.Animation;
import ocotillo.graph.Graph;
import ocotillo.gui.GraphCanvas;

/**
 * Simple dynamic graph visualiser.
 */
public class DyQuickViewMulti extends JFrame {

    private final DyGraph dyGraph1;
    private final DyGraph dyGraph2;
    private GraphCanvas canvas1;
    private GraphCanvas canvas2;
    private Animation animation1;
    private Animation animation2;

    private final JPanel content1 = new JPanel(new CardLayout());
    private final JPanel content2 = new JPanel(new CardLayout());
    private final PlayCommandListner currentListner = new PlayCommandListner();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<GraphCanvas> frameCanvases1 = new ArrayList<>();
    private final List<GraphCanvas> frameCanvases2 = new ArrayList<>();
    private ScheduledFuture<?> refreshTaskHandle1;
    private ScheduledFuture<?> refreshTaskHandle2;

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a DyQuickViewMulti for the graph.
     *
     * @param dyGraph1 the dynamic graph with location 1 highlight to be visualised.
     * @param dyGraph2 the dynamic graph with location 2 highlight to be visualised.
     * @param staticTiming the time used for the static image.
     */
    public DyQuickViewMulti(DyGraph dyGraph1, DyGraph dyGraph2, double staticTiming) {
        setTitle("Graph QuickViewMulti");
        add(content1);
        add(content2);
        this.dyGraph1 = dyGraph1;
        this.dyGraph2 = dyGraph2;
        this.canvas1 = new GraphCanvas(dyGraph1.snapshotAt(staticTiming));
        add(Box.createRigidArea(new Dimension(5, 5)));
        this.canvas2 = new GraphCanvas(dyGraph2.snapshotAt(staticTiming));
        content1.add(canvas1);
        content2.add(canvas2);
        canvas1.addKeyListener(currentListner);
        canvas2.addKeyListener(currentListner);
        canvas1.requestFocus();
        canvas2.requestFocus();
        setSize(2400, 1200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Gets the currently set animation.
     *
     * @return the current animation.
     */
    public Animation animation1() {
        return animation1;
    }

    public Animation animation2() {
        return animation2;
    }

    /**
     * Sets the current animation.
     *
     * @param animation1 animation of graph1.
     * @param animation2 animation of graph2.
     */
    public void setAnimation(Animation animation1, Animation animation2) {
        this.animation1 = animation1;
        this.animation2 = animation2;
        computeFrameCanvases();
    }


    private void computeFrameCanvases() {
        if (animation1 == null && animation2 == null) {
            return;
        }

        for (GraphCanvas frameCanvas : frameCanvases1) {
            content1.remove(frameCanvas);
        }
        frameCanvases1.clear();
        for (Double frameTiming : animation1.frames()) {
            Graph snapshot = dyGraph1.snapshotAt(frameTiming);
            GraphCanvas frameCanvas = new GraphCanvas(snapshot);
            frameCanvases1.add(frameCanvas);
            content1.add(frameCanvas);
        }

        for (GraphCanvas frameCanvas : frameCanvases2) {
            content2.remove(frameCanvas);
        }
        frameCanvases2.clear();
        for (Double frameTiming : animation2.frames()) {
            Graph snapshot = dyGraph2.snapshotAt(frameTiming);
            GraphCanvas frameCanvas = new GraphCanvas(snapshot);
            frameCanvases2.add(frameCanvas);
            content2.add(frameCanvas);
        }

        validate();
    }

    /**
     * Sets the canvas to be 2D only by disabling rotations.
     */
    public void set2D() {
        canvas1.set2D();
        canvas2.set2D();
    }

    /**
     * Sets the canvas to be 3D by enabling rotations.
     */
    public void set3D() {
        canvas1.set3D();
        canvas2.set3D();
    }

    /**
     * Display the DyQuickViewMulti window.
     *
     */
    public void showNewWindow() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }

    /**
     * Prepares the listener for start and stop commands.
     */
    private class PlayCommandListner implements KeyListener {

        @Override
        public void keyTyped(KeyEvent ke) {
        }

        @Override
        public void keyPressed(KeyEvent ke) {
            switch (ke.getKeyChar()) {
                case 'p':
                    startPlaying();
                    break;
                case 's':
                    stopPlaying();
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent ke) {
        }
    }

    /**
     * Performs the operations required to start the animation.
     */
    private void startPlaying() {
        if (animation1 == null || animation2 == null) {
            return;
        }
        if (refreshTaskHandle1 != null || refreshTaskHandle2 != null) {
            stopPlaying();
        }
        content1.addKeyListener(currentListner);
        content2.addKeyListener(currentListner);
        content1.requestFocusInWindow();
        content2.requestFocusInWindow();

        canvas1.disableCameraControl();
        for (GraphCanvas frameCanvas : frameCanvases1) {
            frameCanvas.copyCameraSettings(canvas1);
            frameCanvas.viewAngleMoved();
            frameCanvas.disableCameraControl();
        }



        canvas2.disableCameraControl();
        for (GraphCanvas frameCanvas : frameCanvases2) {
            frameCanvas.copyCameraSettings(canvas2);
            frameCanvas.viewAngleMoved();
            frameCanvas.disableCameraControl();
        }

        final MultiAnimationTask multiAnimationTask = new MultiAnimationTask();
        int refreshPeriod1 = 1000 / animation1.framesPerSecond();
        refreshTaskHandle1 = scheduler.scheduleAtFixedRate(multiAnimationTask,
                0, refreshPeriod1, TimeUnit.MILLISECONDS);
        refreshTaskHandle2 = scheduler.scheduleAtFixedRate(multiAnimationTask,
                0, refreshPeriod1, TimeUnit.MILLISECONDS);
    }

    /**
     * Performs the operations required to stop the animation.
     */
    private void stopPlaying() {
        if (refreshTaskHandle1 != null || refreshTaskHandle2 != null) {
            refreshTaskHandle1.cancel(false);
            refreshTaskHandle2.cancel(false);
            refreshTaskHandle1 = null;
            refreshTaskHandle2 = null;
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new IllegalStateException("Something went wrong");
            }
            content1.removeKeyListener(currentListner);
            content2.removeKeyListener(currentListner);
            canvas1.addKeyListener(currentListner);
            canvas2.addKeyListener(currentListner);
            canvas1.enableCameraControl();
            canvas2.enableCameraControl();
            canvas1.requestFocusInWindow();
            canvas2.requestFocusInWindow();
        }
    }

    /**
     * Refreshes the graph canvas view with the current animation.
     */
    private class MultiAnimationTask implements Runnable {

        private int index1;
        private int index2;

        public MultiAnimationTask() {
            this.index1 = 0;
            this.index2 = 0;
        }

        @Override
        public void run() {
            try {
                canvas1 = frameCanvases1.get(index1);
                canvas2 = frameCanvases2.get(index2);
                CardLayout cardLayout1 = (CardLayout) content1.getLayout();
                CardLayout cardLayout2 = (CardLayout) content2.getLayout();
                if (index1 == 0 || index2 == 0) {
                    cardLayout1.first(content1);
                    cardLayout2.first(content2);
                }
                cardLayout1.next(content1);
                cardLayout2.next(content2);
                index1++;
                index2++;
                int maxTime = Math.max(frameCanvases1.size(), frameCanvases2.size());
                if (index1 == maxTime || index2 == maxTime) {
                    stopPlaying();
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
