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
package org.spongepowered.api.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * A wrapper around a class marked with an {@link Plugin} annotation to retrieve
 * information from the annotation for easier use.
 */
public interface PluginContainer {

    /**
     * Gets the ID of the {@link Plugin} within this container.
     *
     * @return The plugin ID
     * @see Plugin#id()
     */
    String getId();

    /**
     * Gets the name of the {@link Plugin} within this container.
     *
     * @return The plugin name
     * @see Plugin#name()
     */
    String getName();

    /**
     * Gets the version of the {@link Plugin} within this container.
     *
     * @return The plugin version
     * @see Plugin#version()
     */
    String getVersion();

    /**
     * Gets the description of the {@link Plugin} within this container.
     *
     * @return The plugin description
     * @see Plugin#description()
     */
    String getDescription();

    /**
     * Gets the url or website of the {@link Plugin} within this container.
     *
     * @return The plugin url
     * @see Plugin#url()
     */
    String getUrl();

    /**
     * Gets the authors of the {@link Plugin} within this container.
     *
     * @return The plugin authors
     * @see Plugin#authors()
     */
    List<String> getAuthors();

    /**
     * Returns the assigned logger to this {@link Plugin}.
     *
     * @return The assigned logger
     */
    default Logger getLogger() {
        return LoggerFactory.getLogger(getId());
    }

    /**
     * Returns the created instance of {@link Plugin} if it is available.
     *
     * @return The instance if available
     */
    Optional<Object> getInstance();

}
