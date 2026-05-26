package com.s15;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSink;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


public class XHS extends AbstractJni {

    //1 定义一个request变量--->okhttp3的request对象--》使用maven--》引入okhttp3--》python中要创建一个requests模块的request对象，需要下载这个模块
    private static Request request;

    public static AndroidEmulator emulator;
    public static Memory memory;
    public static VM vm;
    public static Module module;
    // 1 构造方法：以后这个代码，如果破解哪个app，几乎固定，只需要改动一小点位置
    public XHS(){
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("xhsud").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("apks/xhs/v6.73.0.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节
        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("apks/xhs/libshield.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();


    }

    public void initializeNative(){
        // 1 找到类
        DvmClass cls = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        // 2 找到方法
        String method = "initializeNative()V";
        // 3 执行方法
        cls.callStaticJniMethodObject(emulator,method);

    }
    public long initialize(String str){
        // 1 找到类：
        DvmClass cls = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        //2 找到方法：
        String method = "initialize(Ljava/lang/String;)J";
        //3 执行
        long cPtr=cls.callStaticJniMethodLong(emulator,method,new StringObject(vm,str));
        return cPtr;
    }
    public  void intercept(long cPtr){
        // 1 找到类
        DvmClass cls = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        // 2 找到方法
        String method = "intercept(Lokhttp3/Interceptor$Chain;J)Lokhttp3/Response;";
        // 3 执行
        cls.callStaticJniMethodObject(
                emulator,
                method,
                vm.resolveClass("okhttp3/Interceptor$Chain").newObject(null),// chain没有，传入null
                cPtr
        );
    }





    // 3 运行主函数
    public static void main(String[] args) {
        // 1 根据python调用我们，传入的值，进行构造request对象
        String method = args[0];  // get  post
        String url = args[1];
        String commonParams = args[2];
        String content = args[3]; // 请求体


//        // 初始化request
//        request = new Request.Builder()
//                .url("https://www.xiaohongshu.com/api/sns/v1/system_service/check_code?zone=86&phone=18630099999&code=112233")
//                .addHeader("x-b3-traceid","5353e04e101a090f")
//                .addHeader("xy-common-params", "fid=166893364910401a044595fd44d95587c504e09275d9&device_fingerprint=2022112007045266bb5eba5505d24d7b1d35dec1975913018dc5b30ffa5ab5&device_fingerprint1=2022112007045266bb5eba5505d24d7b1d35dec1975913018dc5b30ffa5ab5&launch_id=1673279778&tz=Asia%2FHong_Kong&channel=YingYongBao&versionName=6.73.0&deviceId=d7a8fa4f-98a2-398a-a7a5-417bf5e0b971&platform=android&sid=session.1668933583120053844884&identifier_flag=0&t=1673279684&project_id=ECFAAF&build=6730157&x_trace_page_current=login_full_screen_sms_page&lang=zh-Hans&app_id=ECFAAF01&uis=light")
//                .build();


        // 2 实例化得到小红书对象
        XHS xhs=new XHS(); // 执行构造方法完成初始化-->初始化了设备
        // 3 执行initializeNative--》在static中，最先执行
        xhs.initializeNative();
        // 4 构造方法
        long l=xhs.initialize("main"); //参数要hook到，返回值给了 intercept
        // 5 创建出request对象-->new出Request对象：如果是get请求，构造get的request对象，如果是post，构造post的reqeust的对象
        if (method.equalsIgnoreCase("post")) {
            MediaType TEXT = MediaType.parse("text/plan;charset=utf-8");
            RequestBody body = RequestBody.create(TEXT, content);
            request = new Request.Builder()
                    .url(url)
                    .addHeader("xy-common-params", commonParams)
                    .addHeader("X-B3-TraceId","ffffa1ffaf1f0b0f")
                    .addHeader("User-Agent","Dalvik/2.1.0 (Linux; U; Android 11; Pixel 2 XL Build/RP1A.201005.004.A1) Resolution/1440*2880 Version/6.73.0 Build/6730157 Device/(Google;Pixel 2 XL) discover/6.73.0 NetType/Unknown")
                    .addHeader("content-type", "application/x-www-form-urlencoded")
                    .method("post", body)
                    .build();
        }
        else {
            request = new Request.Builder()
                    .url(url)
                    .addHeader("xy-common-params", commonParams)
                    .addHeader("X-B3-TraceId","ffffa1ffaf1f0b0f")
                    .addHeader("User-Agent","Dalvik/2.1.0 (Linux; U; Android 11; Pixel 2 XL Build/RP1A.201005.004.A1) Resolution/1440*2880 Version/6.73.0 Build/6730157 Device/(Google;Pixel 2 XL) discover/6.73.0 NetType/Unknown")
                    .build();
        }
        // 6 执行intercept
        xhs.intercept(l); // 如参是 initialize 的返回结果
    }

    // 补环境
    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if(signature.equals("java/nio/charset/Charset->defaultCharset()Ljava/nio/charset/Charset;")){
            // 获取编码方式
            Charset charset=Charset.defaultCharset();
            return ProxyDvmObject.createObject(vm,charset);
        }

        if(signature.equals("com/xingin/shield/http/Base64Helper->decode(Ljava/lang/String;)[B")){
            // 1  com/xingin/shield/http/Base64Helper 类是 小红书自己包下--》Base64Helper类的decode是原生base64的方法吗？
            // 2 com/xingin/shield/http/Base64Helper 有没有被小红书魔改--》如果魔改了--》我们需要补：Base64Helper；如果没魔改，就是原生，我们直接使用base64即可
            // 直接使用标准base64
            String input=(String)vaList.getObjectArg(0).getValue();
            byte[] result = Base64.decodeBase64(input);
            return new ByteArray(vm,result);
     }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        // ContextHolder中有个属性：sDeviceId--》字符串类型
        if(signature.equals("com/xingin/shield/http/ContextHolder->sDeviceId:Ljava/lang/String;")){
            // 1 反编译找到他--》看它如何生成的--》uuid
            // 2 通过hook得到
            // 3 直接返回
            return ProxyDvmObject.createObject(vm,"c34b33ff-fd85-319c-9693-2e5523e33f34");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        if(signature.equals("com/xingin/shield/http/ContextHolder->sAppId:I")){
            return -319115519; // 不变的
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public boolean getStaticBooleanField(BaseVM vm, DvmClass dvmClass, String signature) {
        if(signature.equals("com/xingin/shield/http/ContextHolder->sExperiment:Z")){
            return true;
        }
        return super.getStaticBooleanField(vm, dvmClass, signature);
    }


    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if(signature.equals("android/content/Context->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;")){
            //1 之前学安卓开发学的
            /*
            1.1 调用getSharedPreferences 第一个参数是 哪个 xml文件--->先打印出看看是要拿那个xml文件
            SharedPreferences sp = getSharedPreferences("sp_token.xml", MODE_PRIVATE);
            String token = sp.getString("token","");
             */
            // 2 先打印出拿哪个xml---》sp对象代指：s.xml 这个xml文件
            //System.out.println(vaList.getObjectArg(0).getValue());   // s.xml 这个xml文件
            // 3 我们无法返回sp对象--》没有这个类--》返回空---》里面再用对象执行的时候，又会报错，又需要补环境
            // 3.1 返回了 空的 sp对象--》后续肯定还需要补环境  sp.getString("token",""); 又报错
            return vm.resolveClass("android/content/SharedPreferences").newObject(null);
        }

        if(signature.equals("android/content/SharedPreferences->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")){
            // 1 要去 s.xml中取那个key值？第一个参数是去xml中拿那个值，如果拿到到，返回默认值是第二个参数
            //String token = sp.getString("main","");
            // 2 打印出   去xml中拿那个值:main，如果拿不到默认值是多少: defaultVale=""
            String key=(String)vaList.getObjectArg(0).getValue();
            String defaultVale=(String) vaList.getObjectArg(1).getValue();
            //System.out.println(key);
            //System.out.println(defaultVale);
            // 我们现在要去手机中找到s.xml-->拿出 main对应的值
            //手机的s.xml中找到main对应的值：cd /data/data/com.xingin.xhs/shared_prefs/
            if(key.equals("main")){
                return new StringObject(vm,defaultVale);
            }
            if(key.equals("main_hmac")){
                return new StringObject(vm,"4DsITIIfhsx35evPZ0pMeKkxdkMMSG6e52r48VnfSZRI29FzLtheGCYo6jRX8KLW35UDvbpWYjrH7nD2VgiaAMjbAxFn0ZSHBZtqhYceo4KQN3Qqm9UQnG8Rmkooxroy");
            }

            // <string name="main_hmac">  4DsITIIfhsx35evPZ0pMeKkxdkMMSG6e52r48VnfSZRI29FzLtheGCYo6jRX8KLW35UDvbpWYjrH7nD2VgiaAMjbAxFn0ZSHBZtqhYceo4KQN3Qqm9UQnG8Rmkooxroy  </string>
        }

        if(signature.equals("okhttp3/Interceptor$Chain->request()Lokhttp3/Request;")){
            //Chain.request()--->返回一个request对象---》就是我们okhttp3发送请求的request对象
            // 这个对象需要我们造出来---》借助于okhttp3包--》new Request对象
            // 后续这个request对象会一直用，所以我们把它做成全局-->包裹返回-->代码执行到这，request已经有了
            return vm.resolveClass("okhttp3/Request").newObject(request);

        }

        //2 okhttp3/Request->url()Lokhttp3/HttpUrl;
        if (signature.equals("okhttp3/Request->url()Lokhttp3/HttpUrl;")) {
            Request request = (Request) dvmObject.getValue();
            return vm.resolveClass("okhttp3/HttpUrl").newObject(request.url());
        }
        //3 okhttp3/HttpUrl->encodedPath()Ljava/lang/String;
        if (signature.equals("okhttp3/HttpUrl->encodedPath()Ljava/lang/String;")) {
            HttpUrl httpUrl = (HttpUrl) dvmObject.getValue();
            return new StringObject(vm, httpUrl.encodedPath());
        }
        //4 okhttp3/HttpUrl->encodedQuery()Ljava/lang/String;
        if (signature.equals("okhttp3/HttpUrl->encodedQuery()Ljava/lang/String;")) {
            HttpUrl httpUrl = (HttpUrl) dvmObject.getValue();
            if (httpUrl.encodedQuery() != null) {
                return new StringObject(vm, httpUrl.encodedQuery());
            }
            return new StringObject(vm, "");
        }

        //5 okhttp3/Request->body()Lokhttp3/RequestBody;
        if (signature.equals("okhttp3/Request->body()Lokhttp3/RequestBody;")) {
            Request request = (Request) dvmObject.getValue();
            return vm.resolveClass("okhttp3/RequestBody").newObject(request.body());
        }
        //6 okhttp3/Request->headers()Lokhttp3/Headers;
        if (signature.equals("okhttp3/Request->headers()Lokhttp3/Headers;")) {
            Request request = (Request) dvmObject.getValue();
            return vm.resolveClass("okhttp3/Headers").newObject(request.headers());
        }

        // 8 okio/Buffer->writeString(Ljava/lang/String;Ljava/nio/charset/Charset;)Lokio/Buffer;
        if (signature.equals("okio/Buffer->writeString(Ljava/lang/String;Ljava/nio/charset/Charset;)Lokio/Buffer;")) {
            Buffer buffer = (Buffer) dvmObject.getValue();
            buffer.writeString(vaList.getObjectArg(0).getValue().toString(), (Charset) vaList.getObjectArg(1).getValue());
            return dvmObject;
        }


        //10 okhttp3/Headers->name(I)Ljava/lang/String;
        if (signature.equals("okhttp3/Headers->name(I)Ljava/lang/String;")) {
            Headers headers = (Headers) dvmObject.getValue();
            return new StringObject(vm, headers.name(vaList.getIntArg(0)));
        }
        //11 okhttp3/Headers->value(I)Ljava/lang/String;
        if (signature.equals("okhttp3/Headers->value(I)Ljava/lang/String;")) {
            Headers headers = (Headers) dvmObject.getValue();
            return new StringObject(vm, headers.value(vaList.getIntArg(0)));
        }

        //13 okio/Buffer->clone()Lokio/Buffer;
        if (signature.equals("okio/Buffer->clone()Lokio/Buffer;")) {
            Buffer buffer = (Buffer) dvmObject.getValue();
            return vm.resolveClass("okio/Buffer").newObject(buffer.clone());
        }
        //15 okhttp3/Request->newBuilder()Lokhttp3/Request$Builder;
        if (signature.equals("okhttp3/Request->newBuilder()Lokhttp3/Request$Builder;")) {
            Request request = (Request) dvmObject.getValue();
            return vm.resolveClass("okhttp3/Request$Builder").newObject(request.newBuilder());
        }
        //16 okhttp3/Request$Builder->header(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;（有shield）
        if (signature.equals("okhttp3/Request$Builder->header(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;")) {
            Request.Builder builder = (Request.Builder) dvmObject.getValue();
            builder.header(vaList.getObjectArg(0).getValue().toString(), vaList.getObjectArg(1).getValue().toString());
            if ("shield".equals(vaList.getObjectArg(0).getValue().toString())) {
                String shield = vaList.getObjectArg(1).getValue().toString();
                System.out.println("shield=" + shield);
            }
            return dvmObject;
        }

        //17 okhttp3/Request$Builder->build()Lokhttp3/Request;
        if (signature.equals("okhttp3/Request$Builder->build()Lokhttp3/Request;")) {
            Request.Builder builder = (Request.Builder) dvmObject.getValue();
            Request request = builder.build();
            return vm.resolveClass("okhttp3/Request").newObject(request);
        }
        //18 okhttp3/Interceptor$Chain->proceed(Lokhttp3/Request;)Lokhttp3/Response;
        if (signature.equals("okhttp3/Interceptor$Chain->proceed(Lokhttp3/Request;)Lokhttp3/Response;")) {
            return vm.resolveClass("okhttp3/Response").newObject(null);
        }

        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        // 7 okio/Buffer-><init>()V
        if (signature.equals("okio/Buffer-><init>()V")) {
            return dvmClass.newObject(new Buffer());
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        // 9 okhttp3/Headers->size()I
        if (signature.equals("okhttp3/Headers->size()I")) {
            Headers headers = (Headers) dvmObject.getValue();
            return headers.size();
        }
        //14 okio/Buffer->read([B)I
        if (signature.equals("okio/Buffer->read([B)I")) {
            Buffer buffer = (Buffer) dvmObject.getValue();
            return buffer.read((byte[]) vaList.getObjectArg(0).getValue());
        }

        //19 okhttp3/Response->code()I
        if (signature.equals("okhttp3/Response->code()I")) {
            return 200;
        }

        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }
    //12 okhttp3/RequestBody->writeTo(Lokio/BufferedSink;)V
    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if (signature.equals("okhttp3/RequestBody->writeTo(Lokio/BufferedSink;)V")) {
            BufferedSink bufferedSink = (BufferedSink) vaList.getObjectArg(0).getValue();
            RequestBody requestBody = (RequestBody) dvmObject.getValue();
            if (requestBody != null) {
                try {
                    requestBody.writeTo(bufferedSink);
                } catch (IOException e) {
                    System.out.println("错误了" + e);
                }
            }
            return;
        }

        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

}
