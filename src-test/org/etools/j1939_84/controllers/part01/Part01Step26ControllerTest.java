/*
 * Copyright (c) 2020. Equipment & Tool Institute
 */

package org.etools.j1939_84.controllers.part01;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.model.FuelType.DSL;
import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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

import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.J1939DaRepository;
import org.etools.j1939_84.bus.j1939.packets.GenericPacket;
import org.etools.j1939_84.bus.j1939.packets.SupportedSPN;
import org.etools.j1939_84.bus.j1939.packets.model.PgnDefinition;
import org.etools.j1939_84.bus.j1939.packets.model.Spn;
import org.etools.j1939_84.bus.j1939.packets.model.SpnDefinition;
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
import org.junit.Ignore;
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

    @Mock
    private DataRepository dataRepository;

    @Mock
    private J1939DaRepository j1939DaRepository;

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
            SupportedSPN spn = mock(SupportedSPN.class);
            when(spn.getSpn()).thenReturn(id);
            return spn;
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

    private static PgnDefinition pgnDef(int... spns) {
        PgnDefinition mock = mock(PgnDefinition.class);
        List<SpnDefinition> spnDefs = Arrays.stream(spns)
                                            .mapToObj(s -> {
                                                SpnDefinition spn = mock(SpnDefinition.class);
                                                when(spn.getSpnId()).thenReturn(s);
                                                return spn;
                                            })
                                            .collect(Collectors.toList());
        when(mock.getSpnDefinitions()).thenReturn(spnDefs);
        return mock;
    }

    @Before
    public void setUp() throws Exception {
        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);

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
                                 j1939DaRepository,
                                 dataRepository,
                                 broadcastValidator,
                                 busService,
                                 mockListener);
    }

    @Ignore("Fix once Step 26 is accepted")
    @Test
    public void runWithFailures() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(DSL);
        when(dataRepository.getVehicleInformation()).thenReturn(vehicleInformation);

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

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0));

        OBDModuleInformation obdModule = mock(OBDModuleInformation.class);
        when(dataRepository.getObdModules()).thenReturn(List.of(obdModule));
        when(obdModule.getSourceAddress()).thenReturn(0);
        List<SupportedSPN> supportedSPNList = spns(111, 222, 333, 444, 555, 666, 777, 888, 999);
        when(obdModule.getFilteredDataStreamSPNs()).thenReturn(supportedSPNList);

        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        GenericPacket packet3 = packet(333, true);
        packets.add(packet3);
        when(busService.readBus(12, "6.1.26.1.a")).thenReturn(packets.stream());

        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap = new HashMap<>();
        packetMap.put(11111, Map.of(0, List.of(packet1)));
        packetMap.put(33333, Map.of(0, List.of(packet3)));
        when(broadcastValidator.buildPGNPacketsMap(packets)).thenReturn(packetMap);

        when(busService.collectNonOnRequestPGNs(supportedSpns))
                                                               .thenReturn(List.of(11111, 22222, 33333));
        when(busService.collectNonOnRequestPGNs(List.of(222))).thenReturn(List.of(22222));

        PgnDefinition pgnDef2 = pgnDef(222);
        when(j1939DaRepository.findPgnDefinition(22222)).thenReturn(pgnDef2);

        when(busService.getPGNsForDSRequest(List.of(222, 333), supportedSpns))
                                                                              .thenReturn(List.of(22222,
                                                                                                  44444,
                                                                                                  55555,
                                                                                                  66666));
        when(busService.dsRequest(eq(22222), eq(0), any())).thenReturn(Stream.empty());

        GenericPacket packet4 = packet(444, false);
        when(busService.dsRequest(eq(44444), eq(0), any())).thenReturn(Stream.of(packet4));

        GenericPacket packet5 = packet(555, false);
        when(busService.dsRequest(eq(55555), eq(0), any())).thenReturn(Stream.of(packet5));

        when(busService.dsRequest(eq(55555), eq(0), any())).thenReturn(Stream.empty());

        GenericPacket packet6 = packet(666, true);
        when(busService.dsRequest(eq(66666), eq(0), any())).thenReturn(Stream.of(packet6));

        GenericPacket packet2 = packet(222, false);
        when(busService.globalRequest(eq(22222), any())).thenReturn(Stream.of(packet2));

        when(busService.globalRequest(eq(55555), any())).thenReturn(Stream.empty());

        when(busService.globalRequest(eq(66666), any())).thenReturn(Stream.of(packet6));

        when(busService.collectBroadcastPGNs(List.of(22222, 44444, 55555, 66666))).thenReturn(List.of(22222));
        when(broadcastValidator.getMaximumBroadcastPeriod(List.of(22222))).thenReturn(2);

        when(busService.readBus(eq(8), any())).thenReturn(Stream.of(packet2));
        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap2 = new HashMap<>();
        packetMap2.put(22222, Map.of(0, List.of(packet2)));
        when(broadcastValidator.buildPGNPacketsMap(List.of(packet2))).thenReturn(packetMap2);

        runTest();

        verify(busService).setup(eq(j1939), any());
        verify(dataRepository).getVehicleInformation();
        verify(dataRepository, atLeastOnce()).getObdModules();
        verify(j1939DaRepository, atLeastOnce()).findPgnDefinition(22222);

        // verify(tableA1Validator).report(eq(supportedSpns),
        // any(ResultsListener.class),
        // eq(DSL),
        // eq(1),
        // eq(26));
        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(busService).readBus(12, "6.1.26.1.a");
        verify(broadcastValidator).buildPGNPacketsMap(packets);
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));
        verify(busService).collectNonOnRequestPGNs(supportedSpns);
        verify(busService).collectNonOnRequestPGNs(List.of(222));

        verify(mockListener).addOutcome(1, 26, FAIL, "SPN 222 was not broadcast by Engine #1 (0)");
        verify(mockListener).addOutcome(1, 26, FAIL, "SPN 333 was broadcast as NOT AVAILABLE by Engine #1 (0)");
        verify(mockListener).addOutcome(1, 26, FAIL, "No DS response for PGN 22222 from Engine #1 (0)");
        verify(mockListener).addOutcome(1, 26, FAIL, "No DS response for PGN 55555 from Engine #1 (0)");
        verify(mockListener).addOutcome(1, 26, FAIL, "SPNs received as NOT AVAILABLE from Engine #1 (0): 666");
        verify(mockListener).addOutcome(1, 26, FAIL, "No Global response for PGN 55555");
        verify(mockListener).addOutcome(1, 26, FAIL, "SPNs received as NOT AVAILABLE: 666");

        verify(busService).getPGNsForDSRequest(List.of(222, 333), supportedSpns);
        verify(busService).dsRequest(eq(22222), eq(0), "");
        verify(busService).dsRequest(eq(44444), eq(0), "");
        verify(busService).dsRequest(eq(55555), eq(0), "");
        verify(busService).dsRequest(eq(66666), eq(0), "");
        verify(busService).globalRequest(eq(22222), "");
        verify(busService).globalRequest(eq(55555), "");
        verify(busService).globalRequest(eq(66666), "");
        verify(busService).collectBroadcastPGNs(List.of(22222, 44444, 55555, 66666));
        verify(broadcastValidator).getMaximumBroadcastPeriod(List.of(22222));
        verify(busService).readBus(eq(8), any());
        verify(broadcastValidator).buildPGNPacketsMap(List.of(packet2));
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap2),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));

        verify(dataRepository).getObdModuleAddresses();
        verify(tableA1Validator).reportNonObdModuleProvidedSPNs(any(),
                                                                any(ResultsListener.class),
                                                                any());
        verify(tableA1Validator).reportImplausibleSPNValues(any(),
                                                            any(ResultsListener.class),
                                                            eq(false),
                                                            any());
        verify(tableA1Validator).reportDuplicateSPNs(any(), any(ResultsListener.class), any());

        String expected = "";
        expected += "FAIL: SPN 222 was not broadcast by Engine #1 (0)" + NL;
        expected += "FAIL: SPN 333 was broadcast as NOT AVAILABLE by Engine #1 (0)" + NL;
        expected += "FAIL: No DS response for PGN 22222 from Engine #1 (0)" + NL;
        expected += "FAIL: No DS response for PGN 55555 from Engine #1 (0)" + NL;
        expected += "FAIL: No Global response for PGN 55555" + NL;
        expected += "FAIL: SPNs received as NOT AVAILABLE from Engine #1 (0): 666" + NL;
        expected += "FAIL: SPNs received as NOT AVAILABLE: 666" + NL;
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        expectedMsg += "Start Part 1 Step 26" + NL;
        expectedMsg += "DS Request for 22222 to Engine #1 (0)" + NL;
        expectedMsg += "Global Request for PGN 22222" + NL;
        expectedMsg += "DS Request for 44444 to Engine #1 (0)" + NL;
        expectedMsg += "DS Request for 55555 to Engine #1 (0)" + NL;
        expectedMsg += "Global Request for PGN 55555" + NL;
        expectedMsg += "DS Request for 66666 to Engine #1 (0)" + NL;
        expectedMsg += "Global Request for PGN 66666" + NL;
        expectedMsg += "End Part 1 Step 26";
        assertEquals(expectedMsg, listener.getMessages());
    }

    @Ignore("Fix once Step 26 is accepted")
    @Test
    public void runWithoutFailures() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(DSL);
        when(dataRepository.getVehicleInformation()).thenReturn(vehicleInformation);

        // SPNs
        // 111 - Broadcast with value
        // 444 - DS with value
        List<Integer> supportedSpns = Arrays.asList(111, 444);

        when(dataRepository.getObdModuleAddresses()).thenReturn(List.of(0));

        OBDModuleInformation obdModule = mock(OBDModuleInformation.class);
        when(dataRepository.getObdModules()).thenReturn(List.of(obdModule));
        when(obdModule.getSourceAddress()).thenReturn(0);
        List<SupportedSPN> supportedSPNList = spns(111, 444);
        when(obdModule.getFilteredDataStreamSPNs()).thenReturn(supportedSPNList);

        when(broadcastValidator.getMaximumBroadcastPeriod()).thenReturn(3);

        List<GenericPacket> packets = new ArrayList<>();
        GenericPacket packet1 = packet(111, false);
        packets.add(packet1);
        when(busService.readBus(12, "6.1.26.1.a")).thenReturn(packets.stream());

        Map<Integer, Map<Integer, List<GenericPacket>>> packetMap = new HashMap<>();
        packetMap.put(11111, Map.of(0, List.of(packet1)));
        when(broadcastValidator.buildPGNPacketsMap(packets)).thenReturn(packetMap);

        when(busService.collectNonOnRequestPGNs(supportedSpns)).thenReturn(List.of(11111));
        when(busService.collectBroadcastPGNs(List.of(44444))).thenReturn(List.of());
        when(busService.getPGNsForDSRequest(List.of(), supportedSpns)).thenReturn(List.of(44444));

        GenericPacket packet4 = packet(444, false);
        when(busService.dsRequest(eq(44444), eq(0), any())).thenReturn(Stream.of(packet4));

        runTest();

        verify(busService).setup(eq(j1939), any());
        verify(dataRepository).getVehicleInformation();
        verify(dataRepository, atLeastOnce()).getObdModules();

        verify(broadcastValidator).getMaximumBroadcastPeriod();
        verify(busService).readBus(12, "6.1.2.3.a");
        verify(broadcastValidator).buildPGNPacketsMap(packets);
        verify(broadcastValidator).reportBroadcastPeriod(eq(packetMap),
                                                         any(),
                                                         any(ResultsListener.class),
                                                         eq(1),
                                                         eq(26));
        verify(busService).collectNonOnRequestPGNs(supportedSpns);
        verify(busService).collectBroadcastPGNs(List.of(44444));

        verify(busService).getPGNsForDSRequest(List.of(), supportedSpns);
        verify(busService).dsRequest(eq(44444), eq(0), "");

        verify(dataRepository).getObdModuleAddresses();
        verify(tableA1Validator).reportNonObdModuleProvidedSPNs(any(),
                                                                any(ResultsListener.class),
                                                                any());
        verify(tableA1Validator).reportImplausibleSPNValues(any(),
                                                            any(ResultsListener.class),
                                                            eq(false),
                                                            any());
        verify(tableA1Validator).reportDuplicateSPNs(any(), any(ResultsListener.class), any());

        String expected = "";
        assertEquals(expected, listener.getResults());

        String expectedMsg = "";
        expectedMsg += "Start Part 1 Step 26" + NL;
        expectedMsg += "DS Request for 44444 to Engine #1 (0)" + NL;
        expectedMsg += "End Part 1 Step 26";
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
