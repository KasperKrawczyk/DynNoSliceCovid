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
package ocotillo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.*;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import javafx.scene.control.*;
import ocotillo.dygraph.DyGraph;
import ocotillo.dygraph.extra.DyClustering;
import ocotillo.dygraph.extra.SpaceTimeCubeSynchroniser;
import ocotillo.dygraph.layout.fdl.modular.DyModularFdl;
import ocotillo.dygraph.rendering.Animation;
import ocotillo.graph.Graph;
import ocotillo.graph.layout.fdl.modular.ModularStatistics;
import ocotillo.gui.quickview.*;
import ocotillo.various.ColorCollection;

/**
 * Default GUI for continuous graph experiments.
 */
public class Gui extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Color activeButton = new Color(220, 245, 220);
    private final JTabbedPane tabbedPane = new JTabbedPane();

    public Gui() {
        setTitle("Continuous Dynamic Graph Experiments");
        setSize(1450, 1000);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        add(Box.createRigidArea(new Dimension(10, 10)));
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        JLabel title = new JLabel("Continuous Dynamic Graph Experiments");
        Font currentFont = title.getFont();
        title.setFont(new Font(currentFont.getName(), Font.BOLD, 20));

        header.add(Box.createRigidArea(new Dimension(15, 15)));
        header.add(title);
        header.add(Box.createGlue());
        JButton instructionButton = new JButton("Instructions");
        instructionButton.setBackground(activeButton);
        instructionButton.addActionListener((ActionEvent ae) -> {
            JDialog dialog = new JDialog(this, "Instruction Dialog",
                    Dialog.ModalityType.MODELESS);
            dialog.add(new JLabel(instructions()));
            dialog.setBounds(100, 100, 600, 700);
            dialog.setVisible(true);
        });
        header.add(instructionButton);
        header.add(Box.createRigidArea(new Dimension(15, 15)));
        add(header);


        add(Box.createRigidArea(new Dimension(15, 15)));
        add(tabbedPane);

        addExperiment(new Experiment.Bunt());
        addExperiment(new Experiment.Newcomb());
        addExperiment(new Experiment.InfoVis());
        addExperiment(new Experiment.Rugby());
        addExperiment(new Experiment.Pride());
        //addExperiment(new Experiment.Covid());
        addCovidExperiment(new Experiment.Covid());
        pack();
    }

    private void addExperiment(Experiment experiment) {
        JPanel experimentPanel = new ExperimentPanel(experiment);
        experimentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        tabbedPane.addTab(experiment.name, experimentPanel);
    }

    private void addCovidExperiment(Experiment.Covid covidExperiment) {
        JPanel covidExperimentPanel = new CovidExperimentPanel(covidExperiment);
        covidExperimentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        tabbedPane.addTab(covidExperiment.name, covidExperimentPanel);
    }

    public String instructions() {
        return "<html><h1>Instructions</h1>"
                + "<p> First, you need to parse or compute the graph you want to visualise. "
                + "Computing certain graphs, in particular with the discrete algorithm, can take up "
                + "to a half hour (check the paper for the expected running time). "
                + "Once the computation is ended, the buttons to view and cluster the graph activate.</p>"
                + "<h2>Graph Navigation</h2>"
                + "<p>Both the space-time cube and the dynamic graph can be panned by clicking and "
                + "dragging with the left mouse button. Zoom can be performed using the mouse wheel. "
                + "It is possible to recenter the view and reset the zoom using the 'r' key.</p>"
                + "<h2>Graph Rotations</h2>"
                + "<p>The axes can be rotated by clicking and dragging the right mouse button. "
                + "It is also possible decide which axis should be put as third dimension by "
                + "using the keys 'x', 'y' or 'z'. For example, the standard x-y 2D view is obtained "
                + "by pressing 'z'."
                + "<h2>Dynamic Graph Animation</h2>"
                + "<p>The dynamic graph (not the space-time cube) can be animated to see the evolution "
                + "over time by pressing 'p'. The animation can be restarted at any time by pressing 'p'. "
                + "The animation can be stopped by pressing 's'. During an animation, it is no more possible"
                + "to navigate or rotate the graph. If this is required, stop the animation, select the "
                + "desired level of zoom and view point and restart the animation.</p>"
                + "<h2>Dynamic Graph Clustering</h2>"
                + "<p>The graph can be clustered by using the k-means algorithm. The algorithm can be "
                + "applied to the time dimension only or to the entire space-time cube. For colouring "
                + "and perception issues, the maximum number of clusters is 12. Once the clustering "
                + "is applied, it can be visualised by opening a new dynamic graph view window. Old "
                + "windows are not updated to reflect the latest clustering.</p>"
                + "<h2>Cluster Visualisation</h2>"
                + "<p>The computed clusters can also be seen as a flattened dynamic graph. Once a clustering "
                + "is computed, the view cluster button is activated. It is possible to select a cluster "
                + "index using the appropriate spinner field and open it as a new static graph by pressing "
                + "the view button.</p>"
                + "</html>";
    }

    public static class ExperimentPanel extends JPanel {

        private final JPanel visonePanel;
        private final ContentPanel discretePanel;
        private final ContentPanel continuousPanel;

        private static final long serialVersionUID = 1L;

        public ExperimentPanel(Experiment experiment) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
            visonePanel = new VisonePanel(experiment);
            contentPanel.add(visonePanel);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            discretePanel = new DiscretePanel(experiment);
            contentPanel.add(discretePanel);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            continuousPanel = new ContinuousPanel(experiment);
            contentPanel.add(continuousPanel);

            add(Box.createRigidArea(new Dimension(10, 10)));
            this.add(contentPanel);
        }
    }

    public static class CovidExperimentPanel extends JPanel {

        //private final JPanel visonePanel;
        private final ContentPanel discretePanel;
        private final ContentPanel continuousPanel;
        private final CovidContinuousPanel covidContinuousPanel;
        private final CovidContinuousPanelWithLocationFilter covidContinuousPanelWithLocationFilter;
        private final CovidContinuousPanelWithTransmissionFilter covidContinuousPanelWithTransmissionFilter;
        private final CovidContinuousPanelWithLocationAttraction covidContinuousPanelWithLocationAttraction;
        private final CovidContinuousPanelWithMultipleLocationsAttraction covidContinuousPanelWithMultipleLocationsAttraction;

        private static final long serialVersionUID = 1L;

        public CovidExperimentPanel(Experiment.Covid experiment) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            JPanel contentPanel = new JPanel();

            //visonePanel = new VisonePanel(experiment);
            //contentPanel.add(visonePanel);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            discretePanel = new DiscretePanel(experiment);
            contentPanel.add(discretePanel);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            continuousPanel = new ContinuousPanel(experiment);
            contentPanel.add(continuousPanel);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            covidContinuousPanel = new CovidContinuousPanel(experiment);
            contentPanel.add(covidContinuousPanel);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            covidContinuousPanelWithLocationFilter = new CovidContinuousPanelWithLocationFilter(experiment);
            contentPanel.add(covidContinuousPanelWithLocationFilter);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            covidContinuousPanelWithTransmissionFilter = new CovidContinuousPanelWithTransmissionFilter(experiment);
            contentPanel.add(covidContinuousPanelWithTransmissionFilter);

            contentPanel.add(Box.createRigidArea(new Dimension(25, 25)));

            covidContinuousPanelWithLocationAttraction = new CovidContinuousPanelWithLocationAttraction(experiment);
            contentPanel.add(covidContinuousPanelWithLocationAttraction);

            covidContinuousPanelWithMultipleLocationsAttraction = new CovidContinuousPanelWithMultipleLocationsAttraction(experiment);
            contentPanel.add(covidContinuousPanelWithMultipleLocationsAttraction);

            GroupLayout contentPanelLayout = new GroupLayout(contentPanel);

            contentPanelLayout.setAutoCreateGaps(true);
            contentPanelLayout.setAutoCreateContainerGaps(true);

            contentPanel.setLayout(contentPanelLayout);

            contentPanelLayout.setHorizontalGroup((contentPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addGroup(contentPanelLayout.createSequentialGroup()
                            .addComponent(discretePanel)
                            .addComponent(continuousPanel)
                            .addComponent(covidContinuousPanel))
                    .addGroup(contentPanelLayout.createSequentialGroup()
                            .addComponent(covidContinuousPanelWithLocationFilter)
                            .addComponent(covidContinuousPanelWithTransmissionFilter)
                            .addComponent(covidContinuousPanelWithLocationAttraction))
                    .addGroup(contentPanelLayout.createSequentialGroup()
                            .addComponent(covidContinuousPanelWithMultipleLocationsAttraction))));

            contentPanelLayout.setVerticalGroup(contentPanelLayout.createSequentialGroup()
                    .addGroup(contentPanelLayout.createParallelGroup()
                            .addComponent(discretePanel)
                            .addComponent(continuousPanel)
                            .addComponent(covidContinuousPanel))
                    .addGroup(contentPanelLayout.createParallelGroup()
                            .addComponent(covidContinuousPanelWithLocationFilter)
                            .addComponent(covidContinuousPanelWithTransmissionFilter)
                            .addComponent(covidContinuousPanelWithLocationAttraction))
                    .addGroup(contentPanelLayout.createParallelGroup()
                            .addComponent(covidContinuousPanelWithMultipleLocationsAttraction)));


            add(Box.createRigidArea(new Dimension(10, 10)));
            this.add(contentPanel);
        }
    }

    public static abstract class ContentPanel extends JPanel {

        protected final Experiment experiment;
        protected final String type;
        protected final JLabel titleLabel;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;
        protected final JPanel clusterRow;
        protected final JSpinner kSpinner;
        protected final JButton onTimeButton;
        protected final JButton onCubeButton;
        protected final JPanel clusterViewRow;
        protected final JSpinner clusterSpinner;
        protected final JButton viewClusterButton;
        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro;

        private static final long serialVersionUID = 1L;

        public ContentPanel(String type, String buttonText, Experiment experiment) {
            this.type = type;
            this.experiment = experiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 180, 20);

            computeButton = new JButton(buttonText);
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                viewAnimation();
            });
            viewAnimationButton.setEnabled(false);

            clusterRow = new JPanel();
            clusterRow.setLayout(new BoxLayout(clusterRow, BoxLayout.X_AXIS));
            JLabel clusterRowLabel = new JLabel("Cluster with k: ");
            setSize(clusterRowLabel, 110, 25);
            clusterRow.add(clusterRowLabel);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            kSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));
            setSize(kSpinner, 40, 25);
            clusterRow.add(kSpinner);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            onTimeButton = new JButton("On Time");
            onTimeButton.setEnabled(false);
            setSize(onTimeButton, 100, 25);
            clusterRow.add(onTimeButton);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            onCubeButton = new JButton("On Cube");
            onCubeButton.setEnabled(false);
            setSize(onCubeButton, 100, 25);
            clusterRow.add(onCubeButton);
            clusterRow.setAlignmentX(LEFT_ALIGNMENT);

            clusterViewRow = new JPanel();
            clusterViewRow.setLayout(new BoxLayout(clusterViewRow, BoxLayout.X_AXIS));
            JLabel clusterViewLabel = new JLabel("View cluster: ");
            setSize(clusterViewLabel, 110, 25);
            clusterViewRow.add(clusterViewLabel);
            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));

            clusterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
            setSize(clusterSpinner, 40, 25);
            clusterViewRow.add(clusterSpinner);
            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));

            viewClusterButton = new JButton("View");
            viewClusterButton.setEnabled(false);
            setSize(viewClusterButton, 205, 25);
            clusterViewRow.add(viewClusterButton);
            clusterViewRow.setAlignmentX(LEFT_ALIGNMENT);

            preComputationLayout();

            onTimeButton.addActionListener((ActionEvent ae) -> {
                int k = (int) kSpinner.getValue();
                cluster(new DyClustering.Stc.KMeansTime(
                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
                        experiment.delta / 3.0, k,
                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
            });

            onCubeButton.addActionListener((ActionEvent ae) -> {
                int k = (int) kSpinner.getValue();
                cluster(new DyClustering.Stc.KMeans3D(
                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
                        experiment.delta / 3.0, k,
                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
            });

            viewClusterButton.addActionListener((ActionEvent ae) -> {
                int clusterNumber = (int) clusterSpinner.getValue() - 1;
                QuickView.showNewWindow(flattenedClusters.get(clusterNumber));
            });
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                synchro = getSynchro();
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
                onTimeButton.setBackground(activeButton);
                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                onTimeButton.setEnabled(true);
                onCubeButton.setEnabled(true);
            });
        }

        private void cluster(DyClustering clustering) {
            int k = (int) kSpinner.getValue();
            clustering.colorGraph();
            flattenedClusters = clustering.flattenClusters();
            clusterSpinner.setModel(new SpinnerNumberModel(1, 1, k, 1));
            viewClusterButton.setBackground(activeButton);
            clusterSpinner.setEnabled(true);
            viewClusterButton.setEnabled(true);
//            for (int i = 0; i < flattenedClusters.size(); i++) {
//                Graph graph = flattenedClusters.get(i);
//                SvgExporter.saveSvg(graph, new File(experiment.name + "_" + type + "_" + i + ".svg"));
//            }
        }

        protected void viewCube() {
            QuickView.showNewWindow(synchro.mirrorGraph());
        }

        private void viewAnimation() {
            DyQuickView view = new DyQuickView(synchro.originalGraph(), experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));
            view.showNewWindow();
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }

        protected abstract SpaceTimeCubeSynchroniser getSynchro();

        protected void showErrorDialog(String errorMessage){
            JOptionPane.showMessageDialog(new JFrame(), errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class VisonePanel extends ContentPanel {

        private static final long serialVersionUID = 1L;

        public VisonePanel(Experiment experiment) {
            super("Visone", "Parse layout", experiment);
        }

        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() {
            DyGraph discreteGraph = experiment.discretise();
            experiment.importVisone(experiment.directory, discreteGraph);
            updateComputationReport("Graph successfully parsed.");
            return new SpaceTimeCubeSynchroniser.StcsBuilder(
                    discreteGraph, experiment.dataset.suggestedTimeFactor).build();
        }
    }

    private static class DiscretePanel extends ContentPanel {

        private static final long serialVersionUID = 1L;

        public DiscretePanel(Experiment experiment) {
            super("Discrete", "Compute layout", experiment);
        }

        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() {
            DyGraph graph = experiment.discretise();
            DyModularFdl algorithm = experiment.getDiscreteLayoutAlgorithm(graph, null);
            SpaceTimeCubeSynchroniser syncrho = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return syncrho;
        }
    }

    private static class ContinuousPanel extends ContentPanel {

        private static final long serialVersionUID = 1L;

        public ContinuousPanel(Experiment experiment) {
            super("Continuous", "Compute layout", experiment);
        }

        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() {
            DyGraph graph = experiment.getContinuousCopy();
            DyModularFdl algorithm = experiment.getContinuousLayoutAlgorithm(graph, null);
            SpaceTimeCubeSynchroniser synchro = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return synchro;
        }
    }

    private static class CovidContinuousPanel extends ContentPanel {
        protected final Experiment.Covid covidExperiment;

        protected final JLabel titleLabel;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;
        protected final JPanel clusterRow;
        protected final JSpinner kSpinner;
        protected final JButton onTimeButton;
        protected final JButton onCubeButton;
        protected final JPanel clusterViewRow;
        protected final JSpinner clusterSpinner;
        protected final JButton viewClusterButton;
        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro;

        private static final long serialVersionUID = 1L;
        private final JComboBox<String> locationOptionsComboBox;
        private String selectedLocation;

        public CovidContinuousPanel(Experiment.Covid covidExperiment) {
            super("Continuous with Location Highlight", "Compute layout", covidExperiment);

            this.covidExperiment = covidExperiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 220, 20);

            ArrayList<String> comboBoxOptionsArrayList = new ArrayList<>(covidExperiment.locationHighlightOptions);
            comboBoxOptionsArrayList.add(0, "No Highlight");

            String[] comboBoxOptions = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBox = new JComboBox<>(comboBoxOptions);
            ((JLabel) locationOptionsComboBox.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBox.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBox, 365, 25);
            locationOptionsComboBox.setBackground(activeButton);
            locationOptionsComboBox.addActionListener((ActionEvent ae) -> {
                selectedLocation = (String) locationOptionsComboBox.getSelectedItem();
                if(selectedLocation.equalsIgnoreCase("No Highlight")){
                    selectedLocation = null;
                }
            });
            locationOptionsComboBox.setToolTipText("Choose a location or setting to highlight in the animation");


            computeButton = new JButton("Compute layout");
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                System.out.println("selectedLocation = " + selectedLocation);
                viewCovidAnimationWithLocation();
            });
            viewAnimationButton.setEnabled(false);


            clusterRow = new JPanel();
            clusterRow.setLayout(new BoxLayout(clusterRow, BoxLayout.X_AXIS));
            JLabel clusterRowLabel = new JLabel("Cluster with k: ");
            setSize(clusterRowLabel, 110, 25);
            clusterRow.add(clusterRowLabel);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            kSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));
            setSize(kSpinner, 40, 25);
            clusterRow.add(kSpinner);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            onTimeButton = new JButton("On Time");
            onTimeButton.setEnabled(false);
            setSize(onTimeButton, 100, 25);
            clusterRow.add(onTimeButton);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            onCubeButton = new JButton("On Cube");
            onCubeButton.setEnabled(false);
            setSize(onCubeButton, 100, 25);
            clusterRow.add(onCubeButton);
            clusterRow.setAlignmentX(LEFT_ALIGNMENT);

            clusterViewRow = new JPanel();
            clusterViewRow.setLayout(new BoxLayout(clusterViewRow, BoxLayout.X_AXIS));
            JLabel clusterViewLabel = new JLabel("View cluster: ");
            setSize(clusterViewLabel, 110, 25);
            clusterViewRow.add(clusterViewLabel);
            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));

            clusterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
            setSize(clusterSpinner, 40, 25);
            clusterViewRow.add(clusterSpinner);
            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));

            viewClusterButton = new JButton("View");
            viewClusterButton.setEnabled(false);
            setSize(viewClusterButton, 205, 25);
            clusterViewRow.add(viewClusterButton);
            clusterViewRow.setAlignmentX(LEFT_ALIGNMENT);

            preComputationLayout();

            onTimeButton.addActionListener((ActionEvent ae) -> {
                int k = (int) kSpinner.getValue();
                cluster(new DyClustering.Stc.KMeansTime(
                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
                        experiment.delta / 3.0, k,
                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
            });

            onCubeButton.addActionListener((ActionEvent ae) -> {
                int k = (int) kSpinner.getValue();
                cluster(new DyClustering.Stc.KMeans3D(
                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
                        experiment.delta / 3.0, k,
                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
            });

            viewClusterButton.addActionListener((ActionEvent ae) -> {
                int clusterNumber = (int) clusterSpinner.getValue() - 1;
                QuickView.showNewWindow(flattenedClusters.get(clusterNumber));
            });
        }


        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() {
            DyGraph graph = covidExperiment.getContinuousCopyWithLocations(selectedLocation);
            DyModularFdl algorithm = experiment.getContinuousLayoutAlgorithm(graph, null);
            SpaceTimeCubeSynchroniser synchro = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return synchro;
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                synchro = getSynchro();
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
                onTimeButton.setBackground(activeButton);
                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                locationOptionsComboBox.setEnabled(false);
                onTimeButton.setEnabled(true);
                onCubeButton.setEnabled(true);
            });
        }

        private void cluster(DyClustering clustering) {
            int k = (int) kSpinner.getValue();
            clustering.colorGraph();
            flattenedClusters = clustering.flattenClusters();
            clusterSpinner.setModel(new SpinnerNumberModel(1, 1, k, 1));
            viewClusterButton.setBackground(activeButton);
            clusterSpinner.setEnabled(true);
            viewClusterButton.setEnabled(true);
//            for (int i = 0; i < flattenedClusters.size(); i++) {
//                Graph graph = flattenedClusters.get(i);
//                SvgExporter.saveSvg(graph, new File(experiment.name + "_" + type + "_" + i + ".svg"));
//            }
        }

        protected void viewCube() {
            QuickView.showNewWindow(synchro.mirrorGraph());
        }

        private void viewCovidAnimationWithLocation() {
            DyQuickView view = new DyQuickView(synchro.originalGraph(), experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));
            view.showNewWindow();

            //TODO here we could put more view.showNewWindow for a bunch of canvases, or group a few animations together
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }
    }

    private static class CovidContinuousPanelWithLocationFilter extends ContentPanel {
        protected final Experiment.Covid covidExperiment;

        protected final JLabel titleLabel;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;

        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro;

        private static final long serialVersionUID = 1L;
        private final JComboBox<String> locationOptionsComboBox;
        private String selectedLocation;

        private final JSlider filterSlider;
        private static final int MIN_FILTER = 0;
        private static final int MAX_FILTER = 100;
        private static final int INIT_FILTER = 0;
        private double selectedFilterFactor;


        public CovidContinuousPanelWithLocationFilter(Experiment.Covid covidExperiment) {
            super("Continuous Location Filter", "Compute layout", covidExperiment);

            this.covidExperiment = covidExperiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 220, 20);

            ArrayList<String> comboBoxOptionsArrayList = new ArrayList<>(covidExperiment.locationHighlightOptions);
            comboBoxOptionsArrayList.add(0, "No Filter");

            String[] comboBoxOptions = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBox = new JComboBox<>(comboBoxOptions);
            ((JLabel) locationOptionsComboBox.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBox.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBox, 365, 25);
            locationOptionsComboBox.setBackground(activeButton);
            locationOptionsComboBox.addActionListener((ActionEvent ae) -> {
                selectedLocation = (String) locationOptionsComboBox.getSelectedItem();
                if(selectedLocation.equalsIgnoreCase("No Filter")){
                    selectedLocation = null;
                }
                System.out.println("selectedLocation = " + selectedLocation);
            });
            locationOptionsComboBox.setToolTipText("Choose a location or setting filter to show only components with\n" +
                    "50% or over of infections taking place in this location or setting");

            filterSlider = new JSlider(JSlider.HORIZONTAL, MIN_FILTER, MAX_FILTER, INIT_FILTER);
            filterSlider.setAlignmentX(JSlider.LEFT_ALIGNMENT);
            setSize(filterSlider, 365, 35);
            filterSlider.setFont(new Font(filterSlider.getFont().getName(), Font.PLAIN, 8));
            filterSlider.addChangeListener((ChangeEvent ce) -> {
                selectedFilterFactor = (double) filterSlider.getValue() / 100;
                System.out.println("filterFactor = " + selectedFilterFactor);
            });
            filterSlider.setMajorTickSpacing(25);
            filterSlider.setMinorTickSpacing(5);
            filterSlider.setPaintTicks(true);
            filterSlider.setPaintLabels(true);


            computeButton = new JButton("Compute layout");
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                System.out.println("selectedLocation = " + selectedLocation);
                viewCovidAnimationWithLocationFilter();
            });
            viewAnimationButton.setEnabled(false);

            preComputationLayout();

        }


        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() throws NoSuchElementException{
            DyGraph graph = covidExperiment.getContinuousCopyWithLocationFilter(selectedLocation, selectedFilterFactor);
            DyModularFdl algorithm = experiment.getContinuousLayoutAlgorithm(graph, null);
            SpaceTimeCubeSynchroniser synchro = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return synchro;
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(filterSlider);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(filterSlider);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                try {
                    synchro = getSynchro();
                } catch (NoSuchElementException e){
                    System.out.println("AHA"); //TODO reset!
                    showErrorDialog("Selected filter factor too large for this location.\nPlease try a smaller one.");
//                    filterSlider.setEnabled(true);
//                    locationOptionsComboBox.setEnabled(true);
                    preComputationLayout();
                    return;
                }
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
                onTimeButton.setBackground(activeButton);
                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                locationOptionsComboBox.setEnabled(false);
                filterSlider.setEnabled(false);
                onTimeButton.setEnabled(true);
                onCubeButton.setEnabled(true);
            });
        }
        protected void viewCube() {
            QuickView.showNewWindow(synchro.mirrorGraph());
        }

        private void viewCovidAnimationWithLocationFilter() {
            DyQuickView view = new DyQuickView(synchro.originalGraph(), experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));
            view.showNewWindow();
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }
    }

    private static class CovidContinuousPanelWithLocationAttraction extends ContentPanel {
        protected final Experiment.Covid covidExperiment;

        protected final JLabel titleLabel;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;

        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro;

        private static final long serialVersionUID = 1L;
        private final JComboBox<String> locationOptionsComboBox;
        private String selectedLocation;



        public CovidContinuousPanelWithLocationAttraction(Experiment.Covid covidExperiment) {
            super("Continuous Location Attraction", "Compute layout", covidExperiment);

            this.covidExperiment = covidExperiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 220, 20);

            ArrayList<String> comboBoxOptionsArrayList = new ArrayList<>(covidExperiment.locationHighlightOptions);
            comboBoxOptionsArrayList.add(0, "No Filter");

            String[] comboBoxOptions = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBox = new JComboBox<>(comboBoxOptions);
            ((JLabel) locationOptionsComboBox.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBox.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBox, 365, 25);
            locationOptionsComboBox.setBackground(activeButton);
            locationOptionsComboBox.addActionListener((ActionEvent ae) -> {
                selectedLocation = (String) locationOptionsComboBox.getSelectedItem();
                if(selectedLocation.equalsIgnoreCase("No Filter")){
                    selectedLocation = null;
                }
                System.out.println("selectedLocation = " + selectedLocation);
            });
            locationOptionsComboBox.setToolTipText("Choose a location or setting to group on the canvas");


            computeButton = new JButton("Compute layout");
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                System.out.println("selectedLocation = " + selectedLocation);
                viewCovidAnimationWithLocationAttraction();
            });
            viewAnimationButton.setEnabled(false);

            preComputationLayout();

        }


        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() throws NoSuchElementException{
            DyGraph graph = covidExperiment.getContinuousCopyWithLocationAttraction(selectedLocation);
            DyModularFdl algorithm = experiment.getContinuousLayoutAlgorithm(graph, null);
            SpaceTimeCubeSynchroniser synchro = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return synchro;
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                synchro = getSynchro();
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
                onTimeButton.setBackground(activeButton);
                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                locationOptionsComboBox.setEnabled(false);
                onTimeButton.setEnabled(true);
                onCubeButton.setEnabled(true);
            });
        }
        protected void viewCube() {
            QuickView.showNewWindow(synchro.mirrorGraph());
        }

        private void viewCovidAnimationWithLocationAttraction() {
            DyQuickView view = new DyQuickView(synchro.originalGraph(), experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));
            view.showNewWindow();
            System.out.println("I do get indeed executed");
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }
    }

    private static class CovidContinuousPanelWithMultipleLocationsAttraction extends ContentPanel {
        protected final Experiment.Covid covidExperiment;

        protected final JLabel titleLabel;
        protected final JButton pinNodesButton;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;

        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro;

        private static final long serialVersionUID = 1L;
        private boolean isPinned = false;
        private final JList<String> locationOptionsList;
        private List<String> selectedLocationsList;
        protected final JPanel locationOptionsListPanel;

        public CovidContinuousPanelWithMultipleLocationsAttraction(Experiment.Covid covidExperiment) {
            super("Cont. Multi-Locations Attraction", "Compute layout", covidExperiment);

            this.covidExperiment = covidExperiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 220, 20);

            pinNodesButton = new JButton("Pin pole nodes");
            pinNodesButton.setToolTipText("Click to keep pole nodes pinned");
            setSize(pinNodesButton, 365, 25);
            pinNodesButton.setBackground(activeButton);
            pinNodesButton.addActionListener((ActionEvent ae) -> {
                isPinned = true;
                pinNodesButton.setEnabled(false);
            });

            ArrayList<String> listOptionsArrayList = new ArrayList<>(covidExperiment.locationHighlightOptions);
            //listOptionsArrayList.add(0, "No poles chosen");

            String[] listOptions = listOptionsArrayList.toArray(new String[0]);
            selectedLocationsList = new ArrayList<>();

            locationOptionsListPanel = new JPanel();
            locationOptionsList = new JList<>(listOptions);
            locationOptionsListPanel.add(locationOptionsList);
            locationOptionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            locationOptionsList.setLayoutOrientation(JList.VERTICAL);
            locationOptionsList.setAlignmentX(JComboBox.CENTER_ALIGNMENT);
            locationOptionsList.setVisibleRowCount(-1);
            //setSize(locationOptionsList, 365, 70);
            locationOptionsList.setBackground(activeButton);
            locationOptionsList.addListSelectionListener((ListSelectionEvent listSelectionEvent) -> {
                if(!locationOptionsList.getValueIsAdjusting()){
                    selectedLocationsList = locationOptionsList.getSelectedValuesList();
                    if(locationOptionsList.getSelectedValue().equalsIgnoreCase("No Pole")){
                        selectedLocationsList = null;
                    }
                    System.out.println("THE LIST OF SELECTED LOCATIONS");
                    selectedLocationsList.forEach(System.out::println);
                }

            });
            locationOptionsList.setToolTipText("Choose locations and settings to group nodes by");
            JScrollPane locationOptionsScrollPane = new JScrollPane(locationOptionsList);
            locationOptionsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            locationOptionsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            locationOptionsScrollPane.createVerticalScrollBar();
            locationOptionsListPanel.add(locationOptionsScrollPane);
            setSize(locationOptionsScrollPane, 365, 60);

            computeButton = new JButton("Compute layout");
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                selectedLocationsList.forEach(System.out::println);
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                selectedLocationsList.forEach(System.out::println);
                viewCovidAnimationWithMultipleLocationAttraction();
            });
            viewAnimationButton.setEnabled(false);

            preComputationLayout();

        }


        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() throws NoSuchElementException{
            DyGraph graph = covidExperiment.getContinuousCopyWithMultipleLocationsAttraction(selectedLocationsList);
            DyModularFdl algorithm = experiment.getContinuousLayoutAlgorithmCovid(graph, null, isPinned);
            SpaceTimeCubeSynchroniser synchro = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return synchro;
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(pinNodesButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsListPanel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(pinNodesButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsListPanel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                synchro = getSynchro();
                pinNodesButton.setEnabled(false);
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
                onTimeButton.setBackground(activeButton);
                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                locationOptionsList.setEnabled(false);
                onTimeButton.setEnabled(true);
                onCubeButton.setEnabled(true);
            });
        }
        protected void viewCube() {
            QuickView.showNewWindow(synchro.mirrorGraph());
        }

        private void viewCovidAnimationWithMultipleLocationAttraction() {
            DyQuickView view = new DyQuickView(synchro.originalGraph(), experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));
            view.showNewWindow();
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }

    }

    private static class CovidContinuousPanelWithTransmissionFilter extends ContentPanel {
        protected final Experiment.Covid covidExperiment;

        protected final JLabel titleLabel;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;
        protected final JPanel clusterRow;
        protected final JSpinner kSpinner;
        protected final JButton onTimeButton;
        protected final JButton onCubeButton;
        protected final JPanel clusterViewRow;
        protected final JSpinner clusterSpinner;
        protected final JButton viewClusterButton;
        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro;

        private static final long serialVersionUID = 1L;
        private final JComboBox<String> locationOptionsComboBoxFrom;
        private final JComboBox<String> locationOptionsComboBoxTo;
        private String selectedLocationFrom;
        private String selectedLocationTo;

        public CovidContinuousPanelWithTransmissionFilter(Experiment.Covid covidExperiment) {
            super("Continuous Transmission Highlight", "Compute layout", covidExperiment);

            this.covidExperiment = covidExperiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 220, 20);

            ArrayList<String> comboBoxOptionsArrayList = new ArrayList<>(covidExperiment.locationHighlightOptions);
            comboBoxOptionsArrayList.add(0, "No Highlight For Source Location");

            String[] comboBoxOptionsFrom = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBoxFrom = new JComboBox<>(comboBoxOptionsFrom);
            ((JLabel) locationOptionsComboBoxFrom.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBoxFrom.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBoxFrom, 365, 25);
            locationOptionsComboBoxFrom.setBackground(activeButton);
            locationOptionsComboBoxFrom.addActionListener((ActionEvent ae) -> {
                selectedLocationFrom = (String) locationOptionsComboBoxFrom.getSelectedItem();
                if(selectedLocationFrom.equalsIgnoreCase("No Highlight For Source Location")){
                    System.out.println(selectedLocationFrom);
                    selectedLocationFrom = null;
                }
            });
            locationOptionsComboBoxFrom.setToolTipText("Choose a location or setting to highlight in the animation");

            comboBoxOptionsArrayList.remove(0);
            comboBoxOptionsArrayList.add(0, "No Highlight For Target Location");
            String[] comboBoxOptionsTo = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBoxTo = new JComboBox<>(comboBoxOptionsTo);
            ((JLabel) locationOptionsComboBoxTo.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBoxTo.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBoxTo, 365, 25);
            locationOptionsComboBoxTo.setBackground(activeButton);
            locationOptionsComboBoxTo.addActionListener((ActionEvent ae) -> {
                selectedLocationTo = (String) locationOptionsComboBoxTo.getSelectedItem();
                if(selectedLocationTo.equalsIgnoreCase("No Highlight For Target Location")){
                    System.out.println(selectedLocationTo);
                    selectedLocationTo = null;
                }
            });
            locationOptionsComboBoxTo.setToolTipText("Choose a location or setting to highlight in the animation");


            computeButton = new JButton("Compute layout");
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                System.out.println("selectedLocationFrom = " + selectedLocationFrom);
                System.out.println("selectedLocationTo = " + selectedLocationTo);
                viewCovidAnimationWithLocation();
            });
            viewAnimationButton.setEnabled(false);


            clusterRow = new JPanel();
            clusterRow.setLayout(new BoxLayout(clusterRow, BoxLayout.X_AXIS));
            JLabel clusterRowLabel = new JLabel("Cluster with k: ");
            setSize(clusterRowLabel, 110, 25);
            clusterRow.add(clusterRowLabel);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            kSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));
            setSize(kSpinner, 40, 25);
            clusterRow.add(kSpinner);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            onTimeButton = new JButton("On Time");
            onTimeButton.setEnabled(false);
            setSize(onTimeButton, 100, 25);
            clusterRow.add(onTimeButton);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            onCubeButton = new JButton("On Cube");
            onCubeButton.setEnabled(false);
            setSize(onCubeButton, 100, 25);
            clusterRow.add(onCubeButton);
            clusterRow.setAlignmentX(LEFT_ALIGNMENT);

            clusterViewRow = new JPanel();
            clusterViewRow.setLayout(new BoxLayout(clusterViewRow, BoxLayout.X_AXIS));
            JLabel clusterViewLabel = new JLabel("View cluster: ");
            setSize(clusterViewLabel, 110, 25);
            clusterViewRow.add(clusterViewLabel);
            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));

            clusterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
            setSize(clusterSpinner, 40, 25);
            clusterViewRow.add(clusterSpinner);
            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));

            viewClusterButton = new JButton("View");
            viewClusterButton.setEnabled(false);
            setSize(viewClusterButton, 205, 25);
            clusterViewRow.add(viewClusterButton);
            clusterViewRow.setAlignmentX(LEFT_ALIGNMENT);

            preComputationLayout();

            onTimeButton.addActionListener((ActionEvent ae) -> {
                int k = (int) kSpinner.getValue();
                cluster(new DyClustering.Stc.KMeansTime(
                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
                        experiment.delta / 3.0, k,
                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
            });

            onCubeButton.addActionListener((ActionEvent ae) -> {
                int k = (int) kSpinner.getValue();
                cluster(new DyClustering.Stc.KMeans3D(
                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
                        experiment.delta / 3.0, k,
                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
            });

            viewClusterButton.addActionListener((ActionEvent ae) -> {
                int clusterNumber = (int) clusterSpinner.getValue() - 1;
                QuickView.showNewWindow(flattenedClusters.get(clusterNumber));
            });
        }


        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() {
            DyGraph graph = covidExperiment.getContinuousCopyWithTransmission(selectedLocationFrom, selectedLocationTo);
            DyModularFdl algorithm = experiment.getContinuousLayoutAlgorithm(graph, null);
            SpaceTimeCubeSynchroniser synchro = algorithm.getSyncro();
            ModularStatistics stats = algorithm.iterate(100);
            String time = stats.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats.getTotalRunnningTime().getNano() / 10000000);
            updateComputationReport("Layout computed in " + time + " s");
            return synchro;
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBoxFrom);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBoxTo);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBoxFrom);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBoxTo);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterRow);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                synchro = getSynchro();
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
                onTimeButton.setBackground(activeButton);
                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                locationOptionsComboBoxFrom.setEnabled(false);
                locationOptionsComboBoxTo.setEnabled(false);
                onTimeButton.setEnabled(true);
                onCubeButton.setEnabled(true);
            });
        }

        private void cluster(DyClustering clustering) {
            int k = (int) kSpinner.getValue();
            clustering.colorGraph();
            flattenedClusters = clustering.flattenClusters();
            clusterSpinner.setModel(new SpinnerNumberModel(1, 1, k, 1));
            viewClusterButton.setBackground(activeButton);
            clusterSpinner.setEnabled(true);
            viewClusterButton.setEnabled(true);
//            for (int i = 0; i < flattenedClusters.size(); i++) {
//                Graph graph = flattenedClusters.get(i);
//                SvgExporter.saveSvg(graph, new File(experiment.name + "_" + type + "_" + i + ".svg"));
//            }
        }

        protected void viewCube() {
            QuickView.showNewWindow(synchro.mirrorGraph());
        }

        private void viewCovidAnimationWithLocation() {
            DyQuickView view = new DyQuickView(synchro.originalGraph(), experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));
            view.showNewWindow();
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }
    }

    private static class CovidContinuousPanelWithMultipleLocationFilters extends ContentPanel {
        protected final Experiment.Covid covidExperiment;

        protected final JLabel titleLabel;
        protected final JButton computeButton;
        protected final JLabel computationReport;
        protected final JButton viewCubeButton;
        protected final JButton viewAnimationButton;
        protected final JPanel clusterRow;
        protected final JSpinner kSpinner;
//        protected final JButton onTimeButton;
//        protected final JButton onCubeButton;
//        protected final JPanel clusterViewRow;
//        protected final JSpinner clusterSpinner;
//        protected final JButton viewClusterButton;
        protected List<Graph> flattenedClusters;

        private SpaceTimeCubeSynchroniser synchro1;
        private SpaceTimeCubeSynchroniser synchro2;

        private static final long serialVersionUID = 1L;
        private final JComboBox<String> locationOptionsComboBox1;
        private final JComboBox<String> locationOptionsComboBox2;

        private List<String> selectedLocations;

        public CovidContinuousPanelWithMultipleLocationFilters(Experiment.Covid covidExperiment) {
            super("Continuous Double Location Highlight", "Compute layout", covidExperiment);

            this.covidExperiment = covidExperiment;
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(type, JLabel.LEFT);
            setSize(titleLabel, 220, 20);

            selectedLocations = new ArrayList<>();

            ArrayList<String> comboBoxOptionsArrayList = new ArrayList<>(covidExperiment.locationHighlightOptions);
            comboBoxOptionsArrayList.add(0, "No Highlight For Location 1");

            String[] comboBoxOptionsFrom = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBox1 = new JComboBox<>(comboBoxOptionsFrom);
            ((JLabel) locationOptionsComboBox1.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBox1.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBox1, 365, 25);
            locationOptionsComboBox1.setBackground(activeButton);
            locationOptionsComboBox1.addActionListener((ActionEvent ae) -> {
                String selectedLocation1 = (String) locationOptionsComboBox1.getSelectedItem();
                selectedLocations.add(selectedLocation1);
                if(selectedLocation1.equalsIgnoreCase("No Highlight For Location 1")){
                    System.out.println(selectedLocation1);
                    selectedLocation1 = null;
                }
            });
            locationOptionsComboBox1.setToolTipText("Choose a location or setting to highlight in the animation");

            comboBoxOptionsArrayList.remove(0);
            comboBoxOptionsArrayList.add(0, "No Highlight For Location 2");
            String[] comboBoxOptionsTo = comboBoxOptionsArrayList.toArray(new String[0]);

            locationOptionsComboBox2 = new JComboBox<>(comboBoxOptionsTo);
            ((JLabel) locationOptionsComboBox2.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            locationOptionsComboBox2.setAlignmentX(JComboBox.LEFT_ALIGNMENT);
            setSize(locationOptionsComboBox2, 365, 25);
            locationOptionsComboBox2.setBackground(activeButton);
            locationOptionsComboBox2.addActionListener((ActionEvent ae) -> {
                String selectedLocation2 = (String) locationOptionsComboBox2.getSelectedItem();
                selectedLocations.add(selectedLocation2);
                if(selectedLocation2.equalsIgnoreCase("No Highlight For Location 2")){
                    System.out.println(selectedLocation2);
                    selectedLocation2 = null;
                }
            });
            locationOptionsComboBox2.setToolTipText("Choose a location or setting to highlight in the animation");


            computeButton = new JButton("Compute layout");
            setSize(computeButton, 365, 25);
            computeButton.addActionListener((ActionEvent ae) -> {
                compute();
            });
            computeButton.setBackground(activeButton);

            computationReport = new JLabel("Computation in progress...");
            setSize(computationReport, 365, 25);

            viewCubeButton = new JButton("View space-time cube");
            setSize(viewCubeButton, 365, 25);
            viewCubeButton.addActionListener((ActionEvent ae) -> {
                viewCube();
            });
            viewCubeButton.setEnabled(false);

            viewAnimationButton = new JButton("View dynamic graph");
            setSize(viewAnimationButton, 365, 25);
            viewAnimationButton.addActionListener((ActionEvent ae) -> {
                System.out.println("selectedLocation1 = " + selectedLocations.get(0));
                System.out.println("selectedLocation2 = " + selectedLocations.get(1));
                viewCovidAnimationWithMultipleLocationFilters();
            });
            viewAnimationButton.setEnabled(false);


            clusterRow = new JPanel();
            clusterRow.setLayout(new BoxLayout(clusterRow, BoxLayout.X_AXIS));
            JLabel clusterRowLabel = new JLabel("Cluster with k: ");
            setSize(clusterRowLabel, 110, 25);
            clusterRow.add(clusterRowLabel);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

            kSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));
            setSize(kSpinner, 40, 25);
            clusterRow.add(kSpinner);
            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));

