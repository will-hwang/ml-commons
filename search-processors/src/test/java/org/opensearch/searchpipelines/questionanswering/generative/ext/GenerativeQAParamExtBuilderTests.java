/*
 * Copyright 2023 Aryn
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.searchpipelines.questionanswering.generative.ext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opensearch.core.xcontent.ToXContent.EMPTY_PARAMS;

import java.io.EOFException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.search.SearchModule;
import org.opensearch.searchpipelines.questionanswering.generative.llm.MessageBlock;
import org.opensearch.test.OpenSearchTestCase;

public class GenerativeQAParamExtBuilderTests extends OpenSearchTestCase {

    private List<MessageBlock> messageList = null;

    public GenerativeQAParamExtBuilderTests() {
        Map<String, ?> imageMap = Map.of("image", Map.of("format", "jpg", "url", "https://xyz.com/file.jpg"));
        Map<String, ?> textMap = Map.of("text", "what is this");
        Map<String, ?> contentMap = Map.of();
        Map<String, ?> map = Map.of("role", "user", "content", List.of(textMap, imageMap));
        MessageBlock mb = new MessageBlock(map);
        messageList = List.of(mb);
    }

    public void testCtor() throws IOException {
        GenerativeQAParamExtBuilder builder = new GenerativeQAParamExtBuilder();
        GenerativeQAParameters parameters = new GenerativeQAParameters(
            "conversation_id",
            "model_id",
            "question",
            "system_promtp",
            "user_instructions",
            null,
            null,
            null,
            null
        );
        builder.setParams(parameters);
        assertEquals(parameters, builder.getParams());

        GenerativeQAParamExtBuilder builder1 = new GenerativeQAParamExtBuilder(new StreamInput() {
            @Override
            public byte readByte() throws IOException {
                return 0;
            }

            @Override
            public void readBytes(byte[] b, int offset, int len) throws IOException {

            }

            @Override
            public void close() throws IOException {

            }

            @Override
            public int available() throws IOException {
                return 0;
            }

            @Override
            protected void ensureCanReadBytes(int length) throws EOFException {

            }

            @Override
            public int read() throws IOException {
                return 0;
            }
        });

        assertNotNull(builder1);
    }

    public void testMiscMethods() throws IOException {
        GenerativeQAParameters param1 = new GenerativeQAParameters("a", "b", "c", "s", "u", null, null, null, null);
        GenerativeQAParameters param2 = new GenerativeQAParameters("a", "b", "d", "s", "u", null, null, null, null);
        GenerativeQAParamExtBuilder builder1 = new GenerativeQAParamExtBuilder();
        GenerativeQAParamExtBuilder builder2 = new GenerativeQAParamExtBuilder();
        builder1.setParams(param1);
        builder2.setParams(param2);
        assertEquals(builder1, builder1);
        assertNotEquals(builder1, param1);
        assertNotEquals(builder1, builder2);
        assertNotEquals(builder1.hashCode(), builder2.hashCode());

        StreamOutput so = mock(StreamOutput.class);
        builder1.writeTo(so);
        verify(so, times(5)).writeOptionalString(any());
        verify(so, times(1)).writeString(any());
    }

    public void testParse() throws IOException {
        String requiredJsonStr = "{\"llm_question\":\"this is test llm question\"}";

        XContentParser parser = XContentType.JSON
            .xContent()
            .createParser(
                new NamedXContentRegistry(new SearchModule(Settings.EMPTY, Collections.emptyList()).getNamedXContents()),
                null,
                requiredJsonStr
            );

        parser.nextToken();
        GenerativeQAParamExtBuilder builder = GenerativeQAParamExtBuilder.parse(parser);
        assertNotNull(builder);
        assertNotNull(builder.getParams());
        GenerativeQAParameters params = builder.getParams();
        Assert.assertEquals("this is test llm question", params.getLlmQuestion());
    }

    public void testXContentRoundTrip() throws IOException {
        GenerativeQAParameters param1 = new GenerativeQAParameters("a", "b", "c", "s", "u", null, null, null, null, messageList);
        GenerativeQAParamExtBuilder extBuilder = new GenerativeQAParamExtBuilder();
        extBuilder.setParams(param1);

        XContentType xContentType = randomFrom(XContentType.values());
        XContentBuilder builder = XContentBuilder.builder(xContentType.xContent());
        builder = extBuilder.toXContent(builder, EMPTY_PARAMS);
        BytesReference serialized = BytesReference.bytes(builder);

        XContentParser parser = createParser(xContentType.xContent(), serialized);
        parser.nextToken();
        GenerativeQAParamExtBuilder deserialized = GenerativeQAParamExtBuilder.parse(parser);

        assertEquals(extBuilder, deserialized);
        GenerativeQAParameters parameters = deserialized.getParams();
        assertTrue(GenerativeQAParameters.SIZE_NULL_VALUE == parameters.getContextSize());
        assertTrue(GenerativeQAParameters.SIZE_NULL_VALUE == parameters.getInteractionSize());
        assertTrue(GenerativeQAParameters.SIZE_NULL_VALUE == parameters.getTimeout());
    }

    public void testXContentRoundTripAllValues() throws IOException {
        GenerativeQAParameters param1 = new GenerativeQAParameters("a", "b", "c", "s", "u", 1, 2, 3, null);
        GenerativeQAParamExtBuilder extBuilder = new GenerativeQAParamExtBuilder();
        extBuilder.setParams(param1);

        XContentType xContentType = randomFrom(XContentType.values());
        XContentBuilder builder = XContentBuilder.builder(xContentType.xContent());
        builder = extBuilder.toXContent(builder, EMPTY_PARAMS);
        BytesReference serialized = BytesReference.bytes(builder);

        XContentParser parser = createParser(xContentType.xContent(), serialized);
        parser.nextToken();
        GenerativeQAParamExtBuilder deserialized = GenerativeQAParamExtBuilder.parse(parser);

        assertEquals(extBuilder, deserialized);
    }

    public void testStreamRoundTrip() throws IOException {
        GenerativeQAParameters param1 = new GenerativeQAParameters("a", "b", "c", "s", "u", null, null, null, null);
        GenerativeQAParamExtBuilder extBuilder = new GenerativeQAParamExtBuilder();
        extBuilder.setParams(param1);
        BytesStreamOutput bso = new BytesStreamOutput();
        extBuilder.writeTo(bso);
        GenerativeQAParamExtBuilder deserialized = new GenerativeQAParamExtBuilder(bso.bytes().streamInput());
        assertEquals(extBuilder, deserialized);
        GenerativeQAParameters parameters = deserialized.getParams();
        assertTrue(GenerativeQAParameters.SIZE_NULL_VALUE == parameters.getContextSize());
        assertTrue(GenerativeQAParameters.SIZE_NULL_VALUE == parameters.getInteractionSize());
        assertTrue(GenerativeQAParameters.SIZE_NULL_VALUE == parameters.getTimeout());
    }

    public void testStreamRoundTripAllValues() throws IOException {
        GenerativeQAParameters param1 = new GenerativeQAParameters("a", "b", "c", "s", "u", 1, 2, 3, null);
        GenerativeQAParamExtBuilder extBuilder = new GenerativeQAParamExtBuilder();
        extBuilder.setParams(param1);
        BytesStreamOutput bso = new BytesStreamOutput();
        extBuilder.writeTo(bso);
        GenerativeQAParamExtBuilder deserialized = new GenerativeQAParamExtBuilder(bso.bytes().streamInput());
        assertEquals(extBuilder, deserialized);
    }
}
