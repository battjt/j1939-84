/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.modules;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.bus.j1939.J1939.GLOBAL_ADDR;
import static org.etools.j1939_84.bus.j1939.J1939.REQUEST_PGN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.etools.j1939_84.bus.Either;
import org.etools.j1939_84.bus.Packet;
import org.etools.j1939_84.bus.j1939.BusResult;
import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939_84.bus.j1939.packets.DM11ClearActiveDTCsPacket;
import org.etools.j1939_84.bus.j1939.packets.DM12MILOnEmissionDTCPacket;
import org.etools.j1939_84.bus.j1939.packets.DM23PreviouslyMILOnEmissionDTCPacket;
import org.etools.j1939_84.bus.j1939.packets.DM25ExpandedFreezeFrame;
import org.etools.j1939_84.bus.j1939.packets.DM28PermanentEmissionDTCPacket;
import org.etools.j1939_84.bus.j1939.packets.DM2PreviouslyActiveDTC;
import org.etools.j1939_84.bus.j1939.packets.DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime;
import org.etools.j1939_84.bus.j1939.packets.DM6PendingEmissionDTCPacket;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.RequestResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for the {@link DTCModule} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
                    justification = "The values returned are properly ignored on verify statements.")
@RunWith(MockitoJUnitRunner.class)
public class DTCModuleTest {

    /**
     * The Bus address of the tool for testing purposes
     */
    private static final int BUS_ADDR = 0xA5;

    private DTCModule instance;

    @Mock
    private J1939 j1939;

    @Before
    public void setUp() throws Exception {
        instance = new DTCModule(new TestDateTimeModule());
        instance.setJ1939(j1939);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(j1939);
    }

