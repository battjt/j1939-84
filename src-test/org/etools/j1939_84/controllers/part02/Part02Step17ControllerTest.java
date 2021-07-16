/*
 * Copyright (c) 2020. Equipment & Tool Institute
 */

package org.etools.j1939_84.controllers.part02;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.model.FuelType.DSL;
import static org.etools.j1939_84.model.Outcome.WARN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.etools.j1939_84.bus.Packet;
import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.J1939DaRepository;
import org.etools.j1939_84.bus.j1939.packets.DM24SPNSupportPacket;
import org.etools.j1939_84.bus.j1939.packets.GenericPacket;
import org.etools.j1939_84.bus.j1939.packets.SupportedSPN;
import org.etools.j1939_84.bus.j1939.packets.model.Spn;
import org.etools.j1939_84.controllers.BroadcastValidator;
import org.etools.j1939_84.controllers.BusService;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.TableA1Validator;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.model.VehicleInformation;
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

@RunWith(MockitoJUnitRunner.class)
public class Part02Step17ControllerTest extends AbstractControllerTest {

    @Mock
    private Executor executor;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private BannerModule bannerModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    private DataRepository dataRepository;

    @Mock
    private TableA1Validator tableA1Validator;

    @Mock
    private BroadcastValidator broadcastValidator;

    @Mock
    private BusService busService;

    private Part02Step17Controller instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private DiagnosticMessageModule diagnosticMessageModule;

    private static List<SupportedSPN> spns(int... ids) {
        return Arrays.stream(ids).mapToObj(id -> {
            return SupportedSPN.create(id, false, true, false, 1);
        }).collect(Collectors.toList());
    }

    private static GenericPacket packet(int spnId, Boolean isNotAvailable) {
        GenericPacket mock = mock(GenericPacket.class);

        Spn spn = mock(Spn.class);
        when(spn.getId()).thenReturn(spnId);
        if (isNotAvailable != null) {
            when(spn.isNotAvailable()).thenReturn(isNotAvailable);
        }
        when(mock.getSpns()).thenReturn(List.of(spn));

        return mock;
    }

    @Before
    public void setUp() throws Exception {
        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);
        J1939DaRepository j1939DaRepository = J1939DaRepository.getInstance();
        dataRepository = DataRepository.newInstance();

