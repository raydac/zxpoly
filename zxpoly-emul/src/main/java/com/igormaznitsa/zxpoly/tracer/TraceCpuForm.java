package com.igormaznitsa.zxpoly.tracer;

import com.igormaznitsa.z80.MemoryAccessProvider;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.z80.Z80Instruction;
import com.igormaznitsa.z80.disasm.Z80Disasm;
import com.igormaznitsa.zxpoly.MainForm;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import org.apache.commons.lang3.StringUtils;

public class TraceCpuForm extends JFrame implements MemoryAccessProvider {

  private final MainForm mainForm;
  private final Motherboard motherboard;
  private final ZxPolyModule module;
  private final int moduleIndex;
  private final JCheckBox checkBoxC;
  private final JCheckBox checkBoxF3;
  private final JCheckBox checkBoxF5;
  private final JCheckBox checkBoxH;
  private final JCheckBox checkBoxIFF1;
  private final JCheckBox checkBoxIFF2;
  private final JCheckBox checkBoxTrDos;
  private final JCheckBox checkBoxN;
  private final JCheckBox checkBoxPV;
  private final JCheckBox checkBoxS;
  private final JCheckBox checkBoxZ;
  private final JList<String> disasmList;
  private final HexValue2Field fieldAltRegA;
  private final HexValue2Field fieldAltRegB;
  private final HexValue2Field fieldAltRegC;
  private final HexValue2Field fieldAltRegD;
  private final HexValue2Field fieldAltRegE;
  private final HexValue2Field fieldAltRegF;
  private final HexValue2Field fieldAltRegH;
  private final HexValue2Field fieldAltRegL;
  private final HexValue2Field fieldI;
  private final HexValue2Field fieldIM;
  private final HexValue4Field fieldIX;
  private final HexValue4Field fieldIY;
  private final HexValue4Field fieldPC;
  private final HexValue2Field fieldR;
  private final HexValue2Field fieldRegA;
  private final HexValue2Field fieldRegB;
  private final HexValue2Field fieldRegC;
  private final HexValue2Field fieldRegD;
  private final HexValue2Field fieldRegE;
  private final HexValue2Field fieldRegF;
  private final HexValue2Field fieldRegH;
  private final HexValue2Field fieldRegL;
  private final HexValue2Field field7FFD;
  private final HexValue4Field fieldSP;
  private final JPanel panelAltSet;
  private final JPanel panelOthers;
  private final JPanel panelFlags;
  private final JPanel panelMainSet;
  private final StringBuilder buffer = new StringBuilder(32);
  private boolean changeEnabled = true;

