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
package org.spongepowered.api.text.serializer;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.format.TextFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@link TypeSerializer} for {@link TextTemplate}s. TextTemplates
 * are serialized in two parts.
 *
 * <p>First, the template's arguments as defined by
 * {@link TextTemplate#getArguments()} are serialized to the "arguments" node.
 * This is where the argument definitions are kept.</p>
 *
 * <p>Second, the template's text representation as defined by
 * {@link TextTemplate#toText()} is serialized to the "content" node.</p>
 *
 * <p>Deserialization is a bit more complicated. We start by loading the
 * "content" Text and check the root Text element as well as it's children. If
 * a {@link LiteralText} value is found that is wrapped in curly braces we
 * check to see if the value inside the braces is defined as an argument in the
 * "arguments" nodes. If so, we use the name and format from the original
 * LiteralText and obtain whether the argument is optional from the definition.
 * This is repeated until there are no more Text children to check and we
 * return a TextTemplate of the elements we have collected.</p>
 */
public class TextTemplateConfigSerializer implements TypeSerializer<TextTemplate> {

    private static final String NODE_CONTENT = "content";
    private static final String NODE_ARGS = "arguments";
    private static final String NODE_OPT = "optional";
    private static final String NODE_OPEN_ARG = "openArg";
    private static final String NODE_CLOSE_ARG = "closeArg";

    private ConfigurationNode root;
    private String openArg;
    private String closeArg;

    @Override
    public TextTemplate deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        this.root = value;
        this.openArg = value.getNode(NODE_OPEN_ARG).getString(TextTemplate.DEFAULT_OPEN_ARG);
        this.closeArg = value.getNode(NODE_CLOSE_ARG).getString(TextTemplate.DEFAULT_CLOSE_ARG);
        Text content = value.getNode(NODE_CONTENT).getValue(TypeToken.of(Text.class));
        List<Object> elements = new ArrayList<>();
        parse(elements, content);
        return TextTemplate.of(elements.toArray(new Object[elements.size()]));
    }

    @Override
    public void serialize(TypeToken<?> type, TextTemplate obj, ConfigurationNode value) throws ObjectMappingException {
        String openArg = obj.getOpenArgString(), closeArg = obj.getCloseArgString();
        if (!openArg.equals(TextTemplate.DEFAULT_OPEN_ARG) || !closeArg.equals(TextTemplate.DEFAULT_CLOSE_ARG)) {
            // only display if different from default, reduces clutter
            value.getNode(NODE_OPEN_ARG).setValue(openArg);
            value.getNode(NODE_CLOSE_ARG).setValue(closeArg);
        }
        value.getNode(NODE_ARGS).setValue(new TypeToken<Map<String, TextTemplate.Arg>>() {}, obj.getArguments());
        value.getNode(NODE_CONTENT).setValue(TypeToken.of(Text.class), obj.toText());
    }

    private void parse(List<Object> into, Text content) {
        if (isArg(content)) {
            parseArg(into, (LiteralText) content);
        } else {
            into.add(content.toBuilder().removeAll().build());
        }
        content.getChildren().stream().forEach(c -> parse(into, c));
    }

    private void parseArg(List<Object> into, LiteralText source) {
        String name = unwrap(source.getContent(), this.openArg, this.closeArg);
        boolean optional = this.root.getNode(NODE_ARGS, name, NODE_OPT).getBoolean();
        TextFormat format = source.getFormat();
        TextTemplate.Arg arg = TextTemplate.arg(name).format(format).optional(optional).build();
        into.add(arg);
    }

    private boolean isArg(Text element) {
        if (!(element instanceof LiteralText)) {
            return false;
        }
        String literal = ((LiteralText) element).getContent();
        return literal.startsWith(this.openArg) && literal.endsWith(this.closeArg)
                && isArgDefined(unwrap(literal, this.openArg, this.closeArg), this.root);
    }

    private static String unwrap(String str, String openArg, String closeArg) {
        return str.substring(openArg.length(), str.length() - closeArg.length());
    }


    private static boolean isArgDefined(String argName, ConfigurationNode root) {
        return !root.getNode(NODE_ARGS, argName).isVirtual();
    }
}
