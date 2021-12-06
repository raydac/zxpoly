package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.util.function.Function;

public class TzxBlockHardwareType extends AbstractTzxBlock implements InformationBlock {

  private final HwInfo[] hwInfos;

  public TzxBlockHardwareType(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.HARDWARE_TYPE.getId());
    final int items = inputStream.readByte();
    this.hwInfos = new HwInfo[items];
    for (int i = 0; i < this.hwInfos.length; i++) {
      this.hwInfos[i] = new HwInfo(inputStream);
    }
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    outputStream.write(this.hwInfos.length);
    for (final HwInfo info : this.hwInfos) {
      info.write(outputStream);
    }
  }

  public HwInfo[] getHwInfos() {
    return hwInfos;
  }

  public enum HardwareId {
    ZXSPECTRUM_16K,
    ZXSPECTRUM_48K,
    ZXSPECTRUM_48K_PLUS,
    ZXSPECTRUM_48K_ISSUE1,
    ZXSPECTRUM_128K_PLUS_SINCLAIR,
    ZXSPECTRUM_128K_PLUS_2_GREY_CASE,
    ZXSPECTRUM_128K_PLUS_2A_PLUS_3,
    TIMEX_SINCLAIR_TC_2048,
    TIMEX_SINCLAIR_TS_2068,
    PENTAGON_128,
    SAM_COUPE,
    DIDAKTIK_M,
    DIDAKTIK_GAMMA,
    ZX_80,
    ZX_81,
    ZXSPECTUM_128K_SPANISH,
    ZXSPECTUM_128K_ARABIC,
    MICRODIGITAL_TK_90X,
    MICRODIGITAL_TK_95,
    BYTE,
    ELWRO_800_3,
    ZS_SCORPION_256,
    AMSTRAD_CPC_464,
    AMSTRAD_CPC_664,
    AMSTRAD_CPC_6128,
    AMSTRAD_CPC_464_PLUS,
    AMSTRAD_CPC_6128_PLUS,
    JUPITER_ACE,
    ENTERPRISE,
    COMMODORE_64,
    COMMODORE_128,
    INVES_SPECTRUM_PLUS,
    PROFI,
    GRAND_ROM_MAX,
    KAY_1024,
    ICE_FELIX_HC_91,
    ICE_FELIX_HC_2000,
    AMATERSKE_RADIO_MISTRUM,
    QUORUM_128,
    MICROART_ATM,
    MICROART_ATM_TURBO_2,
    CHROME,
    ZX_BADALOC,
    TS_1500,
    LAMBDA,
    TK_65,
    ZX_97,
    ZX_MICRODRIVE,
    OPUS_DISCOVERY,
    MGT_DISCIPLE,
    MGT_PLUS_D,
    ROTRONICS_WAFADRIVE,
    TRDOS_BETADISK,
    BYTE_DRIVE,
    WATSFORD,
    FIZ,
    RADOFIN,
    DIDAKTIK_DISK_DRIVES,
    BSDOS_MB02,
    ZXSPECTRUM_PLUS_3_DISK_DRIVE,
    JLO_OLIGER_DISK_INTERFACE,
    TIMEX_FDD3000,
    ZEBRA_DISK_DRIVE,
    RAMEX_MILLENIA,
    LARKEN,
    KEMPSTON_DISK_INTERFACE,
    SANDY,
    ZXSPECTRUM_PLUS_3E_HARD_DISK,
    ZXATASP,
    DIV_IDE,
    ZXCF,
    SAM_RAM,
    MULTIFACE_ONE,
    MULTIFACE_128K,
    MULTIFACE_PLUS_3,
    MULTIPRINT,
    MB02_ROM_RAM_EXPANSION,
    SOFTROM,
    _1K,
    _16K,
    _48K,
    MEMORY_IN_8_16K_USED,
    CLASSIC_AY_HARDWARE_COMPATIBLE_WITH_128K_ZXS,
    FULLER_BOX_AY_SOUND_HARDWARE,
    CURRAH_MICROSPEECH,
    SPEC_DRUM,
    AY_ACB_STEREO_MELODIK,
    AY_ABC_STEREO,
    RAM_MUSIC_MACHINE,
    COVOX,
    GENERAL_SOUND,
    INTEC_ELECTRONICS_DIGITAL_INTERFACE_B8001,
    ZONX_AY,
    QUICK_SILVA_AY,
    KEMPSTON,
    CURSOR_PROTEK_AGF,
    SINCLAIR_2_LEFT,
    SINCLAIR_1_RIGHT,
    FULLER,
    AMX_MOUSE,
    KEMPSTON_MOUSE,
    TRICKSTICK,
    ZX_LIGHT_GUN,
    ZEBRA_GRAPHICS_TABLET,
    DEFENDER_LIGHT_GUN,
    ZXINTERFACE_1,
    ZXSPECTRUM_128K,
    KEMPSTON_S,
    KEMPSTON_E,
    ZXSPECTRUM_PLUS_3,
    TASMAN,
    DKTRONICS,
    HILDERBAY,
    INES_PRINTERFACE,
    ZX_LPRINT_INTERFACE_3,
    STANDARD_8255_CHIP,
    ZXPRINTER_ALPHACOM32_COMPATIBLES,
    GENERIC_PRINTER,
    EPSON_COMPATIBLE,
    PRISM_VTM_5000,
    TS_2050_OR_WESTRIGE_2050,
    RD_DIGITAL_TRACER,
    DKTRONICS_LIGHT_PEN,
    BRITISH_MICROGRAPH_PAD,
    ROMANTIC_ROBOT_VIDEOFACE,
    ZX_INTERFACE_1,
    KEYPAD_FOR_ZXSPECTRUM_128K,
    HARLEY_SYSTEMS_ADC_8_2,
    BLACKBOARD_ELECTRONICS,
    ORME_ELECTRONICS,
    WRX_HI_RES,
    G007,
    MEMOTECH,
    LAMBDA_COLOUR,
    UNKNOWN;
  }

  public enum HardwareType {
    COMPUTERS(0x00, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.ZXSPECTRUM_16K;
        case 0x01:
          return HardwareId.ZXSPECTRUM_48K_PLUS;
        case 0x02:
          return HardwareId.ZXSPECTRUM_48K_ISSUE1;
        case 0x03:
          return HardwareId.ZXSPECTRUM_128K_PLUS_SINCLAIR;
        case 0x04:
          return HardwareId.ZXSPECTRUM_128K_PLUS_2_GREY_CASE;
        case 0x05:
          return HardwareId.ZXSPECTRUM_128K_PLUS_2A_PLUS_3;
        case 0x06:
          return HardwareId.TIMEX_SINCLAIR_TC_2048;
        case 0x07:
          return HardwareId.TIMEX_SINCLAIR_TS_2068;
        case 0x08:
          return HardwareId.PENTAGON_128;
        case 0x09:
          return HardwareId.SAM_COUPE;
        case 0x0A:
          return HardwareId.DIDAKTIK_M;
        case 0x0B:
          return HardwareId.DIDAKTIK_GAMMA;
        case 0x0C:
          return HardwareId.ZX_80;
        case 0x0D:
          return HardwareId.ZX_81;
        case 0x0E:
          return HardwareId.ZXSPECTUM_128K_SPANISH;
        case 0x0F:
          return HardwareId.ZXSPECTUM_128K_ARABIC;
        case 0x10:
          return HardwareId.MICRODIGITAL_TK_90X;
        case 0x11:
          return HardwareId.MICRODIGITAL_TK_95;
        case 0x12:
          return HardwareId.BYTE;
        case 0x13:
          return HardwareId.ELWRO_800_3;
        case 0x14:
          return HardwareId.ZS_SCORPION_256;
        case 0x15:
          return HardwareId.AMSTRAD_CPC_464;
        case 0x16:
          return HardwareId.AMSTRAD_CPC_664;
        case 0x17:
          return HardwareId.AMSTRAD_CPC_6128;
        case 0x18:
          return HardwareId.AMSTRAD_CPC_464_PLUS;
        case 0x19:
          return HardwareId.AMSTRAD_CPC_6128_PLUS;
        case 0x1A:
          return HardwareId.JUPITER_ACE;
        case 0x1B:
          return HardwareId.ENTERPRISE;
        case 0x1C:
          return HardwareId.COMMODORE_64;
        case 0x1D:
          return HardwareId.COMMODORE_128;
        case 0x1E:
          return HardwareId.INVES_SPECTRUM_PLUS;
        case 0x1F:
          return HardwareId.PROFI;
        case 0x20:
          return HardwareId.GRAND_ROM_MAX;
        case 0x21:
          return HardwareId.KAY_1024;
        case 0x22:
          return HardwareId.ICE_FELIX_HC_91;
        case 0x23:
          return HardwareId.ICE_FELIX_HC_2000;
        case 0x24:
          return HardwareId.AMATERSKE_RADIO_MISTRUM;
        case 0x25:
          return HardwareId.QUORUM_128;
        case 0x26:
          return HardwareId.MICROART_ATM;
        case 0x27:
          return HardwareId.MICROART_ATM_TURBO_2;
        case 0x28:
          return HardwareId.CHROME;
        case 0x29:
          return HardwareId.ZX_BADALOC;
        case 0x2A:
          return HardwareId.TS_1500;
        case 0x2B:
          return HardwareId.LAMBDA;
        case 0x2C:
          return HardwareId.TK_65;
        case 0x2D:
          return HardwareId.ZX_97;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    EXTERNAL_STORAGE(0x01, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.ZX_MICRODRIVE;
        case 0x01:
          return HardwareId.OPUS_DISCOVERY;
        case 0x02:
          return HardwareId.MGT_DISCIPLE;
        case 0x03:
          return HardwareId.MGT_PLUS_D;
        case 0x04:
          return HardwareId.ROTRONICS_WAFADRIVE;
        case 0x05:
          return HardwareId.TRDOS_BETADISK;
        case 0x06:
          return HardwareId.BYTE_DRIVE;
        case 0x07:
          return HardwareId.WATSFORD;
        case 0x08:
          return HardwareId.FIZ;
        case 0x09:
          return HardwareId.RADOFIN;
        case 0x0A:
          return HardwareId.DIDAKTIK_DISK_DRIVES;
        case 0x0B:
          return HardwareId.BSDOS_MB02;
        case 0x0C:
          return HardwareId.ZXSPECTRUM_PLUS_3_DISK_DRIVE;
        case 0x0D:
          return HardwareId.JLO_OLIGER_DISK_INTERFACE;
        case 0x0E:
          return HardwareId.TIMEX_FDD3000;
        case 0x0F:
          return HardwareId.ZEBRA_DISK_DRIVE;
        case 0x10:
          return HardwareId.RAMEX_MILLENIA;
        case 0x11:
          return HardwareId.LARKEN;
        case 0x12:
          return HardwareId.KEMPSTON_DISK_INTERFACE;
        case 0x13:
          return HardwareId.SANDY;
        case 0x14:
          return HardwareId.ZXSPECTRUM_PLUS_3E_HARD_DISK;
        case 0x15:
          return HardwareId.ZXATASP;
        case 0x16:
          return HardwareId.DIV_IDE;
        case 0x17:
          return HardwareId.ZXCF;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    ROM_RAM_ADDONS(0x02, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.SAM_RAM;
        case 0x01:
          return HardwareId.MULTIFACE_ONE;
        case 0x02:
          return HardwareId.MULTIFACE_128K;
        case 0x03:
          return HardwareId.MULTIFACE_PLUS_3;
        case 0x04:
          return HardwareId.MULTIPRINT;
        case 0x05:
          return HardwareId.MB02_ROM_RAM_EXPANSION;
        case 0x06:
          return HardwareId.SOFTROM;
        case 0x07:
          return HardwareId._1K;
        case 0x08:
          return HardwareId._16K;
        case 0x09:
          return HardwareId._48K;
        case 0x0A:
          return HardwareId.MEMORY_IN_8_16K_USED;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    SOUND_DEVICES(0x03, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.CLASSIC_AY_HARDWARE_COMPATIBLE_WITH_128K_ZXS;
        case 0x01:
          return HardwareId.FULLER_BOX_AY_SOUND_HARDWARE;
        case 0x02:
          return HardwareId.CURRAH_MICROSPEECH;
        case 0x03:
          return HardwareId.SPEC_DRUM;
        case 0x04:
          return HardwareId.AY_ACB_STEREO_MELODIK;
        case 0x05:
          return HardwareId.AY_ABC_STEREO;
        case 0x06:
          return HardwareId.RAM_MUSIC_MACHINE;
        case 0x07:
          return HardwareId.COVOX;
        case 0x08:
          return HardwareId.GENERAL_SOUND;
        case 0x09:
          return HardwareId.INTEC_ELECTRONICS_DIGITAL_INTERFACE_B8001;
        case 0x0A:
          return HardwareId.ZONX_AY;
        case 0x0B:
          return HardwareId.QUICK_SILVA_AY;
        case 0x0C:
          return HardwareId.JUPITER_ACE;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    JOYSTICKS(0x04, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.KEMPSTON;
        case 0x01:
          return HardwareId.CURSOR_PROTEK_AGF;
        case 0x02:
          return HardwareId.SINCLAIR_2_LEFT;
        case 0x03:
          return HardwareId.SINCLAIR_1_RIGHT;
        case 0x04:
          return HardwareId.FULLER;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    MICE(0x05, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.AMX_MOUSE;
        case 0x01:
          return HardwareId.KEMPSTON_MOUSE;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    OTHER_CONTROLLERS(0x06, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.TRICKSTICK;
        case 0x01:
          return HardwareId.ZX_LIGHT_GUN;
        case 0x02:
          return HardwareId.ZEBRA_GRAPHICS_TABLET;
        case 0x03:
          return HardwareId.DEFENDER_LIGHT_GUN;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    SERIAL_PORTS(0x07, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.ZX_INTERFACE_1;
        case 0x01:
          return HardwareId.ZXSPECTRUM_128K;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    PARALLEL_PORTS(0x08, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.KEMPSTON_S;
        case 0x01:
          return HardwareId.KEMPSTON_E;
        case 0x02:
          return HardwareId.ZXSPECTRUM_PLUS_3;
        case 0x03:
          return HardwareId.TASMAN;
        case 0x04:
          return HardwareId.DKTRONICS;
        case 0x05:
          return HardwareId.HILDERBAY;
        case 0x06:
          return HardwareId.INES_PRINTERFACE;
        case 0x07:
          return HardwareId.ZX_LPRINT_INTERFACE_3;
        case 0x08:
          return HardwareId.MULTIPRINT;
        case 0x09:
          return HardwareId.OPUS_DISCOVERY;
        case 0x0A:
          return HardwareId.STANDARD_8255_CHIP;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    PRINTERS(0x09, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.ZXPRINTER_ALPHACOM32_COMPATIBLES;
        case 0x01:
          return HardwareId.GENERIC_PRINTER;
        case 0x02:
          return HardwareId.EPSON_COMPATIBLE;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    MODEMS(0x0A, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.PRISM_VTM_5000;
        case 0x01:
          return HardwareId.TS_2050_OR_WESTRIGE_2050;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    DIGITIZERS(0x0B, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.RD_DIGITAL_TRACER;
        case 0x01:
          return HardwareId.DKTRONICS_LIGHT_PEN;
        case 0x02:
          return HardwareId.BRITISH_MICROGRAPH_PAD;
        case 0x03:
          return HardwareId.ROMANTIC_ROBOT_VIDEOFACE;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    NETWORK_ADAPTERS(0x0C, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.ZX_INTERFACE_1;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    KEYBOARDS_KEYPADS(0x0D, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.KEYPAD_FOR_ZXSPECTRUM_128K;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    AD_DA_CONVERTERS(0x0E, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.HARLEY_SYSTEMS_ADC_8_2;
        case 0x01:
          return HardwareId.BLACKBOARD_ELECTRONICS;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    EPROM_PROGRAMMERS(0x0F, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.ORME_ELECTRONICS;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    GRAPHICS(0x10, id -> {
      switch (id) {
        case 0x00:
          return HardwareId.WRX_HI_RES;
        case 0x01:
          return HardwareId.G007;
        case 0x02:
          return HardwareId.MEMOTECH;
        case 0x03:
          return HardwareId.LAMBDA_COLOUR;
        default:
          return HardwareId.UNKNOWN;
      }
    }),
    UNKNOWN(-1, id -> HardwareId.UNKNOWN);

    private final int id;
    private final Function<Integer, HardwareId> decoder;

    HardwareType(final int id, final Function<Integer, HardwareId> decoder) {
      this.id = id;
      this.decoder = decoder;
    }

    public static final HardwareType findForId(final int id) {
      HardwareType result = UNKNOWN;
      for (final HardwareType t : values()) {
        if (id == t.id) {
          result = t;
          break;
        }
      }
      return result;
    }

    public HardwareId decodeHardwareId(final int hardwareId) {
      return this.decoder.apply(hardwareId);
    }
  }

  public static final class HwInfo {
    private final int type;
    private final int id;
    private final int info;

    private HwInfo(final JBBPBitInputStream inputStream) throws IOException {
      this.type = inputStream.readByte();
      this.id = inputStream.readByte();
      this.info = inputStream.readByte();
    }

    public void write(final JBBPBitOutputStream outputStream) throws IOException {
      outputStream.write(this.type);
      outputStream.write(this.id);
      outputStream.write(this.info);
    }
  }


}
