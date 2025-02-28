package com.cyrus.example;

import com.alibaba.fastjson.util.IOUtils;
import com.cyrus.example.unidbg.UnidbgActivity;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class Demo3 {

    public static void main(String[] args) {
        Demo3 demo = new Demo3();
        demo.run();
        demo.destroy();
    }

    private void destroy() {
        IOUtils.close(emulator);
    }

    private final AndroidEmulator emulator;

    private Demo3() {
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
        // 创建一个 Dalvik 虚拟机实例
        VM vm = emulator.createDalvikVM();
        // 启用虚拟机的调试输出
        vm.setVerbose(true);

        vm.setDvmClassFactory(new ProxyClassFactory());

        // 加载共享库 libunidbg.so 到 Dalvik 虚拟机中，并设置为需要自动初始化库
        DalvikModule module = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/cyrus/libunidbg.so"), true);

        // 调用 JNI_Onload
        module.callJNI_OnLoad(emulator);

        // 注册 UnidbgActivity 类
        vm.resolveClass("com/cyrus/example/unidbg/UnidbgActivity");

        // 创建 DvmObject
        UnidbgActivity activity=new UnidbgActivity();
        DvmObject object = ProxyDvmObject.createObject(vm, activity);

        // 调用 Java 对象方法 sign 并传参 String
        DvmObject result = object.callJniMethodObject(emulator, "sign(Ljava/lang/String;)Ljava/lang/String;", "hello");

        System.out.println("sign result:" + result);
    }

}

