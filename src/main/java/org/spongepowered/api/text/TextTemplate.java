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
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
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
public class TextTemplate implements TextRepresentable {

    static {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(TextTemplate.class), new TextTemplateConfigSerializer());
    }

    /**
     * Empty representation of a {@link TextTemplate}. This is returned if the
     * array supplied to {@link #of(Object...)} is empty.
     */
    public static final TextTemplate EMPTY = new TextTemplate();

    protected final List<Object> elements = new ArrayList<>();
    protected final Map<String, Arg> arguments = new HashMap<>();

    protected TextTemplate(Object... elements) {
        for (Object element : elements) {
            if (element instanceof Arg.Builder) {
                element = ((Arg.Builder) element).build();
            }
            this.elements.add(element);
            if (element instanceof Arg) {
                // check for non-equal duplicate argument
                Arg newArg = (Arg) element;
                Arg oldArg = this.arguments.get(newArg.name);
                if (oldArg != null && !oldArg.equals(newArg)) {
                    throw new TextTemplateArgumentException("Tried to supply an unequal argument with a duplicate name \""
                            + newArg.name + "\" to TextTemplate.");
                }
                arguments.put(newArg.name, newArg);
            }
        }
    }

    /**
     * Returns the elements contained in this TextTemplate.
     *
     * @return The elements within the template
     */
    public List<Object> getElements() {
        return ImmutableList.copyOf(this.elements);
    }

    /**
     * Returns the arguments contained within the TextTemplate.
     *
     * @return The arguments within this TextTemplate
     */
    public Map<String, Arg> getArguments() {
        return ImmutableBiMap.copyOf(this.arguments);
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
     * @return Newly constructed TextTemplate
     */
    public static TextTemplate of(Object... elements) {
        checkNotNull(elements, "elements");
        if (elements.length == 0) {
            return of();
        }
        return new TextTemplate(elements);
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

    /**
     * Represents a variable element within a TextTemplate. Arguments are
     * replaced by parameters in {@link #apply(Map)}.
     */
    @ConfigSerializable
    public static class Arg implements TextRepresentable {
        @Setting protected final boolean optional;
        protected final String name; // defined by node name
        protected final TextFormat format; // defined in "content" node

        protected Arg(String name, boolean optional, TextFormat format) {
            this.name = name;
            this.optional = optional;
            this.format = format;
        }

        protected void checkOptional() {
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
            return name;
        }

        /**
         * Returns true if this Arg is optional. If a parameter is missing for
         * a non-optional Arg, a {@link TextTemplateArgumentException} will be
         * thrown.
         *
         * @return True if optional
         */
        public boolean isOptional() {
            return optional;
        }

        /**
         * Returns the base format to be applied to this Arg.
         *
         * @return Base format
         */
        public TextFormat getFormat() {
            return format;
        }

        @Override
        public Text toText() {
            return Text.builder('{' + this.name + '}').format(this.format).build();
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
        public static class Builder {
            protected final String name;
            protected boolean optional = false;
            protected TextFormat format = TextFormat.NONE;

            protected Builder(String name) {
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
                return new Arg(name, optional, format);
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
