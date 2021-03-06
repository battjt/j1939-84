/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part09;

import static org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket.Response.NACK;
import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executor;

import org.etools.j1939_84.bus.j1939.BusResult;
import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939_84.bus.j1939.packets.DM25ExpandedFreezeFrame;
import org.etools.j1939_84.bus.j1939.packets.DiagnosticTroubleCode;
import org.etools.j1939_84.bus.j1939.packets.FreezeFrame;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticMessageModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.TestDateTimeModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Part09Step17ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 9;
    private static final int STEP_NUMBER = 17;

    @Mock
    private BannerModule bannerModule;

    @Mock
    private DiagnosticMessageModule diagnosticMessageModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    @Mock
    private J1939 j1939;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    private TestResultsListener listener;

    private DataRepository dataRepository;

    private StepController instance;

    @Before
    public void setUp() throws Exception {
        dataRepository = DataRepository.newInstance();
        listener = new TestResultsListener(mockListener);

        instance = new Part09Step17Controller(executor,
                                              bannerModule,
                                              new TestDateTimeModule(),
                                              dataRepository,
                                              engineSpeedModule,
                                              vehicleInformationModule,
                                              diagnosticMessageModule);

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
        DateTimeModule.setInstance(null);
        verifyNoMoreInteractions(executor,
                                 bannerModule,
                                 engineSpeedModule,
                                 vehicleInformationModule,
                                 diagnosticMessageModule,
                                 mockListener);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Part " + PART_NUMBER + " Step " + STEP_NUMBER, instance.getDisplayName());
    }

    @Test
    public void testGetPartNumber() {
        assertEquals(PART_NUMBER, instance.getPartNumber());
    }

    @Test
    public void testGetStepNumber() {
        assertEquals(STEP_NUMBER, instance.getStepNumber());
    }

    @Test
    public void testGetTotalSteps() {
        assertEquals(0, instance.getTotalSteps());
    }

    @Test
    public void testHappyPathNoFailures() {
        // Module 0 responds normally
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        obdModuleInformation0.set(DM25ExpandedFreezeFrame.create(0), 8);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm25 = DM25ExpandedFreezeFrame.create(0);
        when(diagnosticMessageModule.requestDM25(any(), eq(0))).thenReturn(BusResult.of(dm25));

        // Module 1 - NACKs
        OBDModuleInformation obdModuleInformation1 = new OBDModuleInformation(1);
        obdModuleInformation1.set(DM25ExpandedFreezeFrame.create(1), 8);
        dataRepository.putObdModule(obdModuleInformation1);
        var nack = AcknowledgmentPacket.create(1, NACK);
        when(diagnosticMessageModule.requestDM25(any(), eq(1))).thenReturn(BusResult.of(nack));

        // Module 2 - No Previous DM25 and no NACK
        dataRepository.putObdModule(new OBDModuleInformation(2));
        when(diagnosticMessageModule.requestDM25(any(), eq(2))).thenReturn(BusResult.empty());

        runTest();

        verify(diagnosticMessageModule).requestDM25(any(), eq(0));
        verify(diagnosticMessageModule).requestDM25(any(), eq(1));
        verify(diagnosticMessageModule).requestDM25(any(), eq(2));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        assertEquals(List.of(), listener.getOutcomes());
    }

    @Test
    public void tesFailureForFreezeFramesPresent() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        obdModuleInformation0.set(DM25ExpandedFreezeFrame.create(0), 8);
        dataRepository.putObdModule(obdModuleInformation0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        var freezeFrame = new FreezeFrame(dtc, new int[10]);
        var dm25 = DM25ExpandedFreezeFrame.create(0, freezeFrame);
        when(diagnosticMessageModule.requestDM25(any(), eq(0))).thenReturn(BusResult.of(dm25));

        runTest();

        verify(diagnosticMessageModule).requestDM25(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.17.2.a - Engine #1 (0) reported other than no Freeze Frame data stored");
    }

    @Test
    public void testFailureForNoNACK() {
        // Module 1 - NACKs
        OBDModuleInformation obdModuleInformation1 = new OBDModuleInformation(1);
        obdModuleInformation1.set(DM25ExpandedFreezeFrame.create(1), 8);
        dataRepository.putObdModule(obdModuleInformation1);
        when(diagnosticMessageModule.requestDM25(any(), eq(1))).thenReturn(BusResult.empty());

        runTest();

        verify(diagnosticMessageModule).requestDM25(any(), eq(1));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.17.2.b - OBD ECU Engine #2 (1) did not provide a NACK for the DS query");
    }

}
