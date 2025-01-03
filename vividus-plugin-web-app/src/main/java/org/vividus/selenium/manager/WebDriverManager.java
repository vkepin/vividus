/*
 * Copyright 2019-2025 the original author or authors.
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

package org.vividus.selenium.manager;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.eventbus.Subscribe;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Browser;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.cdp.BrowserPermissions;
import org.vividus.selenium.cdp.CdpWebDriverSessionAttribute;
import org.vividus.selenium.session.WebDriverSessionAttributes;
import org.vividus.selenium.session.WebDriverSessionInfo;
import org.vividus.ui.web.event.DeviceMetricsOverrideEvent;

public class WebDriverManager extends GenericWebDriverManager implements IWebDriverManager
{
    private final boolean remoteExecution;
    private boolean electronApp;
    private Dimension remoteScreenResolution;

    public WebDriverManager(boolean remoteExecution, IWebDriverProvider webDriverProvider,
            WebDriverSessionInfo webDriverSessionInfo)
    {
        super(webDriverProvider, webDriverSessionInfo);
        this.remoteExecution = remoteExecution;
    }

    @Override
    public boolean isBrowserAnyOf(Browser... browsers)
    {
        return isBrowserAnyOf(getCapabilities(), browsers);
    }

    public static boolean isBrowserAnyOf(WebDriver webDriver, Browser... browsers)
    {
        return isBrowserAnyOf(getCapabilities(webDriver), browsers);
    }

    public static boolean isBrowserAnyOf(Capabilities capabilities, Browser... browsers)
    {
        return checkCapabilities(capabilities,
                () -> Stream.of(browsers).anyMatch(browser -> isBrowser(capabilities, browser)));
    }

    public static boolean isBrowser(Capabilities capabilities, Browser browser)
    {
        // Workaround for https://github.com/SeleniumHQ/selenium/issues/13112
        return Browser.CHROME.equals(browser) && "chrome-headless-shell".equals(capabilities.getBrowserName())
                || browser.browserName().equalsIgnoreCase(capabilities.getBrowserName());
    }

    @Override
    public boolean isElectronApp()
    {
        return electronApp;
    }

    @Override
    public Optional<Dimension> getScreenResolution()
    {
        if (isRemoteExecution())
        {
            return Optional.ofNullable(remoteScreenResolution);
        }
        if (GraphicsEnvironment.isHeadless())
        {
            return Optional.empty();
        }
        java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return Optional.of(new Dimension(screenSize.width, screenSize.height));
    }

    @Override
    public boolean isRemoteExecution()
    {
        return getWebDriverProvider().isWebDriverInitialized() && remoteExecution;
    }

    @Override
    public BrowserPermissions getBrowserPermissions()
    {
        return getWebDriverSessionInfo().get(CdpWebDriverSessionAttribute.BROWSER_PERMISSIONS, BrowserPermissions::new);
    }

    @Subscribe
    public void onDeviceMetricsOverride(DeviceMetricsOverrideEvent event)
    {
        getWebDriverSessionInfo().reset(WebDriverSessionAttributes.SCREEN_SIZE);
    }

    public void setElectronApp(boolean electronApp)
    {
        this.electronApp = electronApp;
    }

    public void setRemoteScreenResolution(Dimension remoteScreenResolution)
    {
        this.remoteScreenResolution = remoteScreenResolution;
    }
}
