package com.musala.atmosphere.client.entity;

import com.musala.atmosphere.client.DeviceCommunicator;
import com.musala.atmosphere.client.Screen;
import com.musala.atmosphere.client.UiElement;
import com.musala.atmosphere.client.entity.annotations.Restriction;
import com.musala.atmosphere.client.exceptions.MultipleElementsFoundException;
import com.musala.atmosphere.commons.exceptions.UiElementFetchingException;
import com.musala.atmosphere.commons.ui.selector.CssAttribute;
import com.musala.atmosphere.commons.ui.selector.UiElementSelector;

/**
 * {@link GpsLocationEntity} responsible for setting the GPS location state on all Samsung devices.
 * 
 * @author yavor.stankov
 *
 */
@Restriction(manufacturer = "Samsung")
public class GpsLocationCheckBoxEntity extends GpsLocationEntity {
    private static final String ANDROID_WIDGET_CHECK_BOX_CLASS_NAME = "android.widget.CheckBox";

    GpsLocationCheckBoxEntity(Screen screen, DeviceCommunicator communicator) {
        super(screen, communicator);
    }

    @Override
    protected UiElement getChangeStateWidget() throws MultipleElementsFoundException, UiElementFetchingException {
        UiElementSelector checkBoxWidgetSelector = new UiElementSelector();
        checkBoxWidgetSelector.addSelectionAttribute(CssAttribute.CLASS_NAME, ANDROID_WIDGET_CHECK_BOX_CLASS_NAME);

        // There are more than one check box on the screen, but only the first one is for setting the GPS location
        // state.
        UiElement checkBox = screen.getElements(checkBoxWidgetSelector).get(0);

        return checkBox;
    }
}
