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

package org.vividus.excel.transformer;

import static org.apache.commons.lang3.Validate.notBlank;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Sheet;
import org.jbehave.core.model.ExamplesTable.TableProperties;
import org.jbehave.core.model.TableParsers;
import org.vividus.excel.ExcelSheetParser;
import org.vividus.excel.ExcelSheetsExtractor;
import org.vividus.excel.IExcelSheetParser;
import org.vividus.excel.IExcelSheetsExtractor;
import org.vividus.excel.WorkbookParsingException;
import org.vividus.model.CellValue;
import org.vividus.transformer.ExtendedTableTransformer;
import org.vividus.util.ExamplesTableProcessor;
import org.vividus.util.ResourceUtils;

public class ExcelTableTransformer implements ExtendedTableTransformer
{
    private static final String RANGE = "range";

    private final boolean preserveCellFormatting;

    public ExcelTableTransformer(boolean preserveCellFormatting)
    {
        this.preserveCellFormatting = preserveCellFormatting;
    }

    @Override
    public String transform(String tableAsString, TableParsers tableParsers, TableProperties properties)
    {
        checkTableEmptiness(tableAsString);
        String path = properties.getMandatoryNonBlankProperty("path", String.class);
        String sheetName = properties.getMandatoryNonBlankProperty("sheet", String.class);
        try
        {
            byte[] excelDocumentAsBytes = ResourceUtils.loadResourceOrFileAsByteArray(path);
            IExcelSheetsExtractor excelSheetsExtractor = new ExcelSheetsExtractor(excelDocumentAsBytes);
            Optional<Sheet> sheet = excelSheetsExtractor.getSheet(sheetName);
            if (sheet.isEmpty())
            {
                throw new IllegalArgumentException("Sheet with name '" + sheetName + "' does not exist");
            }
            IExcelSheetParser excelSheetParser = new ExcelSheetParser(sheet.get(), preserveCellFormatting);
            String column = properties.getProperties().getProperty("column");
            if (column != null)
            {
                notBlank(column, "Table property 'column' is blank");
                String joinValues = properties.getProperties().getProperty("joinValues");
                List<String> result = extractData(excelSheetParser, properties);
                List<String> data = Boolean.parseBoolean(joinValues) ? List.of(String.join(" ", result)) : result;
                return build(List.of(column), List.of(data), properties);
            }
            String range = properties.getMandatoryNonBlankProperty(RANGE, String.class);
            Map<String, List<String>> exactDataTable = excelSheetParser.getDataAsTable(range);
            return build(exactDataTable.keySet(), exactDataTable.values(), properties);
        }
        catch (WorkbookParsingException | IOException e)
        {
            throw new IllegalStateException("Error during parsing excel workbook", e);
        }
    }

    private List<String> extractData(IExcelSheetParser sheetParser, TableProperties properties)
    {
        Map.Entry<String, String> excelSource = processCompetingMandatoryProperties(properties.getProperties(),
                RANGE, "addresses");
        String excelSourceValue = excelSource.getValue();

        return RANGE.equals(excelSource.getKey()) ? extractDataFromRange(sheetParser, properties, excelSourceValue)
                : extractDataFromAddresses(sheetParser, excelSourceValue);
    }

    private List<String> extractDataFromRange(IExcelSheetParser sheetParser, TableProperties properties, String range)
    {
        List<String> data = extractValues(sheetParser, range);
        String incrementAsString = properties.getProperties().getProperty("increment");
        if (incrementAsString != null)
        {
            return IntStream.range(0, data.size())
                    .filter(n -> n % Integer.parseInt(incrementAsString) == 0)
                    .mapToObj(data::get)
                    .toList();
        }
        return data;
    }

    private List<String> extractValues(IExcelSheetParser sheetParser, String range)
    {
        return sheetParser.getDataFromRange(range).stream().map(CellValue::value).toList();
    }

    private List<String> extractDataFromAddresses(IExcelSheetParser sheetParser, String addresses)
    {
        return Stream.of(addresses.split(";")).map(sheetParser::getDataFromCell).toList();
    }

    private String build(Collection<String> headers, Collection<List<String>> data, TableProperties properties)
    {
        String lineBreakReplacementPropertyValue = properties.getProperties().getProperty("lineBreakReplacement");
        String lineBreakReplacement = lineBreakReplacementPropertyValue == null ? ""
            : lineBreakReplacementPropertyValue;
        List<List<String>> result = data.stream()
                                          .map(element -> replaceLineBreaks(element, lineBreakReplacement))
                                          .toList();
        return ExamplesTableProcessor.buildExamplesTableFromColumns(headers, result, properties);
    }

    private List<String> replaceLineBreaks(List<String> list, String lineBreakReplacement)
    {
        return list.stream()
                     .map(e -> e == null ? e : e.replace("\n", lineBreakReplacement))
                     .toList();
    }
}
