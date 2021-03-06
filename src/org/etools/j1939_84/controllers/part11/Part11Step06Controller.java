/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part11;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.etools.j1939_84.bus.j1939.packets.DM29DtcCounts;
import org.etools.j1939_84.bus.j1939.packets.DiagnosticTroubleCodePacket;
import org.etools.j1939_84.bus.j1939.packets.ParsedPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticMessageModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * 6.11.6 DM28: Permanent DTCs
 */
public class Part11Step06Controller extends StepController {
    private static final int PART_NUMBER = 11;
    private static final int STEP_NUMBER = 6;
    private static final int TOTAL_STEPS = 0;

    Part11Step06Controller() {
        this(Executors.newSingleThreadScheduledExecutor(),
             new BannerModule(),
             DateTimeModule.getInstance(),
             DataRepository.getInstance(),
             new EngineSpeedModule(),
             new VehicleInformationModule(),
             new DiagnosticMessageModule());
    }

    Part11Step06Controller(Executor executor,
                           BannerModule bannerModule,
                           DateTimeModule dateTimeModule,
                           DataRepository dataRepository,
                           EngineSpeedModule engineSpeedModule,
                           VehicleInformationModule vehicleInformationModule,
                           DiagnosticMessageModule diagnosticMessageModule) {
        super(executor,
              bannerModule,
              dateTimeModule,
              dataRepository,
              engineSpeedModule,
              vehicleInformationModule,
              diagnosticMessageModule,
              PART_NUMBER,
              STEP_NUMBER,
              TOTAL_STEPS);
    }

    @Override
    protected void run() throws Throwable {
        // 6.11.6.1.a. Global DM28 [(send Request (PGN 59904) for PGN 64896 (SPNs 1213-1215, 1706, and 3038)]).
        var packets = getDiagnosticMessageModule().requestDM28(getListener()).getPackets();

        packets.forEach(this::save);

        // 6.11.6.2.a. Fail if no ECU reports a permanent DTC.
        boolean noDTCs = packets.stream().noneMatch(DiagnosticTroubleCodePacket::hasDTCs);
        if (noDTCs) {
            addFailure("6.11.6.2.a - No ECU report a permanent DTC");
        }

        // 6.11.6.2.b. Fail if any ECU reports a different number of permanent DTCs than indicated in DM29 response
        // earlier in test 6.11.4.
        packets.stream()
               .filter(p -> p.getDtcs().size() != getDTCCount(p.getSourceAddress()))
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.11.6.2.b - " + moduleName
                           + " reported a different number of permanent DTCs that indicate in DM29 response earlier in test 6.11.4");
               });
    }

    private int getDTCCount(int address) {
        var dm29 = get(DM29DtcCounts.class, address, 11);
        return dm29 == null ? 0 : dm29.getEmissionRelatedPermanentDTCCount();
    }

}
