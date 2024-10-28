/*
 * Copyright 2019-2022 the original author or authors.
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

package org.vividus.ui.web.action.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.vividus.selenium.locator.Locator;
import org.vividus.selenium.locator.RelativeElementPosition;
import org.vividus.spring.StringToLocatorConverter;
import org.vividus.steps.ui.validation.IBaseValidations;
import org.vividus.ui.action.search.IElementSearchAction;
import org.vividus.ui.action.search.SearchParameters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RelativeElementSearch extends AbstractWebElementSearchAction implements IElementSearchAction
{
    private static final Pattern ITEM_TO_FIND_LOCATOR_PATTERN = Pattern.compile("^(\\w+\\([^)]+\\))");
    private static final Pattern RELATIVE_LOCATORS_PATTERN = Pattern.compile("\\.(\\w+)\\((\\w+\\([^)]+\\))\\)");

    private StringToLocatorConverter converter;
    private IBaseValidations baseValidations;

    public RelativeElementSearch()
    {
        super(WebLocatorType.RELATIVE);
    }

    @Override
    public List<WebElement> search(SearchContext searchContext, SearchParameters parameters)
    {
        Optional<By> fullLocator = convertRelativeStringToBy(searchContext, parameters.getValue());
        return fullLocator.isPresent() ? findElements(searchContext, fullLocator.get(), parameters) : List.of();
    }

    private Optional<By> convertRelativeStringToBy(SearchContext searchContext, String searchParams)
    {
        Matcher itemToFindMatcher = ITEM_TO_FIND_LOCATOR_PATTERN.matcher(searchParams);
        if (itemToFindMatcher.find())
        {
            final String itemToFindLocatorString = itemToFindMatcher.group(0);
            String relativePart = searchParams.substring(itemToFindLocatorString.length());

            Locator itemToFindLocator = converter.convert(itemToFindLocatorString);
            By itemToFindBy = itemToFindLocator.getLocatorType().buildBy(itemToFindLocator.getSearchParameters().getValue());
            RelativeLocator.RelativeBy relativeBy = RelativeLocator.with(itemToFindBy);

            for (Map.Entry<String, String> relativeLocator : getRelativeLocators(relativePart).entrySet())
            {
                RelativeElementPosition relativeElementPosition = findRelativePosition(relativeLocator.getValue());
                Locator locator = converter.convert(relativeLocator.getKey());
                Optional<WebElement> element = baseValidations.assertElementExists("part of relative locator", searchContext, locator);
                if (element.isEmpty())
                {
                    return Optional.empty();
                }
//                relativeBy = relativeElementPosition.apply(relativeBy, element.get());
                By by = locator.getLocatorType().buildBy(locator.getSearchParameters().getValue());
                relativeBy = relativeElementPosition.apply(relativeBy, by);
            }

            return Optional.of(relativeBy);
        }
        throw new IllegalArgumentException("Invalid relative locator format");
    }

    private Map<String, String> getRelativeLocators(String relativePart)
    {
        Matcher relativeMatcher = RELATIVE_LOCATORS_PATTERN.matcher(relativePart);
        Map<String, String> relativeLocators = new LinkedHashMap<>();
        while (relativeMatcher.find())
        {
            String action = relativeMatcher.group(1);
            String locator = relativeMatcher.group(2);
            relativeLocators.put(locator, action);
        }
        return relativeLocators;
    }

    private RelativeElementPosition findRelativePosition(String relativePosition)
    {
        String typeInLowerCase = relativePosition.toLowerCase();
        return Stream.of(RelativeElementPosition.values())
                .filter(t -> StringUtils.replace(t.name().toLowerCase(), "_", "")
                        .equals(typeInLowerCase))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unsupported relative element position: %s", relativePosition)));
    }

    public void setConverter(StringToLocatorConverter converter)
    {
        this.converter = converter;
    }

    public void setBaseValidations(IBaseValidations baseValidations)
    {
        this.baseValidations = baseValidations;
    }
}
