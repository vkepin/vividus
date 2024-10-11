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

package org.vividus.ui.web.listener;

import java.util.Map;

import com.google.common.eventbus.Subscribe;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.vividus.reporter.event.IAttachmentPublisher;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.softassert.event.AssertionFailedEvent;
import org.vividus.ui.ContextSourceCodeProvider;

@Conditional(AttachShadowDomSourcePropertyCondition.class)
@Component
@Lazy(false)
public class ShadowDomSourceCodePublishingOnFailureListener
{
    private final IWebDriverProvider webDriverProvider;
    private final ContextSourceCodeProvider contextSourceCodeProvider;
    private final IAttachmentPublisher attachmentPublisher;

    public ShadowDomSourceCodePublishingOnFailureListener(IWebDriverProvider webDriverProvider,
                                                          ContextSourceCodeProvider contextSourceCodeProvider,
                                                          IAttachmentPublisher attachmentPublisher)
    {
        this.webDriverProvider = webDriverProvider;
        this.contextSourceCodeProvider = contextSourceCodeProvider;
        this.attachmentPublisher = attachmentPublisher;
    }

    @Subscribe
    public void onAssertionFailure(AssertionFailedEvent event)
    {
        if (webDriverProvider.isWebDriverInitialized())
        {
            Map<String, String> shadowDomSourceCode = contextSourceCodeProvider.getShadowDomSourceCode();
            if (!shadowDomSourceCode.isEmpty())
            {
                attachmentPublisher.publishAttachment("/templates/shadow-code.ftl",
                        Map.of("shadowDomSources", shadowDomSourceCode), "Shadow DOM sources");
            }
        }
    }
}
