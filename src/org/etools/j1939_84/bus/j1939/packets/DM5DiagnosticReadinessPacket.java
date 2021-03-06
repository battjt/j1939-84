/*
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.bus.j1939.packets;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.etools.j1939_84.bus.Packet;

/**
 * The {@link ParsedPacket} for Diagnostic Readiness #1 (DM5)
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 */
public class DM5DiagnosticReadinessPacket extends DiagnosticReadinessPacket {

    public static final int PGN = 65230; // 0xFECE
    private static final List<Byte> obdValues = Arrays.asList(new Byte[] { 0x13, 0x14, 0x22, 0x23 });
    private final byte activeCount;
    private final byte obdCompliance;
    private final byte previousCount;

    public DM5DiagnosticReadinessPacket(Packet packet) {
        super(packet);
        activeCount = getByte(0);
        previousCount = getByte(1);
        obdCompliance = getByte(2);
    }

    public static DM5DiagnosticReadinessPacket create(int sourceAddress,
                                                      int activeCount,
                                                      int previouslyActiveCount,
                                                      int obdCompliance) {
        return create(sourceAddress, activeCount, previouslyActiveCount, obdCompliance, List.of(), List.of());
    }

    public static DM5DiagnosticReadinessPacket create(int sourceAddress,
                                                      int activeCount,
                                                      int previouslyActiveCount,
                                                      int obdCompliance,
                                                      List<CompositeSystem> supportedSystems,
                                                      List<CompositeSystem> completeSystems) {
        int[] data = new int[8];
        data[0] = (byte) activeCount;
        data[1] = (byte) previouslyActiveCount;
        data[2] = (byte) obdCompliance;

        for (CompositeSystem systemId : CompositeSystem.values()) {
            boolean isEnabled = supportedSystems.contains(systemId);
            boolean isComplete = completeSystems.contains(systemId);
            populateData(systemId, isComplete, isEnabled, data);
        }

        return new DM5DiagnosticReadinessPacket(Packet.create(PGN, sourceAddress, data));
    }

    private static String lookupObdCompliance(byte value) {
        switch (value) {
            case 1:
                return "OBD II";
            case 2:
                return "OBD";
            case 3:
                return "OBD and OBD II";
            case 4:
                return "OBD I";
            case 5:
                return "Not intended to meet OBD II requirements";
            case 6:
                return "EOBD";
            case 7:
                return "EOBD and OBD II";
            case 8:
                return "EOBD and OBD";
            case 9:
                return "EOBD, OBD and OBD II";
            case 10:
                return "JOBD";
            case 11:
                return "JOBD and OBD II";
            case 12:
                return "JOBD and EOBD";
            case 13:
                return "JOBD, EOBD and OBD II";
            case 14:
                return "Heavy Duty Vehicles (EURO IV) B1";
            case 15:
                return "Heavy Duty Vehicles (EURO V) B2";
            case 16:
                return "Heavy Duty Vehicles (EURO EEC) C (gas engines)";
            case 17:
                return "EMD";
            case 18:
                return "EMD+";
            case 19:
                return "HD OBD P";
            case 20:
                return "HD OBD";
            case 21:
                return "WWH OBD";
            case 22:
                return "OBD II";
            case 23:
                return "HD EOBD";

            case 25:
                return "OBD-M (SI-SD/I)";
            case 26:
                return "EURO VI";

            case 34:
                return "OBD, OBD II, HD OBD";
            case 35:
                return "OBD, OBD II, HD OBD P";

            case (byte) 251:
                return "value 251";
            case (byte) 252:
                return "value 252";
            case (byte) 253:
                return "value 253";
            case (byte) 254:
                return "Error";
            case (byte) 255:
                return "Not available";

            default:
                return "Reserved for SAE/Unknown";
        }
    }

    /**
     * Returns the number of active DTCs
     *
     * @return byte
     */
    public byte getActiveCodeCount() {
        return activeCount;
    }

    @Override
    public String getName() {
        return "DM5";
    }

    @Override
    public String toString() {
        byte obd = getOBDCompliance();
        return getStringPrefix() + "OBD Compliance: " + lookupObdCompliance(obd) + " (" + (obd & 0xFF) + "), "
                + "Active Codes: " + getValueWithUnits(getActiveCodeCount(), null) + ", Previously Active Codes: "
                + getValueWithUnits(getPreviouslyActiveCodeCount(), null);
    }

    /**
     * Returns the value of the OBD Compliance
     *
     * @return byte
     */
    public byte getOBDCompliance() {
        return obdCompliance;
    }

    /**
     * Returns the number of previously active DTCs
     *
     * @return byte
     */
    public byte getPreviouslyActiveCodeCount() {
        return previousCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getActiveCodeCount(), getPreviouslyActiveCodeCount(), getOBDCompliance(), super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof DM5DiagnosticReadinessPacket)) {
            return false;
        }

        DM5DiagnosticReadinessPacket that = (DM5DiagnosticReadinessPacket) obj;
        return getActiveCodeCount() == that.getActiveCodeCount()
                && getPreviouslyActiveCodeCount() == that.getPreviouslyActiveCodeCount()
                && getOBDCompliance() == that.getOBDCompliance() && super.equals(obj);
    }

    /**
     * Returns true if this module reported that it supports HD OBD
     *
     * @return boolean
     */
    public boolean isHdObd() {
        return getOBDCompliance() == 19 || getOBDCompliance() == 20;
    }

    public boolean isObd() {
        return obdValues.contains(getOBDCompliance());
    }

}
