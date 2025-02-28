package com.cyrus.example;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.Base64;

public class Demo2 extends AbstractJni {

    public static void main(String[] args) {
        Demo2 demo = new Demo2();
        demo.run();
        demo.destroy();
    }

    private void destroy() {
        IOUtils.close(emulator);
    }

    private final AndroidEmulator emulator;

    private Demo2() {
        // 创建一个 64 位的 Android 模拟器实例
        emulator = AndroidEmulatorBuilder
                .for64Bit()  // 设置为 64 位模拟
                .setProcessName("com.cyrus.example") // 进程名称
                .build();    // 创建模拟器实例

        // 获取模拟器的内存实例
        Memory memory = emulator.getMemory();

        // 创建一个库解析器，并设置 Android 版本为 23（Android 6.0）
        LibraryResolver resolver = new AndroidResolver(23);

        // 将库解析器设置到模拟器的内存中，确保加载库时能够解析符号
        memory.setLibraryResolver(resolver);

    }

    public void run() {
        // 加载共享库 libunidbg.so 到 Dalvik 虚拟机中，并设置为需要自动初始化库
        Module module = emulator.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/cyrus/libunidbg.so"));

        // 创建一个 Dalvik 虚拟机实例
        VM vm = emulator.createDalvikVM();
        // 启用虚拟机的调试输出
        vm.setVerbose(true);

        // 绑定自定义 JNI 接口
        vm.setJni(this);

        // 调用 JNI_Onload
        vm.callJNI_OnLoad(emulator, module);

        // 注册 UnidbgActivity 类
        DvmClass unidbgActivityClass = vm.resolveClass("com/cyrus/example/unidbg/UnidbgActivity");

        // 创建 Java 对象
        DvmObject unidbgActivity = unidbgActivityClass.newObject(null);

        // 调用 Java 对象方法 sign 并传参 String
        DvmObject result = unidbgActivity.callJniMethodObject(emulator, "sign(Ljava/lang/String;)Ljava/lang/String;", "hello");

        System.out.println("sign result:" + result);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        System.out.println("getStaticObjectField:" + signature);
        // 通过 signature 判断自定义静态变量访问逻辑
        if (signature.equals("com/cyrus/example/unidbg/UnidbgActivity->a:Ljava/lang/String;")) {
            return new StringObject(vm, "StaticA");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        System.out.println("getObjectField:" + signature);

        if (signature.equals("com/cyrus/example/unidbg/UnidbgActivity->b:Ljava/lang/String;")) {
            return new StringObject(vm, "NonStaticB");
        }

        return super.getObjectField(vm, dvmObject, signature);
    }

    public String base64(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        System.out.println("callObjectMethodV:" + signature);

        if (signature.equals("com/cyrus/example/unidbg/UnidbgActivity->base64(Ljava/lang/String;)Ljava/lang/String;")) {
            // 取出第一个参数
            StringObject content = vaList.getObjectArg(0);
            // 调用本地的 base64 方法
            String result = base64(content.getValue());
            // 返回加密后的字符串对象
            return new StringObject(vm, result);
        }

        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

}

