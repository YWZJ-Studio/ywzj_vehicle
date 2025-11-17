package org.ywzj.vehicle.api.scripts;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class ScriptUtils {

    // 如果一些原版的类需要被操作，为了避免混淆产生的问题，建议都包装一层再注入脚本环境
    public static void inject(Scriptable scope) {
        Object jsVec3Class = Context.javaToJS(MathUtil.INSTANCE, scope);
        ScriptableObject.putProperty(scope, "MathUtil", jsVec3Class);

        Object particleUtil = Context.javaToJS(ParticleUtil.INSTANCE, scope);
        ScriptableObject.putProperty(scope, "ParticleUtil", particleUtil);
    }
}
