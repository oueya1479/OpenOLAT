/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.opencast.manager.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.olat.modules.opencast.OpencastModule;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This tests are run again a real Opencast instance.
 * CAUTION: Do not run the thest agians a productive Opencast instance!
 * Do not add this file to AllTestsJUnit4. You should only run it manually.
 * 
 * Initial date: 5 Aug 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class OpencastRestClientTest extends OlatTestCase {
	
	@Autowired
	private OpencastModule opencastModule;
	
	@Autowired
	private OpencastRestClient sut;
	
	@Before
	public void setUp() {
		opencastModule.setApiUrl("http://localhost:8480");
		opencastModule.setApiCredentials("admin", "opencast");
	}

	@Test
	public void shouldGetApi() {
		Api api = sut.getApi();
		
		assertThat(api).isNotNull();
	}
	
	@Test
	public void shouldGetEvents() {
		GetEventsParams params = new GetEventsParams();
		params.getFilter().setTextFilter("8678c09a-9f76-4d60-b178-fb84d2b9c494");
		
		Event[] events = sut.getEvents(params);
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(events.length).isEqualTo(1);
		Event event = events[0];
		softly.assertThat(event.getIdentifier()).isNotNull();
		softly.assertThat(event.getTitle()).isNotNull();
		softly.assertThat(event.getStart()).isNotNull();
		softly.assertThat(event.getDuration()).isNotNull();
		softly.assertAll();
	}
	
}
