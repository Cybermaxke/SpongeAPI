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
package org.spongepowered.api.text;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.serializer.TextTemplateConfigSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Represents a re-usable template that produces a formatted
 * {@link Text.Builder}. Elements will be appended to the result builder in the
 * order that they are specified in {@link #of(Object...)}.
 */
public final class TextTemplate implements TextRepresentable {

    static {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(TextTemplate.class), new TextTemplateConfigSerializer());
    }

    /**
     * Default "open" String for how arguments are contained within the template.
     */
    public static final String DEFAULT_OPEN_ARG = "{";

    /**
     * Default "close" String for how arguments are contained within the template.
     */
    public static final String DEFAULT_CLOSE_ARG = "}";

    /**
     * Empty representation of a {@link TextTemplate}. This is returned if the
     * array supplied to {@link #of(Object...)} is empty.
     */
    public static final TextTemplate EMPTY = new TextTemplate(DEFAULT_OPEN_ARG, DEFAULT_CLOSE_ARG, new Object[]{});

    final ImmutableList<Object> elements;
    final ImmutableMap<String, Arg> arguments;
    final String openArg;
    final String closeArg;

    TextTemplate(String openArg, String closeArg, Object[] elements) {
        this.openArg = openArg;
        this.closeArg = closeArg;
        List<Object> elementList = new ArrayList<>();
        Map<String, Arg> argumentMap = new HashMap<>();
        for (Object element : elements) {
            if (element instanceof Arg.Builder) {
                element = ((Arg.Builder) element).build();
            }
            elementList.add(element);
            if (element instanceof Arg) {
                // check for non-equal duplicate argument
                Arg newArg = new Arg((Arg) element, this.openArg, this.closeArg);
                Arg oldArg = argumentMap.get(newArg.name);
                if (oldArg != null && !oldArg.equals(newArg)) {
                    throw new TextTemplateArgumentException("Tried to supply an unequal argument with a duplicate name \""
                            + newArg.name + "\" to TextTemplate.");
                }
                argumentMap.put(newArg.name, newArg);
            }
        }
        this.elements = ImmutableList.copyOf(elementList);
        this.arguments = ImmutableMap.copyOf(argumentMap);
    }

    /**
     * Returns the elements contained in this TextTemplate.
     *
     * @return The elements within the template
     */
    public List<Object> getElements() {
        return this.elements;
    }

    /**
     * Returns the arguments contained within the TextTemplate.
     *
     * @return The arguments within this TextTemplate
     */
    public Map<String, Arg> getArguments() {
        return this.arguments;
    }

    /**
     * Returns the string used for containing Args within the template.
     *
     * @return String containing args
     */
    public String getOpenArgString() {
        return this.openArg;
    }

    /**
     * Returns the string used for containing Args within the template.
     *
     * @return String containing args
     */
    public String getCloseArgString() {
        return this.closeArg;
    }

    /**
     * Applies the specified parameters to this TextTemplate and returns the
     * result in a {@link Text.Builder}.
     *
     * @param params Parameters to apply
     * @return Text builder containing result
     * @throws TextTemplateArgumentException if required parameters are missing
     */
    public Text.Builder apply(Map<String, TextElement> params) {
        checkNotNull("params");
        Text.Builder result = null;
        for (Object element : this.elements) {
            result = apply(result, element, params);
        }
        return Optional.ofNullable(result).orElse(Text.builder());
    }

    /**
     * Applies an empty map of parameters to this TextTemplate and returns the
     * result in a {@link Text.Builder}.
     *
     * @return Text builder containing result
     * @throws TextTemplateArgumentException if required parameters are missing
     */
    public Text.Builder apply() {
        return apply(Collections.emptyMap());
    }

    @Nullable
    private Text.Builder apply(@Nullable Text.Builder builder, Object element, Map<String, TextElement> params) {
        // Note: The builder is initialized as null to avoid unnecessary Text nesting
        if (element instanceof Arg) {
            Arg arg = (Arg) element;
            TextElement param = params.get(arg.name);
            if (param == null) {
                arg.checkOptional(); // make sure param is allowed to be missing
            } else {
                // wrap the parameter in the argument format
                Text.Builder wrapper = Text.builder().format(arg.format);
                param.applyTo(wrapper);
                if (builder == null) {
                    builder = wrapper; // set wrapper to "root" Text
                } else {
                    builder.append(wrapper.build());
                }
            }
        } else if (element instanceof TextElement) {
            if (builder == null) {
                builder = Text.builder();
            }
            ((TextElement) element).applyTo(builder);
        } else {
            String str = element.toString();
            if (builder == null) {
                builder = Text.builder(str); // set String to "root" Text
            } else {
                builder.append(Text.of(str));
            }
        }
        return builder;
    }

    /**
     * Constructs a new TextTemplate for the given elements. The order of the
     * elements is the order in which they will be appended to the result
     * builder via {@link #apply(Map)}.
     *
     * <p>The provided elements may be of any type.</p>
     *
     * <p>In the case that an element is a {@link TextElement},
     * {@link TextElement#applyTo(Text.Builder)} will be used to append the
     * element to the builder.</p>
     *
     * <p>In the case that an element is an {@link Arg} the argument will be
     * replaced with the {@link TextElement} provided by the corresponding
     * parameter supplied by {@link #apply(Map)}</p>
     *
     * <p>In the case that an element is any other type, the parameter value's
     * {@link Object#toString()} method will be used to create a {@link Text}
     * object.</p>
     *
     * @param elements Elements to append to builder
     * @param openArg String to use for beginning of Arg containers
     * @param closeArg String to use for end of Arg containers
     * @return Newly constructed TextTemplate
     */
    public static TextTemplate of(String openArg, String closeArg, Object[] elements) {
        checkNotNull(elements, "elements");
        if (elements.length == 0) {
            return of();
        }
        return new TextTemplate(openArg, closeArg, elements);
    }

    /**
     * Constructs a new TextTemplate for the given elements. The order of the
     * elements is the order in which they will be appended to the result
     * builder via {@link #apply(Map)}.
     *
     * <p>The provided elements may be of any type.</p>
     *
     * <p>In the case that an element is a {@link TextElement},
     * {@link TextElement#applyTo(Text.Builder)} will be used to append the
     * element to the builder.</p>
     *
     * <p>In the case that an element is an {@link Arg} the argument will be
     * replaced with the {@link TextElement} provided by the corresponding
     * parameter supplied by {@link #apply(Map)}</p>
     *
     * <p>In the case that an element is any other type, the parameter value's
     * {@link Object#toString()} method will be used to create a {@link Text}
     * object.</p>
     *
     * @param elements Elements to append to builder
     * @return Newly constructed TextTemplate
     */
    public static TextTemplate of(Object... elements) {
        return of(DEFAULT_OPEN_ARG, DEFAULT_CLOSE_ARG, elements);
    }

    /**
     * Returns the empty representation of a TextTemplate.
     *
     * @return Empty TextTemplate
     */
    public static TextTemplate of() {
        return EMPTY;
    }

    /**
     * Constructs a new {@link Arg} to be supplied to {@link #of(Object...)}.
     * This argument expects a {@link TextElement} parameter.
     *
     * @param name name of argument
     * @return argument builder
     */
    public static Arg.Builder arg(String name) {
        return new Arg.Builder(name);
    }

    @Override
    public Text toText() {
        Text.Builder builder = null;
        for (Object element : this.elements) {
            if (element instanceof TextElement) {
                if (builder == null) {
                    builder = Text.builder();
                }
                ((TextElement) element).applyTo(builder);
            } else {
                String str = element.toString();
                if (builder == null) {
                    builder = Text.builder(str);
                } else {
                    builder.append(Text.of(str));
                }
            }
        }
        return Optional.ofNullable(builder).orElse(Text.builder()).build();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(TextTemplate.class)
                .add("elements", this.elements)
                .add("arguments", this.arguments)
                .add("openArg", this.openArg)
                .add("closeArg", this.closeArg)
                .toString();
    }

    /**
     * Represents a variable element within a TextTemplate. Arguments are
     * replaced by parameters in {@link #apply(Map)}.
     */
    @ConfigSerializable
    public final static class Arg implements TextRepresentable {

        @Setting final boolean optional;
        final String name; // defined by node name
        final TextFormat format; // defined in "content" node
        final String openArg;
        final String closeArg;

        Arg(String name, boolean optional, TextFormat format, String openArg, String closeArg) {
            this.name = name;
            this.optional = optional;
            this.format = format;
            this.openArg = openArg;
            this.closeArg = closeArg;
        }

        Arg(String name, boolean optional, TextFormat format) {
            this(name, optional, format, DEFAULT_OPEN_ARG, DEFAULT_CLOSE_ARG);
        }

        Arg(Arg arg, String openArg, String closeArg) {
            this(arg.name, arg.optional, arg.format, openArg, closeArg);
        }

        void checkOptional() {
            if (!this.optional) {
                throw new TextTemplateArgumentException("Missing required argument in TextTemplate \"" + this.name + "\".");
            }
        }

        /**
         * Returns the name of this argument to be matched with incoming
         * parameters.
         *
         * @return Argument name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Returns true if this Arg is optional. If a parameter is missing for
         * a non-optional Arg, a {@link TextTemplateArgumentException} will be
         * thrown.
         *
         * @return True if optional
         */
        public boolean isOptional() {
            return this.optional;
        }

        /**
         * Returns the base format to be applied to this Arg.
         *
         * @return Base format
         */
        public TextFormat getFormat() {
            return this.format;
        }

        /**
         * Returns the beginning string of the Arg's container.
         *
         * @return Open string
         */
        public String getOpenArgString() {
            return this.openArg;
        }

        /**
         * Returns the end string of the Arg's container.
         *
         * @return Close string
         */
        public String getCloseArgString() {
            return this.closeArg;
        }

        @Override
        public Text toText() {
            return Text.builder(openArg + this.name + closeArg)
                    .format(this.format).build();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(Arg.class)
                    .omitNullValues()
                    .add("optional", optional)
                    .add("name", name)
                    .add("format", this.format.isEmpty() ? null : this.format)
                    .add("openArg", openArg)
                    .add("closeArg", closeArg)
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.name, this.optional);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Arg)) {
                return false;
            }
            Arg that = (Arg) obj;
            return that.name.equals(this.name) && that.optional == this.optional;
        }

        /**
         * Represents a builder for {@link Arg}s.
         */
        public static final class Builder {

            final String name;
            boolean optional = false;
            TextFormat format = TextFormat.NONE;

            Builder(String name) {
                this.name = name;
            }

            /**
             * Builds a new {@link Arg}. Note that it is not necessary to call
             * this method when supplying an argument to a template. You may
             * pass the builder to {@link TextTemplate#of(Object...)} directly.
             *
             * @return Newly created Arg
             */
            public Arg build() {
                return new Arg(this.name, this.optional, this.format);
            }

            /**
             * Sets whether the Arg should be optional (false by default).
             *
             * @param optional True if should be optional
             * @return This builder
             */
            public Builder optional(boolean optional) {
                this.optional = optional;
                return this;
            }

            /**
             * Makes the Arg optional.
             *
             * @return This builder
             */
            public Builder optional() {
                return optional(true);
            }

            /**
             * Sets the "base" format of the Arg. This acts as a default format
             * when no formatting data is provided by the parameter.
             *
             * @param format Base format of Arg
             * @return This builder
             */
            public Builder format(TextFormat format) {
                this.format = format;
                return this;
            }

            /**
             * Sets the "base" color of the Arg. This acts as a default color
             * when no color data is provided by the parameter.
             *
             * @param color Base color of Arg
             * @return This builder
             */
            public Builder color(TextColor color) {
                this.format = this.format.color(color);
                return this;
            }

            /**
             * Sets the "base" style of the Arg. This acts as a default style
             * when no style data is provided by the parameter.
             *
             * @param style Base style of Arg
             * @return This builder
             */
            public Builder style(TextStyle style) {
                this.format = this.format.style(style);
                return this;
            }
        }
    }

}
