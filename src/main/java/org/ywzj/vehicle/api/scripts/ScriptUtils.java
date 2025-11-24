package org.ywzj.vehicle.api.scripts;

import org.mozillaa.javascript.Context;
import org.mozillaa.javascript.Scriptable;
import org.mozillaa.javascript.ScriptableObject;

public class ScriptUtils {

    // 为了绕开混淆和安全策略限制，原版类都需要包装一层
    // 这里提供了一些全局的工具类
    public static void inject(Scriptable scope) {
        Object jsVec3Class = Context.javaToJS(MathUtil.INSTANCE, scope);
        ScriptableObject.putProperty(scope, "MathUtil", jsVec3Class);

        Object particleUtil = Context.javaToJS(ParticleUtil.INSTANCE, scope);
        ScriptableObject.putProperty(scope, "ParticleUtil", particleUtil);
    }
}
