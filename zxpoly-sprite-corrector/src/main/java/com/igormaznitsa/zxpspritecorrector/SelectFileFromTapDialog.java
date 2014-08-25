package com.igormaznitsa.zxpspritecorrector;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class SelectFileFromTapDialog extends javax.swing.JDialog implements TableModel,ListSelectionListener
{
    protected static class TapItem
    {
        public static final int TYPE_BASIC = 0;
        public static final int TYPE_NUMARRAY = 1;
        public static final int TYPE_SYMARRAY = 2;
        public static final int TYPE_BINFILE = 3;

        protected int i_Type;
        protected int i_OffsetAtFile;
        protected int i_Length;
        protected String s_Name;
        protected int i_SpecialWord1;
        protected int i_SpecialWord2;

        protected int i_BlockType;

        public TapItem(int _offsetAtTAP, String _name, int _type, int _blockType, int _length, int _specialWord1, int _specialWord2)
        {
            i_OffsetAtFile = _offsetAtTAP;
            s_Name = _name;
            i_Type = _type;
            i_Length = _length;
            i_SpecialWord1 = _specialWord1;
            i_SpecialWord2 = _specialWord2;
            i_BlockType = _blockType;
        }
    }

    public static final long serialVersionUID = 3624824l;
    protected HobetaContainer p_Result;
    protected ArrayList<TapItem> p_TAPFileList;
    protected ArrayList<TableModelListener> p_TableModelLIsteners;

    protected File p_File;

    protected boolean lg_Error;

    /** Creates new form SelectFileFromTapDialog */
    public SelectFileFromTapDialog(JFrame _parent)
    {
        super(_parent, true);

        p_TableModelLIsteners = new ArrayList<TableModelListener>();
        p_TAPFileList = new ArrayList<TapItem>();

        initComponents();

        p_Table_Files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p_Table_Files.getSelectionModel().addListSelectionListener(this);

        p_Table_Files.setModel(this);
        p_Result = null;
    }

    public HobetaContainer select(File _tapFile) throws IOException
    {
        p_Result = null;

        p_File = _tapFile;

        fillTableFromFile(_tapFile);

        Dimension p_scrSize = Toolkit.getDefaultToolkit().getScreenSize();

        setLocation((p_scrSize.width - getWidth()) / 2, (p_scrSize.height - getHeight()) / 2);

        setTitle(_tapFile.getName());
        setVisible(true);

        if (lg_Error) throw new IOException("Error during block loading");

        HobetaContainer p_res = p_Result;
        p_Result = null;
        return p_res;
    }

    protected void fillTableFromFile(File _tapFile) throws IOException
    {
        RandomAccessFile p_randomFileAcces = null;

        try
        {
            p_TAPFileList.clear();

            p_randomFileAcces = new RandomAccessFile(_tapFile, "r");

            int i_nonamedDataBlockIndex = 1;
            final String NONAMEDBLOCK = "dblock_";

            TapItem p_item = null;

            while (true)
            {
                int i_position = (int) p_randomFileAcces.getFilePointer();

                int i_len = p_randomFileAcces.read();
                
                if (i_len<0) break;

                i_len |= (p_randomFileAcces.read() << 8);

                if (i_len < 0)
                {
                    throw new IllegalArgumentException();
                }

                if (i_len==0)
                {
                    p_item = null;
                    continue;
                }

                i_len -= 2;

                int i_blockType = p_randomFileAcces.readUnsignedByte();

                switch (i_blockType)
                {
                    case 0:
                        {
                            // rom loader header
                            int i_type = p_randomFileAcces.readUnsignedByte();
                            byte[] ab_arr = new byte[10];
                            p_randomFileAcces.readFully(ab_arr);
                            String s_str = new String(ab_arr);
                            int i_datalen = p_randomFileAcces.readUnsignedByte();
                            i_datalen |= (p_randomFileAcces.readUnsignedByte() << 8);

                            int i_autostart = p_randomFileAcces.readUnsignedByte();
                            i_autostart |= (p_randomFileAcces.readUnsignedByte() << 8);

                            int i_proglength = p_randomFileAcces.readUnsignedByte();
                            i_proglength |= (p_randomFileAcces.readUnsignedByte() << 8);

                            p_item = new TapItem(i_position, s_str, i_type,i_blockType, i_datalen, i_autostart, i_proglength);

                            p_randomFileAcces.skipBytes(1);
                        }
                        break;
                    case 0xFF:
                        {
                            // data block
                            if (p_item == null)
                            {
                                p_item = new TapItem(i_position, NONAMEDBLOCK + (i_nonamedDataBlockIndex++), TapItem.TYPE_BINFILE,i_blockType, i_len, 0, 0);
                                p_TAPFileList.add(p_item);
                            }
                            else
                            {
                                if (p_item.i_Length != i_len)
                                {
                                    p_item = new TapItem(i_position, NONAMEDBLOCK + (i_nonamedDataBlockIndex++), TapItem.TYPE_BINFILE,i_blockType, i_len, 0, 0);
                                }
                                else
                                {
                                    p_item.i_OffsetAtFile = i_position;
                                }

                                p_TAPFileList.add(p_item);
                                p_item = null;
                            }

                            p_randomFileAcces.skipBytes(i_len);
                            p_randomFileAcces.skipBytes(1);
                        }
                        break;
                    default:
                    {
                        // unknown data
                        p_item = new TapItem(i_position, NONAMEDBLOCK + (i_nonamedDataBlockIndex++), TapItem.TYPE_BINFILE,i_blockType, i_len, 0, 0);
                        p_TAPFileList.add(p_item);
                        p_item = null;

                        p_randomFileAcces.skipBytes(i_len);
                        p_randomFileAcces.skipBytes(1);
                    }
                }
            }

        }
        finally
        {
            if (p_randomFileAcces != null)
            {
                try
                {
                    p_randomFileAcces.close();
                }
                catch (Throwable _thr)
                {
                }
            }

            for (TableModelListener p_listener : p_TableModelLIsteners)
            {
                p_listener.tableChanged(new TableModelEvent(this));
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    p_Button_Cancel = new javax.swing.JButton();
    p_Button_Ok = new javax.swing.JButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    p_Table_Files = new javax.swing.JTable();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Select a data file from the TAP file");

    p_Button_Cancel.setText("Cancel");
    p_Button_Cancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        p_Button_CancelActionPerformed(evt);
      }
    });

    p_Button_Ok.setText("Select");
    p_Button_Ok.setEnabled(false);
    p_Button_Ok.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        p_Button_OkActionPerformed(evt);
      }
    });

    p_Table_Files.setAutoCreateRowSorter(true);
    p_Table_Files.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][] {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String [] {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    jScrollPane1.setViewportView(p_Table_Files);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addComponent(p_Button_Ok)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(p_Button_Cancel))
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE))
        .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {p_Button_Cancel, p_Button_Ok});

    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(p_Button_Cancel)
          .addComponent(p_Button_Ok))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void p_Button_OkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_p_Button_OkActionPerformed
    {//GEN-HEADEREND:event_p_Button_OkActionPerformed

        try
        {
            TapItem p_item = p_TAPFileList.get(p_Table_Files.getSelectedRow());

            RandomAccessFile p_accFile = null;

            try
            {
                p_accFile = new RandomAccessFile(p_File,"r");
                p_accFile.seek(p_item.i_OffsetAtFile);

                byte [] ab_data = new byte[p_item.i_Length];

                p_accFile.skipBytes(3);
                p_accFile.readFully(ab_data);

                p_Result = new HobetaContainer(p_item, ab_data);
            }
            finally
            {
                if (p_accFile != null)
                {
                    try
                    {
                      p_accFile.close();
                    }
                    catch(Throwable _th)
                    {}
                }
            }
        }
        catch(Throwable _thr)
        {
            lg_Error = true;
            p_Result = null;
            _thr.printStackTrace();
        }

        dispose();
    }//GEN-LAST:event_p_Button_OkActionPerformed

    private void p_Button_CancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_p_Button_CancelActionPerformed
    {//GEN-HEADEREND:event_p_Button_CancelActionPerformed
        p_Result = null;
        dispose();
    }//GEN-LAST:event_p_Button_CancelActionPerformed
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JButton p_Button_Cancel;
  private javax.swing.JButton p_Button_Ok;
  private javax.swing.JTable p_Table_Files;
  // End of variables declaration//GEN-END:variables

    public int getRowCount()
    {
        return p_TAPFileList.size();
    }

    public int getColumnCount()
    {
        return 5;
    }

    public String getColumnName(int columnIndex)
    {
        switch (columnIndex)
        {
            case 0:
                return "№";
            case 1:
                return "Name";
            case 2:
                return "Type";
            case 3:
                return "Len.";
            case 4:
                return "Start str./Load addr.";

            default:
                return null;
        }
    }

    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        TapItem p_item = p_TAPFileList.get(rowIndex);

        switch (columnIndex)
        {
            case 0:
                return Integer.toString(rowIndex + 1);
            case 1:
                return p_item.s_Name;
            case 2:
            {
                switch (p_item.i_Type)
                {
                    case TapItem.TYPE_BASIC:
                        return "basic";
                    case TapItem.TYPE_BINFILE:
                        return "code";
                    case TapItem.TYPE_NUMARRAY:
                        return "num.array";
                    case TapItem.TYPE_SYMARRAY:
                        return "sym.array";
                    default:
                    {
                        return "<unknown>";
                    }
                }
            }
            case 3:
            {
                return Integer.toString(p_item.i_Length);
            }
            case 4:
            {
                switch (p_item.i_Type)
                {
                    case TapItem.TYPE_BASIC:
                    case TapItem.TYPE_BINFILE:
                        return p_item.i_SpecialWord1;
                    default:
                        return "";
                }
            }
            default:
                return "<?>";
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
    }

    public void addTableModelListener(TableModelListener l)
    {
        synchronized (p_TableModelLIsteners)
        {
            p_TableModelLIsteners.add(l);
        }
    }

    public void removeTableModelListener(TableModelListener l)
    {
        synchronized (p_TableModelLIsteners)
        {
            p_TableModelLIsteners.remove(l);
        }
    }

    public void valueChanged(ListSelectionEvent e)
    {
        p_Button_Ok.setEnabled(p_Table_Files.getSelectedRow()>=0);
    }
}
