/*
 * This file is part of SpongeAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.api.statistic;

import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

/**
 * A utility class for getting all available {@link StatisticFormat}s.
 */
public final class StatisticFormats {

    // SORTFIELDS:ON

    /**
     * A statistic without a format.
     */
    public static StatisticFormat COUNT = DummyObjectProvider.createFor(StatisticFormat.class, "COUNT");

    /**
     * A statistic measured in centimeters, meters, or kilometers depending on
     * the magnitude. The input is taken as centimeters with a scale of 1 block
     * equaling 1 meter.
     */
    public static StatisticFormat DISTANCE = DummyObjectProvider.createFor(StatisticFormat.class, "DISTANCE");

    /**
     * A statistic measured in 0.1 steps.
     */
    public static StatisticFormat FRACTIONAL = DummyObjectProvider.createFor(StatisticFormat.class, "FRACTIONAL");

    /**
     * A statistic measured in seconds, minutes, hours, or days depending on the
     * magnitude. The input is taken as ticks with 20 ticks equaling one second.
     */
    public static StatisticFormat TIME = DummyObjectProvider.createFor(StatisticFormat.class, "TIME");

    // SORTFIELDS:OFF

    private StatisticFormats() {
    }

}
