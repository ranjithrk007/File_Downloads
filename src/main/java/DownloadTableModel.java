import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
class DownloadsTableModel extends AbstractTableModel
        implements Observer {
    private static final String[] columnNames = {"URL", "Size in MB", "Progress", "Speed in MB/s",
            "Avg Speed in KB/s", "Elapsed Time", "Remaing Time" ,"Status"};
    private static final Class[] columnClasses = {String.class, String.class,
            JProgressBar.class, String.class, String.class, String.class, String.class, String.class};
    public ArrayList<Download> downloadList = new ArrayList<Download>();
    public void addDownload(Download download) {
        download.addObserver(this);
        downloadList.add(download);
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }
    public Download getDownload(int row) {
        return downloadList.get(row);
    }
    public void clearDownload(int row) {
        downloadList.remove(row);
        fireTableRowsDeleted(row, row);
    }
    public int getColumnCount() {
        return columnNames.length;
    }
    public String getColumnName(int col) {
        return columnNames[col];
    }
    public Class getColumnClass(int col) {
        return columnClasses[col];
    }
    public int getRowCount() {
        return downloadList.size();
    }
    public Object getValueAt(int row, int col) {
        Download download = downloadList.get(row);
        switch (col) {
            case 0:
                return download.getUrl();
            case 1:
                long size = download.getSize();
                return (size == -1) ? "" : Float.toString((float)size/1048576);
            case 2:
                return new Float(download.getProgress());
            case 3:
                return download.getSpeed();
            case 4:
                return download.getAvgSpeed();
            case 5:
                return download.getElapsedTime();
            case 6:
                return download.getRemainingTime();
            case 7:
                return Download.STATUSES[download.getStatus()];
        }
        return "";
    }
    public void update(Observable o, Object arg) {
        int index = downloadList.indexOf(o);
        fireTableRowsUpdated(index, index);
    }
}