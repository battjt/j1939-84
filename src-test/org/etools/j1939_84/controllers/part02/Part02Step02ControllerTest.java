/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part02;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.bus.j1939.packets.DM5DiagnosticReadinessPacket.PGN;
import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.WARN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import org.etools.j1939_84.controllers.part01.SectionA6Validator;
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
 * The unit test for {@link Part02Step02Controller}
 *
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class Part02Step02ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 2;
    private static final int STEP_NUMBER = 2;

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

    private Part02Step02Controller instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private SectionA6Validator sectionA6Validator;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() throws Exception {
        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);

        instance = new Part02Step02Controller(
                                              executor,
                                              engineSpeedModule,
                                              bannerModule,
                                              vehicleInformationModule,
                                              diagnosticMessageModule,
                                              DateTimeModule.getInstance(),
                                              sectionA6Validator,
                                              dataRepository);
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
                                 vehicleInformationModule,
                                 diagnosticMessageModule,
                                 dataRepository,
                                 mockListener,
                                 sectionA6Validator);
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
    public void testModulesEmpty() {
        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(PGN,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x14,
                                                                                              0x37,
                                                                                              0xE0,
                                                                                              0x1E,
                                                                                              0xE0,
                                                                                              0x1E));
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
        DM5DiagnosticReadinessPacket packet21 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(PGN,
                                                                                               0x21,
                                                                                               0x10,
                                                                                               0x20,
                                                                                               0x30,
                                                                                               0x40,
                                                                                               0x50,
                                                                                               0x60,
                                                                                               0x70,
                                                                                               0x80));
        RequestResult<DM5DiagnosticReadinessPacket> globalRequestResponse = new RequestResult<>(false,
                                                                                                Collections.emptyList(),
                                                                                                Collections.emptyList());
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x00 = new BusResult<>(false,
                                                                                packet0);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x00))).thenReturn(busResult0x00);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x17 = new BusResult<>(false,
                                                                                packet44);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17))).thenReturn(busResult0x17);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x21 = new BusResult<>(false,
                                                                                packet21);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x21))).thenReturn(busResult0x21);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x21));

        runTest();

        verify(dataRepository).getObdModuleAddresses();

        verify(diagnosticMessageModule).setJ1939(j1939);
        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x21));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.2.2.1.a - Global DM5 request did not receive any response packets");

        verify(sectionA6Validator).verify(any(), eq("6.2.2.2.a"), eq(globalRequestResponse));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
    }

    @Test
    public void testPacketFailure() {
        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(PGN,
                                                                                              0x00,
                                                                                              0x01,
                                                                                              0x00,
                                                                                              0x14,
                                                                                              0x37,
                                                                                              0xE0,
                                                                                              0x1E,
                                                                                              0xE0,
                                                                                              0x1E));

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
        DM5DiagnosticReadinessPacket packet21 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(PGN,
                                                                                               0x21,
                                                                                               0x00,
                                                                                               0x20,
                                                                                               0x30,
                                                                                               0x40,
                                                                                               0x50,
                                                                                               0x60,
                                                                                               0x70,
                                                                                               0x80));
        DM5DiagnosticReadinessPacket packet21V2 = new DM5DiagnosticReadinessPacket(
                                                                                   Packet.create(PGN,
                                                                                                 0x21,
                                                                                                 0x00,
                                                                                                 0x22,
                                                                                                 0x33,
                                                                                                 0x44,
                                                                                                 0x55,
                                                                                                 0x60,
                                                                                                 0x70,
                                                                                                 0x80));
        RequestResult<DM5DiagnosticReadinessPacket> globalRequestResponse = new RequestResult<>(false,
                                                                                                List.of(packet0,
                                                                                                        packet21),
                                                                                                List.of(packet44));
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x00 = new BusResult<>(false, packet0);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x00))).thenReturn(busResult0x00);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x17 = new BusResult<>(false, packet44);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17))).thenReturn(busResult0x17);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x21 = new BusResult<>(false, packet21V2);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x21))).thenReturn(busResult0x21);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x21));

        runTest();

        verify(dataRepository).getObdModuleAddresses();

        verify(diagnosticMessageModule).setJ1939(j1939);
        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x21));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.2.2.2.b - OBD ECU Engine #1 (0) reported active DTC count not = 0");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.2.2.2.c - Required monitor Exhaust Gas Sensor heater is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.2.2.4.a - Difference compared to data received during global request");

        verify(sectionA6Validator).verify(any(), eq("6.2.2.2.a"), eq(globalRequestResponse));

        assertEquals("", listener.getMessages());

        String expectedResults = NL + "Vehicle Composite of DM5:" + NL +
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
                "    Secondary air system       not supported,     complete" + NL +
                NL;

        assertEquals(expectedResults, listener.getResults());
    }

    @Test
    public void testStepDM5PacketsEmpty() {
        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(PGN,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x14,
                                                                                              0x37,
                                                                                              0xE0,
                                                                                              0x1E,
                                                                                              0xE0,
                                                                                              0x1E));
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
        DM5DiagnosticReadinessPacket packet21 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(PGN,
                                                                                               0x21,
                                                                                               0x10,
                                                                                               0x20,
                                                                                               0x30,
                                                                                               0x40,
                                                                                               0x50,
                                                                                               0x60,
                                                                                               0x70,
                                                                                               0x80));
        RequestResult<DM5DiagnosticReadinessPacket> globalRequestResponse = new RequestResult<>(false,
                                                                                                Collections.emptyList(),
                                                                                                Collections.singletonList(
                                                                                                                          packet44));
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x00 = new BusResult<>(false,
                                                                                packet0);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x00))).thenReturn(busResult0x00);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x17 = new BusResult<>(false,
                                                                                packet44);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17))).thenReturn(busResult0x17);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x21 = new BusResult<>(false,
                                                                                packet21);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x21))).thenReturn(busResult0x21);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x21));

        runTest();

        verify(dataRepository).getObdModuleAddresses();

        verify(diagnosticMessageModule).setJ1939(j1939);
        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x21));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.2.2.1.a - Global DM5 request did not receive any response packets");

        verify(sectionA6Validator).verify(any(), eq("6.2.2.2.a"), eq(globalRequestResponse));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
    }

    @Test
    public void testStepAllDM5PacketsEmpty() {
        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(PGN,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x14,
                                                                                              0x37,
                                                                                              0xE0,
                                                                                              0x1E,
                                                                                              0xE0,
                                                                                              0x1E));
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
        DM5DiagnosticReadinessPacket packet21 = new DM5DiagnosticReadinessPacket(
                                                                                 Packet.create(PGN,
                                                                                               0x21,
                                                                                               0x10,
                                                                                               0x20,
                                                                                               0x30,
                                                                                               0x40,
                                                                                               0x50,
                                                                                               0x60,
                                                                                               0x70,
                                                                                               0x80));
        RequestResult<DM5DiagnosticReadinessPacket> globalRequestResponse = new RequestResult<>(false,
                                                                                                List.of(packet0,
                                                                                                        packet21),
                                                                                                Collections.singletonList(
                                                                                                                          packet44));
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x00 = new BusResult<>(false,
                                                                                Optional.empty());
        when(diagnosticMessageModule.requestDM5(any(), eq(0x00))).thenReturn(busResult0x00);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x17 = new BusResult<>(false,
                                                                                Optional.empty());
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17))).thenReturn(busResult0x17);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x21 = new BusResult<>(false,
                                                                                packet21);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x21))).thenReturn(busResult0x21);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x21));

        runTest();

        verify(dataRepository).getObdModuleAddresses();

        verify(diagnosticMessageModule).setJ1939(j1939);
        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x21));

        verify(mockListener).addOutcome(
                                        PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.2.2.2.c - Required monitor Exhaust Gas Sensor heater is supported by more than one OBD ECU");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.2.2.3.a - OBD ECU Engine #1 (0) did not return a response to a destination specific DM5 request");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.2.2.3.a - OBD ECU Instrument Cluster #1 (23) did not return a response to a destination specific DM5 request");

        verify(sectionA6Validator).verify(any(), eq("6.2.2.2.a"), eq(globalRequestResponse));

        assertEquals("", listener.getMessages());

        String expectedResults = NL + "Vehicle Composite of DM5:" + NL +
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
                "    Secondary air system       not supported,     complete" + NL + NL;
        assertEquals(expectedResults, listener.getResults());
    }

    @Test
    public void testRun() {
        DM5DiagnosticReadinessPacket packet0 = new DM5DiagnosticReadinessPacket(
                                                                                Packet.create(PGN,
                                                                                              0x00,
                                                                                              0x00,
                                                                                              0x00,
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
        DM5DiagnosticReadinessPacket packet23 = new DM5DiagnosticReadinessPacket(
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
                                                                                                List.of(packet0,
                                                                                                        packet17,
                                                                                                        packet23),
                                                                                                Collections.emptyList());
        when(diagnosticMessageModule.requestDM5(any())).thenReturn(globalRequestResponse);

        BusResult<DM5DiagnosticReadinessPacket> busResult0x00 = new BusResult<>(false, packet0);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x00))).thenReturn(busResult0x00);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x17 = new BusResult<>(false, packet17);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x17))).thenReturn(busResult0x17);
        BusResult<DM5DiagnosticReadinessPacket> busResult0x23 = new BusResult<>(false, packet23);
        when(diagnosticMessageModule.requestDM5(any(), eq(0x23))).thenReturn(busResult0x23);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0x00, 0x17, 0x23));

        runTest();

        verify(dataRepository).getObdModuleAddresses();

        verify(diagnosticMessageModule).setJ1939(j1939);
        verify(diagnosticMessageModule).requestDM5(any());
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x00));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x17));
        verify(diagnosticMessageModule).requestDM5(any(), eq(0x23));

        verify(sectionA6Validator).verify(any(), eq("6.2.2.2.a"), eq(globalRequestResponse));

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
