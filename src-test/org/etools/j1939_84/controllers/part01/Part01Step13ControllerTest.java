/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part01;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.WARN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.etools.j1939_84.bus.Packet;
import org.etools.j1939_84.bus.j1939.BusResult;
import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939_84.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.model.RequestResult;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticMessageModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class Part01Step13ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 1;
    private static final int STEP_NUMBER = 13;
    private static final int PGN = DM5DiagnosticReadinessPacket.PGN;

    @Mock
    private BannerModule bannerModule;

    @Mock
    private DataRepository dataRepository;

    @Mock
    private DiagnosticMessageModule diagnosticMessageModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    private Part01Step13Controller instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private OBDModuleInformation obdModuleInformation;

    @Mock
    private SectionA6Validator sectionA6Validator;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() throws Exception {

        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);

        instance = new Part01Step13Controller(
                                              executor,
                                              engineSpeedModule,
                                              bannerModule,
                                              vehicleInformationModule,
                                              diagnosticMessageModule,
                                              dataRepository,
                                              sectionA6Validator,
                                              DateTimeModule.getInstance());

        ReportFileModule reportFileModule = mock(ReportFileModule.class);
        setup(instance,
              listener,
              j1939,
              executor,
              reportFileModule,
              engineSpeedModule,
              vehicleInformationModule,
              diagnosticMessageModule);

    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(executor,
                                 engineSpeedModule,
                                 bannerModule,
                                 obdModuleInformation,
                                 vehicleInformationModule,
                                 dataRepository,
                                 mockListener,
                                 sectionA6Validator,
                                 diagnosticMessageModule);
    }

    @Test
    public void testGetDisplayName() {
        String name = "Part " + PART_NUMBER + " Step " + STEP_NUMBER;
        assertEquals("Display Name", name, instance.getDisplayName());
    }

    @Test
    public void testGetStepNumber() {
        assertEquals(STEP_NUMBER, instance.getStepNumber());
    }

    @Test
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 0, instance.getTotalSteps());
    }

    @Test
    public void testRun() {

        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(PGN,
                                                                                              0x00,
                                                                                              0xFF,
                                                                                              0xFF,
                                                                                              0x14,
                                                                                              0x37,
                                                                                              0xE0,
                                                                                              0x1E,
                                                                                              0xE0,
                                                                                              0x1E));
        DM5DiagnosticReadinessPacket packet17 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(PGN,
                                                                                               0x17,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x05,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x00));
        DM5DiagnosticReadinessPacket packet21 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(PGN,
                                                                                               0x21,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x05,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x00));

        RequestResult<DM5DiagnosticReadinessPacket> globalRequestResponse = new RequestResult<>(
                                                                                                false,
                                                                                                packet0,
                                                                                                packet17,
                                                                                                packet21);
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of());

        runTest();

        verify(dataRepository, atLeastOnce()).getObdModuleAddresses();

        verify(diagnosticMessageModule).requestDM5(any());

        verify(sectionA6Validator).verify(any(), eq("6.1.13.2.a"), eq(globalRequestResponse));

        assertEquals("", listener.getMessages());
        String expectedVehicleComposite = NL + "Vehicle Composite of DM5:" + NL +
                "    A/C system refrigerant     not supported,     complete" + NL +
                "    Boost pressure control sys     supported, not complete" + NL +
                "    Catalyst                   not supported,     complete" + NL +
                "    Cold start aid system      not supported,     complete" + NL +
                "    Comprehensive component        supported,     complete" + NL +
                "    Diesel Particulate Filter      supported, not complete" + NL +
                "    EGR/VVT system                 supported, not complete" + NL +
                "    Evaporative system         not supported,     complete" + NL +
                "    Exhaust Gas Sensor             supported, not complete" + NL +
                "    Exhaust Gas Sensor heater      supported, not complete" + NL +
                "    Fuel System                    supported, not complete" + NL +
                "    Heated catalyst            not supported,     complete" + NL +
                "    Misfire                        supported, not complete" + NL +
                "    NMHC converting catalyst       supported, not complete" + NL +
                "    NOx catalyst/adsorber          supported, not complete" + NL +
                "    Secondary air system       not supported,     complete" + NL;
        assertEquals(expectedVehicleComposite + NL, listener.getResults());
    }

    @Test
    public void testStep13DM5PacketsEmpty() {
        AcknowledgmentPacket packet44 = new AcknowledgmentPacket(
                                                                 Packet.create(PGN,
                                                                               0x44,
                                                                               0x01,
                                                                               0x02,
                                                                               0x03,
                                                                               0x04,
                                                                               0x05,
                                                                               0x06,
                                                                               0x07,
                                                                               0x08));
        RequestResult<DM5DiagnosticReadinessPacket> globalRequestResponse = new RequestResult<>(false,
                                                                                                List.of(),
                                                                                                List.of(packet44));
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x00 = new BusResult<>(false,
                                                                                Optional.empty());
        when(diagnosticMessageModule.requestDM5(any(), eq(0x00))).thenReturn(busResult0x00);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x17 = new BusResult<>(false,
                                                                                Optional.empty());
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17))).thenReturn(busResult0x17);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x21 = new BusResult<>(false,
                                                                                Optional.empty());
        when(diagnosticMessageModule.requestDM5(any(), eq(0x21))).thenReturn(busResult0x21);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x21));

        runTest();

        verify(dataRepository, atLeastOnce()).getObdModuleAddresses();

        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x21));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.1.a - Global DM5 request did not receive any response packets");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.2.c - No OBD ECU provided DM5 with readiness bits showing monitor support");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.4.b. - OBD ECU Engine #1 (0) did not provide a response to Global query and did not provide a NACK for the DS query");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.4.b. - OBD ECU Instrument Cluster #1 (23) did not provide a response to Global query and did not provide a NACK for the DS query");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.4.b. - OBD ECU Body Controller (33) did not provide a response to Global query and did not provide a NACK for the DS query");

        verify(sectionA6Validator).verify(any(), eq("6.1.13.2.a"), eq(globalRequestResponse));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
    }

    @Test
    public void testStep13DM5PacketsFail() {
        final int pgn = DM5DiagnosticReadinessPacket.PGN;
        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(pgn,
                                                                                              0x00,
                                                                                              0x03,
                                                                                              0x10,
                                                                                              0x14,
                                                                                              0x37,
                                                                                              0xE0,
                                                                                              0x1E,
                                                                                              0xE0,
                                                                                              0x1E));
        DM5DiagnosticReadinessPacket packet21 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(pgn,
                                                                                               0x21,
                                                                                               0x00,
                                                                                               0x00,
                                                                                               0x14,
                                                                                               0x37,
                                                                                               0xE0,
                                                                                               0x1E,
                                                                                               0xE0,
                                                                                               0x1E));
        DM5DiagnosticReadinessPacket packet21V2 = new DM5DiagnosticReadinessPacket(
                                                                                   Packet.create(pgn,
                                                                                                 0x21,
                                                                                                 0x00,
                                                                                                 0x00,
                                                                                                 0x00,
                                                                                                 0x00,
                                                                                                 0xE0,
                                                                                                 0x1E,
                                                                                                 0xE0,
                                                                                                 0x1E));

        final int ackPgn = AcknowledgmentPacket.PGN;
        AcknowledgmentPacket packet23 = new AcknowledgmentPacket(
                                                                 Packet.create(ackPgn,
                                                                               0x23,
                                                                               0x10,
                                                                               0x20,
                                                                               0x30,
                                                                               0x40,
                                                                               0x50,
                                                                               0x60,
                                                                               0x70,
                                                                               0x80));
        RequestResult<DM5DiagnosticReadinessPacket> globalResponse = new RequestResult<>(
                                                                                         false,
                                                                                         List.of(packet0, packet21),
                                                                                         List.of(packet23));
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalResponse);

        when(diagnosticMessageModule.requestDM5(any(), eq(0x00)))
                                                                 .thenReturn(new BusResult<>(false, Optional.empty()));
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17)))
                                                                 .thenReturn(new BusResult<>(false, Optional.empty()));
        when(diagnosticMessageModule.requestDM5(any(), eq(0x21)))
                                                                 .thenReturn(new BusResult<>(false, packet21V2));
        when(diagnosticMessageModule.requestDM5(any(), eq(0x23)))
                                                                 .thenReturn(new BusResult<>(false, packet23));

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x21, 0x23));

        runTest();

        verify(dataRepository, atLeastOnce()).getObdModuleAddresses();

        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x21));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x23));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.2.b - OBD ECU Engine #1 (0) reported active DTC count not = 0");

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.1.13.2.b - OBD ECU Engine #1 (0) reported previously active DTC count not = 0");

        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor Boost pressure control sys is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor Diesel Particulate Filter is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor EGR/VVT system is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor Exhaust Gas Sensor is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor Exhaust Gas Sensor heater is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor Fuel System is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor Misfire is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor NMHC converting catalyst is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        WARN,
                                        "6.1.13.2.d - Required monitor NOx catalyst/adsorber is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        FAIL,
                                        "6.1.13.4.a - Difference compared to data received during global request from Body Controller (33)");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        FAIL,
                                        "6.1.13.4.b. - OBD ECU Instrument Cluster #1 (23) did not provide a response to Global query and did not provide a NACK for the DS query");
        verify(mockListener).addOutcome(
                                        1,
                                        13,
                                        FAIL,
                                        "6.1.13.4.b. - OBD ECU Hitch Control (35) did not provide a response to Global query and did not provide a NACK for the DS query");

        verify(sectionA6Validator).verify(any(), eq("6.1.13.2.a"), eq(globalResponse));

        assertEquals("", listener.getMessages());
        String expectedVehicleComposite = NL + "Vehicle Composite of DM5:" + NL +
                "    A/C system refrigerant     not supported,     complete" + NL +
                "    Boost pressure control sys     supported, not complete" + NL +
                "    Catalyst                   not supported,     complete" + NL +
                "    Cold start aid system      not supported,     complete" + NL +
                "    Comprehensive component        supported,     complete" + NL +
                "    Diesel Particulate Filter      supported, not complete" + NL +
                "    EGR/VVT system                 supported, not complete" + NL +
                "    Evaporative system         not supported,     complete" + NL +
                "    Exhaust Gas Sensor             supported, not complete" + NL +
                "    Exhaust Gas Sensor heater      supported, not complete" + NL +
                "    Fuel System                    supported, not complete" + NL +
                "    Heated catalyst            not supported,     complete" + NL +
                "    Misfire                        supported, not complete" + NL +
                "    NMHC converting catalyst       supported, not complete" + NL +
                "    NOx catalyst/adsorber          supported, not complete" + NL +
                "    Secondary air system       not supported,     complete" + NL;
        assertEquals(expectedVehicleComposite + NL, listener.getResults());
    }
}
