/*
 * Copyright (c) 2020. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers;

import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.INFO;
import static org.etools.j1939_84.model.Outcome.PASS;
import static org.etools.j1939_84.model.Outcome.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.etools.j1939_84.bus.j1939.Lookup;
import org.etools.j1939_84.bus.j1939.packets.GenericPacket;
import org.etools.j1939_84.bus.j1939.packets.SupportedSPN;
import org.etools.j1939_84.bus.j1939.packets.model.Spn;
import org.etools.j1939_84.bus.j1939.packets.model.SpnDefinition;
import org.etools.j1939_84.model.FuelType;
import org.etools.j1939_84.model.Outcome;

public class TableA1Validator {

    private static void addOutcome(ResultsListener listener,
            int partNumber,
            int stepNumber,
            Outcome outcome,
            String message) {
        listener.addOutcome(partNumber, stepNumber, outcome, message);
        listener.onResult(outcome + ": 6." + partNumber + "." + stepNumber + " - " + message);
    }

    private static List<Integer> getFailureSPNs(FuelType fuelType) {
        List<Integer> allRequiredSpns = new ArrayList<>();

        // These SPNS must be provided by all vehicles
        // noinspection CollectionAddAllCanBeReplacedWithConstructor
        allRequiredSpns.addAll(List.of(27, 84, 91, 92, 108,
                235, 247, 248,
                512, 513, 514, 539, 540, 541, 542, 543, 544,
                2791, 2978,
                5837, 5829));

        if (fuelType.isCompressionIgnition()) {
            // These are SPNs required for CI Engines
            allRequiredSpns.addAll(List.of(5466, 3700, 6895, 7333, 3226));
        } else if (fuelType.isSparkIgnition()) {
            // These are SPNs required for SI Engines
            allRequiredSpns.addAll(List.of(51, 3464, 4236, 4237, 4240, 3249, 3241, 3217, 3227));
        }

        return allRequiredSpns;
    }

    private static List<Integer> getInfoSPNs() {
        return new ArrayList<>(List.of(96, 110, 132, 157, 190, 5827, 5313));
    }

    private static Collection<List<Integer>> getSPNGroups(FuelType fuelType) {
        Collection<List<Integer>> spnGroups = new ArrayList<>();
        spnGroups.add(List.of(110, 1637, 4076, 4193));
        spnGroups.add(List.of(190, 723, 4201, 4202));
        spnGroups.add(List.of(158, 168));
        spnGroups.add(List.of(183, 1413, 1600));

        spnGroups.add(List.of(102, 106, 1127, 3563));

        if (fuelType.isCompressionIgnition()) {
            spnGroups.add(List.of(94, 157, 164, 5313, 5314, 5578));
            spnGroups.add(List.of(5454, 5827));
            spnGroups.add(List.of(3251, 3609, 3610));
            spnGroups.add(List.of(3516, 3518, 7346));
            spnGroups.add(List.of(3031, 3515));
        } else if (fuelType.isSparkIgnition()) {
            spnGroups.add(List.of(94, 157, 5313, 5578));
        }

        return spnGroups;
    }

    private static List<Integer> getWarningSPNs(FuelType fuelType) {
        List<Integer> allWarningSPNs = new ArrayList<>();
        allWarningSPNs.add(158);
        if (fuelType.isCompressionIgnition()) {
            allWarningSPNs.add(5466);
        }
        return allWarningSPNs;
    }

    private static boolean packetContainsSupportedSPNs(Collection<Integer> supportedSpns, GenericPacket packet) {
        return packet.getPgnDefinition()
                .getSpnDefinitions()
                .stream()
                .mapToInt(SpnDefinition::getSpnId)
                .anyMatch(supportedSpns::contains);
    }

    private static String stringifySpns(List<Integer> spns) {
        return spns.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private final DataRepository dataRepository;

    private final TableA1ValueValidator valueValidator;

    public TableA1Validator(DataRepository dataRepository) {
        this(new TableA1ValueValidator(dataRepository), dataRepository);
    }

    TableA1Validator(TableA1ValueValidator valueValidator, DataRepository dataRepository) {
        this.dataRepository = dataRepository;
        this.valueValidator = valueValidator;
    }

    /**
     * Writes a Failure/Warning if any SPNs is provided by more than one module
     */
    public void reportDuplicateSPNs(List<GenericPacket> packets,
            ResultsListener listener,
            int partNumber,
            int stepNumber) {
        // f. Fail/warn per Table A-1 if two or more ECUs provide an SPN listed
        // in Table A-1
        Map<Integer, Integer> uniques = new HashMap<>();
        Set<Integer> reportedSpns = new HashSet<>();

        for (GenericPacket packet : packets) {
            for (Spn spn : packet.getSpns()) {
                int spnId = spn.getId();
                Integer address = uniques.get(spnId);
                if (address == null) {
                    uniques.put(spnId, packet.getSourceAddress());
                } else if (address != packet.getSourceAddress()) {
                    Outcome outcome = Lookup.getOutcomeForDuplicateSpn(spnId);
                    if (outcome != PASS && !reportedSpns.contains(spnId)) {
                        reportedSpns.add(spnId);
                        addOutcome(listener,
                                partNumber,
                                stepNumber,
                                outcome, "SPN " + spnId + " provided by more than one module");
                    }
                }
            }
        }
    }

    /**
     * Reports on SPN values which are implausible given the key state and fuel
     * type
     */
    public void reportImplausibleSPNValues(List<GenericPacket> packets,
            Collection<Integer> supportedSpns,
            ResultsListener listener,
            boolean isEngineOn,
            FuelType fuelType,
            int partNumber,
            int stepNumber) {
        // Map of Source Address to PGNs for packets already written to the log
        Map<Integer, Set<Integer>> foundPackets = new HashMap<>();

        // Map of Source Address to SPN for SPNs with invalid values (to avoid
        // duplicate reporting)
        Map<Integer, Set<Integer>> invalidSPNs = new HashMap<>();

        // d. Fail/warn if any broadcast data is not valid for KOEO conditions
        // as per Table A-1, Min Data Stream Support.
        for (GenericPacket packet : packets) {

            boolean isReported = false;

            int moduleAddress = packet.getSourceAddress();
            int pgn = packet.getPacket().getPgn();

            Set<Integer> modulePackets = foundPackets.getOrDefault(moduleAddress, new HashSet<>());
            if (!modulePackets.contains(pgn)) {
                if (packetContainsSupportedSPNs(supportedSpns, packet)) {
                    // Only report on Support SPNs
                    listener.onResult("Found: " + packet.toString());
                    isReported = true;
                }
                modulePackets.add(pgn);
                foundPackets.put(moduleAddress, modulePackets);
            }

            boolean isInvalid = false;
            for (Spn spn : packet.getSpns()) {
                int spnId = spn.getId();
                Double value = spn.getValue();
                if (valueValidator.isImplausible(spnId, value, isEngineOn, fuelType)) {
                    Set<Integer> invalid = invalidSPNs.getOrDefault(moduleAddress, new HashSet<>());
                    if (!invalid.contains(spnId)) {
                        isInvalid = true;
                        String message = "Value for SPN " + spnId + " (" + value + ") is implausible";
                        addOutcome(listener, partNumber, stepNumber, Outcome.WARN, message);
                        invalid.add(spnId);
                        invalidSPNs.put(moduleAddress, invalid);
                    }
                }
            }

            if (isInvalid && !isReported) {
                listener.onResult("Found: " + packet.toString());
            }
        }
    }

    /**
     * Writes a Failure/Warning for any Supported SPNs which aren't supported by
     * any OBD Modules
     */
    public void reportMissingSPNs(List<Integer> supportedSpns,
            ResultsListener listener,
            FuelType fuelType,
            int partNumber,
            int stepNumber) {

        // This processes the SPNs in steps
        // The first step is to report the SPNs which *must* be provided and
        // result in a FAIL if they are not
        // The second are SPNs which results in WARN
        // Then SPNs which are reported at INFO
        // Then SPNs for which only one in the group is required

        // The resultant missing SPNs
        Map<Outcome, List<Integer>> missingSpns = new HashMap<>();

        // Failure SPNs
        List<Integer> allRequiredSpns = getFailureSPNs(fuelType);
        allRequiredSpns.removeAll(supportedSpns);
        Collections.sort(allRequiredSpns);
        missingSpns.put(FAIL, allRequiredSpns);

        // Warnings SPNs
        List<Integer> warningSpns = getWarningSPNs(fuelType);
        warningSpns.removeAll(supportedSpns);
        Collections.sort(warningSpns);
        missingSpns.put(WARN, warningSpns);

        // INFO SPNs
        List<Integer> infoSpns = getInfoSPNs();
        infoSpns.removeAll(supportedSpns);
        Collections.sort(infoSpns);
        missingSpns.put(INFO, infoSpns);

        // For any Missing SPNs write an outcome to the report
        for (Outcome outcome : Outcome.values()) {
            List<Integer> value = missingSpns.get(outcome);
            if (value != null) {
                for (int spn : value) {
                    addOutcome(listener, partNumber, stepNumber, outcome, "Required SPN " + spn + " is not supported");
                }
            }
        }

        // Report on SPN Groups
        for (List<Integer> spns : getSPNGroups(fuelType)) {
            if (spns.stream().noneMatch(supportedSpns::contains)) {
                addOutcome(listener,
                        partNumber,
                        stepNumber,
                        FAIL, "At least one of these SPNs is not supported: " + stringifySpns(spns));
            }
        }
    }

    /**
     * Writes a Failures/Warning to the report if an OBD Supported SPN is
     * provided by a Non-OBD Module.
     */
    public void reportNonObdModuleProvidedSPNs(List<GenericPacket> packets,
            int obdModuleAddress,
            ResultsListener listener,
            int partNumber,
            int stepNumber) {

        // e. Fail/warn per Table A-1, if an expected SPN from the DM24 support
        // list from an OBD ECU is provided by a non-OBD
        // ECU. (provided extraneously)
        List<Integer> supportedSpns = dataRepository.getObdModule(obdModuleAddress)
                .getDataStreamSpns()
                .stream()
                .map(SupportedSPN::getSpn)
                .collect(Collectors.toList());

        packets.stream()
                .filter(p -> dataRepository.getObdModule(p.getSourceAddress()) == null)
                .flatMap(p -> p.getSpns().stream())
                .filter(spn -> !spn.isNotAvailable())
                .map(Spn::getId)
                .filter(supportedSpns::contains)
                .forEach(id -> {
                    Outcome outcome = Lookup.getOutcomeForNonObdModuleProvidingSpn(id);
                    if (outcome != PASS) {
                        addOutcome(listener,
                                partNumber,
                                stepNumber,
                                outcome,
                                "SPN " + id + " provided by non-OBD Module");
                    }
                });
    }

}
