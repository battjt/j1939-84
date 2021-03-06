/*
 * Copyright (c) 2020. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.INFO;
import static org.etools.j1939_84.model.Outcome.PASS;
import static org.etools.j1939_84.model.Outcome.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.etools.j1939_84.bus.j1939.J1939DaRepository;
import org.etools.j1939_84.bus.j1939.Lookup;
import org.etools.j1939_84.bus.j1939.packets.GenericPacket;
import org.etools.j1939_84.bus.j1939.packets.SupportedSPN;
import org.etools.j1939_84.bus.j1939.packets.model.PgnDefinition;
import org.etools.j1939_84.bus.j1939.packets.model.Spn;
import org.etools.j1939_84.bus.j1939.packets.model.SpnDefinition;
import org.etools.j1939_84.model.FuelType;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.model.Outcome;

public class TableA1Validator {

    private final DataRepository dataRepository;
    // Map of Source Address to PGNs for packets already written to the log
    private final Map<Integer, Set<Integer>> foundPackets = new HashMap<>();

    // Map of Source Address to SPN for SPNs with invalid values (to avoid
    // duplicate reporting)
    private final Map<Integer, Set<Integer>> invalidSPNs = new HashMap<>();

    private final J1939DaRepository j1939DaRepository;

    // Map of Source Address to SPNs for packets already written to the log
    private final Map<Integer, Set<Integer>> nonObdProvidedSPNs = new HashMap<>();

    // Map of Source Address to SPN for SPNs with value of Not Available (to
    // avoid duplicate reporting)
    private final Map<Integer, Set<Integer>> notAvailableSPNs = new HashMap<>();

    // Map of Source Address to SPN for SPNs provided by not supported (to avoid
    // duplicate reporting)
    private final Map<Integer, Set<Integer>> providedNotSupportedSPNs = new HashMap<>();

    private final TableA1ValueValidator valueValidator;

    private final int partNumber;
    private final int stepNumber;

    public TableA1Validator(DataRepository dataRepository, int partNumber, int stepNumber) {
        this(new TableA1ValueValidator(dataRepository),
             dataRepository,
             J1939DaRepository.getInstance(),
             partNumber,
             stepNumber);
    }

    TableA1Validator(TableA1ValueValidator valueValidator,
                     DataRepository dataRepository,
                     J1939DaRepository j1939DaRepository,
                     int partNumber,
                     int stepNumber) {
        this.dataRepository = dataRepository;
        this.valueValidator = valueValidator;
        this.j1939DaRepository = j1939DaRepository;
        this.partNumber = partNumber;
        this.stepNumber = stepNumber;
    }

    private void addOutcome(ResultsListener listener,
                                   String section,
                                   Outcome outcome,
                                   String message) {
        listener.addOutcome(partNumber, stepNumber, outcome, section + " - " + message);
    }

    private static List<Integer> getFailureSPNs(FuelType fuelType) {
        var spns = new ArrayList<>(List.of(92, 102, 512, 513, 514, 539, 540, 541, 542, 543, 544, 2978, 3563));

        if (fuelType.isCompressionIgnition()) {
            spns.addAll(List.of(3719, 5466));
        } else if (fuelType.isSparkIgnition()) {
            spns.addAll(List.of(51, 3217, 3227, 3241, 3245, 3249, 3464, 4236, 4237, 4240));
        }

        return spns;
    }

    private static List<Integer> getInfoSPNs() {
        return new ArrayList<>(List.of(38, 96, 175));
    }

    private static List<String> getSupportedSPNs(Collection<Integer> supportedSPNs, GenericPacket packet) {
        return packet.getPgnDefinition()
                     .getSpnDefinitions()
                     .stream()
                     .map(SpnDefinition::getSpnId)
                     .filter(supportedSPNs::contains)
                     .sorted()
                     .map(Object::toString)
                     .collect(Collectors.toList());
    }

    private static List<Integer> getWarningSPNs(FuelType fuelType) {
        //@formatter:off
        var spns = new ArrayList<>(List.of(27, 84, 91, 94,
                                           106, 108, 110, 132, 157, 158, 183, 190, 235, 247, 248, 723,
                                           1127, 1413, 1433, 1436, 1600, 1637,
                                           2791,
                                           4076, 4193, 4201, 4202,
                                           5829, 5837,
                                           6393, 6895,
                                           7333));
        //@formatter:on

        if (fuelType.isCompressionIgnition()) {
            //@formatter:off
            spns.addAll(List.of(164, 
                                3031, 3226, 3251, 3515, 3516, 3518, 3609, 3610, 3700, 
                                5313, 5314, 5454, 5466, 5578, 5827, 
                                7346));
            //@formatter:on
        }
        return spns;
    }

    private Collection<Integer> getAllSupportedSPNs() {
        return getModuleSupportedSPNs(null);
    }

    private Map<Integer, List<Integer>> getMessages(int moduleAddress, ResultsListener listener) {
        Map<Integer, List<Integer>> pgnMap = new HashMap<>();

        String moduleName = Lookup.getAddressName(moduleAddress);

        OBDModuleInformation moduleInformation = dataRepository.getObdModule(moduleAddress);
        List<Integer> dataStreamSPNs = moduleInformation.getDataStreamSPNs()
                                                        .stream()
                                                        .map(SupportedSPN::getSpn)
                                                        .collect(Collectors.toList());

        moduleInformation.getOmittedDataStreamSPNs()
                         .stream()
                         .filter(dataStreamSPNs::contains)
                         .forEach(spn -> reportOmittedSPN(listener, moduleName, spn));

        moduleInformation.getFilteredDataStreamSPNs()
                         .stream()
                         .map(SupportedSPN::getSpn)
                         .sorted()
                         .distinct()
                         .forEach(spn -> {
                             Set<Integer> pgns = j1939DaRepository.getPgnForSpn(spn);
                             if (pgns == null) {
                                 listener.onResult("Unable to find PGN for SPN " + spn);
                             } else if (pgns.size() > 1) {
                                 reportOmittedSPN(listener, moduleName, spn);
                                 moduleInformation.addOmittedDataStreamSPN(spn);
                                 dataRepository.putObdModule(moduleInformation);
                             } else {
                                 int pgn = new ArrayList<>(pgns).get(0);
                                 List<Integer> spns = pgnMap.getOrDefault(pgn, new ArrayList<>());
                                 spns.add(spn);
                                 pgnMap.put(pgn, spns);
                             }
                         });

        // Sort SPNs
        for (Map.Entry<Integer, List<Integer>> pgn : pgnMap.entrySet()) {
            int key = pgn.getKey();
            List<Integer> spns = pgnMap.get(key)
                                       .stream()
                                       .sorted(Comparator.comparingInt(s -> s))
                                       .collect(Collectors.toList());
            pgnMap.put(key, spns);
        }
        return pgnMap;
    }

    private Collection<OBDModuleInformation> getModules(Integer moduleAddress) {
        Collection<OBDModuleInformation> modules;
        if (moduleAddress == null) {
            modules = dataRepository.getObdModules();
        } else {
            OBDModuleInformation obdModule = dataRepository.getObdModule(moduleAddress);
            if (obdModule == null) {
                modules = List.of(); // Don't return Supported SPNs for non-OBD
                                     // Modules
            } else {
                modules = List.of(obdModule);
            }
        }
        return modules;
    }

    private List<Integer> getModuleSupportedSPNs(Integer moduleAddress) {
        return getModules(moduleAddress)
                                        .stream()
                                        .flatMap(m -> m.getFilteredDataStreamSPNs().stream())
                                        .map(SupportedSPN::getSpn)
                                        .distinct()
                                        .sorted()
                                        .collect(Collectors.toList());
    }

    private Set<Integer> getReportedPGNs(int moduleAddress) {
        return foundPackets.getOrDefault(moduleAddress, new HashSet<>());
    }

    private boolean isReported(GenericPacket packet) {
        return getReportedPGNs(packet.getSourceAddress()).contains(packet.getPacket().getPgn());
    }

    /**
     * Writes a Failure/Warning if any SPNs is provided by more than one module
     */
    public void reportDuplicateSPNs(List<GenericPacket> packets,
                                    ResultsListener listener,
                                    String section) {
        // f. Fail/warn per Table A-1 if two or more ECUs provide an SPN listed in Table A-1
        Map<Integer, Integer> uniques = new HashMap<>();
        Map<Integer, Outcome> duplicateSPNs = new HashMap<>();

        for (GenericPacket packet : packets) {
            for (Spn spn : packet.getSpns()) {
                if (!spn.isNotAvailable()) {
                    int spnId = spn.getId();
                    Integer address = uniques.get(spnId);
                    if (address == null) {
                        uniques.put(spnId, packet.getSourceAddress());
                    } else if (address != packet.getSourceAddress()) {
                        Outcome outcome = Lookup.getOutcomeForDuplicateSpn(spnId);
                        if (outcome != PASS && !duplicateSPNs.containsKey(spnId)) {
                            duplicateSPNs.put(spnId, outcome);
                        }
                    }
                }
            }
        }

        duplicateSPNs.entrySet()
                     .stream()
                     .sorted(Comparator.comparingInt(Map.Entry::getKey))
                     .forEach(entry -> addOutcome(listener,
                                                  section,
                                                  entry.getValue(),
                                                  "N.5 SPN " + entry.getKey() + " provided by more than one ECU"));
    }

    public void reportExpectedMessages(ResultsListener listener) {
        listener.onResult("Expecting the following messages:");
        dataRepository.getObdModuleAddresses()
                      .stream()
                      .sorted()
                      .map(dataRepository::getObdModule)
                      .forEach(moduleInfo -> {
                          String moduleName = Lookup.getAddressName(moduleInfo.getSourceAddress());

                          Map<Integer, List<Integer>> pgnMap = getMessages(moduleInfo.getSourceAddress(), listener);
                          pgnMap.keySet().stream().sorted().forEach(pgn -> {
                              String spns = pgnMap.get(pgn)
                                                  .stream()
                                                  .sorted()
                                                  .map(Object::toString)
                                                  .collect(Collectors.joining(", "));
                              String msg = "PGN " + pgn + " from " + moduleName + " with SPNs " + spns;

                              PgnDefinition pgnDefinition = j1939DaRepository.findPgnDefinition(pgn);
                              if (pgnDefinition.isOnRequest()) {
                                  msg = "  Req " + msg;
                              } else {
                                  msg = "  BCT " + msg;
                              }

                              listener.onResult(msg);
                          });
                      });
    }

    /**
     * Reports on SPN values which are implausible given the key state and fuel type
     */
    public void reportImplausibleSPNValues(GenericPacket packet,
                                           ResultsListener listener,
                                           boolean isEngineRunning,
                                           String section) {

        int moduleAddress = packet.getSourceAddress();
        if (!dataRepository.isObdModule(moduleAddress)) {
            return;
        }

        Collection<Integer> moduleSPNs = getModuleSupportedSPNs(moduleAddress);

        packet.getSpns()
              .stream()
              .sorted(Comparator.comparingInt(Spn::getId))
              .forEach(spn -> {
                  int spnId = spn.getId();
                  Double value = spn.getValue();
                  if ((spn.isError() && moduleSPNs.contains(spnId))
                          || valueValidator.isImplausible(spnId, value, isEngineRunning)) {
                      Set<Integer> invalid = invalidSPNs.getOrDefault(moduleAddress, new HashSet<>());
                      if (!invalid.contains(spnId)) {
                          reportPacketIfNotReported(packet, listener, true);
                          String moduleName = Lookup.getAddressName(moduleAddress);

                          String message;
                          if (spn.isError()) {
                              message = "N.8 " + moduleName + " reported value for SPN " + spnId
                                      + " (ERROR) is implausible";
                          } else {
                              message = "N.8 " + moduleName + " reported value for SPN " + spnId + " (" + value
                                      + ") is implausible";
                          }
                          addOutcome(listener, section, WARN, message);
                          invalid.add(spnId);
                          invalidSPNs.put(moduleAddress, invalid);
                      }
                  }
              });
    }

    /**
     * Writes a Failures/Warning to the report if an OBD Supported SPN is provided by a Non-OBD Module.
     */
    public void reportNonObdModuleProvidedSPNs(GenericPacket packet,
                                               ResultsListener listener,
                                               String section) {

        int sourceAddress = packet.getSourceAddress();
        if (dataRepository.isObdModule(sourceAddress)) {
            return;
        }

        Collection<Integer> supportedSPNs = getAllSupportedSPNs();
        packet.getSpns()
              .stream()
              .filter(spn -> !spn.isNotAvailable())
              .map(Spn::getId)
              .filter(supportedSPNs::contains)
              .distinct()
              .sorted()
              .forEach(id -> {
                  Outcome outcome = Lookup.getOutcomeForNonObdModuleProvidingSpn(id);
                  if (outcome != PASS) {
                      Set<Integer> reported = nonObdProvidedSPNs.getOrDefault(sourceAddress, new HashSet<>());
                      if (!reported.contains(id)) {
                          String moduleName = Lookup.getAddressName(sourceAddress);
                          reportPacketIfNotReported(packet, listener, true);
                          addOutcome(listener,
                                     section,
                                     outcome,
                                     "N.6 SPN " + id + " provided by non-OBD ECU " + moduleName);
                          listener.onResult("");
                          reported.add(id);
                          nonObdProvidedSPNs.put(sourceAddress, reported);
                      }
                  }
              });
    }

    public void reportNotAvailableSPNs(GenericPacket packet,
                                       ResultsListener listener,
                                       String section) {

        int moduleAddress = packet.getSourceAddress();
        if (!dataRepository.isObdModule(moduleAddress)) {
            return;
        }

        Collection<Integer> moduleSPNs = getModuleSupportedSPNs(moduleAddress);

        // Find any Supported SPNs which has a value of Not Available
        packet.getSpns()
              .stream()
              .filter(Spn::isNotAvailable)
              .map(Spn::getId)
              .filter(moduleSPNs::contains)
              .sorted()
              .forEach(spn -> {
                  Set<Integer> naSPNs = notAvailableSPNs.getOrDefault(moduleAddress, new HashSet<>());
                  if (!naSPNs.contains(spn)) {
                      reportPacketIfNotReported(packet, listener, true);
                      String moduleName = Lookup.getAddressName(moduleAddress);
                      addOutcome(listener,
                                 section,
                                 Outcome.FAIL,
                                 "SPN " + spn + " was received as NOT AVAILABLE from " + moduleName);
                      listener.onResult("");
                      naSPNs.add(spn);
                      notAvailableSPNs.put(moduleAddress, naSPNs);

                  }
              });
    }

    private static void reportOmittedSPN(ResultsListener listener, String moduleName, Integer spn) {
        listener.onResult("  SPN " + spn + " is supported by " + moduleName + " but will be omitted" + NL);
    }

    public void reportPacketIfNotReported(GenericPacket packet, ResultsListener listener, boolean forceReporting) {
        if (!isReported(packet)) {
            int moduleAddress = packet.getSourceAddress();
            int pgn = packet.getPacket().getPgn();

            Collection<Integer> spns = dataRepository.isObdModule(moduleAddress) ? getModuleSupportedSPNs(moduleAddress)
                    : getAllSupportedSPNs();
            List<String> supportedSPNs = getSupportedSPNs(spns, packet);
            if (forceReporting || !supportedSPNs.isEmpty()) {
                listener.onResult("PGN " + pgn + " with Supported SPNs " + String.join(", ", supportedSPNs));
                listener.onResult(packet.getPacket().toTimeString());
                listener.onResult("Found: " + packet);
            }

            Set<Integer> modulePackets = getReportedPGNs(moduleAddress);
            modulePackets.add(pgn);
            foundPackets.put(moduleAddress, modulePackets);
        }
    }

    /**
     * Writes a Failure/Warning for any SPNs found in the data stream which aren't in the DM24
     */
    public void reportProvidedButNotSupportedSPNs(GenericPacket packet,
                                                  ResultsListener listener,
                                                  String section) {

        int sourceAddress = packet.getSourceAddress();
        if (!dataRepository.isObdModule(sourceAddress)) {
            return;
        }

        Collection<Integer> providedSPNs = packet.getSpns()
                                                 .stream()
                                                 .filter(s -> !s.isNotAvailable())
                                                 .map(Spn::getId)
                                                 .filter(s -> !getModuleSupportedSPNs(sourceAddress).contains(s))
                                                 .collect(Collectors.toSet());

        Map<Integer, Outcome> outcomes = new HashMap<>();

        // Failure SPNs
        List<Integer> failureSPNs = getFailureSPNs(getFuelType());
        providedSPNs.stream().filter(failureSPNs::contains).forEach(s -> outcomes.put(s, FAIL));

        // Warnings SPNs
        List<Integer> warningSPNs = getWarningSPNs(getFuelType());
        providedSPNs.stream().filter(warningSPNs::contains).forEach(s -> outcomes.put(s, WARN));

        // INFO SPNs
        List<Integer> infoSPNs = getInfoSPNs();
        providedSPNs.stream().filter(infoSPNs::contains).forEach(s -> outcomes.put(s, INFO));

        if (!outcomes.isEmpty()) {
            outcomes.keySet().stream().sorted().forEach(spn -> {
                Set<Integer> reportedSPNs = providedNotSupportedSPNs.getOrDefault(sourceAddress, new HashSet<>());
                if (!reportedSPNs.contains(spn)) {
                    reportPacketIfNotReported(packet, listener, true);
                    String moduleName = Lookup.getAddressName(sourceAddress);
                    addOutcome(listener,
                               section,
                               outcomes.get(spn),
                               "N.7 Provided SPN " + spn + " is not indicated as supported by " + moduleName);
                    listener.onResult("");
                    reportedSPNs.add(spn);
                    providedNotSupportedSPNs.put(sourceAddress, reportedSPNs);
                }
            });
        }
    }

    public void reset() {
        invalidSPNs.clear();
        notAvailableSPNs.clear();
        foundPackets.clear();
    }

    private FuelType getFuelType() {
        return dataRepository.getVehicleInformation().getFuelType();
    }

}