  public TraceCpuForm(final MainForm mainForm, final Motherboard motherboard,
                      final int moduleIndex) {
    super(mainForm.getGraphicsConfiguration());

    this.setLocation(mainForm.getLocation());

    this.mainForm = mainForm;
    this.motherboard = motherboard;
    this.moduleIndex = moduleIndex;
    this.module = motherboard.getModules()[this.moduleIndex];

    this.checkBoxC = new JCheckBox();
    this.checkBoxF3 = new JCheckBox();
    this.checkBoxF5 = new JCheckBox();
    this.checkBoxH = new JCheckBox();
    this.checkBoxIFF1 = new JCheckBox("IFF1");
    this.checkBoxIFF2 = new JCheckBox("IFF2");
    this.checkBoxTrDos = new JCheckBox("TrDos");
    this.checkBoxN = new JCheckBox();
    this.checkBoxPV = new JCheckBox();
    this.checkBoxS = new JCheckBox();
    this.checkBoxZ = new JCheckBox();
    this.disasmList = new JList();
    this.disasmList.setPreferredSize(new Dimension(
        this.disasmList.getFontMetrics(this.disasmList.getFont())
            .stringWidth(StringUtils.repeat('@', 24)), 128));

    this.fieldAltRegA = new HexValue2Field();
    this.fieldAltRegB = new HexValue2Field();
    this.fieldAltRegC = new HexValue2Field();
    this.fieldAltRegD = new HexValue2Field();
    this.fieldAltRegE = new HexValue2Field();
    this.fieldAltRegF = new HexValue2Field();
    this.fieldAltRegH = new HexValue2Field();
    this.fieldAltRegL = new HexValue2Field();
    this.fieldIM = new HexValue2Field();
    this.fieldI = new HexValue2Field();
    this.fieldIX = new HexValue4Field();
    this.fieldIY = new HexValue4Field();
    this.fieldPC = new HexValue4Field();
    this.fieldR = new HexValue2Field();
    this.fieldRegA = new HexValue2Field();
    this.fieldRegB = new HexValue2Field();
    this.fieldRegC = new HexValue2Field();
    this.fieldRegD = new HexValue2Field();
    this.fieldRegE = new HexValue2Field();
    this.fieldRegF = new HexValue2Field();
    this.fieldRegH = new HexValue2Field();
    this.fieldRegL = new HexValue2Field();
    this.field7FFD = new HexValue2Field();
    this.fieldSP = new HexValue4Field();

    this.panelMainSet = this.makePanelMainSet();
    this.panelAltSet = this.makePanelAltSet();
    this.panelOthers = this.makePanelOthers();
    this.panelFlags = this.makePanelFlags();

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.ipadx = 0;
    gbc.ipady = 0;
    gbc.weightx = 1;
    gbc.weighty = 1;

    final JPanel disasmPanel = new JPanel(new BorderLayout());
    disasmPanel.add(this.disasmList, BorderLayout.CENTER);
    disasmPanel.setBorder(new TitledBorder("Disassembler"));
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 4;
    gbc.weightx = 1000;
    gbc.weighty = 1000;
    gbc.fill = GridBagConstraints.BOTH;
    mainPanel.add(disasmPanel, gbc);
    gbc.weightx = 1;
    gbc.weighty = 1;

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    mainPanel.add(this.panelFlags, gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    mainPanel.add(this.panelMainSet, gbc);

    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    mainPanel.add(this.panelAltSet, gbc);

    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    mainPanel.add(this.panelOthers, gbc);

    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.weighty = 1000;
    gbc.fill = GridBagConstraints.BOTH;
    mainPanel.add(Box.createVerticalGlue(), gbc);

    final JButton buttonMemory = new JButton("Change memory");
    buttonMemory.addActionListener(e ->
        new MemoryDialog(this, true, this.module).setVisible(true));

    final JButton buttonSetIFF1 = new JButton("Set IFF1");
    buttonSetIFF1.addActionListener(e ->
        this.module.getCpu().setIFF(true, true));

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonsPanel.add(buttonMemory);
    buttonsPanel.add(buttonSetIFF1);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 3;
    gbc.gridheight = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    mainPanel.add(buttonsPanel, gbc);

    final JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
    contentPanel.add(mainPanel, BorderLayout.CENTER);

    this.setTitle("CPU Module#" + this.moduleIndex);
    this.setResizable(true);
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    this.setContentPane(contentPanel);

    this.pack();

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowActivated(WindowEvent e) {
        TraceCpuForm.this.mainForm.onTracerActivated(TraceCpuForm.this);
      }

      @Override
      public void windowClosed(WindowEvent e) {
        TraceCpuForm.this.mainForm.onTracerDeactivated(TraceCpuForm.this);
      }
    });


  }

  private static JLabel makeCntrLabel(final String text) {
    final JLabel result = new JLabel(text);
    result.setHorizontalAlignment(JLabel.CENTER);
    return result;
  }

  private static void int2hex4(final StringBuilder buffer, final int value) {
    final String str = Integer.toHexString(value).toUpperCase(Locale.ENGLISH);
    if (str.length() < 4) {
      for (int i = 0; i < 4 - str.length(); i++) {
        buffer.append('0');
      }
    }
    buffer.append(str);
  }

  private JPanel makePanelFlags() {
    final JPanel result = new JPanel(new GridLayout(2, 8, 8, 8));

    result.setBorder(new TitledBorder("Flags"));

    result.add(makeCntrLabel("S"));
    result.add(makeCntrLabel("Z"));
    result.add(makeCntrLabel("F5"));
    result.add(makeCntrLabel("H"));
    result.add(makeCntrLabel("F3"));
    result.add(makeCntrLabel("P/V"));
    result.add(makeCntrLabel("N"));
    result.add(makeCntrLabel("C"));

    this.checkBoxS.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxC.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxF3.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxF5.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxH.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxN.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxZ.setHorizontalAlignment(JCheckBox.CENTER);
    this.checkBoxPV.setHorizontalAlignment(JCheckBox.CENTER);

    result.add(this.checkBoxS);
    result.add(this.checkBoxZ);
    result.add(this.checkBoxF5);
    result.add(this.checkBoxH);
    result.add(this.checkBoxF3);
    result.add(this.checkBoxPV);
    result.add(this.checkBoxN);
    result.add(this.checkBoxC);

    result.doLayout();

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

    result.add(new JLabel("R:"), gbc);
    result.add(new JLabel("I:"), gbc);
    result.add(new JLabel("IM:"), gbc);
    result.add(new JLabel("7FFD:"), gbc);

    gbc.gridx = 3;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 0, 0);

    result.add(this.fieldR, gbc);
    result.add(this.fieldI, gbc);
    result.add(this.fieldI, gbc);
    result.add(this.fieldIM, gbc);
    result.add(this.field7FFD, gbc);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    final JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flagsPanel.add(this.checkBoxIFF1);
    flagsPanel.add(this.checkBoxIFF2);
    flagsPanel.add(this.checkBoxTrDos);

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
    result.add(new JLabel("B':"), gbc);
    result.add(new JLabel("C':"), gbc);
    result.add(new JLabel("D':"), gbc);
    result.add(new JLabel("E':"), gbc);
    result.add(new JLabel("H':"), gbc);
    result.add(new JLabel("L':"), gbc);

    gbc.gridx = 1;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(this.fieldAltRegA, gbc);
    result.add(this.fieldAltRegB, gbc);
    result.add(this.fieldAltRegC, gbc);
    result.add(this.fieldAltRegD, gbc);
    result.add(this.fieldAltRegE, gbc);
    result.add(this.fieldAltRegH, gbc);
    result.add(this.fieldAltRegL, gbc);
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
    result.add(new JLabel("B:"), gbc);
    result.add(new JLabel("C:"), gbc);
    result.add(new JLabel("D:"), gbc);
    result.add(new JLabel("E:"), gbc);
    result.add(new JLabel("H:"), gbc);
    result.add(new JLabel("L:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(this.fieldRegA, gbc);
    result.add(this.fieldRegB, gbc);
    result.add(this.fieldRegC, gbc);
    result.add(this.fieldRegD, gbc);
    result.add(this.fieldRegE, gbc);
    result.add(this.fieldRegH, gbc);
    result.add(this.fieldRegL, gbc);
    return result;
  }

  public int getModuleIndex() {
    return this.moduleIndex;
  }

  public void refresh() {
    final int pc = this.module.getCpu().getPC();

    if (this.module.isActiveRegistersAsMemorySource()) {
      final DefaultListModel<String> model = new DefaultListModel<>();
      model.addElement("Registers as data source!");
      this.disasmList.setModel(model);
    } else {
      final int lines = this.disasmList.getHeight() / this.disasmList.getFont().getSize();

      final List<Z80Instruction> instructions = Z80Disasm.decodeList(this, null, pc, lines);
      final DefaultListModel<String> model = new DefaultListModel<>();
      int address = pc;

      model.addElement(this.makeCodeLine(this, pc - 9, 8));

      for (final Z80Instruction i : instructions) {
        model.addElement(this.makeInstructionLine(i, address));
        address += i == null ? 1 : i.getLength();
      }

      this.disasmList.setModel(model);
      this.disasmList.setSelectedIndex(1);

    }
    this.refreshRegisterValue();
  }

  private String makeCodeLine(
      final MemoryAccessProvider provider,
      final int addr,
      final int bytes
  ) {
    final StringBuilder result = new StringBuilder();
    final String addrAsHex = Integer.toHexString(addr).toUpperCase(Locale.ENGLISH);
    switch (addrAsHex.length()) {
      case 3:
        result.append('0');
        break;
      case 2:
        result.append("00");
        break;
      case 1:
        result.append("000");
        break;
    }
    result.append(addrAsHex);
    for (int i = 0; i < bytes; i++) {
      result.append(' ')
          .append(Integer.toHexString(provider.readAddress(addr + i) & 0xFF)
              .toUpperCase(Locale.ENGLISH));
    }
    return result.toString();
  }

  private String makeInstructionLine(final Z80Instruction instruction, final int address) {
    this.buffer.setLength(0);

    int2hex4(this.buffer, address);
    this.buffer.append("    ");

    if (instruction == null) {
      this.buffer.append("---");
    } else {
      this.buffer.append(instruction.decode(this, address, address));
    }

    return this.buffer.toString();
  }

  private void setEnableForComponentsOfPanel(final JPanel panel, final boolean flag) {
    for (final Component c : panel.getComponents()) {
      if (c instanceof AbstractHexValueField) {
        ((AbstractHexValueField) c).setEditable(flag);
      } else if (c instanceof JCheckBox) {
        c.setEnabled(flag);
      } else if (c instanceof JPanel) {
        this.setEnableForComponentsOfPanel((JPanel) c, flag);
      }

    }
  }

  private void refreshRegisterValue() {
    if (this.changeEnabled) {
      this.changeEnabled = false;
      setEnableForComponentsOfPanel(this.panelAltSet, this.changeEnabled);
      setEnableForComponentsOfPanel(this.panelOthers, this.changeEnabled);
      setEnableForComponentsOfPanel(this.panelFlags, this.changeEnabled);
      setEnableForComponentsOfPanel(this.panelMainSet, this.changeEnabled);
    }

    final Z80 cpu = this.module.getCpu();
    this.fieldPC.setValue(cpu.getPC());
    this.fieldSP.setValue(cpu.getSP());
    this.fieldIX.setValue(cpu.getRegister(Z80.REG_IX));
    this.fieldIY.setValue(cpu.getRegister(Z80.REG_IY));
    this.fieldI.setValue(cpu.getRegister(Z80.REG_I));
    this.fieldIM.setValue(cpu.getIM());
    this.fieldR.setValue(cpu.getRegister(Z80.REG_R));

    final int regf = cpu.getRegister(Z80.REG_F);

    this.checkBoxC.setSelected((regf & Z80.FLAG_C) != 0);
    this.checkBoxF3.setSelected((regf & Z80.FLAG_X) != 0);
    this.checkBoxF5.setSelected((regf & Z80.FLAG_Y) != 0);
    this.checkBoxH.setSelected((regf & Z80.FLAG_H) != 0);
    this.checkBoxN.setSelected((regf & Z80.FLAG_N) != 0);
    this.checkBoxPV.setSelected((regf & Z80.FLAG_PV) != 0);
    this.checkBoxS.setSelected((regf & Z80.FLAG_S) != 0);
    this.checkBoxZ.setSelected((regf & Z80.FLAG_Z) != 0);

    this.checkBoxIFF1.setSelected(cpu.isIFF1());
    this.checkBoxIFF2.setSelected(cpu.isIFF2());
    this.checkBoxTrDos.setSelected(this.module.isTrdosActive());

    this.field7FFD.setValue(this.module.read7FFD());

    this.fieldRegA.setValue(cpu.getRegister(Z80.REG_A));
    this.fieldRegF.setValue(cpu.getRegister(Z80.REG_F));
    this.fieldRegB.setValue(cpu.getRegister(Z80.REG_B));
    this.fieldRegC.setValue(cpu.getRegister(Z80.REG_C));
    this.fieldRegD.setValue(cpu.getRegister(Z80.REG_D));
    this.fieldRegE.setValue(cpu.getRegister(Z80.REG_E));
    this.fieldRegH.setValue(cpu.getRegister(Z80.REG_H));
    this.fieldRegL.setValue(cpu.getRegister(Z80.REG_L));

    this.fieldAltRegA.setValue(cpu.getRegister(Z80.REG_A, true));
    this.fieldAltRegF.setValue(cpu.getRegister(Z80.REG_F, true));
    this.fieldAltRegB.setValue(cpu.getRegister(Z80.REG_B, true));
    this.fieldAltRegC.setValue(cpu.getRegister(Z80.REG_C, true));
    this.fieldAltRegD.setValue(cpu.getRegister(Z80.REG_D, true));
    this.fieldAltRegE.setValue(cpu.getRegister(Z80.REG_E, true));
    this.fieldAltRegH.setValue(cpu.getRegister(Z80.REG_H, true));
    this.fieldAltRegL.setValue(cpu.getRegister(Z80.REG_L, true));

  }


  @Override
  public byte readAddress(final int address) {
    return this.module.readMemory(this.module.getCpu(), 0, address & 0xFFFF, false, false);
  }

}
