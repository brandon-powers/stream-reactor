/*
 * Copyright 2017-2024 Lenses.io Ltd
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
package io.lenses.streamreactor.connect.gcp.common.auth.mode;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DefaultAuthModeTest {

    @Test
    void getCredentialsShouldReturnCredentials() throws Exception {
        val mockCreds = mock(GoogleCredentials.class);

        try (MockedStatic<GoogleCredentials> mockedStatic = mockStatic(GoogleCredentials.class)) {
            when(GoogleCredentials.getApplicationDefault()).thenReturn(mockCreds);

            val credentials = new DefaultAuthMode().getCredentials();
            assertEquals(mockCreds, credentials);

            mockedStatic.verify(GoogleCredentials::getApplicationDefault);
        }
    }
}