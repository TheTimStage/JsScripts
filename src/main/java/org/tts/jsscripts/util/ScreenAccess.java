package org.tts.jsscripts.util;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;

import javax.swing.text.Element;

public interface ScreenAccess {

    <T extends Element & Drawable & Selectable> T js_addDrawable(T element);

    <T extends net.minecraft.client.gui.Element & Drawable & Selectable> T js_addDrawable(T element);
}
