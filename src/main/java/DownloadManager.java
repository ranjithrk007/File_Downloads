import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.event.*;

public class DownloadManager extends JFrame
        implements Observer {
    public Queue<Download> remaining = new LinkedList();
    private JTextField addTextField, limitTextField;
    static DownloadsTableModel tableModel;
    private JTable table;
    private JButton pauseButton, resumeButton;
    private JButton cancelButton, clearButton;
    private Download selectedDownload;
    private boolean clearing;
    int value, end;
    private static Queue<URL> queue = new LinkedList<URL>();

    public DownloadManager() {
        setTitle("Download Manager");
        setSize(640, 480);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit",
                KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        limitTextField = new JTextField(5);
        addPanel.add(addTextField);
        addPanel.add(limitTextField);
        String count = limitTextField.getText();
        System.out.println(count);
        JButton addButton = new JButton("Add Download");
        JButton download = new JButton("Download");
        download.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadaction();
            }
        });
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        addPanel.add(addButton);
        addPanel.add(download);
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true);
        table.setDefaultRenderer(JProgressBar.class, renderer);
        table.setRowHeight(
                (int) renderer.getPreferredSize().getHeight());
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(
                BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table),
                BorderLayout.CENTER);
        JPanel buttonsPanel = new JPanel();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        });
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    ExecutorService service = Executors.newFixedThreadPool(4);

    public boolean startDownload(final DownloadsTableModel tableModel, final int start, final int end1) {
        start(tableModel, start, end1);
        SwingWorker swingWorker = new SwingWorker() {
            @Override
            protected String doInBackground() throws Exception {


                for (int i = start; i < end1; ) {
                    if (tableModel.downloadList.get(i).getStatus() == 2) {
                        System.out.println(tableModel.downloadList.get(i) + " downloaded");
                        i++;
                    }
                }
                return "ok";
            }

            @Override
            protected void done() {
                try {
                    String msg = (String) get();
                    System.out.println("ok");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        swingWorker.execute();
        return true;
    }

    private void downloadaction() {
//http://releases.ubuntu.com/18.04.2/ubuntu-18.04.2-desktop-amd64.iso

        String parallelcount = limitTextField.getText();
        value = Integer.parseInt(parallelcount);
        System.out.println("value is " + value);
        System.out.println("table " + tableModel.downloadList.size());

        List<Download> candidateDownloads = new ArrayList<Download>();

        for (int k = 0; k < tableModel.downloadList.size(); k++) {
            if (tableModel.downloadList.get(k).getStatus() == 0 || tableModel.downloadList.get(k).getStatus() == 1) {
                tableModel.downloadList.get(k).pause();
                if (candidateDownloads.size()< value){
                    candidateDownloads.add(tableModel.downloadList.get(k));
                }
            }
        }

        for (final Download candidate: candidateDownloads){
            service.submit(new Runnable() {
                public void run() {
                    candidate.resume();
                }
            });
        }

        updateButtons();
    }

    public void start(DownloadsTableModel tableModel, int value, int end) {
        for (int i = value; i < end; i++) {
            if (tableModel.downloadList.size() <= i) {
                System.out.println("ended");
                break;
            }
            tableModel.downloadList.get(i).resume();
        }
        System.out.println("started");
    }

    private void actionExit() {
        System.exit(0);
    }

    private void actionAdd() {
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if (verifiedUrl != null) {
            queue.add(verifiedUrl);
            Download download = new Download(verifiedUrl);
            download.pause();
            tableModel.addDownload(download);
            addTextField.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid Download URL", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public int checker(DownloadsTableModel tableModel, int value, int end) {
        for (int i = value; i < end; i++) {
            if (tableModel.downloadList.get(i).getStatus() == 2) {
                System.out.println(tableModel.downloadList.get(i) + " downloaded");
            } else {
                return 0;
            }
        }
        return 1;
    }

    private URL verifyUrl(String url) {
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
            return null;
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }
        if (verifiedUrl.getFile().length() < 2)
            return null;
        return verifiedUrl;
    }

    private void tableSelectionChanged() {
        if (selectedDownload != null)
            selectedDownload.deleteObserver(DownloadManager.this);
        if (!clearing) {
            selectedDownload =
                    tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    private void actionPause() {
        selectedDownload.pause();
        updateButtons();
    }

    private void actionResume() {
        selectedDownload.resume();
        updateButtons();
    }

    private void actionCancel() {
        selectedDownload.cancel();
        updateButtons();
    }

    private void actionClear() {
        clearing = true;
        tableModel.clearDownload(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    private void updateButtons() {
        if (selectedDownload != null) {
            int status = selectedDownload.getStatus();
            switch (status) {
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
                default:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
            }
        } else {
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    public void update(Observable o, Object arg) {
        if (selectedDownload != null && selectedDownload.equals(o))
            updateButtons();
    }

    public static void main(String[] args) {
        DownloadManager manager = new DownloadManager();
        ImageIcon img = new ImageIcon("download.png");
        manager.setIconImage(img.getImage());
        manager.setVisible(true);
    }
}