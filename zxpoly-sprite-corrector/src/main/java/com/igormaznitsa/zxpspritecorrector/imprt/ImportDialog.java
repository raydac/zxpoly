package com.igormaznitsa.zxpspritecorrector.imprt;

import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;
import com.igormaznitsa.zxpspritecorrector.HobetaContainer;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class ImportDialog extends javax.swing.JDialog
{
    protected EditorComponent p_Editor;
    protected static File p_LastSelected;
    protected File p_FileCPU0;
    protected File p_FileCPU1;
    protected File p_FileCPU2;
    protected File p_FileCPU3;

    public ImportDialog(java.awt.Frame parent, EditorComponent _editor)
    {
        super(parent);
        initComponents();

        p_FileCPU0 = null;
        p_FileCPU1 = null;
        p_FileCPU2 = null;
        p_FileCPU3 = null;

        p_TextField_CPU0.setText("");
        p_TextField_CPU1.setText("");
        p_TextField_CPU2.setText("");
        p_TextField_CPU3.setText("");

        p_Editor = _editor;
        
        Dimension p_dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((p_dim.width-getWidth())/2,(p_dim.height-getHeight())/2);
        
        setVisible(true);
    }

    protected File selectFile()
    {
        final int i_origlen = p_Editor.getHobetaContainer().getDataArray().length + 17;

        JFileChooser p_chooser = new JFileChooser(p_LastSelected);
        p_chooser.setFileFilter(new FileFilter()
        {

            @Override
            public boolean accept(File f)
            {
                if (f == null)
                {
                    return false;
                }
                if (f.isDirectory())
                {
                    return true;
                }
                if (f.length() == i_origlen && f.getName().contains(".$"))
                {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription()
            {
                return "Hobeta files (*.$*)";
            }
        });

        p_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (p_chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            p_LastSelected = p_chooser.getSelectedFile();
            return p_LastSelected;
        }
        else
        {
            return null;
        }
    }

  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jPanel1 = new javax.swing.JPanel();
    p_TextField_CPU0 = new javax.swing.JTextField();
    p_Select_CPU0 = new javax.swing.JButton();
    jPanel2 = new javax.swing.JPanel();
    p_TextField_CPU1 = new javax.swing.JTextField();
    p_Select_CPU1 = new javax.swing.JButton();
    jPanel3 = new javax.swing.JPanel();
    p_TextField_CPU2 = new javax.swing.JTextField();
    p_Select_CPU2 = new javax.swing.JButton();
    jPanel4 = new javax.swing.JPanel();
    p_TextField_CPU3 = new javax.swing.JTextField();
    p_Select_CPU3 = new javax.swing.JButton();
    jButton5 = new javax.swing.JButton();
    jButton6 = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Import Hobeta files as color data");
    setModal(true);
    setResizable(false);

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("File for CPU0"));

    p_TextField_CPU0.setEditable(false);

    p_Select_CPU0.setText("Select");
    p_Select_CPU0.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        p_Select_CPU0ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(p_TextField_CPU0, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(p_Select_CPU0)
        .addContainerGap(22, Short.MAX_VALUE))
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(p_TextField_CPU0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(p_Select_CPU0))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("File for CPU1"));

    p_TextField_CPU1.setEditable(false);

    p_Select_CPU1.setText("Select");
    p_Select_CPU1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        p_Select_CPU1ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel2Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(p_TextField_CPU1, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(p_Select_CPU1)
        .addContainerGap(22, Short.MAX_VALUE))
    );
    jPanel2Layout.setVerticalGroup(
      jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel2Layout.createSequentialGroup()
        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(p_TextField_CPU1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(p_Select_CPU1))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("File for CPU2"));

    p_TextField_CPU2.setEditable(false);

    p_Select_CPU2.setText("Select");
    p_Select_CPU2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        p_Select_CPU2ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
    jPanel3.setLayout(jPanel3Layout);
    jPanel3Layout.setHorizontalGroup(
      jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 380, Short.MAX_VALUE)
      .addGroup(jPanel3Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(p_TextField_CPU2, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(p_Select_CPU2)
        .addContainerGap(22, Short.MAX_VALUE))
    );
    jPanel3Layout.setVerticalGroup(
      jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 34, Short.MAX_VALUE)
      .addGroup(jPanel3Layout.createSequentialGroup()
        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(p_TextField_CPU2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(p_Select_CPU2))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("File for CPU3"));

    p_TextField_CPU3.setEditable(false);

    p_Select_CPU3.setText("Select");
    p_Select_CPU3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        p_Select_CPU3ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
    jPanel4.setLayout(jPanel4Layout);
    jPanel4Layout.setHorizontalGroup(
      jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 380, Short.MAX_VALUE)
      .addGap(0, 380, Short.MAX_VALUE)
      .addGroup(jPanel4Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(p_TextField_CPU3, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(p_Select_CPU3)
        .addContainerGap(22, Short.MAX_VALUE))
    );
    jPanel4Layout.setVerticalGroup(
      jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 34, Short.MAX_VALUE)
      .addGap(0, 34, Short.MAX_VALUE)
      .addGroup(jPanel4Layout.createSequentialGroup()
        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(p_TextField_CPU3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(p_Select_CPU3))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    jButton5.setText("Import");
    jButton5.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton5ActionPerformed(evt);
      }
    });

    jButton6.setText("Cancel");
    jButton6.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton6ActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addComponent(jButton5)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(jButton6)))
        .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButton5, jButton6});

    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jButton6)
          .addComponent(jButton5))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel1, jPanel2, jPanel3, jPanel4});

    pack();
  }// </editor-fold>//GEN-END:initComponents
    private void p_Select_CPU0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p_Select_CPU0ActionPerformed
        p_FileCPU0 = selectFile();
        if (p_FileCPU0 != null)
        {
            p_TextField_CPU0.setText(p_FileCPU0.getAbsolutePath());
        }
        else
        {
            p_TextField_CPU0.setText("");
        }
    }//GEN-LAST:event_p_Select_CPU0ActionPerformed

    private void p_Select_CPU1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p_Select_CPU1ActionPerformed
        p_FileCPU1 = selectFile();
        if (p_FileCPU1 != null)
        {
            p_TextField_CPU1.setText(p_FileCPU1.getAbsolutePath());
        }
        else
        {
            p_TextField_CPU1.setText("");
        }

    }//GEN-LAST:event_p_Select_CPU1ActionPerformed

    private void p_Select_CPU2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p_Select_CPU2ActionPerformed
        p_FileCPU2 = selectFile();
        if (p_FileCPU2 != null)
        {
            p_TextField_CPU2.setText(p_FileCPU2.getAbsolutePath());
        }
        else
        {
            p_TextField_CPU2.setText("");
        }
    }//GEN-LAST:event_p_Select_CPU2ActionPerformed

    private void p_Select_CPU3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p_Select_CPU3ActionPerformed
        p_FileCPU3 = selectFile();
        if (p_FileCPU3 != null)
        {
            p_TextField_CPU3.setText(p_FileCPU3.getAbsolutePath());
        }
        else
        {
            p_TextField_CPU3.setText("");
        }
    }//GEN-LAST:event_p_Select_CPU3ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // import 
        byte[] ab_hobdata = p_Editor.getDataArray();
        int[] ai_curcolor = p_Editor.getColorArray();

        byte[] ab_mask = new byte[ab_hobdata.length];
        int[] ai_colordata = new int[ab_hobdata.length];//GEN-LAST:event_jButton5ActionPerformed
        System.arraycopy(ai_curcolor, 0, ai_colordata, 0, ai_curcolor.length);

        try
        {
            if (p_FileCPU0 != null)
            {
                importFile(p_FileCPU0, 0, ai_colordata);
            }

            if (p_FileCPU1 != null)
            {
                importFile(p_FileCPU1, 1, ai_colordata);
            }
        
            if (p_FileCPU2 != null)
            {
                importFile(p_FileCPU2, 2, ai_colordata);
            }

            if (p_FileCPU3 != null)
            {
                importFile(p_FileCPU3, 3, ai_colordata);
            }
        
            
            // calculating mask
            int i_len = ab_mask.length;
            for(int li=0;li<i_len;li++)
            {
                int i_origdata = ab_hobdata[li];
                int i_newcolordata = ai_colordata[li];
                
                int i_maskacc = 0;
                
                for(int ld=0;ld<8;ld++)
                {
                    i_maskacc<<=1;
                    
                    boolean lg_zx = (i_origdata & 0x80)!=0;
                    int i_colorindex = (i_newcolordata>>>28)&0xF;
                    
                    switch(i_colorindex)
                    {
                        case 0:
                        {
                            if (lg_zx) i_maskacc|=1;
                        }break;
                        case 0xF:
                        {
                            if (!lg_zx) i_maskacc|=1;
                        }break;
                        default:
                        {
                            i_maskacc|=1;
                        }break;
                    }
                    
                    i_origdata<<=1;
                    i_newcolordata<<=4;
                }
                
                ab_mask[li] = (byte)i_maskacc;
            }
            
            System.arraycopy(ai_colordata,0,ai_curcolor, 0, ai_colordata.length);
            System.arraycopy(ab_mask, 0,p_Editor.getMaskArray(), 0, ab_mask.length);
            
            JOptionPane.showMessageDialog(this, "Data has been imported", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Throwable _thr)
        {
            _thr.printStackTrace();
            
            JOptionPane.showMessageDialog(this, _thr.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        dispose();
    }

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void importFile(File _file, int _cpu, int[] _colorsarray) throws IOException
    {
        int[] ai_colorarray = _colorsarray;

        HobetaContainer p_cont = new HobetaContainer(_file);

        byte[] ab_data = p_cont.getDataArray();
        if (ab_data.length != ai_colorarray.length)
        {
            throw new IOException("File " + _file.getName() + " has wrong length");
        }

        int i_len = ab_data.length;
        for (int li = 0; li < i_len; li++)
        {
            int i_zx = ab_data[li] & 0xFF;
            int i_poly = ai_colorarray[li];

            int i_mask = 0;
            switch (_cpu)
            {
                case 0:
                    {
                        i_mask = 0x40000000;
                    }
                    break;
                case 1:
                    {
                        i_mask = 0x20000000;
                    }
                    break;
                case 2:
                    {
                        i_mask = 0x10000000;
                    }
                    break;
                case 3:
                    {
                        i_mask = 0x80000000;
                    }
                    break;
                default: throw new Error ("Wrong cpu index");
            }

            for (int ld = 0; ld < 8; ld++)
            {
                if ((i_zx & 0x80) == 0)
                {
                    i_poly &= (~i_mask);
                }
                else
                {
                    i_poly |= i_mask;
                }

                i_zx <<= 1;
                i_mask >>>= 4;
            }

            ai_colorarray[li] = i_poly;
        }
    }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton5;
  private javax.swing.JButton jButton6;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JButton p_Select_CPU0;
  private javax.swing.JButton p_Select_CPU1;
  private javax.swing.JButton p_Select_CPU2;
  private javax.swing.JButton p_Select_CPU3;
  private javax.swing.JTextField p_TextField_CPU0;
  private javax.swing.JTextField p_TextField_CPU1;
  private javax.swing.JTextField p_TextField_CPU2;
  private javax.swing.JTextField p_TextField_CPU3;
  // End of variables declaration//GEN-END:variables
}
