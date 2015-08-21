package com.musala.atmosphere.client.util.webview;

import com.musala.atmosphere.client.exceptions.InvalidCssQueryException;
import com.musala.atmosphere.client.uiutils.CssToXPathConverter;
import com.musala.atmosphere.commons.webelement.selection.WebElementSelectionCriterion;

/**
 * Basic utilities for conversions between different {@link WebElementSelectionCriterion criterion types} .
 * 
 * @author filareta.yordanova
 *
 */
public class WebElementSelectionCriterionConverter {
    private static final String PARTIAL_LINK_PATTERN = "//*[contains(@%s, '%s')]";

    private static final String ATTRIBUTE_PATTERN = "//*[@%s='%s']";

    private static final String TAG_PATTERN = "//%s";

    private static final String ELEMENT_AT_INDEX_PATTERN = "(%s)[%d]";

    /**
     * Converts the given {@link WebElementSelectionCriterion selection criterion} to xpath query, appending [index] to
     * the result query.
     * 
     * @param selectionCriterion
     *        - type of the selection criterion
     * @param criterionValue
     *        - value that is used for matching
     * @param index
     *        - index of the element in the list of all available elements in the DOM that can be selected by the given
     *        criterion
     * @return the converted xpath query
     * @throws InvalidCssQueryException
     *         if {@link WebElementSelectionCriterion selection criterion} is set to CSS_SELECTOR and the query is
     *         invalid
     */
    public static String convertToXpathQuery(WebElementSelectionCriterion selectionCriterion,
                                             String criterionValue,
                                             int index) throws InvalidCssQueryException {
        String xpathQuery = null;

        switch (selectionCriterion) {
            case TAG:
                xpathQuery = String.format(TAG_PATTERN, criterionValue);
                break;
            case ID:
            case NAME:
            case CLASS:
            case LINK:
                xpathQuery = String.format(ATTRIBUTE_PATTERN, selectionCriterion.getName(), criterionValue);
                break;
            case PARTIAL_LINK:
                xpathQuery = String.format(PARTIAL_LINK_PATTERN, selectionCriterion.getName(), criterionValue);
                break;
            case CSS_SELECTOR:
                xpathQuery = CssToXPathConverter.convertCssToXPath(criterionValue);
                break;
            case XPATH:
                xpathQuery = criterionValue;
                break;
            default:
                break;
        }

        return xpathQuery != null ? String.format(ELEMENT_AT_INDEX_PATTERN, xpathQuery, index) : null;
    }
}
