package com.rr.bosses.yama;

import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.KeyCode;
import net.runelite.api.VarClientStr;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.*;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Most of this code was copied from the Charge Calculator plugin.
 */
public class DuoNameAutoFillWidget
{
    private final Client client;
    private Widget duoDisplayNameWidget;

    /**
     * OSRS keycode -> AWT keycode
     */
    public static final Map<Integer, Integer> keyCodeMap = new HashMap<>();
    static {
        // send help
        keyCodeMap.put(KeyCode.KC_0, KeyEvent.VK_0);
        keyCodeMap.put(KeyCode.KC_1, KeyEvent.VK_1);
        keyCodeMap.put(KeyCode.KC_2, KeyEvent.VK_2);
        keyCodeMap.put(KeyCode.KC_3, KeyEvent.VK_3);
        keyCodeMap.put(KeyCode.KC_4, KeyEvent.VK_4);
        keyCodeMap.put(KeyCode.KC_5, KeyEvent.VK_5);
        keyCodeMap.put(KeyCode.KC_6, KeyEvent.VK_6);
        keyCodeMap.put(KeyCode.KC_7, KeyEvent.VK_7);
        keyCodeMap.put(KeyCode.KC_8, KeyEvent.VK_8);
        keyCodeMap.put(KeyCode.KC_9, KeyEvent.VK_9);
        keyCodeMap.put(KeyCode.KC_A, KeyEvent.VK_A);
        keyCodeMap.put(KeyCode.KC_ADD, KeyEvent.VK_ADD);
        keyCodeMap.put(KeyCode.KC_ALT, KeyEvent.VK_ALT);
        keyCodeMap.put(KeyCode.KC_B, KeyEvent.VK_B);
        keyCodeMap.put(KeyCode.KC_BACK_QUOTE, KeyEvent.VK_BACK_QUOTE);
        keyCodeMap.put(KeyCode.KC_BACK_SLASH, KeyEvent.VK_BACK_SLASH);
        keyCodeMap.put(KeyCode.KC_BACK_SPACE, KeyEvent.VK_BACK_SPACE);
        keyCodeMap.put(KeyCode.KC_C, KeyEvent.VK_C);
        keyCodeMap.put(KeyCode.KC_CLEAR, KeyEvent.VK_CLEAR);
        keyCodeMap.put(KeyCode.KC_CLOSE_BRACKET, KeyEvent.VK_CLOSE_BRACKET);
        keyCodeMap.put(KeyCode.KC_COMMA, KeyEvent.VK_COMMA);
        keyCodeMap.put(KeyCode.KC_CONTROL, KeyEvent.VK_CONTROL);
        keyCodeMap.put(KeyCode.KC_D, KeyEvent.VK_D);
        keyCodeMap.put(KeyCode.KC_DECIMAL, KeyEvent.VK_DECIMAL);
        keyCodeMap.put(KeyCode.KC_DELETE, KeyEvent.VK_DELETE);
        keyCodeMap.put(KeyCode.KC_DIVIDE, KeyEvent.VK_DIVIDE);
        keyCodeMap.put(KeyCode.KC_DOWN, KeyEvent.VK_DOWN);
        keyCodeMap.put(KeyCode.KC_E, KeyEvent.VK_E);
        keyCodeMap.put(KeyCode.KC_END, KeyEvent.VK_END);
        keyCodeMap.put(KeyCode.KC_ENTER, KeyEvent.VK_ENTER);
        keyCodeMap.put(KeyCode.KC_EQUALS, KeyEvent.VK_EQUALS);
        keyCodeMap.put(KeyCode.KC_ESCAPE, KeyEvent.VK_ESCAPE);
        keyCodeMap.put(KeyCode.KC_F, KeyEvent.VK_F);
        keyCodeMap.put(KeyCode.KC_F1, KeyEvent.VK_F1);
        keyCodeMap.put(KeyCode.KC_F10, KeyEvent.VK_F10);
        keyCodeMap.put(KeyCode.KC_F11, KeyEvent.VK_F11);
        keyCodeMap.put(KeyCode.KC_F12, KeyEvent.VK_F12);
        keyCodeMap.put(KeyCode.KC_F2, KeyEvent.VK_F2);
        keyCodeMap.put(KeyCode.KC_F3, KeyEvent.VK_F3);
        keyCodeMap.put(KeyCode.KC_F4, KeyEvent.VK_F4);
        keyCodeMap.put(KeyCode.KC_F5, KeyEvent.VK_F5);
        keyCodeMap.put(KeyCode.KC_F6, KeyEvent.VK_F6);
        keyCodeMap.put(KeyCode.KC_F7, KeyEvent.VK_F7);
        keyCodeMap.put(KeyCode.KC_F8, KeyEvent.VK_F8);
        keyCodeMap.put(KeyCode.KC_F9, KeyEvent.VK_F9);
        keyCodeMap.put(KeyCode.KC_G, KeyEvent.VK_G);
        keyCodeMap.put(KeyCode.KC_H, KeyEvent.VK_H);
        keyCodeMap.put(KeyCode.KC_HOME, KeyEvent.VK_HOME);
        keyCodeMap.put(KeyCode.KC_I, KeyEvent.VK_I);
        keyCodeMap.put(KeyCode.KC_INSERT, KeyEvent.VK_INSERT);
        keyCodeMap.put(KeyCode.KC_J, KeyEvent.VK_J);
        keyCodeMap.put(KeyCode.KC_K, KeyEvent.VK_K);
        keyCodeMap.put(KeyCode.KC_L, KeyEvent.VK_L);
        keyCodeMap.put(KeyCode.KC_LEFT, KeyEvent.VK_LEFT);
        keyCodeMap.put(KeyCode.KC_M, KeyEvent.VK_M);
        keyCodeMap.put(KeyCode.KC_MINUS, KeyEvent.VK_MINUS);
        keyCodeMap.put(KeyCode.KC_MULTIPLY, KeyEvent.VK_MULTIPLY);
        keyCodeMap.put(KeyCode.KC_N, KeyEvent.VK_N);
        keyCodeMap.put(KeyCode.KC_NUMBER_SIGN, KeyEvent.VK_NUMBER_SIGN);
        keyCodeMap.put(KeyCode.KC_NUMPAD0, KeyEvent.VK_NUMPAD0);
        keyCodeMap.put(KeyCode.KC_NUMPAD1, KeyEvent.VK_NUMPAD1);
        keyCodeMap.put(KeyCode.KC_NUMPAD2, KeyEvent.VK_NUMPAD2);
        keyCodeMap.put(KeyCode.KC_NUMPAD3, KeyEvent.VK_NUMPAD3);
        keyCodeMap.put(KeyCode.KC_NUMPAD4, KeyEvent.VK_NUMPAD4);
        keyCodeMap.put(KeyCode.KC_NUMPAD5, KeyEvent.VK_NUMPAD5);
        keyCodeMap.put(KeyCode.KC_NUMPAD6, KeyEvent.VK_NUMPAD6);
        keyCodeMap.put(KeyCode.KC_NUMPAD7, KeyEvent.VK_NUMPAD7);
        keyCodeMap.put(KeyCode.KC_NUMPAD8, KeyEvent.VK_NUMPAD8);
        keyCodeMap.put(KeyCode.KC_NUMPAD9, KeyEvent.VK_NUMPAD9);
        keyCodeMap.put(KeyCode.KC_O, KeyEvent.VK_O);
        keyCodeMap.put(KeyCode.KC_OPEN_BRACKET, KeyEvent.VK_OPEN_BRACKET);
        keyCodeMap.put(KeyCode.KC_P, KeyEvent.VK_P);
        keyCodeMap.put(KeyCode.KC_PAGE_DOWN, KeyEvent.VK_PAGE_DOWN);
        keyCodeMap.put(KeyCode.KC_PAGE_UP, KeyEvent.VK_PAGE_UP);
        keyCodeMap.put(KeyCode.KC_PERIOD, KeyEvent.VK_PERIOD);
        keyCodeMap.put(KeyCode.KC_Q, KeyEvent.VK_Q);
        keyCodeMap.put(KeyCode.KC_QUOTE, KeyEvent.VK_QUOTE);
        keyCodeMap.put(KeyCode.KC_R, KeyEvent.VK_R);
        keyCodeMap.put(KeyCode.KC_RIGHT, KeyEvent.VK_RIGHT);
        keyCodeMap.put(KeyCode.KC_S, KeyEvent.VK_S);
        keyCodeMap.put(KeyCode.KC_SEMICOLON, KeyEvent.VK_SEMICOLON);
        keyCodeMap.put(KeyCode.KC_SHIFT, KeyEvent.VK_SHIFT);
        keyCodeMap.put(KeyCode.KC_SLASH, KeyEvent.VK_SLASH);
        keyCodeMap.put(KeyCode.KC_SPACE, KeyEvent.VK_SPACE);
        keyCodeMap.put(KeyCode.KC_SUBTRACT, KeyEvent.VK_SUBTRACT);
        keyCodeMap.put(KeyCode.KC_T, KeyEvent.VK_T);
        keyCodeMap.put(KeyCode.KC_TAB, KeyEvent.VK_TAB);
        keyCodeMap.put(KeyCode.KC_U, KeyEvent.VK_U);
        keyCodeMap.put(KeyCode.KC_UP, KeyEvent.VK_UP);
        keyCodeMap.put(KeyCode.KC_V, KeyEvent.VK_V);
        keyCodeMap.put(KeyCode.KC_W, KeyEvent.VK_W);
        keyCodeMap.put(KeyCode.KC_X, KeyEvent.VK_X);
        keyCodeMap.put(KeyCode.KC_Y, KeyEvent.VK_Y);
        keyCodeMap.put(KeyCode.KC_Z, KeyEvent.VK_Z);
    }