//            onTimeButton = new JButton("On Time");
//            onTimeButton.setEnabled(false);
//            setSize(onTimeButton, 100, 25);
//            clusterRow.add(onTimeButton);
//            clusterRow.add(Box.createRigidArea(new Dimension(5, 5)));
//
//            onCubeButton = new JButton("On Cube");
//            onCubeButton.setEnabled(false);
//            setSize(onCubeButton, 100, 25);
//            clusterRow.add(onCubeButton);
//            clusterRow.setAlignmentX(LEFT_ALIGNMENT);
//
//            clusterViewRow = new JPanel();
//            clusterViewRow.setLayout(new BoxLayout(clusterViewRow, BoxLayout.X_AXIS));
//            JLabel clusterViewLabel = new JLabel("View cluster: ");
//            setSize(clusterViewLabel, 110, 25);
//            clusterViewRow.add(clusterViewLabel);
//            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));
//
//            clusterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
//            setSize(clusterSpinner, 40, 25);
//            clusterViewRow.add(clusterSpinner);
//            clusterViewRow.add(Box.createRigidArea(new Dimension(5, 5)));
//
//            viewClusterButton = new JButton("View");
//            viewClusterButton.setEnabled(false);
//            setSize(viewClusterButton, 205, 25);
//            clusterViewRow.add(viewClusterButton);
//            clusterViewRow.setAlignmentX(LEFT_ALIGNMENT);

            preComputationLayout();

