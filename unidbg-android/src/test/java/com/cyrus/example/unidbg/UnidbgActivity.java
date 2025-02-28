package com.cyrus.example.unidbg;


import org.apache.commons.io.Charsets;

public final class UnidbgActivity {

    private static String a;
    private String b;

    public UnidbgActivity() {
        UnidbgActivity.a = "StaticA";
        this.b = "NonStaticB";
    }

    public final String base64(String content) {
        byte[] arr_b = content.getBytes(Charsets.UTF_8);
        String s1 = Base64.encodeToString(arr_b, 2);
        return s1;
    }

}

