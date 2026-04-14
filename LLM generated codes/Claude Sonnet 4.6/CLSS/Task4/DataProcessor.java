package com.example.app;

/**
 * Java data-processing component triggered by native C++ code.
 */
public class DataProcessor implements NativeControllable {

    private static final String COMPONENT_ID = "DataProcessor";

    @Override
    public String getComponentId() {
        return COMPONENT_ID;
    }

    /**
     * Triggered by native code to run a data processing operation.
     *
     * @param params  Processing descriptor (e.g. "TRANSFORM:format=JSON").
     * @param flags   Mode flags (e.g. 0x01 = async, 0x04 = validate).
     * @return        Serialised result string returned to native.
     */
    @Override
    public String trigger(String params, int flags) {
        System.out.printf("[DataProcessor] Triggered — params='%s', flags=0x%X%n",
                params, flags);

        if (params.startsWith("TRANSFORM")) {
            return transform(params, flags);
        } else if (params.startsWith("VALIDATE")) {
            return validate(params, flags);
        }

        return "DataProcessor:UNKNOWN_OPERATION";
    }

    private String transform(String params, int flags) {
        boolean validate = (flags & 0x04) != 0;
        System.out.printf("  → Transforming data [validate=%b]: %s%n",
                validate, params);
        return "DataProcessor:TRANSFORM_COMPLETE:result={}";
    }

    private String validate(String params, int flags) {
        System.out.println("  → Validating data: " + params);
        return "DataProcessor:VALIDATION_OK";
    }
}