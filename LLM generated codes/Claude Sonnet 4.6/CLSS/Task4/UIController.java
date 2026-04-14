package com.example.app;

/**
 * Java UI component whose actions can be triggered by native C++ code.
 */
public class UIController implements NativeControllable {

    private static final String COMPONENT_ID = "UIController";

    @Override
    public String getComponentId() {
        return COMPONENT_ID;
    }

    /**
     * Triggered by native code to perform a UI action.
     *
     * @param params  Action descriptor (e.g. "SHOW_DIALOG:title=Alert").
     * @param flags   Control flags (e.g. 0x01 = animate, 0x02 = modal).
     * @return        Outcome string returned to the native caller.
     */
    @Override
    public String trigger(String params, int flags) {
        System.out.printf("[UIController] Triggered — params='%s', flags=0x%X%n",
                params, flags);

        if (params.startsWith("SHOW_DIALOG")) {
            return showDialog(params, flags);
        } else if (params.startsWith("UPDATE_VIEW")) {
            return updateView(params, flags);
        }

        return "UIController:UNKNOWN_ACTION";
    }

    private String showDialog(String params, int flags) {
        boolean isModal = (flags & 0x02) != 0;
        System.out.printf("  → Showing dialog [modal=%b] with params: %s%n",
                isModal, params);
        return "UIController:DIALOG_SHOWN";
    }

    private String updateView(String params, int flags) {
        boolean animate = (flags & 0x01) != 0;
        System.out.printf("  → Updating view [animate=%b] with params: %s%n",
                animate, params);
        return "UIController:VIEW_UPDATED";
    }
}