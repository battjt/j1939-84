/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part01;

import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.WARN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.etools.j1939_84.bus.j1939.J1939;
import org.etools.j1939_84.bus.j1939.packets.VehicleIdentificationPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.model.VehicleInformation;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.VinDecoder;
import org.etools.testdoc.TestDoc;
import org.etools.testdoc.TestItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The unit test for {@link Part01Step05Controller}
 *
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class Part01Step05ControllerTest {

    @Mock
    private BannerModule bannerModule;

    private DataRepository dataRepository;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    private Part01Step05Controller instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Mock
    private VinDecoder vinDecoder;

    private void runTest() {
        instance.execute(listener, j1939, reportFileModule);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        verify(engineSpeedModule).setJ1939(j1939);
    }

    @Before
    public void setUp() throws Exception {
        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);

        dataRepository = DataRepository.newInstance();

        instance = new Part01Step05Controller(
                                              executor,
                                              engineSpeedModule,
                                              bannerModule,
                                              vehicleInformationModule,
                                              vinDecoder,
                                              dataRepository,
                                              DateTimeModule.getInstance());
    }

    @After
    public void tearDown() throws Exception {

        verifyNoMoreInteractions(executor,
                                 engineSpeedModule,
                                 bannerModule,
                                 vehicleInformationModule,
                                 vinDecoder,
                                 mockListener);
    }

    @Test
    @TestDoc(value = { @TestItem(verifies = "6.1.5.2.b"), @TestItem(verifies = "6.1.5.2.c"),
            @TestItem(verifies = "6.1.5.2.d"), @TestItem(verifies = "6.1.5.2.e"),
            @TestItem(verifies = "6.1.5.3.a"), @TestItem(verifies = "6.1.5.3.b"),
            @TestItem(verifies = "6.1.5.3.d") }, description = "More than one OBD ECU responded with VIN" + "<br>"
                    + "VIN does not match user entered VIN."
                    + "<br>" + "VIN Model Year does not match user entered Vehicle Model Year" + "<br>"
                    + "VIN is not valid (not 17 legal chars, incorrect checksum, or non-numeric sequence" + "<br>"
                    + "Non-OBD ECU responded with VIN" + "<br>" + "More than one VIN response from an ECU" + "<br>"
                    + "Manufacturer defined data follows the VIN")

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The method is called just to get some exception.")
    public void testError() {
        List<VehicleIdentificationPacket> packets = new ArrayList<>();
        VehicleIdentificationPacket packet = mock(VehicleIdentificationPacket.class);
        when(packet.getSourceAddress()).thenReturn(1);
        packets.add(packet);
        packets.add(packet);
        when(packet.getVin()).thenReturn("vin");
        when(packet.getManufacturerData()).thenReturn("NightHawk");

        VehicleIdentificationPacket packet2 = mock(VehicleIdentificationPacket.class);
        when(packet.getSourceAddress()).thenReturn(3);
        packets.add(packet2);

        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setVin("78654321345667889");
        vehicleInformation.setVehicleModelYear(2006);

        when(vehicleInformationModule.reportVin(any())).thenReturn(packets);
        dataRepository.putObdModule(new OBDModuleInformation(1));
        dataRepository.putObdModule(new OBDModuleInformation(2));
        dataRepository.putObdModule(new OBDModuleInformation(3));
        dataRepository.setVehicleInformation(vehicleInformation);

        runTest();

        verify(engineSpeedModule).setJ1939(j1939);

        verify(mockListener).addOutcome(1,
                                        5,
                                        FAIL,
                                        "6.1.5.2.b - More than one OBD ECU responded with VIN");
        verify(mockListener).addOutcome(1,
                                        5,
                                        FAIL,
                                        "6.1.5.2.c - VIN does not match user entered VIN");
        verify(mockListener).addOutcome(1,
                                        5,
                                        FAIL,
                                        "6.1.5.2.e - VIN is not valid (not 17 legal chars, incorrect checksum, or non-numeric sequence");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.a - Non-OBD ECU responded with VIN");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.b - More than one VIN response from an ECU");

        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.d - Manufacturer defined data follows the VIN");

        verify(vehicleInformationModule).setJ1939(j1939);
        verify(vehicleInformationModule).reportVin(any());

        verify(vinDecoder).isVinValid("vin");
    }

    @Test
    @TestDoc(description = "Verify step name.")
    public void testGetDisplayName() {
        assertEquals("Display Name", "Part 1 Step 5", instance.getDisplayName());
    }

    @Test
    @TestDoc(description = "Verify step 5 has 1 step.")
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 0, instance.getTotalSteps());
    }

    @Test
    @TestDoc(value = @TestItem(verifies = "6.1.5", description = "Verify no failures when no fail criteria are met."))
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The method is called just to get some exception.")
    public void testNoError() {
        dataRepository.putObdModule(new OBDModuleInformation(0));

        List<VehicleIdentificationPacket> packets = new ArrayList<>();

        VehicleIdentificationPacket packet = mock(VehicleIdentificationPacket.class);
        when(packet.getVin()).thenReturn("vin");
        when(packet.getManufacturerData()).thenReturn("");
        packets.add(packet);

        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setVehicleModelYear(2006);
        vehicleInformation.setVin("vin");
        dataRepository.setVehicleInformation(vehicleInformation);

        when(vehicleInformationModule.reportVin(any())).thenReturn(packets);

        when(vinDecoder.getModelYear(any())).thenReturn(2006);
        when(vinDecoder.isVinValid(any())).thenReturn(true);

        runTest();

        verify(vehicleInformationModule).setJ1939(j1939);
        verify(vehicleInformationModule).reportVin(any());

        verify(vinDecoder).getModelYear("vin");
        verify(vinDecoder).isVinValid("vin");

    }

    /**
     * This test will have an error on the nonObdResponses due to error
     * combination restrictions/requirements
     */
    @Test
    @TestDoc(value = {
            @TestItem(verifies = "6.1.5.2.c,d,e"),
            @TestItem(verifies = "6.1.5.2.d"),
            @TestItem(verifies = "6.1.5.2.e"),
            @TestItem(verifies = "6.1.5.3.a,b,c,d") }, description = "Verify fails and warns are reported.")
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The method is called just to get some exception.")
    public void testNoObdResponses() {
        List<VehicleIdentificationPacket> packets = new ArrayList<>();
        VehicleIdentificationPacket packet = mock(VehicleIdentificationPacket.class);
        when(packet.getSourceAddress()).thenReturn(1);
        packets.add(packet);
        packets.add(packet);
        when(packet.getVin()).thenReturn("vin");
        when(packet.getManufacturerData()).thenReturn("NightHawk");

        VehicleIdentificationPacket packet2 = mock(VehicleIdentificationPacket.class);
        when(packet.getSourceAddress()).thenReturn(3);
        packets.add(packet2);

        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setVin("78654321345667889");
        vehicleInformation.setVehicleModelYear(2006);

        when(vehicleInformationModule.reportVin(any())).thenReturn(packets);

        dataRepository.setVehicleInformation(vehicleInformation);

        runTest();

        verify(engineSpeedModule).setJ1939(j1939);

        verify(mockListener).addOutcome(1,
                                        5,
                                        FAIL,
                                        "6.1.5.2.c - VIN does not match user entered VIN");
        verify(mockListener).addOutcome(1,
                                        5,
                                        FAIL,
                                        "6.1.5.2.e - VIN is not valid (not 17 legal chars, incorrect checksum, or non-numeric sequence");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.a - Non-OBD ECU responded with VIN");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.b - More than one VIN response from an ECU");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.c - VIN provided from more than one non-OBD ECU");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.d - Manufacturer defined data follows the VIN");

        verify(vehicleInformationModule).setJ1939(j1939);
        verify(vehicleInformationModule).reportVin(any());

        verify(vinDecoder).isVinValid("vin");
    }

    @Test
    @TestDoc(@TestItem(verifies = "6.1.5.2.a", description = "Verify fail reported with no response."))
    public void testPacketsEmpty() {

        when(vehicleInformationModule.reportVin(any())).thenReturn(List.of());

        runTest();

        verify(mockListener).addOutcome(1, 5, FAIL, "6.1.5.2.a - No VIN was provided by any ECU");

        verify(vehicleInformationModule).setJ1939(j1939);
        verify(vehicleInformationModule).reportVin(any());
    }

    @Test
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The method is called just to get some exception.")
    @TestDoc(@TestItem(verifies = "6.1.5.3.a,b,c,d"))
    public void testWarnError() {
        List<VehicleIdentificationPacket> packets = new ArrayList<>();
        VehicleIdentificationPacket packet = mock(VehicleIdentificationPacket.class);
        when(packet.getSourceAddress()).thenReturn(1);
        when(packet.getVin()).thenReturn("78654321345667889");
        packets.add(packet);
        when(packet.getManufacturerData()).thenReturn("NightHawk");

        VehicleIdentificationPacket packet2 = mock(VehicleIdentificationPacket.class);
        when(packet2.getSourceAddress()).thenReturn(1);
        packets.add(packet2);

        when(vehicleInformationModule.reportVin(any())).thenReturn(packets);
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setVin("78654321345667889");
        vehicleInformation.setVehicleModelYear(2006);

        dataRepository.setVehicleInformation(vehicleInformation);

        when(vinDecoder.getModelYear("78654321345667889")).thenReturn(2006);
        when(vinDecoder.isVinValid("78654321345667889")).thenReturn(true);

        when(vehicleInformationModule.reportVin(any())).thenReturn(packets);

        runTest();

        verify(engineSpeedModule).setJ1939(j1939);

        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.a - Non-OBD ECU responded with VIN");
        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.b - More than one VIN response from an ECU");

        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.c - VIN provided from more than one non-OBD ECU");

        verify(mockListener).addOutcome(1,
                                        5,
                                        WARN,
                                        "6.1.5.3.d - Manufacturer defined data follows the VIN");

        verify(vehicleInformationModule).setJ1939(j1939);
        verify(vehicleInformationModule).reportVin(any());

        verify(vinDecoder).getModelYear("78654321345667889");
        verify(vinDecoder).isVinValid("78654321345667889");
    }

}
