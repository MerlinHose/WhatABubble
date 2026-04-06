package de.mmerlin.mmxp.whatabubble.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    /** Category shown in Options → Controls. */
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("whatabubble", "whatabubble"));

    public static KeyBinding OPEN_SETTINGS;

    public static void register() {
        OPEN_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.whatabubble.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                CATEGORY
        ));
    }
}



