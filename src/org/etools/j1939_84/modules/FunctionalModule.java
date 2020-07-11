/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.modules;

import static org.etools.j1939_84.J1939_84.NL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.etools.j1939_84.bus.Either;
import org.etools.j1939_84.bus.Packet;
import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939_84.bus.j1939.packets.ParsedPacket;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.model.RequestResult;

/**
 * Super class for all Functional Modules
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public abstract class FunctionalModule {

    public static final String TIMEOUT_MESSAGE = "Error: Timeout - No Response.";

    private final DateTimeModule dateTimeModule;

    private J1939 j1939;

    /**
     * Constructor
     *
     * @param dateTimeModule
     *            the {@link DateTimeModule} that generates the date/time
     */
    protected FunctionalModule(DateTimeModule dateTimeModule) {
        this.dateTimeModule = dateTimeModule;
    }

    /**
     * Helper Method that will parse the results send them to the
     * {@link ResultsListener}
     *
     * @param <T>
     *            the class of the Packets
     * @param listener
     *            the {@link ResultsListener} to add the results to
     * @param results
     *            the response packets to parse
     * @return a List of the Packets
     */
    protected <T extends ParsedPacket> List<Either<T, AcknowledgmentPacket>> addToReport(ResultsListener listener,
            Stream<Either<T, AcknowledgmentPacket>> results) {
        List<Either<T, AcknowledgmentPacket>> packets = results.collect(Collectors.toList());
        if (packets.isEmpty()) {
            listener.onResult(TIMEOUT_MESSAGE);
        } else {
            List<String> strings = packets.stream()
                    // FIXME what about NACKS?
                    .flatMap(p -> p.left.stream())
                    .map(getPacketMapperFunction()).collect(Collectors.toList());
            listener.onResult(strings);
        }
        return packets;
    }

    /**
     * Helper method to generate a report
     *
     * @param <T>
     *            the class of the Packet that will received
     * @param listener
     *            the {@link ResultsListener} that will be given the results
     * @param title
     *            the Title of the report section
     * @param clazz
     *            the Class of a ParsedPacket that's expected to be returned
     *            from the vehicle
     * @param request
     *            the {@link Packet} that will be sent to solicit responses from
     *            the vehicle modules
     *
     * @return the List of Packets that were received
     */
    protected <T extends ParsedPacket> List<Either<T, AcknowledgmentPacket>> generateReport(ResultsListener listener,
            String title,
            Class<T> clazz,
            Packet request) {
        listener.onResult(getTime() + " " + title);
        listener.onResult(getTime() + " " + request.toString());
        return addToReport(listener, getJ1939().requestMultiple(clazz, request));
    }

    /**
     * Returns the {@link DateTimeModule}
     *
     * @return {@link DateTimeModule}
     */
    protected DateTimeModule getDateTimeModule() {
        return dateTimeModule;
    }

    /**
     * Returns the {@link J1939} used to communicate with vehicle
     *
     * @return {@link J1939}
     */
    protected J1939 getJ1939() {
        return j1939;
    }

    protected Function<ParsedPacket, String> getPacketMapperFunction() {
        return t -> t.getPacket().toString(getDateTimeModule().getTimeFormatter()) + NL + t.toString();
    }

    protected <T extends ParsedPacket> RequestResult<T> getPacket(String title,
            int pgn,
            Class<T> clazz,
            ResultsListener listener,
            boolean fullString,
            int address) {
        Packet request = getJ1939().createRequestPacket(pgn, address);
        listener.onResult(getTime() + " " + title);
        listener.onResult(getTime() + " " + request.toString());

        boolean retryUsed = false;
        // Try three times to get packets and ensure there's one from the module
        Optional<Either<T, AcknowledgmentPacket>> packet = Optional.empty();
        for (int i = 0; i < 3; i++) {
            // FIXME where did 5.5 s come from?
            packet = getJ1939().requestRaw(clazz, request, 5500, TimeUnit.MILLISECONDS).findFirst();
            if (packet.isPresent()) {
                // The module responded, report the results
                break;
            } else {
                retryUsed = true;
            }
        }

        if (packet.isEmpty()) {
            listener.onResult(TIMEOUT_MESSAGE);
        } else {
            ParsedPacket pp = packet.get().as(ParsedPacket.class);
            listener.onResult(pp.getPacket().toString(getDateTimeModule().getTimeFormatter()));
            if (fullString) {
                listener.onResult(pp.toString());
            }
        }
        return new RequestResult<>(retryUsed, packet.map(p -> Arrays.asList(p)).orElse(Collections.emptyList()));
    }

    /**
     * Helper method to request packets from the vehicle
     *
     * @param <T>
     *            The class of packets that will be returned
     * @param title
     *            the section title for inclusion in report
     * @param pgn
     *            the PGN that's being requested
     * @param clazz
     *            the {@link Class} of packet that will be returned
     * @param listener
     *            the {@link ResultsListener} that will be notified of the
     *            traffic
     * @param fullString
     *            true to include the full string of the results in the report;
     *            false to only include the returned raw packet in the report
     * @return the List of packets returned
     */
    protected <T extends ParsedPacket> RequestResult<T> getPacketsFromGlobal(String title,
            int pgn,
            Class<T> clazz,
            ResultsListener listener,
            boolean fullString) {
        Packet request = getJ1939().createRequestPacket(pgn, J1939.GLOBAL_ADDR);
        if (listener != null) {
            listener.onResult(getTime() + " " + title);
            listener.onResult(getTime() + " " + request.toString());
        }

        boolean retryUsed = false;

        // Try three times to get packets and ensure there's one from the engine
        List<Either<T, AcknowledgmentPacket>> packets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            // FIXME where did 5.5 s come from?
            packets = getJ1939().requestRaw(clazz, request, 5500, TimeUnit.MILLISECONDS).collect(Collectors.toList());
            if (packets.stream().findFirst().isPresent()) {
                // The something responded, report the results
                break;
            } else {
                // There was no messages. Clear the results to produce a timeout
                // message/try
                // again
                packets.clear();
                retryUsed = true;
            }
        }

        if (listener != null) {
            if (packets.isEmpty()) {
                listener.onResult(TIMEOUT_MESSAGE);
            } else {
                for (Either<T, AcknowledgmentPacket> packet : packets) {
                    ParsedPacket pp = packet.as(ParsedPacket.class);
                    listener.onResult(
                            pp.getPacket().toString(getDateTimeModule().getTimeFormatter()));
                    if (fullString) {
                        listener.onResult(pp.toString());
                    }
                }
            }
        }
        return new RequestResult<>(retryUsed, packets);
    }

    /**
     * Returns the Time formatted for the reports
     *
     * @return {@link String}
     */
    protected String getTime() {
        return getDateTimeModule().getTime();
    }

    /**
     * Sets the {@link J1939} that is used to communicate with the vehicle
     *
     * @param j1939
     *            the {@link J1939} to set
     */
    public void setJ1939(J1939 j1939) {
        this.j1939 = j1939;
    }
}
