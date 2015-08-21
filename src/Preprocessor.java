import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

// Takes a set of kanji images (the i'th kanji in /resources/kanjilist.txt should be named 'i.gif'), "processes"
// each image for its attributes, and then populates 'data.txt' with the attributes of each kanji. This only
// needs to be run whenever the set of images is modified.
public class Preprocessor {

    private static final String INPUT_PATH = "/home/joey/IdeaProjects/Kanji Detection/resources/images/";
    private static final String OUTPUT_PATH = "/home/joey/IdeaProjects/Kanji Detection/resources/data.txt";

    public static void main(String[] args) {
        BufferedImage bi;
        ArrayList<Double> attributes;
        BufferedWriter bw;
        long start = System.currentTimeMillis();
        Processor p;

        try {

            bw = new BufferedWriter(new FileWriter(OUTPUT_PATH));

            for (int i = 1; i <= new File(INPUT_PATH).listFiles().length; i++) {
                bi = ImageIO.read(new File(INPUT_PATH + i + ".gif"));
                p = new Processor(bi);
                attributes = p.getAttributes();
                for (Double attribute : attributes)
                    bw.write(attribute + " ");
                bw.write("\n");
            }
            bw.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}