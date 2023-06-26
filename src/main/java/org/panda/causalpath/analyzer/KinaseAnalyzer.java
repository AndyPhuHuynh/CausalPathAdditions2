package org.panda.causalpath.analyzer;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.RankingAlgorithm;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.panda.causalpath.resource.ProteomicsLoader;
import org.panda.resource.KinaseLibrary;

import org.jfree.chart.ChartUtils;

import org.panda.utility.statistics.*;


import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;
import java.util.List;

public class KinaseAnalyzer {

    /**
     * Function takes a list of sequence, and for each sequence it will
     * take every kinases score for that sequence
     * @return
     */

    ProteomicsLoader pL;

    /**
     * Key: Kinase Value: A list of scores for that kinase
     * for every peptide
     */
    HashMap<String, List<Double>> kinasePeptideScoresList;

    /**
     * Stores the changeValues
     */
    public List<Double> changeValues;

    KinaseLibrary kN;

    HashMap<String, Double> kinasePVal;

    HashMap<Integer, String> indexKinase;

    SpearmansCorrelation sP;
    RankingAlgorithm rankingObject = new NaturalRanking();

    PearsonsCorrelation pC;

    double[][] changeScoreMap;

    double[][] matrix;

    HashMap<String, Double> pValues;

    HashMap<String, Integer> kinaseToColumnIndex;

   List<String> kinaseBH;

   HashMap<String, Double> signedPValues;

    public KinaseAnalyzer(ProteomicsLoader pL){

        this.pL = pL;

        this.kN = new KinaseLibrary();

        sP = new SpearmansCorrelation();

        rankingObject = new NaturalRanking();

        pValues = new HashMap<>();

        signedPValues = new HashMap<>();

        kinasePeptideScoresList = new HashMap<>();

        changeValues = new ArrayList<>();

        kinasePVal = new HashMap<>();

        // InitializeSequenceKScores will initialize both kinasePeptideScoreList
        // and changeValues
        initializeSequenceKScores();


        kinaseColumnIndex();

        initializeMatrix();

        generatePValues();

        // Alternative method:
        initializeKinasePVal();


        // This chunk of code is just setting up pValues nad kinasePValues to compare two methods
        int count = 0;
        int totalCount = 0;
        double[] pValues1 = new double[pValues.keySet().size()];
        double[] pValues2 = new double[pValues.keySet().size()];
        LibraryPValueGenerator lN = new LibraryPValueGenerator(pL);
        HashMap<String, Double> map = lN.pValueMap;
        for(String kinase: pValues.keySet()){
            System.out.println(pValues.get(kinase) + " " + kinasePVal.get(kinase));
            pValues1[totalCount] = pValues.get(kinase);
            pValues2[totalCount] = map.get(kinase);
            if(!pValues.get(kinase).equals(map.get(kinase))){
               System.out.println("ERROR");
               System.exit(1);
            }
            totalCount++;
            if(pValues.get(kinase) < 0.005){
                count++;
            }
        }
        System.out.println("Final count is: " + count + " total count is " + totalCount);

        System.out.println("Benjamini hochberg results: ");

        kinaseBH = FDR.select(pValues, null, 0.05);
        System.out.println(FDR.select(pValues, null, 0.05).size());




        // Stuff to display the scatter plot

        SwingUtilities.invokeLater(() -> {
            chartingPValues example = new chartingPValues("Scatter Chart Example", pValues1, pValues2);
            example.setSize(800, 400);
            example.setLocationRelativeTo(null);
            example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            example.setVisible(true);
        });




        // resultsToFile("C:\\Users\\25jul\\OneDrive\\Desktop\\resultsFile");












    }

    public HashMap<String, Double> returnMap(){
        return pValues;
    }


    public void initializeKinasePVal(){
        List<Double> nullDistribution = generateNullDistribution(changeValues, changeValues);
        // Iterate through every kinase
        for(String kinase: kinasePeptideScoresList.keySet()){
            // Get its corresponding score list from the map(analagous to a column)
            List<Double> scoreList = kinasePeptideScoresList.get(kinase);
            // Get the correlation coefecient between that and the changeValues
            double coeff = listCorrelationCoeffecient(changeValues, scoreList);
            // Call getPValue() to calculate the p value
            double pVal = getPValue(nullDistribution, coeff);
            // Store it into the new map
            kinasePVal.put(kinase, pVal);
        }
    }

    public void resultsToFile(String fileName){

        // qVals is a map for each kinase, and it will calculate the FDR
        // assuming that the kinase's pvalue is used as the threshold
        Map<String, Double> qVals= FDR.getQVals(pValues, null);
        try{
            Writer w = new FileWriter(fileName);
            w.write("Kinase\tpValues\n");
            for(String kinase: kinaseBH){
                w.write(kinase + "\t" + signedPValues.get(kinase) +"\t" + qVals.get(kinase));
                w.write("\n");
            }

            w.close();
        }
        catch(Exception e){

        }

    }


