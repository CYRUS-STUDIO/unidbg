package com.cyrus.example;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.*;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import unicorn.Arm64Const;

import java.io.File;
import java.io.IOException;

public class Demo {

    public static void main(String[] args) {
        // 创建一个 64 位的 Android 模拟器实例
        AndroidEmulator emulator = AndroidEmulatorBuilder
                .for64Bit()  // 设置为 64 位模拟
                .build();    // 创建模拟器实例

        // 获取模拟器的内存实例
        Memory memory = emulator.getMemory();

        // 创建一个库解析器，并设置 Android 版本为 23（Android 6.0）
        LibraryResolver resolver = new AndroidResolver(23);

        // 将库解析器设置到模拟器的内存中，确保加载库时能够解析符号
        memory.setLibraryResolver(resolver);

        // 加载共享库 libunidbg.so 到 Dalvik 虚拟机中，并设置为需要自动初始化库
        Module module = emulator.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/cyrus/libunidbg.so"));

        // 调用 add 方法
        Number result = module.callFunction(emulator, "add", 1, 2, 3, 4, 5, 6);
        System.out.println(result.intValue());

        // 调用 string_length 方法
        result = module.callFunction(emulator, "string_length", "abc");
        System.out.println(result.intValue());

        // 使用 memory.malloc 分配 10 字节的内存并写入字符串
        MemoryBlock block = memory.malloc(10, true);
        UnidbgPointer strPtr = block.getPointer();
        strPtr.write("hello".getBytes());

        // 读取内存中的字符串
        String content = strPtr.getString(0);
        System.out.println("Read string from memory: " + content);  // 打印读取的字符串

        // 调用 string_length 并以为指针对象形式传参
        result = module.callFunction(emulator, "string_length", new PointerNumber(strPtr));
        System.out.println("Result from string_length function: " + result);  // 打印 string_length 返回的结果

        // 通过直接读取 X0 寄存器得到返回值
        Number x0 = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_X0);
        System.out.println("Value in register x0: " + x0);  // 打印寄存器 x0 的值

        // 创建一个 Dalvik 虚拟机实例
        VM vm = emulator.createDalvikVM();
        // 启用虚拟机的调试输出
        vm.setVerbose(true);

        // 调用 JNI_Onload
        vm.callJNI_OnLoad(emulator, module);

        // 调用 JNI 方法
        callJniMethod(emulator, vm);

        // 清理资源
        IOUtils.close(emulator);
    }

    public static void callJniMethod(AndroidEmulator emulator, VM vm) {
        // 注册 UnidbgActivity 类
        DvmClass unidbgActivityClass = vm.resolveClass("com/cyrus/example/unidbg/UnidbgActivity");

        // 调用 Java 类静态方法
        int result = unidbgActivityClass.callStaticJniMethodInt(emulator, "staticAdd(IIIIII)I", 1, 2, 3, 4, 5, 6);
        System.out.println("staticAdd result:" + result);

        // 创建 Java 对象
        DvmObject unidbgActivity = unidbgActivityClass.newObject(null);

        // 调用动态注册的 jni 函数 add（必须在调用 JNI_Onload 之后）
        result = unidbgActivity.callJniMethodInt(emulator, "add(IIIIII)I", 1, 2, 3, 4, 5, 6);
        System.out.println("add result:" + result);

        // 调用 Java 对象方法 stringLength 并传参 String
        result = unidbgActivity.callJniMethodInt(emulator, "stringLength(Ljava/lang/String;)I", "hello");
        System.out.println("stringLength result:" + result);

        // 调用 Java 对象方法 stringLength 并通过 StringObject 传参
        result = unidbgActivity.callJniMethodInt(emulator, "stringLength(Ljava/lang/String;)I", new StringObject(vm, "hello"));
        System.out.println("stringLength(StringObject) result:" + result);
    }

}

