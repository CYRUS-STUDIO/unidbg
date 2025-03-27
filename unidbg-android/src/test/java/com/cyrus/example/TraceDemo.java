package com.cyrus.example;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TraceDemo {

    public static void main(String[] args) {
        TraceDemo demo = new TraceDemo();
        demo.run();
        demo.destroy();
    }

    private void destroy() {
        IOUtils.close(emulator);
    }

    private final AndroidEmulator emulator;

    private TraceDemo() {
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

        // 加载 so 到 Dalvik 虚拟机中，并设置为需要自动初始化库
        DalvikModule module = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/cyrus/lib_ollvm_fla_base64.so"), true);

        // 调用 JNI_Onload
        module.callJNI_OnLoad(emulator);

        // 注册 Base64Activity 类
        DvmClass dvmClass = vm.resolveClass("com/cyrus/example/base64/Base64Activity");

        // 创建 Java 对象
        DvmObject object = dvmClass.newObject(null);


        // 日志保存到文件
        PrintStream fileOut;
        try {
            // 获取当前时间，格式化为 yyyyMMdd_HHmmss
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "trace_log_" + timestamp + ".txt";

            fileOut = new PrintStream(new FileOutputStream(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.setOut(fileOut);
        System.setErr(fileOut);

        // trace 指令
        emulator.traceCode();
        // trace 内存读取
        emulator.traceRead();
        // trace 内存写入
        emulator.traceWrite();

        // 调用 jni 方法 dynamicBase64Encode 并传参 ByteArray
        DvmObject result = object.callJniMethodObject(emulator, "dynamicBase64Encode([B)Ljava/lang/String;", "lskvIIF".getBytes());

        System.out.println("result:" + result);
    }

}