    /**
     * Method will generate pValues by taking the associated pValue with
     * correlation coeffecient between peptide-change column(first column
     * in matrix instance variable) and every other column(all columns corresponding
     * to kinase's and their scores for each peptide)
     * These pValues will be stored into p-value map
     */
    public void generatePValues(){
        PearsonsCorrelation pC = new PearsonsCorrelation(matrix);

        RealMatrix r = pC.getCorrelationPValues();

        RealMatrix r1 = pC.getCorrelationMatrix();

        for(int i = 1; i < r.getColumnDimension(); i++){
            // Get the p-value corresponding to correlation coeff between col 0, i
            double pVal = r.getEntry(0, i);
            // Find the appropriate kinase for which this p-value is associated with
            String kinase = indexKinase.get(i);
            // Input into map
            pValues.put(kinase, pVal);


            // Get the correlation coeffecient for this kinase
            double correlation = r1.getEntry(0, i);
            // Get the sign of the correlation so that we can determine signed pValue
            double signedPVal = correlation >= 0 ? pVal : pVal * -1;
            // Put into signed p value map
            signedPValues.put(kinase, signedPVal);
        }
    }


    public Set<Map.Entry<String, String>> dependentKinases(){

        // A list storing pairs of kinases, where each pair represents two kinases for which they are likely to be dependent
        Set<Map.Entry<String, String>> dependentKinases = new HashSet<Map.Entry<String, String>>();
        // Null distribution
        List<Double> nullDistribution = generateNullDistribution(changeValues, changeValues);
        // Make a list, rather than set, so I can avoid repeat calculations due to iterating over reverse pairs
        List<String> kinasePepScoresArr = new ArrayList<>(kinasePeptideScoresList.keySet());
        for(int i = 0; i < kinasePepScoresArr.size() - 1; i++){
            for(int j = i + 1; j < kinasePeptideScoresList.size(); j++){
                double coeff = listCorrelationCoeffecient(kinasePeptideScoresList.get(kinasePepScoresArr.get(i)), kinasePeptideScoresList.get(kinasePepScoresArr.get(j)));
                double pVal = getPValue(nullDistribution, coeff);
                if(pVal <= 0.05){
                    Map.Entry<String, String> e1 = new AbstractMap.SimpleEntry<String, String>(kinasePepScoresArr.get(i), kinasePepScoresArr.get(j));
                    dependentKinases.add(e1);
                }

            }
        }

        return dependentKinases;
    }

    public HashMap<String, Double> getKinasePVal(){
        return kinasePVal;
    }


    /**
     * A method that takes a null distribution and observation as arguments
     * and tells you the proportion of test statistics in the null distribution
     * that are as extreme or more extreme than your observation
     * @param nullDistribution
     * @param observation
     * @return
     */
    public double getPValue(List<Double> nullDistribution, double observation){

        int count = 0;

        for (Double nullVal : nullDistribution) {
            if (Math.abs(nullVal) >= Math.abs(observation)) {
                count++;
            }
        }

        return count/ ((double) nullDistribution.size());

    }


    // Delete before pushing
    public HashMap<String, List<Double>> getMap(){
        return this.kinasePeptideScoresList;
    }

    // Finds the correlation coeffecient between two lists
    private double listCorrelationCoeffecient(List<Double> ls1, List<Double> ls2){
        double[] ls1Arr = listToArray(ls1);
        double[] ls2Arr = listToArray(ls2);

        return sP.correlation(ls1Arr, ls2Arr);
    }


    // Returns a sorted list of the corrleated coeffecients
    public List<Double> generateNullDistribution(List<Double> changeValues, List<Double> peptideScoresList){

        ArrayList<Double> correlationCoeffecients = new ArrayList<>();

        for(int i = 0; i < 5000; i++){

            // Produce and shuffle a deep copy; avoid shuffling actual list!
            List<Double> shuffledPeptideScores = copyList(peptideScoresList);
            Collections.shuffle(shuffledPeptideScores);

            // Calculate correlation coeffecient

            double coeff = listCorrelationCoeffecient(changeValues, shuffledPeptideScores);

            correlationCoeffecients.add(coeff);

        }

        Collections.sort(correlationCoeffecients);

        return correlationCoeffecients;
    }

    private List<Double> copyList(List<Double> originalList){

        return new ArrayList<>(originalList);
    }

    private double[] listToArray(List<Double> list){
        double[] arr = new double[list.size()];
        int cap = 0;
        for(double d: list){
            arr[cap] = d;
            cap++;
        }

        return arr;
    }

    // A method that produces a mapping from kinases to column indexes
    // on the matrix
    public void kinaseColumnIndex(){
        kinaseToColumnIndex = new HashMap<>();
        indexKinase = new HashMap<>();

        // 0th column reserved for change values
        int column = 1;
        for(String kinase: kN.kinaseSet()){
            kinaseToColumnIndex.put(kinase, column);
            indexKinase.put(column, kinase);
            column++;
        }

    }

