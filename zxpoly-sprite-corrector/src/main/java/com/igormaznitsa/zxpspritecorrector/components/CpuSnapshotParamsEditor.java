package com.igormaznitsa.zxpspritecorrector.components;

import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.IFF1;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.IFF2;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.PORT_7FFD;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_A;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_ALT_A;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_ALT_BC;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_ALT_DE;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_ALT_F;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_ALT_HL;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_BC;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_DE;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_F;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_HL;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_IM;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_IR;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_IX;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_IY;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_PC;
import static com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties.REG_SP;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.extractHeader;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.extractZ80SnapshotHeader;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.getPcReg;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.getPort7ffd;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.getVersion;

import com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class CpuSnapshotParamsEditor extends JPanel {

  private final JCheckBox checkBoxIFF1;
  private final JCheckBox checkBoxIFF2;
  private final HexValue2Field fieldAltRegA;
  private final HexValue4Field fieldAltRegBC;
  private final HexValue4Field fieldAltRegDE;
  private final HexValue4Field fieldAltRegHL;
  private final HexValue4Field fieldIR;
  private final HexValue2Field fieldIM;
  private final HexValue4Field fieldIX;
  private final HexValue4Field fieldIY;
  private final HexValue4Field fieldPC;
  private final HexValue2Field fieldRegA;
  private final HexValue4Field fieldRegBC;
  private final HexValue4Field fieldRegDE;
  private final HexValue4Field fieldRegHL;
  private final HexValue2Field field7FFD;
  private final HexValue4Field fieldSP;
  private final JPanel panelAltSet;
  private final JPanel panelOthers;
  private final FlagPanel panelFlags;
  private final FlagPanel panelFlagsAlt;
  private final JPanel panelMainSet;
  private final int snapshotVersion;
  private final byte[] snapshotHeader;
  private final Z80InZXPOutPlugin.Z80MainHeader mainSnapshotHeader;
  public CpuSnapshotParamsEditor(
      final ZXPolyData zxPolyData,
      final CpuRegProperties properties) throws IOException {
    super(new GridBagLayout());

    final byte[] extra = zxPolyData.getInfo().getExtra();
    this.snapshotHeader = extractHeader(extra);
    this.snapshotVersion = getVersion(snapshotHeader);
    this.mainSnapshotHeader = extractZ80SnapshotHeader(snapshotHeader);
    final int regPc = getPcReg(mainSnapshotHeader, snapshotHeader);

    this.checkBoxIFF1 =
        new JCheckBox("IFF1", properties.extractBoolean(IFF1, mainSnapshotHeader.iff != 0));
    this.checkBoxIFF2 =
        new JCheckBox("IFF2", properties.extractBoolean(IFF2, mainSnapshotHeader.iff2 != 0));

    this.fieldAltRegA =
        new HexValue2Field(properties.extractInt(REG_ALT_A, mainSnapshotHeader.reg_a_alt));
    this.fieldAltRegBC =
        new HexValue4Field(properties.extractInt(REG_ALT_BC, mainSnapshotHeader.reg_bc_alt));
    this.fieldAltRegDE =
        new HexValue4Field(properties.extractInt(REG_ALT_DE, mainSnapshotHeader.reg_de_alt));
    this.fieldAltRegHL =
        new HexValue4Field(properties.extractInt(REG_ALT_HL, mainSnapshotHeader.reg_hl_alt));
    this.fieldIM = new HexValue2Field(
        properties.extractInt(REG_IM, mainSnapshotHeader.emulFlags.interruptmode));
    this.fieldIR = new HexValue4Field(properties.extractInt(REG_IR,
        makePair(mainSnapshotHeader.reg_ir, mainSnapshotHeader.reg_r)));
    this.fieldIX = new HexValue4Field(properties.extractInt(REG_IX, mainSnapshotHeader.reg_ix));
    this.fieldIY = new HexValue4Field(properties.extractInt(REG_IY, mainSnapshotHeader.reg_iy));
    this.fieldPC = new HexValue4Field(properties.extractInt(REG_PC, regPc));
    this.fieldSP = new HexValue4Field(properties.extractInt(REG_SP, mainSnapshotHeader.reg_sp));
    this.fieldRegA = new HexValue2Field(properties.extractInt(REG_A, mainSnapshotHeader.reg_a));
    this.fieldRegBC = new HexValue4Field(properties.extractInt(REG_BC, mainSnapshotHeader.reg_bc));
    this.fieldRegDE = new HexValue4Field(properties.extractInt(REG_DE, mainSnapshotHeader.reg_de));
    this.fieldRegHL = new HexValue4Field(properties.extractInt(REG_HL, mainSnapshotHeader.reg_hl));
    this.field7FFD = new HexValue2Field(
        properties.extractInt(PORT_7FFD, getPort7ffd(snapshotVersion, snapshotHeader)));

    this.panelMainSet = this.makePanelMainSet();
    this.panelAltSet = this.makePanelAltSet();
    this.panelOthers = this.makePanelOthers();
    this.panelFlags =
        new FlagPanel("Flags", false, properties.extractInt(REG_F, mainSnapshotHeader.reg_f));
    this.panelFlagsAlt = new FlagPanel("Flags'", true,
        properties.extractInt(REG_ALT_F, mainSnapshotHeader.reg_f_alt));

    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.insets = new Insets(0, 0, 0, 0);
    constraints.ipadx = 0;
    constraints.ipady = 0;
    constraints.weightx = 1;
    constraints.weighty = 1;

    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 2;
    constraints.gridheight = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    this.add(this.panelFlags, constraints);

    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.gridwidth = 2;
    constraints.gridheight = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    this.add(this.panelFlagsAlt, constraints);

    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.fill = GridBagConstraints.BOTH;
    this.add(this.panelMainSet, constraints);

    constraints.gridx = 1;
    constraints.gridy = 2;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.fill = GridBagConstraints.BOTH;
    this.add(this.panelAltSet, constraints);

    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.gridwidth = 2;
    constraints.gridheight = 1;
    constraints.weighty = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    this.add(this.panelOthers, constraints);

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    final JButton buttonReset = new JButton("Reset");
    buttonReset.addActionListener(a -> {
      if (JOptionPane.showConfirmDialog(this, "Do you really want reset data?", "Confirmation",
          JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        this.reset();
      }
    });
    buttonsPanel.add(buttonReset);

    constraints.gridx = 1;
    constraints.gridy = 4;
    constraints.gridwidth = 2;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    this.add(buttonsPanel, constraints);

    constraints.gridx = 1;
    constraints.gridy = 5;
    constraints.gridwidth = 2;
    constraints.gridheight = 1;
    constraints.weighty = 1000;
    constraints.fill = GridBagConstraints.BOTH;

    this.add(Box.createVerticalGlue(), constraints);
  }

  private static void addPropertyIfChanged(final Properties properties, final String name,
                                           final boolean value, final boolean orig) {
    if (value != orig) {
      properties.put(name, Boolean.toString(value));
    }
  }

  private static void addPropertyIfChanged(final Properties properties, final String name,
                                           final int value, final byte orig) {
    if (value != (orig & 0xFF)) {
      properties.put(name, Integer.toString(value));
    }
  }

  private static void addPropertyIfChanged(final Properties properties, final String name,
                                           final int value, final int orig) {
    if (value != (orig & 0xFFFF)) {
      properties.put(name, Integer.toString(value));
    }
  }

  public static int makePair(final byte a, final byte b) {
    return ((a & 0xFF) << 8) | (b & 0xFF);
  }

  private static JLabel makeCenteredLabel(final String text) {
    final JLabel result = new JLabel(text);
    result.setHorizontalAlignment(JLabel.CENTER);
    return result;
  }

  public void reset() {
    this.checkBoxIFF1.setSelected(mainSnapshotHeader.iff != 0);
    this.checkBoxIFF2.setSelected(mainSnapshotHeader.iff2 != 0);

    this.fieldAltRegA.setValue(mainSnapshotHeader.reg_a_alt);
    this.fieldAltRegBC.setValue(mainSnapshotHeader.reg_bc_alt);
    this.fieldAltRegDE.setValue(mainSnapshotHeader.reg_de_alt);
    this.fieldAltRegHL.setValue(mainSnapshotHeader.reg_hl_alt);
    this.panelFlagsAlt.setData(mainSnapshotHeader.reg_f_alt);

    this.fieldRegA.setValue(mainSnapshotHeader.reg_a);
    this.fieldRegBC.setValue(mainSnapshotHeader.reg_bc);
    this.fieldRegDE.setValue(mainSnapshotHeader.reg_de);
    this.fieldRegHL.setValue(mainSnapshotHeader.reg_hl);
    this.panelFlags.setData(mainSnapshotHeader.reg_f);

    this.field7FFD.setValue(getPort7ffd(snapshotVersion, snapshotHeader));

    this.fieldIM.setValue(mainSnapshotHeader.emulFlags.interruptmode);
    this.fieldIR.setValue(makePair(mainSnapshotHeader.reg_ir, mainSnapshotHeader.reg_r));
    this.fieldSP.setValue(mainSnapshotHeader.reg_sp);
    this.fieldPC.setValue(getPcReg(mainSnapshotHeader, snapshotHeader));

    this.fieldIX.setValue(mainSnapshotHeader.reg_ix);
    this.fieldIY.setValue(mainSnapshotHeader.reg_iy);
  }

  public CpuRegProperties asProperties() {
    final CpuRegProperties result = new CpuRegProperties();

    addPropertyIfChanged(result, IFF1, this.checkBoxIFF1.isSelected(), mainSnapshotHeader.iff != 0);
    addPropertyIfChanged(result, IFF2, this.checkBoxIFF2.isSelected(),
        mainSnapshotHeader.iff2 != 0);

    addPropertyIfChanged(result, REG_ALT_A, this.fieldAltRegA.getIntValue(),
        mainSnapshotHeader.reg_a_alt);
    addPropertyIfChanged(result, REG_ALT_BC, this.fieldAltRegBC.getIntValue(),
        mainSnapshotHeader.reg_bc_alt);
    addPropertyIfChanged(result, REG_ALT_DE, this.fieldAltRegDE.getIntValue(),
        mainSnapshotHeader.reg_de_alt);
    addPropertyIfChanged(result, REG_ALT_HL, this.fieldAltRegHL.getIntValue(),
        mainSnapshotHeader.reg_hl_alt);
    addPropertyIfChanged(result, REG_ALT_F, this.panelFlagsAlt.getData(),
        mainSnapshotHeader.reg_f_alt);

    addPropertyIfChanged(result, REG_A, this.fieldRegA.getIntValue(), mainSnapshotHeader.reg_a);
    addPropertyIfChanged(result, REG_BC, this.fieldRegBC.getIntValue(), mainSnapshotHeader.reg_bc);
    addPropertyIfChanged(result, REG_DE, this.fieldRegDE.getIntValue(), mainSnapshotHeader.reg_de);
    addPropertyIfChanged(result, REG_HL, this.fieldRegHL.getIntValue(), mainSnapshotHeader.reg_hl);
    addPropertyIfChanged(result, REG_F, this.panelFlags.getData(), mainSnapshotHeader.reg_f);

    addPropertyIfChanged(result, PORT_7FFD, this.field7FFD.getIntValue(),
        getPort7ffd(snapshotVersion, snapshotHeader));
    addPropertyIfChanged(result, REG_IM, this.fieldIM.getIntValue(),
        mainSnapshotHeader.emulFlags.interruptmode);
    addPropertyIfChanged(result, REG_IR, this.fieldIR.getIntValue(),
        makePair(mainSnapshotHeader.reg_ir, mainSnapshotHeader.reg_r));
    addPropertyIfChanged(result, REG_SP, this.fieldSP.getIntValue(), mainSnapshotHeader.reg_sp);
    addPropertyIfChanged(result, REG_PC, this.fieldPC.getIntValue(),
        getPcReg(mainSnapshotHeader, snapshotHeader));

    addPropertyIfChanged(result, REG_IX, this.fieldIX.getIntValue(), mainSnapshotHeader.reg_ix);
    addPropertyIfChanged(result, REG_IY, this.fieldIY.getIntValue(), mainSnapshotHeader.reg_iy);

    return result;
  }

  private JPanel makePanelOthers() {
    final JPanel result = new JPanel(new GridBagLayout());
    result.setBorder(new TitledBorder("Others"));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.anchor = GridBagConstraints.EAST;

    result.add(new JLabel("PC:"), gbc);
    result.add(new JLabel("SP:"), gbc);
    result.add(new JLabel("IX:"), gbc);
    result.add(new JLabel("IY:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(this.fieldPC, gbc);
    result.add(this.fieldSP, gbc);
    result.add(this.fieldIX, gbc);
    result.add(this.fieldIY, gbc);

    gbc.gridx = 2;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.insets = new Insets(0, 16, 0, 0);

    result.add(new JLabel("IR:"), gbc);
    result.add(new JLabel("IM:"), gbc);
    result.add(new JLabel("7FFD:"), gbc);

    gbc.gridx = 3;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 0, 0);

    result.add(this.fieldIR, gbc);
    result.add(this.fieldIM, gbc);
    result.add(this.field7FFD, gbc);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    final JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flagsPanel.add(this.checkBoxIFF1);
    flagsPanel.add(this.checkBoxIFF2);

    result.add(flagsPanel, gbc);

    return result;
  }

  private JPanel makePanelAltSet() {
    final JPanel result = new JPanel(new GridBagLayout());
    result.setBorder(new TitledBorder("Alt set"));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.EAST;

    result.add(new JLabel("A':"), gbc);
    result.add(new JLabel("BC':"), gbc);
    result.add(new JLabel("DE':"), gbc);
    result.add(new JLabel("HL':"), gbc);

    gbc.gridx = 1;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(this.fieldAltRegA, gbc);
    result.add(this.fieldAltRegBC, gbc);
    result.add(this.fieldAltRegDE, gbc);
    result.add(this.fieldAltRegHL, gbc);
    return result;
  }

  private JPanel makePanelMainSet() {
    final JPanel result = new JPanel(new GridBagLayout());
    result.setBorder(new TitledBorder("Main set"));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.EAST;

    result.add(new JLabel("A:"), gbc);
    result.add(new JLabel("BC:"), gbc);
    result.add(new JLabel("DE:"), gbc);
    result.add(new JLabel("HL:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(this.fieldRegA, gbc);
    result.add(this.fieldRegBC, gbc);
    result.add(this.fieldRegDE, gbc);
    result.add(this.fieldRegHL, gbc);
    return result;
  }

  private static class FlagPanel extends JPanel {
    private final JCheckBox checkBoxC = new JCheckBox();
    private final JCheckBox checkBoxF3 = new JCheckBox();
    private final JCheckBox checkBoxF5 = new JCheckBox();
    private final JCheckBox checkBoxH = new JCheckBox();
    private final JCheckBox checkBoxN = new JCheckBox();
    private final JCheckBox checkBoxPV = new JCheckBox();
    private final JCheckBox checkBoxS = new JCheckBox();
    private final JCheckBox checkBoxZ = new JCheckBox();

    FlagPanel(final String title, final boolean alt, final int value) {
      super(new GridLayout(2, 8, 8, 8));
      this.setBorder(new TitledBorder(title));

      if (alt) {
        this.add(makeCenteredLabel("S'"));
        this.add(makeCenteredLabel("Z'"));
        this.add(makeCenteredLabel("F5'"));
        this.add(makeCenteredLabel("H'"));
        this.add(makeCenteredLabel("F3'"));
        this.add(makeCenteredLabel("P/V'"));
        this.add(makeCenteredLabel("N'"));
        this.add(makeCenteredLabel("C'"));
      } else {
        this.add(makeCenteredLabel("S"));
        this.add(makeCenteredLabel("Z"));
        this.add(makeCenteredLabel("F5"));
        this.add(makeCenteredLabel("H"));
        this.add(makeCenteredLabel("F3"));
        this.add(makeCenteredLabel("P/V"));
        this.add(makeCenteredLabel("N"));
        this.add(makeCenteredLabel("C"));
      }

      this.checkBoxS.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxC.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxF3.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxF5.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxH.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxN.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxZ.setHorizontalAlignment(JCheckBox.CENTER);
      this.checkBoxPV.setHorizontalAlignment(JCheckBox.CENTER);

      this.add(this.checkBoxS);
      this.add(this.checkBoxZ);
      this.add(this.checkBoxF5);
      this.add(this.checkBoxH);
      this.add(this.checkBoxF3);
      this.add(this.checkBoxPV);
      this.add(this.checkBoxN);
      this.add(this.checkBoxC);

      this.setData(value);

      this.doLayout();
    }

    int getData() {
      int result = 0;

      result |= this.checkBoxS.isSelected() ? 0b1000_0000 : 0;
      result |= this.checkBoxZ.isSelected() ? 0b0100_0000 : 0;
      result |= this.checkBoxF5.isSelected() ? 0b0010_0000 : 0;
      result |= this.checkBoxH.isSelected() ? 0b0001_0000 : 0;
      result |= this.checkBoxF3.isSelected() ? 0b0000_1000 : 0;
      result |= this.checkBoxPV.isSelected() ? 0b0000_0100 : 0;
      result |= this.checkBoxN.isSelected() ? 0b0000_0010 : 0;
      result |= this.checkBoxC.isSelected() ? 0b0000_0001 : 0;

      return result;
    }

    void setData(final int data) {
      this.checkBoxS.setSelected((data & 0b1000_0000) != 0);
      this.checkBoxZ.setSelected((data & 0b0100_0000) != 0);
      this.checkBoxF5.setSelected((data & 0b0010_0000) != 0);
      this.checkBoxH.setSelected((data & 0b0001_0000) != 0);
      this.checkBoxF3.setSelected((data & 0b0000_1000) != 0);
      this.checkBoxPV.setSelected((data & 0b0000_0100) != 0);
      this.checkBoxN.setSelected((data & 0b0000_0010) != 0);
      this.checkBoxC.setSelected((data & 0b0000_0001) != 0);
    }

  }


}
