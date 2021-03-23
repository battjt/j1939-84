/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.bus;

import static org.etools.j1939_84.bus.RP1210Library.BLOCKING_NONE;
import static org.etools.j1939_84.bus.RP1210Library.CLAIM_BLOCK_UNTIL_DONE;
import static org.etools.j1939_84.bus.RP1210Library.CMD_ECHO_TRANSMITTED_MESSAGES;
import static org.etools.j1939_84.bus.RP1210Library.CMD_GET_PROTOCOL_CONNECTION_SPEED;
import static org.etools.j1939_84.bus.RP1210Library.CMD_PROTECT_J1939_ADDRESS;
import static org.etools.j1939_84.bus.RP1210Library.CMD_SET_ALL_FILTERS_STATES_TO_PASS;
import static org.etools.j1939_84.bus.RP1210Library.ECHO_ON;
import static org.etools.j1939_84.bus.RP1210Library.NOTIFICATION_NONE;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.etools.j1939_84.J1939_84;

/**
 * The RP1210 implementation of a {@link Bus}
 *
 * @author Joe Batt (joe@soliddesign.net)
 *
 */
public class RP1210Bus implements Bus {

    /**
     * The source address for this tool
     */
    private final int address;

    /**
     * The id of this client for the {@link RP1210Library}
     */
    private final short clientId;

    /**
     * The byte array used to read data from the {@link RP1210Library}
     */
    private final byte[] data = new byte[2048];

    /**
     * The thread pool used for polling
     */
    private final ScheduledExecutorService exec;

    /**
     * The {@link Logger} for errors
     */
    private final Logger logger;

    /**
     * The {@link RP1210Library}
     */
    private final RP1210Library rp1210Library;

    /**
     * From the adapter .ini file.
     */
    final private long timeStampWeight;

    /**
     * The Queue of {@link Packet}s
     */
    private MultiQueue<Packet> queue = new MultiQueue<>();

    /**
     * An adjustment offset for the adapter time to system time. Adapters don't have batteries, so their clocks are
     * always wrong.
     */
    private long timeStampStartMicroseconds;

    /**
     * Constructor
     *
     * @param  adapter
     *                          the {@link Adapter} thats connected to the vehicle
     * @param  address
     *                          the address of this branch on the bus
     * @throws BusException
     *                          if there is a problem connecting to the adapter
     */
    public RP1210Bus(Adapter adapter, int address, boolean appPacketize) throws BusException {
        this(RP1210Library.load(adapter),
             Executors.newSingleThreadScheduledExecutor(),
             new MultiQueue<>(),
             adapter,
             address,
             appPacketize,
             J1939_84.getLogger());
    }