    public void initializeMatrix(){
        // Map containing all change values as values
        HashMap<String, Double> seqChangeVal = pL.getSeqChangeValMap();
        // Matrix where each value in the first column is a change value for a given peptide
        // and every column that is not the first refers to a different kinase. Each value
        // in each of those columns refers to that respective kinases score for the peptide
        // that the row refers to
        matrix = new double[seqChangeVal.size()][kN.kinaseSet().size() + 1];
        // Used to keep track of what column refers to what kinase

        // Convert the change-Values to ranks
        int row = 0;
        for(double d: convertToRanks(changeValues)){
            matrix[row][0] = d;
            row++;
        }

        // Convert each kinase's list of scores for each sequence to ranks
        for(String kinase: kinasePeptideScoresList.keySet()){
            // Get the list of scores for this kinase
            List<Double> scores = kinasePeptideScoresList.get(kinase);
            // Get the column index that this kinase refers to
            int columnIndex = kinaseToColumnIndex.get(kinase);
            // Convert scores to ranks, and map the ranks to the column
            row = 0;
            for(double d: convertToRanks(scores)){
                matrix[row][columnIndex] = d;
                row++;
            }

        }




    }

    private double[] convertToRanks(List<Double> list){

        double[] arr = new double[list.size()];

        int index = 0;

        for(double d: list){
            arr[index] = d;
            index++;
        }

        return rankingObject.rank(arr);


    }



    private void initializeSequenceKScores(){
        HashMap<String, Double> seqChangeVal = pL.getSeqChangeValMap();
        for(String sequence: seqChangeVal.keySet()){

            // To maintain corresponding order of change-values
            changeValues.add(seqChangeVal.get(sequence));

            HashMap<String, Double> scoresForPeptide = kN.peptideScore(sequence);
            for(String kinase: scoresForPeptide.keySet()){

                // If this kinase isn't already in the map, put it in
                if(!kinasePeptideScoresList.containsKey(kinase)){
                    kinasePeptideScoresList.put(kinase, new ArrayList<>());
                }

                // Add the kinase's score for this sequence at the correct position
                kinasePeptideScoresList.get(kinase).add(scoresForPeptide.get(kinase));
            }

        }

    }

    public double[][] getChangeScoreMap(){
        return changeScoreMap;
    }




    private void initializeChangeScoreMap(){
        // A map that stores the change values for the different peptides
        HashMap<String, Double> seqChangeVal = pL.getSeqChangeValMap();

        // Map the kinases to their correct index
        kinaseToColumnIndex = new HashMap<>();

        // The first column is reverved for the peptide scores, so the first kinase column
        // will be column 1
        int kinColumn = 1;

        for(String kinase: kN.kinaseSet()){
            // Fill out the kinaseToColumnIndex map
            kinaseToColumnIndex.put(kinase, kinColumn);
            kinColumn++;
        }

        // The number of rows is the number of peptide change values, and the number of columns
        // is the number of kinases (one column per kinase) plus one column for the peptide change values
        changeScoreMap = new double[seqChangeVal.size()][kN.kinaseSet().size() + 1];


        // Each row refers to one peptide change value
        int scoreRow = 0;

        for(String sequence: seqChangeVal.keySet()){


            // First column will represent change values for each peptide
            changeScoreMap[scoreRow][0] = seqChangeVal.get(sequence);

            HashMap<String, Double> scoresForPeptide = kN.peptideScore(sequence);

            for(String kinase: scoresForPeptide.keySet()){

                int kinColumnInd = kinaseToColumnIndex.get(kinase);

                // Add the kinase's score for this sequence at the correct position
                changeScoreMap[scoreRow][kinColumnInd] = scoresForPeptide.get(kinase);

            }

            scoreRow++;

        }
    }

    private class chartingPValues extends JFrame{
        public chartingPValues(String title, double[] pVal1, double[] pVal2){
            super(title);

            XYDataset dataset = createDataSet(pVal1, pVal2);

            JFreeChart chart = ChartFactory.createScatterPlot("P-Values calculated in two different ways",
                    "X-axis", "Y-axis", dataset);

            //Changes background color
            org.jfree.chart.plot.XYPlot plot = (XYPlot)chart.getPlot();

            XYItemRenderer renderer = plot.getRenderer();

// Set the size and shape of the points for the first series (Boys)
            renderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
            renderer.setSeriesPaint(0, Color.BLUE);

// Set the size and shape of the points for the second series (Girls)
            renderer.setSeriesShape(1, new Ellipse2D.Double(-2, -2, 6, 6));
            renderer.setSeriesPaint(1, Color.RED);

            plot.setBackgroundPaint(new Color(255,228,196));


            // Create Panel
            ChartPanel panel = new ChartPanel(chart);
            setContentPane(panel);

        }

        public XYDataset createDataSet(double[] pVal1, double[] pVal2){
            XYSeriesCollection dataSet = new XYSeriesCollection();

            XYSeries series1 = new XYSeries("First Series");

            for(int i = 0; i < pVal1.length; i++){
                series1.add(pVal1[i], pVal2[i]);
            }

            dataSet.addSeries(series1);

            XYSeries series2 = new XYSeries("Second Series");

            for(double i = 0; i < 1; i = i + 0.001){
                series2.add(i, i);
            }

            dataSet.addSeries(series2);

            return dataSet;

        }
    }
}


