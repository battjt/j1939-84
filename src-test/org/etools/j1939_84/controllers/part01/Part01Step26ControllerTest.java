/*
 * Copyright (c) 2020. Equipment & Tool Institute
 */

package org.etools.j1939_84.controllers.part01;

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

import org.etools.j1939_84.bus.Bus;
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
public class Part01Step26ControllerTest extends AbstractControllerTest {

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

    private Part01Step26Controller instance;

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

        instance = new Part01Step26Controller(executor,
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
    public void runWithFailures() {
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

        OBDModuleInformation obdModule = new OBDModuleInformation(0);
        dataRepository.putObdModule(obdModule);
        DM24SPNSupportPacket obdDM24Packet = DM24SPNSupportPacket.create(0,
                                                                         supportedSpns.stream()
                                                                                      .map(num -> SupportedSPN.create(num,
                                                                                                                      true,
                                                                                                                      true,
                                                                                                                      true,
                                                                                                                      1))
                                                                                      .toArray(SupportedSPN[]::new));
        obdModule.set(obdDM24Packet, 1);

        when(broadcastValidator.collectAndReportNotAvailableSPNs(any(),
                                                                 anyInt(),
                                                                 any(),
                                                                 eq(0),
                                                                 any(ResultsListener.class),
                                                                 eq(1),
                                                                 eq(26),
                                                                 eq("6.1.26.6.a")))
                                                                                   .thenReturn(List.of("111"));

        OBDModuleInformation module1 = new OBDModuleInformation(1);
        DM24SPNSupportPacket dm24SPNSupportPacket = DM24SPNSupportPacket.create(1,
                                                                                SupportedSPN.create(111,
                                                                                                    false,
                                                                                                    true,
                                                                                                    false,
                                                                                                    1));
        module1.set(dm24SPNSupportPacket, 1);
        dataRepository.putObdModule(module1);

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

        Bus busMock = mock(Bus.class);
        when(j1939.getBus()).thenReturn(busMock);
        when(busMock.imposterDetected()).thenReturn(false);
        runTest();

        verify(busService).setup(eq(j1939), any());
        verify(busService, times(2)).collectNonOnRequestPGNs(any());
        verify(busService, times(2)).getPGNsForDSRequest(any(), any());
        verify(busService).dsRequest(eq(22222), eq(0), any());
        verify(busService).dsRequest(eq(44444), eq(0), any());
        verify(busService).dsRequest(eq(55555), eq(0), any());
        verify(busService).dsRequest(eq(66666), eq(0), any());

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(eq(List.of()));
        verify(broadcastValidator).reportBroadcastPeriod(any(),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    any(),
                                                                    any(),
                                                                    eq(List.of(11111, 22222, 33333)),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    any(),
                                                                    any(),
                                                                    eq(List.of(11111, 22222, 33333)),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));

        pgns.forEach(pgn -> {
            verify(broadcastValidator).collectAndReportNotAvailableSPNs(any(),
                                                                        eq(pgn),
                                                                        any(),
                                                                        eq(0),
                                                                        any(ResultsListener.class),
                                                                        eq(1),
                                                                        eq(26),
                                                                        eq("6.1.26.6.a"));
            verify(broadcastValidator).collectAndReportNotAvailableSPNs(any(),
                                                                        eq(pgn),
                                                                        any(),
                                                                        eq(1),
                                                                        any(ResultsListener.class),
                                                                        eq(1),
                                                                        eq(26),
                                                                        eq("6.1.26.6.a"));
            verify(broadcastValidator).collectAndReportNotAvailableSPNs(any(),
                                                                        eq(pgn),
                                                                        any(),
                                                                        isNull(),
                                                                        any(ResultsListener.class),
                                                                        eq(1),
                                                                        eq(26),
                                                                        eq("6.1.26.6.a"));
            verify(busService).dsRequest(eq(pgn), eq(0), any());
            verify(busService).dsRequest(eq(pgn), eq(1), any());
        });

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        pgns.forEach(pgn -> {
            verify(busService).globalRequest(eq(pgn), any());
            verify(busService).dsRequest(eq(pgn), eq(0), any());
        });
        verify(busService).readBus(eq(0),
                                   eq("6.1.26.1.a"));

        verify(tableA1Validator).reportExpectedMessages(any(ResultsListener.class));
        verify(tableA1Validator).reportNotAvailableSPNs(eq(packet2),
                                                        any(ResultsListener.class),
                                                        eq("6.1.26.6.a"));
        verify(tableA1Validator).reportNotAvailableSPNs(eq(packet4),
                                                        any(ResultsListener.class),
                                                        eq("6.1.26.6.a"));
        verify(tableA1Validator).reportImplausibleSPNValues(eq(packet2),
                                                            any(ResultsListener.class),
                                                            eq(false),
                                                            eq("6.1.26.6.d"));
        verify(tableA1Validator).reportImplausibleSPNValues(eq(packet4),
                                                            any(ResultsListener.class),
                                                            eq(false),
                                                            eq("6.1.26.6.d"));
        verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet2),
                                                                any(ResultsListener.class),
                                                                eq("6.1.26.6.e"));
        verify(tableA1Validator).reportDuplicateSPNs(eq(List.of()),
                                                     any(ResultsListener.class),
                                                     eq("6.1.26.2.f"));

        dsPackets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet), any(ResultsListener.class), eq("6.1.26.6.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(false),
                                                                eq("6.1.26.6.d"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                    any(ResultsListener.class),
                                                                    eq("6.1.26.6.e"));

        });

        verify(tableA1Validator).reportDuplicateSPNs(eq(List.of()),
                                                     any(ResultsListener.class),
                                                     eq("6.1.26.2.f"));

        verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                     any(ResultsListener.class),
                                                     eq("6.1.26.6.f"));

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "Test 1.26 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #1 (0)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #2 (1)" + NL;
        expectedMsg += "Test 1.26 - Verifying Engine #2 (1)";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void runWithFailuresUnExpectedToolSa() {
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
                                                                       1)),
                       1);
        dataRepository.putObdModule(obdModule0);
        OBDModuleInformation obdModule1 = new OBDModuleInformation(0);
        obdModule1.set(DM24SPNSupportPacket.create(1, supportedSPNList.toArray(new SupportedSPN[0])),
                       1);
        dataRepository.putObdModule(obdModule1);

        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        GenericPacket packet3 = packet(333, true);
        packets.add(packet3);
        GenericPacket packet8 = packet(888, true);
        packets.add(packet8);
        when(busService.readBus(12, "6.1.26.1.a")).thenReturn(packets.stream());

        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap = new HashMap<>();
        packetMap.put(11111, Map.of(0, List.of(packet1)));
        packetMap.put(33333, Map.of(0, List.of(packet3)));
        when(broadcastValidator.buildPGNPacketsMap(packets)).thenReturn(packetMap);

        when(busService.collectNonOnRequestPGNs(supportedSpns))
                                                               .thenReturn(List.of(11111, 22222, 33333));

        Bus busMock = mock(Bus.class);
        when(j1939.getBus()).thenReturn(busMock);
        when(busMock.imposterDetected()).thenReturn(true);
        runTest();

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(packets);
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));
        packets.forEach(packet -> {
            verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(packet.getSourceAddress()),
                                                         any(),
                                                                        eq(supportedSpns.subList(1,
                                                                                                 supportedSpns.size())),
                                                                        eq(List.of(11111, 22222, 33333)),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                                        eq(26),
                                                                        eq("6.1.26.2.a"));
        });
        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService).readBus(12, "6.1.26.1.a");
        verify(busService).collectNonOnRequestPGNs(supportedSpns);
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(supportedSpns.subList(1, supportedSpns.size())));
        verify(busService).getPGNsForDSRequest(any(), any());

        verify(mockListener).addOutcome(1,
                                        26,
                                        WARN,
                                        "6.1.26 - Unexpected Service Tool Message from SA 0xF9 observed. Test results uncertain. False failures are possible");
        verify(mockListener).onUrgentMessage(eq("6.1.26 - Unexpected Service Tool Message from SA 0xF9 observed. Test results uncertain. False failures are possible"),
                                             eq("Second device using SA 0xF9"),
                                             eq(ResultsListener.MessageType.ERROR),
                                             any());
        verify(tableA1Validator).reportExpectedMessages(any());
        verify(tableA1Validator, atLeastOnce()).reportNotAvailableSPNs(any(),
                                                                any(ResultsListener.class),
                                                                any());
        verify(tableA1Validator, atLeastOnce()).reportImplausibleSPNValues(any(),
                                                            any(ResultsListener.class),
                                                            eq(false),
                                                            any());
        verify(tableA1Validator, atLeastOnce()).reportNonObdModuleProvidedSPNs(any(),
                                                                               any(ResultsListener.class),
                                                                               any());
        verify(tableA1Validator, atLeastOnce()).reportProvidedButNotSupportedSPNs(any(),
                                                                                  any(ResultsListener.class),
                                                                                  any());
        verify(tableA1Validator, atLeastOnce()).reportPacketIfNotReported(any(),
                                                                          any(ResultsListener.class),
                                                                          eq(false));
        verify(tableA1Validator, atLeastOnce()).reportDuplicateSPNs(any(), any(ResultsListener.class), any());

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void runWithoutFailures() {
        // SPNs
        // 111 - Broadcast with value
        // 444 - DS with value
        List<SupportedSPN> supportedSPNList = spns(111, 444);

        OBDModuleInformation obdModule0 = new OBDModuleInformation(0);
        OBDModuleInformation obdModule1 = new OBDModuleInformation(1);
        obdModule0.set(DM24SPNSupportPacket.create(0,
                                                   SupportedSPN.create(111,
                                                                       false,
                                                                       true,
                                                                       false,
                                                                       1)),
                       1);
        obdModule1.set(DM24SPNSupportPacket.create(1,
                                                   supportedSPNList.toArray(new SupportedSPN[0])),
                       1);

        dataRepository.putObdModule(obdModule0);
        dataRepository.putObdModule(obdModule1);

        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        when(busService.readBus(12, "6.1.26.1.a")).thenReturn(packets.stream());

        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap = new HashMap<>();
        packetMap.put(11111, Map.of(0, List.of(packet1)));
        when(broadcastValidator.buildPGNPacketsMap(packets)).thenReturn(packetMap);

        Bus busMock = mock(Bus.class);
        when(j1939.getBus()).thenReturn(busMock);
        when(busMock.imposterDetected()).thenReturn(false);
        runTest();

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(packets);
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    any(),
                                                                    eq(List.of()),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    eq(List.of()),
                                                                    eq(List.of(111, 444)),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService).readBus(eq(12), eq("6.1.26.1.a"));
        verify(busService, times(2))
                                    .collectNonOnRequestPGNs(eq(List.of(111, 111, 444)));

        verify(busService).getPGNsForDSRequest(List.of(), List.of(111, 444));
        verify(busService).getPGNsForDSRequest(List.of(), List.of());

        verify(tableA1Validator).reportExpectedMessages(any(ResultsListener.class));
        packets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet),
                                                            any(ResultsListener.class),
                                                            eq("6.1.26.2.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(false),
                                                                eq("6.1.26.2.d"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                    any(ResultsListener.class),
                                                                    eq("6.1.26.2.e"));
            verify(tableA1Validator).reportProvidedButNotSupportedSPNs(eq(packet),
                                                                       any(ResultsListener.class),
                                                                       eq("6.1.26.4.a"));
            verify(tableA1Validator).reportPacketIfNotReported(any(GenericPacket.class),
                                                               any(ResultsListener.class),
                                                               eq(false));

            verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                         any(ResultsListener.class),
                                                         eq("6.1.26.2.f"));

        });
        verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                     any(ResultsListener.class),
                                                     eq("6.1.26.6.f"));

        verify(tableA1Validator).reportPacketIfNotReported(any(), any(ResultsListener.class), eq(false));

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void runWithInterruptFailure() {
        // SPNs
        // 111 - Broadcast with value
        // 444 - DS with value
        List<Integer> supportedSpns = Arrays.asList(111, 444);
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

        when(busService.readBus(eq(12), eq("6.1.26.1.a"))).thenAnswer(a -> {
            instance.stop();
            return packets.stream();
        });

        Bus busMock = mock(Bus.class);
        when(j1939.getBus()).thenReturn(busMock);
        when(busMock.imposterDetected()).thenReturn(false);
        runTest();

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(broadcastValidator).buildPGNPacketsMap(List.of(packet1));
        verify(broadcastValidator).reportBroadcastPeriod(any(),
                                                         eq(List.of(111, 111, 444)),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(0),
                                                                    any(),
                                                                    eq(List.of()),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));
        verify(broadcastValidator).collectAndReportNotAvailableSPNs(
                                                                    eq(1),
                                                                    eq(List.of()),
                                                                    eq(supportedSpns),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));

        verify(broadcastValidator).collectAndReportNotAvailableSPNs(eq(1),
                                                                    eq(List.of()),
                                                                    eq(List.of(111, 444)),
                                                                    eq(List.of()),
                                                                    any(ResultsListener.class),
                                                                    eq(1),
                                                                    eq(26),
                                                                    eq("6.1.26.2.a"));

        verify(busService).setup(eq(j1939), any(ResultsListener.class));
        verify(busService).readBus(eq(12), eq("6.1.26.1.a"));
        verify(busService, times(2))
                                    .collectNonOnRequestPGNs(eq(List.of(111, 111, 444)));
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(List.of()));
        verify(busService).getPGNsForDSRequest(eq(List.of()), eq(List.of(111, 444)));

        verify(tableA1Validator).reportExpectedMessages(any(ResultsListener.class));
        packets.forEach(packet -> {
            verify(tableA1Validator).reportNotAvailableSPNs(eq(packet),
                                                            any(ResultsListener.class),
                                                            eq("6.1.26.2.a"));
            verify(tableA1Validator).reportImplausibleSPNValues(eq(packet),
                                                                any(ResultsListener.class),
                                                                eq(false),
                                                                eq("6.1.26.2.d"));
            verify(tableA1Validator).reportNonObdModuleProvidedSPNs(eq(packet),
                                                                any(ResultsListener.class),
                                                                    eq("6.1.26.2.e"));
            verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                         any(ResultsListener.class),
                                                         eq("6.1.26.2.f"));
            verify(tableA1Validator).reportProvidedButNotSupportedSPNs(eq(packet),
                                                                       any(ResultsListener.class),
                                                                       eq("6.1.26.4.a"));
            verify(tableA1Validator).reportPacketIfNotReported(eq(packet),
                                                               any(ResultsListener.class),
                                                               eq(false));
        });

        verify(tableA1Validator).reportImplausibleSPNValues(any(),
                                                            any(ResultsListener.class),
                                                            eq(false),
                                                            eq("6.1.26.2.d"));
        verify(tableA1Validator).reportDuplicateSPNs(any(),
                                                     any(ResultsListener.class),
                                                     eq("6.1.26.6.f"));

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Display Name", "Part 1 Step 26", instance.getDisplayName());
    }

    @Test
    public void testGetPartNumber() {
        assertEquals("Part Number", 1, instance.getPartNumber());
    }

    @Test
    public void testGetStepNumber() {
        assertEquals(26, instance.getStepNumber());
    }

    @Test
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 0, instance.getTotalSteps());
    }

}
