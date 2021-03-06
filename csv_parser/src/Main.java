/**
 * Created by Wang.Daoping on 04.05.2017.
 */

import com.opencsv.*;
import com.sun.org.apache.bcel.internal.generic.ARRAYLENGTH;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    class ArticleWrapper {
        public ArticleWrapper(  )
    }
    static ArrayList<String> categoricalFeatureList = new ArrayList<>();
    static ArrayList<String> tabletSetIDList = new ArrayList<>();
    static HashMap<String, String> setIDList = new HashMap<>();

    static ArrayList<String> tablet_keywords = new ArrayList<>();

    public static void main(String[] args){
        ArrayList<Article> article_list = new ArrayList<>();
        ArrayList<String> feature_list = new ArrayList<>();
        ArrayList<Float> completeness_list = new ArrayList<>();
        ArrayList<String> manual_feature_list = new ArrayList<>();
        ArrayList<String[]> baseData = new ArrayList<>();

        tablet_keywords.add("Tablet");
        tablet_keywords.add("Tablet PC");
        tablet_keywords.add("TabletPC");
        tablet_keywords.add("2-in-1 Tablet");
        tablet_keywords.add("Hybrid-Tablet");
        tablet_keywords.add("Tab Samsung");

        manual_feature_list.add("Betriebssystem");
        manual_feature_list.add("SpeicherkapazitÃ¤t");
        manual_feature_list.add("Arbeitsspeicher");
        manual_feature_list.add("CPU-Taktfrequenz");
        manual_feature_list.add("Bilddiagonale");
        manual_feature_list.add("AuflÃ¶sung");
        //manual_feature_list.add("Festplatte");
        manual_feature_list.add("AkkukapazitÃ¤t");
        manual_feature_list.add("AuflÃ¶sung Hauptkamera");
        //manual_feature_list.add("SSD-SpeicherkapazitÃ¤t");
        manual_feature_list.add("Grafik-Controller-Serie");

        categoricalFeatureList.add("Betriebssystem");
        categoricalFeatureList.add("Bilddiagonale");
        categoricalFeatureList.add("AuflÃ¶sung");
        categoricalFeatureList.add("AkkukapazitÃ¤t");
        categoricalFeatureList.add("Grafik-Controller-Serie");
        categoricalFeatureList.add("brand");


        String feature_data_path = "C:/Users/wang.daoping/Documents/feature data/";
        String tablet_article_keywords_file = feature_data_path + "tablets_article_mercateo_keywords.dat";
        String article_feature_value_file = feature_data_path + "tablets_article_feature_value.dat";
        String price_data_file = feature_data_path + "tablets_article_price_data.dat";
        String base_data_file = feature_data_path + "tablets_article_base_data.dat";

        String dublicate_relation_file = feature_data_path + "tablets_article_duplicate_relation.dat";

        try {
            System.out.println("collectTableArticles");
            collectTabletArticles(tablet_article_keywords_file, article_list);
            System.out.println("collectFeatureValues");
            collectFeatureValues(article_list, feature_list, article_feature_value_file);
            System.out.println("collectPriceData");
            collectPriceData(price_data_file, article_list);
        } catch (IOException e){
            e.printStackTrace();
        }

        System.out.println(article_list.size());

        //ArrayList<Article> new_article_list = findCompleteArticlesForFeatures(article_list, manual_feature_list);
        ArrayList<Article> new_article_list = findArticlesForFeatures(article_list, manual_feature_list, 6);

        try {
            System.out.println("collectBaseData");
            collectBaseData(baseData, new_article_list, base_data_file);
        } catch (IOException e){
            e.printStackTrace();
        }

        //findCompleteFeatures(article_list, feature_list);
        System.out.println("calculateFeatureCompleteness");
        float avgCompleteness = 0;
        for(int i = 0; i < feature_list.size(); i++){
            float buf = calculateFeatureCompleteness(article_list, feature_list.get(i));
            completeness_list.add(buf);
            avgCompleteness += buf;
        }
        avgCompleteness = avgCompleteness /completeness_list.size();
        System.out.println("removeWeakFeatures");
        //removeWeakFeatures(feature_list, completeness_list, Math.min(avgCompleteness, 0));
        System.out.println("removeIncompleteArticles");
        //removeIncompleteArticles(article_list, feature_list);
        System.out.println("flushing");
        flushArticleFeatureList(new_article_list, manual_feature_list);

        try {
            writeArticleFeatureCSV(new_article_list, manual_feature_list, feature_data_path);
        } catch (IOException e){
            e.printStackTrace();
        }

        System.out.println();
        System.out.println(new_article_list.size() + " articles");
        for(int i = 0; i < baseData.size(); i++){
            //System.out.println(baseData.get(i)[6] + "\t" + baseData.get(i)[7]);
        }
        for(int i = 0; i < manual_feature_list.size(); i++){
            //System.out.println(manual_feature_list.get(i) + "\t" + Float.toString(completeness_list.get(i)) + "%");
        }
    }

    public static void writeArticleFeatureCSV(ArrayList<Article> article_list, ArrayList<String> feature_list, String outputPath) throws IOException{
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputPath + "tablets_training.csv"), "Cp1252"), ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER);
        String lineBuffer = "article_id" + "," + "brand";
        for(int i = 0; i < feature_list.size(); i++){
            lineBuffer += "," + feature_list.get(i);
        }
        lineBuffer += "," + "avg_price";
        String[] rec = lineBuffer.split(",");
        writer.writeNext(rec);

        for(int i = 1; i < article_list.size(); i++){
            //lineBuffer = article_list.get(i).getArticleID();
            lineBuffer = article_list.get(i).getArticleID() + "," + article_list.get(i).getBrand();
            for(int j = 0; j < feature_list.size(); j++){
                lineBuffer += "\t" + article_list.get(i).getFeatures().get(j).getValue();
            }
            lineBuffer += "\t" + article_list.get(i).getAvgPrice();
            String[] record = lineBuffer.split("\t");
            writer.writeNext(record);
        }
        writer.close();
    }

    public static void collectBaseData(ArrayList<String[]> baseData, ArrayList<Article> article_list, String baseFilePath) throws IOException{
        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(baseFilePath), "Cp1252"), '\t', '\"', 0);
        String[] lineBuffer;
        lineBuffer = reader.readNext();
        baseData.add(lineBuffer);
        while ((lineBuffer = reader.readNext()) != null){
            for(int i = 0; i < article_list.size(); i++){
                if(article_list.get(i).getArticleID().equals(lineBuffer[1])){
                    baseData.add(lineBuffer);
                    article_list.get(i).setBrand(lineBuffer[7]);
                    break;
                }
            }
        }
    }

    public static void flushArticleFeatureList(ArrayList<Article> article_list, ArrayList<String> feature_list){
        for(int i = 0; i < article_list.size(); i++){
            if(article_list.get(i).getPrice().size() == 0){
                article_list.remove(i);
                i--;
                continue;
            }
            for(int j = 0; j < feature_list.size(); j++){
                for(int k = 0; k < article_list.get(i).getFeatures().size(); k++){
                    if(article_list.get(i).getFeatures().get(k).getName().equals(feature_list.get(j))){
                        Feature buf = article_list.get(i).getFeatures().get(k);
                        article_list.get(i).getFeatures().set(k, article_list.get(i).getFeatures().get(j));
                        article_list.get(i).getFeatures().set(j, buf);
                        break;
                    }
                    if(k == article_list.get(i).getFeatures().size() - 1){
                        if(categoricalFeatureList.contains(feature_list.get(j))){
                            article_list.get(i).getFeatures().add(j, new Feature(feature_list.get(j), Float.NaN));
                        } else {
                            article_list.get(i).getFeatures().add(j, new Feature(feature_list.get(j), 666));
                        }
                        break;
                    }
                }
            }
        }
    }

    public static ArrayList<Article> findCompleteArticlesForFeatures(ArrayList<Article> article_list, ArrayList<String> features){
        ArrayList<Article> newArticleList = new ArrayList<>();

        for(int i = 0; i < article_list.size(); i++){
            Article currentArticle = article_list.get(i);
            boolean isReallyComplete = true;
            for(int j = 0; j < features.size(); j++){
                String currentSetFeature = features.get(j);
                boolean isComplete = false;
                for(int k = 0; k < currentArticle.getFeatures().size(); k++){
                    Feature currentIsFeature = currentArticle.getFeatures().get(k);
                    if(currentIsFeature.getName().equals(currentSetFeature)){
                        isComplete = true;
                        break;
                    }
                }
                if(!isComplete){
                    isReallyComplete = false;
                    break;
                }
            }
            if(isReallyComplete)
                newArticleList.add(currentArticle);
        }

        return newArticleList;
    }

    public static ArrayList<Article> findArticlesForFeatures(ArrayList<Article> article_list, ArrayList<String> features, int num){
        ArrayList<Article> newArticleList = new ArrayList<>();
        String previousID = "start";
        int knownFeatures = 0;
        ArrayList<Article> currentArticleSet = new ArrayList<>();

        for(int i = 0; i < article_list.size(); i++){
            Article currentArticle = article_list.get(i);
            String currentSetID = currentArticle.getSetID();

            if(!currentSetID.equals(previousID)){
                previousID = currentSetID;
                knownFeatures = 0;
                currentArticleSet.add(currentArticle);
            } else {
                for(int j = 0; j < features.size(); j++){
                    String currentSetFeature = features.get(j);
                    for(int k = 0; k < currentArticle.getFeatures().size(); k++){
                        Feature currentIsFeature = currentArticle.getFeatures().get(k);
                        if(currentIsFeature.getName().equals(currentSetFeature)){
                            knownFeatures++;
                        }
                    }
                }
            }



            if(knownFeatures >= num){
                newArticleList.add(currentArticle);
            }
        }

        return newArticleList;
    }

    public static void removeWeakFeatures(ArrayList<String> feature_List, ArrayList<Float> completeness_list, float avgCompleteness){
        for(int i = 0; i < feature_List.size(); i++){
            if(completeness_list.get(i) < avgCompleteness){
                feature_List.remove(i);
                completeness_list.remove(i);
                i--;
            }
        }
    }

    public static void collectPriceData(String price_data_file, ArrayList<Article> article_list) throws IOException{
        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(price_data_file), "Cp1252"), '\t', '\"', 1);
        String[] lineBuffer;

        while((lineBuffer = reader.readNext()) != null) {
            for(int i = 0; i < article_list.size(); i++){
                if(article_list.get(i).equals(lineBuffer[1])){
                    article_list.get(i).addPrice(Float.parseFloat(lineBuffer[2]));
                }
            }
        }

        for(int i = 0; i < article_list.size(); i++){
            article_list.get(i).calculateAvgPrice();
        }
     }

    public static void collectSetID(String tabletsArticleDupRelFilepath) throws IOException{
        String[] lineBuffer;

        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(tabletsArticleDupRelFilepath), "Cp1252"), '\t', '\"', 1);
        while((lineBuffer = reader.readNext()) != null){
            setIDList.put(lineBuffer[1], lineBuffer[2]);
        }
    }

    public static void collectTabletArticles(String tablet_article_keywords_file, ArrayList<Article> article_list) throws IOException{
        String[] lineBuffer;

        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(tablet_article_keywords_file), "Cp1252"), '\t', '\"', 1);
        while((lineBuffer = reader.readNext()) != null) {
            if(isTablet(lineBuffer[2], setIDList.get(lineBuffer[1]))){
                pushList(lineBuffer[1], setIDList.get(lineBuffer[1]), article_list);
            }
        }
    }

    public static float calculateFeatureCompleteness(ArrayList<Article> article_list, String feature){
        float articleNumber = article_list.size();
        float foundNum = 0;

        for(int i = 0; i < articleNumber; i++){
            for(int j = 0; j < article_list.get(i).getFeatures().size(); j++){
                if(article_list.get(i).getFeatures().get(j).getName().equals(feature)){
                    foundNum++;
                    break;
                }
            }
        }
        return (foundNum/articleNumber)*100;
    }

    public static void removeIncompleteArticles(ArrayList<Article> article_list, ArrayList<String> feature_list){
        for(int i = 0; i < article_list.size(); i++){
            boolean full = true;
            for(int j = 0; j < feature_list.size(); j++){
                for(int k = 0; k < article_list.get(i).getFeatures().size(); k++){
                    if(feature_list.get(j).equals(article_list.get(i).getFeatures().get(k).getName())){
                        break;
                    }
                    if(k == article_list.get(i).getFeatures().size() - 1){
                        full = false;
                        break;
                    }
                }
                if(full) continue;
                article_list.remove(i);
                i--;
                break;
            }
        }
    }

    public static void findCompleteFeatures(ArrayList<Article> article_list, ArrayList<String> feature_list){
        for(int i = 0; i < feature_list.size(); i++){
            boolean exists = false;
            for(int j = 0; j < article_list.size(); j++){
                for(int k = 0; k < article_list.get(j).getFeatures().size(); k++){
                    if(article_list.get(j).getFeatures().get(k).getName().equals(feature_list.get(i))){
                        exists = true;
                        break;
                    }
                }
                if(!exists){
                    feature_list.remove(i);
                    i--;
                    break;
                }
                exists = false;
            }
        }
    }

    public static void collectFeatureValues(ArrayList<Article> article_list, ArrayList<String> feature_list, String article_feature_value_file) throws IOException{
        String[] lineBuffer;

        CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(article_feature_value_file), "Cp1252"), '\t', '\"', 1);
        while ((lineBuffer = reader.readNext()) != null){
            for(int i = 0; i < article_list.size(); i++){
                if(article_list.get(i).getArticleID().equals(lineBuffer[1])){
                    switch (lineBuffer.length){
                        case 3:
                            article_list.get(i).addFeature(lineBuffer[2]);
                            break;
                        case 4:
                            article_list.get(i).addFeature(lineBuffer[2], lineBuffer[3]);
                            break;
                        default:
                            System.out.println("collectFeatureValues: unexpected lineBuffer length: " + Integer.toString(lineBuffer.length));
                            System.exit(666);
                    }
                    if(!feature_list.contains(lineBuffer[2])){
                        feature_list.add(lineBuffer[2]);
                    }
                    break;
                }
            }
        }

        // Remove articles with no feature information
        for(int i = 0; i < article_list.size(); i++){
            if(article_list.get(i).getNumFeatures() == 0){
                article_list.remove(i);
                i--;
            }
        }
    }

    public static boolean pushList(String article_id, String setID, ArrayList<Article> list){
        ArrayList<String> importedArticles = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            importedArticles.add(list.get(i).getArticleID());
        }
        if(importedArticles.contains(article_id)) return true;
        else {
            Article article = new Article(article_id);
            article.setSetID(setID);
            if(!tabletSetIDList.contains(setID)){
                tabletSetIDList.add(setID);
            }
            list.add(article);
        }
        return true;
    }

    public static boolean isTablet(String keyword, String setID){
        return tablet_keywords.contains(keyword) || tabletSetIDList.contains(setID);
    }
}
