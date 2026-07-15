import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import java.io.FileInputStream;


public class Main {
    public static void main(String[] args) {

        File folder = new File("data"); // adjust path if needed
        File[] files = folder.listFiles();

        // Step 1: Processed Data (file → words)
        Map<String, List<String>> processedData = new HashMap<>();

        if (files != null) {
            for (File file : files) {

                if (file.isFile()) {

                    try {
                        String content = "";

                        if (file.getName().endsWith(".txt")) {
                            content = Files.readString(file.toPath());

                        } else if (file.getName().endsWith(".pdf")) {

                            PDDocument document = PDDocument.load(file);
                            PDFTextStripper stripper = new PDFTextStripper();
                            content = stripper.getText(document);
                            document.close();
                        } else if (file.getName().endsWith(".docx")) {

                            FileInputStream fis = new FileInputStream(file);
                            XWPFDocument doc = new XWPFDocument(fis);
                            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);

                            content = extractor.getText();

                            extractor.close();
                            doc.close();
                            fis.close();
                        }

                        // 🔹 TEXT PROCESSING
                        content = content.toLowerCase();
                        content = content.replaceAll("[^a-z ]", "");
                        String[] words = content.split("\\s+");

                        processedData.put(file.getName(), Arrays.asList(words));

                    } catch (IOException e) {
                        System.out.println("Error reading: " + file.getName());
                    }
                }
            }
        }

        // Step 2: Print processed data//
        System.out.println("\n--- Processed Data ---");
        for (String fileName : processedData.keySet()) {
            System.out.println(fileName + " → " + processedData.get(fileName));
        }

        // Step 3: Build Inverted Index (word → files)
        Map<String, List<String>> invertedIndex = new HashMap<>();

        for (String fileName : processedData.keySet()) {
            List<String> words = processedData.get(fileName);

            for (String word : words) {
                if (word.isEmpty()) continue;

                invertedIndex.putIfAbsent(word, new ArrayList<>());

                if (!invertedIndex.get(word).contains(fileName)) {
                    invertedIndex.get(word).add(fileName);
                }
            }
        }
        // Step 5: Search + TF-IDF Ranking
        Scanner scanner = new Scanner(System.in);

        System.out.print("\nEnter search query: ");
        String query = scanner.nextLine().toLowerCase();

        // Clean query
        query = query.replaceAll("[^a-z ]", "");
        String[] queryWords = query.split("\\s+");

        Map<String, Double> scores = new HashMap<>();
        int totalDocs = processedData.size();

        for (String word : queryWords) {

            if (!invertedIndex.containsKey(word)) continue;

            List<String> docs = invertedIndex.get(word);
            int df = docs.size();

            double idf = Math.log((double) totalDocs / df);

            for (String doc : docs) {

                List<String> wordsInDoc = processedData.get(doc);

                int termCount = 0;
                for (String w : wordsInDoc) {
                    if (w.equals(word)) termCount++;
                }

                double tf = (double) termCount / wordsInDoc.size();
                double tfidf = tf * idf;

                scores.put(doc, scores.getOrDefault(doc, 0.0) + tfidf);
            }
        }

        // Step 6: Sort Results
        List<Map.Entry<String, Double>> sortedResults = new ArrayList<>(scores.entrySet());

        sortedResults.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Step 7: Display Results
        System.out.println("\n--- Ranked Results ---");

        if (sortedResults.isEmpty()) {
            System.out.println("No matching files found.");
        } else {
            for (Map.Entry<String, Double> entry : sortedResults) {
                System.out.println(entry.getKey() + " → Score: " + entry.getValue());
            }
        }

        scanner.close();
    }
}