    /**
     * Constructor exposed for testing
     *
     * @param  rp1210Library
     *                           the {@link RP1210Library} that connects to the adapter
     *
     * @param  exec
     *                           the {@link ScheduledExecutorService} that will execute tasks
     *
     * @param  adapter
     *                           the {@link Adapter} thats connected to the vehicle
     *
     * @param  address
     *                           the source address of this branch on the bus
     *
     * @param  logger
     *                           the {@link Logger} for logging errors
     *
     * @param  queue
     *                           the {@link Packet} for logging errors
     *
     * @throws BusException
     *                           if there is a problem connecting to the adapter
     *
     */
    public RP1210Bus(RP1210Library rp1210Library,
                     ScheduledExecutorService exec,
                     MultiQueue<Packet> queue,
                     Adapter adapter,
                     int address,
                     boolean appPacketize,
                     Logger logger) throws BusException {
        this.rp1210Library = rp1210Library;
        this.exec = exec;
        this.queue = queue;
        this.address = address;
        this.logger = logger;
        timeStampWeight = adapter.getTimeStampWeight();
        timeStampStartMicroseconds = 0;

        clientId = rp1210Library
                                .RP1210_ClientConnect(0,
                                                      adapter.getDeviceId(),
                                                      "J1939:Baud=Auto",
                                                      0,
                                                      0,
                                                      (short) (appPacketize ? 1 : 0));
        verify(clientId);
        try {
            sendCommand(CMD_PROTECT_J1939_ADDRESS,
                        new byte[] { (byte) address, 0, 0, (byte) 0xE0, (byte) 0xFF, 0,
                                (byte) 0x81, 0, 0, CLAIM_BLOCK_UNTIL_DONE });
            sendCommand(CMD_ECHO_TRANSMITTED_MESSAGES, ECHO_ON);
            sendCommand(CMD_SET_ALL_FILTERS_STATES_TO_PASS);

            this.exec.scheduleAtFixedRate(() -> poll(), 1, 1, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            stop();
            throw new BusException("Failed to configure adapter.", e);
        }
    }

    @Override
    public void close() {
        queue.close();
        queue = new MultiQueue<>();
    }

    @Override
    public Stream<Packet> duplicate(Stream<Packet> stream, int time, TimeUnit unit) {
        return queue.duplicate(stream, time, unit);
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getConnectionSpeed() throws BusException {
        byte[] bytes = new byte[17];
        sendCommand(CMD_GET_PROTOCOL_CONNECTION_SPEED, bytes);
        String result = new String(bytes, StandardCharsets.UTF_8).trim();
        return Integer.parseInt(result);
    }

    @Override
    public Stream<Packet> read(long timeout, TimeUnit unit) throws BusException {
        return queue.stream(timeout, unit);
    }

    /**
     * Reset stream timeout for stream created with bus.read(). To be used in a
     * stream call like peek, map or forEach.
     */
    @Override
    public void resetTimeout(Stream<Packet> stream, int time, TimeUnit unit) {
        queue.resetTimeout(stream, time, unit);
    }

    @Override
    public Packet send(Packet tx) throws BusException {
        byte[] data = encode(tx);
        try (Stream<Packet> stream = read(1000, TimeUnit.MILLISECONDS)) {
            // rp1210 libraries may not be thread safe
            short rtn = exec.submit(() -> rp1210Library.RP1210_SendMessage(clientId,
                                                                           data,
                                                                           (short) data.length,
                                                                           NOTIFICATION_NONE,
                                                                           BLOCKING_NONE))
                            .get();
            verify(rtn);
            int id = tx.getId(0xFFFF);
            int source = tx.getSource();
            return stream
                         .filter(rx -> rx.isTransmitted() && id == rx.getId(0xFFFF) && rx.getSource() == source)
                         .findFirst()
                         .orElseThrow(() -> new BusException("Failed to send: " + tx));
        } catch (Throwable e) {
            throw e instanceof BusException ? (BusException) e : new BusException("Failed to send: " + tx, e);
        }
    }

    /**
     * Decodes the given byte array into a {@link Packet}
     *
     * @param  data
     *                    the byte array to decode
     * @param  length
     *                    the total length of the payload data
     * @return        {@link Packet}
     */
    private Packet decode(byte[] data, int length) {
        // only 32 bits used, but to get a u32, use a s64.
        long timestamp = (0xFF000000L & data[0] << 24) | (0xFF0000L & data[1] << 16) | (0xFF00L & data[2] << 8)
                | (0xFFL & data[3]);
        // data[4] is echo
        int echoed = data[4];
        int pgn = ((data[7] & 0xFF) << 16) | ((data[6] & 0xFF) << 8) | (data[5] & 0xFF);
        int priority = data[8] & 0x07;
        int source = data[9] & 0xFF;
        if (pgn < 0xF000) {
            int destination = data[10];
            pgn = pgn | (destination & 0xFF);
        }

        Instant time = adapterTimestampToInstant(timestamp);
        final long diff = Math.abs(time.toEpochMilli() - Instant.now().toEpochMilli());
        if (diff > 1000) {
            timeStampStartMicroseconds = 1000 * System.currentTimeMillis() - timestamp * timeStampWeight;
            getLogger().log(Level.INFO, String.format("adapter time offset: %,d µs", timeStampStartMicroseconds));
            time = adapterTimestampToInstant(timestamp);
        }
        return Packet.create(LocalDateTime.ofInstant(time, ZoneId.systemDefault()),
                             priority,
                             pgn,
                             source,
                             echoed != 0,
                             Arrays.copyOfRange(data, 11, length));
    }

    /** Apply adapter time offset. */
    private Instant adapterTimestampToInstant(long timestamp) {
        long microseconds = timestamp * timeStampWeight + timeStampStartMicroseconds;
        Instant time = Instant.ofEpochSecond( /* seconds */ microseconds / 1000000,
                                             /* nanoseconds */(microseconds % 1000000) * 1000);
        return time;
    }

    /**
     * Transforms the given {@link Packet} into a byte array so it can be sent
     * to the vehicle bus
     *
     * @param  packet
     *                    the {@link Packet} to encode
     * @return        a byte array of the encoded packet
     */
    private byte[] encode(Packet packet) {
        byte[] buf = new byte[packet.getLength() + 6];
        int id = packet.getId(0xFFFF);
        buf[0] = (byte) id;
        buf[1] = (byte) (id >> 8);
        buf[2] = (byte) (id >> 16);
        buf[3] = (byte) packet.getPriority();
        buf[4] = (byte) packet.getSource();
        buf[5] = (id < 0xF000) ? (byte) (id & 0xFF) : 0;
        for (int i = 0; i < packet.getLength(); i++) {
            buf[6 + i] = (byte) packet.get(i);
        }
        return buf;
    }

    private Logger getLogger() {
        return logger;
    }

    /**
     * Checks the {@link RP1210Library} for any incoming messages. Any incoming
     * messages are decoded and added to the queue
     */
    private void poll() {
        try {
            while (true) {
                short rtn = rp1210Library.RP1210_ReadMessage(clientId, data, (short) data.length, BLOCKING_NONE);
                if (rtn > 0) {
                    Packet packet = decode(data, rtn);
                    if (packet.getSource() == getAddress() && !packet.isTransmitted()) {
                        getLogger().log(Level.WARNING, "Another ECU is using this address: " + packet);
                    }
                    getLogger().log(Level.FINE, packet.toTimeString());
                    queue.add(packet);
                } else if (rtn == -RP1210Library.ERR_RX_QUEUE_FULL) {
                    // RX queue full, remedy is to reread.
                    byte[] buffer = new byte[256];
                    rp1210Library.RP1210_GetErrorMsg((short) Math.abs(rtn), buffer);
                    getLogger().log(Level.SEVERE,
                                    "Error (" + rtn + "): " + new String(buffer, StandardCharsets.UTF_8).trim());
                } else {
                    verify(rtn);
                    break;
                }
            }
        } catch (BusException e) {
            getLogger().log(Level.SEVERE, "Failed to read RP1210", e);
        }
    }

    /**
     * Helper method to send a command to the library
     *
     * @param  command
     *                          the command to send
     * @param  data
     *                          the data to include in the command
     * @throws BusException
     *                          if there result of the command was unsuccessful
     */
    private void sendCommand(short command, byte... data) throws BusException {
        short rtn = rp1210Library.RP1210_SendCommand(command, clientId, data, (short) data.length);
        verify(rtn);
    }

    /**
     * Disconnects from the {@link RP1210Library}
     *
     * @throws BusException
     *                          if there is a problem disconnecting
     */
    public void stop() throws BusException {
        try {
            if (rp1210Library != null) {
                exec.submit(() -> rp1210Library.RP1210_ClientDisconnect(clientId)).get();
            }
        } catch (Exception e) {
            throw new BusException("Failed to stop RP1210.", e);
        } finally {
            exec.shutdown();
        }
    }

    /**
     * Checks the code returned from calls to the adapter to determine if it's
     * an error.
     *
     * @param  rtnCode
     *                          the return code to check
     * @throws BusException
     *                          if the return code is an error
     */
    private void verify(short rtnCode) throws BusException {
        if (rtnCode > 127 || rtnCode < 0) {
            rtnCode = (short) Math.abs(rtnCode);
            byte[] buffer = new byte[256];
            rp1210Library.RP1210_GetErrorMsg(rtnCode, buffer);
            throw new BusException("Error (" + rtnCode + "): " + new String(buffer, StandardCharsets.UTF_8).trim());
        }
    }
}
