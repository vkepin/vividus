/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.selenium.mobileapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;
import org.vividus.selenium.IWebDriverProvider;

@ExtendWith(MockitoExtension.class)
class MobileContextSourceCodeProviderTests
{
    @Mock private IWebDriverProvider webDriverProvider;
    @InjectMocks private MobileContextSourceCodeProvider sourceCodeProvider;

    @Test
    void shouldReturnSourceCode()
    {
        WebDriver webDriver = mock(WebDriver.class);
        String source = "</beans>";
        when(webDriver.getPageSource()).thenReturn(source);
        when(webDriverProvider.get()).thenReturn(webDriver);
        assertEquals(Map.of("Application source code", source), sourceCodeProvider.getSourceCode());
    }

    @Test
    void shouldReturnException()
    {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                sourceCodeProvider::getShadowDomSourceCode);
        assertEquals("Method is not supported in the mobile context", exception.getMessage());
    }
}