//            onTimeButton.addActionListener((ActionEvent ae) -> {
//                int k = (int) kSpinner.getValue();
//                cluster(new DyClustering.Stc.KMeansTime(
//                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
//                        experiment.delta / 3.0, k,
//                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
//            });
//
//            onCubeButton.addActionListener((ActionEvent ae) -> {
//                int k = (int) kSpinner.getValue();
//                cluster(new DyClustering.Stc.KMeans3D(
//                        synchro.originalGraph(), experiment.dataset.suggestedTimeFactor,
//                        experiment.delta / 3.0, k,
//                        ColorCollection.cbQualitativePastel, experiment.dataset.suggestedInterval));
//            });
//
//            viewClusterButton.addActionListener((ActionEvent ae) -> {
//                int clusterNumber = (int) clusterSpinner.getValue() - 1;
//                QuickView.showNewWindow(flattenedClusters.get(clusterNumber));
//            });
        }


        public List<SpaceTimeCubeSynchroniser> getSynchroList() {
            List<SpaceTimeCubeSynchroniser> synchrosList = new ArrayList<>();

            DyGraph graph1 = covidExperiment.getContinuousCopyWithLocations(selectedLocations.get(0));
            DyGraph graph2 = covidExperiment.getContinuousCopyWithLocations(selectedLocations.get(1));
            DyModularFdl algorithm1 = experiment.getContinuousLayoutAlgorithm(graph1, null);
            DyModularFdl algorithm2 = experiment.getContinuousLayoutAlgorithm(graph2, null);
            SpaceTimeCubeSynchroniser synchro1 = algorithm1.getSyncro();
            SpaceTimeCubeSynchroniser synchro2 = algorithm2.getSyncro();
            ModularStatistics stats1 = algorithm1.iterate(100);
            ModularStatistics stats2 = algorithm2.iterate(100);
            String time1 = stats1.getTotalRunnningTime().getSeconds() + "."
                    + String.format("%02d", stats1.getTotalRunnningTime().getNano() / 10000000);
//            String time2 = stats2.getTotalRunnningTime().getSeconds() + "."
//                    + String.format("%02d", stats1.getTotalRunnningTime().getNano() / 10000000);
//            int time1Int = Integer.parseInt(time1);
//            int time2Int = Integer.parseInt(time2);
//
//            int time = Math.max(time1Int, time2Int);
//            String timeString = Integer.toString(time);
            updateComputationReport("Layout computed in "
                    + time1 + " s");

            synchrosList.add(synchro1);
            synchrosList.add(synchro2);
            return synchrosList;
        }

        private void preComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox1);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox2);
