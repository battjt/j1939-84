/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.bus.j1939.packets;

import org.etools.j1939_84.bus.Packet;
import org.etools.j1939_84.bus.j1939.Lookup;

/**
 * Parses the Component Identification Packet
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ComponentIdentificationPacket extends ParsedPacket {

    public static final int PGN = 65259;

    public static ComponentIdentificationPacket error(Integer address, String str) {
        return new ComponentIdentificationPacket(
                Packet.create(PGN, 0, (str + "*" + str + "*" + str + "*" + str).getBytes()));
    }

    /**
     * Holds the different parts of the component identification:
     *
     * <pre>
     * 0 - Make
     * 1 - Model
     * 2 - Serial Number
     * 3 - Unit Number
     * </pre>
     */
    final private String[] parts = new String[4];

    /**
     * Constructor
     *
     * @param packet
     *            the {@link Packet} to parse
     */
    public ComponentIdentificationPacket(Packet packet) {
        super(packet);
        String str = new String(packet.getBytes());
        String[] array = str.split("\\*", -1);
        for (int i = 0; i < 4 && i < array.length; i++) {
            parts[i] = array[i].trim();
        }
    }

    /**
     * Returns the Make, never null
     *
     * @return String
     */
    public String getMake() {
        return parts[0];
    }

    /**
     * Returns the Model, never null
     *
     * @return String
     */
    public String getModel() {
        return parts[1];
    }

    @Override
    public String getName() {
        return "Component Identification";
    }

    /**
     * Returns the Serial Number, never null
     *
     * @return String
     */
    public String getSerialNumber() {
        return parts[2];
    }

    /**
     * Returns the Unit Number, never null
     *
     * @return String
     */
    public String getUnitNumber() {
        return parts[3];
    }

    @Override
    public String toString() {
        String result = "Found " + Lookup.getAddressName(getSourceAddress()) + ": ";
        result += "Make: " + getMake() + ", ";
        result += "Model: " + getModel() + ", ";
        result += "Serial: " + getSerialNumber() + ", ";
        result += "Unit: " + getUnitNumber();
        return result;
    }
}