    public DuoNameAutoFillWidget(Widget parent, Client client)
    {
        this.client = client;
        if (parent == null)
        {
            return;
        }

        this.duoDisplayNameWidget = parent.createChild(WidgetType.TEXT);

        if (Objects.requireNonNull(parent.getChildren()).length == 2 && !this.duoDisplayNameWidget.isHidden())
        {
            prep(this.duoDisplayNameWidget, parent.getWidth()/4*(-1));
            prep(Objects.requireNonNull(parent.getChild(0)), parent.getWidth()/4);
        }
        else
        {
            prep(this.duoDisplayNameWidget, 0);
        }

    }

    private void prep(Widget widget, int x)
    {
        widget.setTextColor(0xFF);
        widget.setFontId(FontID.PLAIN_12);

        widget.setOriginalX(x);
        widget.setOriginalY(0);
        widget.setOriginalHeight(36);
        widget.setOriginalWidth(252);

        widget.setWidthMode(WidgetSizeMode.MINUS);
        widget.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
        widget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
        widget.setXTextAlignment(WidgetTextAlignment.CENTER);
        widget.setYTextAlignment(WidgetTextAlignment.CENTER);

        widget.setHasListener(true);
        widget.setOnMouseRepeatListener((JavaScriptCallback) ev -> widget.setTextColor(0xFFFFFF));
        widget.setOnMouseLeaveListener((JavaScriptCallback) ev -> widget.setTextColor(0x800000));

        widget.revalidate();
    }

