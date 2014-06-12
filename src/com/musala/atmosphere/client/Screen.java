package com.musala.atmosphere.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.musala.atmosphere.client.exceptions.UiElementFetchingException;
import com.musala.atmosphere.client.uiutils.CssAttribute;
import com.musala.atmosphere.client.uiutils.UiElementAttributeExtractor;
import com.musala.atmosphere.client.uiutils.UiElementSelectionOption;
import com.musala.atmosphere.client.uiutils.UiElementSelector;
import com.musala.atmosphere.client.uiutils.UiXmlParser;
import com.musala.atmosphere.commons.RoutingAction;
import com.musala.atmosphere.commons.ui.UiElementDescriptor;

/**
 * Class that holds a device screen information.
 * 
 * @author georgi.gaydarov
 * 
 */

public class Screen {
    private static final Logger LOGGER = Logger.getLogger(Screen.class.getCanonicalName());

    private String screenXml;

    private Device onDevice;

    private Document xPathDomDocument;

    private org.jsoup.nodes.Document jSoupDocument;

    private DeviceCommunicator communicator;

    /**
     * 
     * @param onDevice
     * @param uiHierarchyXml
     */
    Screen(Device onDevice, String uiHierarchyXml) {
        this.onDevice = onDevice;
        communicator = onDevice.getCommunicator();
        screenXml = uiHierarchyXml;

        // XPath DOM Document building
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            xPathDomDocument = documentBuilder.parse(new InputSource(new StringReader(screenXml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.warn("XPath XML to DOM Document parsing failed.", e);
        }

        // JSoup Document building
        jSoupDocument = Jsoup.parse(screenXml);

        onDevice.getUiValidator().setActiveScreen(this);
    }

    /**
     * Updates the current {@link Screen} instance to contain the newest possible device screen information. Equivalent
     * to reinvoking the {@link Device#getActiveScreen()} method.
     */
    public void updateScreen() {
        Screen newScreen = onDevice.getActiveScreen();
        screenXml = newScreen.screenXml;
        xPathDomDocument = newScreen.xPathDomDocument;
        jSoupDocument = newScreen.jSoupDocument;
    }

    /**
     * Saves the underlying device UI XML into a file.
     * 
     * @param path
     *        - file to which the UI XML should be saved.
     * @throws FileNotFoundException
     */
    public void exportToXml(String path) throws FileNotFoundException {
        PrintStream export = new PrintStream(path);
        export.print(screenXml);
        export.close();
    }

    /**
     * Searches for given UI element in the current screen XML structure using CSS.
     * 
     * @param query
     *        - CSS selector query.
     * @return the requested {@link UiElement UiElement}.
     * @throws UiElementFetchingException
     */
    public UiElement getElementByCSS(String query) throws UiElementFetchingException {
        org.jsoup.nodes.Node node = UiXmlParser.getJSoupNode(jSoupDocument, query);
        UiElement returnElement = new UiElement(node, onDevice);
        return returnElement;
    }

    /**
     * Searches for given ScrollableView in the current screen XML structure using CSS
     * 
     * @param query
     *        CSS selector query
     * @return the requested ScrollableView
     * @throws UiElementFetchingException
     */
    public ScrollableView getScrollableViewtByCSS(String query) throws UiElementFetchingException {
        org.jsoup.nodes.Node node = UiXmlParser.getJSoupNode(jSoupDocument, query);
        ScrollableView returnElement = new ScrollableView(node, onDevice);
        return returnElement;
    }

    /**
     * Searches for given UI element in the current screen XML structure using XPath.
     * 
     * @param query
     *        XPath query.
     * @return the requested {@link UiElement UiElement}.
     * @throws XPathExpressionException
     * @throws UiElementFetchingException
     */
    public UiElement getElementByXPath(String query) throws XPathExpressionException, UiElementFetchingException {
        Node node = UiXmlParser.getXPathNode(xPathDomDocument, query);
        UiElement returnElement = new UiElement(node, onDevice);
        return returnElement;
    }

    /**
     * Searches for given UI element in the current screen XML structure using a {@link UiElementSelector
     * UiElementSelector} instance.
     * 
     * @param selector
     *        - object of type {@link UiElementSelector}.
     * @return the requested {@link UiElement UiElement}.
     * @throws UiElementFetchingException
     */
    public UiElement getElement(UiElementSelector selector) throws UiElementFetchingException {
        String cssQuery = selector.buildCssQuery();
        UiElement result = getElementByCSS(cssQuery);
        return result;
    }

    /**
     * Searches for given ScrollableView in the current screen XML structure using a {@link UiElementSelector
     * UiElementSelector} instance.
     * 
     * @param selector
     *        - object of type {@link UiElementSelector}
     * @return the requested {@link ScrollableView ScrollableView}
     * @throws UiElementFetchingException
     */
    public ScrollableView getScrollableView(UiElementSelector selector) throws UiElementFetchingException {
        String cssQuery = selector.buildCssQuery();
        ScrollableView result = getScrollableViewtByCSS(cssQuery);
        return result;
    }

    /**
     * Searches for UI elements in the current screen XML structure using given CSS. Returns a list of all found
     * elements having the used CSS.
     * 
     * @param query
     *        - CSS selector query.
     * @return List containing all found elements of type {@link UiElement UiElement}.
     * @throws UiElementFetchingException
     */
    public List<UiElement> getAllElementsByCSS(String query) throws UiElementFetchingException {
        List<UiElement> uiElementList = new ArrayList<UiElement>();

        Elements elements = UiXmlParser.getJSoupElements(jSoupDocument, query);
        for (org.jsoup.nodes.Node node : elements) {
            UiElement returnElement = new UiElement(node, onDevice);
            uiElementList.add(returnElement);
        }
        return uiElementList;
    }

    /**
     * Searches for UI elements in the current screen XML structure using a given {@link UiElementSelector
     * UiElementSelector}. Returns a list of all found elements having the used selector.
     * 
     * @param selector
     *        - object of type {@link UiElementSelector}.
     * @return List containing all found elements of type {@link UiElement UiElement}.
     * @throws UiElementFetchingException
     */
    public List<UiElement> getElements(UiElementSelector selector) throws UiElementFetchingException {
        String cssQuery = selector.buildCssQuery();
        List<UiElement> result = getAllElementsByCSS(cssQuery);
        return result;
    }

    /**
     * Tap on first found {@link UiElement UiElement}, displaying exactly the supplied search text.
     * 
     * @param text
     *        - search text.
     * @return <code>true</code> if the tapping of element is successful, <code>false</code> if it fails.
     * @throws UiElementFetchingException
     */
    public boolean tapElementWithText(String text) throws UiElementFetchingException {
        return tapElementWithText(text, 0);
    }

    /**
     * Tap on {@link UiElement}, displaying exactly the supplied search text.
     * 
     * @param text
     *        - search text.
     * @param match
     *        - determines which element to tap if multiple matches exist; zero based index.
     * @return <code>true</code> if the tapping of element is successful, <code>false</code> if it fails.
     * @throws UiElementFetchingException
     */
    public boolean tapElementWithText(String text, int match) throws UiElementFetchingException {
        UiElementSelector selector = new UiElementSelector();
        selector.addSelectionAttribute(CssAttribute.TEXT, UiElementSelectionOption.EQUALS, text);
        List<UiElement> elementList = getElements(selector);
        int listSize = elementList.size();
        if (listSize <= match) {
            throw new UiElementFetchingException("Tapping match with index " + match + " requested, but only "
                    + listSize + "elements matching the criteria found.");
        }
        UiElement element = elementList.get(match);
        return element.tap();
    }

    /**
     * Check existence of element, displaying exactly the supplied search text.
     * 
     * @param text
     *        - search text.
     * @return - true if element with supplied search text exists on screen.
     */
    public boolean hasElementWithText(String text) {
        try {
            UiElementSelector selector = new UiElementSelector();
            selector.addSelectionAttribute(CssAttribute.TEXT, UiElementSelectionOption.EQUALS, text);
            List<UiElement> elementList = getElements(selector);
            return elementList.size() > 0;
        } catch (UiElementFetchingException e) {
            // no elements were found by the supplied text
            return false;
        }
    }

    /**
     * Checks if an element is present in the current {@link Screen} instance.
     * 
     * @param query
     *        - the CSS element query to be checked for matches.
     * @return <b><true/b> if the element is present, <b>false</b> otherwise.
     */
    public boolean containsElementByCSS(String query) {
        Elements elements = null;
        try {
            elements = UiXmlParser.getJSoupElements(jSoupDocument, query);
        } catch (UiElementFetchingException e) {
            return false;
        }

        return elements.size() > 0;
    }

    /**
     * Waits for the existence of a given UiElement with a given timeout.
     * 
     * @param selector
     *        - the selector of the given UI element.
     * 
     * @param timeout
     *        - the given timeout.
     * 
     * @return boolean indicating if this action was successful.
     */
    public boolean waitForElementExists(UiElementSelector selector, Integer timeout) {
        UiElementDescriptor descriptor = UiElementAttributeExtractor.extract(selector);
        boolean response = (boolean) communicator.sendAction(RoutingAction.WAIT_FOR_EXISTS, descriptor, timeout);
        return response;
    }

    /**
     * Waits until a given UiElement disappears with a given timeout.
     * 
     * @param selector
     *        - the selector of the given UI element.
     * 
     * @param timeout
     *        - the given timeout.
     * 
     * @return boolean indicating if this action was successful.
     */
    public boolean waitUntilElementGone(UiElementSelector selector, Integer timeout) {
        UiElementDescriptor descriptor = UiElementAttributeExtractor.extract(selector);
        boolean response = (boolean) communicator.sendAction(RoutingAction.WAIT_UNTIL_GONE, descriptor, timeout);
        return response;
    }

    /**
     * Waits for a window content update event to occur. If a package name for the window is specified, but the current
     * window does not have the same package name, the function returns immediately.
     * 
     * @param packageName
     *        - the specified window package name (can be null). If null, a window update from any front-end window will
     *        end the wait
     * @param timeout
     *        - the timeout of the operation
     * @return <code>true</code> if a window update occurred, <code>false</code> if timeout has elapsed or if the
     *         current window does not have the specified package name
     */
    public boolean waitForWindowUpdate(String packageName, int timeout) {
        boolean response = (boolean) communicator.sendAction(RoutingAction.WAIT_FOR_WINDOW_UPDATE, packageName, timeout);
        return response;
    }

    /**
     * Searches for given UI Collection in the current screen XML structure using a {@link UiElementSelector
     * UiElementSelector} instance.
     * 
     * @param selector
     *        - object of type {@link UiElementSelector}.
     * @return the requested {@link UiCollection UiCollection}.
     * @throws UiElementFetchingException
     */
    public UiCollection getCollectionBySelector(UiElementSelector selector) throws UiElementFetchingException {
        String cssQuery = selector.buildCssQuery();
        UiCollection result = getCollectionByCSS(cssQuery);
        return result;
    }

    /**
     * Searches for given UI Collection in the current screen XML structure using CSS.
     * 
     * @param query
     *        - CSS selector query.
     * @return the requested {@link UiCollection UiCollection}.
     * @throws UiElementFetchingException
     */
    public UiCollection getCollectionByCSS(String cssQuery) throws UiElementFetchingException {
        org.jsoup.nodes.Node node = UiXmlParser.getJSoupNode(jSoupDocument, cssQuery);
        UiCollection returnElement = new UiCollection(node, onDevice);
        return returnElement;
    }

    /**
     * Searches for given UI Collection in the current screen XML structure using XPath.
     * 
     * @param query
     *        XPath query.
     * @return the requested {@link UiCollection UiCollection}.
     * @throws XPathExpressionException
     * @throws UiElementFetchingException
     */
    public UiCollection getCollectionByXPath(String xPathQuery)
        throws UiElementFetchingException,
            XPathExpressionException {
        Node node = UiXmlParser.getXPathNode(xPathDomDocument, xPathQuery);
        UiCollection returnElement = new UiCollection(node, onDevice);
        return returnElement;
    }
}
