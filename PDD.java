package com.s15;

import com.bilibili.nativelibrary.SignedQuery;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

public class PDD extends AbstractJni {
    public static AndroidEmulator emulator;
    public static Memory memory;
    public static VM vm;
    public static Module module;
    // 1 构造方法：以后这个代码，如果破解哪个app，几乎固定，只需要改动一小点位置
    public PDD(){
        //类实例化得到对象，就会触发它的执行
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("pdd").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("apks/pdd/v6.32.0.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节
        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("apks/pdd/libpdd_secure.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator);

        // 6 dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件


    }

    //2 sign 成员方法--》也是固定步骤
    public void sign(){

        // 1 找到类
        DvmClass DeviceNative = vm.resolveClass("com/xunmeng/pinduoduo/secure/DeviceNative");
        // 2 找到类中的方法--》固定写法
        String method = "info2(Landroid/content/Context;J)Ljava/lang/String;";
        // 3 执行方法，传入参数,返回值使用DvmObject 泛指
        StringObject obj = DeviceNative.callStaticJniMethodObject(
                emulator,  // 固定设备对象
                method,    // 我们要执行的方法
                vm.resolveClass("android/content/Context").newObject(null),
                1765978948421L
        );
        // 4 打印结果
        System.out.println(obj.getValue());

    }

    // 3 运行主函数
    public static void main(String[] args) {
        PDD pdd=new PDD(); // 执行构造方法完成初始化-->初始化了设备
        pdd.sign(); //真正执行so中某个方法，得到结果
    }




    // 补环境

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if(signature.equals("com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V")){
            return;
        }

        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if(signature.equals("android/content/Context->checkSelfPermission(Ljava/lang/String;)I")){
            //返回 int类型
            // 我们可以打印出--》校验的权限名是什么
            // String str=(String) varArg.getObjectArg(0).getValue();
            // System.out.println(str);   // android.permission.READ_PHONE_STATE
            return 0;
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if(signature.equals("android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;")){
            //return vm.resolveClass("android/telephony/TelephonyManager").newObject(null);
            // 暂时补成null
            return null;
        }

        if(signature.equals("java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;")){
            return new ArrayObject(vm.resolveClass("java/lang/StackTraceElement").newObject(null));
        }
        if (signature.equals("java/lang/StackTraceElement->getClassName()Ljava/lang/String;")){
            return new StringObject(vm, "");
        }
        if (signature.equals("java/io/ByteArrayOutputStream->toByteArray()[B")) {
            ByteArrayOutputStream obj = (ByteArrayOutputStream) dvmObject.getValue();
            return new ByteArray(vm, obj.toByteArray());
        }

        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if(signature.equals("com/xunmeng/pinduoduo/secure/EU->gad()Ljava/lang/String;")){
            return new StringObject(vm,"7202111111112f2");
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if (signature.equals("android/os/Debug->isDebuggerConnected()Z")) {
            return false;
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }
    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if(signature.equals("java/lang/Throwable-><init>()V")){
            Throwable t=new Throwable(); // 可以new出来，可以传null
            return vm.resolveClass("java/lang/Throwable").newObject(t);
        }
        if (signature.equals("java/io/ByteArrayOutputStream-><init>()V")) {
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            return vm.resolveClass("java/io/ByteArrayOutputStream").newObject(obj);
        }
        if (signature.equals("java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V")) {
            try {
                OutputStream chunk = (OutputStream) varArg.getObjectArg(0).getValue();
                GZIPOutputStream obj = new GZIPOutputStream(chunk);
                return vm.resolveClass("java/util/zip/GZIPOutputStream").newObject(obj);
            } catch (Exception e) {
                System.out.println("写入错误1" + e);
            }
        }


        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if(signature.equals("java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")){
            // 1 拿到要替换的字符串
            String str=(String) dvmObject.getValue();
            // 2 取出两个参数
            String a0=(String) vaList.getObjectArg(0).getValue();
            String a1=(String) vaList.getObjectArg(1).getValue();
            // 3 我们替换
            String result=str.replaceAll(a0,a1);
            // 4 包裹返回
            return  new StringObject(vm,result);

        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if (signature.equals("java/util/zip/GZIPOutputStream->write([B)V")) {
            GZIPOutputStream obj = (GZIPOutputStream) dvmObject.getValue();
            byte[] chunk = (byte[]) varArg.getObjectArg(0).getValue();
            try {
                obj.write(chunk);
            } catch (Exception e) {
            }
            return;
        }
        if (signature.equals("java/util/zip/GZIPOutputStream->finish()V")) {
            GZIPOutputStream obj = (GZIPOutputStream) dvmObject.getValue();
            try {
                obj.finish();
            } catch (Exception e) {
            }
            return;
        }
        if (signature.equals("java/util/zip/GZIPOutputStream->close()V")) {
            GZIPOutputStream obj = (GZIPOutputStream) dvmObject.getValue();
            try {
                obj.close();
            } catch (Exception e) {
            }
            return;
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }
}
