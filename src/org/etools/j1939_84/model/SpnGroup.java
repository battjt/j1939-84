/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class SpnGroup {
    public final List<Integer> spns;

    public SpnGroup(Integer... spns) {
        this.spns = Arrays.asList(spns);
    }

    public boolean isSatisfied(Collection<Integer> supportedSpnValues) {
        return supportedSpnValues.stream().anyMatch(spns::contains);
    }

    @Override
    public String toString() {
        return "SPNs: " + spns.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
}
