package org.ywzj.vehicle.util;

import com.sighs.apricityui.init.Element;

public class AuiTextHelper {

    public static void setDescription(Element element, String text) {
        if (element == null) {
            return;
        }
        element.setInnerHTML(escapeHtml(text).replace("-", "&#x2011;"));
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
