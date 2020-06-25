/**
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.bus.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;

import java.util.Arrays;

import org.etools.j1939_84.bus.Packet;

/**
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 *
 */
public class DTCLampStatus {

    private LampStatus awlStatus;
    private final int[] data;
    private DiagnosticTroubleCode dtc;
    private LampStatus milStatus;
    private LampStatus plStatus;
    private LampStatus rslStatus;

    /**
     * Constructor
     *
     * @param int[]
     * the {@link Packet} to parse
     */
    public DTCLampStatus(int[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Returns the Amber Warning Lamp (AWL) Status
     *
     * @return {@link LampStatus}
     */
    public LampStatus getAmberWarningLampStatus() {
        if (awlStatus == null) {
            awlStatus = getLampStatus(0x0C, 2);
        }
        return awlStatus;
    }

    /**
     * Helper method to get one byte at the given index
     *
     * @param index
     * the index of the byte to get
     * @return one byte
     */
    protected byte getByte(int index) {
        return (byte) (getData()[index] & 0xFF);
    }

    /**
     * @return the data
     */
    public int[] getData() {
        return data;
    }

    /**
     * Returns the a link to a {@link DiagnosticTroubleCode}. If the only
     * "DTC" has an SPN of 0 or 524287, the list will be empty, but never null
     *
     * @return DTC
     */
    public DiagnosticTroubleCode getDtc() {
        if (dtc == null) {
            dtc = parseDTC();
        }
        return dtc;
    }

    /**
     * Helper method to get a {@link LampStatus}
     *
     * @param mask
     * the bit mask
     * @param shift
     * the number of bits to shift to the right
     * @return the {@link LampStatus} that corresponds to the value
     */
    private LampStatus getLampStatus(int mask, int shift) {
        int onOff = getShaveAndAHaircut(0, mask, shift);
        int flash = getShaveAndAHaircut(1, mask, shift);
        return LampStatus.getStatus(onOff, flash);
    }

    /**
     * Returns the Malfunction Indicator Lamp (MIL) Status
     *
     * @return {@link LampStatus}
     */
    public LampStatus getMalfunctionIndicatorLampStatus() {
        if (milStatus == null) {
            milStatus = getLampStatus(0xC0, 6);
        }
        return milStatus;
    }

    /**
     * Returns the Protect Lamp Status
     *
     * @return {@link LampStatus}
     */
    public LampStatus getProtectLampStatus() {
        if (plStatus == null) {
            plStatus = getLampStatus(0x03, 0);
        }
        return plStatus;
    }

    /**
     * Returns the Red Stop Lamp (RSL) Status
     *
     * @return {@link LampStatus}
     */
    public LampStatus getRedStopLampStatus() {
        if (rslStatus == null) {
            rslStatus = getLampStatus(0x30, 4);
        }
        return rslStatus;
    }

    /**
     * Helper method to get two bits at the given byte index
     *
     * @param index
     * the index of the byte that contains the bits
     * @param mask
     * the bit mask for the bits
     * @param shift
     * the number bits to shift right so the two bits are fully right
     * shifted
     * @return two bit value
     */
    private int getShaveAndAHaircut(int index, int mask, int shift) {
        return (getByte(index) & mask) >> shift;
    }

    private DiagnosticTroubleCode parseDTC() {
        return new DiagnosticTroubleCode(Arrays.copyOfRange(data, 0, 4));
    }

    @Override
    public String toString() {
        String result = "MIL: " + getMalfunctionIndicatorLampStatus() + ", RSL: "
                + getRedStopLampStatus() + ", AWL: " + getAmberWarningLampStatus() + ", PL: " + getProtectLampStatus()
                + NL;
        result += getDtc().toString();

        return result;
    }

}