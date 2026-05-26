package com.s15;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class VIP1 extends AbstractJni {
    public static AndroidEmulator emulator;
    public static Memory memory;
    public static VM vm;
    public static Module module;
    public VIP1() {
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("vip").build();
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("apks/vip/vip-9.42.8.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        DalvikModule dm = vm.loadLibrary(new File("apks/vip/libkeyinfo.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();

    }
    public void sign(){
        //1 找到java的类，jni的类，native类，固定写法
        DvmClass KeyInfo = vm.resolveClass("com/vip/vcsp/KeyInfo");
        //2 找到方法--》固定写法
        String method = "getNavInfo(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;";
        // 3 执行方法，传入参数
        StringObject obj = KeyInfo.callStaticJniMethodObject(
                emulator,
                method, // 要包裹
                vm.resolveClass("android/content/Context").newObject(null),
                new StringObject(vm,"skey")
        );
        // 4 得到结果打印出来
        System.out.println(obj.getValue());


    }

    public static void main(String[] args) {
        VIP1 vip1=new VIP1();
        vip1.sign();

 }
}
