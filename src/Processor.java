import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Processor {

    // 'Weights' for the various criteria that determine the similarity between two kanji. These are, for the most part,
    // 'magic numbers' that seem to work well.
    static final double[] WEIGHTS = {
        0.35, // Number of components
        0.6,  // Number of enclosed regions
        0.05, // Kanji proportions
        0.1,  // Kanji "density" (portion of kanji region that's black)
        1,    // Kanji "x-center of mass"
        1,    // Kanji "y-center of mass"
        1,    // "Relative mass" of first 1/3rd vertical region
        1,    // "Relative mass" of second 1/3rd vertical region
        1,    // "Relative mass" of third 1/3rd vertical region
        1,    // "Relative mass" of first 1/3rd horizontal region
        1,    // "Relative mass" of second 1/3rd horizontal region
        1,    // "Relative mass" of third 1/3rd horizontal region
        0.3,  // Number of components in first 1/3rd vertical region
        0.3,  // Number of components in second 1/3rd vertical region
        0.3,  // Number of components in third 1/3rd vertical region
        0.3,  // Number of components in first 1/3rd horizontal region
        0.3,  // Number of components in second 1/3rd horizontal region
        0.3   // Number of components in third 1/3rd horizontal region
    };

    boolean[][] found;
    int[][] imgArr; //2D array representation of the
    int minX,maxX,minY,maxY;
    int numPixels;
    ArrayList<Integer> bestMatches;

    public Processor(BufferedImage img) {
        found = new boolean[img.getHeight()][img.getWidth()];
        imgArr = new int[img.getHeight()][img.getWidth()];
        bestMatches = new ArrayList<Integer>();

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                found[y][x] = false;
                if ((img.getRGB(x, y) & 0xFF) < 250) {
                    imgArr[y][x] = 1;
                }
            }
        }
        setImageDimensions();
    }

    public ArrayList<Double> getAttributes() {
        ArrayList<Double> attributes = new ArrayList<Double>();

        attributes.add((double)getComponents(0, 0, imgArr.length - 1, imgArr[0].length - 1));
        attributes.add((double)getEnclosedRegions());
        attributes.add(getProportions());
        attributes.add(getDensity());
        attributes.add(getXCenter());
        attributes.add(getYCenter());
        attributes.add(getRegionWeight(minY, minX, maxY, minX + (maxX - minX) / 3));
        attributes.add(getRegionWeight(minY, minX + (maxX - minX)/3, maxY, maxX - (maxX - minX)/3));
        attributes.add(getRegionWeight(minY, maxX - (maxX - minX)/3,maxY, maxX));
        attributes.add(getRegionWeight(minY, minX, maxX, minY + (maxY - minY)/3));
        attributes.add(getRegionWeight(minY + (maxY - minY) / 3, minX, maxY - (maxY - minY) / 3, maxX));
        attributes.add(getRegionWeight(maxY - (maxY - minY)/3, minX, maxY, maxX));
        attributes.add((double)getComponents(minY, minX, maxY,minX + (maxX - minX) / 3));
        attributes.add((double)getComponents(minY, minX + (maxX - minX)/3, maxY, maxX - (maxX - minX)/3));
        attributes.add((double)getComponents(minY, maxX - (maxX - minX)/3,maxY, maxX));
        attributes.add((double)getComponents(minY, minX, maxX, minY + (maxY - minY)/3));
        attributes.add((double)getComponents(minY + (maxY - minY) / 3, minX, maxY - (maxY - minY) / 3, maxX));
        attributes.add((double)getComponents(maxY - (maxY - minY)/3, minX, maxY, maxX));
        return attributes;
    }

    // Populate the "bestMatches" ArrayList with the IDs of all the other kanji ordered by their similarity to
    // the present processor's
    public ArrayList<Integer> discoverBestMatches() {
        ArrayList<Double> attributes = getAttributes();
        TreeMap<Double, Integer> distances = new TreeMap<Double, Integer>();

        try {
            String stdAttributesStr[];
            double stdAttributes[];
            BufferedReader br = new BufferedReader(new FileReader(
                    "/home/joey/IdeaProjects/Kanji Detection/resources/data.txt"));
            String line = "";
            double distance;

            for (int count = 1; (line = br.readLine()) != null; count++) {
                distance = 0;
                stdAttributesStr = line.split(" ");
                stdAttributes = new double[stdAttributesStr.length];

                for (int i = 0; i < stdAttributesStr.length; i++) {
                    stdAttributes[i] = Double.parseDouble(stdAttributesStr[i]);
                    distance += Math.pow(WEIGHTS[i]*(stdAttributes[i] - attributes.get(i)),2);
                }
                distance = Math.pow(distance, 0.5);
                distances.put(distance, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (double key : distances.keySet()) {
            bestMatches.add(distances.get(key));
        }
        return bestMatches;
    }

    public ArrayList<Integer> getBestMatches() {
        return bestMatches;
    }

    // Use BFS to "tag" every point in the same component as (y,x)
    public void fillComponent(int y, int x, int ymin, int xmin, int ymax, int xmax) {
        Queue<Point> bfsQueue = new LinkedList<Point>();
        int[][] neighborOffsets = {{1,0},{-1,0},{0,1},{0,-1}};
        int neighborX, neighborY;

        found[y][x] = true;
        bfsQueue.add(new Point(y,x));

        while (!bfsQueue.isEmpty()) {
            Point current = bfsQueue.remove();
            for (int[] offset : neighborOffsets) {
                neighborY = current.getY() + offset[0];
                neighborX = current.getX() + offset[1];

                if (neighborY <= ymax && neighborY >= ymin && neighborX <= xmax && neighborX >= xmin
                        && !found[neighborY][neighborX] && imgArr[neighborY][neighborX] == 1) {
                    found[neighborY][neighborX] = true;
                    bfsQueue.add(new Point(neighborY, neighborX));
                }
            }
        }
    }

    public int getComponents(int ymin, int xmin, int ymax, int xmax) {
        int compCount = 0;

        for (int y = ymin; y < ymax; y++) {
            for (int x = xmin; x < xmax; x++) {
                if (imgArr[y][x] == 1 && !found[y][x]) {
                    fillComponent(y, x, ymin, xmin, ymax, xmax);
                    compCount++;
                }
            }
        }

        //Reset the array for use next time
        for (int y = 0; y < found.length; y++) {
            for (int x = 0; x < found[0].length; x++) {
                found[y][x] = false;
            }
        }
        return compCount;
    }

    public void printImgArr(int miny, int minx, int maxy, int maxx) {
        for (int y = miny; y <= maxy;y++) {
            for (int x = minx; x <= maxx; x++) {
                System.out.print(imgArr[y][x]);
            }
            System.out.println();
        }
    }

    public void invertImage() {
        for (int y = 0; y < imgArr.length; y++) {
            for (int x = 0; x < imgArr[0].length; x++) {
                imgArr[y][x] = 1 - imgArr[y][x];
            }
        }
    }

    // Finds the number of enclosed regions within the image by "inverting" the image and finding the number of
    // components of the inverted image. There is a corner case for which this doesn't work: when a region is partially
    // enclosed by the boundary of the entire image (we only want to count regions that are enclosed entirely by
    // the kanji's pixels). That's a pretty unnatural situation in application, however.
    public int getEnclosedRegions() {
        int numEnclosedRegions;

        invertImage();
        numEnclosedRegions = getComponents(0,0,imgArr.length - 1,imgArr[0].length - 1) - 1;
        invertImage();
        return numEnclosedRegions;
    }

    public void setImageDimensions() {
        minY = minX = Integer.MAX_VALUE;
        maxY = maxX = 0;

        for (int y = 0; y < imgArr.length; y++) {
            for (int x = 0; x < imgArr[0].length; x++) {
                if (imgArr[y][x] == 1) {
                    numPixels++;
                    minY = y < minY? y : minY;
                    maxY = y > maxY? y : maxY;
                    minX = x < minX? x : minX;
                    maxX = x > maxX? x : maxX;
                }
            }
        }
    }

    //Return the number of kanji pixels in the specified region divided by the total number of kanji pixels
    public double getRegionWeight(int ystart, int xstart, int yend, int xend) {
        int numRegionPixels = 0;

        for (int y = ystart; y <= xend; y++) {
            for (int x = xstart; x <= yend; x++) {
                if (imgArr[y][x] == 1)
                    numRegionPixels++;
            }
        }
        return ((double)numRegionPixels)/numPixels;
    }

    // Return the number of black pixels divided by the total number of pixels in the image
    public double getDensity() {
        int pixCount = 0;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (imgArr[y][x] == 1)
                    pixCount++;
            }
        }
        return ((double)(pixCount))/((maxX - minX + 1) * (maxY - minY + 1));
    }

    // Find the vertical 'center of mass' of the kanji
    public double getYCenter() {
        int yTotal = 0;
        int pixCount = 0;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (imgArr[y][x] == 1) {
                    yTotal += (y - minY);
                    pixCount++;
                }
            }
        }
        return ((double)yTotal)/(pixCount * (maxY - minY));
    }

    // Find the horizontal 'center of mass/ of the kanji
    public double getXCenter() {
        int xTotal = 0;
        int pixCount = 0;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (imgArr[y][x] == 1) {
                    xTotal += (x - minX);
                    pixCount++;
                }
            }
        }
        return ((double)xTotal)/(pixCount * (maxX - minX));
    }

    public double getProportions() {
        return ((double)(maxX - minX))/(maxY - minY);
    }

    // Makes the "fill component" method a little simpler
    private class Point {
        private int y, x;

        public Point(int y, int x) {
            this.y = y;
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