//            add(Box.createRigidArea(new Dimension(5, 5)));
//            this.add(clusterRow);
//            add(Box.createRigidArea(new Dimension(5, 5)));
//            this.add(clusterViewRow);
        }

        private void postComputationLayout() {
            removeAll();
            add(titleLabel);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(computationReport);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewCubeButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(viewAnimationButton);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox1);
            add(Box.createRigidArea(new Dimension(5, 5)));
            this.add(locationOptionsComboBox2);
//            add(Box.createRigidArea(new Dimension(5, 5)));
//            this.add(clusterRow);
//            add(Box.createRigidArea(new Dimension(5, 5)));
//            this.add(clusterViewRow);
        }

        protected void setSize(Component component, int xSize, int ySize) {
            component.setMinimumSize(new Dimension(xSize, ySize));
            component.setMaximumSize(new Dimension(xSize, ySize));
            component.setPreferredSize(new Dimension(xSize, ySize));
        }

        private void compute() {
            postComputationLayout();
            revalidate();
            EventQueue.invokeLater(() -> {
                synchro1 = getSynchroList().get(0);
                synchro2 = getSynchroList().get(1);
                viewCubeButton.setBackground(activeButton);
                viewAnimationButton.setBackground(activeButton);
//                onTimeButton.setBackground(activeButton);
//                onCubeButton.setBackground(activeButton);
                viewCubeButton.setEnabled(true);
                viewAnimationButton.setEnabled(true);
                locationOptionsComboBox1.setEnabled(false);
                locationOptionsComboBox2.setEnabled(false);
//                onTimeButton.setEnabled(true);
//                onCubeButton.setEnabled(true);
            });
        }

        private void cluster(DyClustering clustering) {
            int k = (int) kSpinner.getValue();
            clustering.colorGraph();
            flattenedClusters = clustering.flattenClusters();
            clusterSpinner.setModel(new SpinnerNumberModel(1, 1, k, 1));
            viewClusterButton.setBackground(activeButton);
            clusterSpinner.setEnabled(true);
            viewClusterButton.setEnabled(true);
//            for (int i = 0; i < flattenedClusters.size(); i++) {
//                Graph graph = flattenedClusters.get(i);
//                SvgExporter.saveSvg(graph, new File(experiment.name + "_" + type + "_" + i + ".svg"));
//            }
        }

//        protected void viewCubeLocation1() {
//            QuickView.showNewWindow(synchro1.mirrorGraph());
//        }
//
//        protected void viewCubeLocation2() {
//            QuickView.showNewWindow(synchro2.mirrorGraph());
//        }

        private void viewCovidAnimationWithMultipleLocationFilters() {
            DyQuickViewMulti view = new DyQuickViewMulti(synchro1.originalGraph(),
                    synchro2.originalGraph(),
                    experiment.dataset.suggestedInterval.leftBound());
            view.setAnimation(new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)),
                    new Animation(experiment.dataset.suggestedInterval, Duration.ofSeconds(30)));

            view.showNewWindow();
        }

        protected void updateComputationReport(String text) {
            computationReport.setForeground(Color.GRAY);
            computationReport.setText(text);
        }

        @Override
        protected SpaceTimeCubeSynchroniser getSynchro() {
            return null;
        }
    }

}