    @Test
    public void testReportDM28DestinationSpecificWithDTCs() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        DM28PermanentEmissionDTCPacket packet1 = new DM28PermanentEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));

        when(j1939.requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM28 Request" + NL;
        expected += "10:15:30.000 18EA00A5 80 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FD8000 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM28 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM28PermanentEmissionDTCPacket> expectedResult = new RequestResult<>(
                false, Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM28(listener, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28DestinationSpecificWithNoResponses() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x17, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x17)).thenReturn(requestPacket);

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM28 Request" + NL;
        expected += "10:15:30.000 18EA17A5 80 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM28PermanentEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM28(listener, 0x17));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x17);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28Global() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM28PermanentEmissionDTCPacket packet1 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM28PermanentEmissionDTCPacket packet2 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM28PermanentEmissionDTCPacket packet3 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM28 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 80 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FD8000 00 00 00 00 00 00 00 00" + NL;
        expected += "DM28 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FD8017 00 00 00 00 00 00 00 00" + NL;
        expected += "DM28 from Instrument Cluster #1 (23): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FD8021 00 00 00 00 00 00 00 00" + NL;
        expected += "DM28 from Body Controller (33): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM28PermanentEmissionDTCPacket> expectedResult = new RequestResult<>(
                false, Arrays.asList(packet1, packet2, packet3), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM28(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28GlobalWithDTCs() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM28PermanentEmissionDTCPacket packet1 = new DM28PermanentEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));

        when(j1939.requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM28 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 80 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FD8000 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM28 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM28PermanentEmissionDTCPacket> expectedResult = new RequestResult<>(
                false, Arrays.asList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM28(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testReportDM28GlobalWithNoResponses() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        String expected = "";
        expected += "10:15:30.000 Global DM28 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 80 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM28PermanentEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM28(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM11DestinationSpecificNoResponseWithManyModules() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | 0x01, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x01)).thenReturn(requestPacket);

        // DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
        // Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE,
        // 0x00));
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of());

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Destination Specific DM11 Request" + NL;
        expected += "10:15:30.000 18EA01A5 D3 FE 00 (TX)" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM11(listener, 0x01));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x01);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11DestinationSpecificWithNoResponsesOneModule() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | 0x17, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x17)).thenReturn(requestPacket);
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Destination Specific DM11 Request" + NL;
        expected += "10:15:30.000 18EA17A5 D3 FE 00 (TX)" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM11(listener, 0x17));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x17);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11DestinationSpecificWithOneModule() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket1 = Packet.create(REQUEST_PGN | 0x01, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x01)).thenReturn(requestPacket1);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket1, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Destination Specific DM11 Request" + NL;
        expected += "10:15:30.000 18EA01A5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is Acknowledged" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM11(listener, 0x01));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x01);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket1, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11DestinationSpecificWithOneModuleWithNack() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | 0x01, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x01)).thenReturn(requestPacket);

        AcknowledgmentPacket packet1 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Destination Specific DM11 Request" + NL;
        expected += "10:15:30.000 18EA01A5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Engine #1 (0): Response: NACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;
        expected += "ERROR: Clearing Diagnostic Trouble Codes failed." + NL;
        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM11(listener, 0x01));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x01);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11DestinationSpecificWithTwoModuleWithNack() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | 0x01, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x01)).thenReturn(requestPacket);

        AcknowledgmentPacket packet1 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x21, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Destination Specific DM11 Request" + NL;
        expected += "10:15:30.000 18EA01A5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80021 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Body Controller (33): Response: NACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;
        expected += "ERROR: Clearing Diagnostic Trouble Codes failed." + NL;
        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM11(listener, 0x01));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x01);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11GlobalNoResponseWithManyModules() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of());

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D3 FE 00 (TX)" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM11(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11GlobalWithNoResponsesOneModule() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D3 FE 00 (TX)" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM11(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11GlobalWithOneModule() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket1 = Packet.create(REQUEST_PGN | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket1);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket1, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is Acknowledged" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM11(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket1, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11GlobalWithOneModuleWithNack() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        AcknowledgmentPacket packet1 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Engine #1 (0): Response: NACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;
        expected += "ERROR: Clearing Diagnostic Trouble Codes failed." + NL;
        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM11(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11WithManyModules() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM11ClearActiveDTCsPacket packet1 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        DM11ClearActiveDTCsPacket packet2 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x17, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        DM11ClearActiveDTCsPacket packet3 = new DM11ClearActiveDTCsPacket(
                Packet.create(0xE800, 0x21, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Engine #1 (0): Response is Acknowledged" + NL;
        expected += "10:15:30.000 18E80017 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Instrument Cluster #1 (23): Response is Acknowledged" + NL;
        expected += "10:15:30.000 18E80021 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "DM11 from Body Controller (33): Response is Acknowledged" + NL;
        expected += "Diagnostic Trouble Codes were successfully cleared." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Arrays.asList(new DM11ClearActiveDTCsPacket[] { packet1, packet2, packet3 }));
        assertEquals(expectedResult, instance.requestDM11(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM11WithManyModulesWithNack() {
        final int pgn = DM11ClearActiveDTCsPacket.PGN;

        Packet requestPacket = Packet.create(REQUEST_PGN | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        AcknowledgmentPacket packet1 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        AcknowledgmentPacket packet2 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x17, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        AcknowledgmentPacket packet3 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x21, 0x00, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));
        when(j1939.requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(null, p)));

        String expected = "";
        expected += "10:15:30.000 Clearing Diagnostic Trouble Codes" + NL;
        expected += "10:15:30.000 Global DM11 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D3 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Engine #1 (0): Response: NACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;
        expected += "10:15:30.000 18E80017 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Instrument Cluster #1 (23): Response: ACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;
        expected += "10:15:30.000 18E80021 00 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Body Controller (33): Response: ACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;
        expected += "ERROR: Clearing Diagnostic Trouble Codes failed." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM11ClearActiveDTCsPacket> expectedResult = new RequestResult<>(false, Collections.emptyList(),
                Arrays.asList(new AcknowledgmentPacket[] { packet1, packet2, packet3 }));
        assertEquals(expectedResult, instance.requestDM11(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM11ClearActiveDTCsPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM12DestinationSpecific() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        DM12MILOnEmissionDTCPacket packet1 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM12 Request" + NL;
        expected += "10:15:30.000 18EA00A5 D4 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FED400 00 00 00 00 00 00 00 00" + NL;
        expected += "DM12 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM12MILOnEmissionDTCPacket> expectedPackets = new ArrayList<>() {
            {
                add(packet1);
            }
        };
        RequestResult<DM12MILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false, expectedPackets,
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM12(listener, true, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM12DestinationSpecificWithDTCs() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        DM12MILOnEmissionDTCPacket packet1 = new DM12MILOnEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));
        when(j1939.requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM12 Request" + NL;
        expected += "10:15:30.000 18EA00A5 D4 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FED400 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM12 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM12MILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet1), Collections.emptyList());

        assertEquals(expectedResult, instance.requestDM12(listener, true, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM12DestinationSpecificWithNoResponses() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x17, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x17)).thenReturn(requestPacket);

        when(j1939.requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty()).thenReturn(Stream.empty()).thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM12 Request" + NL;
        expected += "10:15:30.000 18EA17A5 D4 FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM12MILOnEmissionDTCPacket> expectedResult = new RequestResult<>(true, Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM12(listener, true, 0x17));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x17);
        verify(j1939, times(3)).requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500,
                TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM12Global() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM12MILOnEmissionDTCPacket packet1 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM12MILOnEmissionDTCPacket packet2 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM12MILOnEmissionDTCPacket packet3 = new DM12MILOnEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM12 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D4 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FED400 00 00 00 00 00 00 00 00" + NL;
        expected += "DM12 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FED417 00 00 00 00 00 00 00 00" + NL;
        expected += "DM12 from Instrument Cluster #1 (23): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FED421 00 00 00 00 00 00 00 00" + NL;
        expected += "DM12 from Body Controller (33): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM12MILOnEmissionDTCPacket> expectedPackets = new ArrayList<>() {
            {
                add(packet1);
                add(packet2);
                add(packet3);
            }
        };
        RequestResult<DM12MILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false, expectedPackets,
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM12(listener, true));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM12GlobalWithDTCs() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM12MILOnEmissionDTCPacket packet1 = new DM12MILOnEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));
        when(j1939.requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM12 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D4 FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FED400 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM12 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM12MILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet1), Collections.emptyList());

        assertEquals(expectedResult, instance.requestDM12(listener, true));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM12GlobalWithNoResponses() {
        final int pgn = DM12MILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        when(j1939.requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty()).thenReturn(Stream.empty()).thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Global DM12 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 D4 FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM12MILOnEmissionDTCPacket> expectedResult = new RequestResult<>(true, Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM12(listener, true));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939, times(3)).requestRaw(DM12MILOnEmissionDTCPacket.class, requestPacket, 5500,
                TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM23DestinationSpecific() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x21, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x21)).thenReturn(requestPacket);

        DM23PreviouslyMILOnEmissionDTCPacket packet1 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM23 Request" + NL;
        expected += "10:15:30.000 18EA21A5 B5 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FDB521 00 00 00 00 00 00 00 00" + NL;
        expected += "DM23 from Body Controller (33): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM23PreviouslyMILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Arrays.asList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM23(listener, 0x21));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x21);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM23DestinationSpecificWithDTCs() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        DM23PreviouslyMILOnEmissionDTCPacket packet1 = new DM23PreviouslyMILOnEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));
        when(j1939.requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM23 Request" + NL;
        expected += "10:15:30.000 18EA00A5 B5 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FDB500 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM23 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM23PreviouslyMILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM23(listener, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM23DestinationSpecificWithNoResponses() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x17, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x17)).thenReturn(requestPacket);

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM23 Request" + NL;
        expected += "10:15:30.000 18EA17A5 B5 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM23PreviouslyMILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM23(listener, 0x17));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x17);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM23Global() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM23PreviouslyMILOnEmissionDTCPacket packet1 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM23PreviouslyMILOnEmissionDTCPacket packet2 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM23PreviouslyMILOnEmissionDTCPacket packet3 = new DM23PreviouslyMILOnEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM23 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 B5 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FDB500 00 00 00 00 00 00 00 00" + NL;
        expected += "DM23 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FDB517 00 00 00 00 00 00 00 00" + NL;
        expected += "DM23 from Instrument Cluster #1 (23): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FDB521 00 00 00 00 00 00 00 00" + NL;
        expected += "DM23 from Body Controller (33): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM23PreviouslyMILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Arrays.asList(packet1, packet2, packet3), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM23(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM23GlobalWithDTCs() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM23PreviouslyMILOnEmissionDTCPacket packet1 = new DM23PreviouslyMILOnEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));
        when(j1939.requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM23 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 B5 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FDB500 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM23 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM23PreviouslyMILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM23(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM23GlobalWithNoResponses() {
        final int pgn = DM23PreviouslyMILOnEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        String expected = "";
        expected += "10:15:30.000 Global DM23 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 B5 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM23PreviouslyMILOnEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM23(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM23PreviouslyMILOnEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM28DestinationSpecific() {
        final int pgn = DM28PermanentEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x21, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x21)).thenReturn(requestPacket);

        DM28PermanentEmissionDTCPacket packet1 = new DM28PermanentEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM28 Request" + NL;
        expected += "10:15:30.000 18EA21A5 80 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FD8021 00 00 00 00 00 00 00 00" + NL;
        expected += "DM28 from Body Controller (33): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM28PermanentEmissionDTCPacket> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM28(listener, 0x21));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x21);
        verify(j1939).requestMultiple(DM28PermanentEmissionDTCPacket.class, requestPacket);
    }

    @Test
    public void testRequestDM2DestinationSpecificNoResponse() {
        final int pgn = DM2PreviouslyActiveDTC.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x17, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x17)).thenReturn(requestPacket);

        when(j1939.requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty()).thenReturn(Stream.empty()).thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM2 Request" + NL;
        expected += "10:15:30.000 18EA17A5 CB FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM2PreviouslyActiveDTC> expectedPackets = new ArrayList<>();
        assertEquals(expectedPackets, instance.requestDM2(listener, true, 0x17).getPackets());
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x17);
        verify(j1939, times(3))
                .requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM2DestinationSpecificWithEngine1Response() {
        final int pgn = DM2PreviouslyActiveDTC.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x01, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x01)).thenReturn(requestPacket);

        DM2PreviouslyActiveDTC packet1 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x01, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88));
        when(j1939.requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM2 Request" + NL;
        expected += "10:15:30.000 18EA01A5 CB FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FECB01 11 22 33 44 55 66 77 88" + NL;
        expected += "DM2 from Engine #2 (1): MIL: off, RSL: other, AWL: off, PL: other" + NL;
        expected += "DTC: Unknown (148531) Data Drifted Low (21) 102 times" + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM2PreviouslyActiveDTC> expectedPackets = new ArrayList<>() {
            {
                add(packet1);
            }
        };
        assertEquals(expectedPackets, instance.requestDM2(listener, true, 0x01).getPackets());
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x01);
        verify(j1939).requestRaw(DM2PreviouslyActiveDTC.class,
                requestPacket,
                5500,
                TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM2GlobalFullStringTrue() {
        final int pgn = DM2PreviouslyActiveDTC.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM2PreviouslyActiveDTC packet1 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88));
        DM2PreviouslyActiveDTC packet2 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x17, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08));
        DM2PreviouslyActiveDTC packet3 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x21, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80));
        when(j1939.requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM2 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CB FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FECB00 11 22 33 44 55 66 77 88" + NL;
        expected += "DM2 from Engine #1 (0): MIL: off, RSL: other, AWL: off, PL: other" + NL;
        expected += "DTC: Unknown (148531) Data Drifted Low (21) 102 times" + NL;
        expected += "10:15:30.000 18FECB17 01 02 03 04 05 06 07 08" + NL;
        expected += "DM2 from Instrument Cluster #1 (23): MIL: off, RSL: off, AWL: off, PL: other" + NL;
        expected += "DTC: Trip Time in Derate by Engine (1027) Current Below Normal Or Open Circuit (5) 6 times" + NL;
        expected += "10:15:30.000 18FECB21 10 20 30 40 50 60 70 80" + NL;
        expected += "DM2 from Body Controller (33): MIL: off, RSL: other, AWL: off, PL: off" + NL;
        expected += "DTC: Unknown (147504) Data Valid But Above Normal Operating Range - Moderately Severe Level (16) 96 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM2PreviouslyActiveDTC> expectedPackets = new ArrayList<>() {
            {
                add(packet1);
                add(packet2);
                add(packet3);
            }
        };
        assertEquals(expectedPackets, instance.requestDM2(listener, true).getPackets());

        // instance.getDM2Packets(listener, true, 0x17);
        assertEquals("", listener.getMessages());
        assertEquals("", listener.getMilestones());
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM2GlobalPacketsFullStringFalse() {
        final int pgn = DM2PreviouslyActiveDTC.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, false, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM2PreviouslyActiveDTC packet1 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88));
        DM2PreviouslyActiveDTC packet2 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x17, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08));
        DM2PreviouslyActiveDTC packet3 = new DM2PreviouslyActiveDTC(
                Packet.create(pgn, 0x21, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80));
        when(j1939.requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM2 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CB FE 00" + NL;
        expected += "10:15:30.000 18FECB00 11 22 33 44 55 66 77 88" + NL;
        expected += "10:15:30.000 18FECB17 01 02 03 04 05 06 07 08" + NL;
        expected += "10:15:30.000 18FECB21 10 20 30 40 50 60 70 80" + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM2PreviouslyActiveDTC> expectedPackets = new ArrayList<>() {
            {
                add(packet1);
                add(packet2);
                add(packet3);
            }
        };
        assertEquals(expectedPackets, instance.requestDM2(listener, false).getPackets());
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM2GlobalWithDTCs() {
        final int pgn = DM2PreviouslyActiveDTC.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        DM2PreviouslyActiveDTC packet1 = new DM2PreviouslyActiveDTC(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));
        when(j1939.requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM2 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CB FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FECB00 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM2 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();

        RequestResult<DM2PreviouslyActiveDTC> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet1),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM2(listener, true));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    /**
     * Test method for
     * {@link org.etools.j1939_84.modules.DTCModule.requestDM2(ResultsListener
     * listener, boolean fullString)}.
     */
    @Test
    public void testRequestDM2GlobalWithNoResponses() {
        final int pgn = DM2PreviouslyActiveDTC.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);
        when(j1939.requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty()).thenReturn(Stream.empty()).thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Global DM2 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CB FE 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        assertEquals(new ArrayList<DM2PreviouslyActiveDTC>(), instance.requestDM2(listener, true).getPackets());
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939, times(3)).requestRaw(DM2PreviouslyActiveDTC.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM33DestinationSpecificEmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime() {
        final int pgn = DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        byte[] data = { 0x01, 0x2B, 0x0B, 0x01, 0x00, 0x2B, (byte) 0xC4, 0x0B, 0x00,
                // 1 with FE for timer 1 and FF for timer 2
                0x02, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF,
                0x03, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, 0x2C, 0x0B, 0x03, 0x00,
                // 1 with FF for timer 1 and FE for timer 2
                0x04, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0xFF };
        DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime packet1 = new DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime(
                Packet.create(pgn,
                        0x00,
                        data));

        when(j1939.requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM33 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 00 A1 00 (TX)" + NL;
        expected += "10:15:30.000 18A10000 01 2B 0B 01 00 2B C4 0B 00 02 FE FE FE FE FF FF FF FF 03 FE FE FE FE 2C 0B 03 00 04 FF FF FF FE FE FE FE FF"
                + NL;
        expected += "DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 1" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = 68395 minutes" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = 771115 minutes" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 2" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = n/a" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 3" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = 199468 minutes" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 4" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = n/a" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime> expectedResult = new RequestResult<>(
                false,
                Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM33(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class,
                requestPacket);
    }

    @Test
    public void testRequestDM33DestinationSpecificNoResponse() {
        final int pgn = DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        when(j1939.requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class, requestPacket))
                .thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Destination Specific DM33 Request" + NL;
        expected += "10:15:30.000 18EA00A5 00 A1 00 (TX)" + NL;
        expected += "Error: Timeout - No Response."
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime> expectedResult = new RequestResult<>(
                false,
                Collections.emptyList(), Collections.emptyList());
        System.out.println(requestPacket);
        assertEquals(expectedResult, instance.requestDM33(listener, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class,
                requestPacket);
    }

    @Test
    public void testRequestDM33GlobalEmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime() {
        final int pgn = DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        byte[] data = { 0x01, 0x2B, 0x0B, 0x01, 0x00, 0x2B, (byte) 0xC4, 0x0B, 0x00,
                // 1 with FE for timer 1 and FF for timer 2
                0x02, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF,
                0x03, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, 0x2C, 0x0B, 0x03, 0x00,
                // 1 with FF for timer 1 and FE for timer 2
                0x04, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0xFF };
        DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime packet1 = new DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime(
                Packet.create(pgn,
                        GLOBAL_ADDR,
                        data));

        when(j1939.requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM33 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 00 A1 00 (TX)" + NL;
        expected += "10:15:30.000 18A100FF 01 2B 0B 01 00 2B C4 0B 00 02 FE FE FE FE FF FF FF FF 03 FE FE FE FE 2C 0B 03 00 04 FF FF FF FE FE FE FE FF"
                + NL;
        expected += "DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 1" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = 68395 minutes" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = 771115 minutes" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 2" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = n/a" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 3" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = 199468 minutes" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 4" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = n/a" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime> expectedResult = new RequestResult<>(
                false,
                Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM33(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class,
                requestPacket);
    }

    @Test
    public void testRequestDM33GlobalNoResponse() {
        final int pgn = DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.PGN;

        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        byte[] data = { 0x01, 0x2B, 0x0B, 0x01, 0x00, 0x2B, (byte) 0xC4, 0x0B, 0x00,
                // 1 with FE for timer 1 and FF for timer 2
                0x02, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF,
                0x03, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, 0x2C, 0x0B, 0x03, 0x00,
                // 1 with FF for timer 1 and FE for timer 2
                0x04, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
                (byte) 0xFF };
        DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime packet1 = new DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime(
                Packet.create(pgn,
                        0x00,
                        data));

        when(j1939.requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class, requestPacket))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM33 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 00 A1 00 (TX)" + NL;
        expected += "10:15:30.000 18A10000 01 2B 0B 01 00 2B C4 0B 00 02 FE FE FE FE FF FF FF FF 03 FE FE FE FE 2C 0B 03 00 04 FF FF FF FE FE FE FE FF"
                + NL;
        expected += "DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 1" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = 68395 minutes" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = 771115 minutes" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 2" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = n/a" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 3" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = 199468 minutes" + NL;
        expected += "EngineHoursTimer" + NL;
        expected += "  EI-AECD Number = 4" + NL;
        expected += "  EI-AECD Engine Hours Timer 1 = errored" + NL;
        expected += "  EI-AECD Engine Hours Timer 2 = n/a" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime> expectedResult = new RequestResult<>(
                false,
                Collections.singletonList(packet1), Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM33(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestMultiple(DM33EmissionIncreasingAuxiliaryEmissionControlDeviceActiveTime.class,
                requestPacket);
    }

    @Test
    public void testRequestDM6() {
        final int pgn = DM6PendingEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM6PendingEmissionDTCPacket packet1 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x00, 0, 0, 0, 0, 0, 0, 0, 0));
        DM6PendingEmissionDTCPacket packet2 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x17, 0, 0, 0, 0, 0, 0, 0, 0));
        DM6PendingEmissionDTCPacket packet3 = new DM6PendingEmissionDTCPacket(
                Packet.create(pgn, 0x21, 0, 0, 0, 0, 0, 0, 0, 0));
        when(j1939.requestRaw(DM6PendingEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1, packet2, packet3).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM6 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CF FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FECF00 00 00 00 00 00 00 00 00" + NL;
        expected += "DM6 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FECF17 00 00 00 00 00 00 00 00" + NL;
        expected += "DM6 from Instrument Cluster #1 (23): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;
        expected += "10:15:30.000 18FECF21 00 00 00 00 00 00 00 00" + NL;
        expected += "DM6 from Body Controller (33): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "No DTCs" + NL;

        TestResultsListener listener = new TestResultsListener();
        List<DM6PendingEmissionDTCPacket> dm6Packets = new ArrayList<>() {
            {
                add(packet1);
                add(packet2);
                add(packet3);
            }
        };
        RequestResult<DM6PendingEmissionDTCPacket> result = new RequestResult<>(false, dm6Packets,
                Collections.emptyList());

        assertEquals(result, instance.requestDM6(listener));
        assertEquals(expected, listener.getResults());
        assertEquals("", listener.getMessages());
        assertEquals("", listener.getMilestones());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestRaw(DM6PendingEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM6WithDTCs() {
        final int pgn = DM6PendingEmissionDTCPacket.PGN;

        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        DM6PendingEmissionDTCPacket packet1 = new DM6PendingEmissionDTCPacket(Packet.create(pgn,
                0x00,
                0x00,
                0xFF,
                0x61,
                0x02,
                0x13,
                0x00,
                0x21,
                0x06,
                0x1F,
                0x00,
                0xEE,
                0x10,
                0x04,
                0x00));
        when(j1939.requestRaw(DM6PendingEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.of(packet1).map(p -> new Either<>(p, null)));

        String expected = "";
        expected += "10:15:30.000 Global DM6 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CF FE 00 (TX)" + NL;
        expected += "10:15:30.000 18FECF00 00 FF 61 02 13 00 21 06 1F 00 EE 10 04 00" + NL;
        expected += "DM6 from Engine #1 (0): MIL: off, RSL: off, AWL: off, PL: off" + NL;
        expected += "DTC: Controller #2 (609) Received Network Data In Error (19) 0 times" + NL;
        expected += "DTC: Engine Protection Torque Derate (1569) Condition Exists (31) 0 times" + NL;
        expected += "DTC: Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure (4334) Voltage Below Normal, Or Shorted To Low Source (4) 0 times"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM6PendingEmissionDTCPacket> result = new RequestResult<>(false,
                Collections.singletonList(packet1),
                Collections.emptyList());
        assertEquals(result, instance.requestDM6(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestRaw(DM6PendingEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRequestDM6WithNoResponses() {
        final int pgn = DM6PendingEmissionDTCPacket.PGN;
        Packet requestPacket = Packet.create(0xEA00 | 0xFF, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0xFF)).thenReturn(requestPacket);

        when(j1939.requestRaw(DM6PendingEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS))
                .thenReturn(Stream.empty());

        String expected = "";
        expected += "10:15:30.000 Global DM6 Request" + NL;
        expected += "10:15:30.000 18EAFFA5 CF FE 00 (TX)" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM6PendingEmissionDTCPacket> result = new RequestResult<>(false,
                Collections.emptyList(),
                Collections.emptyList());
        assertEquals(result, instance.requestDM6(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0xFF);
        verify(j1939).requestRaw(DM6PendingEmissionDTCPacket.class, requestPacket, 5500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testResquestDM25DestinationSpecificNackOnly() {
        final int pgn = DM25ExpandedFreezeFrame.PGN;
        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        AcknowledgmentPacket packet1 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestPacket(requestPacket,
                DM25ExpandedFreezeFrame.class, 0x00, 3,
                TimeUnit.SECONDS.toMillis(15)))
                        .thenReturn(Optional.of(new BusResult<>(false,
                                new Either<DM25ExpandedFreezeFrame, AcknowledgmentPacket>(null, packet1))));

        String expected = "10:15:30.000 Destination Specific DM25 Request to Engine #1 (0)" + NL;
        expected += "10:15:30.000 18EA00A5 B7 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Engine #1 (0): Response: NACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM25ExpandedFreezeFrame> expectedResult = new RequestResult<>(false,
                Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM25(listener, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, 0x00, 3,
                TimeUnit.SECONDS.toMillis(15));
    }

    @Test
    public void testResquestDM25DestinationSpecificNoResponse() {
        final int pgn = DM25ExpandedFreezeFrame.PGN;
        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        when(j1939.requestPacket(requestPacket,
                DM25ExpandedFreezeFrame.class, 0x00, 3,
                TimeUnit.SECONDS.toMillis(15)))
                        .thenReturn(Optional.empty());
        String expected = "10:15:30.000 Destination Specific DM25 Request to Engine #1 (0)" + NL;
        expected += "10:15:30.000 18EA00A5 B7 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response." + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM25ExpandedFreezeFrame> expectedResult = new RequestResult<>(false,
                Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM25(listener, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, 0x00, 3,
                TimeUnit.SECONDS.toMillis(15));
    }

    @Test
    public void testResquestDM25DestinationSpecificWithResponse() {
        final int pgn = DM25ExpandedFreezeFrame.PGN;
        Packet requestPacket = Packet.create(0xEA00 | 0x00, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, 0x00)).thenReturn(requestPacket);

        int[] realData = new int[] {
                0x56,
                0x9D,
                0x00,
                0x07,
                0x7F,
                0x00,
                0x01,
                0x7B,
                0x00,
                0x00,
                0x39,
                0x3A,
                0x5C,
                0x0F,
                0xC4,
                0xFB,
                0x00,
                0x00,
                0x00,
                0xF1,
                0x26,
                0x00,
                0x00,
                0x00,
                0x12,
                0x7A,
                0x7D,
                0x80,
                0x65,
                0x00,
                0x00,
                0x32,
                0x00,
                0x00,
                0x00,
                0x00,
                0x84,
                0xAD,
                0x00,
                0x39,
                0x2C,
                0x30,
                0x39,
                0xFC,
                0x38,
                0xC6,
                0x35,
                0xE0,
                0x34,
                0x2C,
                0x2F,
                0x00,
                0x00,
                0x7D,
                0x7D,
                0x8A,
                0x28,
                0xA0,
                0x0F,
                0xA0,
                0x0F,
                0xD1,
                0x37,
                0x00,
                0xCA,
                0x28,
                0x01,
                0xA4,
                0x0D,
                0x00,
                0xA8,
                0xC3,
                0xB2,
                0xC2,
                0xC3,
                0x00,
                0x00,
                0x00,
                0x00,
                0x7E,
                0xD0,
                0x07,
                0x00,
                0x7D,
                0x04,
                0xFF,
                0xFA };

        DM25ExpandedFreezeFrame packet = new DM25ExpandedFreezeFrame(Packet.create(pgn, 0x00, realData));
        when(j1939.requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, 0x00, 3,
                TimeUnit.SECONDS.toMillis(15)))
                        .thenReturn(Optional.of(new BusResult<>(false, packet)));

        String expected = "10:15:30.000 Destination Specific DM25 Request to Engine #1 (0)" + NL;
        expected += "10:15:30.000 18EA00A5 B7 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FDB700 56 9D 00 07 7F 00 01 7B 00 00 39 3A 5C 0F C4 FB 00 00 00 F1 26 00 00 00 12 7A 7D 80 65 00 00 32 00 00 00 00 84 AD 00 39 2C 30 39 FC 38 C6 35 E0 34 2C 2F 00 00 7D 7D 8A 28 A0 0F A0 0F D1 37 00 CA 28 01 A4 0D 00 A8 C3 B2 C2 C3 00 00 00 00 7E D0 07 00 7D 04 FF FA"
                + NL;
        expected += "DM25 from Engine #1 (0): " + NL;
        expected += "Freeze Frames: [" + NL;
        expected += "DTC: Engine Fuel 1 Injector Metering Rail 1 Pressure (157) Mechanical System Not Responding Or Out Of Adjustment (7) 127 times"
                + NL;
        expected += "SPN Data: 00 01 7B 00 00 39 3A 5C 0F C4 FB 00 00 00 F1 26 00 00 00 12 7A 7D 80 65 00 00 32 00 00 00 00 84 AD 00 39 2C 30 39 FC 38 C6 35 E0 34 2C 2F 00 00 7D 7D 8A 28 A0 0F A0 0F D1 37 00 CA 28 01 A4 0D 00 A8 C3 B2 C2 C3 00 00 00 00 7E D0 07 00 7D 04 FF"
                + NL;
        expected += "]" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM25ExpandedFreezeFrame> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM25(listener, 0x00));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, 0x00);
        verify(j1939).requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, 0x00, 3,
                TimeUnit.SECONDS.toMillis(15));
    }

    @Test
    public void testResquestDM25GlobalNackOnly() {
        final int pgn = DM25ExpandedFreezeFrame.PGN;
        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        AcknowledgmentPacket packet1 = new AcknowledgmentPacket(
                Packet.create(0xE800, 0x00, 0x01, 0xFF, 0xFF, 0xFF, 0xF9, 0xD3, 0xFE, 0x00));

        when(j1939.requestPacket(requestPacket,
                DM25ExpandedFreezeFrame.class, GLOBAL_ADDR, 3,
                TimeUnit.SECONDS.toMillis(15)))
                        .thenReturn(Optional.of(new BusResult<>(false,
                                new Either<DM25ExpandedFreezeFrame, AcknowledgmentPacket>(null, packet1))));

        String expected = "10:15:30.000 Global DM25 Request to Global (255)" + NL;
        expected += "10:15:30.000 18EAFFA5 B7 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18E80000 01 FF FF FF F9 D3 FE 00" + NL;
        expected += "Acknowledgment from Engine #1 (0): Response: NACK, Group Function: 255, Address Acknowledged: 249, PGN Requested: 65235"
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM25ExpandedFreezeFrame> expectedResult = new RequestResult<>(false,
                Collections.emptyList(),
                Collections.singletonList(packet1));
        assertEquals(expectedResult, instance.requestDM25(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, GLOBAL_ADDR, 3,
                TimeUnit.SECONDS.toMillis(15));
    }

    @Test
    public void testResquestDM25GlobalNoResponse() {
        final int pgn = DM25ExpandedFreezeFrame.PGN;
        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        String expected = "10:15:30.000 Global DM25 Request to Global (255)" + NL;
        expected += "10:15:30.000 18EAFFA5 B7 FD 00 (TX)" + NL;
        expected += "Error: Timeout - No Response."
                + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM25ExpandedFreezeFrame> expectedResult = new RequestResult<>(false,
                Collections.emptyList(),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM25(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestPacket(requestPacket,
                DM25ExpandedFreezeFrame.class, GLOBAL_ADDR, 3,
                TimeUnit.SECONDS.toMillis(15));

    }

    @Test
    public void testResquestDM25GlobalWithResponse() {
        final int pgn = DM25ExpandedFreezeFrame.PGN;
        Packet requestPacket = Packet.create(0xEA00 | GLOBAL_ADDR, BUS_ADDR, true, pgn, pgn >> 8, pgn >> 16);
        when(j1939.createRequestPacket(pgn, GLOBAL_ADDR)).thenReturn(requestPacket);

        int[] realData = new int[] {
                0x56,
                0x9D,
                0x00,
                0x07,
                0x7F,
                0x00,
                0x01,
                0x7B,
                0x00,
                0x00,
                0x39,
                0x3A,
                0x5C,
                0x0F,
                0xC4,
                0xFB,
                0x00,
                0x00,
                0x00,
                0xF1,
                0x26,
                0x00,
                0x00,
                0x00,
                0x12,
                0x7A,
                0x7D,
                0x80,
                0x65,
                0x00,
                0x00,
                0x32,
                0x00,
                0x00,
                0x00,
                0x00,
                0x84,
                0xAD,
                0x00,
                0x39,
                0x2C,
                0x30,
                0x39,
                0xFC,
                0x38,
                0xC6,
                0x35,
                0xE0,
                0x34,
                0x2C,
                0x2F,
                0x00,
                0x00,
                0x7D,
                0x7D,
                0x8A,
                0x28,
                0xA0,
                0x0F,
                0xA0,
                0x0F,
                0xD1,
                0x37,
                0x00,
                0xCA,
                0x28,
                0x01,
                0xA4,
                0x0D,
                0x00,
                0xA8,
                0xC3,
                0xB2,
                0xC2,
                0xC3,
                0x00,
                0x00,
                0x00,
                0x00,
                0x7E,
                0xD0,
                0x07,
                0x00,
                0x7D,
                0x04,
                0xFF,
                0xFA };

        DM25ExpandedFreezeFrame packet = new DM25ExpandedFreezeFrame(Packet.create(pgn, 0x00, realData));
        when(j1939.requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, GLOBAL_ADDR, 3,
                TimeUnit.SECONDS.toMillis(15)))
                        .thenReturn(Optional.of(new BusResult<>(false, packet)));

        String expected = "10:15:30.000 Global DM25 Request to Global (255)" + NL;
        expected += "10:15:30.000 18EAFFA5 B7 FD 00 (TX)" + NL;
        expected += "10:15:30.000 18FDB700 56 9D 00 07 7F 00 01 7B 00 00 39 3A 5C 0F C4 FB 00 00 00 F1 26 00 00 00 12 7A 7D 80 65 00 00 32 00 00 00 00 84 AD 00 39 2C 30 39 FC 38 C6 35 E0 34 2C 2F 00 00 7D 7D 8A 28 A0 0F A0 0F D1 37 00 CA 28 01 A4 0D 00 A8 C3 B2 C2 C3 00 00 00 00 7E D0 07 00 7D 04 FF FA"
                + NL;
        expected += "DM25 from Engine #1 (0): " + NL;
        expected += "Freeze Frames: [" + NL;
        expected += "DTC: Engine Fuel 1 Injector Metering Rail 1 Pressure (157) Mechanical System Not Responding Or Out Of Adjustment (7) 127 times"
                + NL;
        expected += "SPN Data: 00 01 7B 00 00 39 3A 5C 0F C4 FB 00 00 00 F1 26 00 00 00 12 7A 7D 80 65 00 00 32 00 00 00 00 84 AD 00 39 2C 30 39 FC 38 C6 35 E0 34 2C 2F 00 00 7D 7D 8A 28 A0 0F A0 0F D1 37 00 CA 28 01 A4 0D 00 A8 C3 B2 C2 C3 00 00 00 00 7E D0 07 00 7D 04 FF"
                + NL;
        expected += "]" + NL;

        TestResultsListener listener = new TestResultsListener();
        RequestResult<DM25ExpandedFreezeFrame> expectedResult = new RequestResult<>(false,
                Collections.singletonList(packet),
                Collections.emptyList());
        assertEquals(expectedResult, instance.requestDM25(listener));
        assertEquals(expected, listener.getResults());

        verify(j1939).createRequestPacket(pgn, GLOBAL_ADDR);
        verify(j1939).requestPacket(requestPacket, DM25ExpandedFreezeFrame.class, GLOBAL_ADDR, 3,
                TimeUnit.SECONDS.toMillis(15));
    }

}
