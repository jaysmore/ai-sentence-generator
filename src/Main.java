import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main {

	private static final int NUM_SENTENCES = 10;
	private static final int SENTENCE_LENGTH = 100;
	private static char[] alphabet = "abcdefghijklmnopqrstuvwxyz ".toCharArray();
	private static int totalCharsInScript = 0;
	static String trainPath = "lyrics.csv";
	private static Map<String, Integer> count_unigrams = new HashMap<String, Integer>();
	private static Map<String, Integer> count_bigrams = new HashMap<String, Integer>();
	private static Map<String, Integer> count_trigrams = new HashMap<String, Integer>();

	private static Map<String, Double> transitionProbability1 = new HashMap<String, Double>();
	private static Map<String, Double> transitionProbability2 = new HashMap<String, Double>();
	private static Map<String, Double> transitionProbability3 = new HashMap<String, Double>();

	private static String pathBigramTxt = "src/bigram.txt";
	private static String pathOutputTxt = "src/output.txt";

	public static List<List<String>> readCSV(String path) {
		List<List<String>> result = new ArrayList<>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		try {
			br = new BufferedReader(new FileReader(path));
			while ((line = br.readLine()) != null) {
				if (line.contains("daft")) {
					String[] parseByLine = line.split(cvsSplitBy);

//				doubleByLine[0] = Double.parseDouble(parseByLine[0]);
//				for (int i = 1; i < parseByLine.length; i++) {
//					doubleByLine[i] = Double.parseDouble(parseByLine[i]);

					result.add(Arrays.asList(parseByLine));
				}
			}
		} catch (

		Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String[] args) throws IOException {

		List<List<String>> train = new ArrayList<List<String>>();
		train = readCSV(trainPath);

		BufferedWriter writer = new BufferedWriter(new FileWriter("train.txt"));
		PrintWriter out = new PrintWriter("train.txt");
		for (int i = 0; i < train.size(); i++) {
			for (int j = 0; j < train.get(i).size(); j++) {
				System.out.println(train.get(i).get(j));
				String k = train.get(i).get(j);
				writer.write(k + " ");
				out.print(train.get(i).get(j));
			}
			writer.newLine();
			out.println();
		}

		String script = new String(Files.readAllBytes(Paths.get("train.txt")));

		script = script.replaceAll("\n", " ");
		script = script.toLowerCase().replaceAll("[^a-z]", " ");
		script = script.replaceAll(" +", " ");

		totalCharsInScript = script.length();
		System.out.println("Finished cleaning the script.");

		countNGrams(script);
		System.out.println("Counted all unigrams, bigrams and trigrams.");

		estimateTransitionProbabilities();
		System.out.println("Estimated transition probabilities using MLE.");

		generateSentences();
		System.out.println("Finished generating sentences.");

		writeTransitionMatrix();
		System.out.println("Wrote bigram transition matrix to the file. \nDone! ");
	}

	public static void countNGrams(String script) {

		int count = 0;
		for (int i = 0; i < alphabet.length; i++) {
			count = script.length() - script.replace(String.valueOf(alphabet[i]), "").length();
			count_unigrams.put(String.valueOf(alphabet[i]), count);
		}

		for (int i = 0; i < alphabet.length; i++) {
			for (int j = 0; j < alphabet.length; j++) {
				count = (script.length()
						- script.replace(String.valueOf(alphabet[i]) + String.valueOf(alphabet[j]), "").length()) / 2;
				count_bigrams.put(String.valueOf(alphabet[i]) + String.valueOf(alphabet[j]), count);
			}
		}

		for (int i = 0; i < alphabet.length; i++) {
			for (int j = 0; j < alphabet.length; j++) {
				for (int k = 0; k < alphabet.length; k++) {
					count = (script.length() - script.replace(
							String.valueOf(alphabet[i]) + String.valueOf(alphabet[j]) + String.valueOf(alphabet[k]), "")
							.length()) / 3;
					count_trigrams.put(
							String.valueOf(alphabet[i]) + String.valueOf(alphabet[j]) + String.valueOf(alphabet[k]),
							count);
				}
			}
		}
	}

	public static void estimateTransitionProbabilities() {

		helperEstimateProbabilities(1, count_unigrams, transitionProbability1);
		helperEstimateProbabilities(2, count_bigrams, transitionProbability2);
		helperEstimateProbabilities(3, count_trigrams, transitionProbability3);

	}

	public static void helperEstimateProbabilities(int n, Map<String, Integer> ngram_count,
			Map<String, Double> trans_prob) {

		double probability;
		for (String key : ngram_count.keySet()) {
			if (n == 1) {
				probability = (ngram_count.get(key) + 1) / (double) (totalCharsInScript + 27);
			} else if (n == 2) {
				probability = (ngram_count.get(key) + 1)
						/ (double) (count_unigrams.get(String.valueOf(key.charAt(0))) + 27);
			} else {
				probability = (ngram_count.get(key) + 1) / (double) (count_bigrams.get(key.substring(0, 2)) + 27);
			}
			trans_prob.put(key, probability);
		}
	}

	public static void generateSentences() throws IOException {

		FileWriter file = new FileWriter(pathOutputTxt);
		PrintWriter wr = new PrintWriter(file);

		for (int i = 0; i < alphabet.length - 1; i++) {

			for (int j = 0; j < NUM_SENTENCES; j++) {

				StringBuilder sb = new StringBuilder();
				sb.append(String.valueOf(alphabet[i])); // append the first letter

				while (sb.length() < SENTENCE_LENGTH) {

					Random r = new Random();
					double u = r.nextDouble();

					double[] cdf;

					if (sb.length() == 1) {
						cdf = computeCDF(String.valueOf(sb.toString().charAt(0)), transitionProbability2);
					} else {
						cdf = computeCDF(sb.toString().substring(sb.length() - 2, sb.length()), transitionProbability3);
					}

					char letter = findNextLetter(u, cdf); // found next letter/space

					sb.append(letter); // append next letter to the sentence
				}
				wr.println(sb.toString());
			}
		}
		wr.close();
	}

	public static double[] computeCDF(String c, Map<String, Double> trans_prob) {

		double[] cdf = new double[27];
		double sum = 0;
		for (int i = 0; i < 27; i++) {
			sum = sum + trans_prob.get(c + String.valueOf(alphabet[i]));
			cdf[i] = sum;
		}
		return cdf;
	}

	public static char findNextLetter(double u, double[] cdf) {

		for (int i = 0; i < alphabet.length; i++) {
			if (u <= cdf[i]) {
				return alphabet[i];
			}
		}
		return ' '; // if greater than sum of all P then return space
	}

	public static void writeTransitionMatrix() throws IOException {

		FileWriter file = new FileWriter(pathBigramTxt);
		PrintWriter wr = new PrintWriter(file);

		for (int i = 0; i < alphabet.length; i++) {
			for (int j = 0; j < alphabet.length; j++) {
				wr.printf("%.4f",
						transitionProbability2.get(String.valueOf(alphabet[j]) + String.valueOf(alphabet[i])));
				if (j < alphabet.length - 1) {
					wr.print(", ");
				}
			}
			wr.println();
		}
		wr.close();
	}
}
