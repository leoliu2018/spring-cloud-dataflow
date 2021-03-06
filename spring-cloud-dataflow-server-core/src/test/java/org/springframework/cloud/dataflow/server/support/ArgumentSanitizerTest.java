/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.support;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.controller.support.ArgumentSanitizer;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public class ArgumentSanitizerTest {

	private ArgumentSanitizer sanitizer;

	private static final String[] keys = { "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services" };

	@Before
	public void before() {
		sanitizer = new ArgumentSanitizer();
	}

	@Test
	public void testSanitizeProperties() {
		for (String key : keys) {
			Assert.assertEquals("--" + key + "=******", sanitizer.sanitize("--" + key + "=foo"));
			Assert.assertEquals("******", sanitizer.sanitize(key, "bar"));
		}
	}


	@Test
	public void testMultipartProperty() {
		Assert.assertEquals("--password=******", sanitizer.sanitize("--password=boza"));
		Assert.assertEquals("--one.two.password=******", sanitizer.sanitize("--one.two.password=boza"));
		Assert.assertEquals("--one_two_password=******", sanitizer.sanitize("--one_two_password=boza"));
	}

	@Test
	public void testHierarchicalPropertyNames() {
		Assert.assertEquals("time --password='******' | log",
				sanitizer.sanitizeStream(new StreamDefinition("stream", "time --password=bar | log")));
	}

	@Test
	public void testStreamPropertyOrder() {
		Assert.assertEquals("time --another-secret='******' --some.password='******' | log",  //FIXME Order should be "time --some.password='******' --another-secret='******' | log"
				sanitizer.sanitizeStream(new StreamDefinition("stream", "time --some.password=foobar --another-secret=kenny | log")));
	}

	@Test
	public void testStreamMatcherWithHyphenDotChar() {
		Assert.assertEquals("twitterstream --twitter.credentials.access-token-secret='******' "
						+ "--twitter.credentials.access-token='******' --twitter.credentials.consumer-secret='******' "
						+ "--twitter.credentials.consumer-key='******' | "
						+ "filter --expression=#jsonPath(payload,'$.lang')=='en' | "
						+ "twitter-sentiment --vocabulary=http://dl.bintray.com/test --model-fetch=output/test "
						+ "--model=http://dl.bintray.com/test | field-value-counter --field-name=sentiment --name=sentiment",
				sanitizer.sanitizeStream(new StreamDefinition("stream", "twitterstream "
						+ "--twitter.credentials.consumer-key=dadadfaf --twitter.credentials.consumer-secret=dadfdasfdads "
						+ "--twitter.credentials.access-token=58849055-dfdae "
						+ "--twitter.credentials.access-token-secret=deteegdssa4466 | filter --expression='#jsonPath(payload,''$.lang'')==''en''' | "
						+ "twitter-sentiment --vocabulary=http://dl.bintray.com/test --model-fetch=output/test --model=http://dl.bintray.com/test | "
						+ "field-value-counter --field-name=sentiment --name=sentiment")));
	}
}
