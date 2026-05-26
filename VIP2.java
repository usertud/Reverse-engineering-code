package com.s15;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class VIP2 extends AbstractJni {
    public static AndroidEmulator emulator;
    public static Memory memory;
    public static VM vm;
    public static Module module;
    public VIP2() {
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
        // 0 定义map
        Map<String,String> map=new TreeMap<>(); // 有序
        /*
        app_name=achievo_ad,
        app_version=7.83.3,
        channel=oziq7dxw:::,
        device=Pixel 2 XL,
        device_token=ce6f1b65-2456-3f07-b539-1406906ae6e2,
        manufacturer=Google,
        os_version=30,
         regPlat=0,
         regid=null,
         rom=Dalvik/2.1.0 (Linux; U; Android 11; Pixel 2 XL Build/RP1A.201005.004.A1),
         skey=6692c461c3810ab150c9a980d0c275ec,
         status=1,
         vipruid=,
         warehouse=VIP_SH
         */
        map.put("app_name","achievo_ad");
        map.put("app_version","7.83.3");
        map.put("channel","oziq7dxw:::");
        map.put("device","Pixel 2 XL");
        map.put("device_token","ce6f1b65-2456-3f07-b539-1406906ae6e2");
        map.put("manufacturer","Google");
        map.put("os_version","30");
        map.put("regPlat","0");
        map.put("regid","null");
        map.put("rom","Dalvik/2.1.0 (Linux; U; Android 11; Pixel 2 XL Build/RP1A.201005.004.A1)");
        map.put("skey","6692c461c3810ab150c9a980d0c275ec");
        map.put("status","1");
        map.put("vipruid","");
        map.put("warehouse","VIP_SH");

        //1 找到java的类，jni的类，native类，固定写法
        DvmClass KeyInfo = vm.resolveClass("com/vip/vcsp/KeyInfo");
        //2 找到方法--》固定写法
        String method = "gsNav(Landroid/content/Context;Ljava/util/Map;Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/String;";
        // 3 执行方法，传入参数
        StringObject obj = KeyInfo.callStaticJniMethodObject(
                emulator,
                method, // 要包裹
                vm.resolveClass("android/content/Context").newObject(null), // 必须这样写
                ProxyDvmObject.createObject(vm,map), // 先放在这里
                //new StringObject(vm,"")
                ProxyDvmObject.createObject(vm,""), // 上面和这种写法都可以
                false
        );
        // 4 得到结果打印出来
        System.out.println(obj.getValue());


    }

    public static void main(String[] args) {
        VIP2 vip2=new VIP2();
        vip2.sign();

 }



 // 补环境

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if (signature.equals("java/util/TreeMap->entrySet()Ljava/util/Set;")){
            // 1 拿到对象,以后都是这么写
            TreeMap map=(TreeMap)dvmObject.getValue();
            // 2 拿到执行的结果
            Set set=map.entrySet();
            // 3 包裹返回
            return ProxyDvmObject.createObject(vm,set);

        }
        if(signature.equals("java/util/Set->iterator()Ljava/util/Iterator;")){
            Set set=(Set)dvmObject.getValue();
            //Iterator it=set.iterator();
            //return ProxyDvmObject.createObject(vm,it);
            return ProxyDvmObject.createObject(vm,set.iterator());
        }
        if(signature.equals("java/util/Iterator->next()Ljava/lang/Object;")){
            Iterator it =(Iterator)dvmObject.getValue();
            Object o=it.next();
            return ProxyDvmObject.createObject(vm,o);
        }
        if(signature.equals("java/util/Map$Entry->getKey()Ljava/lang/Object;")) {
            Map.Entry e = (Map.Entry) dvmObject.getValue();
            //Object o=e.getKey();
            //return ProxyDvmObject.createObject(vm, o);
            String key=(String)e.getKey();
            return new StringObject(vm,key);

        }
        if(signature.equals("java/util/Map$Entry->getValue()Ljava/lang/Object;")) {
            Map.Entry e = (Map.Entry) dvmObject.getValue();
            return ProxyDvmObject.createObject(vm, e.getValue());
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if(signature.equals("java/util/Iterator->hasNext()Z")){
            Iterator it=(Iterator)dvmObject.getValue();
            // boolean b=it.hasNext();
            // return b;
            return it.hasNext();
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }
}