    public void showWidget(String displayName, YamaUtilitiesConfig config)
    {
        duoDisplayNameWidget.setText("<col=000000>Party Plugin:</col> " + displayName);
        duoDisplayNameWidget.setAction(0, "Join");
        duoDisplayNameWidget.setOnOpListener((JavaScriptCallback) ev -> {
            Objects.requireNonNull(client.getWidget(InterfaceID.Chatbox.MES_TEXT2)).setText(displayName + "*");
            client.setVarcStrValue(VarClientStr.INPUT_TEXT, displayName);
        });
        Objects.requireNonNull(client.getWidget(InterfaceID.Chatbox.MES_TEXT2))
                .setOnKeyListener((JavaScriptCallback) ev -> {
                    // Numpad does not seem to be supported, getTypedKeyCode returns -1 on numpad usage.
                    final int typedCode = ev.getTypedKeyCode();
                    final int typedChar = ev.getTypedKeyChar();
                    final int enterBind = config.enterKeybind().getKeyCode();
                    final int fillBind  = config.autofillKeybind().getKeyCode();

                    final int awtEv;
                    if (typedCode == -1)
                    {
                        awtEv = typedChar;
                    } else
                    {
                        awtEv = keyCodeMap.getOrDefault(typedCode, -1);
                    }
                    client.runScript(112, typedCode, typedChar, "");

                    // Numpad works using only keychars, can't really make it ignored from keybinds.
                    // This means that a VK_1 will map to VK_NUMPAD1.
                    // Solution if someone uses a numpad is to not assign numbers as keybind.
                    // The only "real" solution would be afaik to write my own parser for the chat box, but I don't want to.
                    if (awtEv == enterBind)
                    {
                        // Enter sends 84, rest unused.
                        client.runScript(112, 84, 0, "");
                    } else if (awtEv == fillBind)
                    {
                        Objects.requireNonNull(client.getWidget(InterfaceID.Chatbox.MES_TEXT2)).setText(displayName + "*");
                        client.setVarcStrValue(VarClientStr.INPUT_TEXT, displayName);
                    }
                });
    }
}