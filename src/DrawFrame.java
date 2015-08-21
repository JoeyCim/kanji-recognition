import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

public class DrawFrame extends JFrame {

    private static final int FRAME_WIDTH = 564, FRAME_HEIGHT = 475;

    private Canvas canvas;
    private JLabel promptLabel;
    private JLabel resultLabel;
    private JTextField resultField;
    private JButton submitButton;
    private JButton clearButton;
    private JPanel bottomPanel;
    private JPanel canvasPanel;
    private String kanjiStr;
    private JButton leftArrow;
    private JButton rightArrow;
    private ArrayList<JButton> kanjiButtons;
    private JPanel labelPanel;
    private JPanel resultPanel;

    private Processor p;
    private int kanjiStart;

    public DrawFrame() {
        initializeComponents();
    }

    public void initializeComponents() {

        canvas = new Canvas();
        promptLabel = new JLabel("Draw a Kanji in the white space above.");
        resultLabel = new JLabel();
        submitButton = new JButton("Find a match");
        submitButton.setEnabled(false);
        clearButton = new JButton("Clear");
        leftArrow = new JButton("<");
        leftArrow.setEnabled(false);
        rightArrow = new JButton(">");
        rightArrow.setEnabled(false);
        bottomPanel = new JPanel();
        canvasPanel = new JPanel();
        resultPanel = new JPanel();
        labelPanel = new JPanel();
        kanjiButtons = new ArrayList<JButton>();

        this.setLayout(new BorderLayout(5, 5));
        bottomPanel.setLayout(new BorderLayout(5, 5));
        canvasPanel.setLayout(new BorderLayout(5, 5));
        resultPanel.setLayout(new GridLayout(1, 12));
        labelPanel.setLayout(new GridLayout(2,1,0,10));

        for (int i = 0; i < 10; i++) {
            kanjiButtons.add(new JButton(" "));
            kanjiButtons.get(i).setEnabled(false);
        }

        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.clear();
                submitButton.setEnabled(false);
                leftArrow.setEnabled(false);
                rightArrow.setEnabled(false);
                resultLabel.setText("");
                for (JButton kButton : kanjiButtons) {
                    kButton.setText(" ");
                    kButton.setEnabled(false);
                }

            }
        });

        rightArrow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                kanjiStart += 10;
                displayKanji();
                leftArrow.setEnabled(true);
            }
        });

        leftArrow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                kanjiStart -= 10;
                displayKanji();
                rightArrow.setEnabled(true);
            }
        });

        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                p = new Processor(canvas.getImage());
                kanjiStart = 0;
                displayKanji();
                rightArrow.setEnabled(true);
            }
        });

        // When the user clicks a 'kanji button', pull up an internet dictionary that provides more information about
        // that kanji,
        for (final JButton kButton : kanjiButtons) {
            kButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        String kanji = URLEncoder.encode(kButton.getText(), "utf-8").replace("+","");
                        String url = "http://jisho.org/search/" + kanji;
                        Desktop.getDesktop().browse(java.net.URI.create(url));
                    } catch (IOException err) {
                        err.printStackTrace();
                    }
                }
            });
        }

        bottomPanel.add(resultPanel, BorderLayout.NORTH);
        bottomPanel.add(clearButton, BorderLayout.CENTER);
        bottomPanel.add(submitButton, BorderLayout.SOUTH);
        canvasPanel.add(canvas, BorderLayout.CENTER);
        resultPanel.add(leftArrow);
        for (JButton button : kanjiButtons)
            resultPanel.add(button);
        resultPanel.add(rightArrow);
        labelPanel.add(promptLabel);
        labelPanel.add(resultLabel);
        canvasPanel.add(labelPanel, BorderLayout.SOUTH);
        this.add(canvasPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);

        try {
            BufferedReader br = new BufferedReader
                (new FileReader("/home/joey/IdeaProjects/Kanji Detection/resources/kanjilist.txt"));
            kanjiStr = br.readLine();
        } catch(IOException e ) { e.printStackTrace(); }
    }

    public void displayKanji() {
        int match;
        int maxResult;

        // When "Find a match" is pressed, we need to generate the new kanji list. If one of the arrows is pressed,
        // we just need to retrieve the list that we've already generated (and this list will have size 0 if it
        // hasn't already been generated).
        ArrayList<Integer> bestMatches = p.getBestMatches().size() == 0 ? p.discoverBestMatches() : p.getBestMatches();

        // Fill the buttons with the ten next best kanji, or leave some empty if there aren't ten more available
        for (int i = kanjiStart; i < kanjiStart + 10; i++) {
            if (i < bestMatches.size()) {
                match = bestMatches.get(i);
                kanjiButtons.get(i - kanjiStart).setEnabled(true);
                kanjiButtons.get(i - kanjiStart).setText(kanjiStr.charAt(match - 1) + "");
            }
            else {
                kanjiButtons.get(i - kanjiStart).setEnabled(false);
                kanjiButtons.get(i - kanjiStart).setText(" ");
            }
        }

        // Keep the user from going beyond the boundaries of the kanji list.
        if (kanjiStart == 0)
            leftArrow.setEnabled(false);
        if (kanjiStart >= bestMatches.size() - 10)
            rightArrow.setEnabled(false);

        maxResult = Math.min(kanjiStart + 10, bestMatches.size());
        resultLabel.setText("Showing best results " + (kanjiStart + 1) + "-" + maxResult + ":");
    }

    private class Canvas extends JPanel {

        private final int IMAGE_WIDTH = 250;
        private final int IMAGE_OFFSET_X = (FRAME_WIDTH - IMAGE_WIDTH) / 2;
        private final int IMAGE_OFFSET_Y = 40;
        private BufferedImage image;
        private Graphics2D g2;
        private int startX, startY, endX, endY;

        public Canvas() {
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    startX = e.getX();
                    startY = e.getY();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    endX = e.getX();
                    endY = e.getY();
                    g2.setStroke(new BasicStroke(3));

                    // 'IMAGE_OFFSET's account for the image not being located at the
                    // top-left of the screen.
                    g2.drawLine(startX - IMAGE_OFFSET_X, startY - IMAGE_OFFSET_Y,
                        endX - IMAGE_OFFSET_X, endY - IMAGE_OFFSET_Y);
                    repaint();
                    startX = endX;
                    startY = endY;
                    submitButton.setEnabled(true);
                }
            });
        }

        // Painting is done on a Graphics object derived from a BufferedImage (which is what the user is drawing).
        public void paintComponent(Graphics g) {
            if (image == null) {
                image = new BufferedImage(IMAGE_WIDTH, IMAGE_WIDTH, BufferedImage.TYPE_INT_RGB);
                g2 = (Graphics2D) image.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                clear();
            }
            g.drawImage(image, IMAGE_OFFSET_X, IMAGE_OFFSET_Y, null);
        }

        public void clear(){
            g2.setPaint(Color.white);
            g2.fillRect(0, 0, getSize().width, getSize().height);
            g2.setPaint(Color.black);
            repaint();
        }
        public BufferedImage getImage() {
            return image;
        }
    }

    public static void main(String[] args) {
        DrawFrame df = new DrawFrame();
        df.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        df.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        df.setResizable(false);
        df.setVisible(true);
    }
}