        instance = new Part02Step17Controller(executor,
                                              bannerModule,
                                              DateTimeModule.getInstance(),
                                              dataRepository,
                                              engineSpeedModule,
                                              vehicleInformationModule,
                                              diagnosticMessageModule,
                                              tableA1Validator,
                                              j1939DaRepository,
                                              broadcastValidator,
                                              busService);
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
                                 tableA1Validator,
                                 broadcastValidator,
                                 busService,
                                 mockListener);
    }

   @Test
    public void runWithPgnVerification() {
        // SPNs
        // 111 - Broadcast with value
        // 222 - Not Broadcast not found
        // 333 - Broadcast found with N/A
        // 444 - DS with value
        // 555 - DS No Response
        // 666 - DS found with n/a
        // 222 - Global Request with value
        // 555 - Global Request no response
        // 666 - Global Request with n/a
        List<Integer> supportedSpns = Arrays.asList(111, 222, 333, 444, 555, 666, 777, 888, 999);

        OBDModuleInformation obdModule0 = new OBDModuleInformation(0);
        dataRepository.putObdModule(obdModule0);
        DM24SPNSupportPacket packet0 = DM24SPNSupportPacket.create(0,
                                                                   supportedSpns.stream()
                                                                                .map(spn -> SupportedSPN.create(spn,
                                                                                                                false,
                                                                                                                false,
                                                                                                                false,
                                                                                                                0))
                                                                                .toArray(SupportedSPN[]::new));
        obdModule0.set(packet0, 1);
        OBDModuleInformation obdModule1 = new OBDModuleInformation(0x01);
        dataRepository.putObdModule(obdModule1);
        //@formatter:off
        DM24SPNSupportPacket packet1 = new DM24SPNSupportPacket(Packet.create(DM24SPNSupportPacket.PGN,
                                                                           0x01,
                                                                           0x5C, 0x00, 0x1C, 0x01, 0x00, 0x02, 0x1C, 0x01, 0x01, 0x02, 0x1C, 0x01, 0x20, 0x02, 0x1C,
                                                                           0x02, 0x1B, 0x02, 0x1C, 0x01, 0x1C, 0x02, 0x1C, 0x01, 0x1D, 0x02, 0x1C, 0x01, 0x1E, 0x02,
                                                                           0x1C, 0x01, 0x1F, 0x02, 0x1C, 0x01, 0x6E, 0x00, 0x1C, 0x01, 0xAF, 0x00, 0x1C, 0x02, 0xBE,
                                                                           0x00, 0x1C, 0x02, 0x54, 0x00, 0x1C, 0x02, 0x6C, 0x00, 0x1C, 0x01, 0x9E, 0x00, 0x1C, 0x02,
                                                                           0x33, 0x00, 0x1C, 0x01, 0x5E, 0x00, 0x1C, 0x01, 0xAC, 0x00, 0x1C, 0x01, 0x69, 0x00, 0x1C,
                                                                           0x01, 0x84, 0x00, 0x1C, 0x02, 0xD0, 0x03, 0x1C, 0x01, 0x5B, 0x00, 0x1C, 0x01, 0xB7, 0x00,
                                                                           0x1C, 0x02, 0x66, 0x00, 0x18, 0x01, 0xAD, 0x00, 0x1C, 0x02, 0xB3, 0x0C, 0x1C, 0x02, 0x9B,
                                                                           0x0D, 0x1C, 0x01, 0xCD, 0x16, 0x1C, 0x01, 0xE5, 0x0C, 0x1C, 0x02, 0x5A, 0x15, 0x1C, 0x02,
                                                                           0xCB, 0x14, 0x1C, 0x01, 0x88, 0x0D, 0x1C, 0x02, 0xB9, 0x04, 0x1C, 0x02, 0xA5, 0x15, 0x1C,
                                                                           0x01, 0xA4, 0x00, 0x18, 0x02, 0xE7, 0x0A, 0x1C, 0x02, 0x85, 0x05, 0x1C, 0x02, 0x86, 0x05,
                                                                           0x1C, 0x02, 0x87, 0x05, 0x1C, 0x02, 0x88, 0x05, 0x1C, 0x02, 0x89, 0x05, 0x1C, 0x02, 0x8A,
                                                                           0x05, 0x1C, 0x02, 0xEB, 0x0D, 0x1C, 0x01, 0x1B, 0x00, 0x1C, 0x02, 0xAA, 0x0C, 0x1C, 0x02,
                                                                           0xAE, 0x0C, 0x1C, 0x02, 0x90, 0x0C, 0x18, 0x02, 0x39, 0x04, 0x1C, 0x01, 0xA5, 0x04, 0x1C,
                                                                           0x01, 0xC2, 0x14, 0x1C, 0x02, 0x19, 0x0E, 0x1E, 0x02, 0x98, 0x0D, 0x1E, 0x02, 0x9A, 0x0C,
                                                                           0x1C, 0x02, 0xE1, 0x06, 0x1C, 0x01, 0x9A, 0x0D, 0x1E, 0x01, 0xA2, 0x0D, 0x1E, 0x01, 0x08,
                                                                           0x11, 0x1E, 0x02, 0x0B, 0x11, 0x1E, 0x02, 0xD7, 0x0B, 0x1E, 0x01, 0x45, 0x11, 0x1F, 0x01,
                                                                           0x95, 0x04, 0x18, 0x02, 0xED, 0x00, 0x1D, 0x11, 0xA1, 0x10, 0x1B, 0x01, 0x00, 0x09, 0x1F,
                                                                           0x01, 0x83, 0x03, 0x1D, 0x01, 0x01, 0x09, 0x1F, 0x01, 0xFD, 0x0B, 0x1D, 0x01, 0xDE, 0x0C,
                                                                           0x1D, 0x01, 0xDF, 0x0C, 0x1D, 0x01, 0xE0, 0x0C, 0x1D, 0x01, 0xE6, 0x0C, 0x1D, 0x01, 0x87,
                                                                           0x0E, 0x1D, 0x01, 0x2B, 0x05, 0x1B, 0x01, 0x2C, 0x05, 0x1B, 0x01, 0x2D, 0x05, 0x1B, 0x01,
                                                                           0x2E, 0x05, 0x1B, 0x01, 0x2F, 0x05, 0x1B, 0x01, 0x30, 0x05, 0x1B, 0x01, 0x46, 0x0A, 0x1B,
                                                                           0x02, 0x63, 0x0A, 0x1B, 0x02, 0x2A, 0x05, 0x1B, 0x01, 0x90, 0x12, 0x1B, 0x01, 0x9E, 0x12,
                                                                           0x1F, 0x02, 0xC7, 0x14, 0x1F, 0x01, 0x8B, 0x02, 0x1B, 0x01, 0x8C, 0x02, 0x1B, 0x01, 0x8D,
                                                                           0x02, 0x1B, 0x01, 0x8E, 0x02, 0x1B, 0x01, 0x8F, 0x02, 0x1B, 0x01, 0x90, 0x02, 0x1B, 0x01,
                                                                           0x8F, 0x0D, 0x1B, 0x01, 0xE4, 0x0D, 0x1B, 0x01, 0x03, 0x09, 0x1F, 0x01, 0x0A, 0x10, 0x1D,
                                                                           0x01, 0xC0, 0x04, 0x1D, 0x01, 0xC4, 0x04, 0x1D, 0x01, 0x1F, 0x10, 0x1D, 0x01, 0x22, 0x10,
                                                                           0x1D, 0x01, 0xAB, 0x00, 0x1D, 0x02, 0x9D, 0x00, 0x1F, 0x02, 0x3F, 0x0A, 0x1D, 0x01, 0xF7,
                                                                           0x00, 0x1D, 0x04, 0xEB, 0x00, 0x1D, 0x04, 0xBD, 0x04, 0x1D, 0x01, 0xE7, 0x0C, 0x1D, 0x01,
                                                                           0xE8, 0x0C, 0x1D, 0x01, 0x4E, 0x15, 0x1D, 0x04, 0x4C, 0x02, 0x1D, 0x11, 0xEF, 0x0B, 0x1F,
                                                                           0x01, 0x57, 0x15, 0x1D, 0x01, 0xF8, 0x00, 0x1D, 0x04, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00,
                                                                           0x1F, 0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00, 0x1F, 0x00, 0x00,
                                                                           0x00, 0x1F, 0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00, 0x1F, 0x00));
        //@formatter:on
        obdModule1.set(packet1, 1);

        when(broadcastValidator.collectAndReportNotAvailableSPNs(any(),
                                                                 anyInt(),
                                                                 any(),
                                                                 eq(0),
                                                                 any(ResultsListener.class),
                                                                 eq(2),
                                                                 eq(17),
                                                                 eq("6.2.17.6.a")))
                                                                                   .thenReturn(List.of("111"));
        OBDModuleInformation obdModule17 = new OBDModuleInformation(17);
        dataRepository.putObdModule(obdModule17);
        DM24SPNSupportPacket packet17 = DM24SPNSupportPacket.create(17,
                                                                    supportedSpns.stream()
                                                                                 .map(spn -> SupportedSPN.create(spn,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 false,
                                                                                                                 0))
                                                                                 .toArray(SupportedSPN[]::new));
        obdModule17.set(packet17, 1);

        when(busService.collectNonOnRequestPGNs(any())).thenReturn(List.of(11111, 22222, 33333));

        List<Integer> pgns = List.of(22222,
                                     44444,
                                     55555,
                                     66666);
        when(busService.getPGNsForDSRequest(any(), any())).thenReturn(pgns);

        List<GenericPacket> dsPackets = new ArrayList<>();
        GenericPacket packet4 = packet(444, false);
        dsPackets.add(packet4);
        when(busService.dsRequest(eq(44444), eq(0), any())).thenReturn(Stream.of(packet4));

        GenericPacket packet5 = packet(555, false);
        dsPackets.add(packet5);
        when(busService.dsRequest(eq(55555), eq(0), any())).thenReturn(Stream.of(packet5));


        GenericPacket packet6 = packet(666, true);
        dsPackets.add(packet6);
        when(busService.dsRequest(eq(66666), eq(0), any())).thenReturn(Stream.of(packet6));

        GenericPacket packet2 = packet(222, false);
        when(busService.globalRequest(eq(22222), any())).thenReturn(Stream.of(packet2));

        runTest();

        verify(busService).setup(eq(j1939), any());
        verify(busService, times(3)).collectNonOnRequestPGNs(any());
        verify(busService, times(3)).getPGNsForDSRequest(any(), any());
        verify(busService).dsRequest(eq(22222), eq(0), any());
        verify(busService).dsRequest(eq(22222), eq(1), any());
        verify(busService).dsRequest(eq(44444), eq(0), any());
        verify(busService).dsRequest(eq(55555), eq(0), any());
        verify(busService).dsRequest(eq(66666), eq(0), any());

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(eq(List.of()));
        verify(broadcastValidator).reportBroadcastPeriod(any(),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(2),
                                                         eq(17));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    any(),
                                                                    eq(List.of()),
                                                                    eq(List.of(11111, 22222, 33333)),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    eq(List.of()),
                                                                    any(),
                                                                    eq(List.of(11111, 22222, 33333)),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(17),
                                                                    eq(List.of()),
                                                                    any(),
                                                                    eq(List.of(11111, 22222, 33333)),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));
        verify(broadcastValidator, times(12)).collectAndReportNotAvailableSPNs(any(),
                                                                               anyInt(),
                                                                               any(),
                                                                               anyInt(),
                                                                               any(ResultsListener.class),
                                                                               eq(2),
                                                                               eq(17),
                                                                               eq("6.2.17.6.a"));

        pgns.forEach(pgn -> {
            verify(busService).globalRequest(eq(pgn), any());
            verify(busService).dsRequest(eq(pgn), eq(0), any());
            verify(busService).dsRequest(eq(pgn), eq(1), any());
            verify(busService).dsRequest(eq(pgn), eq(17), any());

            verify(broadcastValidator).collectAndReportNotAvailableSPNs(any(),
                                                                        eq(pgn),
                                                                        any(),
                                                                        eq(0),
                                                                        any(ResultsListener.class),
                                                                        eq(2),
                                                                        eq(17),
                                                                        eq("6.2.17.6.a"));
            verify(broadcastValidator).collectAndReportNotAvailableSPNs(any(),
                                                                        eq(pgn),
                                                                        any(),
                                                                        isNull(),
                                                                        any(ResultsListener.class),
                                                                        eq(2),
                                                                        eq(17),
                                                                        eq("6.2.17.6.a"));

        });

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService).readBus(eq(0),
                                   eq("6.2.17.1.a"));
        verify(busService).globalRequest(eq(22222), any());
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(any(),
                                                                    eq(22222),
                                                                    any(),
                                                                    eq(0),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.6.a"));

        verify(mockListener).addOutcome(eq(2),
                                        eq(17),
                                        eq(WARN),
                                        eq("6.2.17.6.c - Global request was required for PGN 22222 for broadcast SPNs 111"));
        verify(mockListener).addOutcome(eq(2),
                                        eq(17),
                                        eq(WARN),
                                        eq("6.2.17.6.c - Global request was required for PGN 44444 for broadcast SPNs 111"));
        verify(mockListener).addOutcome(eq(2),
                                        eq(17),
                                        eq(WARN),
                                        eq("6.2.17.6.c - Global request was required for PGN 55555 for broadcast SPNs 111"));
        verify(mockListener).addOutcome(eq(2),
                                        eq(17),
                                        eq(WARN),
                                        eq("6.2.17.6.c - Global request was required for PGN 66666 for broadcast SPNs 111"));
        verify(tableA1Validator).reportExpectedMessages(any(ResultsListener.class));
        verify(tableA1Validator).reportDuplicateSPNs(eq(List.of()),
                                                     any(ResultsListener.class),
                                                     eq("6.2.17.2.d"));
        dsPackets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet),
                                                           any(ResultsListener.class),
                                                           eq("6.2.17.6.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                        any(ResultsListener.class),
                                                        eq(true),
                                                        eq("6.2.17.6.b"));
        });
        verify(tableA1Validator).reportNotAvailableSPNs(eq(packet2),
                                                        any(ResultsListener.class),
                                                        eq("6.2.17.6.a"));
        verify(tableA1Validator).reportImplausibleSPNValues(eq(packet2),
                                                            any(ResultsListener.class),
                                                            eq(true),
                                                            eq("6.2.17.6.b"));

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "Test 2.17 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 2.17 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 2.17 - Verifying Cruise Control (17)" + NL;
        expectedMsg += "Test 2.17 - Verifying Cruise Control (17)" + NL;
        expectedMsg += "Test 2.17 - Verifying Cruise Control (17)" + NL;
        expectedMsg += "Test 2.17 - Verifying Cruise Control (17)";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void runWithUnExpectedToolSaMsg() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(DSL);
        dataRepository.setVehicleInformation(vehicleInformation);
        // SPNs
        // 111 - Broadcast with value
        // 222 - Not Broadcast not found
        // 333 - Broadcast found with N/A
        // 444 - DS with value
        // 555 - DS No Response
        // 666 - DS found with n/a
        // 222 - Global Request with value
        // 555 - Global Request no response
        // 666 - Global Request with n/a
        List<Integer> supportedSpns = Arrays.asList(111, 222, 333, 444, 555, 666, 777, 888, 999);
        List<SupportedSPN> supportedSPNList = spns(111, 222, 333, 444, 555, 666, 777, 888, 999);

        OBDModuleInformation obdModule0 = new OBDModuleInformation(0);
        obdModule0.set(DM24SPNSupportPacket.create(0,
                                                   SupportedSPN.create(111,
                                                                       false,
                                                                       true,
                                                                       false,
                                                                       2)),
                       1);
        dataRepository.putObdModule(obdModule0);
        OBDModuleInformation obdModule1 = new OBDModuleInformation(1);
        obdModule1.set(DM24SPNSupportPacket.create(1, supportedSPNList.toArray(new SupportedSPN[0])),
                       1);
        dataRepository.putObdModule(obdModule1);

        OBDModuleInformation obdModule17 = new OBDModuleInformation(17);
        obdModule17.set(DM24SPNSupportPacket.create(17,
                                                    SupportedSPN.create(111,
                                                                        false,
                                                                        false,
                                                                        false,
                                                                        2)),
                        1);
        dataRepository.putObdModule(obdModule17);
        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        GenericPacket packet3 = packet(333, true);
        packets.add(packet3);
        GenericPacket packet8 = packet(888, true);
        packets.add(packet8);
        when(busService.readBus(eq(12), eq("6.2.17.1.a"))).thenAnswer(a -> {
            instance.stop();
            return packets.stream();
        });

        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap = new HashMap<>();
        packetMap.put(11111, Map.of(0, List.of(packet1)));
        packetMap.put(33333, Map.of(0, List.of(packet3)));
        when(broadcastValidator.buildPGNPacketsMap(packets)).thenReturn(packetMap);
        List<Integer> onRequestPgns = Arrays.asList(11111, 22222, 33333);

        when(busService.collectNonOnRequestPGNs(any())).thenReturn(onRequestPgns);

        runTest();

        verify(busService).setup(eq(j1939), any());
        verify(busService).readBus(eq(12), eq("6.2.17.1.a"));
        verify(busService, times(3)).collectNonOnRequestPGNs(any());
        verify(busService, times(2)).getPGNsForDSRequest(eq(List.of()), eq(List.of()));
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(supportedSpns));

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(eq(packets));
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap),
                                                         eq(List.of(111, 111, 222, 333, 444, 555, 666, 777, 888, 999)),
                                                         any(ResultsListener.class),
                                                         eq(2),
                                                         eq(17));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    eq(packets),
                                                                    eq(List.of()),
                                                                    eq(onRequestPgns),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    eq(List.of()),
                                                                    eq(supportedSpns),
                                                                    eq(onRequestPgns),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(17),
                                                                    eq(List.of()),
                                                                    eq(List.of()),
                                                                    eq(onRequestPgns),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService,
               times(3)).collectNonOnRequestPGNs(eq(List.of(111, 111, 222, 333, 444, 555, 666, 777, 888, 999)));
        verify(busService, times(2)).getPGNsForDSRequest(eq(List.of()), eq(List.of()));
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(supportedSpns));
        verify(busService, times(2)).getPGNsForDSRequest(eq(List.of()), eq(List.of()));

        verify(tableA1Validator).reportExpectedMessages(any());
        packets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet), any(ResultsListener.class), eq("6.2.17.2.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(true),
                                                                eq("6.2.17.2.b"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(true),
                                                                eq("6.2.17.2.b"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                    any(ResultsListener.class),
                                                                    eq("6.2.17.2.c"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                    any(ResultsListener.class),
                                                                    eq("6.2.17.2.c"));
            verify(tableA1Validator).reportProvidedButNotSupportedSPNs(eq(packet),
                                                                       any(ResultsListener.class),
                                                                       eq("6.2.17.4.a"));
            verify(tableA1Validator).reportPacketIfNotReported(eq(packet), any(ResultsListener.class), eq(false));
        });
        verify(tableA1Validator, atLeastOnce()).reportDuplicateSPNs(any(),
                                                                    any(ResultsListener.class),
                                                                    eq("6.2.17.2.d"));
        assertEquals(dataRepository.getObdModuleAddresses(), List.of(0, 1, 17));

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void runWithInterruptedExceptionToForcePacketStreamClose() {
        // SPNs
        // 111 - Broadcast with value
        // 444 - DS with value
        List<SupportedSPN> supportedSPNList = spns(111, 444);

        OBDModuleInformation obdModule0 = new OBDModuleInformation(0);
        obdModule0.set(DM24SPNSupportPacket.create(0,
                                                   SupportedSPN.create(111,
                                                                       false,
                                                                       true,
                                                                       false,
                                                                       1)),
                       1);
        dataRepository.putObdModule(obdModule0);

        OBDModuleInformation obdModule1 = new OBDModuleInformation(1);
        obdModule1.set(DM24SPNSupportPacket.create(1,
                                                   supportedSPNList.toArray(new SupportedSPN[0])),
                       1);
        dataRepository.putObdModule(obdModule1);

        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        when(busService.readBus(12, "6.2.17.1.a")).thenReturn(packets.stream());

        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap = new HashMap<>();
        packetMap.put(11111, Map.of(0, List.of(packet1)));
        when(broadcastValidator.buildPGNPacketsMap(packets)).thenReturn(packetMap);

        runTest();

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(packets);
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(2),
                                                         eq(17));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    any(),
                                                                    eq(List.of()),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    eq(List.of()),
                                                                    eq(List.of(111, 444)),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService).readBus(eq(12), eq("6.2.17.1.a"));
        verify(busService, times(2))
                                    .collectNonOnRequestPGNs(eq(List.of(111, 111, 444)));

        verify(busService).getPGNsForDSRequest(List.of(), List.of(111, 444));
        verify(busService).getPGNsForDSRequest(List.of(), List.of());

        verify(tableA1Validator).reportExpectedMessages(any(ResultsListener.class));
        packets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet),
                                                            any(ResultsListener.class),
                                                            eq("6.2.17.2.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(true),
                                                                eq("6.2.17.2.b"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                    any(ResultsListener.class),
                                                                    eq("6.2.17.2.c"));
            verify(tableA1Validator).reportProvidedButNotSupportedSPNs(eq(packet),
                                                                       any(ResultsListener.class),
                                                                       eq("6.2.17.4.a"));
            verify(tableA1Validator).reportPacketIfNotReported(any(GenericPacket.class),
                                                               any(ResultsListener.class),
                                                               eq(false));

            verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                         any(ResultsListener.class),
                                                         eq("6.2.17.2.d"));
        });

        verify(tableA1Validator).reportPacketIfNotReported(any(), any(ResultsListener.class), eq(false));

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void runWithUiInterruption() {
        // SPNs
        // 111 - Broadcast with value
        // 444 - DS with value
        List<SupportedSPN> supportedSPNList = spns(111, 444);

        OBDModuleInformation obdModule0 = new OBDModuleInformation(0);
        obdModule0.set(DM24SPNSupportPacket.create(0,
                                                   SupportedSPN.create(111,
                                                                       false,
                                                                       true,
                                                                       false,
                                                                       1)),
                       1);
        dataRepository.putObdModule(obdModule0);

        OBDModuleInformation obdModule1 = new OBDModuleInformation(1);
        obdModule1.set(DM24SPNSupportPacket.create(1,
                                                   supportedSPNList.toArray(new SupportedSPN[0])),
                       1);

        dataRepository.putObdModule(obdModule1);

        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        when(busService.readBus(eq(12), eq("6.2.17.1.a"))).thenAnswer(a -> {
            instance.stop();
            return packets.stream();
        });

        runTest();

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(List.of(packet1));
        verify(broadcastValidator).reportBroadcastPeriod(any(),
                                                         eq(List.of(111, 111, 444)),
                                                         any(ResultsListener.class),
                                                         eq(2),
                                                         eq(17));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    any(),
                                                                    eq(List.of()),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(
                                                                    eq(1),
                                                                    eq(List.of()),
                                                                    eq(List.of(111, 444)),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    eq(List.of()),
                                                                    eq(List.of(111, 444)),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(2),
                                                                    eq(17),
                                                                    eq("6.2.17.2.a"));

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService).readBus(eq(12), eq("6.2.17.1.a"));
        verify(busService, times(2))
                                    .collectNonOnRequestPGNs(eq(List.of(111, 111, 444)));
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(List.of()));
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(List.of(111, 444)));

        verify(tableA1Validator).reportExpectedMessages(any(ResultsListener.class));
        packets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet), any(ResultsListener.class), eq("6.2.17.2.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(true),
                                                                eq("6.2.17.2.b"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                    any(ResultsListener.class),
                                                                    eq("6.2.17.2.c"));
            verify(tableA1Validator).reportProvidedButNotSupportedSPNs(eq(packet),
                                                                       any(ResultsListener.class),
                                                                       eq("6.2.17.4.a"));
            verify(tableA1Validator).reportPacketIfNotReported(eq(packet), any(ResultsListener.class), eq(false));
            verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                         any(ResultsListener.class),
                                                         eq("6.2.17.2.d"));

        });

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Display Name", "Part 2 Step 17", instance.getDisplayName());
    }

    @Test
    public void testGetPartNumber() {
        assertEquals("Part Number", 2, instance.getPartNumber());
    }

    @Test
    public void testGetStepNumber() {
        assertEquals(17, instance.getStepNumber());
    }

    @Test
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 0, instance.getTotalSteps());
    }

}
