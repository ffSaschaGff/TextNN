import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

class Unigramm {

    public Set<String> getNGram(ArrayList<String> texts) {
        // get all significant words
        if (texts == null) {
            throw new IllegalArgumentException();
        }

        String[][] words = new String[texts.size()][];
        int counter = 0;
        for (String text: texts) {
            words[counter] = text.toLowerCase().replaceAll("[\\pP\\d]", " ").split("[ \n\t\r$+<>№=]");
            counter++;
        }

        Set<String> uniqueValues = new LinkedHashSet<>();

        // remove endings of words
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words[i].length; j++) {
                uniqueValues.add(PorterStemmer.stem(words[i][j]));
            }
        }

        uniqueValues.removeIf(s -> s.equals(""));

        return uniqueValues;
    }

    public Set<String> getNGram(String text) {
        // get all significant words
        String[] words = clean(text).split("[ \n\t\r$+<>№=]");

        // remove endings of words
        for (int i = 0; i < words.length; i++) {
            words[i] = PorterStemmer.stem(words[i]);
        }

        Set<String> uniqueValues = new LinkedHashSet<>(Arrays.asList(words));
        uniqueValues.removeIf(s -> s.equals(""));

        return uniqueValues;
    }

    private String clean(String text) {
        // remove all digits and punctuation marks
        if (text != null) {
            return text.toLowerCase().replaceAll("[\\pP\\d]", " ");
        } else {
            throw new IllegalArgumentException();
        }
    }

}