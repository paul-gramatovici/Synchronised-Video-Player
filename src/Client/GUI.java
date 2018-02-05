package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;

class GUI {
    private final static int FRAME_PERIOD = 33;

    private Client client;
    private VideoBuffer videoBuffer;

    private JLabel statLabel1 = new JLabel();
    private JLabel statLabel2 = new JLabel();
    private JLabel statLabel3 = new JLabel();
    private JLabel iconLabel = new JLabel();

    private int frames = 0;

    GUI(Client client, VideoBuffer videoBuffer) {
        this.videoBuffer = videoBuffer;
        this.client = client;

        //build GUI
        //--------------------------

        Timer timer = new Timer(FRAME_PERIOD, (e) -> update());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //Frame
        JFrame f = new JFrame("Client");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,0));
        JButton setupButton = new JButton("Setup");
        buttonPanel.add(setupButton);
        JButton playButton = new JButton("Play");
        buttonPanel.add(playButton);
        JButton pauseButton = new JButton("Pause");
        buttonPanel.add(pauseButton);
        JButton tearButton = new JButton("Close");
        buttonPanel.add(tearButton);
        JButton describeButton = new JButton("Session");
        buttonPanel.add(describeButton);
        setupButton.addActionListener((e) -> client.setup());
        playButton.addActionListener((e) -> {client.play(); timer.start();});
        pauseButton.addActionListener((e) -> {client.pause(); timer.stop();});
        tearButton.addActionListener((e) -> {client.teardown(); timer.stop();});
        describeButton.addActionListener((e) -> client.describe());

        //Statistics
        statLabel1.setText("Total Bytes Received: 0");
        statLabel2.setText("Packets Lost: 0");
        statLabel3.setText("Data Rate (bytes/sec): 0");

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        mainPanel.add(statLabel1);
        mainPanel.add(statLabel2);
        mainPanel.add(statLabel3);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);
        statLabel1.setBounds(0,330,380,20);
        statLabel2.setBounds(0,350,380,20);
        statLabel3.setBounds(0,370,380,20);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(380,420));
        f.setVisible(true);
    }

    private void updateStatsLabel() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        statLabel1.setText("Total Bytes Received: " + client.getStats().getTotalBytes());
        statLabel2.setText("Packet Lost Rate: " + formatter.format(client.getStats().getFractionLost()));
        statLabel3.setText("Data Rate: " + formatter.format(client.getStats().getDataRate()) + " bytes/s");
    }

    private void updateFrame() {
        Image frame = videoBuffer.nextFrame();
        ++frames;
        System.out.println(frames + "\n");
        iconLabel.setIcon(new ImageIcon(frame));
    }

    private void update() {
        updateStatsLabel();
        updateFrame();
    }
}
